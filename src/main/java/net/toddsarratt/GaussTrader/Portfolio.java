package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Portfolio {
   private String name;
   private BigDecimal netAssetValue;
   private BigDecimal freeCash;
   private BigDecimal reservedCash;
   private BigDecimal totalCash;
   private Set<Position> positions;
   private Set<Order> orders;
   private static final DataStore dataStore = GaussTrader.getDataStore();
   private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);

   /**
    * Use static factory method .of() to create objects of the Portfolio class.
    */
   private Portfolio(String portfolioName,
                     BigDecimal netAssetValue,
                     BigDecimal freeCash,
                     BigDecimal reservedCash,
                     BigDecimal totalCash,
                     Set<Position> positions,
                     Set<Order> orders) {
      LOGGER.debug("Entering private constructor");
      this.name = portfolioName;
      this.netAssetValue = netAssetValue;
      this.freeCash = freeCash;
      this.reservedCash = reservedCash;
      this.positions = positions;
      this.orders = orders;
   }

   public Portfolio of(String portfolioName) {
      Portfolio newPortfolio;
      LOGGER.debug("Entering Portfolio constructor Portfolio(String {})", portfolioName);
      if (dataStore.portfolioInStore(portfolioName)) {
         LOGGER.info("Starting with portfolio named \"{}\"", portfolioName);
         PortfolioSummary summary = dataStore.getPortfolioSummary(portfolioName);
         Set<Position> positions = dataStore.getPortfolioPositions();
         Set<Order> orders = dataStore.getPortfolioOrders();
      } else {
         LOGGER.info("Could not find portfolio \"{}\"", portfolioName);
         newPortfolio = new Portfolio(portfolioName,
                 BigDecimal.valueOf(Constants.STARTING_CASH),
                 BigDecimal.valueOf(Constants.STARTING_CASH),
                 BigDecimal.ZERO,
                 BigDecimal.valueOf(Constants.STARTING_CASH),
                 Collections.EMPTY_SET,
                 Collections.EMPTY_SET);
         LOGGER.info("Created portfolio \"{}\" with ${} free cash", portfolioName, newPortfolio.getFreeCash());
         }
      newPortfolio.calculateNetAssetValue();
      LOGGER.debug("Starting portfolio \"{}\" with netAssetValue {} reservedCash {} totalCash {}",
              portfolioName, newPortfolio.getNetAssetValue(), newPortfolio.getReservedCash(), newPortfolio.getTotalCash());
      return newPortfolio;
   }

   public Portfolio of(String portfolioName, double startingCash) {
      LOGGER.debug("Entering of(String {}, double {})", portfolioName, startingCash);
      if (dataStore.portfolioInStore(portfolioName)) {
         LOGGER.error("Portfolio {} already exists", portfolioName);
         throw new IllegalArgumentException("Portfolio already exists.");
      }

   }


   void getPortfolioFromStore() throws SQLException {
      LOGGER.debug("Entering Portfolio.getDbPortfolio()");

   }

   void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
   public double calculateNetAssetValue() {
      double openPositionNavs = 0.00;
      calculateTotalCash();
      for (Position positionToUpdate : portfolioPositions) {
         if (positionToUpdate.isOpen()) {
            openPositionNavs += positionToUpdate.calculateNetAssetValue();
         }
      }
      netAssetValue = totalCash + openPositionNavs;
      return netAssetValue;
   }

   private void setFreeCash(double freeCash) {
      this.freeCash = freeCash;
   }

   public double getFreeCash() {
      return freeCash;
   }

   private void setReservedCash(double reservedCash) {
      this.reservedCash = reservedCash;
   }

   public double getReservedCash() {
      return reservedCash;
   }

   public double calculateTotalCash() {
      totalCash = freeCash + reservedCash;
      return totalCash;
   }

   int countUncoveredLongStockPositions(Stock stock) {
      return (numberOfOpenStockLongs(stock) - numberOfOpenCallShorts(stock));
   }

   public int numberOfOpenStockLongs(Security security) {
      int openLongCount = 0;

      for (Position portfolioPosition : portfolioPositions) {
         if ((security.getTicker().equals(portfolioPosition.getTicker())) &&
            portfolioPosition.isOpen() &&
            portfolioPosition.isLong() &&
            (portfolioPosition.isStock())) {
            openLongCount += portfolioPosition.getNumberTransacted();
         }
      }
      openLongCount /= 100;
      LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenStockLongs(Security {})", openLongCount, security.getTicker());
      return openLongCount;
   }

   public int numberOfOpenStockShorts(Security security) {
      int openShortCount = 0;

      for (Position portfolioPosition : portfolioPositions) {
         if ((security.getTicker().equals(portfolioPosition.getTicker())) && portfolioPosition.isOpen() &&
            portfolioPosition.isShort() && portfolioPosition.isStock()) {
            openShortCount += portfolioPosition.getNumberTransacted();
         }
      }
      openShortCount /= 100;
      LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenStockShorts(Security {})", openShortCount, security.getTicker());
      return openShortCount;
   }

   public int numberOfOpenCallLongs(Security security) {
      int openLongCount = 0;

      for (Position portfolioPosition : portfolioPositions) {
         if ((security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) && portfolioPosition.isOpen() &&
            portfolioPosition.isLong() && portfolioPosition.isCall()) {
            openLongCount++;
         }
      }
      LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenCallLongs(Security {})", openLongCount, security.getTicker());
      return openLongCount;
   }

   public int numberOfOpenCallShorts(Security security) {
      int openShortCount = 0;

      for (Position portfolioPosition : portfolioPositions) {
         if ((security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) &&
            portfolioPosition.isOpen() &&
            portfolioPosition.isShort() &&
            portfolioPosition.isCall()) {
            openShortCount += portfolioPosition.getNumberTransacted();
         }
      }
      for (Order portfolioOrder : portfolioOrders) {
         if ((security.getTicker().equals(portfolioOrder.getUnderlyingTicker())) &&
            portfolioOrder.isOpen() &&
            portfolioOrder.isShort() &&
            portfolioOrder.isCall()) {
            openShortCount += portfolioOrder.getTotalQuantity();
         }
      }
      LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenCallShorts(Security {})", openShortCount, security.getTicker());
      return openShortCount;
   }

   public int numberOfOpenPutLongs(Security security) {
      int openLongCount = 0;

      for (Position portfolioPosition : portfolioPositions) {
         if ((security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) &&
            portfolioPosition.isOpen() &&
            portfolioPosition.isLong() &&
            portfolioPosition.isPut()) {
            openLongCount += portfolioPosition.getNumberTransacted();
         }
      }
      for (Order portfolioOrder : portfolioOrders) {
         if ((security.getTicker().equals(portfolioOrder.getUnderlyingTicker())) &&
            portfolioOrder.isOpen() &&
            portfolioOrder.isLong() &&
            portfolioOrder.isPut()) {
            openLongCount += portfolioOrder.getTotalQuantity();
         }
      }
      LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenPutLongs(Security {})", openLongCount, security.getTicker());
      return openLongCount;
   }

   public int numberOfOpenPutShorts(Security security) {
      String securityTicker = security.getTicker();
      int openShortCount = 0;

      for (Position portfolioPosition : portfolioPositions) {
         if ((securityTicker.equals(portfolioPosition.getUnderlyingTicker())) &&
            portfolioPosition.isOpen() &&
            portfolioPosition.isShort() &&
            portfolioPosition.isPut()) {
            openShortCount += portfolioPosition.getNumberTransacted();
         }
      }
      for (Order portfolioOrder : portfolioOrders) {
         if ((securityTicker.equals(portfolioOrder.getUnderlyingTicker())) &&
            portfolioOrder.isOpen() &&
            portfolioOrder.isShort() &&
            portfolioOrder.isPut()) {
            openShortCount += portfolioOrder.getTotalQuantity();
         }
      }
      LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenPutShorts(Security {})", openShortCount, security.getTicker());
      return openShortCount;
   }

   public void addNewOrder(Order orderToAdd) throws InsufficientFundsException {
      LOGGER.debug("Entering Portfolio.addNewOrder(Order {})", orderToAdd);
      double orderRequiredCash = orderToAdd.getClaimAgainstCash();
      LOGGER.debug("orderRequiredCash = orderToAdd.getClaimAgainstCash() = ${}", orderRequiredCash);
      if (freeCash < orderRequiredCash) {
         LOGGER.debug("freeCash {} < orderRequiredCash {}", freeCash, orderRequiredCash);
         throw new InsufficientFundsException(orderToAdd.getTicker(), orderRequiredCash, freeCash);
      }
      LOGGER.debug("reservedCash ${} += orderRequiredCash ${} == ${}", reservedCash, orderRequiredCash, (reservedCash + orderRequiredCash));
      reservedCash += orderRequiredCash;
      LOGGER.debug("freeCash ${} -= orderRequiredCash ${} == ${}", freeCash, orderRequiredCash, (freeCash - orderRequiredCash));
      freeCash -= orderRequiredCash;
      LOGGER.info("orderRequiredCash == ${}, freeCash == ${}, reservedCash == ${}", orderRequiredCash, freeCash, reservedCash);
      portfolioOrders.add(orderToAdd);
      LOGGER.info("Added order id {} to portfolio {}", orderToAdd.getOrderId(), name);
      try {
         dataStore.addOrder(orderToAdd);
      } catch (SQLException sqle) {
         LOGGER.warn("Unable to add order {} to DB", orderToAdd.getOrderId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   public void addNewPosition(Position position) {
      LOGGER.debug("Entering Portfolio.addNewPosition(Position {})", position.getPositionId());
      portfolioPositions.add(position);
      LOGGER.debug("freeCash ${} -= position.getCostBasis() ${} == ${}", freeCash, position.getCostBasis(), (freeCash - position.getCostBasis()));
      freeCash -= position.getCostBasis();
      LOGGER.debug("freeCash ${} -= position.getClaimAgainstCash() ${} == ${}", freeCash, position.getClaimAgainstCash(), (freeCash - position.getClaimAgainstCash()));
      freeCash -= position.getClaimAgainstCash();
      LOGGER.debug("reservedCash ${} += position.getClaimAgainstCash() ${} == ${}", reservedCash, position.getClaimAgainstCash(), (reservedCash + position.getClaimAgainstCash()));
      reservedCash += position.getClaimAgainstCash();
      try {
         insertDbPosition(position);
      } catch (SQLException sqle) {
         LOGGER.warn("Unable to add position {} to DB", position.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
      /** TODO : Move try catch to called method which should write to a file if dbwrite fails */
   }

   public List<Order> getListOfOpenOrders() {
      LOGGER.debug("Entering Portfolio.getListOfOpenOrders()");
      List<Order> openOrderList = new ArrayList<>();
      for (Order portfolioOrder : portfolioOrders) {
         if (portfolioOrder.isOpen()) {
            openOrderList.add(portfolioOrder);
         }
      }
      LOGGER.debug("Returning {}", Arrays.toString(openOrderList.toArray()));
      return openOrderList;
   }

   @SuppressWarnings("WeakerAccess")
   public List<Position> getListOfOpenPositions() {
      LOGGER.debug("Entering Portfolio.getListOfOpenPositions()");
      List<Position> openPositionList = new ArrayList<>();
      for (Position portfolioPosition : portfolioPositions) {
         if (portfolioPosition.isOpen()) {
            openPositionList.add(portfolioPosition);
         }
      }
      LOGGER.debug("Returning {}", Arrays.toString(openPositionList.toArray()));
      return openPositionList;
   }

   public List<Position> getListOfOpenOptionPositions() {
      LOGGER.debug("Entering Portfolio.getListOfOpenOptionPositions()");
      List<Position> openOptionPositionList = new ArrayList<>();
      for (Position portfolioPosition : portfolioPositions) {
         if (portfolioPosition.isOpen() && portfolioPosition.isOption()) {
            openOptionPositionList.add(portfolioPosition);
         }
      }
      LOGGER.debug("Returning {}", openOptionPositionList.toString());
      return openOptionPositionList;
   }

   public List<Position> getListOfOpenStockPositions() {
      LOGGER.debug("Entering Portfolio.getListOfOpenStockPositions()");
      List<Position> openStockPositionList = new ArrayList<>();
      for (Position portfolioPosition : portfolioPositions) {
         if (portfolioPosition.isOpen() && portfolioPosition.isStock()) {
            openStockPositionList.add(portfolioPosition);
         }
      }
      LOGGER.debug("Returning {}", openStockPositionList.toString());
      return openStockPositionList;
   }

   public void fillOrder(Order orderToFill, double fillPrice) {
      LOGGER.debug("Entering Portfolio.fillOrder(Order {}, double {})", orderToFill.getOrderId(), fillPrice);
      Position positionTakenByOrder = new Position(orderToFill, fillPrice);
      portfolioPositions.add(positionTakenByOrder);
   /* Unreserve cash to fill order */
      LOGGER.debug("freeCash ${} += orderToFill.getClaimAgainstCash() ${} == ${}", freeCash, orderToFill.getClaimAgainstCash(), (freeCash + orderToFill.getClaimAgainstCash()));
      freeCash += orderToFill.getClaimAgainstCash();
      LOGGER.debug("reservedCash ${} -= orderToFill.getClaimAgainstCash() ${} == ${}", reservedCash, orderToFill.getClaimAgainstCash(), (reservedCash - orderToFill.getClaimAgainstCash()));
      reservedCash -= orderToFill.getClaimAgainstCash();
	/* Reserve cash if position creates liability (selling an option or shorting a stock) */
      LOGGER.debug("freeCash ${} -= positionTakenByOrder.getClaimAgainstCash() ${} == ${}", freeCash, positionTakenByOrder.getClaimAgainstCash(), (freeCash - positionTakenByOrder.getClaimAgainstCash()));
      freeCash -= positionTakenByOrder.getClaimAgainstCash();
      LOGGER.debug("reservedCash ${} -= positionTakenByOrder.getClaimAgainstCash() ${} == ${}", reservedCash, positionTakenByOrder.getClaimAgainstCash(), (reservedCash - positionTakenByOrder.getClaimAgainstCash()));
      reservedCash += positionTakenByOrder.getClaimAgainstCash();
	/* Adjust free cash based on position cost basis */
      LOGGER.debug("freeCash ${} -= positionTakenByOrder.getCostBasis() ${} == ${}", freeCash, positionTakenByOrder.getCostBasis(), (freeCash - positionTakenByOrder.getCostBasis()));
      freeCash -= positionTakenByOrder.getCostBasis();
      calculateTotalCash();
      orderToFill.fill(fillPrice);
      try {
         insertDbPosition(positionTakenByOrder);
         closeDbOrder(orderToFill);
      } catch (SQLException sqle) {
         LOGGER.warn("Error writing to database filling order {} with position {}", orderToFill.getOrderId(), positionTakenByOrder.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   private void reconcileExpiredOptionPosition(Position expiredOptionPosition) {
      MutableDateTime expiryFriday = new MutableDateTime(expiredOptionPosition.getExpiry());
      expiryFriday.addDays(-1);
      expiryFriday.setMillisOfDay((16 * 60 + 20) * 60 * 1000);
      for(int attempts = 1; attempts <= GaussTrader.YAHOO_RETRIES; attempts++) {
         try {
            double expirationPrice = YahooFinance.getHistoricalClosingPrice(expiredOptionPosition.getUnderlyingTicker(), expiryFriday);
            if (expiredOptionPosition.isPut() &&
               (expirationPrice <= expiredOptionPosition.getStrikePrice())) {
               exerciseOption(expiredOptionPosition);
            } else if (expiredOptionPosition.isCall() &&
               (expirationPrice >= expiredOptionPosition.getStrikePrice())) {
               exerciseOption(expiredOptionPosition);
            } else {
               expireOptionPosition(expiredOptionPosition);
            }
            return;
         } catch(FileNotFoundException fnfe) {
            LOGGER.warn("File not found exception from Yahoo! indicating invalid ticker. Returning from method.");
            LOGGER.debug("Caught exception", fnfe);
            return;
         } catch(IOException ioe) {
            LOGGER.warn("Attempt {} to get historical price for expired option position {} failed", attempts, expiredOptionPosition.getPositionId());
            LOGGER.debug("Caught exception", ioe);
         }
      }
   }

   void exerciseOption(Position optionPositionToExercise) {
	/* If short put buy the stock at the strike price
	 * if short call find a position in the stock to sell at strike price or buy the stock and then deliver
	 * If long put find position to put, or take the cash
	 * If long call buy stock at strike price, or take the cash
	 */
      LOGGER.debug("Entering Portfolio.exerciseOption(Position {})", optionPositionToExercise.getPositionId());
      if (optionPositionToExercise.isShort()) {
         if (optionPositionToExercise.isPut()) {
            exerciseShortPut(optionPositionToExercise);
         } else {
            exerciseShortCall(optionPositionToExercise);
         }
      } else {
         if (optionPositionToExercise.isPut()) {
            exerciseLongPut(optionPositionToExercise);
         } else {
            exerciseLongCall(optionPositionToExercise);
         }
      }
      optionPositionToExercise.close(0.00);
      try {
         closeDbPosition(optionPositionToExercise);
      } catch (SQLException sqle) {
         LOGGER.warn("Error writing to database position {}", optionPositionToExercise.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
       LOGGER.debug("reservedCash ${} -= optionPositionToExercise.getClaimAgainstCash() ${} == ${}", reservedCash, optionPositionToExercise.getClaimAgainstCash(), (reservedCash - optionPositionToExercise.getClaimAgainstCash()));
       reservedCash -= optionPositionToExercise.getClaimAgainstCash();
   }

   private void exerciseShortPut(Position optionPositionToExercise) {
      LOGGER.debug("Entering Portfolio.exerciseShortPut(Position {})", optionPositionToExercise.getPositionId());
      Position optionToStockPosition = Position.exerciseOptionPosition(optionPositionToExercise);
      portfolioPositions.add(optionToStockPosition);
      try {
         insertDbPosition(optionToStockPosition);
      } catch (SQLException sqle) {
         LOGGER.warn("Error writing to database position {}", optionToStockPosition.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   private void exerciseShortCall(Position optionPositionToExercise) {
      LOGGER.debug("Entering Portfolio.exerciseShortCall(Position {})", optionPositionToExercise.getPositionId());
      int contractsToHonor = optionPositionToExercise.getNumberTransacted();
      while(contractsToHonor > 0) {
         Position calledAwayStockPosition = findStockPositionToDeliver(optionPositionToExercise.getUnderlyingTicker());
         if (calledAwayStockPosition != null) {
            while ((calledAwayStockPosition.getNumberTransacted() >= 100) && (contractsToHonor > 0)) {
               /* Exercise 100 shares / 1 contract per loop */
               calledAwayStockPosition.setNumberTransacted(calledAwayStockPosition.getNumberTransacted() - 100);
               contractsToHonor--;
               LOGGER.debug("freeCash {} += optionPositionToExercise.getStrikePrice() {} * 100", freeCash, optionPositionToExercise.getStrikePrice());
               freeCash += optionPositionToExercise.getStrikePrice() * 100;
               LOGGER.debug("freeCash == {}", freeCash);
            }
            if(calledAwayStockPosition.getNumberTransacted() == 0) {
               calledAwayStockPosition.close(optionPositionToExercise.getStrikePrice());
            }
         } else {
		/* Buy the stock at market price and deliver it */
            optionPositionToExercise.setNumberTransacted(contractsToHonor);
            Position buyStockToDeliverPosition = Position.exerciseOptionPosition(optionPositionToExercise);
            double positionLastTick = buyStockToDeliverPosition.getLastTick();
            LOGGER.debug("freeCash ${} -= buyStockToDeliverPosition.getLastTick() ${} * buyStockToDeliverPosition.getNumberTransacted() ${}",
               freeCash, positionLastTick, buyStockToDeliverPosition.getNumberTransacted());
            freeCash -= positionLastTick * buyStockToDeliverPosition.getNumberTransacted();
            LOGGER.debug("freeCash == ${}", freeCash);
            buyStockToDeliverPosition.close(optionPositionToExercise.getStrikePrice());
            contractsToHonor--;
            LOGGER.debug("freeCash ${} += optionPositionToExercise.getStrikePrice() ${} * 100.00", freeCash, optionPositionToExercise.getStrikePrice());
            freeCash += optionPositionToExercise.getStrikePrice() * 100.00;
            LOGGER.debug("freeCash == ${}", freeCash);
         }
      }
   }

   private void exerciseLongPut(Position optionPositionToExercise) {
      LOGGER.debug("Entering Portfolio.exerciseLongPut(Position {})", optionPositionToExercise.getPositionId());
      for (int contractsToHonor = 1; contractsToHonor <= optionPositionToExercise.getNumberTransacted(); contractsToHonor++) {
         Position puttingToStockPosition = findStockPositionToDeliver(optionPositionToExercise.getUnderlyingTicker());
         if (puttingToStockPosition != null) {
            puttingToStockPosition.close(optionPositionToExercise.getStrikePrice());
            try {
               closeDbPosition(puttingToStockPosition);
            } catch (SQLException sqle) {
               LOGGER.warn("Unable to update closed position {} in DB", puttingToStockPosition.getPositionId());
               LOGGER.debug("Caught (SQLException sqle)", sqle);
            }
            LOGGER.debug("freeCash ${} += optionPositionToExercise.getStrikePrice() ${} * optionPositionToExercise.getNumberTransacted() ${} * 100.0== ${}",
                    freeCash, optionPositionToExercise.getStrikePrice(), optionPositionToExercise.getNumberTransacted(),
                    (freeCash + (optionPositionToExercise.getStrikePrice() * optionPositionToExercise.getNumberTransacted() * 100)));
            freeCash += optionPositionToExercise.getStrikePrice() * optionPositionToExercise.getNumberTransacted() * 100.0;
         } else {
                /* Buy the stock at market price and deliver it */
            Position buyStockToDeliverPosition = Position.exerciseOptionPosition(optionPositionToExercise);
            LOGGER.debug("freeCash ${} -= buyStockToDeliverPosition.getCostBasis() ${} = ${}", freeCash, buyStockToDeliverPosition.getCostBasis());
            freeCash -= buyStockToDeliverPosition.getCostBasis();
            buyStockToDeliverPosition.close(optionPositionToExercise.getStrikePrice());
            LOGGER.debug("freeCash ${} += buyStockToDeliverPosition.getPriceAtOpen() ${} * buyStockToDeliverPosition.getNumberTransacted() ${} = ${}",
                    freeCash, buyStockToDeliverPosition.getPriceAtOpen(), buyStockToDeliverPosition.getNumberTransacted(), freeCash + buyStockToDeliverPosition.getPriceAtOpen() * buyStockToDeliverPosition.getNumberTransacted());
            freeCash += buyStockToDeliverPosition.getPriceAtOpen() * buyStockToDeliverPosition.getNumberTransacted();
         }
      }
   }

   private void exerciseLongCall(Position optionPositionToExercise) {
      LOGGER.debug("Entering Portfolio.exerciseLongCall(Position {})", optionPositionToExercise.getPositionId());
      Position optionToStockPosition = Position.exerciseOptionPosition(optionPositionToExercise);
      portfolioPositions.add(optionToStockPosition);
      LOGGER.debug("freeCash ${} -= optionToStockPosition.getCostBasis() ${} = ${}", freeCash, optionToStockPosition.getCostBasis(), (freeCash - optionToStockPosition.getCostBasis()));
      freeCash -= optionToStockPosition.getCostBasis();
      try {
         insertDbPosition(optionToStockPosition);
      } catch (SQLException sqle) {
         LOGGER.warn("Error writing to database position {} ", optionToStockPosition.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   void expireOptionPosition(Position optionPositionToExercise) {
      LOGGER.debug("Entering Portfolio.expireOptionPosition(Position {})", optionPositionToExercise.getPositionId());
      if(optionPositionToExercise.isStock()) {
         LOGGER.warn("Attempted to expire a stock position");
         return;
      }
      LOGGER.debug("freeCash ${} += optionPositionToExercise.getClaimAgainstCash() ${} = ${}",
              freeCash, optionPositionToExercise.getClaimAgainstCash(), (freeCash + optionPositionToExercise.getClaimAgainstCash()));
      freeCash += optionPositionToExercise.getClaimAgainstCash();
      LOGGER.debug("reservedCash -= optionPositionToExercise.getClaimAgainstCash()",
              reservedCash, optionPositionToExercise.getClaimAgainstCash(), (reservedCash - optionPositionToExercise.getClaimAgainstCash()));
      reservedCash -= optionPositionToExercise.getClaimAgainstCash();
      optionPositionToExercise.close(0.00);
      try {
         closeDbPosition(optionPositionToExercise);
      } catch (SQLException sqle) {
         LOGGER.warn("Unable to update closed position {} in DB", optionPositionToExercise.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   /* When exercising a short call this method returns a position that can fulfill delivery
   Must be a stock position with the same ticker as the short call. There may be multiple positions, so deliver
   the position with the lowest cost basis. Make sure there are at least 100 shares.
    */
   Position findStockPositionToDeliver(String tickerToDeliver) {
      LOGGER.debug("Entering Portfolio.findStockPositionToDeliver(String {})", tickerToDeliver);
      double lowestCostBasis = Double.MAX_VALUE;
      Position positionToDeliver = null;
      for (Position openPosition : getListOfOpenPositions()) {
         if (openPosition.isStock() &&
                 openPosition.getTicker().equals(tickerToDeliver) &&
                 (openPosition.getCostBasis() < lowestCostBasis) &&
                 (openPosition.getNumberTransacted() >= 100) ) {
            lowestCostBasis = openPosition.getCostBasis();
            positionToDeliver = openPosition;
         }
      }
      return positionToDeliver;
   }

   void endOfDayDbWrite() {
      LOGGER.debug("Entering Portfolio.endOfDayDbWrite()");
      try {
         dbSummaryWrite();
         dbPositionsWrite();
      } catch (SQLException sqle) {
         LOGGER.warn("Database error");
         LOGGER.debug("Caught (SQLException sqle) ", sqle);
         /** TODO : If cannot write to database write to file to preserve data */
      }
   }

   void dbSummaryWrite() throws SQLException {
      if (portfolioInDb()) {
         LOGGER.debug("Portfolio \"{}\" exists in database. Running updates instead of inserts", name);
         updateDbSummary();
      } else {
         LOGGER.debug("Inserting portfolio \"{}\" newly into database", name);
         insertDbSummary();
      }
   }








   public void expireOrder(Order expiredOrder) {
      LOGGER.debug("Entering Portfolio.expireOrder(Order {}) with freeCash {} and reservedCash {}",
         expiredOrder.getOrderId(), freeCash, reservedCash);
      freeCash += expiredOrder.getClaimAgainstCash();
      reservedCash -= expiredOrder.getClaimAgainstCash();
      LOGGER.debug("claimAgainstCash() ${}, freeCash ${}, reservedCash ${}", expiredOrder.getClaimAgainstCash(), freeCash, reservedCash);
      calculateTotalCash();
      expiredOrder.closeExpired();
      try {
         closeDbOrder(expiredOrder);
      } catch (SQLException sqle) {
         LOGGER.warn("Unable to update expired order {} in DB", expiredOrder.getOrderId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   void updateOptionPositions(Stock stock) throws IOException, SQLException {
      // TODO : Load returning options tickers into a set and only getLastTick() once
      LOGGER.debug("Entering Portfolio.updateOptionPositions");
      for(Position optionPositionToUpdate : getListOfOpenOptionPositions()) {
         if(stock.getTicker().equals(optionPositionToUpdate.getUnderlyingTicker())) {
            optionPositionToUpdate.setPrice(Option.lastTick(optionPositionToUpdate.getTicker()));
            optionPositionToUpdate.calculateNetAssetValue();
            updateDbPosition(optionPositionToUpdate);
         }
      }
   }

   void updateStockPositions(Stock stock) throws IOException, SQLException {
      LOGGER.debug("Entering Portfolio.updateStockPositions");
      for(Position stockPositionToUpdate : getListOfOpenStockPositions()) {
         if(stockPositionToUpdate.getTicker().equals(stock.getTicker())) {
            stockPositionToUpdate.setPrice(stock.getPrice());
            stockPositionToUpdate.calculateNetAssetValue();
            updateDbPosition(stockPositionToUpdate);
         }
      }
   }
}
