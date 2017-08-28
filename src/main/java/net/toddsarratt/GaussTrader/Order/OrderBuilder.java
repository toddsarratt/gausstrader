package net.toddsarratt.GaussTrader.Order;

import net.toddsarratt.GaussTrader.Security.SecurityType;
import net.toddsarratt.GaussTrader.TransactionId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Builder class for Order objects. Useful when recreating Orders saved to persistent storage.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class OrderBuilder {
	private Order order;

	private OrderBuilder(Order order) {
		this.order = order;
	}

	public static OrderBuilder of(SecurityType securityType) {
		switch (securityType) {
			case CALL:
			case PUT:
				return new OrderBuilder(new OptionOrder());
			case STOCK:
				return new OrderBuilder(new StockOrder());
		}
		throw new IllegalArgumentException("Must call OrderBuilder.of() with a valid SecurityType");
	}

	public Order build() {
		return order;
	}

	public OrderBuilder orderId(TransactionId orderId) {
		order.setOrderId(orderId);
		return this;
	}

	public OrderBuilder open(boolean open) {
		order.setOpen(open);
		return this;
	}

	public OrderBuilder ticker(String ticker) {
		order.setTicker(ticker);
		return this;
	}

	public OrderBuilder instantOpened(Instant instantOpened) {
		order.setInstantOpened(instantOpened);
		return this;
	}

	public OrderBuilder claimAgainstCash(BigDecimal claimAgainstCash) {
		order.setClaimAgainstCash(claimAgainstCash);
		return this;
	}

	public OrderBuilder instantClosed(Instant instantClosed) {
		order.setInstantClosed(instantClosed);
		return this;
	}
}
