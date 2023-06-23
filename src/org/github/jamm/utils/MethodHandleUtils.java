package org.github.jamm.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class MethodHandleUtils
{
    /**
     * Returns the {@code MethodHandle} for the specified class and method if the method exists.
     *
     * @param klass the class
     * @param methodName the method name
     * @return an {@code Optional} for the {@code MethodHandle}
     */
    public static Optional<MethodHandle> mayBeMethodHandle(Class<?> klass, String methodName) {
        try {
            Method method = klass.getMethod(methodName, new Class[0]);
            return Optional.of(methodHandle(method));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the {@code MethodHandle} for the specified method.
     *
     * @param method the method
     * @return the {@code MethodHandle} for the specified method
     * @throws IllegalAccessException 
     */
    public static MethodHandle methodHandle(Method method) throws IllegalAccessException {
        return MethodHandles.lookup().unreflect(method);
    }

    /**
     * Returns the {@code MethodHandle} for the specified field.
     *
     * @param field the field
     * @return the {@code MethodHandle} for the specified field
     * @throws IllegalAccessException 
     */
    public static MethodHandle methodHandle(Field field) throws IllegalAccessException {
        return MethodHandles.lookup().unreflectGetter(field);
    }

    private MethodHandleUtils() {}
}
