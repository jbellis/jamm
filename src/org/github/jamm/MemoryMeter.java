package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;

import org.github.jamm.MemoryMeterListener.Factory;
import org.github.jamm.strategies.MemoryMeterStrategies;

public class MemoryMeter {

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

    private final FieldAndClassFilter classFilters;

    private final FieldFilter fieldFilters;

    private final boolean omitSharedBufferOverhead;
    private final MemoryMeterListener.Factory listenerFactory;

    private MemoryMeter(Builder builder) {

        this.strategy = MemoryMeterStrategies.getInstance().getStrategy(builder.guess);
        this.omitSharedBufferOverhead = builder.omitSharedBufferOverhead;
        this.classFilters = Filters.getClassFilters(builder.ignoreKnownSingletons);
        this.fieldFilters = Filters.getFieldFilters(builder.ignoreKnownSingletons, builder.ignoreOuterClassReference, builder.ignoreNonStrongReferences);
        this.listenerFactory = builder.listenerFactory;
    }

    public static Builder builder()
    {
        return new Builder();
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

        if (classFilters.ignore(object.getClass()))
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
                addFieldChildren(current, stack, tracker, listener);
            }
        }

        listener.done(total);
        return total;
    }

    private void addFieldChildren(Object current, Deque<Object> stack, Set<Object> tracker, MemoryMeterListener listener) {
        Class<?> cls = current.getClass();
        while (cls != null) {
            for (Field field : cls.getDeclaredFields()) {
                if (fieldFilters.ignore(field)) {
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
                    listener.fieldAdded(current, field.getName(), child);
                }
            }

            cls = cls.getSuperclass();
        }
    }

    private void addArrayChildren(Object[] current, Deque<Object> stack, Set<Object> tracker, MemoryMeterListener listener) {
        for (int i = 0; i < current.length; i++) {
            Object child = current[i];
            if (child != null && !tracker.contains(child)) {

                Class<?> childCls = child.getClass();
                if (classFilters.ignore(childCls)) {
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
