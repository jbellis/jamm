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
        return type.isArray() ? measureArray(object, type)
                              : measureInstance(type);
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
        return roundTo(arrayBaseOffset(type) + length * (long) elementSize, memoryLayout.getObjectAlignment());
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
