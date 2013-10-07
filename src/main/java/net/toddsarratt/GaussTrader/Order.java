package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Class to record each order 
 * Fields :
 * orderId : generated from System time
 * open : boolean, open or closed order
 * ticker : security being traded
 * limitPrice : Limit orders only
 * Action : "BUY" or "SELL"
 * totalQuantity : probably 1 contract, could be 100 shares
 * secType : "CALL", "PUT", "STOCK"
 * tif (Time in Force) : "GTC" (Good 'Til Cancelled) vs "GFD" (Good For Day = day order)
 * epochOpened : milliseconds since epoch when order was opened
 * epochClosed : milliseconds since epoch when order was closed
 * closeReason : "FILLED", "EXPIRED", or "CANCELLED"
 * fillPrice : price at which order was filled and closed
 */

class Order {
	
    private long orderId;
    private boolean open;
    private String ticker;
    private DateTime expiry = null;
    private String underlyingTicker = null;
    private double strikePrice;
    private double limitPrice;
    private double claimAgainstCash;
    private String action;
    private int totalQuantity;
    private String secType;
    private String tif;
    private long epochOpened;
    private long epochClosed;
    private String closeReason;
    private double fillPrice;
    private static final Logger LOGGER = LoggerFactory.getLogger(Order.class);

    public Order() {}    

    public Order(Security security, double limitPrice, String action, int totalQuantity, String tif) {
	LOGGER.debug("Entering constructor Order(Security {}, double {}, String {}, int {}, String {})",
		     security, limitPrice, action, totalQuantity, tif);
	orderId = GaussTrader.getNewId();
	LOGGER.debug("Security is of type {}", security.getClass());
	ticker = security.getTicker();
	LOGGER.debug("Assigning ticker = {} from security.getTicker() = {}", ticker, security.getTicker());
	this.limitPrice = limitPrice;
	LOGGER.debug("limitPrice = ${}", limitPrice);
	this.action = action;
	this.totalQuantity = totalQuantity;
	secType = security.getSecType();
	if( (secType.equals("CALL")) || (secType.equals("PUT")) ) {
	    LOGGER.debug("Security is an option");
	    expiry = ((Option)security).getExpiry();
	    LOGGER.debug("expiry = {}", expiry.toString("MMMM dd YYYY")); 
	    underlyingTicker = ((Option)security).getUnderlyingTicker();
	    LOGGER.debug("underlyingTicker = {}", underlyingTicker);
	    strikePrice = ((Option)security).getStrike();
	    LOGGER.debug("strikePrice = ${}", strikePrice);
	}
	calculateClaimAgainstCash();
	LOGGER.debug("claimAgainstCash = ${}", claimAgainstCash);
	this.tif = tif;
	open = true;
        epochOpened = System.currentTimeMillis();
	LOGGER.info("Created order ID {} for {} to {} {} of {} @ ${} TIF : {}", orderId, underlyingTicker, action, totalQuantity, ticker, limitPrice, tif);
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
    public String getTicker() {
	return ticker;
    }
    void setTicker(String ticker) {
        this.ticker = ticker;
    }
    public double getLimitPrice() {
	return limitPrice;
    }
    void setLimitPrice(double limitPrice) {
        this.limitPrice = limitPrice;
    }
    public String getAction() {
	return action;
    }
    void setAction(String action) {
        this.action = action;
    }
    public int getTotalQuantity() {
	return totalQuantity;
    }
    void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    public String getSecType() {
	return secType;
    }
    void setSecType(String secType) {
        this.secType = secType;
    }
    public String getTif() {
	return tif;
    }
    void setTif(String tif) {
        this.tif = tif;
    }
    public boolean isOption() {
	return (secType.equals("PUT") || secType.equals("CALL"));
    }
    /* Write changes in order to database */
    public void fill(double fillPrice) {
	LOGGER.debug("Entering Order.fill(double {})", fillPrice);
	closeReason = "FILLED";
	open = false;
	this.fillPrice = fillPrice;
	epochClosed = System.currentTimeMillis();
	LOGGER.info("Order {} {} @ ${} epoch {}", orderId, closeReason, this.fillPrice, epochClosed);
    }
    public void closeExpired() {
	LOGGER.debug("Entering Order.closeExpired()");
	closeReason = "EXPIRED";
	open = false;
	fillPrice = 0.00;
        epochClosed = System.currentTimeMillis();
        LOGGER.info("Order {} {} @ ${} epoch {}", this.orderId, closeReason, fillPrice, epochClosed);
    }
    public void closeCancelled() {
	LOGGER.debug("Entering Order.closeCancelled()");
        closeReason = "CANCELLED";
        open = false;
        fillPrice = 0.00;
        epochClosed = System.currentTimeMillis();
	LOGGER.info("Order {} {} @ ${} epoch {}", this.orderId, closeReason, fillPrice, epochClosed);
    }
    void setEpochOpened(long epochOpened) {
        this.epochOpened = epochOpened;
    }
    long getEpochOpened() {
	return epochOpened;
    }
    void setEpochClosed(long epochClosed) {
        this.epochClosed = epochClosed;
    }
    long getEpochClosed() {
	return epochClosed;
    }
    void setCloseReason(String closeReason) {
	this.closeReason = closeReason;
    }
    public String getCloseReason() {
	return closeReason;
    }
    void setFillPrice(double fillPrice) {
        this.fillPrice = fillPrice;
    }
    double getFillPrice() {
	return fillPrice;
    }
    void setExpiry(DateTime expiry) {
	this.expiry = expiry;
    }
    DateTime getExpiry() {
	return expiry;
    }
    void setUnderlyingTicker(String underlyingTicker) {
	this.underlyingTicker = underlyingTicker;
    }
    String getUnderlyingTicker() {
	return underlyingTicker;
    }
    void setStrikePrice(double strikePrice) {
	this.strikePrice = strikePrice;
    }
    double getStrikePrice() {
	return strikePrice;
    }
    double getClaimAgainstCash() {
	return claimAgainstCash;
    }
    void setClaimAgainstCash(double requiredCash) {
	claimAgainstCash = requiredCash;
    }
    void calculateClaimAgainstCash() {
	LOGGER.debug("Entering Order.calculateClaimAgainstCash()");
        double costBasis = limitPrice * totalQuantity * (secType.equals("STOCK") ? 1.0 : 100.0) * (action.equals("BUY") ? 1.0 : -1.0);
        LOGGER.debug("costBasis = ${}", costBasis);
	claimAgainstCash = 0.00;
	if(action.equals("BUY")) {
	    claimAgainstCash = costBasis;
	} else if(secType.equals("PUT")) {
	    claimAgainstCash = strikePrice * 100.0 + costBasis;
	}
    }
    @Override
    public String toString() {
	return (orderId + " : Qty " + totalQuantity + " " + ticker + " @ $" + limitPrice);
    }
    public static void main(String[] args) {
	// TODO Auto-generated method stub
    }
}
