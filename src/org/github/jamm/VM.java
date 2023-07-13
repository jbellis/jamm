package org.github.jamm;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

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
    private static final int DEFAULT_ALIGNMENT_IN_BYTES = 8;
    private static final int DEFAULT_CONTENDED_PADDING_WIDTH = 128;

    private static final boolean IS_PRE_JAVA12_JVM = !supportStringIndentMethod();

    private static final Unsafe UNSAFE = loadUnsafe();

    /**
     * Checks if the JVM support the {@code String#indent} method added in Java 12.
     * @return {@code true} if the JVM support the {@code String#indent} method, {@code false} otherwise. 
     */
    private static boolean supportStringIndentMethod() {
        try {
            String.class.getMethod("indent", int.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

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

        String alignment = getVMOption("ObjectAlignmentInBytes");
        return alignment == null ? DEFAULT_ALIGNMENT_IN_BYTES : Integer.parseInt(alignment);
    }

    /**
     * Checks if the JVM uses compressed reference.
     *
     * @return {{@code true} if the JVM use compressed references {@code false} otherwise.
     */
    public static boolean useCompressedOops() {

        String useCompressedOops = getVMOption("UseCompressedOops");
        return useCompressedOops == null ? DEFAULT_USE_COMPRESSED_OOPS : Boolean.parseBoolean(useCompressedOops);
    }

    /**
     * Checks if the JVM uses compressed class pointers.
     *
     * @return {{@code true} if the JVM use compressed class pointers {@code false} otherwise.
     */
    public static boolean useCompressedClassPointers() {

        String useCompressedClassPointers = getVMOption("UseCompressedClassPointers");
        return useCompressedClassPointers == null ? useCompressedOops() : Boolean.parseBoolean(useCompressedClassPointers);
    }

    /**
     * Checks if the JVM uses more aggressive optimizations to avoid unused gaps in instances.
     *
     * @return {{@code true} if the JVM use empty slots in super class {@code false} otherwise.
     */
    public static boolean useEmptySlotsInSuper() {

        String useEmptySlotsInSuper = getVMOption("UseEmptySlotsInSupers");
        return useEmptySlotsInSuper == null ? false : Boolean.parseBoolean(useEmptySlotsInSuper);
    }

    /**
     * Checks if the JVM restricts the use of {@code @Contended} to internal classes.
     *
     * @return {{@code true} if the JVM restricts the use of {@code @Contended} to internal classes, {@code false} otherwise.
     */
    public static boolean restrictContended() {

        String restrictContended = getVMOption("RestrictContended");
        return restrictContended == null ? true : Boolean.parseBoolean(restrictContended);
    }

    /**
     * Checks if {@code @Contended} annotations are enabled.
     *
     * @return {{@code true} if {@code @Contended} annotations are enabled, {@code false} otherwise.
     */
    public static boolean enableContended() {

        String enableContended = getVMOption("EnableContended");
        return enableContended == null ? true : Boolean.parseBoolean(enableContended);
    }

    /**
     * Returns the number of bytes used to pad the fields/classes annotated with {@code Contended}.
     * <p>The value will be between 0 and 8192 (inclusive) and will be a multiple of 8.</p>
     *
     * @return the number of bytes used to pad the fields/classes annotated with {@code Contended}.
     */
    public static int contendedPaddingWidth() {

        String contendedPaddingWidth = getVMOption("ContendedPaddingWidth");
        return contendedPaddingWidth == null ? DEFAULT_CONTENDED_PADDING_WIDTH : Integer.parseInt(contendedPaddingWidth);
    }

    /**
     * Checks if the JVM is a 32 bits one.
     * @return {@code true} if the JVM is a 32 bits version, {@code false} otherwise.
     */
    public static boolean is32Bits() {
        return "32".equals(System.getProperty("sun.arch.data.model"));
    }

    /**
     * Checks if the JVM is a pre-Java 12 version.
     * @return {@code true} if the JVM is a pre-Java 12 version, {@code false} otherwise.
     */
    public static boolean isPreJava12JVM() {
        return IS_PRE_JAVA12_JVM;
    }

    /**
     * Checks if {@code Unsafe} is available.
     * @return {@code true} if unsafe is available, {@code false} otherwise.
     */
    public static boolean hasUnsafe() {
        return UNSAFE != null;
    }

    /**
     * Returns {@code Unsafe} if it is available.
     * @return {@code Unsafe} if it is available, {@code null} otherwise.
     */
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }

    /**
     * Utility method using {@code Unsafe} to print the field offset for debugging.
     *
     * @param obj the object to analyze
     */
    public static void printOffsets(Object obj) {
        if (UNSAFE == null)
            throw new IllegalStateException("printOffsets relies on Unsafe which could not be loaded");

        Class<?> type = obj.getClass();

        if (type.isArray()) {

            System.out.println("---------------------------------------------------------------------------------");
            System.out.println("Memory layout for: " + obj.getClass().getComponentType().getName() + "[]");
            System.out.println("arrayBaseOffset : " + UNSAFE.arrayBaseOffset(obj.getClass()));
            System.out.println("---------------------------------------------------------------------------------");

        } else {

            Map<Long, String> fieldInfo = new TreeMap<>();
            while (type != null) {
                for (Field f : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        long offset = UNSAFE.objectFieldOffset(f);
                        Class<?> fieldType = f.getType();
                        String fieldTypeAsString = fieldType.isArray() ? fieldType.getComponentType().getName() + "[]" : fieldType.getName();
                        fieldInfo.put(offset, "class=" + type.getName() + ", field=" + f.getName() + ", offset=" + offset + ", field type=" + fieldTypeAsString);
                    }
                }
                type = type.getSuperclass();
            }

            System.out.println("---------------------------------------------------------------------------------");
            System.out.println("Memory layout for: " + obj.getClass().getName());
            fieldInfo.forEach((k, v) -> System.out.println(v));
            System.out.println("---------------------------------------------------------------------------------");
        }
    }

    private static Unsafe loadUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (final Exception ex) {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return (Unsafe) field.get(null);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private VM() { 
    }
}
