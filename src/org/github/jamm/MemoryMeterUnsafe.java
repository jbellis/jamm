package org.github.jamm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import sun.misc.Unsafe;

final class MemoryMeterUnsafe extends MemoryMeterRef
{
    private static boolean warned;

    private static final Unsafe unsafe;

    static
    {
        Unsafe tryGetUnsafe;
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            tryGetUnsafe = (Unsafe) field.get(null);
        }
        catch (Exception e)
        {
            tryGetUnsafe = null;
        }
        unsafe = tryGetUnsafe;
    }

    static boolean hasUnsafe()
    {
        return unsafe != null && !Boolean.getBoolean("jamm.no-unsafe");
    }

    MemoryMeterUnsafe(Builder builder)
    {
        super(builder, MemoryMeterUnsafe::sizeOf);
        maybeWarn();
    }

    private static void maybeWarn()
    {
        if (warned)
            return;
        warned = true;
        System.err.println("***********************************************************************************");
        System.err.println("** jamm will use sun.misc.Unsafe to compute the size of objects on heap.");
        System.err.println("** This is not safe and results in wrong assumptions of the free/occupied Java");
        System.err.println("** heap and potentially in OOMs. The implementation performs arithmetics on the");
        System.err.println("**  \"cookies\" returned by Unsafe.objectFieldOffset(), althought the Javadoc says:");
        System.err.println("** \"Do not expect to perform any sort of arithmetic on this offset; ");
        System.err.println("** it is just a cookie which is passed to the unsafe heap memory accessors.\"");
        System.err.println("** The implementation does not always consider Java object layouts in under");
        System.err.println("** all circumstances for all JVMs.");
        System.err.println("**");
        System.err.println("** Solutions:");
        System.err.println("** - Use a JDK/JVM with JEP-8249196");
        System.err.println("** - Load jamm as an agent into the JVM");
        System.err.println("***********************************************************************************");
    }

    private static long sizeOf(Class<?> type)
    {
        while (type != null)
        {
            long size = 0;

            for (Field f : type.getDeclaredFields())
                if (!Modifier.isStatic(f.getModifiers()))
                    size = Math.max(size, unsafe.objectFieldOffset(f) + sizeOfField(f.getType()));

            if (size > 0)
                return roundTo(size, SPEC.getObjectAlignment());
            type = type.getSuperclass();
        }

        return roundTo(SPEC.getObjectHeaderSize(), SPEC.getObjectAlignment());
    }
}
