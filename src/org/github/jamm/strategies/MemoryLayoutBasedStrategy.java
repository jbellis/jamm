package org.github.jamm.strategies;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Predicate;

import org.github.jamm.CannotMeasureObjectException;
import org.github.jamm.MemoryLayoutSpecification;
import org.github.jamm.MemoryMeterStrategy;
import org.github.jamm.VM;

import static org.github.jamm.MathUtils.roundTo;

import static org.github.jamm.strategies.MethodHandleUtils.mayBeMethodHandle;

/**
 * Base class for strategies that need access to the {@code MemoryLayoutSpecification} for computing object size.
 */
public abstract class MemoryLayoutBasedStrategy implements MemoryMeterStrategy
{
    /**
     * {@code true} if contended is enabled, {@code false} otherwise.
     */
    private static final boolean CONTENDED_ENABLED = VM.enableContended();

    /**
     * {@code true} if contended is restricted, {@code false} otherwise.
     */
    private static final boolean CONTENDED_RESTRICTED = VM.restrictContended();

    /**
     * The predicate used to check if a ClassLoader is a platform one.
     */
    private static final Predicate<ClassLoader> PLATFORM_PREDICATE = platformClassLoaderPredicate();

    /**
     * The memory layout to use when computing object sizes.
     */
    protected final MemoryLayoutSpecification memoryLayout;

    /**
     * The {@code Contended} annotation class.
     */
    protected final Class<? extends Annotation> contendedClass;

    /**
     * The {@code MethodHandle} used to invoke the value method from {@code @Contended} if it is accessible.
     */
    private final Optional<MethodHandle> mayBeContendedValueMH;
    
    /**
     * Array base is aligned based on heap word. It is not visible by default as compressed references are used and the
     * header size is 16 but becomes visible when they are disabled.
     */
    private final int arrayBaseOffset;

    public MemoryLayoutBasedStrategy(MemoryLayoutSpecification memoryLayout,
                                     int arrayBaseOffset,
                                     Class<? extends Annotation> contendedClass,
                                     Optional<MethodHandle> mayBeContendedValueMH) {

        this.memoryLayout = memoryLayout;
        this.arrayBaseOffset = arrayBaseOffset;
        this.contendedClass = contendedClass;
        this.mayBeContendedValueMH = mayBeContendedValueMH;
    }

    @Override
    public final long measure(Object object) {
        Class<?> type = object.getClass();
         return type.isArray() ? measureArray(object, type) : measureInstance(type);
    }

    @Override
    public long measureArray(Object[] array) {
        return computeArraySize(arrayBaseOffset , array.length, memoryLayout.getReferenceSize());
    }

    @Override
    public long measureArray(byte[] array) {
        return computeArraySize(arrayBaseOffset , array.length, Byte.BYTES);
    }

    @Override
    public long measureArray(boolean[] array) {
        return computeArraySize(arrayBaseOffset , array.length, 1);
    }

    @Override
    public long measureArray(short[] array) {
        return computeArraySize(arrayBaseOffset , array.length, Short.BYTES);
    }

    @Override
    public long measureArray(char[] array) {
        return computeArraySize(arrayBaseOffset , array.length, Character.BYTES);
    }

    @Override
    public long measureArray(int[] array) {
        return computeArraySize(arrayBaseOffset , array.length, Integer.BYTES);
    }

    @Override
    public long measureArray(float[] array) {
        return computeArraySize(arrayBaseOffset, array.length, Float.BYTES);
    }

    @Override
    public long measureArray(long[] array) {
        return computeArraySize(arrayBaseOffset, array.length, Long.BYTES);
    }

    @Override
    public long measureArray(double[] array) {
        return computeArraySize(arrayBaseOffset, array.length, Double.BYTES);
    }

    @Override
    public long measureString(String s) {
        return measure(s);
    }

    /**
     * Measures the shallow memory used by objects of the specified class.
     *
     * @param type the object type
     * @return the shallow memory used by the object
     */
    protected abstract long measureInstance(Class<?> type);

    /**
     * Measure the shallow memory used by the specified array.
     *
     * @param instance the array instance
     * @param type the array type
     * @return the shallow memory used by the specified array
     */
    protected final long measureArray(Object instance, Class<?> type) {
        int length = Array.getLength(instance);
        int elementSize = measureField(type.getComponentType());
        return computeArraySize(arrayBaseOffset, length, elementSize);
    }

    /**
     * Computes the size of an array from its base offset, length and elementSize.
     *
     * @param arrayBaseOffset the array base offset
     * @param length the array length
     * @param elementSize the size of the array elements
     * @return the size of the array
     */
    private long computeArraySize(int arrayBaseOffset, int length, int elementSize) {
        return roundTo(arrayBaseOffset + length * (long) elementSize, memoryLayout.getObjectAlignment());
    }

    /**
     * @return The memory size of a field of a class of the provided type; for Objects this is the size of the reference only
     */
    protected final int measureField(Class<?> type) {

        if (!type.isPrimitive())
            return memoryLayout.getReferenceSize();

        if (type == boolean.class || type == byte.class)
            return 1;

        if (type == char.class || type == short.class)
            return 2;

        if (type == float.class || type == int.class)
            return 4;

        if (type == double.class || type == long.class)
            return 8;

        throw new IllegalStateException();
    }

    /**
     * Checks if the specified type is annotated with {@code Contended}
     *
     * @param type the type to check 
     * @return {@code true} if the specified type is annotated with {@code Contended}, {@code false} otherwise. 
     */
    protected final boolean isClassAnnotatedWithContended(Class<?> type) {
        return type.isAnnotationPresent(contendedClass);
    }

    /**
     * Checks if the specified field is annotated with {@code Contended}
     *
     * @param f the field to check
     * @return {@code true} if the specified field is annotated with {@code Contended}, {@code false} otherwise. 
     */
    protected final boolean isFieldAnnotatedWithContended(Field f) {
        return f.isAnnotationPresent(contendedClass);
    }

    /**
     * Returns the {@code @Contended} annotation of the specified field
     *
     * @param f the field
     * @return the {@code @Contended} annotation of the specified field 
     */
    private Object getContendedAnnotation(Field f) {
        return f.getAnnotation(contendedClass);
    }

    /**
     * Adds to the counter the contention group tag for the specified field
     * 
     * @param counter the counter to add to
     * @param f the field
     * @return the provided counter or a new one if the provided one was {@code null}
     */
    protected final ContentionGroupsCounter countContentionGroup(ContentionGroupsCounter counter, Field f) {

        if (isFieldAnnotatedWithContended(f)) {
            String tag = getContentionGroupTag(f);
            if (counter == null)
                counter = new ContentionGroupsCounter();
            counter.add(tag);
        }
        return counter;
    }

    /**
     * Returns the contention group tag of the {@code @Contended} annotation of the specified field.
     *
     * @param f the field for which the contention group tag must be retrieved
     * @return the contention group tag of the {@code @Contended} annotation of the specified field
     */
    private String getContentionGroupTag(Field f) {

        try {
            if (mayBeContendedValueMH.isPresent())
                return (String) mayBeContendedValueMH.get().invoke(getContendedAnnotation(f));
        } catch (Throwable e) {
            throw new CannotMeasureObjectException("The field " + f.getName() + " from the class " + f.getDeclaringClass() + "cannot be measured.", e);
        }

        // We cannot retrieve the contention group tag as the annotation can only be used by internal classes.
        // Up to Java 17 the only internal java class using @Contended on fields was Thread.
        if (f.getDeclaringClass().equals(Thread.class))
            return "tlr"; // for ThreadLocalRandom

        throw new CannotMeasureObjectException("The field " + f.getName() + " from the class " + f.getDeclaringClass() 
                                                 + "cannot be measured as the @Contended contention group tag cannot be retrieved."
                                                 + " Consider using: --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED to remove that problem");
    }

    /**
     * Checks if a class is trusted (loaded by the root or platform (named extension in Java 8) ClassLoader) or not. 
     *
     * @param cls the class to check
     * @return {@code true} if the class is trusted, {@code false} otherwise.
     */
    private boolean isTrustedClass(Class<?> cls) {
        ClassLoader classLoader = cls.getClassLoader();
        return classLoader == null || PLATFORM_PREDICATE.test(classLoader);
    }

    /**
     * Checks if @Contended is enabled for the specified class
     *
     * @param cls the class
     * @return {@code true} if @Contended is enabled, {@code false} otherwise.
     */
    protected final boolean isContendedEnabled(Class<?> cls) {

        return CONTENDED_ENABLED && (isTrustedClass(cls) || !CONTENDED_RESTRICTED); 
    }

    /**
     * Returns the predicate used to determine if a ClassLoader is a Platform one (Extension in Java 8).
     * @return the predicate used to determine if a ClassLoader is a Platform one.
     */
    private static Predicate<ClassLoader> platformClassLoaderPredicate() {

        Optional<MethodHandle> mayBeMethodHandle = mayBeMethodHandle(ClassLoader.class, "getPlatformClassLoader()");

        // The getPlatformClassLoader method was added in Java 9
        if (mayBeMethodHandle.isPresent()) {
            try {
                ClassLoader platformClassLoader = (ClassLoader) mayBeMethodHandle.get().invoke();
                return cl -> platformClassLoader == cl;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        // For Java 8, we have to check the class name. A bit fragile but did not find another robust way.
        return cl -> cl.toString().startsWith("sun.misc.Launcher$ExtClassLoader");
    }
}
