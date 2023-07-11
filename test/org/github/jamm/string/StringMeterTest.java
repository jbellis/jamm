package org.github.jamm.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.github.jamm.MemoryMeter;
import org.github.jamm.MemoryMeter.Guess;
import org.github.jamm.MemoryMeterStrategy;
import org.github.jamm.strategies.MemoryMeterStrategies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(Parameterized.class)
public class StringMeterTest {

    @BeforeClass
    public static void turnOffStringOptimization() {
        System.setProperty("org.github.jamm.string.Optimize", "false");
    }

    @Parameterized.Parameters
    public static Collection<Guess> guesses() {

        return Arrays.asList(Guess.INSTRUMENTATION,
                             Guess.INSTRUMENTATION_AND_SPECIFICATION,
                             Guess.UNSAFE,
                             Guess.SPECIFICATION);
    }

    private final Guess guess;

    public StringMeterTest(Guess guess) {
        this.guess = guess;
    }

    @Test
    public void testMeasureDeepString() {

        String[] strings = new String[] {"",
                                         "a",
                                         "a bit longuer",
                                         "significantly longuer",
                                         "...... really ...... really .... really ... really .... longuer",
                                         "with a chinese character: 我"};

        MemoryMeter reference = MemoryMeter.builder().withGuessing(MemoryMeter.Guess.INSTRUMENTATION).build();
        assertFalse(MemoryMeter.useStringOptimization());
        StringMeter stringMeter = StringMeter.newInstance();
        List<Guess> guesses = new ArrayList<>();
        guesses.add(guess);
        MemoryMeterStrategy strategy = MemoryMeterStrategies.getInstance().getStrategy(guesses);

        for (String string : strings) {
            assertEquals(reference.measureDeep(string), stringMeter.measureDeep(strategy, string));
        }
     }
}
