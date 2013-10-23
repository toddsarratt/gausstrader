package net.toddsarratt.GaussTrader;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class OptionExpiryTest {
    @Test public void testGetExpirySaturday() {
        assertEquals(Option.calculateFutureExpiry(12, 2013), 21);
	assertEquals(Option.calculateFutureExpiry(1, 2014), 18);
        assertEquals(Option.calculateFutureExpiry(2, 2014), 22);
        assertEquals(Option.calculateFutureExpiry(3, 2014), 22);
        assertEquals(Option.calculateFutureExpiry(4, 2014), 19);
        assertEquals(Option.calculateFutureExpiry(5, 2014), 17);
        assertEquals(Option.calculateFutureExpiry(6, 2014), 21);
        assertEquals(Option.calculateFutureExpiry(7, 2014), 19);
        assertEquals(Option.calculateFutureExpiry(8, 2014), 16);
        assertEquals(Option.calculateFutureExpiry(9, 2014), 20);
        assertEquals(Option.calculateFutureExpiry(10, 2014), 18);
        assertEquals(Option.calculateFutureExpiry(11, 2014), 22);
        assertEquals(Option.calculateFutureExpiry(12, 2014), 20);
        assertEquals(Option.calculateFutureExpiry(1, 2015), 17);
    }
}
