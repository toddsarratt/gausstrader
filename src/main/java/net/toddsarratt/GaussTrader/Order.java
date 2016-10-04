package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Class to record each order
 * Fields :
 * orderId : Use GaussTrader.generateNewId() to populate
 * open : boolean, open or closed order
 * ticker : security being traded
 * limitPrice : Limit orders only
 * Action : "BUY" or "SELL"
 * totalQuantity : probably 1 contract, could be 100 shares
 * secType : "CALL", "PUT", "STOCK"
 * tif (Time in Force) : "GTC" (Good 'Til Cancelled) vs "GFD" (Good For Day = day order)
 * epochOpened : milliseconds since epoch when order was opened
 * instantClosed : milliseconds since epoch when order was closed
 * closeReason : "FILLED", "EXPIRED", or "CANCELLED"
 * fillPrice : price at which order was filled and closed
 */

abstract class Order {
	final Logger logger = LoggerFactory.getLogger(getClass());
	TransactionId orderId;
	boolean open;
	String ticker;
	BigDecimal limitPrice;
	PriceBasedAction action;
	BigDecimal claimAgainstCash;
	String tif;
	Instant instantOpened;
	Instant instantClosed;
	String closeReason;
	BigDecimal fillPrice;

	Order() {
	}

	Order of(Security security, BigDecimal limitPrice, PriceBasedAction action, String tif) {
		logger.debug("Entering Order factory method of(Security {}, BigDecimal {}, PriceBasedAction {}, String {})",
				security, limitPrice.toPlainString(), action, tif);
		logger.debug("Security is type {}", security.getSecType());
		if ((action.getSecurityType().equals(SecurityType.CALL))
				|| (action.getSecurityType().equals(SecurityType.PUT))) {
			logger.debug("Security is an option");
			return new OptionOrder((Option) security, limitPrice, action, tif);
		}
		return new StockOrder((Stock) security, limitPrice, action, tif);
		calculateClaimAgainstCash();
		logger.debug("claimAgainstCash = ${}", claimAgainstCash);
	}

	TransactionId getOrderId() {
		return orderId;
	}

//	void setOrderId(TransactionId orderId) { this.orderId = orderId;	}

	boolean isOpen() {
		return open;
	}

	void setOpen(boolean open) {
		this.open = open;
	}

	public String getTicker() {
		return ticker;
	}

	void setTicker(String ticker) {
		this.ticker = ticker;
	}

	BigDecimal getLimitPrice() {
		return limitPrice;
	}

	void setLimitPrice(BigDecimal limitPrice) {
		this.limitPrice = limitPrice;
	}

	PriceBasedAction getAction() {
		return action;
	}

	void setAction(String action) {
		this.action = action;
	}

	int getTotalQuantity() {
		return totalQuantity;
	}

//	void setTotalQuantity(int totalQuantity) {this.totalQuantity = totalQuantity;}

	String getSecType() {
		return secType;
	}

//	void setSecType(SecurityType secType) {this.secType = secType;}

	String getTif() {
		return tif;
	}

	/*

	void setTif(String tif) {
		this.tif = tif;
	}

	public boolean isOption() {
		return isPut() || isCall();
	}

	public boolean isCall() {
		return secType.equals("CALL");
	}

	public boolean isPut() {
		return secType.equals("PUT");
	}

	public boolean isStock() {
		return secType.equals("STOCK");
	}

	public boolean isLong() {
		return action.getBuyOrSell().equals("BUY");
	}

	public boolean isShort() {
		return !isLong();
	}
*/
	/* Write changes in order to database */
	void fill(BigDecimal fillPrice) {
		logger.debug("Entering Order.fill(double {})", fillPrice);
		closeReason = "FILLED";
		open = false;
		this.fillPrice = fillPrice;
		instantClosed = Instant.now();
		logger.info("Order {} {} @ ${} epoch {}", orderId, closeReason, this.fillPrice, instantClosed);
	}

	void closeExpired() {
		close("EXPIRED");
	}

	public void closeCancelled() {
		close("CANCELLED");
	}

	private void close(String closeReason) {
		logger.debug("Entering close()");
		this.closeReason = closeReason;
		open = false;
		fillPrice = BigDecimal.ZERO;
		instantClosed = Instant.now();
		logger.info("Order {} {} @ ${} epoch {}", this.orderId, closeReason, fillPrice, instantClosed);

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

	void setCloseReason(String closeReason) {
		this.closeReason = closeReason;
	}

	BigDecimal getFillPrice() {
		return fillPrice;
	}

	void setFillPrice(BigDecimal fillPrice) {
		this.fillPrice = fillPrice;
	}

	void setExpiry(Instant expiry) {
		this.expiry = expiry;
	}

	Instant getExpiry() {
		return expiry;
	}

	void setExpiry(long expiryMillis) {
		this.expiry = Instant.ofEpochMilli(expiryMillis);
	}

	String getUnderlyingTicker() {
		return underlyingTicker;
	}

	void setUnderlyingTicker(String underlyingTicker) {
		this.underlyingTicker = underlyingTicker;
	}


	BigDecimal getClaimAgainstCash() {
		return claimAgainstCash;
	}

	void setClaimAgainstCash(BigDecimal requiredCash) {
		claimAgainstCash = requiredCash;
	}

	BigDecimal calculateCostBasis() {
		logger.debug("Entering Order.calculateClaimAgainstCash()");
		BigDecimal costBasis = limitPrice.multiply(
				new BigDecimal(
						action.getNumberToTransact()
								* (action.getSecurityType().equals(SecurityType.STOCK) ? 1.0 : 100.0)
								* (action.getBuyOrSell().equals("BUY") ? 1.0 : -1.0)));
		logger.debug("costBasis = ${}", costBasis);
		return costBasis;
	}

	abstract BigDecimal calculateClaimAgainstCash()

	boolean canBeFilled(BigDecimal lastTick) {
		if (lastTick.compareTo(BigDecimal.ZERO) < 0) {
			return false;
		}
		return (isLong() && (lastTick.compareTo(limitPrice) <= 0)) || (isShort() && (lastTick.compareTo(limitPrice) >= 0));
	}

	@Override
	public String toString() {
		return (orderId + " " + action + " " + totalQuantity + " " + ticker + " " + secType + " @ $" + limitPrice);
	}
}
