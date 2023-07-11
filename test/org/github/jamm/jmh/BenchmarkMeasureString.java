package org.github.jamm.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.github.jamm.MemoryMeter;
//import org.github.jamm.MemoryMeterStrategy;
//import org.github.jamm.utils.ArrayMeasurementUtils;
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
        "-javaagent:target/jamm-0.4.0-SNAPSHOT.jar",
//        "--add-opens=java.base/java.lang=ALL-UNNAMED"
})
@Warmup(iterations=4, time=5)
@Measurement(iterations=5, time=5)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BenchmarkMeasureString
{
    @Param({"INSTRUMENTATION", "INSTRUMENTATION_AND_SPECIFICATION", "UNSAFE", "SPECIFICATION"})
    private String guess;

    private MemoryMeter meter;

    private static String[] strings;

    private long emptySize;

    static
    {
        try {
            Random random = new Random();

            String[] array = new String[1000];
            for (int i = 0; i < array.length; i++) {
                int length = random.nextInt(50);
                array[i] = random.ints('a', 'z' + 1)
                                 .limit(length)
                                 .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                 .toString();
            }
            strings = array;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Setup(Level.Iteration)
    public void setup() throws Exception {
        MemoryMeter.Guess guess = MemoryMeter.Guess.valueOf(this.guess);
        this.meter = MemoryMeter.builder().withGuessing(guess).build();
        emptySize = this.meter.measure("");
    }

    @Benchmark
    public void measureStringDeep(Blackhole bh) {
        for (String s : strings)
            bh.consume(meter.measureStringDeep(s));
    }
//
//    @Benchmark
//    public void measureReference(Blackhole bh) {
//        for (String s : strings) {
//            bh.consume(emptySize + ArrayMeasurementUtils.computeArraySize(MemoryMeterStrategy.MEMORY_LAYOUT.getArrayHeaderSize(), s.length(), Character.BYTES, MemoryMeterStrategy.MEMORY_LAYOUT.getObjectAlignment()));
//        }
//    }

    @Benchmark
    public void measureDeep(Blackhole bh) {
        for (String s : strings)
            bh.consume(meter.measureDeep(s));
    }
}
