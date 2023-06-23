package org.github.jamm;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.util.Arrays;
import java.util.List;

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

    private static final List<String> CLEANER_FIELDS_TO_IGNORE = Arrays.asList("queue", "prev", "next");

    private static final Class<?> CLEANER_CLASS = getCleanerClass();

    private static Class<?> getCleanerClass() {
        try {
            return ByteBuffer.allocateDirect(0)
                             .getClass()
                             .getDeclaredField("cleaner")
                             .getType();

        } catch (Exception e) {
            System.out.print("WARN: Jamm could not load the sun.misc.Cleaner Class. This might lead to overestimating DirectByteBuffer size.");
            return null;
        }
    }

    /**
     * Filter excluding static and primitive fields
     */
    public static final FieldFilter IGNORE_STATIC_AND_PRIMITIVE_FIELDS = (c, f) -> Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive();

    /**
     * Filter excluding class such as {@code Enum}, {@code Class}, {@code ClassLoader} and {@code AccessControlContext}
     */
    public static final FieldAndClassFilter IGNORE_KNOWN_SINGLETONS = c -> Class.class.equals(c) 
                                                                         || Enum.class.isAssignableFrom(c)
                                                                         || ClassLoader.class.isAssignableFrom(c)
                                                                         || AccessControlContext.class.isAssignableFrom(c);

    /**
     * Filter excluding non-strong references
     */
    public static final FieldFilter IGNORE_NON_STRONG_REFERENCES = (c, f) -> Reference.class.isAssignableFrom(c) && "referent".equals(f.getName());

    /**
     * Filter excluding some of the fields from sun.misc.Cleaner as they should not be taken into account.
     * The fields being excluded are: 
     * <ul>
     *     <li>queue: as it is a dummy queue referenced by all Cleaner instances.</li>
     *     <li>next and prev: as they are used to create a doubly-linked list of live cleaners and therefore refer to other Cleaners instances</li>
     * </ul>
     */
    public static final FieldFilter IGNORE_CLEANER_FIELDS = (c, f) -> c.equals(CLEANER_CLASS) && CLEANER_FIELDS_TO_IGNORE.contains(f.getName()) ;

    /**
     * Filter excluding the {@code group} field from thread classes as that field holds the references to all the other threads from the group to which the thread belongs. 
     */
    public static final FieldFilter IGNORE_THREAD_FIELDS = (c, f) -> c.equals(Thread.class) && "group".equals(f.getName()) ;

    /**
     * Filter excluding the outer class reference from non-static inner classes.
     * In practice that filter is only useful if the top class is an inner class, and we wish to ignore the outer class in the measurement.
     */
    public static final FieldFilter IGNORE_OUTER_CLASS_REFERENCES = (c, f) -> f.getName().matches(OUTER_CLASS_REFERENCE);

    /**
     * Filter excluding fields and class annotated with {@code Unmetered}
     */
    public static final FieldAndClassFilter IGNORE_UNMETERED_FIELDS_AND_CLASSES = new FieldAndClassFilter()
    {
        @Override
        public boolean ignore(Class<?> cls, Field field) {
            return field.isAnnotationPresent(Unmetered.class) || ignore(field.getType());
        }

        @Override
        public boolean ignore(Class<?> cls) {
            // The @Inherited annotation only causes annotations to be inherited from superclasses. Therefore we need to check the interfaces manually 
            return cls != null && (cls.isAnnotationPresent(Unmetered.class) || isAnnotationPresentOnInterfaces(cls));
        }

        /**
         * Checks if any of the implemented interfaces has the {@code @Unmetered} annotation
         * @param cls the class for which the interfaces must be checked
         * @return {@code true} if any of the interfaces is annotated with {@code @Unmetered}. {@code false} otherwise.
         */
        private boolean isAnnotationPresentOnInterfaces(Class<?> cls) {
            Class<?>[] interfaces = cls.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (cls.getInterfaces()[i].isAnnotationPresent(Unmetered.class))
                    return true;
            }

            return false;
        }
    };

    public static FieldAndClassFilter getClassFilters(boolean ignoreKnownSingletons) {

        if (ignoreKnownSingletons)
            return new FieldAndClassFilter() {

                @Override
                public boolean ignore(Class<?> cls, Field field) {
                    return IGNORE_KNOWN_SINGLETONS.ignore(cls, field) || IGNORE_UNMETERED_FIELDS_AND_CLASSES.ignore(cls, field);
                }

                @Override
                public boolean ignore(Class<?> cls)
                {
                    return IGNORE_KNOWN_SINGLETONS.ignore(cls) || IGNORE_UNMETERED_FIELDS_AND_CLASSES.ignore(cls);
                }
        };

        return IGNORE_UNMETERED_FIELDS_AND_CLASSES;
    }

    public static FieldFilter getFieldFilters(boolean ignoreKnownSingletons,
                                              boolean ignoreOuterClassReference,
                                              boolean ignoreNonStrongReferences) {

        if (ignoreOuterClassReference) {

            if (ignoreNonStrongReferences)
                return (c, f) -> IGNORE_STATIC_AND_PRIMITIVE_FIELDS.ignore(c, f) 
                        || getClassFilters(ignoreKnownSingletons).ignore(c, f)
                        || IGNORE_CLEANER_FIELDS.ignore(c, f)
                        || IGNORE_THREAD_FIELDS.ignore(c, f)
                        || IGNORE_NON_STRONG_REFERENCES.ignore(c, f)
                        || IGNORE_OUTER_CLASS_REFERENCES.ignore(c, f);

            return (c, f) -> IGNORE_STATIC_AND_PRIMITIVE_FIELDS.ignore(c, f) 
                    || getClassFilters(ignoreKnownSingletons).ignore(c, f)
                    || IGNORE_CLEANER_FIELDS.ignore(c, f)
                    || IGNORE_THREAD_FIELDS.ignore(c, f)
                    || IGNORE_OUTER_CLASS_REFERENCES.ignore(c, f);
        }

        if (ignoreNonStrongReferences)
            return (c, f) -> IGNORE_STATIC_AND_PRIMITIVE_FIELDS.ignore(c, f) 
                    || getClassFilters(ignoreKnownSingletons).ignore(c, f)
                    || IGNORE_CLEANER_FIELDS.ignore(c, f)
                    || IGNORE_THREAD_FIELDS.ignore(c, f)
                    || IGNORE_NON_STRONG_REFERENCES.ignore(c, f);

        return (c, f) -> IGNORE_STATIC_AND_PRIMITIVE_FIELDS.ignore(c, f) 
                || getClassFilters(ignoreKnownSingletons).ignore(c, f)
                || IGNORE_CLEANER_FIELDS.ignore(c, f)
                || IGNORE_THREAD_FIELDS.ignore(c, f);
    }

    /**
     * The class should not be instantiated.
     */
    private Filters() {
    }
}
