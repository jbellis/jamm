package org.github.jamm.string;

import java.lang.invoke.MethodHandle;

import org.github.jamm.CannotAccessFieldException;
import org.github.jamm.MemoryMeterStrategy;

/**
 * {@code StringMeter} used to measure pre-Java 9 strings where the {@code String} value is a {@code char} array.
 *
 */
final class PreJava9StringMeter extends  StringMeter {

    /**
     * The method handle used to access the value field.
     */
    private final MethodHandle valueMH;

    public PreJava9StringMeter(MethodHandle valueMH) {
        this.valueMH = valueMH;
    }

    @Override
    public long measureStringValue(MemoryMeterStrategy strategy, String s) {

        if (strategy.supportComputeArraySize())
            return strategy.computeArraySize(s.length(), Character.BYTES);

        try {
            return strategy.measureArray((char[]) valueMH.invoke(s));
        }  catch (Throwable e) {
            throw new CannotAccessFieldException("The value of the 'value' field from java.lang.String cannot be retrieved", e);
        }
    }
}
