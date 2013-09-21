package net.toddsarratt.GaussTrader;

import java.io.IOException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Position {
    private long positionId;
    private boolean open = true;
    private String ticker;
    private String secType;
    private DateTime expiry = null;
    private String underlyingTicker = null;
    private double strikePrice = 0.00;
    private long epochOpened;
    private Boolean longPosition;
    private int numberTransacted;
    private double priceAtOpen;
    private double costBasis;
    private double lastTick = 0.00;
    private double netAssetValue = 0.00;
    private long epochClosed;
    private double priceAtClose;
    private double profit;

    private static final Logger LOGGER = LoggerFactory.getLogger(Position.class);

    Position() {
	positionId = GaussTrader.getNewId();
	epochOpened = System.currentTimeMillis();
	open = true;
    }

    Position(Order orderToFill, double priceAtOpen) {
        positionId = GaussTrader.getNewId();
        open = true;
        ticker = orderToFill.getTicker();
        secType = orderToFill.getSecType();
	if(secType.equals("CALL") || secType.equals("PUT")) {
	    expiry = orderToFill.getExpiry();
	    underlyingTicker = orderToFill.getUnderlyingTicker();
	    strikePrice = orderToFill.getStrikePrice();
	}
        epochOpened = System.currentTimeMillis();
        longPosition = orderToFill.getAction().equals("BUY");
        numberTransacted = orderToFill.getTotalQuantity();
        this.priceAtOpen = priceAtOpen;
        costBasis = priceAtOpen * numberTransacted * (secType.equals("STOCK") ? 1 : 100) * (longPosition ? 1 : -1);
	lastTick = priceAtOpen;
	netAssetValue = costBasis;
        LOGGER.info("New position created with positionId " + positionId + " ticker " + ticker + 
			   " secType " + secType + " open " + open + " epochOpened " + epochOpened);
        LOGGER.info("longPosition " + longPosition + " numberTransacted " + numberTransacted + 
			   " priceAtOpen " + priceAtOpen + " costBasis " + costBasis);
    }

    static Position exerciseOptionPosition(Position exercisingOptionPosition) {
	LOGGER.debug("Entering Position.exerciseOptionPosition(Position exercisingOptionPosition)");
	Position newStockPosition = new Position();
	String ticker = exercisingOptionPosition.getTicker();
	newStockPosition.setTicker(ticker);
	newStockPosition.setSecType("STOCK");
	newStockPosition.setLongPosition(true);
	newStockPosition.setNumberTransacted(exercisingOptionPosition.getNumberTransacted() * 100);
	newStockPosition.setPriceAtOpen(exercisingOptionPosition.getStrikePrice());
	newStockPosition.setCostBasis(newStockPosition.getPriceAtOpen() * newStockPosition.getNumberTransacted());
	try {
	    newStockPosition.setLastTick(Stock.lastTick(ticker));
	} catch (IOException ioe) {
	    LOGGER.info("Could not connect to yahoo! to get lastTick() for {} when exercising option position {}", ticker, exercisingOptionPosition.getPositionId());
	    LOGGER.info("lastTick and netAssetValue are incorrect for current open position {}", newStockPosition.getPositionId());
	    LOGGER.debug("Caught (IOException ioe)", ioe);
	    newStockPosition.setLastTick(newStockPosition.getPriceAtOpen());
	} finally {
	    newStockPosition.setNetAssetValue(newStockPosition.getNumberTransacted() * newStockPosition.getLastTick());
	    return newStockPosition;
	}
    }

    public void close(double closePrice) {
	open = false;
	epochClosed = System.currentTimeMillis();
	priceAtClose = closePrice;
	profit = (priceAtClose * numberTransacted * (secType.equals("STOCK") ? 1 : 100) * (longPosition ? 1 : -1)) - costBasis;
    }
    public long getPositionId() {
        return positionId;
    }
    void setPositionId(int positionId ) {
        this.positionId = positionId;
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
    public String getSecType() {
        return secType;
    }
    void setSecType(String secType) {
        this.secType = secType;
    }
    public DateTime getExpiry() {
	return expiry;
    }
    void setExpiry(DateTime expiry) {
	this.expiry = expiry;
    }
    public String getUnderlyingTicker() {
	return underlyingTicker;
    }
    void setUnderlyingTicker(String underlyingTicker) {
	this.underlyingTicker = underlyingTicker;
    }
    public double getStrikePrice() {
	return strikePrice;
    }
    void setStrikePrice(double strikePrice) {
	this.strikePrice = strikePrice;
    }
    public long getEpochOpened() {
        return epochOpened;
    }
    void setEpochOpened(long epochOpened) {
        this.epochOpened = epochOpened;
    }
    public boolean isLong() {
        return longPosition;
    }
    void setLongPosition(boolean longPosition) {
        this.longPosition = longPosition;
    }
    public boolean isShort() {
        return !longPosition;
    }
    public int getNumberTransacted() {
	return numberTransacted;
    }	
    void setNumberTransacted(int numberTransacted) {
        this.numberTransacted = numberTransacted;
    }
    public double getPriceAtOpen() {
	return priceAtOpen;
    }
    void setPriceAtOpen(double priceAtOpen) {
        this.priceAtOpen = priceAtOpen;
    }
    public double getCostBasis() {
	return costBasis;
    }
    void setCostBasis(double costBasis) {
        this.costBasis = costBasis;
    }
    public double getLastTick() {
        try {
            if(secType.equals("STOCK")) {
                lastTick = Stock.lastTick(ticker);
            } else {
                lastTick = Option.lastTick(ticker);
            }
            netAssetValue = lastTick * numberTransacted * (secType.equals("STOCK") ? 1 : 100) * (longPosition ? 1 : -1);
        } catch(IOException ioe) {
            LOGGER.warn("Caught IOException trying to get lastTick({}). Returning last known tick (We are no longer real-time).", ticker);
            LOGGER.debug("Caught (IOException ioe) ", ioe);
        } finally {
            return lastTick;
        }
    }
    void setLastTick(double lastTick) {
	this.lastTick = lastTick;
    }
    void setNetAssetValue(double netAssetValue) {
	this.netAssetValue = netAssetValue;
    }
    public long getEpochClosed() {
	if(!open) {
	    return epochClosed;
	}
	return -1;
    }
    void setEpochClosed(long epochClosed) {
        this.epochClosed = epochClosed;
    }
    public double getPriceAtClose() {
        if(!open) {
            return priceAtClose;
        }
        return -1;
    }
    void setPriceAtClose(double priceAtClose) {
        this.priceAtClose = priceAtClose;
    }
    public double getProfit() {
        if(!open) {
            return profit;
        }
        return -1;
    }
    void setProfit(double profit) {
        this.profit = profit;
    }
    public boolean isOpen() {
	return open;
    }
    public double calculateNetAssetValue() {
	try {
	    if(secType.equals("STOCK")) {
		lastTick = Stock.lastTick(ticker);
	    } else {
		lastTick = Option.lastTick(ticker);
	    }
	    netAssetValue = lastTick * numberTransacted * (secType.equals("STOCK") ? 1 : 100) * (longPosition ? 1 : -1);
	} catch(IOException ioe) {
	    LOGGER.warn("Caught IOException trying to get lastTick({}). Returning last known netAssetValue (We are no longer real-time).", ticker);
	    LOGGER.debug("Caught (IOException ioe)", ioe);
	} finally {
	    return netAssetValue;
	}
    }
    public String toString() {		
	return 	(positionId + " | " + ticker + " | " + secType + " | " + open + " | " + epochOpened + 
		 " | " + longPosition + " | " + numberTransacted + " | " + priceAtOpen + " | " + 
		 costBasis + " | " + epochClosed + " | " + priceAtClose + " | " + profit);
    }
}