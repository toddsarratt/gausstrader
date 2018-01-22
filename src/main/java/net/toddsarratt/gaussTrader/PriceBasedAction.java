package net.toddsarratt.gaussTrader;

import net.toddsarratt.gaussTrader.singletons.BuyOrSell;
import net.toddsarratt.gaussTrader.singletons.Constants;
import net.toddsarratt.gaussTrader.singletons.SecurityType;

import java.math.BigDecimal;

/**
 * The class provides a workaround for Java's lack of tuples.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.1
 */

public class PriceBasedAction {
	static final PriceBasedAction DO_NOTHING = new PriceBasedAction(Constants.BIGDECIMAL_MINUS_ONE, false, null, null, 0);
	private final BigDecimal triggerPrice;
	private final boolean isActionable;
	private final BuyOrSell buyOrSell;
	private final SecurityType securityType;
	private final int numberToTransact;

	PriceBasedAction(BigDecimal triggerPrice, boolean isActionable, BuyOrSell buyOrSell, SecurityType securityType, int numberToTransact) {
		this.triggerPrice = triggerPrice;
		this.isActionable = isActionable;
		this.buyOrSell = buyOrSell;
		this.securityType = securityType;
		this.numberToTransact = numberToTransact;
	}

	@Override
	public String toString() {
		return "";
	}

	public BigDecimal getTriggerPrice() {
		return triggerPrice;
	}

	public boolean isActionable() {
		return isActionable;
	}

	public BuyOrSell getBuyOrSell() {
		return buyOrSell;
	}

	public SecurityType getSecurityType() {
		return securityType;
	}

	public int getNumberToTransact() {
		return numberToTransact;
	}
}
