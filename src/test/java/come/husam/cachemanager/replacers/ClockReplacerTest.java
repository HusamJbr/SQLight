package come.husam.cachemanager.replacers;

import com.husam.cachemanager.replacers.ClockReplacer;
import com.husam.cachemanager.replacers.Replacer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClockReplacerTest {

    private Replacer replacer;
    @Before
    public void setUp() {
        this.replacer = new ClockReplacer(7);
    }
    @Test
    public void sampleTest() {

        // Scenario: unpin six elements, i.e. add them to the replacer.
        replacer.unpin(1);
        replacer.unpin(2);
        replacer.unpin(3);
        replacer.unpin(4);
        replacer.unpin(5);
        replacer.unpin(6);
        replacer.unpin(1);
        assertEquals(6, replacer.size());

        // Scenario: get three victims from the clock.
        int value = replacer.victim();
        assertEquals(1, value);
        value = replacer.victim();
        assertEquals(2, value);
        value = replacer.victim();
        assertEquals(3, value);

        // Scenario: pin elements in the replacer.
        // Note that 3 has already been victimized, so pinning 3 should have no effect.
        replacer.pin(3);
        replacer.pin(4);
        assertEquals(2, replacer.size());

        // Scenario: unpin 4. We expect that the reference bit of 4 will be set to 1.
        replacer.unpin(4);

        // Scenario: continue looking for victims. We expect these victims.
        value = replacer.victim();
        assertEquals(5, value);
        value = replacer.victim();
        assertEquals(6, value);
        value = replacer.victim();
        assertEquals(4, value);
    }
}
