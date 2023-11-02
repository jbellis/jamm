package org.github.jamm;

import org.github.jamm.accessors.FieldAccessor;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Stores metadata about a class to be used when measuring objects.
 * <p>
 * CONTRACT:
 * There is an implicit assumption that calling methods on an instance of this class is for an
 * object of the same class as what was passed into the constructor during initialization.
 */
public class ClassMetadata {

    /**
     * Reusable empty array to minimize the cache size as many classes will end up with no
     * measurable fields. This includes primitive wrapper classes or any custom classes which
     * don't contain any fields that pass the field filter.
     */
    public static final Field[] NO_FIELDS = new Field[0];
    public final int shallowSize;

    /**
     * The declared fields that pass the fieldFilter.  These are used to discover nested objects.
     */
    private final Field[] measurableFields;

    public ClassMetadata(int shallowSize, Class<?> clazz, FieldFilter fieldFilter) {
        this.shallowSize = shallowSize;
        measurableFields = getMeasurableFields(clazz, fieldFilter);
    }

    /**
     * Adds the applicable nested child objects from the parentObject into the measurement stack.
     *
     * @param parentObject  The parent object from which to fetch the nested child objects
     * @param fieldAccessor The accessor for fetching the field value
     * @param classFilter   The FieldAndClassFilter that determines whether a child object should be ignored
     * @param stack         The measurement stack to add the child objects into
     */
    public void addFieldReferences(
            Object parentObject,
            FieldAccessor fieldAccessor,
            FieldAndClassFilter classFilter,
            MeasurementStack stack
    ) {
        MemoryMeterListener listener = stack.listener();
        // We only need to check the classFilter since the field filter was already applied when
        // this cache entry was initialized
        for (Field field : measurableFields) {
            Object child = getFieldValue(fieldAccessor, parentObject, field, listener);
            if (child != null && !classFilter.ignore(child.getClass())) {
                stack.pushObject(parentObject, field.getName(), child);
            }
        }
    }

    /**
     * Retrieves the field value if possible.
     *
     * @param obj      the object for which the field value must be retrieved
     * @param field    the field for which the value must be retrieved
     * @param listener the {@code MemoryMeterListener}
     * @return the field value if it was possible to retrieve it
     * @throws CannotAccessFieldException if the field could not be accessed
     */
    private static Object getFieldValue(
            FieldAccessor accessor,
            Object obj,
            Field field,
            MemoryMeterListener listener
    ) {
        try {
            return accessor.getFieldValue(obj, field);
        } catch (CannotAccessFieldException e) {
            listener.failedToAccessField(obj, field.getName(), field.getType());
            throw e;
        }
    }

    /**
     * Gets the fields which should be measured when fetching nested child objects.
     *
     * @param clazz       The class for which to discover the declared fields to be measured
     * @param fieldFilter The field filter which determines which fields to ignore
     * @return An array of the fields to be measured
     */
    private static Field[] getMeasurableFields(Class<?> clazz, FieldFilter fieldFilter) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> type = clazz;
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!fieldFilter.ignore(clazz, field)) {
                    fields.add(field);
                }
            }
            type = type.getSuperclass();
        }
        // Share the empty array when there aren't any measurable fields in order to minimize cache size
        return fields.isEmpty() ? NO_FIELDS : fields.toArray(new Field[fields.size()]);
    }
}
