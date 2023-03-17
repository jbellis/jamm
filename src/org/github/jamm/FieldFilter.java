package org.github.jamm;

import java.lang.reflect.Field;

/**
 * A filter for class fields.
 */
@FunctionalInterface
public interface FieldFilter
{
    /**
     * Checks whether a {@code Field} must be ignored or not.
     *
     * @param cls the class to which the field belong. Which might be different from the declaring class if the field is
     * from a superclass.
     * @param field the field to check
     * @return {@code true} if the field must be ignored {@code false} otherwise.
     */
    boolean ignore(Class<?> cls, Field field);
}
