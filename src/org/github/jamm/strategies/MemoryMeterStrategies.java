package org.github.jamm.strategies;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Optional;

import org.github.jamm.MemoryLayoutSpecification;
import org.github.jamm.MemoryMeter.Guess;
import org.github.jamm.MemoryMeterStrategy;
import org.github.jamm.VM;

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

    private MemoryMeterStrategies(MemoryMeterStrategy instrumentationStrategy,
                                  MemoryMeterStrategy unsafeStrategy,
                                  MemoryMeterStrategy specStrategy) {

        this.instrumentationStrategy = instrumentationStrategy;
        this.unsafeStrategy = unsafeStrategy;
        this.specStrategy = specStrategy;
    }

    public static synchronized MemoryMeterStrategies getInstance() {

        if (instance == null)
            instance = createStrategies();

        return instance;
    }

    /**
     * Creates the strategies available based on the JVM information.
     * @return the strategies available
     */
    private static MemoryMeterStrategies createStrategies() {

        MemoryLayoutSpecification specification = MemoryLayoutSpecification.getEffectiveMemoryLayoutSpecification();

        Optional<MethodHandle> mayBeIsHiddenMethodHandle = mayBeIsHiddenMethodHandle();

        MemoryMeterStrategy instrumentationStrategy = createInstrumentationStrategy();
        MemoryMeterStrategy specStrategy = createSpecStrategy(specification, mayBeIsHiddenMethodHandle);
        MemoryMeterStrategy unsafeStrategy = createUnsafeStrategy(specification, mayBeIsHiddenMethodHandle, specStrategy);


        return new MemoryMeterStrategies(instrumentationStrategy, unsafeStrategy, specStrategy);
    }

    private static MemoryMeterStrategy createSpecStrategy(MemoryLayoutSpecification specification, 
                                                          Optional<MethodHandle> mayBeIsHiddenMethodHandle) {

        return mayBeIsHiddenMethodHandle.isPresent() ? new SpecStrategy(specification) : new PreJava15SpecStrategy(specification);
    }

    private static MemoryMeterStrategy createUnsafeStrategy(MemoryLayoutSpecification specification, 
                                                            Optional<MethodHandle> mayBeIsHiddenMH,
                                                            MemoryMeterStrategy specStrategy) {

        Unsafe unsafe = VM.getUnsafe();

        if (unsafe == null)
            return null;

        // The hidden method was added in Java 15 so if isHidden exists we are on a version greater or equal to Java 15
        return mayBeIsHiddenMH.isPresent() ? new UnsafeStrategy(specification, unsafe, mayBeIsHiddenMH.get(), (SpecStrategy) specStrategy)
                                           : new PreJava15UnsafeStrategy(specification, unsafe);

    }

    /**
     * Returns the {@code MethodHandle} for the {@code Class.isHidden} method introduced in Java 15 if we are running
     * on a Java 15+ JVM.
     * @return an {@code Optional} for the {@code MethodHandle}
     */
    private static Optional<MethodHandle> mayBeIsHiddenMethodHandle()
    {
        try {

            Method method = Class.class.getMethod("isHidden", new Class[0]);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return Optional.of(lookup.unreflect(method));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static MemoryMeterStrategy createInstrumentationStrategy() {
        return instrumentation != null ? new InstrumentationStrategy(instrumentation) : null;
    }

    public boolean hasInstrumentation() {
        return instrumentationStrategy != null;
    }

    public boolean hasUnsafe() {
        return unsafeStrategy != null;
    }

    @SuppressWarnings("incomplete-switch")
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
