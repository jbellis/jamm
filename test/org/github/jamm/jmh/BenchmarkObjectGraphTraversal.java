package org.github.jamm.jmh;

import java.util.concurrent.TimeUnit;

import org.github.jamm.FieldAndClassFilter;
import org.github.jamm.FieldFilter;
import org.github.jamm.Filters;
import org.github.jamm.Measurable;
import org.github.jamm.MeasurementStack;
import org.github.jamm.MemoryMeter;
import org.github.jamm.MemoryMeterStrategy;
import org.github.jamm.NoopMemoryMeterListener;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
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
public class BenchmarkObjectGraphTraversal
{
    private MemoryMeter meter;

    /**
     * The object being measured through reflection
     */
    private static Object OBJ;
    
    /**
     * The object being measured through the Measurable interface
     */
    private static Object MEASURABLE;

    static {

        OBJ = new ClassWithFiveObjectFields(new byte[20],
                                            new ClassWithOneObjectFieldsAndTwoPrimitives((byte) 8, new ClassWithoutFields(), Double.MAX_VALUE),
                                            new ClassWithOneObjectField(new Object[] {"test", new ClassWithOneObjectField(new Object()), new int[3]}),
                                            new ClassWithTreeObjectFields(new ClassWithTreeObjectFields(Boolean.TRUE, new ClassWithOnePrimitiveFields(12), new ClassWithoutFields()),
                                                                          new ClassWithOneObjectField(new Object()),
                                                                          new ClassWithoutFields()),
                                            "end");

        MEASURABLE = new MeasurableClassWithFiveObjectFields(new byte[20],
                                                             new MeasurableClassWithOneObjectFieldsAndTwoPrimitives((byte) 8, new MeasurableClassWithoutFields(), Double.MAX_VALUE),
                                                             new MeasurableClassWithOneObjectField(new Object[] {"test", new MeasurableClassWithOneObjectField(new Object()), new int[3]}),
                                                             new MeasurableClassWithTreeObjectFields(new MeasurableClassWithTreeObjectFields(Boolean.TRUE, new MeasurableClassWithOnePrimitiveFields(12), new MeasurableClassWithoutFields()),
                                                                                                     new MeasurableClassWithOneObjectField(new Object()),
                                                                                                     new MeasurableClassWithoutFields()),
                                                             "end");
    }

    @Setup(Level.Iteration)
    public void setup() throws Exception {
        MemoryMeterStrategy strategy = o -> 1;
        FieldFilter fieldFilter = Filters.getFieldFilters(false, false, false);
        FieldAndClassFilter classFilter = Filters.getClassFilters(false);

        this.meter = new MemoryMeter(strategy, classFilter, fieldFilter, false, NoopMemoryMeterListener.FACTORY);
    }

    @Benchmark
    public void measureThroughReflection(Blackhole bh) {
        bh.consume(meter.measureDeep(OBJ));
    }

    @Benchmark
    public void measureThroughMeasurable(Blackhole bh) {
        bh.consume(meter.measureDeep(MEASURABLE));
    }

    public static class ClassWithoutFields {

    }

    public static class MeasurableClassWithoutFields implements Measurable {

        @Override
        public void addChildrenTo(MeasurementStack stack)
        {
        }
    }
    
    @SuppressWarnings("unused")
    public static class ClassWithOnePrimitiveFields {

        private int intField;

        public ClassWithOnePrimitiveFields(int intField) {
            this.intField = intField;
        }
    }

    @SuppressWarnings("unused")
    public static class MeasurableClassWithOnePrimitiveFields implements Measurable {

        private int intField;

        public MeasurableClassWithOnePrimitiveFields(int intField) {
            this.intField = intField;
        }

        @Override
        public void addChildrenTo(MeasurementStack stack) {
        }
    }

    @SuppressWarnings("unused")
    public static class ClassWithOneObjectField {

        public static String staticField = "static";

        private Object field;

        public ClassWithOneObjectField(Object field) {
            this.field = field;
        }
    }

    @SuppressWarnings("unused")
    public static class MeasurableClassWithOneObjectField implements Measurable {

        public static Object staticField = "static";

        private Object field;

        public MeasurableClassWithOneObjectField(Object field) {
            this.field = field;
        }

        @Override
        public void addChildrenTo(MeasurementStack stack) {
            stack.pushObject(this, "field", field);
        }
    }

    @SuppressWarnings("unused")
    public static class ClassWithTreeObjectFields {

        private Object first;

        private Object second;

        private Object third;

        public ClassWithTreeObjectFields(Object first, Object second, Object third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    @SuppressWarnings("unused")
    public static class MeasurableClassWithTreeObjectFields implements Measurable {

        private Object first;

        private Object second;

        private Object third;

        public MeasurableClassWithTreeObjectFields(Object first, Object second, Object third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        @Override
        public void addChildrenTo(MeasurementStack stack) {
            stack.pushObject(this, "first", first);
            stack.pushObject(this, "second", second);
            stack.pushObject(this, "third", third);
        }
    }

    @SuppressWarnings("unused")
    public static class ClassWithOneObjectFieldsAndTwoPrimitives {

        private byte first;

        private Object second;

        private double third;

        public ClassWithOneObjectFieldsAndTwoPrimitives(byte first, Object second, double third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    @SuppressWarnings("unused")
    public static class MeasurableClassWithOneObjectFieldsAndTwoPrimitives implements Measurable {

        private byte first;

        private Object second;

        private double third;

        public MeasurableClassWithOneObjectFieldsAndTwoPrimitives(byte first, Object second, double third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        @Override
        public void addChildrenTo(MeasurementStack stack) {
            stack.pushObject(this, "second", second);
        }
    }

    @SuppressWarnings("unused")
    public static class ClassWithFiveObjectFields extends ClassWithTreeObjectFields {

        private Object fourth;

        private Object fifth;

        public ClassWithFiveObjectFields(Object first, Object second, Object third, Object fourth, Object fifth) {
            super(first, second, third);
            this.fourth = fourth;
            this.fifth = fifth;
        }
    }

    @SuppressWarnings("unused")
    public static class MeasurableClassWithFiveObjectFields extends MeasurableClassWithTreeObjectFields {

        private Object fourth;

        private Object fifth;

        public MeasurableClassWithFiveObjectFields(Object first, Object second, Object third, Object fourth, Object fifth) {
            super(first, second, third);
            this.fourth = fourth;
            this.fifth = fifth;
        }

        @Override
        public void addChildrenTo(MeasurementStack stack) {
            super.addChildrenTo(stack);
            stack.pushObject(this, "fourth", fourth);
            stack.pushObject(this, "fifth", fifth);
        }
    }
}
