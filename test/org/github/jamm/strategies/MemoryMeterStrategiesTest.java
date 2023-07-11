package org.github.jamm.strategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.github.jamm.MemoryMeter.Guess;

import static org.github.jamm.MemoryMeter.Guess.INSTRUMENTATION;
import static org.github.jamm.MemoryMeter.Guess.INSTRUMENTATION_AND_SPECIFICATION;
import static org.github.jamm.MemoryMeter.Guess.SPECIFICATION;
import static org.github.jamm.MemoryMeter.Guess.UNSAFE;
import static org.junit.Assert.*;

public class MemoryMeterStrategiesTest {

    @Test
    public void testInvalidEmptyGuessList() {
        MemoryMeterStrategies strategies = MemoryMeterStrategies.getInstance();
        assertTrue(strategies.hasInstrumentation());
        assertTrue(strategies.hasUnsafe());

        List<Guess> guesses = new ArrayList<>();
        try {
            strategies.getStrategy(guesses);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("The guessList argument is empty", e.getMessage());
        }

        guesses = Arrays.asList(INSTRUMENTATION, UNSAFE, SPECIFICATION);
        assertTrue(strategies.getStrategy(guesses) instanceof InstrumentationStrategy);

        guesses = Arrays.asList(INSTRUMENTATION_AND_SPECIFICATION, UNSAFE, SPECIFICATION);
        assertTrue(strategies.getStrategy(guesses) instanceof InstrumentationAndSpecStrategy);

        guesses = Arrays.asList(UNSAFE, INSTRUMENTATION);
        assertInvalidOrder(strategies, guesses);

        guesses = Arrays.asList(UNSAFE, INSTRUMENTATION_AND_SPECIFICATION);
        assertInvalidOrder(strategies, guesses);

        guesses = Arrays.asList(SPECIFICATION, INSTRUMENTATION);
        assertInvalidOrder(strategies, guesses);

        guesses = Arrays.asList(SPECIFICATION, INSTRUMENTATION_AND_SPECIFICATION);
        assertInvalidOrder(strategies, guesses);

        guesses = Arrays.asList(SPECIFICATION, UNSAFE);
        assertInvalidOrder(strategies, guesses);

        guesses = Arrays.asList(INSTRUMENTATION_AND_SPECIFICATION, INSTRUMENTATION);
        assertInvalidOrder(strategies, guesses);
    }

    private void assertInvalidOrder(MemoryMeterStrategies strategies, List<Guess> guesses) {
        try {
            strategies.getStrategy(guesses);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
}
