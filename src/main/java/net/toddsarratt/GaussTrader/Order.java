package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Class to record each order 
 * Fields :
 * orderId : equal to System.nanoTime() should be rather unique
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
	orderId = GaussTrader.getNewId();
	LOGGER.info("Security is of type {}", security.getClass());
	ticker = security.getTicker();
	LOGGER.info("Assigning ticker = {} from security.getTicker() = {}", ticker, security.getTicker());
	this.limitPrice = limitPrice;
	LOGGER.info("limitPrice = {}", limitPrice);
	this.action = action;
	this.totalQuantity = totalQuantity;
	this.secType = security.getSecType();
	this.tif = tif;
	epochOpened = System.currentTimeMillis();
	open = true;
	LOGGER.info("Created order ID {} to {} {} of {} time in force : {}", orderId, action, totalQuantity, ticker, tif );
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
    public void fill(double fillPrice) {
	closeReason = "FILLED";
	open = false;
	this.fillPrice = fillPrice;
	epochClosed = System.currentTimeMillis();
    }
    public void closeExpired() {
	closeReason = "EXPIRED";
	open = false;
	fillPrice = 0.00;
        epochClosed = System.currentTimeMillis();
    }
    public void closeCancelled() {
        closeReason = "CANCELLED";
        open = false;
        fillPrice = 0.00;
        epochClosed = System.currentTimeMillis();
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
    public static void main(String[] args) {
	// TODO Auto-generated method stub
    }
}
