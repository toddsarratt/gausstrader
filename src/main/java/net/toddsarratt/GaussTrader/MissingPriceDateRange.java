package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

class MissingPriceDateRange {
    DateTime latest;
    DateTime earliest = new DateTime(DateTimeZone.forID("America/New_York"));

    /* Class HistoricalPrice uses long dateEpoch to store dates at 4pm + 20min for yahoo! delay */
    /*
    MissingPriceDateRange() {
	latest.setMillisOfDay( (16 * 60 + 20) * 60 * 1000);
	earliest = latest.copy();
    }
    */
}
