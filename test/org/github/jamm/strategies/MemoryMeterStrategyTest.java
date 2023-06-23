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
import sun.misc.Contended;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.github.jamm.MemoryMeter;
import org.github.jamm.VM;

import org.github.jamm.MemoryMeter.Guess;

import static org.junit.Assert.assertEquals;

import static org.github.jamm.MemoryMeter.ByteBufferMode.SLAB_ALLOCATION_NO_SLICE;
import static org.github.jamm.MemoryMeter.ByteBufferMode.SLAB_ALLOCATION_SLICE;

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

        return Arrays.asList(MemoryMeter.Guess.INSTRUMENTATION_AND_SPECIFICATION, MemoryMeter.Guess.UNSAFE, MemoryMeter.Guess.SPECIFICATION);
    }

    public MemoryMeterStrategyTest(MemoryMeter.Guess guess) {
        this.guess = guess;
    }

    @Before
    public void setUp() {
        reference = MemoryMeter.builder().withGuessing(Guess.INSTRUMENTATION).build();
        tested = MemoryMeter.builder().withGuessing(guess).build();
    }

    @After
    public void tearDown() {
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

        // Empty/no slab
        ByteBuffer empty = ByteBuffer.allocate(0);
        ByteBuffer readOnlyEmpty = empty.asReadOnlyBuffer();
        // Not empty/no slab 
        ByteBuffer one = ByteBuffer.allocate(1);
        ByteBuffer readOnlyOne = one.asReadOnlyBuffer();
        // Empty/position shift/slab without slice
        ByteBuffer emptyPositionShift = (ByteBuffer) one.duplicate().position(1);
        ByteBuffer readOnlyEmptyPositionShift = emptyPositionShift.asReadOnlyBuffer();
        // Empty/limit shift/slab without slice
        ByteBuffer emptyLimitShift = (ByteBuffer) one.duplicate().limit(0);
        ByteBuffer readOnlyEmptyLimitShift = emptyLimitShift.asReadOnlyBuffer();
        // Not empty/ no slab
        ByteBuffer twenty = ByteBuffer.allocate(20);
        ByteBuffer readOnlyTwenty = twenty.asReadOnlyBuffer();
        ByteBuffer twentySlice = ByteBuffer.allocate(20).slice();
        ByteBuffer readOnlyTwentySlice = twentySlice.asReadOnlyBuffer();
        // Not empty/position shift/slab without slice
        ByteBuffer fivePositionShift = (ByteBuffer) twenty.duplicate().position(15);
        ByteBuffer readOnlyFivePositionShift = fivePositionShift.asReadOnlyBuffer();
        // Not empty/limit shift/slab without slice
        ByteBuffer fiveLimitShift = (ByteBuffer) twenty.duplicate().limit(5);
        ByteBuffer readOnlyFiveLimitShift = fiveLimitShift.asReadOnlyBuffer();
        // Not empty/position and limit shifts/slab without slice
        ByteBuffer fivePositionAndLimitShift = (ByteBuffer) twenty.duplicate().position(10).limit(15);
        ByteBuffer readOnlyFivePositionAndLimitShift = fivePositionAndLimitShift.asReadOnlyBuffer();
        // Not empty/position shift/slab with slice
        ByteBuffer fivePositionShiftSlice = ((ByteBuffer) twenty.duplicate().position(15)).slice();
        ByteBuffer readOnlyFivePositionShiftSlice = fivePositionShiftSlice.asReadOnlyBuffer();
        // Not empty/limit shift/slab with slice
        ByteBuffer fiveLimitShiftSlice = ((ByteBuffer) twenty.duplicate().limit(5)).slice();
        ByteBuffer readOnlyFiveLimitShiftSlice = fiveLimitShiftSlice.asReadOnlyBuffer();
        // Not empty/position and limit shifts/slab with slice
        ByteBuffer fivePositionAndLimitShiftSlice = ((ByteBuffer) twenty.duplicate().position(10).limit(15)).slice();
        ByteBuffer readOnlyFivePositionAndLimitShiftSlice = fivePositionAndLimitShiftSlice.asReadOnlyBuffer();

        MemoryMeter m1 = MemoryMeter.builder().withGuessing(guess).build();

        long sizeShallowEmptyBuffer = reference.measure(empty);
        assertEquals("empty ByteBuffer", sizeShallowEmptyBuffer, m1.measure(empty));
        assertEquals("empty ByteBuffer", sizeShallowEmptyBuffer, m1.measure(readOnlyEmpty));

        long expected = sizeShallowEmptyBuffer + reference.measureArray(empty.array());
        assertEquals("Deep empty ByteBuffer", expected, m1.measureDeep(empty));
        assertEquals("Deep empty ByteBuffer", expected, m1.measureDeep(readOnlyEmpty));

        expected = sizeShallowEmptyBuffer + reference.measureArray(one.array());
        assertEquals("Deep 1-byte ByteBuffer", expected, m1.measureDeep(one));
        assertEquals("Deep 1-byte ByteBuffer", expected, m1.measureDeep(readOnlyOne));

        expected = sizeShallowEmptyBuffer + reference.measureArray(emptyPositionShift.array());
        assertEquals("Deep duplicated ByteBuffer with position shift", expected, m1.measureDeep(emptyPositionShift));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", expected, m1.measureDeep(readOnlyEmptyPositionShift));

        expected = sizeShallowEmptyBuffer + reference.measureArray(emptyLimitShift.array());
        assertEquals("Deep duplicated ByteBuffer with limit shift", expected, m1.measureDeep(emptyLimitShift));
        assertEquals("Deep read-only duplicated ByteBuffer with limit shift", expected, m1.measureDeep(readOnlyEmptyLimitShift));

        expected = sizeShallowEmptyBuffer + reference.measureArray(twenty.array());
        assertEquals("Deep ByteBuffer", expected, m1.measureDeep(twenty));
        assertEquals("Deep read-only ByteBuffer", expected, m1.measureDeep(readOnlyTwenty));
        assertEquals("Deep read-only ByteBuffer", expected, m1.measureDeep(readOnlyTwentySlice));

        assertEquals("Deep duplicated ByteBuffer with position shift", expected, m1.measureDeep(fivePositionShift));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", expected, m1.measureDeep(readOnlyFivePositionShift));
        assertEquals("Deep duplicated ByteBuffer with limit shift", expected, m1.measureDeep(fiveLimitShift));
        assertEquals("Deep duplicated read-only ByteBuffer with limit shift", expected, m1.measureDeep(readOnlyFiveLimitShift));
        assertEquals("Deep duplicated ByteBuffer with position and limit shift", expected, m1.measureDeep(fivePositionAndLimitShift));
        assertEquals("Deep duplicated read-only ByteBuffer with position and limit shift", expected, m1.measureDeep(readOnlyFivePositionAndLimitShift));

        assertEquals("Deep ByteBuffer slice with position shift", expected, m1.measureDeep(fivePositionShiftSlice));
        assertEquals("Deep read-only ByteBuffer slice with position shift", expected, m1.measureDeep(readOnlyFivePositionShiftSlice));
        assertEquals("Deep ByteBuffer slice with limit shift", expected, m1.measureDeep(fiveLimitShiftSlice));
        assertEquals("Deep read-only ByteBuffer slice with limit shift", expected, m1.measureDeep(readOnlyFiveLimitShiftSlice));
        assertEquals("Deep ByteBuffer slice with position and limit shift", expected, m1.measureDeep(fivePositionAndLimitShiftSlice));
        assertEquals("Deep read-only ByteBuffer slice with position and limit shift", expected, m1.measureDeep(readOnlyFivePositionAndLimitShiftSlice));

        // Test omitting shared overhead for slab WITHOUT slice
        MemoryMeter m2 = MemoryMeter.builder().withGuessing(guess).build();

        expected = sizeShallowEmptyBuffer + reference.measureArray(empty.array());
        assertEquals("Deep empty ByteBuffer", expected, m2.measureDeep(empty, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep empty ByteBuffer", expected, m2.measureDeep(readOnlyEmpty, SLAB_ALLOCATION_NO_SLICE));

        expected = sizeShallowEmptyBuffer + reference.measureArray(one.array());
        assertEquals("Deep 1-byte ByteBuffer", expected, m2.measureDeep(one, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep 1-byte ByteBuffer", expected, m2.measureDeep(readOnlyOne, SLAB_ALLOCATION_NO_SLICE));

        expected = sizeShallowEmptyBuffer; // detected as slab no bytes
        assertEquals("Deep duplicated ByteBuffer with position shift", expected, m2.measureDeep(emptyPositionShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", expected, m2.measureDeep(readOnlyEmptyPositionShift, SLAB_ALLOCATION_NO_SLICE));

        expected = sizeShallowEmptyBuffer; // detected as slab no bytes
        assertEquals("Deep duplicated ByteBuffer with limit shift", expected, m2.measureDeep(emptyLimitShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with limit shift", expected, m2.measureDeep(readOnlyEmptyLimitShift, SLAB_ALLOCATION_NO_SLICE));

        expected = sizeShallowEmptyBuffer + reference.measureArray(twenty.array());
        assertEquals("Deep ByteBuffer", expected, m2.measureDeep(twenty, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only ByteBuffer", expected, m2.measureDeep(readOnlyTwenty, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only ByteBuffer", expected, m2.measureDeep(readOnlyTwentySlice, SLAB_ALLOCATION_NO_SLICE));

        expected = sizeShallowEmptyBuffer + 5; // detected as slab 5 bytes
        assertEquals("Deep duplicated ByteBuffer with position shift", expected, m2.measureDeep(fivePositionShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", expected, m2.measureDeep(readOnlyFivePositionShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep duplicated ByteBuffer with limit shift", expected, m2.measureDeep(fiveLimitShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep duplicated read-only ByteBuffer with limit shift", expected, m2.measureDeep(readOnlyFiveLimitShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep duplicated ByteBuffer with position and limit shift", expected, m2.measureDeep(fivePositionAndLimitShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep duplicated read-only ByteBuffer with position and limit shift", expected, m2.measureDeep(readOnlyFivePositionAndLimitShift, SLAB_ALLOCATION_NO_SLICE));

        expected = sizeShallowEmptyBuffer + reference.measureArray(twenty.array()); // not detected as slab due to the use of slice
        assertEquals("Deep ByteBuffer slice with position shift", expected, m2.measureDeep(fivePositionShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with position shift", expected, m2.measureDeep(readOnlyFivePositionShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep ByteBuffer slice with limit shift", expected, m2.measureDeep(fiveLimitShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with limit shift", expected, m2.measureDeep(readOnlyFiveLimitShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep ByteBuffer slice with position and limit shift", expected, m2.measureDeep(fivePositionAndLimitShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with position and limit shift", expected, m2.measureDeep(readOnlyFivePositionAndLimitShiftSlice, SLAB_ALLOCATION_NO_SLICE));

        // Test omitting shared overhead for slab WITH slice
        MemoryMeter m3 = MemoryMeter.builder().withGuessing(guess).build();

        expected = sizeShallowEmptyBuffer + reference.measureArray(empty.array());
        assertEquals("Deep empty ByteBuffer", expected, m3.measureDeep(empty, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep empty ByteBuffer", expected, m3.measureDeep(readOnlyEmpty, SLAB_ALLOCATION_SLICE));

        expected = sizeShallowEmptyBuffer + reference.measureArray(one.array());
        assertEquals("Deep 1-byte ByteBuffer", expected, m3.measureDeep(one, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep 1-byte ByteBuffer", expected, m3.measureDeep(readOnlyOne, SLAB_ALLOCATION_SLICE));

        expected = sizeShallowEmptyBuffer + reference.measureArray(emptyPositionShift.array());
        assertEquals("Deep duplicated ByteBuffer with position shift", expected, m3.measureDeep(emptyPositionShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", expected, m3.measureDeep(readOnlyEmptyPositionShift, SLAB_ALLOCATION_SLICE));

        expected = sizeShallowEmptyBuffer + reference.measureArray(emptyLimitShift.array());
        assertEquals("Deep duplicated ByteBuffer with limit shift", expected, m3.measureDeep(emptyLimitShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with limit shift", expected, m3.measureDeep(readOnlyEmptyLimitShift, SLAB_ALLOCATION_SLICE));

        expected = sizeShallowEmptyBuffer + reference.measureArray(twenty.array());
        assertEquals("Deep ByteBuffer", expected, m3.measureDeep(twenty, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer", expected, m3.measureDeep(readOnlyTwenty, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer", expected, m3.measureDeep(readOnlyTwentySlice, SLAB_ALLOCATION_SLICE));

        assertEquals("Deep duplicated ByteBuffer with position shift", expected, m3.measureDeep(fivePositionShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", expected, m3.measureDeep(readOnlyFivePositionShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep duplicated ByteBuffer with limit shift", expected, m3.measureDeep(fiveLimitShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep duplicated read-only ByteBuffer with limit shift", expected, m3.measureDeep(readOnlyFiveLimitShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep duplicated ByteBuffer with position and limit shift", expected, m3.measureDeep(fivePositionAndLimitShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep duplicated read-only ByteBuffer with position and limit shift", expected, m3.measureDeep(readOnlyFivePositionAndLimitShift, SLAB_ALLOCATION_SLICE));

        expected = sizeShallowEmptyBuffer + 5; // detected as slab 5 bytes
        assertEquals("Deep ByteBuffer slice with position shift", expected, m3.measureDeep(fivePositionShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with position shift", expected, m3.measureDeep(readOnlyFivePositionShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep ByteBuffer slice with limit shift", expected, m3.measureDeep(fiveLimitShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with limit shift", expected, m3.measureDeep(readOnlyFiveLimitShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep ByteBuffer slice with position and limit shift", expected, m3.measureDeep(fivePositionAndLimitShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with position and limit shift", expected, m3.measureDeep(readOnlyFivePositionAndLimitShiftSlice, SLAB_ALLOCATION_SLICE));
    }

    @Test
    public void testDirectByteBuffer() {

        // Empty/no slab
        ByteBuffer empty = ByteBuffer.allocateDirect(0);
        ByteBuffer readOnlyEmpty = empty.asReadOnlyBuffer();
        // Not empty/no slab 
        ByteBuffer one = ByteBuffer.allocateDirect(1);
        ByteBuffer readOnlyOne = one.asReadOnlyBuffer();
        // Empty/position shift/slab without slice
        ByteBuffer emptyPositionShift = (ByteBuffer) one.duplicate().position(1);
        ByteBuffer readOnlyEmptyPositionShift = emptyPositionShift.asReadOnlyBuffer();
        // Empty/limit shift/slab without slice
        ByteBuffer emptyLimitShift = (ByteBuffer) one.duplicate().limit(0);
        ByteBuffer readOnlyEmptyLimitShift = emptyLimitShift.asReadOnlyBuffer();
        // Not empty/ no slab
        ByteBuffer twenty = ByteBuffer.allocateDirect(20);
        ByteBuffer readOnlyTwenty = twenty.asReadOnlyBuffer();
        ByteBuffer twentySlice = ByteBuffer.allocateDirect(20).slice();
        ByteBuffer readOnlyTwentySlice = twentySlice.asReadOnlyBuffer();
        // Not empty/position shift/slab without slice
        ByteBuffer fivePositionShift = (ByteBuffer) twenty.duplicate().position(15);
        ByteBuffer readOnlyFivePositionShift = fivePositionShift.asReadOnlyBuffer();
        // Not empty/limit shift/slab without slice
        ByteBuffer fiveLimitShift = (ByteBuffer) twenty.duplicate().limit(5);
        ByteBuffer readOnlyFiveLimitShift = fiveLimitShift.asReadOnlyBuffer();
        // Not empty/position and limit shifts/slab without slice
        ByteBuffer fivePositionAndLimitShift = (ByteBuffer) twenty.duplicate().position(10).limit(15);
        ByteBuffer readOnlyFivePositionAndLimitShift = fivePositionAndLimitShift.asReadOnlyBuffer();
        // Not empty/position shift/slab with slice
        ByteBuffer fivePositionShiftSlice = ((ByteBuffer) twenty.duplicate().position(15)).slice();
        ByteBuffer readOnlyFivePositionShiftSlice = fivePositionShiftSlice.asReadOnlyBuffer();
        // Not empty/limit shift/slab with slice
        ByteBuffer fiveLimitShiftSlice = ((ByteBuffer) twenty.duplicate().limit(5)).slice();
        ByteBuffer readOnlyFiveLimitShiftSlice = fiveLimitShiftSlice.asReadOnlyBuffer();
        // Not empty/position and limit shifts/slab with slice
        ByteBuffer fivePositionAndLimitShiftSlice = ((ByteBuffer) twenty.duplicate().position(10).limit(15)).slice();
        ByteBuffer readOnlyFivePositionAndLimitShiftSlice = fivePositionAndLimitShiftSlice.asReadOnlyBuffer();

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


        // If a DirectByteBuffer is referencing a part of another DirectByteBuffer (read only, duplicate or slice buffer), it will have a 
        // reference to the original buffer through the att (attachement) field and no cleaner
        long sizeDeepWithAttachedBuffer = sizeShallowBuffer + sizeDeepRwBuffer;
        // root [java.nio.DirectByteBufferR] 200 bytes (64 bytes)
        //   |
        //   +--att [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
        //     |
        //     +--cleaner [jdk.internal.ref.Cleaner] 72 bytes (40 bytes)
        //       |
        //       +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
        //
        assertEquals("Deep empty ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(readOnlyEmpty));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(readOnlyOne));

        assertEquals("Deep duplicated ByteBuffer with position shift", sizeDeepWithAttachedBuffer, m1.measureDeep(emptyPositionShift));
        assertEquals("Deep duplicated ByteBuffer with limit shift", sizeDeepWithAttachedBuffer, m1.measureDeep(emptyLimitShift));

        assertEquals("Deep ByteBuffer", sizeDeepRwBuffer, m1.measureDeep(twenty));
        assertEquals("Deep read-only ByteBuffer", sizeDeepWithAttachedBuffer, m1.measureDeep(readOnlyTwenty));

        assertEquals("Deep duplicated ByteBuffer with position shift", sizeDeepWithAttachedBuffer, m1.measureDeep(fivePositionShift));
        assertEquals("Deep duplicated ByteBuffer with limit shift", sizeDeepWithAttachedBuffer, m1.measureDeep(fiveLimitShift));
        assertEquals("Deep duplicated ByteBuffer with position and limit shift", sizeDeepWithAttachedBuffer, m1.measureDeep(fivePositionAndLimitShift));

        // Pre java 12, a DirectByteBuffer created from another DirectByteBuffer was using the source buffer as an attachment
        // for liveness rather than the source buffer's attachment (https://bugs.openjdk.org/browse/JDK-8208362)
        // With Java < 12:
        // root [java.nio.DirectByteBufferR] 264 bytes (64 bytes)
        //  |
        //  +--att [java.nio.DirectByteBuffer] 200 bytes (64 bytes)
        //    |
        //    +--att [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
        //      |
        //      +--cleaner [sun.misc.Cleaner] 72 bytes (40 bytes)
        //      |
        //      +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
        //
        long sizeDeepWithMultiLayersOfReferences = VM.isPreJava12JVM() ? sizeShallowBuffer + sizeDeepWithAttachedBuffer : sizeDeepWithAttachedBuffer;
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyEmptyPositionShift));
        assertEquals("Deep read-only duplicated ByteBuffer with limit shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyEmptyLimitShift));
        assertEquals("Deep read-only ByteBuffer", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyTwentySlice));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyFivePositionShift));
        assertEquals("Deep duplicated read-only ByteBuffer with limit shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyFiveLimitShift));
        assertEquals("Deep duplicated read-only ByteBuffer with position and limit shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyFivePositionAndLimitShift));

        assertEquals("Deep ByteBuffer slice with position shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(fivePositionShiftSlice));
        assertEquals("Deep ByteBuffer slice with limit shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(fiveLimitShiftSlice));
        assertEquals("Deep ByteBuffer slice with position and limit shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(fivePositionAndLimitShiftSlice));

        // With Java < 12:
        // root [java.nio.DirectByteBufferR] 328 bytes (64 bytes)
        //   |
        //   +--att [java.nio.DirectByteBuffer] 264 bytes (64 bytes)
        //     |
        //     +--att [java.nio.DirectByteBuffer] 200 bytes (64 bytes)
        //        |
        //        +--att [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
        //           |
        //           +--cleaner [sun.misc.Cleaner] 72 bytes (40 bytes)
        //           |
        //           +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
        //
        sizeDeepWithMultiLayersOfReferences = VM.isPreJava12JVM() ? (2 * sizeShallowBuffer) + sizeDeepWithAttachedBuffer : sizeDeepWithAttachedBuffer;
        assertEquals("Deep read-only ByteBuffer slice with position shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyFivePositionShiftSlice));
        assertEquals("Deep read-only ByteBuffer slice with limit shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyFiveLimitShiftSlice));
        assertEquals("Deep read-only ByteBuffer slice with position and limit shift", sizeDeepWithMultiLayersOfReferences, m1.measureDeep(readOnlyFivePositionAndLimitShiftSlice));

        // Test omitting shared overhead for slab WITHOUT slice
        MemoryMeter m2 = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Deep empty ByteBuffer", sizeDeepRwBuffer, m2.measureDeep(empty, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepRwBuffer, m2.measureDeep(one, SLAB_ALLOCATION_NO_SLICE));


        assertEquals("Deep empty ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyEmpty, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyOne, SLAB_ALLOCATION_NO_SLICE));

        assertEquals("Deep duplicated ByteBuffer with position shift", sizeShallowBuffer, m2.measureDeep(emptyPositionShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", sizeShallowBuffer, m2.measureDeep(readOnlyEmptyPositionShift, SLAB_ALLOCATION_NO_SLICE));

        assertEquals("Deep duplicated ByteBuffer with limit shift", sizeShallowBuffer, m2.measureDeep(emptyLimitShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with limit shift", sizeShallowBuffer, m2.measureDeep(readOnlyEmptyLimitShift, SLAB_ALLOCATION_NO_SLICE));

        assertEquals("Deep ByteBuffer", sizeDeepRwBuffer, m2.measureDeep(twenty, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only ByteBuffer", sizeDeepWithAttachedBuffer, m2.measureDeep(readOnlyTwenty, SLAB_ALLOCATION_NO_SLICE));

        // Pre java 12, a DirectByteBuffer created from another DirectByteBuffer was using the source buffer as an attachment
        // for liveness rather than the source buffer's attachment (https://bugs.openjdk.org/browse/JDK-8208362)
        // With Java < 12:
        // root [java.nio.DirectByteBufferR] 264 bytes (64 bytes)
        //  |
        //  +--att [java.nio.DirectByteBuffer] 200 bytes (64 bytes)
        //    |
        //    +--att [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
        //      |
        //      +--cleaner [sun.misc.Cleaner] 72 bytes (40 bytes)
        //      |
        //      +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
        //
        sizeDeepWithMultiLayersOfReferences = VM.isPreJava12JVM() ? sizeShallowBuffer + sizeDeepWithAttachedBuffer : sizeDeepWithAttachedBuffer;
        assertEquals("Deep read-only ByteBuffer", sizeDeepWithMultiLayersOfReferences, m2.measureDeep(readOnlyTwentySlice, SLAB_ALLOCATION_NO_SLICE));

        // Even if the is read-only MemoryMeter can efficiently determine that the buffer are slabs 
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", sizeShallowBuffer, m2.measureDeep(readOnlyEmptyPositionShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with limit shift", sizeShallowBuffer, m2.measureDeep(readOnlyEmptyLimitShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", sizeShallowBuffer, m2.measureDeep(readOnlyFivePositionShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep duplicated read-only ByteBuffer with limit shift", sizeShallowBuffer, m2.measureDeep(readOnlyFiveLimitShift, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep duplicated read-only ByteBuffer with position and limit shift", sizeShallowBuffer, m2.measureDeep(readOnlyFivePositionAndLimitShift, SLAB_ALLOCATION_NO_SLICE));

        // With Java < 12, the top slice as capacity == remaining but the attached duplicated buffer as a capacity < remaining and is considered as a slab.
        // Therefore the original buffer is ignored: 
        // root [java.nio.DirectByteBuffer] 128 bytes (64 bytes)
        //   |
        //   +--att [java.nio.DirectByteBuffer] 64 bytes (64 bytes)
        //
        // With Java >= 12, the slice has capacity = remaining and the underlying buffer is the original one so capacity = remaining too.
        sizeDeepWithMultiLayersOfReferences = VM.isPreJava12JVM() ? 2 * sizeShallowBuffer : sizeDeepWithAttachedBuffer;
        assertEquals("Deep ByteBuffer slice with position shift", sizeDeepWithMultiLayersOfReferences, m2.measureDeep(fivePositionShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep ByteBuffer slice with limit shift", sizeDeepWithMultiLayersOfReferences, m2.measureDeep(fiveLimitShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep ByteBuffer slice with position and limit shift", sizeDeepWithMultiLayersOfReferences, m2.measureDeep(fivePositionAndLimitShiftSlice, SLAB_ALLOCATION_NO_SLICE));

        // MemoryMeter does not see the read-only buffer and top slice as a slab but detect that the slice underlying buffer is a slab.
        // With Java < 12:
        // root [java.nio.DirectByteBufferR] 192 bytes (64 bytes)
        //   |
        //   +--att [java.nio.DirectByteBuffer] 128 bytes (64 bytes)
        //     |
        //     +--att [java.nio.DirectByteBuffer] 64 bytes (64 bytes)
        //
        sizeDeepWithMultiLayersOfReferences = VM.isPreJava12JVM() ? (3 * sizeShallowBuffer) : sizeDeepWithAttachedBuffer;
        assertEquals("Deep read-only ByteBuffer slice with position shift", sizeDeepWithMultiLayersOfReferences, m2.measureDeep(readOnlyFivePositionShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with limit shift", sizeDeepWithMultiLayersOfReferences, m2.measureDeep(readOnlyFiveLimitShiftSlice, SLAB_ALLOCATION_NO_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with position and limit shift", sizeDeepWithMultiLayersOfReferences, m2.measureDeep(readOnlyFivePositionAndLimitShiftSlice, SLAB_ALLOCATION_NO_SLICE));

        // Test omitting shared overhead for slab WITH slice
        MemoryMeter m3 = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Deep empty ByteBuffer", sizeDeepRwBuffer, m3.measureDeep(empty, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepRwBuffer, m3.measureDeep(one, SLAB_ALLOCATION_SLICE));


        assertEquals("Deep empty ByteBuffer", sizeDeepWithAttachedBuffer, m3.measureDeep(readOnlyEmpty, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep 1-byte ByteBuffer", sizeDeepWithAttachedBuffer, m3.measureDeep(readOnlyOne, SLAB_ALLOCATION_SLICE));

        assertEquals("Deep duplicated ByteBuffer with position shift", sizeDeepWithAttachedBuffer, m3.measureDeep(emptyPositionShift, SLAB_ALLOCATION_SLICE));
        // Pre java 12, a DirectByteBuffer created from another DirectByteBuffer was using the source buffer as an attachment
        // for liveness rather than the source buffer's attachment (https://bugs.openjdk.org/browse/JDK-8208362)
        // With Java < 12:
        // root [java.nio.DirectByteBufferR] 264 bytes (64 bytes)
        //  |
        //  +--att [java.nio.DirectByteBuffer] 200 bytes (64 bytes)
        //    |
        //    +--att [java.nio.DirectByteBuffer] 136 bytes (64 bytes)
        //      |
        //      +--cleaner [sun.misc.Cleaner] 72 bytes (40 bytes)
        //      |
        //      +--thunk [java.nio.DirectByteBuffer$Deallocator] 32 bytes (32 bytes)
        //
        sizeDeepWithMultiLayersOfReferences = VM.isPreJava12JVM() ? sizeShallowBuffer + sizeDeepWithAttachedBuffer : sizeDeepWithAttachedBuffer;
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", sizeDeepWithMultiLayersOfReferences, m3.measureDeep(readOnlyEmptyPositionShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with limit shift", sizeDeepWithMultiLayersOfReferences, m3.measureDeep(readOnlyEmptyLimitShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer", sizeDeepWithMultiLayersOfReferences, m3.measureDeep(readOnlyTwentySlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only duplicated ByteBuffer with position shift", sizeDeepWithMultiLayersOfReferences, m3.measureDeep(readOnlyFivePositionShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep duplicated read-only ByteBuffer with limit shift", sizeDeepWithMultiLayersOfReferences, m3.measureDeep(readOnlyFiveLimitShift, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep duplicated read-only ByteBuffer with position and limit shift", sizeDeepWithMultiLayersOfReferences, m3.measureDeep(readOnlyFivePositionAndLimitShift, SLAB_ALLOCATION_SLICE));

        // MemoryMetter can identify all the slice as slabs
        assertEquals("Deep ByteBuffer slice with position shift", sizeShallowBuffer, m3.measureDeep(fivePositionShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep ByteBuffer slice with limit shift", sizeShallowBuffer, m3.measureDeep(fiveLimitShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep ByteBuffer slice with position and limit shift", sizeShallowBuffer, m3.measureDeep(fivePositionAndLimitShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with position shift", sizeShallowBuffer, m3.measureDeep(readOnlyFivePositionShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with limit shift", sizeShallowBuffer, m3.measureDeep(readOnlyFiveLimitShiftSlice, SLAB_ALLOCATION_SLICE));
        assertEquals("Deep read-only ByteBuffer slice with position and limit shift", sizeShallowBuffer, m3.measureDeep(readOnlyFivePositionAndLimitShiftSlice, SLAB_ALLOCATION_SLICE));
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
