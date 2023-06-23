package org.github.jamm.accessors;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.github.jamm.VM;

import static org.github.jamm.utils.MethodHandleUtils.methodHandle;

/**
 * Utility to retrieve {@code Field} values.
 */
public interface FieldAccessor {

    /**
     * Returns the field value for the given object
     *
     * @param object the object for which the field value must be returned
     * @param field the field to access
     * @return the field value for the given object
     */
    Object getFieldValue(Object object, Field field);

    /**
     * Returns the {@code FieldAccessor} instance suitable for the JDK running this code.
     * @return a {@code FieldAccessor} instance
     */
    static FieldAccessor newInstance() {

        try {
            Method method = Field.class.getMethod("trySetAccessible", new Class[0]);
            MethodHandle trySetAccessibleMH = methodHandle(method);

            // If we reached that point we are on a JDK9+ version with a Module System
            return new JpmsAccessor(trySetAccessibleMH, VM.getUnsafe());
        }
        catch (NoSuchMethodException | SecurityException | IllegalAccessException e)
        {
            return new PlainReflectionAccessor();
        }
    }
}
