package org.github.jamm;

import java.util.ArrayList;
import java.util.List;

import org.github.jamm.MemoryMeterBase.VisitedSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VisitedSetTest
{
    @Test
    public void addSame()
    {
        VisitedSet s = new VisitedSet();
        Object o = new Object();
        assertTrue(s.add(o));
        assertFalse(s.add(o));
        assertEquals(1, s.size);
        assertEquals(16, s.table.length);
    }

    @Test
    public void addMany()
    {
        List<Object> ref1 = new ArrayList<>();
        List<Object> ref2 = new ArrayList<>();
        for (int i = 0; i < 250; i++)
            assertTrue(ref1.add("o" + i));
        for (int i = 0; i < 250; i++)
            assertTrue(ref2.add("x" + i));

        VisitedSet s = new VisitedSet();
        for (int i = 0; i < ref1.size(); i++)
        {
            Object o = ref1.get(i);
            assertEquals(i, s.size);
            assertEquals(expectedCapacity(i), s.table.length);
            assertTrue(s.add(o));
        }
        assertEquals(ref1.size(), s.size);

        for (Object o : ref1)
            assertFalse(s.add(o));

        for (int i = 0; i < ref2.size(); i++)
        {
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

    private int expectedCapacity(int i)
    {
        // 3 as the "magic size factor" to have enough 'null's in the open-addressing-map
        i *= 3;

        // next power of 2
        i--;
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i++;
        return Math.max(16, i);
    }
}
