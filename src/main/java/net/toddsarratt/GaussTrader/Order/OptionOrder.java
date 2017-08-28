package net.toddsarratt.GaussTrader.Order;

import net.toddsarratt.GaussTrader.PriceBasedAction;
import net.toddsarratt.GaussTrader.Security.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OptionOrder extends Order to hold Options, specifically.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */

class OptionOrder extends Order {
	private final static Logger LOGGER = LoggerFactory.getLogger(OptionOrder.class);
	private Option option;

	OptionOrder() {
		LOGGER.debug("OptionOrder() created with default constructor. Should only happen when using OrderBuilder");
	}

	OptionOrder(Option option, BigDecimal limitPrice, PriceBasedAction action, String tif) {
		this.option = option;
		this.action = action;
		this.tif = tif;
		this.open = true;
		this.instantOpened = Instant.now();
		this.claimAgainstCash = calculateClaimAgainstCash();
		LOGGER.debug("claimAgainstCash = ${}", claimAgainstCash);
		LOGGER.info("Created order ID {} for {} to {} {} with {} @ ${} TIF : {}",
				orderId, option.getUnderlyingTicker(), action, ticker, limitPrice, tif);
	}

	public Option getOption() {
		return option;
	}

	@Override
	BigDecimal calculateClaimAgainstCash() {
		if (action.getBuyOrSell().equals("BUY")) {
			return calculateCostBasis();
		}
		if (option.isPut()) {
			return option.getStrike().multiply(
					new BigDecimal(action.getNumberToTransact() * 100)).add(calculateCostBasis());
		}
		return BigDecimal.ZERO;
	}
}
