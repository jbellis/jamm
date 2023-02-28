package org.github.jamm;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;

import sun.misc.Unsafe;

/**
 * Utility class for retrieving information from the JVM.
 */
public final class VM
{
    private static final boolean DEFAULT_USE_COMPRESSED_OOPS = true;
    private static final int DEFAULT_ALIGNEMENT_IN_BYTES = 8;

    private static final Unsafe UNSAFE = loadUnsafe();

    /**
     * Returns the value of the specified VM option
     *
     * @param option the option name 
     * @return the value of the specified VM option or {@code null} if the value could not be retrieved.
     */
    private static String getVMOption(String option) {
        try {

            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName mbean = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
            CompositeDataSupport data = (CompositeDataSupport) server.invoke(mbean, "getVMOption", new Object[]{option}, new String[]{"java.lang.String"});
            return data.get("value").toString();

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieve the object alignment in bytes from the JVM. If the alignment cannot be retrieved
     * the default value will be returned.
     *
     * @return the object alignment in bytes if it can be retrieved or the default value (8).
     */
    public static int getObjectAlignmentInBytes() {

        String alignement = getVMOption("ObjectAlignmentInBytes");
        return alignement == null ? DEFAULT_ALIGNEMENT_IN_BYTES : Integer.parseInt(alignement);
    }

    /**
     * Checks if the JVM use compressed reference.
     *  
     * @return {{@code true} if the JVM use compressed references {@code false} otherwise.
     */
    public static boolean useCompressedOops() {

        String useCommpressedOops = getVMOption("UseCompressedOops");
        return useCommpressedOops == null ? DEFAULT_USE_COMPRESSED_OOPS : Boolean.parseBoolean(useCommpressedOops);
    }

    /**
     * Checks if the JVM use compressed class pointers.
     *  
     * @return {{@code true} if the JVM use compressed class pointers {@code false} otherwise.
     */
    public static boolean useCompressedClassPointers() {

        String useCompressedClassPointers = getVMOption("UseCompressedClassPointers");
        return useCompressedClassPointers == null ? useCompressedOops() : Boolean.parseBoolean(useCompressedClassPointers);
    }

    /**
     * Checks if the JVM is a 32 bits one.
     * @return {@code true} if the JVM is a 32 bits version, {@code false} otherwise.
     */
    public static boolean is32Bits()
    {
        return "32".equals(System.getProperty("sun.arch.data.model"));
    }

    /**
     * Returns {@code Unsafe} if it is available.
     * @return {@code Unsafe} if it is available, {@code null} otherwise.
     */
    public static Unsafe getUnsafe()
    {
        return UNSAFE;
    }

    /**
     * Utility method using {@code Unsafe} to print the field offset for debugging.
     *
     * @param obj the object to analyze
     */
    public static void printOffsets(Object obj)
    {
        Class<?> type = obj.getClass();
        while (type != null)
        {
            for (Field f : type.getDeclaredFields())
            {
                if (!Modifier.isStatic(f.getModifiers()))
                {
                    System.out.println("field=" + f.getName() 
                                        + ", offset=" + UNSAFE.objectFieldOffset(f)
                                        + ", type=" + f.getType());
                }
            }

            type = type.getSuperclass();
        }
    }

    private static Unsafe loadUnsafe()
    {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private VM() { 
    }
}
