package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import org.github.jamm.MemoryMeterListener.Factory;
import org.github.jamm.strategies.MemoryMeterStrategies;

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

    public enum Guess {
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

    /**
     * The accessor used to retrieve field values.
     *
     * <p>For JDK prior to Java 9, {@code MemoryMeter} will use plain reflection. From Java 9 onward {@code MemoryMeter}
     * will use reflection if the object is within an accessible module otherwise it will rely on Unsafe to access the field value.</p>
     */
    private final FieldAccessor accessor;
    
    /**
     * Filter used to determine which classes should be ignored.
     */
    private final FieldAndClassFilter classFilter;

    /**
     * Filter used to determine which field should be ignored.
     */
    private final FieldFilter fieldFilters;

    private final boolean omitSharedBufferOverhead;

    /**
     * The factory used to create the listener listening to the object graph traversal.
     */
    private final MemoryMeterListener.Factory listenerFactory;

    private MemoryMeter(Builder builder) {

        this(MemoryMeterStrategies.getInstance().getStrategy(builder.guess),
             Filters.getClassFilters(builder.ignoreKnownSingletons),
             Filters.getFieldFilters(builder.ignoreKnownSingletons, builder.ignoreOuterClassReference, builder.ignoreNonStrongReferences),
             builder.omitSharedBufferOverhead,
             builder.listenerFactory);
    }

    /**
     * Create a new {@link MemoryMeter} instance from the different component it needs to measure object graph.
     * <p>Unless there is a specific need to override some of the {@code MemoryMeter} logic people should only create 
     * {@MemoryMeter} instances through {@code MemoryMeter.builder()}. This constructor provides a way to modify part of the 
     * logic being used by allowing to use specific implementations for the strategy or filters.</p>
     * 
     * @param strategy the {@code MemoryMeterStrategy} to use for measuring object shallow size.
     * @param classFilter the filter used to filter out classes from the measured object graph
     * @param fieldFilter the filter used to filter out fields from the measured object graph
     * @param omitSharedBufferOverhead
     * @param listenerFactory the factory used to create the listener listening to the object graph traversal
     */
    public MemoryMeter(MemoryMeterStrategy strategy,
                       FieldAndClassFilter classFilter,
                       FieldFilter fieldFilter,
                       boolean omitSharedBufferOverhead,
                       MemoryMeterListener.Factory listenerFactory) {

        this.strategy = strategy;
        this.accessor = FieldAccessor.getInstance();
        this.classFilter = classFilter;
        this.fieldFilters = fieldFilter;
        this.omitSharedBufferOverhead = omitSharedBufferOverhead;
        this.listenerFactory = listenerFactory;
    }

    public static Builder builder()
    {
        return new Builder();
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
     * Measures the memory usage of the object including referenced objects.
     *
     * <p>If the object is {@code null} the value returned will be zero.</p>
     *
     * @param object the object to measure
     * @return the memory usage of @param object including referenced objects
     */
    public long measureDeep(Object object) {

        if (object == null) {
            return 0L;
        }

        if (classFilter.ignore(object.getClass()))
            return 0;

        IdentityHashSet tracker = new IdentityHashSet();
        MemoryMeterListener listener = listenerFactory.newInstance();

        tracker.add(object);
        listener.started(object);

        // track stack manually, so we can handle deeper hierarchies than recursion
        Deque<Object> stack = new ArrayDeque<Object>();
        stack.push(object);

        long total = 0;
        while (!stack.isEmpty()) {

            Object current = stack.pop();
            long size = strategy.measure(current);
            listener.objectMeasured(current, size);
            total += size;

            Class<?> cls = current.getClass();

            if (cls.isArray()) {

                if (!cls.getComponentType().isPrimitive())
                    addArrayChildren((Object[]) current, stack, tracker, listener);

            } else {

                if (current instanceof ByteBuffer && omitSharedBufferOverhead) {

                    ByteBuffer buffer = (ByteBuffer) current;

                    if (!buffer.isDirect()) {

                        int arrayLength = buffer.capacity();
                        int bufferLength = buffer.remaining();

                        // if we're only referencing a sub-portion of the ByteBuffer, we do not count the array overhead as we assume that it is SLAB
                        // allocated - the overhead amortized over all the allocations is negligible and better to undercount than over count.
                        if (arrayLength > bufferLength) {
                            total += bufferLength;
                            listener.byteBufferRemainingMeasured(buffer, bufferLength);
                            continue;
                        }
                    }
                }
                addFieldChildren(current, cls, stack, tracker, listener);
            }
        } 
        listener.done(total);
        return total;
    }

    private void addFieldChildren(Object obj, Class<?> cls, Deque<Object> stack, IdentityHashSet tracker, MemoryMeterListener listener) {
        Class<?> type = cls;
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {

                if (fieldFilters.ignore(cls, field)) {
                    continue;
                }

                Object child = getFieldValue(obj, field, listener);

                if (omitSharedBufferOverhead && isDirectBufferView(obj, field, child)) {
                    continue;
                }

                if (child != null && tracker.add(child)) {
                    stack.push(child);
                    listener.fieldAdded(obj, field.getName(), child);
                }
            }

            type = type.getSuperclass();
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
    private Object getFieldValue(Object obj, Field field, MemoryMeterListener listener)
    {
        try {
            return accessor.getFieldValue(obj, field);
        } catch (CannotAccessFieldException e) {
            listener.failedToAccessField(obj, field.getName(), field.getType());
            throw e;
        }
    }

    /**
     * Checks if the object is a direct {@code ByteBuffer} which is the view of another buffer.
     *
     * <p>When a {@code DirectByteBuffer} is a view of another buffer, it uses the {@code att} field 
     * to keep a reference to that buffer.
     * 
     * @param obj The object to check
     * @param field the field
     * @param child the field value
     * @return {@code true} if the object is a direct {@code ByteBuffer} which is the view of another buffer,
     * {@code false} otherwise.
     */
    private boolean isDirectBufferView(Object obj, Field field, Object child)
    {
        if (!(obj instanceof ByteBuffer))
            return false;

        ByteBuffer byteBuffer = (ByteBuffer) obj;

        if (!byteBuffer.isDirect())
            return false;

        // Read-only buffer might actually be the only representation of a ByteBuffer so we cannot simply consider them as shared.
        // Pre java 12, a DirectByteBuffer created from another DirectByteBuffer was using the source buffer as an attachment 
        // for liveness rather than the source buffer's attachment (https://bugs.openjdk.org/browse/JDK-8208362). 
        // Therefore prior to Java 12 it was easy to determine which part was shared but that approach did not work anymore
        // since Java 12 so we have to rely on the usage to guess if it is shared or not
        if (byteBuffer.isReadOnly())
            return VM.isPreJava12JVM() ? false
                                       : byteBuffer.remaining() < byteBuffer.capacity();

        return  field.getName().equals("att") 
                && child != null;
    }

    private void addArrayChildren(Object[] current, Deque<Object> stack, IdentityHashSet tracker, MemoryMeterListener listener) {
        for (int i = 0; i < current.length; i++) {
            Object child = current[i];
            if (child != null && !classFilter.ignore(child.getClass()) && tracker.add(child)) {

                stack.push(child);
                listener.arrayElementAdded(current, i , child);
            }
        }
    }

    /**
     * Builder for {@code MemoryMeter} instances
     */
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
         * Ignores space occupied by known singletons such as {@link Class} objects and {@code enum}s
         *
         * @return this builder
         */
        public Builder ignoreKnownSingletons() {
            this.ignoreKnownSingletons = true;
            return this;
        }

        /**
         * Ignores the references from a {@link java.lang.ref.Reference} (like weak/soft/phantom references).
         *
         * @return this builder
         */
        public Builder ignoreNonStrongReferences() {
            ignoreNonStrongReferences = true;
            return this;
        }

        /**
         * Counts only the bytes remaining in a {@code ByteBuffer} in measureDeep if the buffer only reference a
         * sub-portion of the backing array.
         *
         * <p>This option is to handle SLAB allocated {@code ByteBuffer} where the overhead amortized over all
         * the allocations is negligible and we prefer to under count than over count. </p>
         *
         * @return this builder
         */
        public Builder omitSharedBufferOverhead() {
            omitSharedBufferOverhead = true;
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
