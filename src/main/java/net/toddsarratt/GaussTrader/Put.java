package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Calls and puts are types of options. But there doesn't really seem to be any reason to create separate classes
 * for each. Basically a Put object is an Option with secType = SecurityType.PUT, and conversely a Call object is an
 * Option with secType = SecutiryType.CALL. So probably this class will end up being deleted.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */

public class Put extends Option {
	Put(String ticker,
	    LocalDate expiry,
	    String underlyingTicker,
	    BigDecimal strike) {
		super(ticker, SecurityType.PUT, expiry, underlyingTicker, strike);
	}
}
