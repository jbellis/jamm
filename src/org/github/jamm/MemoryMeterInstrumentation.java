package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.util.concurrent.ConcurrentHashMap;

final class MemoryMeterInstrumentation extends MemoryMeterBase
{
    static Instrumentation instrumentation;

    static boolean hasInstrumentation()
    {
        return instrumentation != null && !Boolean.getBoolean("jamm.no-instrumentation");
    }

    MemoryMeterInstrumentation(Builder builder)
    {
        super(builder);
    }

    // Cannot use java.lang.ClassValue here, because ClassValue.computeValue() only passes the
    // j.l.Class, but Instrumentation.getObjectSize() requires the actual j.l.Object.
    private final ConcurrentHashMap<Class<?>, Long> shallowClassCache = new ConcurrentHashMap<>();

    @Override
    long measureArray(Object obj, Class<?> type)
    {
        // Using
        //      return instrumentation.getObjectSize(obj);
        // would be correct, but Instrumentation.getObjectSize() is surprisingly slow.

        return sizeOfArray(Array.getLength(obj), type.getComponentType());
    }

    @Override
    long measureNonArray(Object obj, Class<?> type)
    {
        // Could be rewritten as
        //    return shallowClassCache.computeIfAbsent(type, x -> instrumentation.getObjectSize(obj));
        // but that would involve a capturing lambda, which is bad in this case.

        Long sz = shallowClassCache.get(type);
        if (sz != null)
            return sz;

        long szp = instrumentation.getObjectSize(obj);
        shallowClassCache.put(type, szp);
        return szp;
    }
}
