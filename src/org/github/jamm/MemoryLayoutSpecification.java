package org.github.jamm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

public abstract class MemoryLayoutSpecification
{

    public static final MemoryLayoutSpecification SPEC = getEffectiveMemoryLayoutSpecification();

    public abstract int getArrayHeaderSize();

    public abstract int getObjectHeaderSize();

    public abstract int getObjectPadding();

    public abstract int getReferenceSize();

    public abstract int getSuperclassFieldPadding();


    /**
     * Describes constant memory overheads for various constructs in a JVM
     * implementation.
     */

    /**
     * Memory a class consumes, including the object header and the size of the fields.
     * @param fieldsSize Total size of the primitive fields of a class
     * @return Total in-memory size of the class
     */
    public static long getSizeOfFields(long fieldsSize)
    {
        return roundTo(SPEC.getObjectHeaderSize() + fieldsSize, SPEC.getObjectPadding());
    }

    /**
     * Memory a super class consumes, given the primitive field sizes
     * @param fieldsSize Total size of the primitive fields of the super class
     * @return Total additional in-memory that the super class takes up
     */
    public static long getSizeOfSuperClassFields(long fieldsSize)
    {
        return roundTo(fieldsSize, SPEC.getSuperclassFieldPadding());
    }

    /**
     * Memory an array will consume
     * @param length Number of elements in the array
     * @param elementSize In-memory size of each element's primitive stored
     * @return In-memory size of the array
     */
    public static long getSizeOfArray(int length, long elementSize)
    {
        return roundTo(SPEC.getArrayHeaderSize() + length * elementSize, SPEC.getObjectPadding());
    }

    /**
     * Memory a byte array consumes
     * @param bytes byte array to get memory size
     * @return In-memory size of the array
     */
    public static long getSizeOfArray(byte[] bytes)
    {
        return getSizeOfArray(bytes.length, 1);
    }

    public static int getSizeOf(Field field)
    {
        return getSizeOfField(field.getType());
    }

    public static int getSizeOfField(Class<?> type)
    {
        if (!type.isPrimitive())
            return SPEC.getReferenceSize();
        if (type == boolean.class || type == byte.class)
            return 1;
        else if (type == char.class || type == short.class)
            return 2;
        else if (type == float.class || type == int.class)
            return 4;
        else if (type == double.class || type == long.class)
            return 8;
        throw new IllegalStateException();
    }

    // TODO : this is very close to accurate, but occasionally yields a slightly incorrect answer
    public static long getSizeOfInstance(Class<?> type)
    {
        long size = SPEC.getObjectHeaderSize() + getSizeOfDeclaredFields(type);
        while ((type = type.getSuperclass()) != Object.class)
            size += roundTo(getSizeOfDeclaredFields(type), SPEC.getSuperclassFieldPadding());
        return roundTo(size, SPEC.getObjectPadding());
    }

    public static long getSizeOfArray(Class<?> type, Object instance)
    {
        return getSizeOfArray(Array.getLength(instance), getSizeOfField(type.getComponentType()));
    }

    private static long getSizeOfDeclaredFields(Class<?> type)
    {
        long size = 0;
        for (Field f : type.getDeclaredFields())
            size += getSizeOf(f);
        return size;
    }

    public static long roundTo(long x, int multiple)
    {
        return ((x + multiple - 1) / multiple) * multiple;
    }

    private static MemoryLayoutSpecification getEffectiveMemoryLayoutSpecification()
    {
        final String dataModel = System.getProperty("sun.arch.data.model");
        if ("32".equals(dataModel))
        {
            // Running with 32-bit data model
            return new MemoryLayoutSpecification()
            {
                public int getArrayHeaderSize()
                {
                    return 12;
                }

                public int getObjectHeaderSize()
                {
                    return 8;
                }

                public int getObjectPadding()
                {
                    return 8;
                }

                public int getReferenceSize()
                {
                    return 4;
                }

                public int getSuperclassFieldPadding()
                {
                    return 4;
                }
            };
        }

        final String strVmVersion = System.getProperty("java.vm.version");
        final int vmVersion = Integer.parseInt(strVmVersion.substring(0, strVmVersion.indexOf('.')));
        final int alignment = getAlignment();
        if (vmVersion >= 17)
        {
            long maxMemory = 0;
            for (MemoryPoolMXBean mp : ManagementFactory.getMemoryPoolMXBeans())
            {
                maxMemory += mp.getUsage().getMax();
            }
            if (maxMemory < 30L * 1024 * 1024 * 1024)
            {
                // HotSpot 17.0 and above use compressed OOPs below 30GB of RAM
                // total for all memory pools (yes, including code cache).
                return new MemoryLayoutSpecification()
                {
                    public int getArrayHeaderSize()
                    {
                        return 16;
                    }

                    public int getObjectHeaderSize()
                    {
                        return 12;
                    }

                    public int getObjectPadding()
                    {
                        return alignment;
                    }

                    public int getReferenceSize()
                    {
                        return 4;
                    }

                    public int getSuperclassFieldPadding()
                    {
                        return 4;
                    }
                };
            }
        }

        /* Worst case we over count. */

        // In other cases, it's a 64-bit uncompressed OOPs object model
        return new MemoryLayoutSpecification()
        {
            public int getArrayHeaderSize()
            {
                return 24;
            }

            public int getObjectHeaderSize()
            {
                return 16;
            }

            public int getObjectPadding()
            {
                return alignment;
            }

            public int getReferenceSize()
            {
                return 8;
            }

            public int getSuperclassFieldPadding()
            {
                return 8;
            }
        };
    }

    private static int getAlignment()
    {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        for (String arg : runtimeMxBean.getInputArguments())
        {
            if (arg.startsWith("-XX:ObjectAlignmentInBytes="))
            {
                try
                {
                    return Integer.parseInt(arg.substring("-XX:ObjectAlignmentInBytes=".length()));
                }
                catch (Exception _){}
            }
        }
        return 8;
    }


}
