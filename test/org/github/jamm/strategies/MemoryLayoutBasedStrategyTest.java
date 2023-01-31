package org.github.jamm.strategies;

import org.junit.Test;

import static org.github.jamm.strategies.MemoryLayoutBasedStrategy.roundTo;
import static org.junit.Assert.*;

public class MemoryLayoutBasedStrategyTest
{

    @Test
    public void testRoundTo()
    {
        assertEquals(0, roundTo(0, 8));
        assertEquals(8, roundTo(1, 8));
        assertEquals(8, roundTo(2, 8));
        assertEquals(8, roundTo(3, 8));
        assertEquals(8, roundTo(8, 8));
        assertEquals(16, roundTo(9, 8));
        assertEquals(16, roundTo(10, 8));
    }

}
