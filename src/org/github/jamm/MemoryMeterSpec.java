package org.github.jamm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Inaccurate guessing. Does not consider any {@code @Contended} or proper, VM dependent field reordering, etc.
 * TL;DR the object sizes are likely incorrect, but hopefully rather too big (better safe than sorry).
 */
final class MemoryMeterSpec extends MemoryMeterRef
{
    private static boolean warned;

    MemoryMeterSpec(Builder builder)
    {
        super(builder, MemoryMeterSpec::sizeOf);
        maybeWarn();
    }

    private static void maybeWarn()
    {
        if (warned)
            return;
        warned = true;
        System.err.println("***********************************************************************************");
        System.err.println("** jamm will GUESS the size of objects on heap. This is wrong and");
        System.err.println("** results in wrong assumptions of the free/occupied Java heap and");
        System.err.println("** potentially in OOMs. The implementation does not always consider");
        System.err.println("** Java object layouts under all circumstances for all JVMs.");
        System.err.println("**");
        System.err.println("** Solutions:");
        System.err.println("** - Use a JDK/JVM with JEP-8249196");
        System.err.println("** - Load jamm as an agent into the JVM");
        System.err.println("***********************************************************************************");
    }

    private static long sizeOf(Class<?> type)
    {
        long size = sizeOf(SPEC.getObjectHeaderSize(), type);

        size = roundTo(size, SPEC.getObjectAlignment());

        return size;
    }

    private static long sizeOf(long size, Class<?> type)
    {
        Class<?> superclass = type.getSuperclass();
        if (superclass != Object.class && superclass != null)
            size = sizeOf(size, superclass);

        size = sizeOfDeclaredFields(size, type);

        return size;
    }


    private static long sizeOfDeclaredFields(long size, Class<?> type)
    {
        boolean any = false;
        for (Field f : type.getDeclaredFields())
        {
            if (!Modifier.isStatic(f.getModifiers()))
            {
                Class<?> t = f.getType();
                if (!any && (t == long.class || !t.isPrimitive() || t == double.class))
                {
                    any = true;
                    size = roundTo(size, SPEC.getObjectAlignment());
                }
                size += sizeOfField(t);
            }
        }
        return size;
    }
}
