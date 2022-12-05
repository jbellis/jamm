package org.github.jamm.strategies;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.github.jamm.MemoryLayoutSpecification;

import sun.misc.Unsafe;

/**
 * {@code MemoryMeterStrategy} relying on {@code Unsafe} to measure object sizes.
 *
 */
public final class UnsafeStrategy extends MemoryLayoutBasedStrategy
{
    private final Unsafe unsafe;

    public UnsafeStrategy(MemoryLayoutSpecification memoryLayout, Unsafe unsafe)
    {
        super(memoryLayout);
        this.unsafe = unsafe;
    }

    // attemps to use sun.misc.Unsafe to find the maximum object offset, this work around helps deal with long alignment
    public long measureInstance(Class<?> type) {
        while (type != null)
        {
            long size = 0;

            for (Field f : type.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()))
                    size = Math.max(size, unsafe.objectFieldOffset(f) + measureField(f.getType()));
            }

            if (size > 0)
                return roundTo(size, memoryLayout.getObjectPadding());

            type = type.getSuperclass();
        }
        return roundTo(memoryLayout.getObjectHeaderSize(), memoryLayout.getObjectPadding());
    }
}
