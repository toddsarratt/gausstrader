package net.toddsarratt.gaussTrader.orders;

import net.toddsarratt.gaussTrader.PriceBasedAction;
import net.toddsarratt.gaussTrader.domain.Option;
import net.toddsarratt.gaussTrader.domain.Stock;
import net.toddsarratt.gaussTrader.singletons.BuyOrSell;
import net.toddsarratt.gaussTrader.singletons.SecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

import static net.toddsarratt.gaussTrader.singletons.BuyOrSell.BUY;
import static net.toddsarratt.gaussTrader.singletons.SecurityType.CALL;
import static net.toddsarratt.gaussTrader.singletons.SecurityType.PUT;

/**
 * Class to record each order
 * Fields :
 * orderId : Use gaussTrader.generateNewId() to populate
 * open : boolean, open or closed order
 * ticker : security being traded
 * limitPrice : Limit orders only
 * Action : "BUY" or "SELL"
 * totalQuantity : probably 1 contract, could be 100 shares :: REPLACED WITH ACTION
 * secType : "CALL", "PUT", "STOCK" :: REPLACED WITH ACTION
 * tif (Time in Force) : "GTC" (Good 'Til Cancelled) vs "GFD" (Good For Day = day order)
 * epochOpened : milliseconds since epoch when order was opened
 * instantClosed : milliseconds since epoch when order was closed
 * closeReason : "FILLED", "EXPIRED", or "CANCELLED"
 * fillPrice : price at which order was filled and closed
 */

public abstract class Order {
	private final static Logger LOGGER = LoggerFactory.getLogger(Option.class);
	long orderId;
	boolean open;
	Security security;
	BuyOrSell buyOrSell;
	BigDecimal limitPrice;
	PriceBasedAction action;
	BigDecimal claimAgainstCash;
	String tif;
	Instant instantOpened;
	Instant instantClosed;
	String closeReason;
	BigDecimal fillPrice;
	private String underlyingTicker;
	private int totalQuantity;

	Order() {
	}

	public static Order of(Security security, BigDecimal limitPrice, PriceBasedAction action, String tif) {
		LOGGER.debug("Entering Order factory method of(Security {}, BigDecimal {}, PriceBasedAction {}, String {})",
				security, limitPrice.toPlainString(), action, tif);
		LOGGER.debug("Security is type {}", security.getSecType());
		if (action.getSecurityType() == CALL || action.getSecurityType() == PUT) {
			LOGGER.debug("Security is an option");
			return new OptionOrder((Option) security, limitPrice, action, tif);
		}
		return new StockOrder((Stock) security, limitPrice, action, tif);
	}

	public boolean isCall() {
		return security.getSecType() == CALL;
	}

	public boolean isPut() {
		return security.getSecType() == PUT;
	}

	public long getOrderId() {
		return orderId;
	}

	void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	public boolean isOpen() {
		return open;
	}

	void setOpen(boolean open) {
		this.open = open;
	}

	BigDecimal getLimitPrice() {
		return limitPrice;
	}

	void setLimitPrice(BigDecimal limitPrice) {
		this.limitPrice = limitPrice;
	}

	public PriceBasedAction getAction() {
		return action;
	}

	void setAction(PriceBasedAction action) {
		this.action = action;
	}

	public String getTif() {
		return tif;
	}

	void setTif(String tif) {
		this.tif = tif;
	}

	public void fill(BigDecimal fillPrice) {
		LOGGER.debug("Entering fill(${})", fillPrice.toPlainString());
		this.closeReason = "FILLED";
		this.open = false;
		this.fillPrice = fillPrice;
		this.instantClosed = Instant.now();
		LOGGER.info("Order {} {} @ ${} epoch {}", orderId, closeReason, this.fillPrice, instantClosed);
	}

	public void closeExpired() {
		close("EXPIRED");
	}

	public void closeCancelled() {
		close("CANCELLED");
	}

	private void close(String closeReason) {
		LOGGER.debug("Entering close()");
		this.closeReason = closeReason;
		this.open = false;
		this.fillPrice = BigDecimal.ZERO;
		this.instantClosed = Instant.now();
		LOGGER.info("Order {} {} @ ${} epoch {}", this.orderId, closeReason, fillPrice, instantClosed);
	}

	Instant getInstantOpened() {
		return instantOpened;
	}

	void setInstantOpened(Instant instantOpened) {
		this.instantOpened = instantOpened;
	}

	Instant getInstantClosed() {
		return instantClosed;
	}

	void setInstantClosed(Instant instantClosed) {
		this.instantClosed = instantClosed;
	}

	String getCloseReason() {
		return closeReason;
	}

	public void setCloseReason(String closeReason) {
		this.closeReason = closeReason;
	}

	BigDecimal getFillPrice() {
		return fillPrice;
	}

	public void setFillPrice(BigDecimal fillPrice) {
		this.fillPrice = fillPrice;
	}

	public BigDecimal getClaimAgainstCash() {
		return claimAgainstCash;
	}

	void setClaimAgainstCash(BigDecimal claimAgainstCash) {
		this.claimAgainstCash = claimAgainstCash;
	}

	BigDecimal calculateCostBasis() {
		LOGGER.debug("Entering Order.calculateClaimAgainstCash()");
		BigDecimal costBasis = limitPrice.multiply(
				new BigDecimal(
						action.getNumberToTransact()
								* (action.getSecurityType().equals(SecurityType.STOCK) ? 1.0 : 100.0)
								* (action.getBuyOrSell().equals("BUY") ? 1.0 : -1.0)));
		LOGGER.debug("costBasis = ${}", costBasis);
		return costBasis;
	}

	abstract BigDecimal calculateClaimAgainstCash();

	public boolean canBeFilled(BigDecimal lastTick) {
		return (lastTick.compareTo(BigDecimal.ZERO) > 0) &&
				(action.getBuyOrSell() == BUY ?
						(lastTick.compareTo(limitPrice) <= 0) : (lastTick.compareTo(limitPrice) >= 0));
	}

	public Security getSecurity() {
		return security;
	}

	public void setSecurity(Security security) {
		this.security = security;
	}

	@Override
	public String toString() {
		return (orderId + " " + action + " " + security.getTicker() + " " + " @ $" + limitPrice);
	}

	public String getUnderlyingTicker() {
		return underlyingTicker;
	}

	public BuyOrSell getBuyOrSell() {
		return buyOrSell;
	}

	public int getTotalQuantity() {
		return totalQuantity;
	}
}
