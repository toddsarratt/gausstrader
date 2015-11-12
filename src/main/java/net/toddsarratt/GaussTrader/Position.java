package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

public class Position {
   private long positionId;
   private long originatingOrderId;
   private boolean open;
   private String ticker;
   private String secType;
   private Instant expiry;
   private String underlyingTicker;
   private BigDecimal strikePrice;
   private Instant instantOpened;
   private boolean longPosition;
   private int numberTransacted;
   private BigDecimal priceAtOpen;
   private BigDecimal costBasis;
   private BigDecimal claimAgainstCash;
   private BigDecimal price;
   private BigDecimal netAssetValue;
   private Instant instantClosed;
   private BigDecimal priceAtClose;
   private BigDecimal profit;
   private static Market market = GaussTrader.getMarket();
   private static final Logger LOGGER = LoggerFactory.getLogger(Position.class);

   Position() {
      LOGGER.debug("Entering Position default constructor");
      positionId = getNewId();
      instantOpened = Instant.now();
      open = true;
   }

   Position(Order orderToFill, BigDecimal priceAtOpen) {
      LOGGER.debug("Entering Position constructor Position(Order {}, price {})", orderToFill.getOrderId(), priceAtOpen);
      positionId = getNewId();
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
      instantOpened = Instant.now();
      longPosition = orderToFill.isLong();
      numberTransacted = orderToFill.getTotalQuantity();
      this.priceAtOpen = priceAtOpen;
      calculateCostBasis();
      calculateClaimAgainstCash();
      LOGGER.debug("claimAgainstCash = ${}", claimAgainstCash);
      price = priceAtOpen;
      netAssetValue = costBasis;
      LOGGER.info("New position created with positionId " + positionId + " ticker " + ticker +
              " secType " + secType + " open " + open + " instantOpened " + instantOpened);
      LOGGER.info("longPosition " + longPosition + " numberTransacted " + numberTransacted +
         " priceAtOpen " + priceAtOpen + " costBasis " + costBasis);
   }

   static Position exerciseOptionPosition(Position exercisingOptionPosition) {
      LOGGER.debug("Entering Position.exerciseOptionPosition(Position {})", exercisingOptionPosition.getPositionId());
      Position newStockPosition = new Position();
      String ticker = exercisingOptionPosition.getUnderlyingTicker();
      BigDecimal lastTick;
      newStockPosition.setTicker(ticker);
      newStockPosition.setUnderlyingTicker(ticker);
      newStockPosition.setSecType("STOCK");
      newStockPosition.setLongPosition(true);
      newStockPosition.setNumberTransacted(exercisingOptionPosition.getNumberTransacted() * 100);
      newStockPosition.setPriceAtOpen(exercisingOptionPosition.getStrikePrice());
      newStockPosition.setCostBasis(newStockPosition.getPriceAtOpen()
              .multiply(new BigDecimal(newStockPosition.getNumberTransacted())));
      newStockPosition.setPrice(lastTick = market.lastTick(ticker).getPrice());
/*      } catch (IOException ioe) {
         LOGGER.info("Could not connect to yahoo! to get lastTick() for {} when exercising option position {}", ticker, exercisingOptionPosition.getPositionId());
         LOGGER.info("lastTick and netAssetValue are incorrect for current open position {}", newStockPosition.getPositionId());
         LOGGER.debug("Caught (IOException ioe)", ioe);
         newStockPosition.setPrice(lastTick = newStockPosition.getPriceAtOpen());
      } */
      newStockPosition.setNetAssetValue(lastTick.multiply(new BigDecimal(newStockPosition.getNumberTransacted())));
      return newStockPosition;
   }

   /**
    * Returns current epoch time + least significant nano seconds to generate unique order and position ids
    *
    * @return
    */
   static long getNewId() {
      return ((System.currentTimeMillis() << 20) & 0x7FFFFFFFFFF00000l) | (System.nanoTime() & 0x00000000000FFFFFl);
   }

   public void close(BigDecimal closePrice) {
      open = false;
      instantClosed = Instant.now();
      priceAtClose = closePrice;
      profit = priceAtClose
              .multiply(new BigDecimal(numberTransacted * (isStock() ? 1 : 100) * (isLong() ? 1 : -1)))
              .subtract(costBasis);
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

   public Instant getExpiry() {
      return expiry;
   }

   void setExpiry(Instant expiry) {
      this.expiry = expiry;
   }

   public String getUnderlyingTicker() {
      return underlyingTicker;
   }

   void setUnderlyingTicker(String underlyingTicker) {
      this.underlyingTicker = underlyingTicker;
   }

   public BigDecimal getStrikePrice() {
      return strikePrice;
   }

   void setStrikePrice(BigDecimal strikePrice) {
      this.strikePrice = strikePrice;
   }

   public Instant getInstantOpened() {
      return instantOpened;
   }

   void setInstantOpened(Instant instantOpened) {
      this.instantOpened = instantOpened;
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

   public BigDecimal getPriceAtOpen() {
      return priceAtOpen;
   }

   void setPriceAtOpen(BigDecimal priceAtOpen) {
      this.priceAtOpen = priceAtOpen;
   }

   public BigDecimal getCostBasis() {
      return costBasis;
   }

   void setCostBasis(BigDecimal costBasis) {
      this.costBasis = costBasis;
   }

   void calculateCostBasis() {
      costBasis = priceAtOpen.multiply(new BigDecimal(numberTransacted * (isStock() ? 1 : 100) * (isLong() ? 1 : -1)));
   }

   public BigDecimal getLastTick() {
      try {
         if (isStock()) {
            price = market.lastTick(ticker).getPrice();
         } else {
            price = Option.lastTick(ticker);
         }
         netAssetValue = price.multiply(new BigDecimal(numberTransacted * (isStock() ? 1 : 100) * (longPosition ? 1 : -1)));
      } catch (IOException ioe) {
         LOGGER.warn("Caught IOException trying to get lastTick({}). Returning last known tick (We are no longer real-time).", ticker);
         LOGGER.debug("Caught (IOException ioe) ", ioe);
      }
      return price;
   }

   void setPrice(BigDecimal lastTick) {
      this.price = lastTick;
   }

   public BigDecimal getPrice() {
      return price;
   }

   void setNetAssetValue(BigDecimal netAssetValue) {
      this.netAssetValue = netAssetValue;
   }

   public Instant getInstantClosed() {
      if (!open) {
         return instantClosed;
      }
      return Instant.MIN;
   }

   void setInstantClosed(Instant instantClosed) {
      this.instantClosed = instantClosed;
   }

   public BigDecimal getPriceAtClose() {
      if (!open) {
         return priceAtClose;
      }
      return Constants.BIGDECIMAL_MINUS_ONE;
   }

   void setPriceAtClose(BigDecimal priceAtClose) {
      this.priceAtClose = priceAtClose;
   }

   public BigDecimal getProfit() {
       if(open) {
          profit = netAssetValue.subtract(costBasis);
       }
      return profit;
   }

   void setProfit(BigDecimal profit) {
      this.profit = profit;
   }

   public boolean isOpen() {
      return open;
   }

   public BigDecimal getClaimAgainstCash() {
      return claimAgainstCash;
   }

   void setClaimAgainstCash(BigDecimal requiredCash) {
      claimAgainstCash = requiredCash;
   }

    public boolean isExpired() {
       return isOption() && expiry.isBefore(Instant.now());
   }

   /* Position.claimAgainstCash() is a bit disingenuous. Selling an option or shorting a stock
    * could result in an infinite liability. Only calculating for selling a put which has
    * a fixed obligation.
    */
   BigDecimal calculateClaimAgainstCash() {
      if (isPut() && isShort()) {
         return (claimAgainstCash = strikePrice.multiply(new BigDecimal(numberTransacted * 100)));
      }
      return (claimAgainstCash = BigDecimal.ZERO);
   }

   public BigDecimal calculateNetAssetValue() {
      netAssetValue = price.multiply(new BigDecimal(numberTransacted))
              .multiply(isStock() ? BigDecimal.ONE : Constants.BIGDECIMAL_ONE_HUNDRED)
              .multiply(isLong() ? BigDecimal.ONE : Constants.BIGDECIMAL_MINUS_ONE);
      profit = netAssetValue.subtract(costBasis);
      return netAssetValue;
   }

   public String toString() {
      return (positionId + " | " + ticker + " | " + secType + " | " + open + " | " + instantOpened +
         " | " + longPosition + " | " + numberTransacted + " | " + priceAtOpen + " | " +
              costBasis + " | " + instantClosed + " | " + priceAtClose + " | " + profit);
   }
}
