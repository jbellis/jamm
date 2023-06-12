package org.github.jamm.strategies;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.github.jamm.CannotMeasureObjectException;
import org.github.jamm.MemoryLayoutSpecification;

import static org.github.jamm.MathUtils.roundTo;
import static org.github.jamm.strategies.ContendedUtils.isClassAnnotatedWithContended;
import static org.github.jamm.strategies.ContendedUtils.isContendedEnabled;
import static org.github.jamm.strategies.ContendedUtils.isFieldAnnotatedWithContended;

import sun.misc.Unsafe;

/**
 * {@code MemoryMeterStrategy} relying on {@code Unsafe} to measure object sizes for Java versions >= 15.
 * 
 * <p>In Java 15, the way the JVM layout fields across the hierarchy changed. Prior to Java 15 superclass field
 * always came first. Therefore that strategy could only look at the current class to find the greatest offsets.
 * From Java 15 onward it is not the case anymore. The JVM optimizes ensure minimal memory usage by packing the fields
 * in the best possible way across the hierarchy (<a href="https://bugs.openjdk.org/browse/JDK-8237767">https://bugs.openjdk.org/browse/JDK-8237767</a>).</p>
 * 
 * <p>Another important change that came in Java 15 is the introduction of hidden classes (https://openjdk.org/jeps/371)
 * and the use of hidden class for lambda. Attempting to use {@code Unsafe.objectFieldOffset} on an hidden class field
 * will result in a {@code UnsupportedOperationException} preventing the {@code UnsafeStrategy} to evaluate correctly
 * the memory used by the class. To avoid that problem {@code UnsafeStrategy} will rely on the {@code SpecStrategy} to
 * measure hidden classes. This can lead to an overestimation of the object size as the {@code SpecStrategy} ignore some 
 * optimizations performed by the JVM</p> 
 */
public final class UnsafeStrategy extends MemoryLayoutBasedStrategy
{
    private final Unsafe unsafe;

    /**
     * Method Handle for the {@code Class.isRecord} method introduced in Java 14.
     */
    private final MethodHandle isRecordMH;

    /**
     * Method Handle for the {@code Class.isHidden} method introduced in Java 15.
     */
    private final MethodHandle isHiddenMH;

    /**
     * The strategy used for hidden classes and records.
     */
    private final MemoryLayoutBasedStrategy hiddenClassesOrRecordsStrategy;

    public UnsafeStrategy(MemoryLayoutSpecification memoryLayout,
                          Unsafe unsafe,
                          MethodHandle isRecordMH,
                          MethodHandle isHiddenMH,
                          MemoryLayoutBasedStrategy strategy)
    {
        super(memoryLayout, unsafe.arrayBaseOffset(Object[].class));
        this.unsafe = unsafe;
        this.isRecordMH = isRecordMH;
        this.isHiddenMH = isHiddenMH;
        this.hiddenClassesOrRecordsStrategy = strategy;
    }

    @Override
    public long measureInstance(Class<?> type) {

        try {

            // If the class is a hidden class ore a record 'unsafe.objectFieldOffset(f)' will throw an UnsupportedOperationException
            // In those cases, rather than failing, we rely on the Spec strategy to provide the measurement.
            if ((Boolean) isRecordMH.invoke(type) || (Boolean) isHiddenMH.invoke(type))
                return hiddenClassesOrRecordsStrategy.measureInstance(type);

            long size = 0;
            boolean isLastFieldWithinContentionGroup = false;
            while (type != null)
            {
                for (Field f : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        long previousSize = size;
                        size = Math.max(size, unsafe.objectFieldOffset(f) + measureField(f.getType()));
                        // In Java 17, disabling Contended has no effect on field level annotations
                        if (previousSize < size)
                            isLastFieldWithinContentionGroup = isFieldAnnotatedWithContended(f);
                    }
                }

                if (isClassAnnotatedWithContended(type) && isContendedEnabled(type))
                    size += memoryLayout.getContendedPaddingWidth();

                type = type.getSuperclass();
            }
            if (size == 0) {
                size = memoryLayout.getObjectHeaderSize();
            } else {
                if (isLastFieldWithinContentionGroup)
                    size += memoryLayout.getContendedPaddingWidth();
            }
            size = size > 0 ? size : memoryLayout.getObjectHeaderSize();
            return roundTo(size, memoryLayout.getObjectAlignment());
        } 
        catch (Throwable e) {
            throw new CannotMeasureObjectException("The object of type " + type + " cannot be measured by the unsafe strategy", e);
        }
    }
}
