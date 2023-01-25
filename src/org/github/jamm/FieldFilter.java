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
     * @param field the field to check
     * @return {@code true} if the field must be ignored {@code false} otherwise.
     */
    boolean ignore(Field field);
}
