package net.toddsarratt.gaussTrader.domain;

import net.toddsarratt.gaussTrader.singletons.SecurityType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Calls and puts are types of options. But there doesn't really seem to be any reason to create separate classes
 * for each. Basically a PutDao object is an OptionDao with secType = SecurityType.PUT, and conversely a CallDao object is an
 * OptionDao with secType = SecutiryType.CALL. So probably this class will end up being deleted.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */

public class Call extends Option {
	Call(String ticker,
	     LocalDate expiry,
	     String underlyingTicker,
	     BigDecimal strike) {
		super(ticker, SecurityType.CALL, expiry, underlyingTicker, strike);
	}
}