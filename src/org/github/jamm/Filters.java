package org.github.jamm;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Utility class providing the different filters used by {@code MemoryMeter}
 *
 */
public final class Filters
{
    /**
     * Name pattern for outer class reference
     */
    private static final String OUTER_CLASS_REFERENCE = "this\\$[0-9]+";

    /**
     * Filter excluding static and primitive fields
     */
    private static final FieldFilter IGNORE_STATIC_AND_PRIMITIVE_FIELDS = f -> Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive();

    /**
     * Filter excluding class such as {@code Enum} and {@code Class}
     */
    private static final FieldAndClassFilter IGNORE_KNOWN_SINGLETONS = c -> Class.class.equals(c) || Enum.class.isAssignableFrom(c);

    /**
     * Filter excluding non strong references
     */
    private static final FieldFilter IGNORE_NON_STRONG_REFERENCES = f -> Reference.class.isAssignableFrom(f.getDeclaringClass()) && "referent".equals(f.getName());

    /**
     * Filter excluding the outer class reference from non static inner classes.
     * In practice that filter is only useful if the top class is an inner class and we wish to ignore the outer class in the measurement.
     */
    private static final FieldFilter IGNORE_OUTER_CLASS_REFERENCES = f -> f.getName().matches(OUTER_CLASS_REFERENCE);

    /**
     * Filter excluding fields and class annotated with {@code Unmetered}
     */
    private static final FieldAndClassFilter IGNORE_UNMETERED_FIELDS_AND_CLASSES = new FieldAndClassFilter()
    {
        @Override
        public boolean ignore(Field field) {
            return field.isAnnotationPresent(Unmetered.class) || ignore(field.getType());
        }

        @Override
        public boolean ignore(Class<?> cls) {
            return isUnmeteredAnnotationPresent(cls);
        }

        /**
         * Checks if the specified class or one of its parent is annotated with {@code Unmetered}
         *
         * @param cls the class to check 
         * @return {@code true} if the specified class or one of its parent is annotated with {@code Unmetered}, {@code false} otherwise.
         */
        private boolean isUnmeteredAnnotationPresent(Class<?> cls) {

            if (cls == null)
                return false;

            if (cls.isAnnotationPresent(Unmetered.class))
                return true;

            Class<?>[] interfaces = cls.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (isUnmeteredAnnotationPresent(cls.getInterfaces()[i]))
                    return true;
            }

            return isUnmeteredAnnotationPresent(cls.getSuperclass());
        }
    };

    public static FieldAndClassFilter getClassFilters(boolean ignoreKnownSingletons) {

        if (ignoreKnownSingletons)
            return c -> IGNORE_KNOWN_SINGLETONS.ignore(c) || IGNORE_UNMETERED_FIELDS_AND_CLASSES.ignore(c);

        return IGNORE_UNMETERED_FIELDS_AND_CLASSES;
    }

    public static FieldFilter getFieldFilters(boolean ignoreKnownSingletons, boolean ignoreOuterClassReference, boolean ignoreNonStrongReferences) {

        if (ignoreOuterClassReference) {

            if (ignoreNonStrongReferences)
                return f -> IGNORE_STATIC_AND_PRIMITIVE_FIELDS.ignore(f) 
                        || getClassFilters(ignoreKnownSingletons).ignore(f)
                        || IGNORE_NON_STRONG_REFERENCES.ignore(f)
                        || IGNORE_OUTER_CLASS_REFERENCES.ignore(f);

            return f -> IGNORE_STATIC_AND_PRIMITIVE_FIELDS.ignore(f) 
                    || getClassFilters(ignoreKnownSingletons).ignore(f)
                    || IGNORE_OUTER_CLASS_REFERENCES.ignore(f);
        }

        if (ignoreNonStrongReferences)
            return f -> IGNORE_STATIC_AND_PRIMITIVE_FIELDS.ignore(f) 
                    || getClassFilters(ignoreKnownSingletons).ignore(f)
                    || IGNORE_NON_STRONG_REFERENCES.ignore(f);

        return f -> IGNORE_STATIC_AND_PRIMITIVE_FIELDS.ignore(f) 
                || getClassFilters(ignoreKnownSingletons).ignore(f);
    }

    /**
     * The class should not be instantiated.
     */
    private Filters() {
    }
}
