package org.github.jamm;

import java.lang.reflect.Array;
import java.util.function.ToLongFunction;

abstract class MemoryMeterRef extends MemoryMeterBase
{
    private final ClassValue<Long> classShallowSIze = new ClassValue<Long>()
    {
        @Override
        protected Long computeValue(Class<?> type)
        {
            return shallowClassSizeProvider.applyAsLong(type);
        }
    };
    private final ToLongFunction<Class<?>> shallowClassSizeProvider;

    MemoryMeterRef(Builder builder, ToLongFunction<Class<?>> shallowClassSizeProvider)
    {
        super(builder);
        this.shallowClassSizeProvider = shallowClassSizeProvider;
    }

    long measureArray(Object obj, Class<?> type)
    {
        return sizeOfArray(Array.getLength(obj), type.getComponentType());
    }

    long measureNonArray(Object obj, Class<?> type)
    {
        return classShallowSIze.get(type);
    }

}
