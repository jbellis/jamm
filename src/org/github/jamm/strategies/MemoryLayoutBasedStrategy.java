package org.github.jamm.strategies;

import java.lang.reflect.Array;

import org.github.jamm.MemoryLayoutSpecification;
import org.github.jamm.MemoryMeterStrategy;

import static org.github.jamm.MathUtils.roundTo;

/**
 * Base class for strategies that need access to the {@code MemoryLayoutSpecification} for computing object size.
 */
public abstract class MemoryLayoutBasedStrategy implements MemoryMeterStrategy
{
    /**
     * The memory layout to use when computing object sizes.
     */
    protected final MemoryLayoutSpecification memoryLayout;

    public MemoryLayoutBasedStrategy(MemoryLayoutSpecification memoryLayout)
    {
        this.memoryLayout = memoryLayout;
    }

    @Override
    public final long measure(Object object)
    {
        Class<?> type = object.getClass();
         return type.isArray() ? measureArray(object, type) : measureInstance(type);
    }

    @Override
    public long measureArray(Object[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, memoryLayout.getReferenceSize());
    }

    @Override
    public long measureArray(byte[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, Byte.BYTES);
    }

    @Override
    public long measureArray(boolean[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, 1);
    }

    @Override
    public long measureArray(short[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, Short.BYTES);
    }

    @Override
    public long measureArray(char[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, Character.BYTES);
    }

    @Override
    public long measureArray(int[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, Integer.BYTES);
    }

    @Override
    public long measureArray(float[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, Float.BYTES);
    }

    @Override
    public long measureArray(long[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, Long.BYTES);
    }

    @Override
    public long measureArray(double[] array) {
        return computeArraySize(arrayBaseOffset(array.getClass()) , array.length, Double.BYTES);
    }

    @Override
    public long measureString(String s) {
        return measure(s);
    }

    /**
     * Measures the shallow memory used by objects of the specified class.
     *
     * @param type the object type
     * @return the shallow memory used by the object
     */
    protected abstract long measureInstance(Class<?> type);

    /**
     * Measure the shallow memory used by the specified array.
     *
     * @param instance the array instance
     * @param type the array type
     * @return the shallow memory used by the specified array
     */
    protected final long measureArray(Object instance, Class<?> type) {
        int length = Array.getLength(instance);
        int elementSize = measureField(type.getComponentType());
        return computeArraySize(arrayBaseOffset(type), length, elementSize);
    }

    /**
     * Computes the size of an array from its base offset, length and elementSize.
     *
     * @param arrayBaseOffset the array base offset
     * @param length the array length
     * @param elementSize the size of the array elements
     * @return the size of the array
     */
    private final long computeArraySize(int arrayBaseOffset, int length, int elementSize) {
        return roundTo(arrayBaseOffset + length * (long) elementSize, memoryLayout.getObjectAlignment());
    }

    /**
     * Returns the array base offset.
     * <p>Array base is aligned based on heap word. It is not visible by default as compressed references are used and the
     * header size is 16 but becomes visible when they are disabled. 
     *
     * @param type the array type
     * @return the array base offset.
     */
    protected abstract int arrayBaseOffset(Class<?> type);

    /**
     * @return The memory size of a field of a class of the provided type; for Objects this is the size of the reference only
     */
    protected final int measureField(Class<?> type) {

        if (!type.isPrimitive())
            return memoryLayout.getReferenceSize();

        if (type == boolean.class || type == byte.class)
            return 1;

        if (type == char.class || type == short.class)
            return 2;

        if (type == float.class || type == int.class)
            return 4;

        if (type == double.class || type == long.class)
            return 8;

        throw new IllegalStateException();
    }
}
