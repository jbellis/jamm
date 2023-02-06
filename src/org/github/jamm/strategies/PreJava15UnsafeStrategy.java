package org.github.jamm.strategies;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.github.jamm.MemoryLayoutSpecification;

import sun.misc.Unsafe;

import static org.github.jamm.MathUtils.roundTo;

/**
 * {@code MemoryMeterStrategy} relying on {@code Unsafe} to measure object sizes for Java version pre-15.
 *
 * <p>In Java 15, the way the JVM layout fields across the hierarchy changed. Prior to Java 15 superclass field
 * always came first. From Java 15 onward it is not the case anymore. This strategy take advantage of that and look at
 * the child class first to find the greatest offsets.</p>
 */
final class PreJava15UnsafeStrategy extends MemoryLayoutBasedStrategy
{
    private final Unsafe unsafe;

    public PreJava15UnsafeStrategy(MemoryLayoutSpecification memoryLayout, Unsafe unsafe)
    {
        super(memoryLayout);
        this.unsafe = unsafe;
    }

    @Override
    protected int arrayBaseOffset(Class<?> type)
    {
        return unsafe.arrayBaseOffset(type);
    }

    @Override
    public long measureInstance(Class<?> type) {
        while (type != null) {

            long size = 0;

            for (Field f : type.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()))
                    size = Math.max(size, unsafe.objectFieldOffset(f) + measureField(f.getType()));
            }

            // As we know that the superclass fields always come first pre-Java 15, if the size is greater than zero
            // we know that all the other fields will have a smaller offset.
            if (size > 0)
                return roundTo(size, memoryLayout.getObjectAlignment());

            type = type.getSuperclass();
        }
        return roundTo(memoryLayout.getObjectHeaderSize(), memoryLayout.getObjectAlignment());
    }
}
