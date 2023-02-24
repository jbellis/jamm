package org.github.jamm;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

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
            long offset = unsafe.objectFieldOffset(field);
            return unsafe.getObject(object, offset);

        }  catch (Throwable e) {
            throw new CannotAccessFieldException("The value of the " + field.getName() + " field from " + object.getClass() + " cannot be retrieved", e);
        }
    }
}
