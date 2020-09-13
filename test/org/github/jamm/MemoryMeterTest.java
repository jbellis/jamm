package org.github.jamm;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assume.assumeThat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
* Numbers here are for 64-bit Sun JVM.  Good luck with anything else.
*/
@RunWith(Parameterized.class)
public class MemoryMeterTest
{
    // JVM memory structure is like follows
    // a ref and a header for all objects
    // always align to 8 byte boundary no matter what
    // separate fields for each class in the family tree
    // for a specific class, group: (depending on the jvm there may be optimized packing)
    //   - longs, doubles (aligned on 8 byte boundary)
    //   - ints, floats (aligned on 4 byte boundary)
    //   - shorts, chars (aligned on 2 byte boundary)
    //   - bytes, booleans (aligned on 1 byte boundary)
    //   - padding
    //   - last come refs
    //
    // for array, last comes the array body (refs or primitives)
    //
    // Mac OS X seems very good at packing and seems to use 4 byte references even on 64-bit!!!

    @SuppressWarnings("deprecation")
    @Parameterized.Parameters
    public static Collection<MemoryMeter.Guess> guesses() {
        List<MemoryMeter.Guess> guesses = new ArrayList<>();
        if (MemoryMeterInstrumentation.hasInstrumentation())
            guesses.add(MemoryMeter.Guess.ALWAYS_INSTRUMENTATION);
        if (MemoryMeterUnsafe.hasUnsafe())
            guesses.add(MemoryMeter.Guess.ALWAYS_UNSAFE);
        guesses.add(MemoryMeter.Guess.ALWAYS_SPEC);
        return guesses;
    }

    private final MemoryMeter.Guess guess;

    public MemoryMeterTest(MemoryMeter.Guess guess)
    {
        this.guess = guess;
    }
    
    static final int REFERENCE_SIZE = sizeOfReference();
    static final int HEADER_SIZE = sizeOfHeader();
    static final int OBJECT_SIZE = pad(HEADER_SIZE + REFERENCE_SIZE);

    public static int sizeOfReference() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(MemoryMeter.Guess.NEVER).build();

        // this is probably the safest way to get the reference size
        // does imply that we are testing non-pure
        // but there is so much variability, this at least will give
        // consistent answers from an "approximate" data source
        long objectArraySize = meter.measure(new Object[123]);
        if (meter.measure(new long[123]) == objectArraySize) return 8;
        if (meter.measure(new int[123]) == objectArraySize) return 4;

        // OK there is strangeness going on... we'll ask some well known sources
        try {
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafe.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object theUnsafe = unsafeField.get(null);
            Method addressSize = unsafe.getMethod("addressSize");
            Object size = addressSize.invoke(theUnsafe);
            return ((Number) size).intValue();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            // ignore
        }
        String sadm = System.getProperty("sun.arch.data.model");
        if (sadm != null) {
            if ("32".equals(sadm)) return 4;
            if ("64".equals(sadm)) return 8;
            if ("128".equals(sadm)) return 16;
        }
        String oa = System.getProperty("os.arch");
        if (oa != null) {
            if (oa.contains("64")) return 8;
            if (oa.contains("32")) return 4;
            if ("x86".equals(oa)) return 4;
            if ("amd".equals(oa)) return 4;
        }
        String jvi = System.getProperty("java.vm.info");
        if (jvi != null) {
            if (jvi.contains("x86-32")) return 4;
        }
        String jvn = System.getProperty("java.vm.name");
        if (jvn != null) {
            if (jvn.toLowerCase().contains("64-bit")) return 8;
            if (jvn.toLowerCase().contains("32-bit")) return 4;
        }
        // ok we'll pretend it's 32-bit
        return 4;
    }

    @SuppressWarnings("unused")
    static class ByteHolder {
        private byte value;
    }

    @SuppressWarnings("unused")
    static class TwoByteHolder {
        private byte value;
        private byte overflow;
    }

    @SuppressWarnings("unused")
    static class ThreeByteHolder {
        private byte value;
        private byte other1;
        private byte overflow;
    }

    @SuppressWarnings("unused")
    static class FourByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte overflow;
    }

    @SuppressWarnings("unused")
    static class FiveByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte overflow;
    }

    @SuppressWarnings("unused")
    static class SixByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte overflow;
    }

    @SuppressWarnings("unused")
    static class SevenByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte other4;
        private byte other5;
        private byte overflow;
    }

    @SuppressWarnings("unused")
    static class EightByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte other4;
        private byte other5;
        private byte other6;
        private byte overflow;
    }

    @SuppressWarnings("unused")
    static class NineByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte other4;
        private byte other5;
        private byte other6;
        private byte other7;
        private byte overflow;
    }

    public static int sizeOfHeader() {
        // The Mac seems to pack things right in!
        MemoryMeter meter = MemoryMeter.builder().withGuessing(MemoryMeter.Guess.NEVER).build();
        long b0 = meter.measure(new Object()) - REFERENCE_SIZE;
        long b1 = meter.measure(new ByteHolder()) - REFERENCE_SIZE;
        long b2 = meter.measure(new TwoByteHolder()) - REFERENCE_SIZE;
        long b3 = meter.measure(new ThreeByteHolder()) - REFERENCE_SIZE;
        long b4 = meter.measure(new FourByteHolder()) - REFERENCE_SIZE;
        long b5 = meter.measure(new FiveByteHolder()) - REFERENCE_SIZE;
        long b6 = meter.measure(new SixByteHolder()) - REFERENCE_SIZE;
        long b7 = meter.measure(new SevenByteHolder()) - REFERENCE_SIZE;
        long b8 = meter.measure(new EightByteHolder()) - REFERENCE_SIZE;
        long b9 = meter.measure(new NineByteHolder()) - REFERENCE_SIZE;
        if (b0 == b8 && b0 != b9) return (int)b0 - 8;
        if (b0 == b7 && b0 != b8) return (int)b0 - 7;
        if (b0 == b6 && b0 != b7) return (int)b0 - 6;
        if (b0 == b5 && b0 != b6) return (int)b0 - 5;
        if (b0 == b4 && b0 != b5) return (int)b0 - 4;
        if (b0 == b3 && b0 != b4) return (int)b0 - 3;
        if (b0 == b2 && b0 != b3) return (int)b0 - 2;
        if (b0 == b1 && b0 != b2) return (int)b0 - 1;
        return (int)b0;
    }

    public static int pad(int size) {
        if (size % 8 == 0) return size;
        return (size | 7) + 1;
    }

    public static int objectSize(int numLongDoubles, int numIntFloats, int numShortChars, int numByteBools, int numRefs) {
        int size = REFERENCE_SIZE + HEADER_SIZE;
        size += numLongDoubles * 8;
        size += numIntFloats * 4;
        size += numShortChars * 2;
        size += numByteBools;
        // now pad to ref-size boundary
        if (size % REFERENCE_SIZE != 0) {
            size = (size | (REFERENCE_SIZE - 1)) + 1;
        }
        size += numRefs * REFERENCE_SIZE;
        return pad(size);
    }

    public static int arraySize(int numElements) {
        int size = REFERENCE_SIZE + HEADER_SIZE + 4;
        if (numElements > 0) {
        // now pad to ref-size boundary
        if (size % REFERENCE_SIZE != 0) {
            size = (size | (REFERENCE_SIZE - 1)) + 1;
        }
        size += numElements * REFERENCE_SIZE;
        }
        return pad(size);
    }

    public static int byteArraySize(int numElements) {
        int size = REFERENCE_SIZE + HEADER_SIZE + 4;
        if (numElements > 0) {
        // now pad to ref-size boundary
        if (size % REFERENCE_SIZE != 0) {
            size = (size | (REFERENCE_SIZE - 1)) + 1;
        }
        size += numElements;
        }
        return pad(size);
    }

    public static int charArraySize(int numElements) {
        int size = REFERENCE_SIZE + HEADER_SIZE + 4;
        if (numElements > 0) {
        // now pad to ref-size boundary
        if (size % REFERENCE_SIZE != 0) {
            size = (size | (REFERENCE_SIZE - 1)) + 1;
        }
        size += numElements * 2;
        }
        return pad(size);
    }

    public static int intArraySize(int numElements) {
        int size = REFERENCE_SIZE + HEADER_SIZE + 4;
        if (numElements > 0) {
        // now pad to ref-size boundary
        if (size % REFERENCE_SIZE != 0) {
            size = (size | (REFERENCE_SIZE - 1)) + 1;
        }
        size += numElements * 4;
        }
        return pad(size);
    }

    public static int longArraySize(int numElements) {
        int size = REFERENCE_SIZE + HEADER_SIZE + 4;
        if (numElements > 0) {
        // now pad to ref-size boundary
        if (size % REFERENCE_SIZE != 0) {
            size = (size | (REFERENCE_SIZE - 1)) + 1;
        }
        size += numElements * 8;
        }
        return pad(size);
    }

    @Test
    public void testObjectArraySizes() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Shallow size of Object[0] " + guess, arraySize(0), meter.measure(new Object[0]));

        assertEquals("Shallow size of Object[1] " + guess, arraySize(1), meter.measure(new Object[1]));

        assertEquals("Shallow size of Object[256] " + guess, arraySize(256), meter.measure(new Object[256]));
    }

    @Test
    public void testByteArraySizes() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Shallow size of byte[0] " + guess, byteArraySize(0), meter.measure(new byte[0]));

        assertEquals("Shallow size of byte[1] " + guess, byteArraySize(1), meter.measure(new byte[1]));

        assertEquals("Shallow size of byte[256] " + guess, byteArraySize(256), meter.measure(new byte[256]));
    }

    @Test
    public void testCharArraySizes() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Shallow size of char[0] " + guess, charArraySize(0), meter.measure(new char[0]));

        assertEquals("Shallow size of char[1] " + guess, charArraySize(1), meter.measure(new char[1]));

        assertEquals("Shallow size of char[256] " + guess, charArraySize(256), meter.measure(new char[256]));
    }

    @Test
    public void testIntArraySizes() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Shallow size of int[0] " + guess, intArraySize(0), meter.measure(new int[0]));

        assertEquals("Shallow size of int[1] " + guess, intArraySize(1), meter.measure(new int[1]));

        assertEquals("Shallow size of int[256] " + guess, intArraySize(256), meter.measure(new int[256]));
    }

    @Test
    public void testLongArraySizes() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Shallow size of long[0] " + guess, longArraySize(0), meter.measure(new long[0]));

        assertEquals("Shallow size of long[1] " + guess, longArraySize(1), meter.measure(new long[1]));

        assertEquals("Shallow size of long[256] " + guess, longArraySize(256), meter.measure(new long[256]));
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

    @SuppressWarnings("deprecation")
    @Test
    public void test_x86_64() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();
        assumeThat(System.getProperty("os.arch"), anyOf(is("x86_64"), is("amd64")));
        assertEquals("no embedded long field " + guess, 24, meter.measure(new LongHolder()));
        assertEquals("Embedded int field " + guess, 16, meter.measure(new IntHolder()));
        assertEquals("Embedded char field " + guess, 16, meter.measure(new CharHolder()));
        assertEquals("Embedded char field * 2 " + guess, 16, meter.measure(new TwoCharHolder()));
        assertEquals("Embedded char field * 3 " + guess, 24, meter.measure(new ThreeCharHolder()));
        assertEquals("Embedded int field only " + guess, 24, meter.measure(new IntCharHolder()));
        assertEquals("Only 4 bytes available " + guess, 24, meter.measure(new FiveByteHolder()));

        assertEquals("4 bytes always available " + guess,
                     (guess != MemoryMeter.Guess.ALWAYS_SPEC) ? 24 : 32, // "Specification" mode overcounts here
                     meter.measure(new LongIntHolder()));

        // The assumes a very JVM-specific object layout
        // assertEquals("4 bytes not available if parent has a field " + guess, 32, meter.measure(new LongIntHolder2()));

        assertEquals(meter.measure(new int[16384]), meter.measure(new Object[16384]));
    }

    @Test
    public void test_i386() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();
        assumeThat(System.getProperty("os.arch"), is("i386"));
        assertEquals("Room for 1 long " + guess, 16, meter.measure(new LongHolder()));
        assertEquals("Room for 1 int " + guess, 16, meter.measure(new IntHolder()));
        assertEquals("Room for 1 char " + guess, 16, meter.measure(new CharHolder()));
        assertEquals("Room for 2 chars " + guess, 16, meter.measure(new TwoCharHolder()));
        assertEquals("Room for 3 chars " + guess, 16, meter.measure(new ThreeCharHolder()));
        assertEquals("Room for 1 int and 1 char " + guess, 16, meter.measure(new IntCharHolder()));
        assertEquals("Room for 5 bytes " + guess, 16, meter.measure(new FiveByteHolder()));
        assertEquals("Room for 7 bytes " + guess, 16, meter.measure(new SevenByteHolder()));
        assertEquals("Room for 8 bytes " + guess, 16, meter.measure(new EightByteHolder()));
        assertEquals("Room for 9 bytes " + guess, 24, meter.measure(new NineByteHolder()));
        assertEquals("Room for 1 long and 1 int " + guess, 24, meter.measure(new LongIntHolder()));
        // The assumes a very JVM-specific object layout
        // assertEquals("4 bytes not available to child classes " + guess, 24, meter.measure(new LongIntHolder2()));
        assertEquals(meter.measure(new int[16384]), meter.measure(new Object[16384]));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPrimitives() {
        assumeThat(guess, not(is(MemoryMeter.Guess.ALWAYS_SPEC)));

        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Shallow size of Object " + guess, OBJECT_SIZE, meter.measure(new Object()));
        assertEquals("Deep size of Object " + guess, OBJECT_SIZE, meter.measureDeep(new Object()));

        assertEquals("Shallow size of Long " + guess, objectSize(1, 0, 0, 0, 0), meter.measure(0L));
        assertEquals("Deep size of Long " + guess, objectSize(1, 0, 0, 0, 0), meter.measureDeep(0L));

        assertEquals("Shallow size of Integer " + guess, objectSize(0, 1, 0, 0, 0), meter.measure(0));
        assertEquals("Deep size of Integer " + guess, objectSize(0, 1, 0, 0, 0), meter.measureDeep(0));

        assertEquals("Shallow size of empty String " + guess, objectSize(0, 0, 4, 0, 0), meter.measure(""));
        assertEquals("Deep size of empty String " + guess, objectSize(0, 0, 4, 0, 0) + charArraySize(0), meter.measureDeep(""));
        assertEquals("Shallow size of one-character String " + guess, objectSize(0, 0, 4, 0, 0), meter.measure("a"));
        assertEquals("Deep size of one-character String " + guess, objectSize(0, 0, 4, 0, 0) + charArraySize(1), meter.measureDeep("a"));

        assertEquals("Shallow size of empty array of objects " + guess, arraySize(0), meter.measure(new Object[0]));
        Object[] objects = new Object[100];
        assertEquals("Shallow size of Object[100] containing all nulls " + guess, arraySize(100), meter.measure(objects));
        assertEquals("Deep size of Object[100] containing all nulls " + guess, arraySize(100), meter.measureDeep(objects));
        for(int i = 0; i < objects.length; i++) {
            objects[i] = new Object();
        }
        assertEquals("Shallow size of Object[100] containing new Object()s " + guess, arraySize(100) + OBJECT_SIZE * 100, meter.measureDeep(objects));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testByteBuffer() {
        assumeThat(guess, not(is(MemoryMeter.Guess.ALWAYS_SPEC)));

        ByteBuffer empty = ByteBuffer.allocate(0);
        ByteBuffer one = ByteBuffer.allocate(1);
        ByteBuffer emptyOne = one.duplicate();
        emptyOne.position(1);
        ByteBuffer b = ByteBuffer.allocate(1000);
        ByteBuffer sl900 = b.duplicate();
        sl900.position(100);

        MemoryMeter m1 = MemoryMeter.builder().withGuessing(guess).build();
        MemoryMeter m2 = m1.unbuild().omitSharedBufferOverhead().build();

        // ByteBuffer (shallow) object sizes depend on the JDK version
        int BYTEBUFFER_SIZE = (int) MemoryMeterInstrumentation.instrumentation.getObjectSize(ByteBuffer.allocate(10));

        assertEquals("Shallow empty ByteBuffer " + guess, BYTEBUFFER_SIZE, m1.measure(empty));
        assertEquals("Deep empty ByteBuffer " + guess, BYTEBUFFER_SIZE + byteArraySize(0), m1.measureDeep(empty));
        // 8 is apparently the minimum number of bytes allocated
        assertEquals("Deep 1-byte ByteBuffer " + guess, BYTEBUFFER_SIZE + byteArraySize(1), m1.measureDeep(one));
        assertEquals("Deep duplicated 1-byte ByteBuffer " + guess, BYTEBUFFER_SIZE + byteArraySize(1), m1.measureDeep(emptyOne));

        // there are hard-coded 64-bit arch values when omiting shared buffer overhead
        assertEquals(BYTEBUFFER_SIZE, m2.measure(empty));
        assertEquals(BYTEBUFFER_SIZE, m2.measureDeep(empty));
        assertEquals(BYTEBUFFER_SIZE + 1, m2.measureDeep(one)); // as of 0.2.4 we don't count the bytes!!!
        assertEquals(BYTEBUFFER_SIZE, m2.measureDeep(emptyOne));

        MemoryMeter m3 = m1.unbuild().onlyShallowByteBuffers().build();
        assertEquals(BYTEBUFFER_SIZE, m3.measure(empty));
        assertEquals(BYTEBUFFER_SIZE, m3.measureDeep(empty));
        assertEquals(BYTEBUFFER_SIZE, m3.measureDeep(one));
        assertEquals(BYTEBUFFER_SIZE, m3.measureDeep(emptyOne));
        assertEquals(BYTEBUFFER_SIZE, m3.measureDeep(ByteBuffer.allocate(1000)));

        MemoryMeter m4 = m1.unbuild().byteBuffersHeapOnlyNoSlice().build();
        assertEquals(BYTEBUFFER_SIZE, m4.measure(empty));
        assertEquals(BYTEBUFFER_SIZE + m1.measure(new byte[0]), m4.measureDeep(empty));
        assertEquals(BYTEBUFFER_SIZE + m1.measure(new byte[1]), m4.measureDeep(one));
        assertEquals(0, m4.measureDeep(emptyOne));
        assertEquals(BYTEBUFFER_SIZE + m1.measure(new byte[1000]), m4.measureDeep(ByteBuffer.allocate(1000)));
        assertEquals(900, m4.measureDeep(sl900));
    }

    @Test
    public void testCycle() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        Recursive dummy = new Recursive();
        assertEquals("Shallow size of Recursive object " + guess, objectSize(0, 1, 0, 0, 1), meter.measure(dummy));
        assertEquals("Deep size of Recursive is shallow size when child==null " + guess, meter.measure(dummy), meter.measureDeep(dummy));
        dummy.child = dummy;
        assertEquals("Deep size of Recursive is shallow size when child==this " + guess, meter.measure(dummy), meter.measureDeep(dummy));
    }

    @Test
    public void testInheritance() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("Shallow size of Parent " + guess, objectSize(0, 1, 0, 0, 0), meter.measure(new Parent()));
        assertEquals("Deep size of Parent " + guess, objectSize(0, 1, 0, 0, 0), meter.measureDeep(new Parent()));
        assertEquals("Shallow size of Child " + guess, objectSize(0, 2, 0, 0, 0), meter.measure(new Child()));
        assertEquals("Deep size of Parent " + guess, objectSize(0, 2, 0, 0, 0), meter.measureDeep(new Child()));
    }

    @Test
    @Ignore("These vary quite radically depending on the JVM.")
    public void testCollections() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        assertEquals("sizeOf ArrayList",
                objectSize(0, 2, 0, 0, 1) // the object itself
                        + arraySize(10), // the backing array's initial load factor
                meter.measureDeep(new ArrayList<>()));
        assertEquals("sizeOf HashMap",
                objectSize(0, 4, 0, 0, 4) // the object itself
                        + arraySize(16), // the backing array
                meter.measureDeep(new HashMap<>()));
        assertEquals("sizeOf LinkedHashMap",
                objectSize(0, 4, 0, 1, 5)  // the object itself
                        + arraySize(16) // the inherited backing array
                        + objectSize(0, 1, 0, 0, 5), // the first node
                meter.measureDeep(new LinkedHashMap<>()));

        // I give up for the ones below!
        assertEquals("sizeOf ReentrantReadWriteLock " + guess, 176, meter.measureDeep(new ReentrantReadWriteLock()));
        assertEquals("sizeOf ConcurrentSkipListMap " + guess, 192, meter.measureDeep(new ConcurrentSkipListMap<>()));
    }

    @Test
    public void testDeep() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        Recursive root = new Recursive();
        Recursive recursive = root;
        for (int i = 0; i < 100000; i++) {
            recursive.child = new Recursive();
            recursive = recursive.child;
        }
        assertEquals("for " + guess, objectSize(0, 1, 0, 0, 1) * 100001, meter.measureDeep(root));
    }

    @SuppressWarnings("unused")
    private static class Parent {
        private int i;
    }

    @SuppressWarnings("unused")
    private static class Child extends Parent {
        private int j;
    }

    @SuppressWarnings("unused")
    private static class Recursive {
        int i;
        Recursive child = null;
    }
    
    @Test
    public void testIgnoreKnownSingletons() {
    	MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();
    	
    	long classFieldSize = meter.measureDeep(new HasClassField());
    	long enumFieldSize = meter.measureDeep(new HasEnumField());
    	
    	meter = meter.unbuild().ignoreKnownSingletons().build();
    	
    	assertNotEquals("classField " + guess, classFieldSize, meter.measureDeep(new HasClassField()));
    	assertNotEquals("enumField " + guess, enumFieldSize, meter.measureDeep(new HasEnumField()));
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testIgnoreNonStrongReferences() {
    	MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();
    	
    	long classFieldSize = meter.measureDeep(new HasReferenceField());

    	meter = meter.unbuild().ignoreNonStrongReferences().build();
    	
    	assertNotEquals("classField " + guess, classFieldSize, meter.measureDeep(new HasClassField()));
    }

    @SuppressWarnings({ "unused", "FieldMayBeFinal" })
    private static class HasClassField {
    	private Class<?> cls = String.class;
    }

    @SuppressWarnings({ "unused", "FieldMayBeFinal" })
    private static class HasEnumField {
    	enum Giant {Fee, Fi, Fo, Fum}
    	
    	private Giant grunt = Giant.Fee;
    }

    @SuppressWarnings({ "unused", "FieldMayBeFinal" })
    private static class HasReferenceField {
    	private SoftReference<Date> ref = new SoftReference<>(new Date());
    }

    @Test
    public void testUnmeteredAnnotationOnFields() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        String s = "test";

        long stringSize = meter.measureDeep(s);
        long withoutSize = meter.measureDeep(new WithoutAnnotationField(null));
        assertEquals("for " + guess, stringSize + withoutSize, meter.measureDeep(new WithoutAnnotationField(s)));
    }

    @Test
    public void testUnmeteredTypeAnnotation() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        String s = "test";
        assertEquals("for " + guess, 0, meter.measureDeep(new WithTypeAnnotation(s)));
    }

    @Test
    public void testUnmeteredAnnotationOnParent() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        String s = "test";
        assertEquals("for " + guess, 0, meter.measureDeep(new WithParentWithAnnotation(s)));
    }

    @Test
    public void testUnmeteredAnnotationOnFieldParent() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        long withoutSize = meter.measureDeep(new WithFieldWithAnnotatedParent(null));

        WithParentWithAnnotation field = new WithParentWithAnnotation("test");
        long withSize = meter.measureDeep(new WithFieldWithAnnotatedParent(field));
        assertEquals("for " + guess, withoutSize, withSize);
    }

    @Test
    public void testUnmeteredAnnotationOnFieldInterface() {
        MemoryMeter meter = MemoryMeter.builder().withGuessing(guess).build();

        long withoutSize = meter.measureDeep(new WithFieldAnnotatedInterface(null));

        AnnotatedInterface field = new AnnotatedInterface() {
        };
        long withSize = meter.measureDeep(new WithFieldAnnotatedInterface(field));
        assertEquals("for " + guess, withoutSize, withSize);
    }

    @SuppressWarnings({ "unused", "FieldCanBeLocal", "FieldMayBeFinal" })
    private static class WithoutAnnotationField {
        private String s;

        public WithoutAnnotationField(String s) {
            this.s = s;
        }
    }

    @Unmetered
    @SuppressWarnings({ "unused", "FieldCanBeLocal", "FieldMayBeFinal" })
    private static class WithTypeAnnotation {
        private String s;

        public WithTypeAnnotation(String s) {
            this.s = s;
        }
    }

    private static class WithParentWithAnnotation extends WithTypeAnnotation {

        public WithParentWithAnnotation(String s) {
            super(s);
        }
    }

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private static class WithFieldWithAnnotatedParent {

        private final WithParentWithAnnotation field;

        public WithFieldWithAnnotatedParent(WithParentWithAnnotation field) {
            this.field = field;
        }
    }

    @Unmetered
    private interface AnnotatedInterface {

    }

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private static class WithFieldAnnotatedInterface {

        private final AnnotatedInterface field;

        public WithFieldAnnotatedInterface(AnnotatedInterface field) {
            this.field = field;
        }
    }
}
