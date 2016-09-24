package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;

/**
 * The class provides a workaround for Java's lack of tuples.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.1
 */

public class PriceBasedAction {
	static final PriceBasedAction DO_NOTHING = new PriceBasedAction(Constants.BIGDECIMAL_MINUS_ONE, false, "", "", 0);
	private final BigDecimal triggerPrice;
	private final boolean isActionable;
	private final String buyOrSell;
	private final String securityType;
	private final int numberToTransact;

	PriceBasedAction(BigDecimal triggerPrice, boolean isActionable, String buyOrSell, String securityType, int numberToTransact) {
		this.triggerPrice = triggerPrice
		this.isActionable = isActionable;
		this.buyOrSell = buyOrSell;
		this.securityType = securityType;
		this.numberToTransact = numberToTransact;
	}

	BigDecimal getTriggerPrice() {
		return triggerPrice;
	}

	public boolean isActionable() {
		return isActionable;
	}

	public String getBuyOrSell() {
		return buyOrSell;
	}

	public String getSecurityType() {
		return securityType;
	}

	public int getNumberToTransact() {
		return numberToTransact;
	}
}
