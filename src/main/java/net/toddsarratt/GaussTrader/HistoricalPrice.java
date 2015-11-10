package net.toddsarratt.GaussTrader;

import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

import java.math.BigDecimal;

public class HistoricalPrice {
   private final long dateEpoch;
   private final BigDecimal adjClose;

   HistoricalPrice(String dateString, String priceString) {
      MutableDateTime mutableDt = new MutableDateTime(dateString, DateTimeZone.forID("America/New_York"));
      mutableDt.setMillisOfDay((16 * 60 + 20) * 60 * 1000);
      dateEpoch = mutableDt.getMillis();
      adjClose = new BigDecimal(priceString);
   }

   long getDateEpoch() {
      return dateEpoch;
   }

   BigDecimal getAdjClose() {
      return adjClose;
   }
}