package org.github.jamm.accessors;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.github.jamm.CannotAccessFieldException;

import sun.misc.Unsafe;

/**
 * {@code FieldAccessor} able to deal with the Java Platform Module System by relying on {@code Unsafe} to access object
 * protected by the Module System.
 */
final class JpmsAccessor implements FieldAccessor
{
    /**
     * The {@code MethodHandle} for the {@code AccessibleObject.trySetAccessible} method introduced in JDK 9.
     */
    private final MethodHandle trySetAccessibleMH;

    /**
     * The unsafe instance used to access object protected by the Module System
     */
    private final Unsafe unsafe;

    public JpmsAccessor(MethodHandle trySetAccessibleMH, Unsafe unsafe) {
        this.trySetAccessibleMH = trySetAccessibleMH;
        this.unsafe = unsafe;
    }

    @Override
    public Object getFieldValue(Object object, Field field) {
        try {
            // This call will unfortunately emit a warning for some scenario (which was a weird decision from the JVM designer)
            if ((boolean) trySetAccessibleMH.invoke(field)) {
                // The field is accessible lets use reflection.
                return field.get(object);
            }

            // The access to the field is being restricted by the module system. Let's try to go around it through Unsafe.
            if (unsafe == null)
                throw new CannotAccessFieldException("The value of the '" + field.getName() + "' field from " + object.getClass().getName()
                                                     + " cannot be retrieved as the field cannot be made accessible and Unsafe is unavailable");

            long offset = unsafe.objectFieldOffset(field);

            boolean isFinal = Modifier.isFinal(field.getModifiers());
            boolean isVolatile = Modifier.isVolatile(field.getModifiers());

            return isFinal || isVolatile ? unsafe.getObjectVolatile(object, offset) : unsafe.getObject(object, offset);

        }  catch (Throwable e) {
            throw new CannotAccessFieldException("The value of the '" + field.getName() + "' field from " + object.getClass().getName() + " cannot be retrieved", e);
        }
    }
}
