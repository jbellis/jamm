package org.github.jamm;

import org.junit.Test;

import static org.github.jamm.MathUtils.roundTo;
import static org.junit.Assert.*;

public class MathUtilsTest
{
    @Test
    public void testRoundToInt()
    {
        assertEquals(0, roundTo(0, 8));
        assertEquals(8, roundTo(1, 8));
        assertEquals(8, roundTo(2, 8));
        assertEquals(8, roundTo(3, 8));
        assertEquals(8, roundTo(8, 8));
        assertEquals(16, roundTo(9, 8));
        assertEquals(16, roundTo(10, 8));
    }

    @Test
    public void testRoundToLong()
    {
        assertEquals(0, roundTo(0L, 8));
        assertEquals(8, roundTo(1L, 8));
        assertEquals(8, roundTo(2L, 8));
        assertEquals(8, roundTo(3L, 8));
        assertEquals(8, roundTo(8L, 8));
        assertEquals(16, roundTo(9L, 8));
        assertEquals(16, roundTo(10L, 8));
    }
    
    @Test
    public void testModulo()
    {
        assertEquals(0, 0 & 7);
        assertEquals(1, 1 & 7);
        assertEquals(2, 2 & 7);
        assertEquals(5, 5 & 7);
        assertEquals(0, 8 & 7);
        assertEquals(1, 9 & 7);
        assertEquals(4, 12 & 7);
    }
}
