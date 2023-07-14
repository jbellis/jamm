package org.github.jamm.strategies;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.github.jamm.MemoryMeter.Guess;

import static org.github.jamm.utils.MethodHandleUtils.mayBeMethodHandle;

import org.github.jamm.MemoryMeterStrategy;
import org.github.jamm.VM;

/**
 * The different strategies that can be used to measure object sizes.
 */
public final class MemoryMeterStrategies {

    public static Instrumentation instrumentation;

    /**
     * The strategies instance.
     */
    private static MemoryMeterStrategies instance; 

    /**
     * Strategy relying on instrumentation or {@code null} if the instrumentation was not provided
     */
    private final MemoryMeterStrategy instrumentationStrategy;

    /**
     * Strategy relying on instrumentation to measure non array object and the {@code Specification} approach to measure arrays.
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

    private MemoryMeterStrategies(MemoryMeterStrategy instrumentationStrategy,
                                  MemoryMeterStrategy instrumentationAndSpecStrategy,
                                  MemoryMeterStrategy unsafeStrategy,
                                  MemoryMeterStrategy specStrategy) {

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

        Optional<MethodHandle> mayBeIsHiddenMH = mayBeIsHiddenMethodHandle();

        MemoryMeterStrategy instrumentationStrategy = createInstrumentationStrategy();
        MemoryMeterStrategy instrumentationAndSpecStrategy = createInstrumentationAndSpecStrategy();
        MemoryMeterStrategy specStrategy = createSpecStrategy(mayBeIsHiddenMH);
        MemoryMeterStrategy unsafeStrategy = createUnsafeStrategy(mayBeIsHiddenMH, (MemoryLayoutBasedStrategy) specStrategy);

        if (logInformationAtStartup()) {
        // Logging important information once at startup for debugging purpose
            System.out.println("Jamm starting with: java.version='" + System.getProperty("java.version")
                                + "', java.vendor='" + System.getProperty("java.vendor")
                                + "', instrumentation=" + (instrumentationStrategy != null)
                                + ", unsafe=" + (unsafeStrategy != null)
                                + ", " + MemoryMeterStrategy.MEMORY_LAYOUT);
        }
 
        return new MemoryMeterStrategies(instrumentationStrategy, instrumentationAndSpecStrategy, unsafeStrategy, specStrategy);
    }

    /**
     * Checks if layout and JDK information should be logged at startup.
     * <p>{@code false} by default to avoid causing issues to existing applications that do not expect that message to be logged.</p>
     * @return {@code true} if the information should be logged, {@code false} otherwise.
     */
    private static boolean logInformationAtStartup() {
        return Boolean.parseBoolean(System.getProperty("org.github.jamm.strategies.LogInfoAtStartup", "false"));
    }

    private static MemoryMeterStrategy createSpecStrategy(Optional<MethodHandle> mayBeIsHiddenMH) {

        if (mayBeIsHiddenMH.isPresent() && !VM.useEmptySlotsInSuper())
            System.out.println("WARNING: Jamm is starting with the UseEmptySlotsInSupers JVM option disabled."
                               + " The memory layout created when this option is enabled cannot always be reproduced accurately by the Specification or UNSAFE strategies."
                               + " By consequence the measured sizes when these strategies are used might be off in some cases.");

        // The Field layout was optimized in Java 15. For backward compatibility reasons, in 15+, the optimization can be disabled through the {@code -XX:-UseEmptySlotsInSupers} option.
        // (see https://bugs.openjdk.org/browse/JDK-8237767 and https://bugs.openjdk.org/browse/JDK-8239016)
        // Unfortunately, when {@code UseEmptySlotsInSupers} is disabled the layout resulting does not match the pre-15 versions
        return mayBeIsHiddenMH.isPresent() ? VM.useEmptySlotsInSuper() ? new SpecStrategy()
                                                                       : new DoesNotUseEmptySlotInSuperSpecStrategy()
                                           : new PreJava15SpecStrategy();
    }

    private static MemoryMeterStrategy createUnsafeStrategy(Optional<MethodHandle> mayBeIsHiddenMH,
                                                            MemoryLayoutBasedStrategy specStrategy) {
        if (!VM.hasUnsafe())
            return null;

        Optional<MethodHandle> mayBeIsRecordMH = mayBeIsRecordMethodHandle();

        // The hidden method was added in Java 15 so if isHidden exists we are on a version greater or equal to Java 15
        return mayBeIsHiddenMH.isPresent() ? new UnsafeStrategy(mayBeIsRecordMH.get(), mayBeIsHiddenMH.get(), specStrategy)
                                           : new PreJava15UnsafeStrategy(mayBeIsRecordMH, specStrategy);

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

    private static MemoryMeterStrategy createInstrumentationAndSpecStrategy() {
        return instrumentation != null ? new InstrumentationAndSpecStrategy(instrumentation) : null;
    }

    public boolean hasInstrumentation() {
        return instrumentationStrategy != null;
    }

    public boolean hasUnsafe() {
        return unsafeStrategy != null;
    }

    public MemoryMeterStrategy getStrategy(List<Guess> guessList) {

        if (guessList.isEmpty())
            throw new IllegalArgumentException("The guessList argument is empty");

        Guess.checkOrder(guessList);

        Queue<Guess> guesses = new LinkedList<>(guessList);

        while (true) {

            Guess guess = guesses.poll();

            if (guess.requireInstrumentation()) {

                if (hasInstrumentation())
                    return guess == Guess.INSTRUMENTATION_AND_SPECIFICATION ? instrumentationAndSpecStrategy
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
