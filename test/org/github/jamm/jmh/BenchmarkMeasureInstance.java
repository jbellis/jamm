package org.github.jamm.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.github.jamm.MemoryMeter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Threads(3)
@Fork(value = 1, jvmArgsPrepend = {
        "-javaagent:target/jamm-0.4.0-SNAPSHOT.jar"
})
@Warmup(iterations=4, time=5, timeUnit=TimeUnit.SECONDS)
@Measurement(iterations=5, time=5, timeUnit=TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkMeasureInstance
{
    @Param({"INSTRUMENTATION", "INSTRUMENTATION_AND_SPEC", "SPEC", "UNSAFE"})
    private String guess;

    private MemoryMeter meter;

    private static Object[] objects;

    static
    {
        try {
            Class<?>[] choices = new Class<?>[] {ClassWithoutFields.class,
                ClassWithOnePrimitiveFields.class,
                ClassWithOneObjectField.class,
                ClassWithTreeObjectFields.class,
                ClassWithOneObjectFieldAndTwoPrimitives.class,
                ClassWithFiveObjectFields.class};

                Random random = new Random();

                objects = new Object[1000];
                for (int i = 0; i < objects.length; i++) {
                    objects[i] = choices[random.nextInt(choices.length)].newInstance(); 
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Setup(Level.Iteration)
    public void setup() throws Exception {
        MemoryMeter.Guess guess = MemoryMeter.Guess.valueOf(this.guess);
        this.meter = MemoryMeter.builder().withGuessing(guess).build();
    }

    @Benchmark
    public void measure(Blackhole bh) {
        for (Object o : objects)
            bh.consume(meter.measure(o));
    }

    public static class ClassWithoutFields {
    }

    public static class ClassWithOnePrimitiveFields {
        int intField;
    }

    
    public static class ClassWithOneObjectField {
        Object field = new Object();
    }

    public static class ClassWithTreeObjectFields {
        Object first = new Object();

        Object second = new Object();

        String[] third = new String[] {"one", "two"};
    }

    public static class ClassWithOneObjectFieldAndTwoPrimitives {
        byte first;

        Object second = new Object();

        double third;
    }
    
    public static class ClassWithFiveObjectFields extends ClassWithTreeObjectFields {
        Object fourth = new Object();

        int[] fifth = new int[12];
    }
}
