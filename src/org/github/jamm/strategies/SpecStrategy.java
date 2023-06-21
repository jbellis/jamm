package org.github.jamm.strategies;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.github.jamm.MemoryLayoutSpecification;

import static org.github.jamm.MathUtils.roundTo;
import static org.github.jamm.strategies.ContendedUtils.countContentionGroup;
import static org.github.jamm.strategies.ContendedUtils.isClassAnnotatedWithContended;
import static org.github.jamm.strategies.ContendedUtils.isContendedEnabled;

/**
 * {@code MemoryMeterStrategy} that computes the size of the memory occupied by an object, in a Java 15+ JVM, based on 
 * the JVM information.
 * <p>In Java 15 the Field layout computation was optimized (https://bugs.openjdk.org/browse/JDK-8237767) to eliminate 
 * a certain amount of the inefficiency from the previous versions (see {@link PreJava15SpecStrategy}).</p>
 */
class SpecStrategy extends MemoryLayoutBasedStrategy
{
    public SpecStrategy(MemoryLayoutSpecification memoryLayout)
    {
        super(memoryLayout, memoryLayout.getArrayHeaderSize());
    }

    @Override
    public final long measureInstance(Object instance, Class<?> type) {

        long size = memoryLayout.getObjectHeaderSize() + sizeOfDeclaredFields(type);
        while ((type = type.getSuperclass()) != Object.class && type != null)
            size += sizeOfDeclaredFields(type);

        return roundTo(size, memoryLayout.getObjectAlignment());
    }

    /**
     * Returns the size of the declared fields of the specified class.
     *
     * @param type the class for which the size of its declared fields must be returned
     * @return the size of the declared fields of the specified class
     */
    private long sizeOfDeclaredFields(Class<?> type) {

        long size = 0;
        ContentionGroupsCounter contentionGroupCounter = null;
        for (Field f : type.getDeclaredFields())
        {
            if (!Modifier.isStatic(f.getModifiers())) {
                size += measureField(f.getType());
                // If some fields are annotated with @Contended we need to count the contention groups to know how much padding needs to be added
                // In Java 17, disabling Contended has no effect on field level annotations
                contentionGroupCounter = countContentionGroup(contentionGroupCounter, f);
            }
        }

        /* From Contended Javadoc:
         * The class level {@code @Contended} annotation is not inherited and has
         * no effect on the fields declared in any sub-classes. The effects of all
         * {@code @Contended} annotations, however, remain in force for all
         * subclass instances, providing isolation of all the defined contention
         * groups. Contention group tags are not inherited, and the same tag used
         * in a superclass and subclass, represent distinct contention groups.
         */
        if (isClassAnnotatedWithContended(type) && isContendedEnabled(type))
            size += (memoryLayout.getContendedPaddingWidth() << 1); 

        if (contentionGroupCounter != null)
            size += contentionGroupCounter.count() * (memoryLayout.getContendedPaddingWidth() << 1);

        return size;
    }
}
