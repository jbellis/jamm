package org.github.jamm.strategies;

import java.lang.reflect.Array;

import org.github.jamm.MemoryLayoutSpecification;
import org.github.jamm.MemoryMeterStrategy;

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
        return type.isArray() ? measureArray(object, type)
                              : measureInstance(type);
    }

    protected abstract long measureInstance(Class<?> type);

    /**
     * Measure the memory that an array will consume
     *
     * @param instance the array instance
     * @param type the array type
     * @return In-memory size of the array
     */
    protected final long measureArray(Object instance, Class<?> type) {
        int length = Array.getLength(instance);
        int elementSize = measureField(type.getComponentType());
        return roundTo(memoryLayout.getArrayHeaderSize() + length * (long) elementSize, memoryLayout.getObjectAlignment());
    }

    /**
     * @return The memory size of a field of a class of the provided type; for Objects this is the size of the reference only
     */
    protected int measureField(Class<?> type) {

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

    /**
     * Rounds x up to the next multiple of m.
     *
     * @param x the number to round
     * @param m the multiple (must be a power of 2)
     * @return the rounded value of x up to the next multiple of m.
     */
    protected static long roundTo(long x, int m) {
        return (x + m - 1) & -m;
    }
}
