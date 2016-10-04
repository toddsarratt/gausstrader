package net.toddsarratt.GaussTrader;

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
public class StockOrder extends Order {
	private final static Logger LOGGER = LoggerFactory.getLogger(StockOrder.class);
	private final Stock stock;

	StockOrder(Stock stock, BigDecimal limitPrice, PriceBasedAction action, String tif) {
		this.stock = stock;
		this.action = action;
		this.tif = tif;
		this.open = true;
		this.instantOpened = Instant.now();
		this.claimAgainstCash = calculateClaimAgainstCash();
		LOGGER.debug("claimAgainstCash = ${}", claimAgainstCash);
		LOGGER.info("Created order ID {} for {} to {} {} with {} @ ${} TIF : {}",
				orderId, stock.getTicker(), action, ticker, limitPrice, tif);
	}

	@Override
	BigDecimal calculateClaimAgainstCash() {
		return action.getBuyOrSell().equals("BUY") ? calculateCostBasis() : BigDecimal.ZERO;
	}
}
