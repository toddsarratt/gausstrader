package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by tsarratt on 9/24/2016.
 */
public class Put extends Option {

	Put(String ticker,
	    SecurityType secType,
	    LocalDate expiry,
	    String underlyingTicker,
	    BigDecimal strike) {
		this.ticker = ticker;
		this.secType = secType;
		this.expiry = expiry;
		this.underlyingTicker = underlyingTicker;
		this.strike = strike;
	}
}
