package org.github.jamm.string;

import org.github.jamm.MemoryMeterStrategy;

import sun.misc.Unsafe;

/**
 * {@code StringMeter} using {@code Unsafe} to measure the String value on Java 9+ versions when the field is not
 * accessible through reflection.
 */
final class UnsafeStringMeter extends StringMeter {

    private final Unsafe unsafe;

    private final long valueFieldOffset;

    UnsafeStringMeter(Unsafe unsafe, long valueFieldOffset) {
        this.unsafe = unsafe;
        this.valueFieldOffset = valueFieldOffset;
    }

    @Override
    public long measureStringValue(MemoryMeterStrategy strategy, String s) {
        return strategy.measureArray((byte[]) unsafe.getObjectVolatile(s, valueFieldOffset));
    }
}
