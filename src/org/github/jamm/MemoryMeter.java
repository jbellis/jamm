package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class MemoryMeter {

    private static ClassValue<Field[]> refFields = new ClassValue<Field[]>() {
        @Override protected Field[] computeValue(Class<?> type) {
            return getReferenceFields(type);
        }
    };

	private static final String outerClassReference = "this\\$[0-9]+";
	
    private static Instrumentation instrumentation;

    public static void premain(String options, Instrumentation inst) {
        MemoryMeter.instrumentation = inst;
    }
    
    public static void agentmain(String options, Instrumentation inst) {
    	MemoryMeter.instrumentation = inst;
    }

    public static boolean hasInstrumentation() {
        return instrumentation != null;
    }

    public static enum Guess {
        /* If instrumentation is not available, error when measuring */
        NEVER,
        /* If instrumentation is available, use it, otherwise guess the size using predefined specifications */
        FALLBACK_SPEC,
        /* If instrumentation is available, use it, otherwise guess the size using sun.misc.Unsafe */
        FALLBACK_UNSAFE,
        /* If instrumentation is available, use it, otherwise guess the size using sun.misc.Unsafe; if that is unavailable,
         * guess using predefined specifications.*/
        FALLBACK_BEST,
        /* Always guess the size of measured objects using predefined specifications*/
        ALWAYS_SPEC,
        /* Always guess the size of measured objects using sun.misc.Unsafe */
        ALWAYS_UNSAFE
    }

    private final Callable<Set<Object>> trackerProvider;
    private final boolean includeFullBufferSize;
    private final Guess guess;
    private final boolean ignoreOuterClassReference;
    private final boolean ignoreKnownSingletons;
    private final boolean ignoreNonStrongReferences;
    private final MemoryMeterListener.Factory listenerFactory;

    public MemoryMeter() {
        this(new Callable<Set<Object>>() {
            public Set<Object> call() throws Exception {
                // using a normal HashSet to track seen objects screws things up in two ways:
                // - it can undercount objects that are "equal"
                // - calling equals() can actually change object state (e.g. creating entrySet in HashMap)
                return Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            }
        }, true, Guess.NEVER, false, false, false, NoopMemoryMeterListener.FACTORY);
    }

    /**
     * @param trackerProvider returns a Set with which to track seen objects and avoid cycles
     * @param includeFullBufferSize
     * @param guess
     * @param listenerFactory the <code>MemoryMeterListener.Factory</code>
     */
    private MemoryMeter(Callable<Set<Object>> trackerProvider,
                        boolean includeFullBufferSize,
                        Guess guess,
                        boolean ignoreOuterClassReference,
                        boolean ignoreKnownSingletons,
                        boolean ignoreNonStrongReferences,
                        MemoryMeterListener.Factory listenerFactory) {

        this.trackerProvider = trackerProvider;
        this.includeFullBufferSize = includeFullBufferSize;
        this.guess = guess;
        this.ignoreOuterClassReference = ignoreOuterClassReference;
        this.ignoreKnownSingletons = ignoreKnownSingletons;
        this.ignoreNonStrongReferences = ignoreNonStrongReferences;
        this.listenerFactory = listenerFactory;
    }

    /**
     * @param trackerProvider
     * @return a MemoryMeter with the given provider
     */
    public MemoryMeter withTrackerProvider(Callable<Set<Object>> trackerProvider) {
        return new MemoryMeter(trackerProvider,
                               includeFullBufferSize,
                               guess,
                               ignoreOuterClassReference,
                               ignoreKnownSingletons,
                               ignoreNonStrongReferences,
                               listenerFactory);
    }

    /**
     * @return a MemoryMeter that only counts the bytes remaining in a ByteBuffer
     * in measureDeep, rather than the full size of the backing array.
     * TODO: handle other types of Buffers
     */
    public MemoryMeter omitSharedBufferOverhead() {
        return new MemoryMeter(trackerProvider,
                               false,
                               guess,
                               ignoreOuterClassReference,
                               ignoreKnownSingletons,
                               ignoreNonStrongReferences,
                               listenerFactory);
    }

    /**
     * @return a MemoryMeter that permits guessing the size of objects if instrumentation isn't enabled
     */
    public MemoryMeter withGuessing(Guess guess) {
        return new MemoryMeter(trackerProvider,
                               includeFullBufferSize,
                               guess,
                               ignoreOuterClassReference,
                               ignoreKnownSingletons,
                               ignoreNonStrongReferences,
                               listenerFactory);
    }
    
    /**
     * @return a MemoryMeter that ignores the size of an outer class reference
     */
    public MemoryMeter ignoreOuterClassReference() {
        return new MemoryMeter(trackerProvider,
                               includeFullBufferSize,
                               guess,
                               true,
                               ignoreKnownSingletons,
                               ignoreNonStrongReferences,
                               listenerFactory);
    }
    
    /**
     * return a MemoryMeter that ignores space occupied by known singletons such as Class objects and Enums
     */
    public MemoryMeter ignoreKnownSingletons() {
        return new MemoryMeter(trackerProvider,
                               includeFullBufferSize,
                               guess,
                               ignoreOuterClassReference,
                               true,
                               ignoreNonStrongReferences,
                               listenerFactory);
    }
    
    /**
     * return a MemoryMeter that ignores space occupied by known singletons such as Class objects and Enums
     */
    public MemoryMeter ignoreNonStrongReferences() {
        return new MemoryMeter(trackerProvider,
                               includeFullBufferSize,
                               guess,
                               ignoreOuterClassReference,
                               ignoreKnownSingletons,
                               true,
                               listenerFactory);
    }

    /**
     * Makes this <code>MemoryMeter</code> prints the classes tree to <code>System.out</code> when measuring
     */
    public MemoryMeter enableDebug() {
        return enableDebug(Integer.MAX_VALUE);
    }

    /**
     * Makes this <code>MemoryMeter</code> prints the classes tree to <code>System.out</code> up to the specified depth
     * when measuring
     * @param depth the maximum depth for which the class tree must be printed
     */
    public MemoryMeter enableDebug(int depth) {
        if (depth <= 0)
            throw new IllegalArgumentException(String.format("the depth must be greater than zero (was %s).", depth));
        return new MemoryMeter(trackerProvider,
                               includeFullBufferSize,
                               guess,
                               ignoreOuterClassReference,
                               ignoreKnownSingletons,
                               ignoreNonStrongReferences,
                               new TreePrinter.Factory(depth));
    }

    /**
     * @return the shallow memory usage of @param object
     * @throws NullPointerException if object is null
     */
    public long measure(Object object) {
        switch (guess) {
            case ALWAYS_UNSAFE:
                return MemoryLayoutSpecification.sizeOfWithUnsafe(object);
            case ALWAYS_SPEC:
                return MemoryLayoutSpecification.sizeOf(object);
            default:
                if (instrumentation == null) {
                    switch (guess) {
                        case NEVER:
                            throw new IllegalStateException("Instrumentation is not set; Jamm must be set as -javaagent");
                        case FALLBACK_UNSAFE:
                            if (!MemoryLayoutSpecification.hasUnsafe())
                                throw new IllegalStateException("Instrumentation is not set and sun.misc.Unsafe could not be obtained; Jamm must be set as -javaagent, or the SecurityManager must permit access to sun.misc.Unsafe");
                            //$FALL-THROUGH$
                        case FALLBACK_BEST:
                            if (MemoryLayoutSpecification.hasUnsafe())
                                return MemoryLayoutSpecification.sizeOfWithUnsafe(object);
                            //$FALL-THROUGH$
                        case FALLBACK_SPEC:
                            return MemoryLayoutSpecification.sizeOf(object);
                    }
                }
                return instrumentation.getObjectSize(object);
        }
    }

    /**
     * @return the memory usage of @param object including referenced objects
     * @throws NullPointerException if object is null
     */
    public long measureDeep(Object object) {
        if (object == null) {
            throw new NullPointerException(); // match getObjectSize behavior
        }

        if (ignoreClass(object.getClass()))
            return 0;

        Set<Object> tracker;
        try {
            tracker = trackerProvider.call();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        MemoryMeterListener listener = listenerFactory.newInstance();

        tracker.add(object);
        listener.started(object);

        // track stack manually so we can handle deeper hierarchies than recursion
        Deque<Object> stack = new ArrayDeque<Object>();
        stack.push(object);

        long total = 0;
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            assert current != null;
            long size = measure(current);
            listener.objectMeasured(current, size);
            total += size;

            if (current instanceof Object[]) {
                addArrayChildren((Object[]) current, stack, tracker, listener);
            } else if (current instanceof ByteBuffer && !includeFullBufferSize) {
                total += ((ByteBuffer) current).remaining();
            } else {
            	Object referent = (ignoreNonStrongReferences && (current instanceof Reference)) ? ((Reference<?>)current).get() : null;
                addFieldChildren(current, stack, tracker, referent, listener);
            }
        }

        listener.done(total);
        return total;
    }

    /**
     * @return the number of child objects referenced by @param object
     * @throws NullPointerException if object is null
     */
    public long countChildren(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }

        MemoryMeterListener listener = listenerFactory.newInstance();
        Set<Object> tracker = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        tracker.add(object);
        listener.started(object);
        Deque<Object> stack = new ArrayDeque<Object>();
        stack.push(object);

        long total = 0;
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            assert current != null;
            total++;
            listener.objectCounted(current);

            if (current instanceof Object[]) {
                addArrayChildren((Object[]) current, stack, tracker, listener);
            } else {
            	Object referent = (ignoreNonStrongReferences && (current instanceof Reference)) ? ((Reference<?>)current).get() : null;
                addFieldChildren(current, stack, tracker, referent, listener);
            }
        }

        listener.done(total);
        return total;
    }

    private static Field[] getReferenceFields(final Class<?> type) {
        return AccessController.doPrivileged(new PrivilegedAction<Field[]>() {
            @Override public Field[] run() {
                return getReferenceFieldsPrivileged(type);
            }
        });
    }

    private static Field[] getReferenceFieldsPrivileged(Class<?> cls) {
        List<Field> fields = new ArrayList<Field>();
        while (!skipClass(cls)) {
            for (Field field : cls.getDeclaredFields()) {
                if (field.getType().isPrimitive()
                        || Modifier.isStatic(field.getModifiers())
                        || field.isAnnotationPresent(Unmetered.class)) {
                    continue;
                }

                field.setAccessible(true);
                fields.add(field);
            }

            cls = cls.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    private void addFieldChildren(Object current, Deque<Object> stack, Set<Object> tracker, Object ignorableChild, MemoryMeterListener listener) {
        for (Field field : refFields.get(current.getClass())) {
            if (ignoreOuterClassReference && field.getName().matches(outerClassReference)) {
                continue;
            }

            if (ignoreClass(field.getType())) {
                continue;
            }

            Object child;
            try {
                child = field.get(current);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            if (child != ignorableChild) {
                if (child != null && !tracker.contains(child)) {
                    stack.push(child);
                    tracker.add(child);
                    listener.fieldAdded(current, field.getName(), child);
                }
            }
        }
    }

    private static final Class clsJLRModule;
    private static final Class clsJLRAccessibleObject;
    private static final Class clsSRAAnnotationInvocationHandler;
    private static final Class clsSRAAnnotationType;
    private static final Class clsJIRUnsafeFieldAccessorImpl;
    private static final Class clsJIRDelegatingMethodAccessorImpl;
    static
    {
        clsJLRModule = maybeGetClass("java.lang.reflect.Module");
        clsJLRAccessibleObject = maybeGetClass("java.lang.reflect.AccessibleObject");
        clsSRAAnnotationInvocationHandler = maybeGetClass("sun.reflect.annotation.AnnotationInvocationHandler");
        clsSRAAnnotationType = maybeGetClass("sun.reflect.annotation.AnnotationType");
        clsJIRUnsafeFieldAccessorImpl = maybeGetClass("jdk.internal.reflect.UnsafeFieldAccessorImpl");
        clsJIRDelegatingMethodAccessorImpl = maybeGetClass("jdk.internal.reflect.DelegatingMethodAccessorImpl");
    }

    private static Class<?> maybeGetClass(String name)
    {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean skipClass(Class<?> cls) {
        return cls == null
               || cls == clsJLRModule || cls == clsJLRAccessibleObject
               || cls == clsSRAAnnotationInvocationHandler || cls == clsSRAAnnotationType
               || cls == clsJIRUnsafeFieldAccessorImpl || cls == clsJIRDelegatingMethodAccessorImpl;
    }

    private boolean ignoreClass(Class<?> cls) {
        return (ignoreKnownSingletons && (cls.equals(Class.class) || Enum.class.isAssignableFrom(cls)))
                || isAnnotationPresent(cls);
    }

    private boolean isAnnotationPresent(Class<?> cls) {

        if (cls == null)
            return false;

        if (cls.isAnnotationPresent(Unmetered.class))
            return true;

        Class<?>[] interfaces = cls.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (isAnnotationPresent(cls.getInterfaces()[i]))
                return true;
        }

        return isAnnotationPresent(cls.getSuperclass());
    }

    private void addArrayChildren(Object[] current, Deque<Object> stack, Set<Object> tracker, MemoryMeterListener listener) {
        for (int i = 0; i < current.length; i++) {
            Object child = current[i];
            if (child != null && !tracker.contains(child)) {
            	
                Class<?> childCls = child.getClass();
                if (ignoreClass(childCls)) {
                	continue;
                }
                
                stack.push(child);
                tracker.add(child);
                listener.fieldAdded(current, Integer.toString(i) , child);
            }
        }
    }
}
