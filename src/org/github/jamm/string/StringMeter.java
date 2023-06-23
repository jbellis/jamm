package org.github.jamm.string;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Optional;

import sun.misc.Unsafe;

import org.github.jamm.CannotAccessFieldException;
import org.github.jamm.MemoryMeter;
import org.github.jamm.MemoryMeterStrategy;
import org.github.jamm.VM;
import org.github.jamm.strategies.MemoryMeterStrategies;
import org.github.jamm.utils.MethodHandleUtils;

import static org.github.jamm.utils.MethodHandleUtils.methodHandle;

/**
 * Utility to measure the value field of a {@code String}
 */
public abstract class StringMeter {

    /**
     * The String shallow size stored as a constant to have it compiled directly into the measure method.
     */
    public static final long STRING_SHALLOW_SIZE = MemoryMeterStrategies.getInstance()
                                                                        .getStrategy(MemoryMeter.BEST)
                                                                        .measure("");

    /**
     * Measure the deep size of the specified String.
     *
     * @param strategy the strategy to perform the measurement
     * @param s the string
     * @return the size of the deep string
     */
    public long measure(MemoryMeterStrategy strategy, String s) {
        return STRING_SHALLOW_SIZE + measureStringValue(strategy, s);
    }

    /**
     * Measure the size of the value of the specified String.
     *
     * @param strategy the strategy to perform the measurement
     * @param s the string
     * @return the size of the string value field
     */
    protected abstract long measureStringValue(MemoryMeterStrategy strategy, String s);

    /**
     * Creates a new {@code StringMeter} instance.
     * @return a new {@code StringMeter} instance.
     */
    public static StringMeter newInstance() {

        try
        {
            Field field = String.class.getDeclaredField("value");

            Optional<MethodHandle> mayBeTrySetAccessible = MethodHandleUtils.mayBeMethodHandle(Field.class, "trySetAccessible"); // Added in Java 9
            if (mayBeTrySetAccessible.isPresent()) {

                if ((boolean) mayBeTrySetAccessible.get().invoke(field)) {
                    return new PlainReflectionStringMeter(methodHandle(field));
                }
                // We do not have access to the field through reflection we need to use Unsafe
                Unsafe unsafe = VM.getUnsafe();

                if (unsafe == null)
                    throw new CannotAccessFieldException("The value of the 'value' field from java.lang.String"
                                                         + " cannot be retrieved as the field cannot be made accessible and Unsafe is unavailable");

                long valueFieldOffset = unsafe.objectFieldOffset(field);
                return new UnsafeStringMeter(unsafe, valueFieldOffset);
            }

            field.setAccessible(true);
            return new PreJava9StringMeter(methodHandle(field));

        } catch (Throwable e) {
            throw new CannotAccessFieldException("The value of the 'value' field from java.lang.String cannot be retrieved", e);
        }
    }
}
