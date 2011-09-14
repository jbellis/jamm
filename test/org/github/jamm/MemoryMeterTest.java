package org.github.jamm;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Ignore;
import org.junit.Test;
import sun.net.www.MeteredStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Numbers here are for 64-bit Sun JVM.  Good luck with anything else.
 */
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

    static final int REFERENCE_SIZE = sizeOfReference();
    static final int HEADER_SIZE = sizeOfHeader();
    static final int OBJECT_SIZE = pad(HEADER_SIZE + REFERENCE_SIZE);

    public static int sizeOfReference() {
        MemoryMeter meter = new MemoryMeter();

        // this is probably the safest way to get the reference size
        // does imply that we are testing non-pure
        // but there is so much variability, this at least will give
        // consistent answers from an "approximate" data source
        long objectArraySize = meter.measure(new Object[123]);
        if (meter.measure(new long[123]) == objectArraySize) return 8;
        if (meter.measure(new int[123]) == objectArraySize) return 4;

        // OK there is strangeness going on... we'll ask some well known sources
        try {
            Class unsafe = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafe.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object theUnsafe = unsafeField.get(null);
            Method addressSize = unsafe.getMethod("addressSize");
            Object size = addressSize.invoke(theUnsafe);
            int i = ((Number) size).intValue();
            return i;
        } catch (ClassNotFoundException e) {
            // ignore
        } catch (NoSuchFieldException e) {
            // ignore
        } catch (IllegalAccessException e) {
            // ignore
        } catch (NoSuchMethodException e) {
            // ignore
        } catch (InvocationTargetException e) {
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

    static class ByteHolder {
        private byte value;
    }

    static class TwoByteHolder {
        private byte value;
        private byte overflow;
    }

    static class ThreeByteHolder {
        private byte value;
        private byte other1;
        private byte overflow;
    }

    static class FourByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte overflow;
    }

    static class FiveByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte overflow;
    }

    static class SixByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte overflow;
    }

    static class SevenByteHolder {
        private byte value;
        private byte other1;
        private byte other2;
        private byte other3;
        private byte other4;
        private byte other5;
        private byte overflow;
    }

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
        MemoryMeter meter = new MemoryMeter();
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
        MemoryMeter meter = new MemoryMeter();

        assertEquals("Shallow size of Object[0]", arraySize(0), meter.measure(new Object[0]));

        assertEquals("Shallow size of Object[1]", arraySize(1), meter.measure(new Object[1]));

        assertEquals("Shallow size of Object[256]", arraySize(256), meter.measure(new Object[256]));
    }

    @Test
    public void testByteArraySizes() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals("Shallow size of byte[0]", byteArraySize(0), meter.measure(new byte[0]));

        assertEquals("Shallow size of byte[1]", byteArraySize(1), meter.measure(new byte[1]));

        assertEquals("Shallow size of byte[256]", byteArraySize(256), meter.measure(new byte[256]));
    }

    @Test
    public void testCharArraySizes() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals("Shallow size of char[0]", charArraySize(0), meter.measure(new char[0]));

        assertEquals("Shallow size of char[1]", charArraySize(1), meter.measure(new char[1]));

        assertEquals("Shallow size of char[256]", charArraySize(256), meter.measure(new char[256]));
    }

    @Test
    public void testIntArraySizes() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals("Shallow size of int[0]", intArraySize(0), meter.measure(new int[0]));

        assertEquals("Shallow size of int[1]", intArraySize(1), meter.measure(new int[1]));

        assertEquals("Shallow size of int[256]", intArraySize(256), meter.measure(new int[256]));
    }

    @Test
    public void testLongArraySizes() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals("Shallow size of long[0]", longArraySize(0), meter.measure(new long[0]));

        assertEquals("Shallow size of long[1]", longArraySize(1), meter.measure(new long[1]));

        assertEquals("Shallow size of long[256]", longArraySize(256), meter.measure(new long[256]));
    }

    static class LongHolder {
        private long value;
    }

    static class IntHolder {
        private int value;
    }

    static class CharHolder {
        private char value;
    }

    static class TwoCharHolder {
        private char value;
        private char other;
    }

    static class ThreeCharHolder {
        private char value;
        private char other;
        private char overflow;
    }

    static class IntCharHolder {
        private int value;
        private char other;
    }

    static class LongIntHolder {
        private long value;
        private int other;
    }

    static class LongIntHolder2 extends LongHolder {
        private int other;
    }

    @Test
    public void testMacOSX_x86_64() {
        // Mac OS X seems to have a way to stash 4 bytes away in a plain object
        MemoryMeter meter = new MemoryMeter();
        assumeThat(System.getProperty("os.name"), is("Mac OS X"));
        assumeThat(System.getProperty("os.arch"), is("x86_64"));
        assertEquals("no embedded long field", 24, meter.measure(new LongHolder()));
        assertEquals("Embedded int field", 16, meter.measure(new IntHolder()));
        assertEquals("Embedded char field", 16, meter.measure(new CharHolder()));
        assertEquals("Embedded char field * 2", 16, meter.measure(new TwoCharHolder()));
        assertEquals("Embedded char field * 3", 24, meter.measure(new ThreeCharHolder()));
        assertEquals("Embedded int field only", 24, meter.measure(new IntCharHolder()));
        assertEquals("Only 4 bytes available", 24, meter.measure(new FiveByteHolder()));
        assertEquals("4 bytes always available", 24, meter.measure(new LongIntHolder()));
        assertEquals("4 bytes not available if parent has a field", 32, meter.measure(new LongIntHolder2()));
        assertEquals(meter.measure(new int[16384]), meter.measure(new Object[16384]));
    }

    @Test
    public void testMacOSX_i386() {
        // Mac OS X seems to have a way to stash 4 bytes away in a plain object
        MemoryMeter meter = new MemoryMeter();
        assumeThat(System.getProperty("os.name"), is("Mac OS X"));
        assumeThat(System.getProperty("os.arch"), is("i386"));
        assertEquals("Room for 1 long", 16, meter.measure(new LongHolder()));
        assertEquals("Room for 1 int", 16, meter.measure(new IntHolder()));
        assertEquals("Room for 1 char", 16, meter.measure(new CharHolder()));
        assertEquals("Room for 2 chars", 16, meter.measure(new TwoCharHolder()));
        assertEquals("Room for 3 chars", 16, meter.measure(new ThreeCharHolder()));
        assertEquals("Room for 1 int and 1 char", 16, meter.measure(new IntCharHolder()));
        assertEquals("Room for 5 bytes", 16, meter.measure(new FiveByteHolder()));
        assertEquals("Room for 7 bytes", 16, meter.measure(new SevenByteHolder()));
        assertEquals("Room for 8 bytes", 16, meter.measure(new EightByteHolder()));
        assertEquals("Room for 9 bytes", 24, meter.measure(new NineByteHolder()));
        assertEquals("Room for 1 long and 1 int", 24, meter.measure(new LongIntHolder()));
        assertEquals("4 bytes not available to child classes", 24, meter.measure(new LongIntHolder2()));
        assertEquals(meter.measure(new int[16384]), meter.measure(new Object[16384]));
    }

    @Test
    public void testPrimitives() {

        MemoryMeter meter = new MemoryMeter();

        assertEquals("Shallow size of Object", OBJECT_SIZE, meter.measure(new Object()));
        assertEquals("Deep size of Object", OBJECT_SIZE, meter.measureDeep(new Object()));

        assertEquals("Shallow size of Long", objectSize(1, 0, 0, 0, 0), meter.measure(new Long(0)));
        assertEquals("Deep size of Long", objectSize(1, 0, 0, 0, 0), meter.measureDeep(new Long(0)));

        assertEquals("Shallow size of Integer", objectSize(0, 1, 0, 0, 0), meter.measure(new Integer(0)));
        assertEquals("Deep size of Integer", objectSize(0, 1, 0, 0, 0), meter.measureDeep(new Integer(0)));

        assertEquals("Shallow size of empty String", objectSize(0, 4, 0, 0, 0), meter.measure(""));
        assertEquals("Deep size of empty String", objectSize(0, 4, 0, 0, 0) + charArraySize(0), meter.measureDeep(""));
        assertEquals("Shallow size of one-character String", objectSize(0, 4, 0, 0, 0), meter.measure("a"));
        assertEquals("Deep size of one-character String", objectSize(0, 4, 0, 0, 0) + charArraySize(1), meter.measureDeep("a"));

        assertEquals("Shallow size of empty array of objects", arraySize(0), meter.measure(new Object[0]));
        Object[] objects = new Object[100];
        assertEquals("Shallow size of Object[100] containing all nulls", arraySize(100), meter.measure(objects));
        assertEquals("Deep size of Object[100] containing all nulls", arraySize(100), meter.measureDeep(objects));
        for(int i = 0; i < objects.length; i++) {
            objects[i] = new Object();
        }
        assertEquals("Shallow size of Object[100] containing new Object()s", arraySize(100) + OBJECT_SIZE * 100, meter.measureDeep(objects));
    }

    @Test
    public void testByteBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        ByteBuffer one = ByteBuffer.allocate(1);
        ByteBuffer emptyOne = (ByteBuffer) one.duplicate().position(1);

        MemoryMeter m1 = new MemoryMeter();
        MemoryMeter m2 = m1.omitSharedBufferOverhead();

        // from Object
        // ref*2
        // from Buffer
        // long
        // int * 4
        // from ByteBuffer
        // int
        // boolean * 3
        // padding
        // byte[]
        int BYTEBUFFER_SIZE = objectSize(1, 5, 0, 3, 1);

        assertEquals("Shallow empty ByteBuffer", BYTEBUFFER_SIZE, m1.measure(empty));
        assertEquals("Deep empty ByteBuffer", BYTEBUFFER_SIZE + byteArraySize(0), m1.measureDeep(empty));
        // 8 is apparently the minimum number of bytes allocated
        assertEquals("Deep 1-byte ByteBuffer", BYTEBUFFER_SIZE + byteArraySize(1), m1.measureDeep(one));
        assertEquals("Deep duplicated 1-byte ByteBuffer", BYTEBUFFER_SIZE + byteArraySize(1), m1.measureDeep(emptyOne));

        // there are hard-coded 64-bit arch values when omiting shared buffer overhead
        assertEquals(BYTEBUFFER_SIZE, m2.measure(empty));
        assertEquals(BYTEBUFFER_SIZE, m2.measureDeep(empty));
        assertEquals(BYTEBUFFER_SIZE + 1, m2.measureDeep(one)); // as of 0.2.4 we don't count the bytes!!!
        assertEquals(BYTEBUFFER_SIZE, m2.measureDeep(emptyOne));
    }

    @Test
    public void testCycle() throws Exception {
        MemoryMeter meter = new MemoryMeter();

        Recursive dummy = new Recursive();
        assertEquals("Shallow size of Recursive object", objectSize(0, 1, 0, 0, 1), meter.measure(dummy));
        assertEquals("Deep size of Recursive is shallow size when child==null", meter.measure(dummy), meter.measureDeep(dummy));
        dummy.child = dummy;
        assertEquals("Deep size of Recursive is shallow size when child==this", meter.measure(dummy), meter.measureDeep(dummy));
    }

    @Test
    public void testInheritance() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals("Shallow size of Parent", objectSize(0, 1, 0, 0, 0), meter.measure(new Parent()));
        assertEquals("Deep size of Parent", objectSize(0, 1, 0, 0, 0), meter.measureDeep(new Parent()));
        assertEquals("Shallow size of Child", objectSize(0, 2, 0, 0, 0), meter.measure(new Child()));
        assertEquals("Deep size of Parent", objectSize(0, 2, 0, 0, 0), meter.measureDeep(new Child()));
    }

    @Test
    @Ignore("These vary quite radically depending on the JVM.")
    public void testCollections() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals("sizeOf ArrayList",
                objectSize(0, 2, 0, 0, 1) // the object itself
                        + arraySize(10), // the backing array's initial load factor
                meter.measureDeep(new ArrayList()));
        assertEquals("sizeOf HashMap",
                objectSize(0, 4, 0, 0, 4) // the object itself
                        + arraySize(16), // the backing array
                meter.measureDeep(new HashMap()));
        assertEquals("sizeOf LinkedHashMap",
                objectSize(0, 4, 0, 1, 5)  // the object itself
                        + arraySize(16) // the inherited backing array
                        + objectSize(0, 1, 0, 0, 5), // the first node
                meter.measureDeep(new LinkedHashMap()));

        // I give up for the ones below!
        assertEquals("sizeOf ReentrantReadWriteLock", 176, meter.measureDeep(new ReentrantReadWriteLock()));
        assertEquals("sizeOf ConcurrentSkipListMap", 192, meter.measureDeep(new ConcurrentSkipListMap()));
    }

    @Test
    public void testDeep() {
        MemoryMeter meter = new MemoryMeter();

        Recursive root = new Recursive();
        Recursive recursive = root;
        for (int i = 0; i < 100000; i++) {
            recursive.child = new Recursive();
            recursive = recursive.child;
        }
        assertEquals(objectSize(0, 1, 0, 0, 1) * 100001, meter.measureDeep(root));
    }

    private static class Parent {
        private int i;
    }

    private static class Child extends Parent {
        private int j;
    }

    private static class Recursive {
        int i;
        Recursive child = null;
    }
}
