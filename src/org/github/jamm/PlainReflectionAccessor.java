package org.github.jamm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * {@code FieldAccessor} relying on plain reflection to retrieve field value.
 * <p>This accessor will not work properly with JDK9+ due to the introduction of the Module System.
 */
final class PlainReflectionAccessor implements FieldAccessor
{
    @Override
    public Object getObjectValue(Object object, Field field) {
        try {
            if (!Modifier.isPublic(field.getModifiers()) && !field.isAccessible())
                field.setAccessible(true);

            return field.get(object);

        } catch (Exception e) {
            throw new CannotAccessFieldException("The value of the " + field.getName() + " field from " + object.getClass() + " cannot be retrieved", e);
        }
    }
}
