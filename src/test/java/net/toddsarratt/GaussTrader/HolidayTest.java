package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HolidayTest {
   @Test
   public void testIsMarketHoliday() {
      assertTrue(TradingSession.isMarketHoliday(new DateTime(2014, 12, 25, 0, 0, DateTimeZone.forID("America/New_York"))));
      assertFalse(TradingSession.isMarketHoliday(new DateTime(2014, 10, 22, 0, 0, DateTimeZone.forID("America/New_York"))));
      assertTrue(TradingSession.isMarketHoliday(new MutableDateTime(2015, 12, 25, 12, 0, 0, 0)));
      assertFalse(TradingSession.isMarketHoliday(new MutableDateTime(2015, 10, 21, 12, 0, 0, 0)));
      assertTrue(TradingSession.isMarketHoliday(359, 2014));
      assertFalse(TradingSession.isMarketHoliday(69, 2015));
      assertTrue(TradingSession.isMarketHoliday(359, 2015));
      assertFalse(TradingSession.isMarketHoliday(358, 2015));
   }
}
