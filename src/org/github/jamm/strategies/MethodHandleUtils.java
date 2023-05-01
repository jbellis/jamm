package org.github.jamm.strategies;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Optional;

final class MethodHandleUtils
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
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return Optional.of(lookup.unreflect(method));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private MethodHandleUtils() {}
}
