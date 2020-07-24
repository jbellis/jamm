package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.util.function.Predicate;

public abstract class MemoryMeter {
    static final MemoryLayoutSpecification SPEC = MemoryLayoutSpecification.getEffectiveMemoryLayoutSpecification();

    public static void premain(String options, Instrumentation inst) {
        MemoryMeterInstrumentation.instrumentation = inst;
    }

    public static void agentmain(String options, Instrumentation inst) {
    	MemoryMeterInstrumentation.instrumentation = inst;
    }

    public enum Guess {
        /**
         * Chose the best implementation in this order:
         * JEP/JDK-8249196, instrumentation, unsafe, spec. Note: the guessed spec is allowed to overcount.
         */
        BEST {
            @Override
            MemoryMeter createMeter(Builder builder)
            {
                if (MemoryMeterInstrumentation.hasInstrumentation())
                    return new MemoryMeterInstrumentation(builder);
                if (MemoryMeterUnsafe.hasUnsafe())
                    return new MemoryMeterUnsafe(builder);
                return new MemoryMeterSpec(builder);
            }
        },
        /**
         * If instrumentation is not available, error when measuring
         *
         * @deprecated use {@link #ALWAYS_INSTRUMENTATION} instead
         */
        @Deprecated
        NEVER {
            @Override
            MemoryMeter createMeter(Builder builder)
            {
                if (MemoryMeterInstrumentation.hasInstrumentation())
                    return new MemoryMeterInstrumentation(builder);
                throw new IllegalStateException("Instrumentation is not set; Jamm must be set as -javaagent");
            }
        },
        /**
         * If instrumentation is available, use it, otherwise guess the size using predefined specifications. Note: the guessed spec is allowed to overcount.
         *
         * @deprecated Using spec (specification, actually: speculation) is strongly discouraged
         */
        @Deprecated
        FALLBACK_SPEC {
            @Override
            MemoryMeter createMeter(Builder builder)
            {
                if (MemoryMeterInstrumentation.hasInstrumentation())
                    return new MemoryMeterInstrumentation(builder);
                return new MemoryMeterSpec(builder);
            }
        },
        /**
         * If instrumentation is available, use it, otherwise guess the size using sun.misc.Unsafe
         *
         * @deprecated Using {@code sun.misc.Unsafe} is strongly discouraged
         */
        @Deprecated
        FALLBACK_UNSAFE {
            @Override
            MemoryMeter createMeter(Builder builder)
            {
                if (MemoryMeterInstrumentation.hasInstrumentation())
                    return new MemoryMeterInstrumentation(builder);
                if (MemoryMeterUnsafe.hasUnsafe())
                    return new MemoryMeterUnsafe(builder);
                throw new IllegalStateException("Instrumentation is not set and sun.misc.Unsafe could not be obtained; Jamm must be set as -javaagent, or the SecurityManager must permit access to sun.misc.Unsafe");
            }
        },
        /**
         * If instrumentation is available, use it, otherwise guess the size using sun.misc.Unsafe; if that is unavailable,
         * guess using predefined specifications. Note: the guessed spec is allowed to overcount.
         *
         * @deprecated Using {@code sun.misc.Unsafe} is strongly discouraged, using spec (specification,
         * actually: speculation) is strongly discouraged
         */
        @Deprecated
        FALLBACK_BEST {
            @Override
            MemoryMeter createMeter(Builder builder)
            {
                if (MemoryMeterInstrumentation.hasInstrumentation())
                    return new MemoryMeterInstrumentation(builder);
                if (MemoryMeterUnsafe.hasUnsafe())
                    return new MemoryMeterUnsafe(builder);
                return new MemoryMeterSpec(builder);
            }
        },
        /**
         * Always guess the size of measured objects using predefined specifications. Note: the guessed spec is allowed to overcount.
         *
         * @deprecated Using spec (specification, actually: speculation) is strongly discouraged
         */
        @Deprecated
        ALWAYS_SPEC {
            @Override
            MemoryMeter createMeter(Builder builder)
            {
                return new MemoryMeterSpec(builder);
            }
        },
        /**
         * Always guess the size of measured objects using {@code sun.misc.Unsafe}
         *
         * @deprecated Using {@code sun.misc.Unsafe} is strongly discouraged
         */
        @Deprecated
        ALWAYS_UNSAFE {
            @Override
            MemoryMeter createMeter(Builder builder)
            {
                if (MemoryMeterUnsafe.hasUnsafe())
                    return new MemoryMeterUnsafe(builder);
                throw new IllegalStateException("Unsafe not available; the SecurityManager must permit access to sun.misc.Unsafe");
            }
        },
        /**
         * Always guess the size of measured objects using instrumentation
         */
        ALWAYS_INSTRUMENTATION {
            @Override
            MemoryMeter createMeter(Builder builder)
            {
                if (MemoryMeterInstrumentation.hasInstrumentation())
                    return new MemoryMeterInstrumentation(builder);
                throw new IllegalStateException("Instrumentation is not set; Jamm must be set as -javaagent");
            }
        };

        abstract MemoryMeter createMeter(Builder builder);
    }

    final Guess guess;
    final byte byteBufferMode;
    final boolean ignoreOuterClassReference;
    final boolean ignoreKnownSingletons;
    final boolean ignoreNonStrongReferences;
    final boolean ignoreDontMeasure;

    final Predicate<Class<?>> ignoreClassPredicate;
    final ClassValue<Boolean> ignoreClass = new ClassValue<Boolean>()
    {
        @Override
        protected Boolean computeValue(Class<?> type)
        {
            return ignoreClassPredicate.test(type);
        }
    };

    public static Builder builder() {
        return new Builder();
    }

    public Builder unbuild() {
        return new Builder(guess,
                           byteBufferMode,
                           ignoreOuterClassReference,
                           ignoreKnownSingletons,
                           ignoreNonStrongReferences,
                           ignoreDontMeasure
        );
    }

    static final byte BB_MODE_NORMAL = 0;
    static final byte BB_MODE_OMIT_SHARED = 1;
    static final byte BB_MODE_SHALLOW = 2;
    static final byte BB_MODE_HEAP_ONLY_NO_SLICE = 3;

    public static class Builder {
        private Guess guess = Guess.BEST;
        private byte byteBufferMode = BB_MODE_NORMAL;
        private boolean ignoreOuterClassReference;
        private boolean ignoreKnownSingletons;
        private boolean ignoreNonStrongReferences;
        private boolean ignoreDontMeasure;

        private Builder() {

        }

        private Builder(Guess guess,
                        byte byteBufferMode,
                        boolean ignoreOuterClassReference,
                        boolean ignoreKnownSingletons,
                        boolean ignoreNonStrongReferences,
                        boolean ignoreDontMeasure) {
            this.guess = guess;
            this.byteBufferMode = byteBufferMode;
            this.ignoreOuterClassReference = ignoreOuterClassReference;
            this.ignoreKnownSingletons = ignoreKnownSingletons;
            this.ignoreNonStrongReferences = ignoreNonStrongReferences;
            this.ignoreDontMeasure = ignoreDontMeasure;
        }

        public MemoryMeter build() {
            return guess.createMeter(this);
        }

        /**
         * Only counts the bytes remaining in a {@link java.nio.ByteBuffer}
         * in measureDeep, rather than the full size of the backing array.
         * TODO: handle other types of Buffers
         */
        public Builder omitSharedBufferOverhead() {
            byteBufferMode = BB_MODE_OMIT_SHARED;
            return this;
        }

        /**
         * Only count the shallow size of a {@link java.nio.ByteBuffer}
         */
        public Builder onlyShallowByteBuffers() {
            byteBufferMode = BB_MODE_SHALLOW;
            return this;
        }

        /**
         * Special Apache Cassandra handling for {@link java.nio.ByteBuffer}.
         * <ol>
         * <li>If the {@link java.nio.ByteBuffer} is a direct buffer, only count its shallow size.</li>
         * <li>If the {@link java.nio.ByteBuffer}'s capacity capacity is bigger than the remaining
         * number of bytes, only cound the remaining number of bytes, but not the shallow size.</li>
         * <li>Otherwise count the whole object tree</li>
         * </ol>
         *
         */
        public Builder byteBuffersHeapOnlyNoSlice() {
            byteBufferMode = BB_MODE_HEAP_ONLY_NO_SLICE;
            return this;
        }

        /**
         * Special mode to ignore the {@link Unmetered} annotation.
         */
        public Builder ignoreDontMeasure() {
            this.ignoreDontMeasure = true;
            return this;
        }

        /**
         * See {@link Guess} for possible guess-modes.
         */
        public Builder withGuessing(Guess guess) {
            this.guess = guess;
            return this;
        }

        /**
         * ignores the size of an outer class reference
         * <em>DOES NOT WORK WITH {@code j.l.Runtime.sizeOf()} / JEP/JDK-8249196 !!</em>
         * @deprecated not supported w/ JEP/JDK-8249196 ({@code j.l.Runtime.deepSizeOf(Object)}}
         */
        @Deprecated
        public Builder ignoreOuterClassReference() {
            this.ignoreOuterClassReference = true;
            return this;
        }

        /**
         * ignores space occupied by known singletons such as {@link Class} objects and {@code enum}s
         */
        public Builder ignoreKnownSingletons() {
            this.ignoreKnownSingletons = true;
            return this;
        }

        /**
         * Ignores the references from a {@link java.lang.ref.Reference} (like weak/soft/phantom references).
         *
         * <em>Don't use this!</em> Whether a reference is "live" or not is something that can only be
         * determined by the JVM itself. Using this mode can keep a weak/soft/phantom reference alive,
         * which is probably not intended!
         *
         * @deprecated Whether a reference is "live" or not is something that can only be determined by the JVM itself.
         */
        @Deprecated
        public Builder ignoreNonStrongReferences() {
            ignoreNonStrongReferences = true;
            return this;
        }
    }

    MemoryMeter(Builder builder) {
        this.guess = builder.guess;
        this.byteBufferMode = builder.byteBufferMode;
        this.ignoreOuterClassReference = builder.ignoreOuterClassReference;
        this.ignoreKnownSingletons = builder.ignoreKnownSingletons;
        this.ignoreNonStrongReferences = builder.ignoreNonStrongReferences;
        this.ignoreDontMeasure = builder.ignoreDontMeasure;

        Predicate<Class<?>> pred = c -> false;
        if (ignoreKnownSingletons)
            pred = pred.or(MemoryMeter::checkKnownSingleton);
        if (!ignoreDontMeasure)
            pred = pred.or(MemoryMeter::isAnnotationPresent);
        this.ignoreClassPredicate = pred;
    }

    private static boolean checkKnownSingleton(Class<?> cls)
    {
        return cls == Class.class || cls.isEnum() || Thread.class.isAssignableFrom(cls);
    }

    private static boolean isAnnotationPresent(Class<?> cls) {
        if (cls == null)
            return false;

        if (cls.isAnnotationPresent(Unmetered.class))
            return true;

        Class<?>[] interfaces = cls.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (isAnnotationPresent(cls.getInterfaces()[i]))
                return true;
        }

        return isAnnotationPresent(cls.getSuperclass());
    }

    public Guess getGuess()
    {
        return guess;
    }

    public String toString()
    {
        return getClass().getName() + "{" +
               "byteBufferMode=" + byteBufferMode +
               ", guess=" + guess +
               ", ignoreOuterClassReference=" + ignoreOuterClassReference +
               ", ignoreKnownSingletons=" + ignoreKnownSingletons +
               ", ignoreNonStrongReferences=" + ignoreNonStrongReferences +
               ", ignoreDontMeasure=" + ignoreDontMeasure +
               ", spec=" + SPEC +
               '}';
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(byte[] bytes) {
        return sizeOfArray(bytes.length, byte.class);
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(short[] shorts) {
        return sizeOfArray(shorts.length, short.class);
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(char[] chars) {
        return sizeOfArray(chars.length, char.class);
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(int[] ints) {
        return sizeOfArray(ints.length, int.class);
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(long[] longs) {
        return sizeOfArray(longs.length, long.class);
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(float[] floats) {
        return sizeOfArray(floats.length, float.class);
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(double[] doubles) {
        return sizeOfArray(doubles.length, double.class);
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(Object[] objects) {
        return sizeOfArray(objects.length, Object.class);
    }

    @SuppressWarnings("unused")
    public long sizeOfArray(int length, Class<?> type) {
        long sz = SPEC.getArrayHeaderSize();
        sz += (long) sizeOfField(type) * (long) length;
        return roundTo(sz, SPEC.getObjectAlignment());
    }

    /**
     * @return the shallow memory usage of @param object
     * @throws NullPointerException if object is null
     */
    public abstract long measure(Object object);

    /**
     * @return the memory usage of @param object including referenced objects
     * @throws NullPointerException if object is null
     */
    public abstract long measureDeep(Object object);

    private static final Class<?> clsJLRModule;
    private static final Class<?> clsJLMModuleDescriptor;
    private static final Class<?> clsJLRAccessibleObject;
    private static final Class<?> clsSRAAnnotationInvocationHandler;
    private static final Class<?> clsSRAAnnotationType;
    private static final Class<?> clsJIRUnsafeFieldAccessorImpl;
    private static final Class<?> clsJIRDelegatingMethodAccessorImpl;
    static
    {
        clsJLRModule = maybeGetClass("java.lang.reflect.Module");
        clsJLMModuleDescriptor = maybeGetClass("java.lang.module.ModuleDescriptor");
        clsJLRAccessibleObject = maybeGetClass("java.lang.reflect.AccessibleObject");
        clsSRAAnnotationInvocationHandler = maybeGetClass("sun.reflect.annotation.AnnotationInvocationHandler");
        clsSRAAnnotationType = maybeGetClass("sun.reflect.annotation.AnnotationType");
        clsJIRUnsafeFieldAccessorImpl = maybeGetClass("jdk.internal.reflect.UnsafeFieldAccessorImpl");
        clsJIRDelegatingMethodAccessorImpl = maybeGetClass("jdk.internal.reflect.DelegatingMethodAccessorImpl");
    }

    private static Class<?> maybeGetClass(String name)
    {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    static boolean skipClass(Class<?> cls) {
        return cls == null || cls == Class.class
               || cls == clsJLRModule || cls == clsJLMModuleDescriptor || cls == clsJLRAccessibleObject
               || cls == clsSRAAnnotationInvocationHandler || cls == clsSRAAnnotationType
               || cls == clsJIRUnsafeFieldAccessorImpl || cls == clsJIRDelegatingMethodAccessorImpl;
    }

    /**
     * @return The memory size of a field of a class of the provided type; for Objects this is the size of the reference only
     */
    static int sizeOfField(Class<?> type) {
        if (!type.isPrimitive())
            return SPEC.getReferenceSize();
        if (type == boolean.class || type == byte.class)
            return 1;
        else if (type == char.class || type == short.class)
            return 2;
        else if (type == float.class || type == int.class)
            return 4;
        else if (type == double.class || type == long.class)
            return 8;
        throw new IllegalStateException();
    }

    static long roundTo(long x, int multiple) {
        return ((x + multiple - 1) / multiple) * multiple;
    }
}

