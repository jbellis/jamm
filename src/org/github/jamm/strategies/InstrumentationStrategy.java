package org.github.jamm.strategies;

import java.lang.instrument.Instrumentation;

import org.github.jamm.MemoryMeterStrategy;

/**
 * {@code MemoryMeterStrategy} relying on {@code Instrumentation} to measure object size.
 *
 */
final class InstrumentationStrategy implements MemoryMeterStrategy {

    private final Instrumentation instrumentation;

    public InstrumentationStrategy(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public long measure(Object object) {
        return instrumentation.getObjectSize(object);
    }
}
