package org.github.jamm;

import java.lang.reflect.Field;

/**
 * Filter for fields and classes.
 */
@FunctionalInterface
public interface FieldAndClassFilter extends FieldFilter
{
    @Override
    default boolean ignore(Class<?> cls, Field field) {
        return ignore(field.getType());
    }

    /**
     * Checks whether a {@code Class} must be ignored or not.
     *
     * @param clazz the class to check
     * @return {@code true} if the class must be ignored {@code false} otherwise.
     */
    boolean ignore(Class<?> clazz);
}
