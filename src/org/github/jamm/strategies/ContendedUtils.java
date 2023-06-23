package org.github.jamm.strategies;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Predicate;

import org.github.jamm.CannotMeasureObjectException;
import org.github.jamm.VM;

import static org.github.jamm.utils.MethodHandleUtils.mayBeMethodHandle;

/**
 * Utility methods to retrieve information about the use of {@code @Contended} annotations.
 */
public final class ContendedUtils {

    /**
     * {@code true} if contended is enabled, {@code false} otherwise.
     */
    private static final boolean CONTENDED_ENABLED = VM.enableContended();

    /**
     * {@code true} if contended is restricted, {@code false} otherwise.
     */
    private static final boolean CONTENDED_RESTRICTED = VM.restrictContended();

    /**
     * The {@code Contended} annotation class.
     */
    private static final Class<? extends Annotation> CONTENDED_CLASS = loadContendedClass();

    /**
     * The {@code MethodHandle} used to invoke the value method from {@code @Contended} if it is accessible.
     *
     * @Contended was introduced in Java 8 as {@code sun.misc.Contended} but was repackaged in the jdk.internal.vm.annotation package in Java 9.
     * Therefore in Java 9+ unless '-XX:-RestrictContended' or '--add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED' is specified we will not have access
     * to the value() method of @Contended and will be unable to retrieve the contention group tags and might be unable to computes the correct sizes.
     * Nevertheless, it also means that only the internal Java classes will use that annotation and we know which one they are. Therefore we can rely on this fact to mitigate the problem.
     */
    private static final Optional<MethodHandle> MAY_BE_CONTENDED_VALUE_MH = mayBeMethodHandle(CONTENDED_CLASS, "value");

    /**
     * The predicate used to check if a ClassLoader is a platform one.
     */
    private static final Predicate<ClassLoader> PLATFORM_PREDICATE = platformClassLoaderPredicate();

    /**
     * Checks if the specified type is annotated with {@code Contended}
     *
     * @param type the type to check 
     * @return {@code true} if the specified type is annotated with {@code Contended}, {@code false} otherwise. 
     */
    public static boolean isClassAnnotatedWithContended(Class<?> type) {
        return type.isAnnotationPresent(CONTENDED_CLASS);
    }

    /**
     * Checks if the specified field is annotated with {@code Contended}
     *
     * @param f the field to check
     * @return {@code true} if the specified field is annotated with {@code Contended}, {@code false} otherwise. 
     */
    public static boolean isFieldAnnotatedWithContended(Field f) {
        return f.isAnnotationPresent(CONTENDED_CLASS);
    }

    /**
     * Returns the {@code @Contended} annotation of the specified field
     *
     * @param f the field
     * @return the {@code @Contended} annotation of the specified field 
     */
    private static Object getContendedAnnotation(Field f) {
        return f.getAnnotation(CONTENDED_CLASS);
    }

    /**
     * Adds to the counter the contention group tag for the specified field
     * 
     * @param counter the counter to add to
     * @param f the field
     * @return the provided counter or a new one if the provided one was {@code null}
     */
    public static ContentionGroupsCounter countContentionGroup(ContentionGroupsCounter counter, Field f) {

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
    public static String getContentionGroupTag(Field f) {

        try {
            if (MAY_BE_CONTENDED_VALUE_MH.isPresent())
                return (String) MAY_BE_CONTENDED_VALUE_MH.get().invoke(getContendedAnnotation(f));
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
    private static boolean isTrustedClass(Class<?> cls) {
        ClassLoader classLoader = cls.getClassLoader();
        return classLoader == null || PLATFORM_PREDICATE.test(classLoader);
    }

    /**
     * Checks if @Contended is enabled for the specified class
     *
     * @param cls the class
     * @return {@code true} if @Contended is enabled, {@code false} otherwise.
     */
    public static boolean isContendedEnabled(Class<?> cls) {

        return CONTENDED_ENABLED && (isTrustedClass(cls) || !CONTENDED_RESTRICTED); 
    }

    /**
     * Load the {@code Contended} class.
     * @return the {@code Contended} class.
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadContendedClass() {
        try {
            return (Class<? extends Annotation>) Class.forName("sun.misc.Contended");
        } catch (ClassNotFoundException e) {
            try {
                return (Class<? extends Annotation>) Class.forName("jdk.internal.vm.annotation.Contended");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("The Contended annotation class could not be loaded.", ex);
            }
        }
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
