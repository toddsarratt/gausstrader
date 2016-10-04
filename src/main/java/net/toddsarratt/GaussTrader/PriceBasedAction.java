package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;

/**
 * The class provides a workaround for Java's lack of tuples.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.1
 */

class PriceBasedAction {
	static final PriceBasedAction DO_NOTHING = new PriceBasedAction(Constants.BIGDECIMAL_MINUS_ONE, false, "", null, 0);
	private final BigDecimal triggerPrice;
	private final boolean isActionable;
	private final String buyOrSell;
	private final SecurityType securityType;
	private final int numberToTransact;

	PriceBasedAction(BigDecimal triggerPrice, boolean isActionable, String buyOrSell, SecurityType securityType, int numberToTransact) {
		this.triggerPrice = triggerPrice;
		this.isActionable = isActionable;
		this.buyOrSell = buyOrSell;
		this.securityType = securityType;
		this.numberToTransact = numberToTransact;
	}

	@Override
	public static String toString() {
		return "";
	}

	BigDecimal getTriggerPrice() {
		return triggerPrice;
	}

	boolean isActionable() {
		return isActionable;
	}

	String getBuyOrSell() {
		return buyOrSell;
	}

	SecurityType getSecurityType() {
		return securityType;
	}

	int getNumberToTransact() {
		return numberToTransact;
	}
}
