package org.github.jamm.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.github.jamm.MemoryMeter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
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
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BenchmarkMeasure
{
    @Param({"ALWAYS_INSTRUMENTATION", "ALWAYS_SPEC", "ALWAYS_UNSAFE"})
    private String guess;

    private MemoryMeter meter;

    private Object[] objects;

    @Setup(Level.Iteration)
    public void setup() throws Exception
    {
        Class<?>[] choices = new Class<?>[] {ClassWithoutFields.class,
                                             ClassWithOnePrimitiveFields.class,
                                             ClassWithOneObjectField.class,
                                             ClassWithTreeObjectFields.class,
                                             ClassWithOneObjectFieldsAndTwoPrimitives.class,
                                             ClassWithFiveObjectFields.class};

        Random random = new Random();

        objects = new Object[1000];
        for (int i = 0; i < objects.length; i++)
        {
            objects[i] = choices[random.nextInt(choices.length)].newInstance(); 
        }

        MemoryMeter.Guess guess = MemoryMeter.Guess.valueOf(this.guess);
        this.meter = MemoryMeter.builder().withGuessing(guess).build();
    }

    @Benchmark
    public void measure(Blackhole bh)
    {
        for (Object o : objects)
            bh.consume(meter.measure(o));
    }

    public static class ClassWithoutFields
    {
    }

    public static class ClassWithOnePrimitiveFields
    {
        int intField;
    }

    
    public static class ClassWithOneObjectField
    {
        Object field = new Object();
    }

    public static class ClassWithTreeObjectFields
    {
        Object first = new Object();

        Object second = new Object();

        String[] third = new String[] {"one", "two"};
    }

    public static class ClassWithOneObjectFieldsAndTwoPrimitives
    {
        byte first;

        Object second = new Object();

        double third;
    }
    
    public static class ClassWithFiveObjectFields extends ClassWithTreeObjectFields
    {
        Object fourth = new Object();

        int[] fifth = new int[12];
    }

//    public static Object generateObject(Random random, Class<?> clazz) throws Exception
//    {
//        if (String.class.isAssignableFrom(clazz))
//            return generateString(random);
//
//        if (clazz.isArray())
//            return generateArray(random, clazz.getComponentType());
//
//        Object o = clazz.newInstance();
//
//        Field[] fields = clazz.getDeclaredFields();
//        AccessibleObject.setAccessible(fields, true);
//
//        for (Field field : fields)
//        {
//            if (!field.getType().isPrimitive())
//                field.set(o, new Object());
//        }
//
//        AccessibleObject.setAccessible(fields, false);
//
//        return o;
//    }
//
//    public static Object generateString(Random random) throws Exception
//    {
//        int letterA = 97;
//        int letterZ = 122;
//        int length = random.nextInt(100);
//
//        return random.ints(letterA, letterZ + 1)
//                     .limit(length)
//                     .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
//                     .toString();
//    }
//
//    public static Object generateArray(Random random, Class<?> componentType) throws Exception
//    {
//        int length = random.nextInt(20);
//        Object array = Array.newInstance(componentType, length);
//
//        if (componentType.isPrimitive())
//            return array; // For primitive we can stop here as "measure" is only looking at the field type not at the actual value.
//
//        for (int i = 0; i < length; i++)
//        {
//            Array.set(array, i, new Object()); // We just need a java reference so new Object is fine
//        }
//
//        return array;
//    }
}
