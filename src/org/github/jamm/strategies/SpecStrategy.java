package org.github.jamm.strategies;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.github.jamm.MemoryLayoutSpecification;

public final class SpecStrategy extends MemoryLayoutBasedStrategy
{
    public SpecStrategy(MemoryLayoutSpecification memoryLayout)
    {
        super(memoryLayout);
    }

    // this is very close to accurate, but occasionally yields a slightly incorrect answer (when long fields are used
    // and cannot be 8-byte aligned, an extra 4-bytes is allocated.
    public long measureInstance(Class<?> type) {

        long size = memoryLayout.getObjectHeaderSize() + sizeOfDeclaredFields(type);

        while ((type = type.getSuperclass()) != Object.class && type != null)
            size += sizeOfDeclaredFields(type);

        return roundTo(size, memoryLayout.getObjectAlignment());
    }

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
