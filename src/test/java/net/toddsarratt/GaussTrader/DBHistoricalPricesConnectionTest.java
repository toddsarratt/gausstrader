package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class DBHistoricalPricesConnectionTest {
   DateTime earliestCloseDate = new DateTime(DateTimeZone.forID("America/New_York")).minusDays(20);

   @Test
   public void testGetDBPrices() {
      assertTrue((DBHistoricalPrices.getDBPrices("AAPL", earliestCloseDate)).size() >= 0);
      assertTrue((DBHistoricalPrices.getDBPrices("MMM", earliestCloseDate)).size() >= 0);
      assertTrue((DBHistoricalPrices.getDBPrices("MU", earliestCloseDate)).size() >= 0);
   }
}
