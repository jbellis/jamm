package org.github.jamm.jmh;

import java.util.concurrent.TimeUnit;

import org.github.jamm.FieldAndClassFilter;
import org.github.jamm.FieldFilter;
import org.github.jamm.Filters;
import org.github.jamm.MemoryMeter;
import org.github.jamm.MemoryMeterStrategy;
import org.github.jamm.NoopMemoryMeterListener;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

@Threads(3)
@Fork(value = 1, jvmArgsPrepend = {
        "-javaagent:target/jamm-0.4.0-SNAPSHOT.jar",
        "-Xms16g", "-Xmx16g",
        "-XX:+UseG1GC",
        "-XX:+AlwaysPreTouch"
})
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BenchmarkObjectGraphTraversal
{
    private MemoryMeter meter;

    /**
     * The object being measured
     */
    private Object obj;

    @Setup(Level.Iteration)
    public void setup() throws Exception
    {
        MemoryMeterStrategy strategy = o -> 1;
        FieldFilter fieldFilter = Filters.getFieldFilters(false, false, false);
        FieldAndClassFilter classFilter = Filters.getClassFilters(false);
        this.meter = new MemoryMeter(strategy, classFilter, fieldFilter, false, NoopMemoryMeterListener.FACTORY);

        this.obj = new ClassWithFiveObjectFields(new byte[20],
                                                 new ClassWithOneObjectFieldsAndTwoPrimitives((byte) 8, new ClassWithoutFields(), Double.MAX_VALUE),
                                                 new ClassWithOneObjectField(new Object[] {"test", new ClassWithOneObjectField(new Object()), new int[3]}),
                                                 new ClassWithTreeObjectFields(new ClassWithTreeObjectFields(Boolean.TRUE, new ClassWithOnePrimitiveFields(12), new ClassWithoutFields()),
                                                                               new ClassWithOneObjectField(new Object()),
                                                                               new ClassWithoutFields()),
                                                 "end");
    }

    @Benchmark
    public void measure(Blackhole bh)
    {
        bh.consume(meter.measureDeep(obj));
    }
    
    public static class ClassWithoutFields
    {
    }

    public static class ClassWithOnePrimitiveFields
    {
        private int intField;

        public ClassWithOnePrimitiveFields(int intField)
        {
            this.intField = intField;
        }
    }

    public static class ClassWithOneObjectField
    {
        public static Object staticField = "static";

        private Object field;

        public ClassWithOneObjectField(Object field)
        {
            this.field = field;
        }
    }

    public static class ClassWithTreeObjectFields
    {
        private Object first;

        private Object second;

        private Object third;

        public ClassWithTreeObjectFields(Object first, Object second, Object third)
        {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    public static class ClassWithOneObjectFieldsAndTwoPrimitives
    {
        private byte first;

        private Object second;

        private double third;

        public ClassWithOneObjectFieldsAndTwoPrimitives(byte first, Object second, double third)
        {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }
    
    public static class ClassWithFiveObjectFields extends ClassWithTreeObjectFields
    {
        private Object fourth;

        private Object fifth;

        public ClassWithFiveObjectFields(Object first, Object second, Object third, Object fourth, Object fifth)
        {
            super(first, second, third);
            this.fourth = fourth;
            this.fifth = fifth;
        }
    }
}
