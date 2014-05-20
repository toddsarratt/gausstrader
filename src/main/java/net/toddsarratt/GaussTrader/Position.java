package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Position {
   private long positionId = System.currentTimeMillis();
   private long originatingOrderId = System.currentTimeMillis();
   private boolean open = true;
   private String ticker = "APPL";
   private String secType = "PUT";
   private DateTime expiry = new DateTime(DateTimeZone.forID("America/New_York"));
   private String underlyingTicker = "AAPL";
   private double strikePrice = 0.00;
   private long epochOpened = System.currentTimeMillis();
   private Boolean longPosition = false;
   private int numberTransacted = 1;
   private double priceAtOpen = 0.00;
   private double costBasis = 0.00;
   private double claimAgainstCash = 0.00;
   private double lastTick = 0.00;
   private double netAssetValue = 0.00;
   private long epochClosed;
   private double priceAtClose;
   private double profit;

   private static final Logger LOGGER = LoggerFactory.getLogger(Position.class);

   Position() {
      LOGGER.debug("Entering Position default constructor");
      positionId = GaussTrader.getNewId();
      epochOpened = System.currentTimeMillis();
      open = true;
   }

   Position(Order orderToFill, double priceAtOpen) {
      LOGGER.debug("Entering Position constructor Position(Order {}, price {})", orderToFill.getOrderId(), priceAtOpen);
      positionId = GaussTrader.getNewId();
      originatingOrderId = orderToFill.getOrderId();
      open = true;
      ticker = orderToFill.getTicker();
      secType = orderToFill.getSecType();
      if (isOption()) {
         expiry = orderToFill.getExpiry();
         underlyingTicker = orderToFill.getUnderlyingTicker();
         strikePrice = orderToFill.getStrikePrice();
      } else {
         underlyingTicker = ticker;
         expiry = null;
      }
      epochOpened = System.currentTimeMillis();
      longPosition = orderToFill.isLong();
      numberTransacted = orderToFill.getTotalQuantity();
      this.priceAtOpen = priceAtOpen;
      calculateCostBasis();
      calculateClaimAgainstCash();
      LOGGER.debug("claimAgainstCash = ${}", claimAgainstCash);
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
      String ticker = exercisingOptionPosition.getUnderlyingTicker();
      double lastTick = 0.00;
      newStockPosition.setTicker(ticker);
      newStockPosition.setUnderlyingTicker(ticker);
      newStockPosition.setSecType("STOCK");
      newStockPosition.setLongPosition(true);
      newStockPosition.setNumberTransacted(exercisingOptionPosition.getNumberTransacted() * 100);
      newStockPosition.setPriceAtOpen(exercisingOptionPosition.getStrikePrice());
      newStockPosition.setCostBasis(newStockPosition.getPriceAtOpen() * newStockPosition.getNumberTransacted());
      try {
         newStockPosition.setLastTick(lastTick = Stock.lastTick(ticker));
      } catch (IOException ioe) {
         LOGGER.info("Could not connect to yahoo! to get lastTick() for {} when exercising option position {}", ticker, exercisingOptionPosition.getPositionId());
         LOGGER.info("lastTick and netAssetValue are incorrect for current open position {}", newStockPosition.getPositionId());
         LOGGER.debug("Caught (IOException ioe)", ioe);
         newStockPosition.setLastTick(lastTick = newStockPosition.getPriceAtOpen());
      } finally {
         newStockPosition.setNetAssetValue(newStockPosition.getNumberTransacted() * lastTick);
      }
      return newStockPosition;
   }

   public void close(double closePrice) {
      open = false;
      epochClosed = System.currentTimeMillis();
      priceAtClose = closePrice;
      profit = (priceAtClose * numberTransacted * (isStock() ? 1 : 100) * (isLong() ? 1 : -1)) - costBasis;
   }

   public long getPositionId() {
      return positionId;
   }

   void setPositionId(long positionId) {
      this.positionId = positionId;
   }

   public long getOriginatingOrderId() {
      return originatingOrderId;
   }

   void setOriginatingOrderId(long originatingOrderId) {
      this.originatingOrderId = originatingOrderId;
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

   public boolean isCall() {
      return secType.equals("CALL");
   }

   public boolean isPut() {
      return secType.equals("PUT");
   }

   public boolean isStock() {
      return secType.equals("STOCK");
   }

   public boolean isOption() {
      return (isCall() || isPut());
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

   void calculateCostBasis() {
      costBasis = priceAtOpen * numberTransacted * (isStock() ? 1 : 100) * (isLong() ? 1 : -1);
   }

   public double getLastTick() {
      try {
         if (isStock()) {
            lastTick = Stock.lastTick(ticker);
         } else {
            lastTick = Option.lastTick(ticker);
         }
         netAssetValue = lastTick * numberTransacted * (isStock() ? 1 : 100) * (longPosition ? 1 : -1);
      } catch (IOException ioe) {
         LOGGER.warn("Caught IOException trying to get lastTick({}). Returning last known tick (We are no longer real-time).", ticker);
         LOGGER.debug("Caught (IOException ioe) ", ioe);
      }
      return lastTick;
   }

   void setLastTick(double lastTick) {
      this.lastTick = lastTick;
   }

   void setNetAssetValue(double netAssetValue) {
      this.netAssetValue = netAssetValue;
   }

   public long getEpochClosed() {
      if (!open) {
         return epochClosed;
      }
      return -1;
   }

   void setEpochClosed(long epochClosed) {
      this.epochClosed = epochClosed;
   }

   public double getPriceAtClose() {
      if (!open) {
         return priceAtClose;
      }
      return -1;
   }

   void setPriceAtClose(double priceAtClose) {
      this.priceAtClose = priceAtClose;
   }

   public double getProfit() {
       if(open) {
        profit = netAssetValue - costBasis;
       }
      return profit;
   }

   void setProfit(double profit) {
      this.profit = profit;
   }

   public boolean isOpen() {
      return open;
   }

   public double getClaimAgainstCash() {
      return claimAgainstCash;
   }

   void setClaimAgainstCash(double requiredCash) {
      claimAgainstCash = requiredCash;
   }

    public boolean isExpired() {
       if(isOption()) {
          return expiry.isBeforeNow();
       }
       return false;
   }

   /* Position.claimAgainstCash() is a bit disingenuous. Selling an option or shorting a stock
    * could result in an infinite liability. Only calculating for selling a put which has
    * a fixed obligation.
    */
   double calculateClaimAgainstCash() {
      if (isPut() && isShort()) {
         return (claimAgainstCash = strikePrice * numberTransacted * 100);
      }
      return (claimAgainstCash = 0.00);
   }

   public double calculateNetAssetValue() {
      netAssetValue = lastTick * numberTransacted * (isStock() ? 1 : 100) * (isLong() ? 1 : -1);
      profit = netAssetValue - costBasis;
      return netAssetValue;
   }

   public String toString() {
      return (positionId + " | " + ticker + " | " + secType + " | " + open + " | " + epochOpened +
         " | " + longPosition + " | " + numberTransacted + " | " + priceAtOpen + " | " +
         costBasis + " | " + epochClosed + " | " + priceAtClose + " | " + profit);
   }
}
