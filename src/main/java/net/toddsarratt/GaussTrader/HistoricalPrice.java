package net.toddsarratt.GaussTrader;

import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

public class HistoricalPrice {
    private final long dateEpoch;
    private final double adjClose;
	
    HistoricalPrice(String dateString, String priceString) {
	MutableDateTime mutableDt = new MutableDateTime(dateString, DateTimeZone.forID("America/New_York"));
	mutableDt.setMillisOfDay( (16 * 60 + 20) * 60 * 1000);
	dateEpoch = mutableDt.getMillis();
	adjClose = Double.parseDouble(priceString);
    }
    long getDateEpoch() {
	return dateEpoch;
    }
    double getAdjClose() {
	return adjClose;
    }
}
