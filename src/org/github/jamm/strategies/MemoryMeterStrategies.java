package org.github.jamm.strategies;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.github.jamm.MemoryLayoutSpecification;
import org.github.jamm.MemoryMeter.Guess;
import org.github.jamm.MemoryMeterStrategy;
import org.github.jamm.VM;

import sun.misc.Unsafe;

import static org.github.jamm.strategies.MethodHandleUtils.mayBeMethodHandle;

/**
 * The different strategies that can be used to measure object sizes.
 */
public final class MemoryMeterStrategies
{
    public static Instrumentation instrumentation;

    /**
     * The strategies instance.
     */
    private static MemoryMeterStrategies instance; 

    /**
     * Information about the memory layout used by the JVM running the code.
     */
    private final MemoryLayoutSpecification memoryLayoutSpecification;

    /**
     * Strategy relying on instrumentation or {@code null} if the instrumentation was not provided
     */
    private final MemoryMeterStrategy instrumentationStrategy;

    /**
     * Strategy relying on instrumentation to measure non array object and the SPEC approach to measure arrays.
     * {@code null} if the instrumentation was not provided
     */
    private final MemoryMeterStrategy instrumentationAndSpecStrategy;

    /**
     * Strategy relying on unsafe or {@code null} if unsafe could not be loaded
     */
    private final MemoryMeterStrategy unsafeStrategy;

    /**
     * Strategy relying on specification 
     */
    private final MemoryMeterStrategy specStrategy;

    private MemoryMeterStrategies(MemoryLayoutSpecification memoryLayoutSpecification,
                                  MemoryMeterStrategy instrumentationStrategy,
                                  MemoryMeterStrategy instrumentationAndSpecStrategy,
                                  MemoryMeterStrategy unsafeStrategy,
                                  MemoryMeterStrategy specStrategy) {

        this.memoryLayoutSpecification = memoryLayoutSpecification;
        this.instrumentationStrategy = instrumentationStrategy;
        this.instrumentationAndSpecStrategy = instrumentationAndSpecStrategy;
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

        Optional<MethodHandle> mayBeIsHiddenMH = mayBeIsHiddenMethodHandle();

        MemoryMeterStrategy instrumentationStrategy = createInstrumentationStrategy();
        MemoryMeterStrategy instrumentationAndSpecStrategy = createInstrumentationAndSpecStrategy(specification);
        MemoryMeterStrategy specStrategy = createSpecStrategy(specification, mayBeIsHiddenMH);
        MemoryMeterStrategy unsafeStrategy = createUnsafeStrategy(specification, mayBeIsHiddenMH, (MemoryLayoutBasedStrategy) specStrategy);

        // Logging important information once at startup for debugging purpose
        System.out.println("Jamm starting with: java.version='" + System.getProperty("java.version")
                            + "', java.vendor='" + System.getProperty("java.vendor")
                            + "', instrumentation=" + (instrumentationStrategy != null)
                            + ", unsafe=" + (unsafeStrategy != null)
                            + ", " + specification);
 
        return new MemoryMeterStrategies(specification, instrumentationStrategy, instrumentationAndSpecStrategy, unsafeStrategy, specStrategy);
    }

    private static MemoryMeterStrategy createSpecStrategy(MemoryLayoutSpecification specification,
                                                          Optional<MethodHandle> mayBeIsHiddenMH) {

        if (mayBeIsHiddenMH.isPresent() && !VM.useEmptySlotsInSuper())
            System.out.println("WARNING: Jamm is starting with the UseEmptySlotsInSupers JVM option disabled."
                               + " The memory layout created when this option is enabled cannot always be reproduced accurately by the SPEC or UNSAFE strategies."
                               + " By consequence the measured sizes when these strategies are used might be off in some cases.");

        // The Field layout was optimized in Java 15. For backward compatibility reasons, in 15+, the optimization can be disabled through the {@code -XX:-UseEmptySlotsInSupers} option.
        // (see https://bugs.openjdk.org/browse/JDK-8237767 and https://bugs.openjdk.org/browse/JDK-8239016)
        // Unfortunately, when {@code UseEmptySlotsInSupers} is disabled the layout resulting does not match the pre-15 versions
        return mayBeIsHiddenMH.isPresent() ? VM.useEmptySlotsInSuper() ? new SpecStrategy(specification)
                                                                       : new DoesNotUseEmptySlotInSuperSpecStrategy(specification)
                                           : new PreJava15SpecStrategy(specification);
    }

    private static MemoryMeterStrategy createUnsafeStrategy(MemoryLayoutSpecification specification,
                                                            Optional<MethodHandle> mayBeIsHiddenMH,
                                                            MemoryLayoutBasedStrategy specStrategy) {

        Unsafe unsafe = VM.getUnsafe();

        if (unsafe == null)
            return null;

        Optional<MethodHandle> mayBeIsRecordMH = mayBeIsRecordMethodHandle();

        // The hidden method was added in Java 15 so if isHidden exists we are on a version greater or equal to Java 15
        return mayBeIsHiddenMH.isPresent() ? new UnsafeStrategy(specification, unsafe, mayBeIsRecordMH.get(), mayBeIsHiddenMH.get(), specStrategy)
                                           : new PreJava15UnsafeStrategy(specification, unsafe, mayBeIsRecordMH, specStrategy);

    }

    /**
     * Returns the {@code MethodHandle} for the {@code Class.isHidden} method introduced in Java 15 if we are running
     * on a Java 15+ JVM.
     * @return an {@code Optional} for the {@code MethodHandle}
     */
    private static Optional<MethodHandle> mayBeIsHiddenMethodHandle() {
        return mayBeMethodHandle(Class.class, "isHidden");
    }

    /**
     * Returns the {@code MethodHandle} for the {@code Class.isRecord} method introduced in Java 14 if we are running
     * on a Java 14+ JVM.
     * @return an {@code Optional} for the {@code MethodHandle}
     */
    private static Optional<MethodHandle> mayBeIsRecordMethodHandle() {
        return mayBeMethodHandle(Class.class, "isRecord");
    }

    private static MemoryMeterStrategy createInstrumentationStrategy() {
        return instrumentation != null ? new InstrumentationStrategy(instrumentation) : null;
    }

    private static MemoryMeterStrategy createInstrumentationAndSpecStrategy(MemoryLayoutSpecification specification) {
        return instrumentation != null ? new InstrumentationAndSpecStrategy(specification, instrumentation) : null;
    }

    public boolean hasInstrumentation() {
        return instrumentationStrategy != null;
    }

    public boolean hasUnsafe() {
        return unsafeStrategy != null;
    }

    /**
     * Provides information about the memory layout used by the JVM.
     * @return information about the memory layout used by the JVM
     */
    public MemoryLayoutSpecification getMemoryLayoutSpecification() {
        return this.memoryLayoutSpecification;
    }

    public MemoryMeterStrategy getStrategy(List<Guess> guessList) {

        Queue<Guess> guesses = new LinkedList<>(guessList);

        while (true) {

            Guess guess = guesses.poll();

            if (guess.requireInstrumentation()) {

                if (hasInstrumentation())
                    return guess == Guess.INSTRUMENTATION_AND_SPEC ? instrumentationAndSpecStrategy
                                                                   : instrumentationStrategy;

                if (guesses.isEmpty())
                    throw new IllegalStateException("Instrumentation is not set; Jamm must be set as -javaagent");

            } else if (guess.requireUnsafe()) {

                if (hasUnsafe())
                    return unsafeStrategy;

                if (guesses.isEmpty())
                    throw new IllegalStateException("sun.misc.Unsafe could not be obtained. The SecurityManager must permit access to sun.misc.Unsafe");

            } else {

                return specStrategy;
            }
        }
    }
}
