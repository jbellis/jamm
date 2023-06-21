package org.github.jamm.jmh;

import java.lang.reflect.Array;
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
public class BenchmarkMeasureArray
{
    @Param({"INSTRUMENTATION", "INSTRUMENTATION_AND_SPEC", "SPEC", "UNSAFE"})
    private String guess;

    private MemoryMeter meter;

    private static Object[] arrays;

    static
    {
        try {

            Class<?>[] choices = new Class<?>[] {byte.class, int.class, double.class, Object.class};

            Random random = new Random();

            arrays = new Object[1000];
            for (int i = 0; i < arrays.length; i++) {
                arrays[i] = Array.newInstance(choices[random.nextInt(choices.length)], random.nextInt(100)); 
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Setup(Level.Iteration)
    public void setup() throws Exception
    {
        MemoryMeter.Guess guess = MemoryMeter.Guess.valueOf(this.guess);
        this.meter = MemoryMeter.builder().withGuessing(guess).build();
    }

    @Benchmark
    public void measure(Blackhole bh)
    {
        for (Object o : arrays)
            bh.consume(meter.measure(o));
    }
}
