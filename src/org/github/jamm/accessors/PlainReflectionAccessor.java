package org.github.jamm.accessors;

import java.lang.reflect.Field;

import org.github.jamm.CannotAccessFieldException;

/**
 * {@code FieldAccessor} relying on plain reflection to retrieve field value.
 * <p>This accessor will not work properly with JDK9+ due to the introduction of the Module System.
 */
final class PlainReflectionAccessor implements FieldAccessor
{
    @Override
    public Object getFieldValue(Object object, Field field) {
        try {
            if (!field.isAccessible())
                field.setAccessible(true);

            return field.get(object);

        } catch (Exception e) {
            throw new CannotAccessFieldException("The value of the " + field.getName() + " field from " + object.getClass() + " cannot be retrieved", e);
        }
    }
}
