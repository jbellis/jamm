package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.github.jamm.accessors.FieldAccessor;
import org.github.jamm.listeners.NoopMemoryMeterListener;
import org.github.jamm.listeners.TreePrinter;
import org.github.jamm.strategies.MemoryMeterStrategies;
import org.github.jamm.string.StringMeter;
import org.github.jamm.utils.ByteBufferMeasurementUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * Utility to measure the heap space used by java objects.
 * <p>This class supports multithreading and can be reused safely.</p>
 */
public final class MemoryMeter {

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

    public static boolean useStringOptimization() {
        return StringMeter.ENABLED;
    }

    /**
     * The different strategies that can be used by a {@code MemoryMeter} instance to measure the shallow size of an object.
     */
    public enum Guess {
        /**
         * Relies on {@code java.lang.instrument.Instrumentation} to measure shallow object size.
         * It requires {@code Instrumentation} to be available, but it is the most accurate strategy.
         */
        INSTRUMENTATION {

            public boolean requireInstrumentation() {
                return true;
            }
        },
        /**
         * Relies on {@code java.lang.instrument.Instrumentation} to measure non array object and the {@code Specification} approach to measure arrays.
         * This strategy tries to combine the best of both strategies the accuracy and speed of {@code Instrumentation} for non array object
         * and the speed of {@code Specification} for measuring array objects for which all strategy are accurate. For some reason {@code Instrumentation} is slower for arrays before Java 17.
         */
        INSTRUMENTATION_AND_SPECIFICATION {

            public boolean requireInstrumentation() {
                return true;
            }
        },
        /**
         * Relies on {@code Unsafe} to measure shallow object size.
         * It requires {@code Unsafe} to be available. After INSTRUMENTATION based strategy UNSAFE is the most accurate strategy.
         */
        UNSAFE {

            public boolean requireUnsafe() {
                return true;
            }

            public boolean canBeUsedAsFallbackFrom(Guess strategy) {
                return strategy.requireInstrumentation();
            }
        },
        /**
         * Computes the shallow size of objects using VM information.
         */
        SPECIFICATION {

            public boolean requireUnsafe() {
                return true;
            }

            public boolean canBeUsedAsFallbackFrom(Guess guess) {
                return true;
            }
        };

        /**
         * Checks if this strategy requires {@code Instrumentation} to be present.
         * @return {@code true} if this strategy requires {@code Instrumentation} to be present, {@code false} otherwise.
         */
        public boolean requireInstrumentation() {
            return false;
        }

        /**
         * Checks if this strategy requires {@code Unsafe} to be present.
         * @return {@code true} if this strategy requires {@code Unsafe} to be present, {@code false} otherwise.
         */
        public boolean requireUnsafe() {
            return false;
        }

        public boolean canBeUsedAsFallbackFrom(Guess guess) {
            return false;
        }

        public static void checkOrder(List<Guess> guesses) {
            Guess previous = null;
            for (Guess guess : guesses) {
                if (previous != null && !guess.canBeUsedAsFallbackFrom(previous)) {
                    throw new IllegalArgumentException("The " + guess + " strategy cannot be used as fallback for the " + previous + " strategy.");
                }
                previous = guess;
            }
        }
    }

    /**
     * The different way of measuring deeply a ByteBuffer.
     */
    public enum ByteBufferMode {
        /**
         * Default mode, measure the ByteBuffer and all of its children
         */
        NORMAL {
            @Override
            public boolean isSlab(ByteBuffer buffer) {
                return false;
            }
        },
        /**
         * Mode used to handle SLAB allocated {@code ByteBuffers}, without slices, where the overhead amortized over all
         * the allocations is negligible and we prefer to undercount than over count.
         */
        SLAB_ALLOCATION_NO_SLICE {
            @Override
            public boolean isSlab(ByteBuffer buffer) {
                // No slice, means that SLAB are only allocated by duplicating the buffer and changing its position and limit.
                return buffer.capacity() > buffer.remaining();
            }
        },
        /**
         * Mode used to handle SLAB allocated {@code ByteBuffers}, with slices, where the overhead amortized over all
         * the allocations is negligible and we prefer to undercount than over count.
         */
        SLAB_ALLOCATION_SLICE {
            @Override
            public boolean isSlab(ByteBuffer buffer) {
                return buffer.capacity() < ByteBufferMeasurementUtils.underlyingCapacity(buffer, ACCESSOR) ;
            }
        };

        /**
         * Checks if this buffer can be considered as a SLAB according to this mode.
         *
         * @param buffer the buffer to check.
         * @return {@code true} if this buffer can be considered as a SLAB according to this mode, {@code false} otherwise.
         */
        public abstract boolean isSlab(ByteBuffer buffer);
    }

    /**
     * The default guesses in accuracy order.
     */
    public static final List<Guess> BEST = unmodifiableList(asList(Guess.INSTRUMENTATION, Guess.UNSAFE, Guess.SPECIFICATION));

    /**
     * The accessor used to retrieve field values.
     *
     * <p>For JDK prior to Java 9, {@code MemoryMeter} will use plain reflection. From Java 9 onward {@code MemoryMeter}
     * will use reflection if the object is within an accessible module otherwise it will rely on Unsafe to access the field value.</p>
     */
    private static final FieldAccessor ACCESSOR = FieldAccessor.newInstance();

    /**
     * The strategy used to measure the objects.
     */
    private final MemoryMeterStrategy strategy;

    /**
     * Filter used to determine which classes should be ignored.
     */
    private final FieldAndClassFilter classFilter;

    /**
     * Filter used to determine which field should be ignored.
     */
    private final FieldFilter fieldFilter;

    /**
     * Utility used to optimize the deep measurement of String objects.
     */
    private final StringMeter STRING_METER = StringMeter.newInstance();

    /**
     * The factory used to create the listener listening to the object graph traversal.
     */
    private final MemoryMeterListener.Factory listenerFactory;

    private MemoryMeter(Builder builder) {

        this(MemoryMeterStrategies.getInstance().getStrategy(builder.guesses),
             Filters.getClassFilters(builder.ignoreKnownSingletons),
             Filters.getFieldFilters(builder.ignoreKnownSingletons, builder.ignoreOuterClassReference, builder.ignoreNonStrongReferences),
             builder.listenerFactory);
    }

    /**
     * Create a new {@link MemoryMeter} instance from the different component it needs to measure object graph.
     * <p>Unless there is a specific need to override some of the {@code MemoryMeter} logic people should only create 
     * {@code MemoryMeter} instances through {@code MemoryMeter.builder()}. This constructor provides a way to modify part of the 
     * logic being used by allowing to use specific implementations for the strategy or filters.</p>
     * 
     * @param strategy the {@code MemoryMeterStrategy} to use for measuring object shallow size.
     * @param classFilter the filter used to filter out classes from the measured object graph
     * @param fieldFilter the filter used to filter out fields from the measured object graph
     * @param listenerFactory the factory used to create the listener listening to the object graph traversal
     */
    public MemoryMeter(MemoryMeterStrategy strategy,
                       FieldAndClassFilter classFilter,
                       FieldFilter fieldFilter,
                       MemoryMeterListener.Factory listenerFactory) {

        this.strategy = strategy;
        this.classFilter = classFilter;
        this.fieldFilter = fieldFilter;
        this.listenerFactory = listenerFactory;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Provides information about the memory layout used by the JVM.
     * @return information about the memory layout used by the JVM
     */
    public static MemoryLayoutSpecification getMemoryLayoutSpecification() {
        return MemoryMeterStrategy.MEMORY_LAYOUT;
    }

    /**
     * Measures the shallow memory usage of the object.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param object the object to measure
     * @return the shallow memory usage of @param object
     */
    public long measure(Object object) {

        if (object == null)
            return 0L;

        return strategy.measure(object);
    }

    /**
     * Measures the shallow memory usage of the specified Object array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the object array to measure
     * @return the shallow memory usage of the array
     */
    public long measureArray(Object[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the shallow memory usage of the specified byte array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the byte array to measure
     * @return the shallow memory usage of the byte array
     */
    public long measureArray(byte[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the shallow memory usage of the specified boolean array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the boolean array to measure
     * @return the shallow memory usage of the boolean array
     */
    public long measureArray(boolean[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the shallow memory usage of the specified short array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the short array to measure
     * @return the shallow memory usage of the short array
     */
    public long measureArray(short[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the shallow memory usage of the specified char array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the char array to measure
     * @return the shallow memory usage of the char array
     */
    public long measureArray(char[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the shallow memory usage of the specified int array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the int array to measure
     * @return the shallow memory usage of the int array
     */
    public long measureArray(int[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the shallow memory usage of the specified float array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the float array to measure
     * @return the shallow memory usage of the float array
     */
    public long measureArray(float[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the shallow memory usage of the specified double array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the double array to measure
     * @return the shallow memory usage of the double array
     */
    public long measureArray(double[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the shallow memory usage of the specified long array.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param array the long array to measure
     * @return the shallow memory usage of the long array
     */
    public long measureArray(long[] array) {

        if (array == null)
            return 0L;

        return strategy.measureArray(array);
    }

    /**
     * Measures the deep memory usage of the specified {@code String}
     *
     * @param s the {@code String} to measure
     * @return the deep memory usage of the specified string
     */
    public long measureStringDeep(String s) {

        if (StringMeter.ENABLED) {

            if (s == null)
                return 0L;

            return STRING_METER.measureDeep(strategy, s);
        }
        return measureDeep(s);
    }

    /**
     * Measures the memory usage of the object including referenced objects.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     * <p>Calling this method is equivalent to calling {@code measureDeep(object, ByteBufferMode)} with a {@code NORMAL} {@code ByteBufferMode}.</p>
     *
     * @param object the object to measure
     * @return the memory usage of @param object including referenced objects
     */
    public long measureDeep(Object object) {
        return measureDeep(object, ByteBufferMode.NORMAL);
    }

    /**
     * Measures the memory usage of the object including referenced objects.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param object the object to measure
     * @param bbMode the mode that should be used to measure ByteBuffers.
     * @return the memory usage of @param object including referenced objects
     */
    public long measureDeep(Object object, ByteBufferMode bbMode) {

        if (object == null) {
            return 0L;
        }

        if (classFilter.ignore(object.getClass()))
            return 0;

        MemoryMeterListener listener = listenerFactory.newInstance();

        // track stack manually, so we can handle deeper hierarchies than recursion
        MeasurementStack stack = new MeasurementStack(classFilter, listener);
        stack.pushRoot(object);

        long total = 0;
        while (!stack.isEmpty()) {

            Object current = stack.pop();

            // Deal with optimizations first.
            if (StringMeter.ENABLED && current instanceof String) {
                String s = (String) current;
                long size1 = measureDeep(s, listener);
                total += size1;
                continue;
            }
 
            if (current instanceof Measurable) {
                Measurable measurable = (Measurable) current;
                total += measure(measurable, listener);
                measurable.addChildrenTo(stack);
                continue;
            }

            long size = strategy.measure(current);
            listener.objectMeasured(current, size);
            total += size;

            Class<?> cls = current.getClass();

            if (cls.isArray()) {
                if (!cls.getComponentType().isPrimitive())
                    addArrayElements((Object[]) current, stack);
             } else {
                if (current instanceof ByteBuffer && bbMode.isSlab((ByteBuffer) current)) {
                    ByteBuffer buffer = (ByteBuffer) current;
                    if (!buffer.isDirect()) { // If direct we should simply not measure the fields
                        long remaining = buffer.remaining();
                        listener.byteBufferRemainingMeasured(buffer, remaining);
                        total += remaining;
                    }
                    continue;
                }
                addFields(current, cls, stack);
            }
        } 
        listener.done(total);
        return total;
    }

    private long measureDeep(String s, MemoryMeterListener listener) {
        long size = STRING_METER.measureDeep(strategy, s);
        listener.objectMeasured(s, size);
        return size;
    }

    private long measure(Measurable measurable, MemoryMeterListener listener) {
        long size = measurable.shallowSize(strategy);
        listener.objectMeasured(measurable, size);
        return size;
    }

    private void addFields(Object obj, Class<?> cls, MeasurementStack stack) {
        Class<?> type = cls;
        while (type != null) {
            addDeclaredFields(obj, type, stack);
            type = type.getSuperclass();
        }
    }

    private void addDeclaredFields(Object obj, Class<?> type, MeasurementStack stack) {
        for (Field field : type.getDeclaredFields()) {
            if (!fieldFilter.ignore(obj.getClass(), field)) {
                addField(obj, field, stack);
            }
        }
    }

    /**
     * Adds the object field value to the stack.
     *
     * @param obj the object from which the field value must be retrieved
     * @param field the field
     * @param stack
     */
    private void addField(Object obj, Field field, MeasurementStack stack) {
        Object child = getFieldValue(obj, field, stack.listener());

        if (child != null && (!classFilter.ignore(child.getClass()))) {
            stack.pushObject(obj, field.getName(), child);
        }
    }

    /**
     * Retrieves the field value if possible.
     *
     * @param obj the object for which the field value must be retrieved
     * @param field the field for which the value must be retrieved
     * @param listener the {@code MemoryMeterListener}
     * @return the field value if it was possible to retrieve it
     * @throws CannotAccessFieldException if the field could not be accessed
     */
    private Object getFieldValue(Object obj, Field field, MemoryMeterListener listener) {
        try {
            return ACCESSOR.getFieldValue(obj, field);
        } catch (CannotAccessFieldException e) {
            listener.failedToAccessField(obj, field.getName(), field.getType());
            throw e;
        }
    }

    private void addArrayElements(Object[] array, MeasurementStack stack) {
        for (int i = 0; i < array.length; i++) {
            stack.pushArrayElement(array, i);
        }
    }

    /**
     * Builder for {@code MemoryMeter} instances
     */
    public static final class Builder {

        /**
         * The strategy to perform shallow measurements and its fallback strategies in case the required classes are not available. 
         */
        private List<Guess> guesses = BEST;
        private boolean ignoreOuterClassReference;
        private boolean ignoreKnownSingletons = true;
        private boolean ignoreNonStrongReferences = true;
        private MemoryMeterListener.Factory listenerFactory = NoopMemoryMeterListener.FACTORY;

        private Builder() {

        }

        public MemoryMeter build() {
            return new MemoryMeter(this);
        }

        /**
         * Specify what should be the strategy used to measure the shallow size of object.
         *
         * @param strategy the strategy that should be used to measure objects
         * @param fallbacks the fallback strategies
         * @return this builder
         */
        public Builder withGuessing(Guess strategy, Guess... fallbacks) {

            if (strategy == null)
                throw new IllegalArgumentException("The strategy parameter should not be null");

            List<Guess> guesseList = new ArrayList<>();
            guesseList.add(strategy);
            for (Guess guess : fallbacks)
                guesseList.add(guess);

            Guess.checkOrder(guesseList);

            this.guesses = guesseList;
            return this;
        }

        /**
         * Ignores the outer class reference from non-static inner classes.
         * <p>In practice this is only useful if the top class provided to {@code MemoryMeter.measureDeep} is an inner 
         * class and we wish to ignore the outer class in the measurement.</p>
         *
         * @return this builder
         */
        public Builder ignoreOuterClassReference() {
            this.ignoreOuterClassReference = true;
            return this;
        }

        /**
         * Measures the space occupied by known singletons such as {@link Class} objects, {@code enum}s, {@code ClassLoader}s and
         * {@code AccessControlContext}s. By default {@code MemoryMeter} will ignore those.
         *
         * @return this builder
         */
        public Builder measureKnownSingletons() {
            this.ignoreKnownSingletons = false;
            return this;
        }

        /**
         * Measures the references from a {@link java.lang.ref.Reference} (like weak/soft/phantom references).
         * By default {@code MemoryMeter} will ignore those.
         *
         * @return this builder
         */
        public Builder measureNonStrongReferences() {
            ignoreNonStrongReferences = false;
            return this;
        }

        /**
         * Prints the classes tree to {@code System.out} when measuring through {@code measureDeep}.
         *
         * @return this builder
         */
        public Builder printVisitedTree() {
            return printVisitedTreeUpTo(Integer.MAX_VALUE);
        }

        /**
         * Prints the classes tree to {@code System.out} up to the specified depth when measuring through {@code measureDeep}.
         *
         * @param depth the depth up to which the class tree must be printed
         * @return this builder
         */
        public Builder printVisitedTreeUpTo(int depth) {
            if (depth <= 0)
                throw new IllegalArgumentException(String.format("the depth must be greater than zero (was %s).", depth));

            listenerFactory = new TreePrinter.Factory(depth);
            return this;
        }
    }
}