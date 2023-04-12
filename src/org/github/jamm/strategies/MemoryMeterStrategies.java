package org.github.jamm.strategies;

import java.lang.annotation.Annotation;
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
     * The strategies instance.
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

        Class<? extends Annotation> contendedClass = loadContendedClass();
        Optional<MethodHandle> mayBeIsHiddenMH = mayBeIsHiddenMethodHandle();

        MemoryMeterStrategy instrumentationStrategy = createInstrumentationStrategy();
        MemoryMeterStrategy specStrategy = createSpecStrategy(specification, mayBeIsHiddenMH, contendedClass);
        MemoryMeterStrategy unsafeStrategy = createUnsafeStrategy(specification, contendedClass, mayBeIsHiddenMH, (MemoryLayoutBasedStrategy) specStrategy);

        // Logging important information once at startup for debugging purpose
        System.out.println("Jamm starting with: java.version='" + System.getProperty("java.version")
                            + "', java.vendor='" + System.getProperty("java.vendor")
                            + "', instrumentation=" + (instrumentationStrategy != null)
                            + ", unsafe=" + (unsafeStrategy != null)
                            + ", " + specification);
 
        return new MemoryMeterStrategies(instrumentationStrategy, unsafeStrategy, specStrategy);
    }

    private static MemoryMeterStrategy createSpecStrategy(MemoryLayoutSpecification specification,
                                                          Optional<MethodHandle> mayBeIsHiddenMH,
                                                          Class<? extends Annotation> contendedClass) {

        if (mayBeIsHiddenMH.isPresent() && !VM.useEmptySlotsInSuper())
            System.out.println("WARNING: Jamm is starting with the UseEmptySlotsInSupers JVM option disabled."
                               + " The memory layout created when this option is enabled cannot always be reproduced accurately by the SPEC or UNSAFE strategies."
                               + " By consequence the measured sizes when these strategies are used might be off in some cases.");

        // @Contended was introduced in Java 8 as {@code sun.misc.Contended} but was repackaged in the jdk.internal.vm.annotation package in Java 9.
        // Therefore in Java 9+ unless '-XX:-RestrictContended' or '--add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED' is specified we will not have access
        // to the value() method of @Contended and will be unable to retrieve the contention group tags and might be unable to computes the correct sizes.
        // Nevertheless, it also means that only the internal Java classes will use that annotation and we know which one they are. Therefore we can rely on this fact to mitigate the problem.
        Optional<MethodHandle> mayBeContendedValueMH = mayBeMethodHandle(contendedClass, "value");

        // The Field layout was optimized in Java 15. For backward compatibility reasons, in 15+, the optimization can be disabled through the {@code -XX:-UseEmptySlotsInSupers} option.
        // (see https://bugs.openjdk.org/browse/JDK-8237767 and https://bugs.openjdk.org/browse/JDK-8239016)
        // Unfortunately, when {@code UseEmptySlotsInSupers} is disabled the layout resulting does not match the pre-15 versions
        return mayBeIsHiddenMH.isPresent() ? VM.useEmptySlotsInSuper() ? new SpecStrategy(specification, contendedClass, mayBeContendedValueMH)
                                                                       : new DoesNotUseEmptySlotInSuperSpecStrategy(specification, contendedClass, mayBeContendedValueMH)
                                           : new PreJava15SpecStrategy(specification, contendedClass, mayBeContendedValueMH);
    }

    private static MemoryMeterStrategy createUnsafeStrategy(MemoryLayoutSpecification specification,
                                                            Class<? extends Annotation> contendedClass,
                                                            Optional<MethodHandle> mayBeIsHiddenMH,
                                                            MemoryLayoutBasedStrategy specStrategy) {

        Unsafe unsafe = VM.getUnsafe();

        if (unsafe == null)
            return null;

        Optional<MethodHandle> mayBeIsRecordMH = mayBeIsRecordMethodHandle();

        // The hidden method was added in Java 15 so if isHidden exists we are on a version greater or equal to Java 15
        return mayBeIsHiddenMH.isPresent() ? new UnsafeStrategy(specification, unsafe, contendedClass, mayBeIsRecordMH.get(), mayBeIsHiddenMH.get(), specStrategy)
                                           : new PreJava15UnsafeStrategy(specification, unsafe, contendedClass, mayBeIsRecordMH, specStrategy);

    }

    /**
     * Returns the {@code MethodHandle} for the {@code Class.isHidden} method introduced in Java 15 if we are running
     * on a Java 15+ JVM.
     * @return an {@code Optional} for the {@code MethodHandle}
     */
    private static Optional<MethodHandle> mayBeIsHiddenMethodHandle()
    {
        return mayBeMethodHandle(Class.class, "isHidden");
    }

    /**
     * Returns the {@code MethodHandle} for the {@code Class.isRecord} method introduced in Java 14 if we are running
     * on a Java 14+ JVM.
     * @return an {@code Optional} for the {@code MethodHandle}
     */
    private static Optional<MethodHandle> mayBeIsRecordMethodHandle()
    {
        return mayBeMethodHandle(Class.class, "isRecord");
    }

    /**
     * Returns the {@code MethodHandle} for the specified class and method if the method exists.
     *
     * @param klass the class
     * @param methodName the method name
     * @return an {@code Optional} for the {@code MethodHandle}
     */
    private static Optional<MethodHandle> mayBeMethodHandle(Class<?> klass, String methodName)
    {
        try {
            Method method = klass.getMethod(methodName, new Class[0]);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return Optional.of(lookup.unreflect(method));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static MemoryMeterStrategy createInstrumentationStrategy() {
        return instrumentation != null ? new InstrumentationStrategy(instrumentation) : null;
    }

    /**
     * Load the {@code Contended} class.
     * @return the {@code Contended} class.
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadContendedClass()
    {
        try {
            return (Class<? extends Annotation>) Class.forName("sun.misc.Contended");
        } catch (ClassNotFoundException e) {
            try {
                return (Class<? extends Annotation>) Class.forName("jdk.internal.vm.annotation.Contended");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("The Contended annotation class could not be loaded.", ex);
            }
        }
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
