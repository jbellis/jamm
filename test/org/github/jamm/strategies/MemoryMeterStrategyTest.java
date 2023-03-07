package org.github.jamm.strategies;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.github.jamm.MemoryMeter;
import org.github.jamm.MemoryMeter.Guess;

import static org.junit.Assert.assertEquals;

/**
 * In order to test the correctness of the values being returned the tests assume that the correct value is the one 
 * returned by the instrumentation strategy.
 */
@RunWith(Parameterized.class)
public class MemoryMeterStrategyTest
{
    private final MemoryMeter.Guess guess;

    private MemoryMeter tested;
    private MemoryMeter reference;

    @Parameterized.Parameters
    public static Collection<MemoryMeter.Guess> guesses() {

        return Arrays.asList(MemoryMeter.Guess.ALWAYS_UNSAFE, MemoryMeter.Guess.ALWAYS_SPEC);
    }

    public MemoryMeterStrategyTest(MemoryMeter.Guess guess)
    {
        this.guess = guess;
    }

    @Before
    public void setUp()
    {
        reference = MemoryMeter.builder().withGuessing(Guess.ALWAYS_INSTRUMENTATION).build();
        tested = MemoryMeter.builder().withGuessing(guess).build();
    }

    @After
    public void tearDown()
    {
        reference = null;
        tested = null;
    }

    @Test
    public void testObjectArraySizes() {

        assertEquals("Shallow size of Object[0]", reference.measure(new Object[0]), tested.measure(new Object[0]));
        assertEquals("Shallow size of Object[1]", reference.measure(new Object[1]), tested.measure(new Object[1]));
        assertEquals("Shallow size of Object[256]", reference.measure(new Object[256]), tested.measure(new Object[256]));
    }

    @Test
    public void testByteArraySizes() {
        assertEquals("Shallow size of byte[0]" + guess, reference.measure(new byte[0]), tested.measure(new byte[0]));
        assertEquals("Shallow size of byte[1]", reference.measure(new byte[1]), tested.measure(new byte[1]));
        assertEquals("Shallow size of byte[256]", reference.measure(new byte[256]), tested.measure(new byte[256]));
    }

    @Test
    public void testCharArraySizes() {
        assertEquals("Shallow size of char[0]", reference.measure(new char[0]), tested.measure(new char[0]));
        assertEquals("Shallow size of char[1]", reference.measure(new char[1]), tested.measure(new char[1]));
        assertEquals("Shallow size of char[256]", reference.measure(new char[256]), tested.measure(new char[256]));
    }

    @Test
    public void testIntArraySizes() {
        assertEquals("Shallow size of int[0]", reference.measure(new int[0]), tested.measure(new int[0]));
        assertEquals("Shallow size of int[1]", reference.measure(new int[1]), tested.measure(new int[1]));
        assertEquals("Shallow size of int[256]", reference.measure(new int[256]), tested.measure(new int[256]));
    }

    @Test
    public void testLongArraySizes() {
        assertEquals("Shallow size of long[0]", reference.measure(new long[0]), tested.measure(new long[0]));
        assertEquals("Shallow size of long[1]", reference.measure(new long[1]), tested.measure(new long[1]));
        assertEquals("Shallow size of long[256]", reference.measure(new long[256]), tested.measure(new long[256]));
    }

    @SuppressWarnings("unused")
    static class LongHolder {
        private long value;
    }

    @SuppressWarnings("unused")
    static class IntHolder {
        private int value;
    }

    @SuppressWarnings("unused")
    static class CharHolder {
        private char value;
    }

    @SuppressWarnings("unused")
    static class TwoCharHolder {
        private char value;
        private char other;
    }

    @SuppressWarnings("unused")
    static class ThreeCharHolder {
        private char value;
        private char other;
        private char overflow;
    }

    @SuppressWarnings("unused")
    static class IntCharHolder {
        private int value;
        private char other;
    }

    @SuppressWarnings("unused")
    static class LongIntHolder {
        private long value;
        private int other;
    }

    @SuppressWarnings("unused")
    static class LongIntHolder2 extends LongHolder {
        private int other;
    }

    @SuppressWarnings("unused")
    static class FiveByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte overflow;
    }

    @Test
    public void testEmbeddedPrimitives() {
        assertEquals("no embedded long field", reference.measure(new LongHolder()), tested.measure(new LongHolder()));
        assertEquals("Embedded int field", reference.measure(new IntHolder()), tested.measure(new IntHolder()));
        assertEquals("Embedded char field", reference.measure(new CharHolder()), tested.measure(new CharHolder()));
        assertEquals("Embedded char field * 2", reference.measure(new TwoCharHolder()), tested.measure(new TwoCharHolder()));
        assertEquals("Embedded char field * 3", reference.measure(new ThreeCharHolder()), tested.measure(new ThreeCharHolder()));
        assertEquals("Embedded int field only", reference.measure(new IntCharHolder()), tested.measure(new IntCharHolder()));
        assertEquals("Only 4 bytes available", reference.measure(new FiveByteHolder()), tested.measure(new FiveByteHolder()));
        assertEquals("4 bytes always available", reference.measure(new LongIntHolder()), tested.measure(new LongIntHolder()));
        assertEquals("4 bytes not available if parent has a field", reference.measure(new LongIntHolder2()), tested.measure(new LongIntHolder2()));
        assertEquals(reference.measure(new Object[16384]), tested.measure(new Object[16384]));
    }

    @Test
    public void testPrimitives() {
        assertEquals("Shallow size of Object", reference.measure(new Object()), tested.measure(new Object()));
        assertEquals("Shallow size of Long", reference.measure(Long.valueOf(0)), tested.measure(Long.valueOf(0)));
        assertEquals("Shallow size of Integer", reference.measure(Integer.valueOf(0)), tested.measure(Integer.valueOf(0)));
        assertEquals("Shallow size of empty String", reference.measure(""), tested.measure(""));
        assertEquals("Shallow size of one-character String", reference.measure("a"), tested.measure("a"));
        assertEquals("Shallow size of empty array of objects", reference.measure(new Object[0]), tested.measure(new Object[0]));

        Object[] objects = new Object[100];
        assertEquals("Shallow size of Object[100] containing all nulls", reference.measure(objects), tested.measure(objects));
    }

    @Test
    public void testByteBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        ByteBuffer one = ByteBuffer.allocate(1);
        ByteBuffer emptyOne = (ByteBuffer) one.duplicate().position(1);

        MemoryMeter m1 = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Deep empty ByteBuffer", reference.measureDeep(empty), m1.measureDeep(empty));
        assertEquals("Deep 1-byte ByteBuffer", reference.measureDeep(one), m1.measureDeep(one));
        assertEquals("Deep duplicated 1-byte ByteBuffer", reference.measureDeep(emptyOne), m1.measureDeep(emptyOne));

        MemoryMeter m2 = MemoryMeter.builder().withGuessing(guess).omitSharedBufferOverhead().build();

        long sizeEmptyByteBuffer = reference.measure(empty);
        assertEquals(sizeEmptyByteBuffer, m2.measure(empty));
        assertEquals(sizeEmptyByteBuffer, m2.measureDeep(empty));
        assertEquals(sizeEmptyByteBuffer + 1, m2.measureDeep(one)); // as of 0.2.4 we don't count the bytes!!!
        assertEquals(sizeEmptyByteBuffer, m2.measureDeep(emptyOne));
    }
    
    @Test
    public void testHierachyPadding()
    {
        assertEquals(reference.measure(new A()), tested.measure(new A()));
        assertEquals(reference.measure(new B()), tested.measure(new B()));
        assertEquals(reference.measure(new C()), tested.measure(new C()));
        assertEquals(reference.measure(new D()), tested.measure(new D()));
        assertEquals(reference.measure(new E()), tested.measure(new E()));
    }

    static class A {
        boolean a;
    }

    static class B extends A {
        boolean b;
    }

    static class C extends B {
        boolean c;
    }

    static class D extends E{
    }

    static class E {
        long e;
    }

    @Test
    public void testHiddenClass()
    {
        WithLambdaField parent = new WithLambdaField("test");

        assertEquals(reference.measure(parent.greater), tested.measure(parent.greater));
    }

    private static class WithLambdaField {

        final String field;

        final Predicate<String> greater;

        public WithLambdaField(String field)
        {
            this.field = field;
            this.greater = (e) -> WithLambdaField.this.isGreater(e);
        }
        
        public boolean isGreater(String s)
        {
            return s.compareTo(field) > 0;
        }
    }

    @Test
    public void testFieldLayout()
    {
        assertEquals(reference.measure(new LongFloatLongFloatHolder()), tested.measure(new LongFloatLongFloatHolder()));
    }

    @SuppressWarnings("unused")
    static class LongFloatHolder extends LongHolder {
        private float value;
    }
    
    @SuppressWarnings("unused")
    static class LongFloatLongHolder extends LongFloatHolder {
        private long value;
    }
    
    @SuppressWarnings("unused")
    static class LongFloatLongFloatHolder extends LongFloatLongHolder {
        private float value;
    }
//
//    @Test
//    public void testRecordClass()
//    {
//        User user = new User("test", 30);
//
//        assertEquals(reference.measure(user), tested.measure(user));
//    }
//
//    public static record User(String name, int age) {};
}
