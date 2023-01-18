package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;

import org.github.jamm.MemoryMeterListener.Factory;
import org.github.jamm.strategies.MemoryMeterStrategies;

public class MemoryMeter {

    private static final String outerClassReference = "this\\$[0-9]+";

    public static void premain(String options, Instrumentation inst) {
        MemoryMeterStrategies.instrumentation = inst;
    }
    
    public static void agentmain(String options, Instrumentation inst) {
        MemoryMeterStrategies.instrumentation = inst;
    }

    public static boolean hasInstrumentation() {
        return MemoryMeterStrategies.getInstance().hasInstrumentation();
    }

    public static boolean hasUnsafe() {
        return MemoryMeterStrategies.getInstance().hasUnsafe();
    }

    public static enum Guess {

        /* If instrumentation is not available, error when measuring */
        ALWAYS_INSTRUMENTATION,
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
        ALWAYS_UNSAFE;
    }

    /**
     * The strategy used to measure the objects.
     */
    private final MemoryMeterStrategy strategy;

    private final Guess guess;
    private final boolean omitSharedBufferOverhead;
    private final boolean ignoreOuterClassReference;
    private final boolean ignoreKnownSingletons;
    private final boolean ignoreNonStrongReferences;
    private final MemoryMeterListener.Factory listenerFactory;

    private MemoryMeter(Builder builder) {

        this.strategy = MemoryMeterStrategies.getInstance().getStrategy(builder.guess);
        this.guess = builder.guess;
        this.omitSharedBufferOverhead = builder.omitSharedBufferOverhead;
        this.ignoreOuterClassReference = builder.ignoreOuterClassReference;
        this.ignoreKnownSingletons = builder.ignoreKnownSingletons;
        this.ignoreNonStrongReferences = builder.ignoreNonStrongReferences;
        this.listenerFactory = builder.listenerFactory;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public Builder unbuild()
    {
        return new Builder(this.guess,
                           this.ignoreOuterClassReference,
                           this.ignoreKnownSingletons,
                           this.ignoreNonStrongReferences,
                           this.omitSharedBufferOverhead,
                           this.listenerFactory);
    }

    /**
     * @return the shallow memory usage of @param object
     * @throws NullPointerException if object is null
     */
    public long measure(Object object) {
        return strategy.measure(object);
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

        Set<Object> tracker = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
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
            } else if (current instanceof ByteBuffer && omitSharedBufferOverhead) {
                total += ((ByteBuffer) current).remaining();
            } else {
            	Object referent = (ignoreNonStrongReferences && (current instanceof Reference)) ? ((Reference<?>)current).get() : null;
                addFieldChildren(current, stack, tracker, referent, listener);
            }
        }

        listener.done(total);
        return total;
    }

    private void addFieldChildren(Object current, Deque<Object> stack, Set<Object> tracker, Object ignorableChild, MemoryMeterListener listener) {
        Class<?> cls = current.getClass();
        while (!skipClass(cls)) {
            for (Field field : cls.getDeclaredFields()) {
                if (field.getType().isPrimitive()
                        || Modifier.isStatic(field.getModifiers())
                        || field.isAnnotationPresent(Unmetered.class)) {
                    continue;
                }
                
                if (ignoreOuterClassReference && field.getName().matches(outerClassReference)) {
                	continue;
                }

                if (ignoreClass(field.getType())) {
                	continue;
                }

                field.setAccessible(true);
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

            cls = cls.getSuperclass();
        }
    }

    private static final Class clsJLRModule;
    private static final Class clsJLMModuleDescriptor;
    private static final Class clsJLRAccessibleObject;
    private static final Class clsSRAAnnotationInvocationHandler;
    private static final Class clsSRAAnnotationType;
    private static final Class clsJIRUnsafeFieldAccessorImpl;
    private static final Class clsJIRDelegatingMethodAccessorImpl;
    static
    {
        clsJLRModule = maybeGetClass("java.lang.reflect.Module");
        clsJLMModuleDescriptor = maybeGetClass("java.lang.module.ModuleDescriptor");
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

    private boolean skipClass(Class<?> cls) {
        return cls == null || cls == Class.class
               || cls == clsJLRModule || cls == clsJLMModuleDescriptor || cls == clsJLRAccessibleObject
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

    public static final class Builder {

        private Guess guess = Guess.ALWAYS_INSTRUMENTATION;
        private boolean ignoreOuterClassReference;
        private boolean ignoreKnownSingletons;
        private boolean ignoreNonStrongReferences;
        private boolean omitSharedBufferOverhead;
        private MemoryMeterListener.Factory listenerFactory = NoopMemoryMeterListener.FACTORY;

        private Builder() {

        }

        private Builder(Guess guess,
                       boolean ignoreOuterClassReference,
                       boolean ignoreKnownSingletons,
                       boolean ignoreNonStrongReferences,
                       boolean omitSharedBufferOverhead,
                       Factory listenerFactory)
        {
            this.guess = guess;
            this.ignoreOuterClassReference = ignoreOuterClassReference;
            this.ignoreKnownSingletons = ignoreKnownSingletons;
            this.ignoreNonStrongReferences = ignoreNonStrongReferences;
            this.omitSharedBufferOverhead = omitSharedBufferOverhead;
            this.listenerFactory = listenerFactory;
        }

        public MemoryMeter build() {
            return new MemoryMeter(this);
        }

        /**
         * See {@link Guess} for possible guess-modes.
         */
        public Builder withGuessing(Guess guess) {
            this.guess = guess;
            return this;
        }

        public Builder ignoreOuterClassReference() {
            this.ignoreOuterClassReference = true;
            return this;
        }

        /**
         * ignores space occupied by known singletons such as {@link Class} objects and {@code enum}s
         */
        public Builder ignoreKnownSingletons() {
            this.ignoreKnownSingletons = true;
            return this;
        }

        /**
         * Ignores the references from a {@link java.lang.ref.Reference} (like weak/soft/phantom references).
         */
        public Builder ignoreNonStrongReferences() {
            ignoreNonStrongReferences = true;
            return this;
        }


        /**
         * Counts only the bytes remaining in a ByteBuffer
         * in measureDeep, rather than the full size of the backing array.
         */
        public Builder omitSharedBufferOverhead() {
            omitSharedBufferOverhead = true;
            return this;
        }
        
        /**
         * Prints the classes tree to {@ code System.out} when measuring through {@code measureDeep}.
         */
        public Builder printVisitedTree() {
            return printVisitedTreeUpTo(Integer.MAX_VALUE);
        }

        /**
         * Prints the classes tree to {@ code System.out} up to the specified depth when measuring through {@code measureDeep}.
         */
        public Builder printVisitedTreeUpTo(int depth) {
            if (depth <= 0)
                throw new IllegalArgumentException(String.format("the depth must be greater than zero (was %s).", depth));

            listenerFactory = new TreePrinter.Factory(depth);
            return this;
        }
    }
}
