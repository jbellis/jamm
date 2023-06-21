package org.github.jamm.strategies;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import org.github.jamm.CannotMeasureObjectException;
import org.github.jamm.MemoryLayoutSpecification;

import sun.misc.Unsafe;

import static org.github.jamm.MathUtils.roundTo;
import static org.github.jamm.strategies.ContendedUtils.isClassAnnotatedWithContended;
import static org.github.jamm.strategies.ContendedUtils.isContendedEnabled;
import static org.github.jamm.strategies.ContendedUtils.isFieldAnnotatedWithContended;

/**
 * {@code MemoryMeterStrategy} relying on {@code Unsafe} to measure object sizes for Java version pre-15.
 *
 * <p>In Java 15, the JVM layout of fields across the hierarchy changed. Prior to Java 15 superclass field
 * always came first. From Java 15 onward it is not the case anymore. This strategy takes advantage and it looks at
 * the child class first to find the greatest offsets.</p>
 */
final class PreJava15UnsafeStrategy extends MemoryLayoutBasedStrategy
{
    private final Unsafe unsafe;

    /**
     * Method Handle for the {@code Class.isRecord} method introduced in Java 14.
     */
    private final Optional<MethodHandle> mayBeIsRecordMH;

    /**
     * The strategy used for records.
     */
    private final MemoryLayoutBasedStrategy recordsStrategy;

    public PreJava15UnsafeStrategy(MemoryLayoutSpecification memoryLayout,
                                   Unsafe unsafe,
                                   Optional<MethodHandle> mayBeIsRecordMH,
                                   MemoryLayoutBasedStrategy strategy) {

        super(memoryLayout, unsafe.arrayBaseOffset(Object[].class)); 
        this.unsafe = unsafe;
        this.mayBeIsRecordMH = mayBeIsRecordMH;
        this.recordsStrategy = strategy;
    }

    @Override
    public long measureInstance(Object instance, Class<?> type) {

        try {

            // If the class is a record 'unsafe.objectFieldOffset(f)' will throw an UnsupportedOperationException
            // In those cases, rather than failing, we rely on the Spec strategy to provide the measurement.
            if (mayBeIsRecordMH.isPresent() &&  ((Boolean) mayBeIsRecordMH.get().invoke(type)))
                return recordsStrategy.measureInstance(instance, type);

            int annotatedClassesWithoutFields = 0; // Keep track of the @Contended annotated classes without fields
            while (type != null) {

                long size = 0;
                boolean isLastFieldWithinContentionGroup = false;
                for (Field f : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers()))
                    {
                        long previousSize = size;
                        size = Math.max(size, unsafe.objectFieldOffset(f) + measureField(f.getType()));
                        if (previousSize < size)
                            isLastFieldWithinContentionGroup = isFieldAnnotatedWithContended(f) && isContendedEnabled(type);
                    }
                }

                // If the last field is within a contention group we need to add the end padding
                if (isLastFieldWithinContentionGroup)
                    size += memoryLayout.getContendedPaddingWidth();

                // As we know that the superclass fields always come first pre-Java 15, if the size is greater than zero
                // we know that all the other fields will have a smaller offset.
                if (size > 0) {
                    // If the class is annotated with @Contended we need to add the end padding
                    if (isClassAnnotatedWithContended(type) && isContendedEnabled(type))
                        size += memoryLayout.getContendedPaddingWidth();

                    size += annotatedClassesWithoutFields * (memoryLayout.getContendedPaddingWidth() << 1);

                    return roundTo(size, memoryLayout.getObjectAlignment());
                }

                // The JVM will add padding even if the annotated class does not have any fields 
                if (isClassAnnotatedWithContended(type) && isContendedEnabled(type))
                    annotatedClassesWithoutFields++;

                type = type.getSuperclass();
            }
            return roundTo(memoryLayout.getObjectHeaderSize(), memoryLayout.getObjectAlignment());
        } 
        catch (Throwable e) {
            throw new CannotMeasureObjectException("The object of type " + type + " cannot be measured by the unsafe strategy", e);
        }
    }
}
