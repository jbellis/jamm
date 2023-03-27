package org.github.jamm.strategies;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.github.jamm.MemoryLayoutSpecification;

import static org.github.jamm.MathUtils.roundTo;

/**
 * {@code MemoryMeterStrategy} that computes the size of the memory occupied by an object, in a Java 15+ JVM, based on 
 * the JVM information.
 * <p>In Java 15 the Field layout computation was optimized (https://bugs.openjdk.org/browse/JDK-8237767) to eliminate 
 * a certain amount of the inefficiency from the previous versions (see {@link PreJava15SpecStrategy}).</p>
 */
class SpecStrategy extends MemoryLayoutBasedStrategy
{
    public SpecStrategy(MemoryLayoutSpecification memoryLayout)
    {
        super(memoryLayout);
    }

    @Override
    public final long measureInstance(Class<?> type) {

        long size = memoryLayout.getObjectHeaderSize() + sizeOfDeclaredFields(type);
        while ((type = type.getSuperclass()) != Object.class && type != null)
            size += sizeOfDeclaredFields(type);

        return roundTo(size, memoryLayout.getObjectAlignment());
    }

    @Override
    protected int arrayBaseOffset(Class<?> type)
    {
        return memoryLayout.getArrayHeaderSize();
    }

    /**
     * Returns the size of the declared fields of the specified class.
     *
     * @param type the class for which the size of its declared fields must be returned
     * @return the size of the declared fields of the specified class
     */
    private long sizeOfDeclaredFields(Class<?> type) {

        long size = 0;
        for (Field f : type.getDeclaredFields())
        {
            if (!Modifier.isStatic(f.getModifiers()))
                size += measureField(f.getType());
        }
        return size;
    }
}
