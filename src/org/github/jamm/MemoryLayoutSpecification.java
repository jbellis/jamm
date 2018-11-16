package org.github.jamm;

import sun.misc.Unsafe;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public abstract class MemoryLayoutSpecification
{

    static final Unsafe unsafe;
    static
    {
        Unsafe tryGetUnsafe;
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            tryGetUnsafe = (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            tryGetUnsafe = null;
        }
        unsafe = tryGetUnsafe;
    }

    public static final MemoryLayoutSpecification SPEC = getEffectiveMemoryLayoutSpecification();

    public abstract int getArrayHeaderSize();

    public abstract int getObjectHeaderSize();

    public abstract int getObjectPadding();

    public abstract int getReferenceSize();

    public abstract int getSuperclassFieldPadding();

    /* Indicates if UNSAFE object size determination is available */
    public static boolean hasUnsafe() {
        return unsafe != null;
    }

    /** @return sizeOfField(field.getType()) */
    public static int sizeOf(Field field) {
        return sizeOfField(field.getType());
    }

    /**
     * @return The memory size of a field of a class of the provided type; for Objects this is the size of the reference only
     */
    public static int sizeOfField(Class<?> type) {
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

    /**
     * @return The size of the provided instance as defined by the detected MemoryLayoutSpecification. For an array this
     * is dependent on the size of the array, but for an object this is fixed for all instances
     */
    public static long sizeOf(Object obj) {
        Class<?> type = obj.getClass();
        if (type.isArray())
            return sizeOfArray(obj, type);
        return sizeOfInstance(type);
    }

    /**
     * @return this allocated heap size of the instance provided; for arrays this is equivalent to sizeOf(obj),
     * which uses the memory layout specification, however for objects this method uses
     */
    public static long sizeOfWithUnsafe(Object obj) {
        Class<?> type = obj.getClass();
        if (type.isArray())
            return sizeOfArray(obj, type);
        return sizeOfInstanceWithUnsafe(type);
    }

    // this is very close to accurate, but occasionally yields a slightly incorrect answer (when long fields are used
    // and cannot be 8-byte aligned, an extra 4-bytes is allocated.
    // sizeOfInstanceWithUnsafe is safe against this miscounting
    public static long sizeOfInstance(Class<?> type) {
        long size = SPEC.getObjectHeaderSize() + sizeOfDeclaredFields(type);
        while ((type = type.getSuperclass()) != Object.class && type != null)
            size += roundTo(sizeOfDeclaredFields(type), SPEC.getSuperclassFieldPadding());
        return roundTo(size, SPEC.getObjectPadding());
    }

    // attemps to use sun.misc.Unsafe to find the maximum object offset, this work around helps deal with long alignment
    public static long sizeOfInstanceWithUnsafe(Class<?> type) {
        while (type != null)
        {
            long size = 0;
            for (Field f : declaredFieldsOf(type))
                size = Math.max(size, unsafe.objectFieldOffset(f) + sizeOf(f));
            if (size > 0)
                return roundTo(size, SPEC.getObjectPadding());
            type = type.getSuperclass();
        }
        return roundTo(SPEC.getObjectHeaderSize(), SPEC.getObjectPadding());
    }

    public static long sizeOfArray(Object instance, Class<?> type) {
        return sizeOfArray(Array.getLength(instance), sizeOfField(type.getComponentType()));
    }

    /**
     * Memory an array
     * @param length Number of elements in the array
     * @param type the array class type
     * @return In-memory size of the array
     */
    public static long sizeOfArray(int length, Class<?> type) {
        return sizeOfArray(length, sizeOfField(type.getComponentType()));
    }

    /**
     * Memory an array will consume
     * @param length Number of elements in the array
     * @param elementSize In-memory size of each element's primitive stored
     * @return In-memory size of the array
     */
    public static long sizeOfArray(int length, long elementSize) {
        return roundTo(SPEC.getArrayHeaderSize() + length * elementSize, SPEC.getObjectPadding());
    }

    private static long sizeOfDeclaredFields(Class<?> type) {
        long size = 0;
        for (Field f : declaredFieldsOf(type))
            size += sizeOf(f);
        return size;
    }

    private static Iterable<Field> declaredFieldsOf(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        for (Field f : type.getDeclaredFields())
        {
            if (!Modifier.isStatic(f.getModifiers()))
                fields.add(f);
        }
        return fields;
    }

    private static long roundTo(long x, int multiple) {
        return ((x + multiple - 1) / multiple) * multiple;
    }

    private static MemoryLayoutSpecification getEffectiveMemoryLayoutSpecification() {

        final String dataModel = System.getProperty("sun.arch.data.model");
        if ("32".equals(dataModel)) {
            // Running with 32-bit data model
            return new MemoryLayoutSpecification() {
                public int getArrayHeaderSize() {
                    return 12;
                }

                public int getObjectHeaderSize() {
                    return 8;
                }

                public int getObjectPadding() {
                    return 8;
                }

                public int getReferenceSize() {
                    return 4;
                }

                public int getSuperclassFieldPadding() {
                    return 4;
                }
            };
        }

        boolean modernJvm = true;

        final String strSpecVersion = System.getProperty("java.specification.version");
        final boolean hasDot = strSpecVersion.indexOf('.') != -1;
        if (hasDot) {
            if ("1".equals(strSpecVersion.substring(0, strSpecVersion.indexOf('.')))) {
                // Java 1.6, 1.7, 1.8
                final String strVmVersion = System.getProperty("java.vm.version");
                if (strVmVersion.startsWith("openj9"))
                {
                    modernJvm = true;
                }
                else
                {
                    final int vmVersion = Integer.parseInt(strVmVersion.substring(0, strVmVersion.indexOf('.')));
                    modernJvm = vmVersion >= 17;
                }
            }
        }

        final int alignment = getAlignment();
        if (modernJvm) {

            long maxMemory = 0;
            for (MemoryPoolMXBean mp : ManagementFactory.getMemoryPoolMXBeans()) {
                maxMemory += mp.getUsage().getMax();
            }

            if (maxMemory < 30L * 1024 * 1024 * 1024) {
                // HotSpot 17.0 and above use compressed OOPs below 30GB of RAM
                // total for all memory pools (yes, including code cache).
                return new MemoryLayoutSpecification() {

                    public int getArrayHeaderSize() {
                        return 16;
                    }

                    public int getObjectHeaderSize() {
                        return 12;
                    }

                    public int getObjectPadding() {
                        return alignment;
                    }

                    public int getReferenceSize() {
                        return 4;
                    }

                    public int getSuperclassFieldPadding() {
                        return 4;
                    }
                };
            }
        }

        /* Worst case we over count. */

        // In other cases, it's a 64-bit uncompressed OOPs object model
        return new MemoryLayoutSpecification() {

            public int getArrayHeaderSize() {
                return 24;
            }

            public int getObjectHeaderSize() {
                return 16;
            }

            public int getObjectPadding() {
                return alignment;
            }

            public int getReferenceSize() {
                return 8;
            }

            public int getSuperclassFieldPadding() {
                return 8;
            }
        };
    }

    // check if we have a non-standard object alignment we need to round to
    private static int getAlignment() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        for (String arg : runtimeMxBean.getInputArguments()) {
            if (arg.startsWith("-XX:ObjectAlignmentInBytes=")) {
                try {
                    return Integer.parseInt(arg.substring("-XX:ObjectAlignmentInBytes=".length()));
                } catch (Exception e){}
            }
        }
        return 8;
    }


}
