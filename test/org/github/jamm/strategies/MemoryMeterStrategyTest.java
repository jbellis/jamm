package org.github.jamm.strategies;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.github.jamm.MemoryMeter;
import org.github.jamm.VM;
import org.github.jamm.MemoryMeter.Guess;

import static org.junit.Assert.assertEquals;

import sun.misc.Contended;

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

        checkMeasureArray(new Object[0]);
        checkMeasureArray(new Object[1]);
        checkMeasureArray(new Object[256]);
    }

    private void checkMeasureArray(Object[] array) {

        String message = "Shallow size of Object[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testByteArraySizes() {

        checkMeasureArray(new byte[0]);
        checkMeasureArray(new byte[1]);
        checkMeasureArray(new byte[256]);
    }

    private void checkMeasureArray(byte[] array) {

        String message = "Shallow size of byte[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testBooleanArraySizes() {

        checkMeasureArray(new boolean[0]);
        checkMeasureArray(new boolean[1]);
        checkMeasureArray(new boolean[256]);
    }

    private void checkMeasureArray(boolean[] array) {

        String message = "Shallow size of boolean[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testShortArraySizes() {

        checkMeasureArray(new short[0]);
        checkMeasureArray(new short[1]);
        checkMeasureArray(new short[256]);
    }

    private void checkMeasureArray(short[] array) {

        String message = "Shallow size of short[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testCharArraySizes() {

        checkMeasureArray(new char[0]);
        checkMeasureArray(new char[1]);
        checkMeasureArray(new char[256]);
    }

    private void checkMeasureArray(char[] array) {

        String message = "Shallow size of char[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testIntArraySizes() {

        checkMeasureArray(new int[0]);
        checkMeasureArray(new int[1]);
        checkMeasureArray(new int[256]);
    }

    private void checkMeasureArray(int[] array) {

        String message = "Shallow size of int[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testFloatArraySizes() {

        checkMeasureArray(new float[0]);
        checkMeasureArray(new float[1]);
        checkMeasureArray(new float[256]);
    }

    private void checkMeasureArray(float[] array) {

        String message = "Shallow size of float[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testLongArraySizes() {

        checkMeasureArray(new long[0]);
        checkMeasureArray(new long[1]);
        checkMeasureArray(new long[256]);
    }

    private void checkMeasureArray(long[] array) {

        String message = "Shallow size of long[" + array.length + "] with guess= " + guess;
        assertEquals(message, reference.measure(array), tested.measure(array));
        assertEquals(message, reference.measureArray(array), tested.measureArray(array));
    }

    @Test
    public void testDoubleArraySizes() {

        checkMeasureArray(new double[0]);
        checkMeasureArray(new double[1]);
        checkMeasureArray(new double[256]);
    }

    private void checkMeasureArray(double[] array) {

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

        // Read-only and normal direct ByteBuffer have the same fields and therefore the same size
        long sizeShallowBuffer = reference.measure(empty);
        assertEquals("empty ByteBuffer", sizeShallowBuffer, m1.measure(empty));
        assertEquals("empty ByteBuffer", sizeShallowBuffer, m1.measure(readOnlyEmpty));

        // Measure deep will include reference to the cleaner for Read-write buffers
        //
        // root [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
        //   |
        //   +--cleaner [jdk.internal.ref.Cleaner] 72 bytes (40 bytes)
        //     |
        //     +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
        long sizeDeepRwBuffer = reference.measureDeep(empty);
        assertEquals("Deep empty ByteBuffer", sizeDeepRwBuffer, m1.measureDeep(empty));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepRwBuffer, m1.measureDeep(one));
        assertEquals("Twenty bytes ByteBuffer", sizeDeepRwBuffer, m1.measureDeep(twenty));

        // If a DirectByteBuffer is referencing a part of another DirectByteBuffer (read only or slice buffer), it will have a 
        // reference to the original buffer through the att (attachement) field and no cleaner
        long sizeDeepWithAttachedBuffer = sizeShallowBuffer + sizeDeepRwBuffer;
        // root [java.nio.DirectByteBufferR] 200 bytes (64 bytes)
        //   |
        //   +--att [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
        //     |
        //     +--cleaner [jdk.internal.ref.Cleaner] 72 bytes (40 bytes)
        //       |
        //       +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
        assertEquals("Deep empty read-only ByteBuffer", sizeDeepWithAttachedBuffer , m1.measureDeep(readOnlyEmpty));
        assertEquals("Deep duplicated 1-byte ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(emptyOne));
        assertEquals("Deep twenty bytes read-only ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(readOnlyTwenty));
        assertEquals("Five bytes slice ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(five));

        // Pre java 12, a DirectByteBuffer created from another DirectByteBuffer was using the source buffer as an attachment
        // for liveness rather than the source buffer's attachment (https://bugs.openjdk.org/browse/JDK-8208362)
        long sizeDeepWithMultiLayersOfReferences = VM.isPreJava12JVM() ? sizeShallowBuffer + sizeDeepWithAttachedBuffer : sizeDeepWithAttachedBuffer;
        assertEquals("Deep duplicated read-only 1-byte ByteBuffer", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyEmptyOne));
        assertEquals("Deep five bytes slice read-only ByteBuffer", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyFive));

        // Test with omitting shared buffer overhead
        MemoryMeter m2 = MemoryMeter.builder().withGuessing(guess).omitSharedBufferOverhead().build();

        // Read-only and normal direct ByteBuffer have the same fields and therefore the same size
        assertEquals("empty ByteBuffer", sizeShallowBuffer, m2.measure(empty));
        assertEquals("empty ByteBuffer", sizeShallowBuffer, m2.measure(readOnlyEmpty));

        // Measure deep is unaffected by the option as long as their is no shared buffer
        assertEquals("Deep empty ByteBuffer", sizeDeepRwBuffer, m2.measureDeep(empty));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepRwBuffer, m2.measureDeep(one));
        assertEquals("Twenty bytes ByteBuffer", sizeDeepRwBuffer, m2.measureDeep(twenty));

        // Read-only buffers even if they are technically a view of another buffer are not considered as shared by the option
        assertEquals("Deep empty read-only ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyEmpty));
        assertEquals("Deep 1-byte read-only ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyOne));
        assertEquals("Twenty bytes read-only ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyTwenty));

        // For duplicated and sliced buffer the attachment should be ignored from the computation
        assertEquals("Deep duplicated 1-byte ByteBuffer", sizeShallowBuffer, m2.measureDeep(emptyOne));
        assertEquals("Five bytes ByteBuffer", sizeShallowBuffer, m2.measureDeep(five));

        // A read-only buffer of a duplicated/sliced buffer should only account for the shallow read-only part and swallow attachment part
        long sizeDeepWithReadOnlyAndShallowBuffer = VM.isPreJava12JVM() ? sizeShallowBuffer + sizeShallowBuffer  // top level (= shallow RByteBuffer) + att top level
                                                                        : sizeShallowBuffer; // only top level (= shallow RByteBuffer)
        assertEquals("Deep duplicated 1-byte read-only ByteBuffer", sizeDeepWithReadOnlyAndShallowBuffer, m2.measureDeep(readOnlyEmptyOne));
        assertEquals("Five bytes sliced read-only ByteBuffer", sizeDeepWithReadOnlyAndShallowBuffer, m2.measureDeep(readOnlyFive));
    }

    @Test
    public void testHierachyPadding()
    {
        assertEquals(reference.measure(new A()), tested.measure(new A()));
        assertEquals(reference.measureDeep(new A()), tested.measureDeep(new A()));
        assertEquals(reference.measure(new B()), tested.measure(new B()));
        assertEquals(reference.measureDeep(new B()), tested.measureDeep(new B()));
        assertEquals(reference.measure(new C()), tested.measure(new C()));
        assertEquals(reference.measureDeep(new C()), tested.measureDeep(new C()));
        assertEquals(reference.measure(new D()), tested.measure(new D()));
        assertEquals(reference.measureDeep(new D()), tested.measureDeep(new D()));
        assertEquals(reference.measure(new E()), tested.measure(new E()));
        assertEquals(reference.measureDeep(new E()), tested.measureDeep(new E()));
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

    @Test
    public void testLongAdder() throws Exception {
        // Create a LongAdder and make sure that we have Contended class being used.
        LongAdder longAdder = new LongAdder();
        final CountDownLatch lach = new CountDownLatch(10);
        Runnable runnable = 
                () -> {
                    for (int i = 0; i < 1000; i++)
                        longAdder.increment();
                    lach.countDown();
                };

        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++)
            pool.execute(runnable);
        lach.await();
        assertEquals(reference.measureDeep(longAdder), tested.measureDeep(longAdder));
    }

    @Test
    public void testThread() throws Exception {
        Thread thread = new Thread();
        assertEquals(reference.measure(thread), tested.measure(thread));
        assertEquals(reference.measureDeep(thread), tested.measureDeep(thread));
    }

    @Test
    public void testConcurrentHashMap() throws Exception {
        // Create a ConcurrentHashMap and make sure that we have Contended class being used.
        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
        final CountDownLatch lach = new CountDownLatch(10);
        Runnable adder = 
                () -> {
                    for (int i = 0; i < 1000; i++)
                        map.put(i, i);
                    lach.countDown();
                };

        Runnable remover = 
                () -> {
                    for (int i = 0; i < 1000; i++)
                        map.remove(i);
                    lach.countDown();
                };

        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++)
            pool.execute(i % 2 == 0 ? adder : remover);
        lach.await();
        assertEquals(reference.measureDeep(map), tested.measureDeep(map));
    }

    @Test
    public void testContendedForNonInternalClasses() throws Exception {

        Object underTest = new WithMultipleAnonymousContendedAnnotations();
        assertEquals(reference.measureDeep(underTest), tested.measureDeep(underTest));

        underTest = new WithMultipleContentionGroupAnnotations();
        assertEquals(reference.measureDeep(underTest), tested.measureDeep(underTest));

        underTest = new WithMultipleContentionGroupAndAnonymousContendedAnnotations();
        assertEquals(reference.measureDeep(underTest), tested.measureDeep(underTest));

        underTest = new WithClassContendedAnnotation();
        assertEquals(reference.measureDeep(underTest), tested.measureDeep(underTest));

        underTest = new WithClassAndMultipleContentionGroupAnnotations();
        assertEquals(reference.measureDeep(underTest), tested.measureDeep(underTest));
    }

    @SuppressWarnings("unused")
    public static class WithMultipleAnonymousContendedAnnotations {

        @Contended
        private int first;

        @Contended
        private int second;

        private int third;

        @Contended
        private int fourth;
    }

    @SuppressWarnings("unused")
    public static class WithMultipleContentionGroupAnnotations {

        @Contended("group1")
        private int first;

        @Contended("group1")
        private int second;

        private int third;

        @Contended("group2")
        private int fourth;
    }

    @SuppressWarnings("unused")
    public static class WithMultipleContentionGroupAndAnonymousContendedAnnotations {

        @Contended("group1")
        private int first;

        @Contended("group1")
        private int second;

        @Contended
        private int third;

        @Contended("group2")
        private int fourth;
    }

    @Contended
    @SuppressWarnings("unused")
    public static class WithClassContendedAnnotation {

        private int first;

        private int second;

        private int third;

         private int fourth;
    }

    @Contended
    @SuppressWarnings("unused")
    public static class WithClassAndMultipleContentionGroupAnnotations {

        @Contended("group1")
        private int first;

        @Contended("group1")
        private int second;

        @Contended
        private int third;

        @Contended("group2")
        private int fourth;
    }

    @Test
    public void testHierachyPaddingWithContendedAnnotation()
    {
        assertEquals(reference.measure(new F()), tested.measure(new F()));
        assertEquals(reference.measureDeep(new F()), tested.measureDeep(new F()));
        assertEquals(reference.measure(new G()), tested.measure(new G()));
        assertEquals(reference.measureDeep(new G()), tested.measureDeep(new G()));

        assertEquals(reference.measure(new H()), tested.measure(new H()));
        assertEquals(reference.measureDeep(new H()), tested.measureDeep(new H()));
        assertEquals(reference.measure(new I()), tested.measure(new I()));
        assertEquals(reference.measureDeep(new I()), tested.measureDeep(new I()));
    }

    @Contended
    @SuppressWarnings("unused")
    static class F extends A {
        boolean f;
    }

    @SuppressWarnings("unused")
    static class G extends F {
        boolean g;
    }

    @Contended
    @SuppressWarnings("unused")
    static class H extends A {
    }

    @SuppressWarnings("unused")
    static class I extends H {
        boolean i;
    }
}
