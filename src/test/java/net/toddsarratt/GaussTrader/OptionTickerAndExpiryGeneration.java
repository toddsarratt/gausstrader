package net.toddsarratt.GaussTrader;

import java.io.IOException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class OptionTickerAndExpiryGeneration {
    @Test public void testExpiry() {

	assertEquals(Option.getExpirySaturday(11, 2013), 16);
	assertEquals(Option.getExpirySaturday(12, 2013), 21);
	assertEquals(Option.getExpirySaturday(1, 2014), 18);
	assertEquals(Option.getExpirySaturday(2, 2014), 15);
    }

    @Test public void testOptionTickerGeneration() {
	DateTime midAugustDay = new DateTime(2013, 8, 17, 0, 0, 0, 0, DateTimeZone.forID("America/New_York"));
	assertEquals(Option.optionTicker("XOM", midAugustDay, 'C', 90.00), "XOM130817C00090000");
    }

    @Test public void testOptionCreation() {
	Option testIbmCall = new Option("IBM130222C00200000");
	assertEquals(testIbmCall.getTicker(), "IBM130222C00200000", "Expected IBM130222C00200000 but got " + testIbmCall.getTicker());
	assertEquals(testIbmCall.getSecType(), "CALL");
	assertTrue(testIbmCall.getExpiry() != null);
	assertEquals(testIbmCall.getUnderlyingTicker(), "IBM");

	Option testXomPut = new Option("XOM130720P00070000");
	assertEquals(testXomPut.getTicker(), "XOM130720P00070000", "Expected XOM130720P00070000 but got " + testXomPut.getTicker());
	assertEquals(testXomPut.getSecType(), "PUT");
	assertTrue(testXomPut.getExpiry() != null);
	assertEquals(testXomPut.getUnderlyingTicker(), "XOM");
    }
}