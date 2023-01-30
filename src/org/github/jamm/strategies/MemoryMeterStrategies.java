package org.github.jamm.strategies;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

import org.github.jamm.MemoryLayoutSpecification;
import org.github.jamm.MemoryMeter.Guess;
import org.github.jamm.MemoryMeterStrategy;

import sun.misc.Unsafe;

/**
 * The different strategies that can be used to measure object sizes.
 */
public final class MemoryMeterStrategies
{
    public static Instrumentation instrumentation;

    /**
     * The strategies instances.
     */
    private static MemoryMeterStrategies instance; 

    /**
     * Strategy relying on instrumentation or {@code null} if the instrumentation was not provided
     */
    private final MemoryMeterStrategy instrumentationStrategy;

    /**
     * Strategy relying on unsafe or {@code null} if unsafe could not be loaded
     */
    private final MemoryMeterStrategy unsafeStrategy;

    /**
     * Strategy relying on specification 
     */
    private final MemoryMeterStrategy specStrategy;

    /**
     * @param maybeInstrumentationStrategy
     * @param maybeUnsafeStrategy
     * @param specStrategy
     */
    private MemoryMeterStrategies(MemoryMeterStrategy instrumentationStrategy,
                                  MemoryMeterStrategy unsafeStrategy,
                                  MemoryMeterStrategy specStrategy)
    {
        this.instrumentationStrategy = instrumentationStrategy;
        this.unsafeStrategy = unsafeStrategy;
        this.specStrategy = specStrategy;
    }

    public static synchronized MemoryMeterStrategies getInstance()
    {
        if (instance == null)
            instance = createStrategies();

        return instance;
    }

    private static MemoryMeterStrategies createStrategies()
    {
        MemoryLayoutSpecification specification = MemoryLayoutSpecification.getEffectiveMemoryLayoutSpecification();

        MemoryMeterStrategy instrumentationStrategy = createInstrumentationStrategy();
        MemoryMeterStrategy unsafeStrategy = createUnsafeStrategy(specification);
        SpecStrategy createSpecStrategy = new SpecStrategy(specification);
        MemoryMeterStrategy specStrategy = createSpecStrategy;

        return new MemoryMeterStrategies(instrumentationStrategy, unsafeStrategy, specStrategy);
    }

    private static MemoryMeterStrategy createUnsafeStrategy(MemoryLayoutSpecification specification)
    {
        Unsafe unsafe = getUnsafe();
        return unsafe != null ? new UnsafeStrategy(specification, unsafe) : null;
    }

    private static MemoryMeterStrategy createInstrumentationStrategy()
    {
        return instrumentation != null ? new InstrumentationStrategy(instrumentation) : null;
    }

    private static Unsafe getUnsafe()
    {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasInstrumentation()
    {
        return instrumentationStrategy != null;
    }

    public boolean hasUnsafe()
    {
        return unsafeStrategy != null;
    }

    /**
     * 
     * @param guess
     * @return
     */
    public MemoryMeterStrategy getStrategy(Guess guess) {
        switch (guess) {
            case ALWAYS_UNSAFE:
                if (!hasUnsafe())
                    throw new IllegalStateException("sun.misc.Unsafe could not be obtained");
                return unsafeStrategy;
            case ALWAYS_SPEC:
                return specStrategy;
            default:
                if (!hasInstrumentation()) {
                    switch (guess) {
                        case ALWAYS_INSTRUMENTATION:
                            throw new IllegalStateException("Instrumentation is not set; Jamm must be set as -javaagent");
                        case FALLBACK_UNSAFE:
                            if (!hasUnsafe())
                                throw new IllegalStateException("Instrumentation is not set and sun.misc.Unsafe could not be obtained; Jamm must be set as -javaagent, or the SecurityManager must permit access to sun.misc.Unsafe");
                        case FALLBACK_BEST:
                            if (hasUnsafe())
                                return unsafeStrategy;
                        case FALLBACK_SPEC:
                            return specStrategy;
                    }
                }
                return instrumentationStrategy;
        }
    }
}
