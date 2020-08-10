package org.github.jamm.jmh;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.github.jamm.MemoryMeter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
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
        "-Xms16g", "-Xmx16g",
        "-XX:+UseG1GC",
        "-XX:+AlwaysPreTouch"
})
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 3, time = 1)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class Microbench
{
    @Param({ "ALWAYS_SPEC", "ALWAYS_UNSAFE", "NEVER" })
    private String guess;

    private MemoryMeter meter;

    private String valString;
    private ByteBuffer heapByteBuffer;
    private Cls1 cls1;
    private Cls1 cls2;
    private Cls1 cls3;
    private byte[] bytes;
    private long[] longs;
    private Object[] objects;

    private ClsX deeplyNested;
    @Param({ "100" })
    private int nested;
    @Param({ "4" })
    private int refs;

    @Setup
    public void setup()
    {
        MemoryMeter.Guess guess = MemoryMeter.Guess.valueOf(this.guess);
        this.meter = new MemoryMeter().withGuessing(guess);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++)
            sb.append('a');
        valString = sb.toString();

        heapByteBuffer = ByteBuffer.allocate(300);

        cls1 = new Cls1();
        cls2 = new Cls2();
        cls3 = new Cls3();

        bytes = new byte[200];
        longs = new long[200];
        objects = new Object[50];
        for (int i = 0; i < objects.length; i++)
            objects[i] = new Object();

        ClsX last = null;
        for (int i = 0; i < nested; i++)
        {
            ClsX o = new ClsX();
            if (last == null)
                deeplyNested = o;
            else
                last.objs.add(o);
            last = o;

            for (int j = 0; j < refs; j++)
                o.objs.add(new ClsX());
        }
    }

    //    @Benchmark
    //    @Measurement(iterations = 1, time = 1)
    //    public void baseline(Blackhole bh)
    //    {
    //        bh.consume(0);
    //    }

    @Benchmark
    public void deeplyNested(Blackhole bh)
    {
        bh.consume(meter.measureDeep(deeplyNested));
    }

    @Benchmark
    public void justString(Blackhole bh)
    {
        bh.consume(meter.measureDeep(valString));
    }

    @Benchmark
    public void justByteBuffer(Blackhole bh)
    {
        bh.consume(meter.measureDeep(heapByteBuffer));
    }

    @Benchmark
    public void justByteArray(Blackhole bh)
    {
        bh.consume(meter.measure(bytes));
    }

    //    @Benchmark
    //    public void justLongArray(Blackhole bh)
    //    {
    //        bh.consume(meter.measure(longs));
    //    }

    //    @Benchmark
    //    public void justObjectArray(Blackhole bh)
    //    {
    //        bh.consume(meter.measure(objects));
    //    }

    @Benchmark
    public void cls1(Blackhole bh)
    {
        bh.consume(meter.measureDeep(cls1));
    }

    @Benchmark
    public void cls2(Blackhole bh)
    {
        bh.consume(meter.measureDeep(cls2));
    }

    @Benchmark
    public void cls3(Blackhole bh)
    {
        bh.consume(meter.measureDeep(cls3));
    }
}

@SuppressWarnings("unused")
class ClsX
{
    List<ClsX> objs = new ArrayList<>();
}

@SuppressWarnings("unused")
class Cls1
{
    int i;
    long l;
}

@SuppressWarnings("unused")
class Cls2 extends Cls1
{
    Object ref = "foop" + System.nanoTime();
    Object[] array = new Object[]{ "abc" + System.nanoTime(), "def" + System.nanoTime(), "ghi" + System.nanoTime() };
}

@SuppressWarnings("unused")
class Cls3 extends Cls2
{
    Object more = new Cls2();
}

