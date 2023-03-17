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

        checkMesureArray(new Object[0]);
        checkMesureArray(new Object[1]);
        checkMesureArray(new Object[256]);
    }

    private void checkMesureArray(Object[] array) {

        String message = "Shallow size of Object[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testByteArraySizes() {

        checkMesureArray(new byte[0]);
        checkMesureArray(new byte[1]);
        checkMesureArray(new byte[256]);
    }

    private void checkMesureArray(byte[] array) {

        String message = "Shallow size of byte[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testBooleanArraySizes() {

        checkMesureArray(new boolean[0]);
        checkMesureArray(new boolean[1]);
        checkMesureArray(new boolean[256]);
    }

    private void checkMesureArray(boolean[] array) {

        String message = "Shallow size of boolean[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testShortArraySizes() {

        checkMesureArray(new short[0]);
        checkMesureArray(new short[1]);
        checkMesureArray(new short[256]);
    }

    private void checkMesureArray(short[] array) {

        String message = "Shallow size of short[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testCharArraySizes() {

        checkMesureArray(new char[0]);
        checkMesureArray(new char[1]);
        checkMesureArray(new char[256]);
    }

    private void checkMesureArray(char[] array) {

        String message = "Shallow size of char[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testIntArraySizes() {

        checkMesureArray(new int[0]);
        checkMesureArray(new int[1]);
        checkMesureArray(new int[256]);
    }

    private void checkMesureArray(int[] array) {

        String message = "Shallow size of int[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testFloatArraySizes() {

        checkMesureArray(new float[0]);
        checkMesureArray(new float[1]);
        checkMesureArray(new float[256]);
    }

    private void checkMesureArray(float[] array) {

        String message = "Shallow size of float[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testLongArraySizes() {

        checkMesureArray(new long[0]);
        checkMesureArray(new long[1]);
        checkMesureArray(new long[256]);
    }

    private void checkMesureArray(long[] array) {

        String message = "Shallow size of long[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testDoubleArraySizes() {

        checkMesureArray(new double[0]);
        checkMesureArray(new double[1]);
        checkMesureArray(new double[256]);
    }

    private void checkMesureArray(double[] array) {

        String message = "Shallow size of double[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
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
    public void testHeapByteBuffer() {

        ByteBuffer empty = ByteBuffer.allocate(0);
        ByteBuffer readOnlyEmpty = empty.asReadOnlyBuffer();
        ByteBuffer one = ByteBuffer.allocate(1);
        ByteBuffer readOnlyOne = one.asReadOnlyBuffer();
        ByteBuffer emptyOne = (ByteBuffer) one.duplicate().position(1);
        ByteBuffer readOnlyEmptyOne = emptyOne.asReadOnlyBuffer();
        ByteBuffer twenty = ByteBuffer.allocate(20);
        ByteBuffer readOnlyTwenty = twenty.asReadOnlyBuffer();
        ByteBuffer five = (ByteBuffer) twenty.slice().limit(5);
        ByteBuffer readOnlyFive = five.asReadOnlyBuffer();

        MemoryMeter m1 = MemoryMeter.builder().withGuessing(guess).build();

        long sizeShallowEmptyBuffer = reference.measure(empty);
        assertEquals("empty ByteBuffer", sizeShallowEmptyBuffer, m1.measure(empty));
        assertEquals("empty ByteBuffer", sizeShallowEmptyBuffer, m1.measure(readOnlyEmpty));

        long expected = sizeShallowEmptyBuffer + reference.measureArray(empty.array());
        assertEquals("Deep empty ByteBuffer", expected, m1.measureDeep(empty));
        assertEquals("Deep empty ByteBuffer", expected, m1.measureDeep(readOnlyEmpty));

        expected = sizeShallowEmptyBuffer + reference.measureArray(emptyOne.array());
        assertEquals("Deep duplicated 1-byte ByteBuffer", expected, m1.measureDeep(emptyOne));
        assertEquals("Deep duplicated 1-byte ByteBuffer", expected, m1.measureDeep(readOnlyEmptyOne));

        expected = sizeShallowEmptyBuffer + reference.measureArray(one.array());
        assertEquals("Deep 1-byte ByteBuffer", expected, m1.measureDeep(one));
        assertEquals("Deep 1-byte ByteBuffer", expected, m1.measureDeep(readOnlyOne));

        expected = sizeShallowEmptyBuffer + reference.measureArray(twenty.array());
        assertEquals("Twenty bytes ByteBuffer", expected, m1.measureDeep(twenty));
        assertEquals("Twenty bytes ByteBuffer", expected, m1.measureDeep(readOnlyTwenty));
        assertEquals("Five bytes ByteBuffer", expected, m1.measureDeep(five));
        assertEquals("Five bytes ByteBuffer", expected, m1.measureDeep(readOnlyFive));

        MemoryMeter m2 = MemoryMeter.builder().withGuessing(guess).omitSharedBufferOverhead().build();

        assertEquals(sizeShallowEmptyBuffer, m2.measure(empty));

        expected = sizeShallowEmptyBuffer + reference.measureArray(empty.array()); // The buffer represent the full array therefore nothing is shared
        assertEquals(expected, m2.measureDeep(empty));
        assertEquals(expected, m2.measureDeep(readOnlyEmpty));

        expected = sizeShallowEmptyBuffer + emptyOne.remaining();
        assertEquals("Deep duplicated 1-byte ByteBuffer", expected, m2.measureDeep(emptyOne));
        assertEquals("Deep duplicated 1-byte ByteBuffer", expected, m2.measureDeep(readOnlyEmptyOne));

        expected = sizeShallowEmptyBuffer + reference.measureArray(twenty.array());
        assertEquals("Twenty bytes ByteBuffer", expected, m2.measureDeep(twenty)); // The buffer represent the full array therefore nothing is shared
        assertEquals("Twenty bytes ByteBuffer", expected, m2.measureDeep(readOnlyTwenty));

        expected = sizeShallowEmptyBuffer + five.remaining();
        assertEquals("Five bytes ByteBuffer", expected, m2.measureDeep(five));
        assertEquals("Five bytes ByteBuffer", expected, m2.measureDeep(readOnlyFive));
    }

    @Test
    public void testDirectByteBuffer() {

        ByteBuffer empty = ByteBuffer.allocateDirect(0);
        ByteBuffer readOnlyEmpty = empty.asReadOnlyBuffer();
        ByteBuffer one = ByteBuffer.allocateDirect(1);
        ByteBuffer readOnlyOne = one.asReadOnlyBuffer();
        ByteBuffer emptyOne = (ByteBuffer) one.duplicate().position(1);
        ByteBuffer readOnlyEmptyOne = emptyOne.asReadOnlyBuffer();
        ByteBuffer twenty = ByteBuffer.allocateDirect(20);
        ByteBuffer readOnlyTwenty = twenty.asReadOnlyBuffer();
        ByteBuffer five = (ByteBuffer) twenty.slice().limit(5);
        ByteBuffer readOnlyFive = five.asReadOnlyBuffer();

        MemoryMeter m1 = MemoryMeter.builder().withGuessing(guess).build();

        long sizeShallowBuffer = reference.measure(empty);
        assertEquals("empty ByteBuffer", sizeShallowBuffer, m1.measure(empty));
        assertEquals("empty ByteBuffer", sizeShallowBuffer, m1.measure(readOnlyEmpty));

        long sizeDeepBuffer = reference.measureDeep(empty);
        assertEquals("Deep empty ByteBuffer", sizeDeepBuffer, m1.measureDeep(empty));

        // If a DirectByteBuffer is referencing a part of another DirectByteBuffer it will have a reference to this buffer
        // through the att (attachement) field and no cleaner
        long sizeDeepWithAttachedBuffer = sizeShallowBuffer + sizeDeepBuffer;
        assertEquals("Deep empty ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(readOnlyEmpty));
        assertEquals("Deep duplicated 1-byte ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(emptyOne));
        assertEquals("Deep duplicated 1-byte ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(readOnlyEmptyOne));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepBuffer, m1.measureDeep(one));
        assertEquals("Twenty bytes ByteBuffer", sizeDeepBuffer, m1.measureDeep(twenty));
        assertEquals("Twenty bytes ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(readOnlyTwenty));
        assertEquals("Five bytes ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(five));
        assertEquals("Five bytes ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(readOnlyFive));

        MemoryMeter m2 = MemoryMeter.builder().withGuessing(guess).omitSharedBufferOverhead().build();

        assertEquals("empty ByteBuffer", sizeShallowBuffer, m2.measure(empty));
        assertEquals("empty ByteBuffer", sizeShallowBuffer, m2.measure(readOnlyEmpty));
        assertEquals("Deep empty ByteBuffer", sizeDeepBuffer, m2.measureDeep(empty));
        assertEquals("Deep empty ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyEmpty));
        assertEquals("Deep duplicated 1-byte ByteBuffer", sizeShallowBuffer, m2.measureDeep(emptyOne));
        assertEquals("Deep duplicated 1-byte ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyEmptyOne));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepBuffer, m2.measureDeep(one));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyOne));
        assertEquals("Twenty bytes ByteBuffer", sizeDeepBuffer, m2.measureDeep(twenty));
        assertEquals("Twenty bytes ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyTwenty));
        assertEquals("Five bytes ByteBuffer", sizeShallowBuffer, m2.measureDeep(five));
        assertEquals("Five bytes ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyFive));
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
