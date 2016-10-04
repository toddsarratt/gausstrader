package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OptionOrder extends Order to hold Options, specifically.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */

public class OptionOrder extends Order {
	private Option option;

	/*
		TransactionId orderId;
	boolean open;
	String ticker;
	BigDecimal limitPrice;
	PriceBasedAction action;
	BigDecimal claimAgainstCash;
	String tif;
	Instant instantOpened;
	 */
	OptionOrder(Option option, BigDecimal limitPrice, PriceBasedAction action, String tif) {
		this.option = option;
		this.ticker = option.getTicker();
		logger.debug("Assigning ticker = {} ", ticker);
		this.limitPrice = limitPrice;
		logger.debug("limitPrice = ${}", limitPrice);
		this.action = action;
		this.tif = tif;
		open = true;
		instantOpened = Instant.now();
		logger.info("Created order ID {} for {} to {} {} with {} @ ${} TIF : {}", orderId, underlyingTicker, action, totalQuantity, ticker, limitPrice, tif);

	}

	public Option getOption() {
		return option;
	}

	BigDecimal getStrikePrice() {
		return option.getStrike();
	}

	@Override
	BigDecimal calculateClaimAgainstCash() {
		if (action.getBuyOrSell().equals("BUY")) {
			return calculateCostBasis();
		}
		if (option.isPut()) {
			return option.getStrike().multiply(new BigDecimal(action.getNumberToTransact() * 100)).add(costBasis);
		}
		return BigDecimal.ZERO;
	}
}
