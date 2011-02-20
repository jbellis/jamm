package org.github.jamm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Numbers here are for 64-bit Sun JVM.  Good luck with anything else.
 */
public class MemoryMeterTest
{
    private static final int EMPTY_ARRAY_SIZE = 24;

    @Test
    public void testPrimitives() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals(16, meter.measure(new Object()));
        assertEquals(16, meter.measureDeep(new Object()));

        assertEquals(24, meter.measure(new Integer(0)));
        assertEquals(24, meter.measureDeep(new Integer(0)));

        assertEquals(40, meter.measure(""));
        assertEquals(40 + EMPTY_ARRAY_SIZE, meter.measureDeep(""));
        assertEquals(40, meter.measure("a"));
        assertEquals(40 + EMPTY_ARRAY_SIZE + 8, meter.measureDeep("a"));

        assertEquals(EMPTY_ARRAY_SIZE, meter.measure(new Object[0]));
        Object[] objects = new Object[100];
        assertEquals(EMPTY_ARRAY_SIZE + 8 * 100, meter.measure(objects));
        assertEquals(EMPTY_ARRAY_SIZE + 8 * 100, meter.measureDeep(objects));
        for(int i = 0; i < objects.length; i++) {
            objects[i] = new Object();
        }
        assertEquals(EMPTY_ARRAY_SIZE + 8 * 100 + 16 * 100, meter.measureDeep(objects));
    }

    @Test
    public void testCycle() {
        MemoryMeter meter = new MemoryMeter();

        Recursive dummy = new Recursive();
        assertEquals(32, meter.measure(dummy));
        assertEquals(32, meter.measureDeep(dummy));
        dummy.child = dummy;
        assertEquals(32, meter.measureDeep(dummy));
    }

    @Test
    public void testInheritance() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals(24, meter.measure(new Parent()));
        assertEquals(24, meter.measureDeep(new Parent()));
        assertEquals(32, meter.measure(new Child()));
        assertEquals(32, meter.measureDeep(new Child()));
    }

    @Test
    public void testCollections() {
        MemoryMeter meter = new MemoryMeter();

        assertEquals(144, meter.measureDeep(new ArrayList()));
        assertEquals(216, meter.measureDeep(new HashMap()));
        assertEquals(296, meter.measureDeep(new LinkedHashMap()));
        assertEquals(176, meter.measureDeep(new ReentrantReadWriteLock()));
        assertEquals(192, meter.measureDeep(new ConcurrentSkipListMap()));
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
        assertEquals(32 * 100001, meter.measureDeep(root));
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
