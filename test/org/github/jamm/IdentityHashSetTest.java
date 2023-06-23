package org.github.jamm;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class IdentityHashSetTest {
    @Test
    public void addSame() {
        IdentityHashSet s = new IdentityHashSet();
        Object o = new Object();
        assertTrue(s.add(o));
        assertFalse(s.add(o));
        assertEquals(1, s.size);
        assertEquals(32, s.table.length);
    }

    @Test
    public void addMany() {
        List<Object> ref1 = new ArrayList<>();
        List<Object> ref2 = new ArrayList<>();
        for (int i = 0; i < 250; i++)
            assertTrue(ref1.add("o" + i));
        for (int i = 0; i < 250; i++)
            assertTrue(ref2.add("x" + i));

        IdentityHashSet s = new IdentityHashSet();
        for (int i = 0; i < ref1.size(); i++) {
            Object o = ref1.get(i);
            assertEquals(i, s.size);
            assertEquals("" + i, expectedCapacity(i), s.table.length);
            assertTrue(s.add(o));
        }
        assertEquals(ref1.size(), s.size);

        for (Object o : ref1)
            assertFalse(s.add(o));

        for (int i = 0; i < ref2.size(); i++) {
            Object o = ref2.get(i);
            assertEquals(ref1.size() + i, s.size);
            assertTrue(s.add(o));
        }
        assertEquals(ref1.size() + ref2.size(), s.size);

        for (Object o : ref1)
            assertFalse(s.add(o));

        for (Object o : ref2)
            assertFalse(s.add(o));
    }

    private static int expectedCapacity(int i) {
        // the backing array must be at most 2/3 full in order to have enough 'null's in the open-addressing-map
        i = (i * 3) / 2;

        // next power of 2
        i = 1 << (32 - Integer.numberOfLeadingZeros(i));
        return Math.max(32, i);
    }
}