package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.Callable;

public class MemoryMeter {

	private static final String IGNORE_OUTER = "ignoreouter";
	
	private static final String outerClassReference = "this\\$[0-9]+";

	private static boolean ignoreOuterClassReference = false;
	
    private static Instrumentation instrumentation;

    public static void premain(String options, Instrumentation inst) {
    	if (IGNORE_OUTER.equalsIgnoreCase(options)){
    		ignoreOuterClassReference = true;
    	}
    		
        MemoryMeter.instrumentation = inst;
    }
    
    public static void agentmain(String options, Instrumentation inst) {
    	if (IGNORE_OUTER.equalsIgnoreCase(options)){
    		ignoreOuterClassReference = true;
    	}
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

    public MemoryMeter() {
        this(new Callable<Set<Object>>() {
            public Set<Object> call() throws Exception {
                // using a normal HashSet to track seen objects screws things up in two ways:
                // - it can undercount objects that are "equal"
                // - calling equals() can actually change object state (e.g. creating entrySet in HashMap)
                return Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            }
        }, true, Guess.NEVER);
    }

    /**
     * @param trackerProvider returns a Set with which to track seen objects and avoid cycles
     * @param includeFullBufferSize
     * @param guess
     */
    private MemoryMeter(Callable<Set<Object>> trackerProvider, boolean includeFullBufferSize, Guess guess) {
        this.trackerProvider = trackerProvider;
        this.includeFullBufferSize = includeFullBufferSize;
        this.guess = guess;
    }

    /**
     * @param trackerProvider
     * @return a MemoryMeter with the given provider
     */
    public MemoryMeter withTrackerProvider(Callable<Set<Object>> trackerProvider) {
        return new MemoryMeter(trackerProvider, includeFullBufferSize, guess);
    }

    /**
     * @return a MemoryMeter that only counts the bytes remaining in a ByteBuffer
     * in measureDeep, rather than the full size of the backing array.
     * TODO: handle other types of Buffers
     */
    public MemoryMeter omitSharedBufferOverhead() {
        return new MemoryMeter(trackerProvider, false, guess);
    }

    /**
     * @return a MemoryMeter that permits guessing the size of objects if instrumentation isn't enabled
     */
    public MemoryMeter withGuessing(Guess guess) {
        return new MemoryMeter(trackerProvider, includeFullBufferSize, guess);
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
                        case FALLBACK_BEST:
                            if (MemoryLayoutSpecification.hasUnsafe())
                                return MemoryLayoutSpecification.sizeOfWithUnsafe(object);
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

        Set<Object> tracker;
        try {
            tracker = trackerProvider.call();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        tracker.add(object);

        // track stack manually so we can handle deeper hierarchies than recursion
        Deque<Object> stack = new ArrayDeque<Object>();
        stack.push(object);

        long total = 0;
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            assert current != null;
            total += measure(current);

            if (current instanceof Object[]) {
                addArrayChildren((Object[]) current, stack, tracker);
            } else if (current instanceof ByteBuffer && !includeFullBufferSize) {
                total += ((ByteBuffer) current).remaining();
            } else {
                addFieldChildren(current, stack, tracker);
            }
        }

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

        Set<Object> tracker = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        tracker.add(object);
        Deque<Object> stack = new ArrayDeque<Object>();
        stack.push(object);

        long total = 0;
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            assert current != null;
            total++;

            if (current instanceof Object[]) {
                addArrayChildren((Object[]) current, stack, tracker);
            } else {
                addFieldChildren(current, stack, tracker);
            }
        }

        return total;
    }

    private void addFieldChildren(Object current, Deque<Object> stack, Set<Object> tracker) {
        Class cls = current.getClass();
        while (cls != null) {
            for (Field field : cls.getDeclaredFields()) {
                if (field.getType().isPrimitive() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                if (ignoreOuterClassReference && field.getName().matches(outerClassReference)) {
                	continue;
                }
                
                field.setAccessible(true);
                Object child;
                try {
                    child = field.get(current);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                if (child != null && !tracker.contains(child)) {
                    stack.push(child);
                    tracker.add(child);
                }
            }

            cls = cls.getSuperclass();
        }
    }

    private void addArrayChildren(Object[] current, Deque<Object> stack, Set<Object> tracker) {
        for (Object child : current) {
            if (child != null && !tracker.contains(child)) {
                stack.push(child);
                tracker.add(child);
            }
        }
    }

}
