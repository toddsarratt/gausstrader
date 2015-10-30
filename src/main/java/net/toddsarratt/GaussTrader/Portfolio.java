package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Portfolio {
   private String name = Long.toString(System.currentTimeMillis());
   private double netAssetValue = 0.00;
   private double freeCash = Constants.STARTING_CASH;
   private double reservedCash = 0.00;
   private double totalCash = 0.00;
   private List<Position> portfolioPositions = new ArrayList<>();
   private List<Order> portfolioOrders = new ArrayList<>();
   private static final DataStore dataStore = GaussTrader.getDataStore();
   private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);

   Portfolio() {
      LOGGER.debug("Entering Portfolio default constructor Portfolio()");
   }

   Portfolio(String portfolioName) {
      LOGGER.debug("Entering Portfolio constructor Portfolio(String {})", portfolioName);
      name = portfolioName;
      try {
         LOGGER.info("Starting with portfolio named \"{}\"", name);
         if (portfolioInStore()) {
            getDbPortfolio();
         } else {
            LOGGER.info("Could not find portfolio \"{}\" in database", portfolioName);
            LOGGER.info("Created portfolio \"{}\" with {} free cash", portfolioName, freeCash);
         }
      } catch (SQLException sqle) {
         LOGGER.info("Error communicating with {}", GaussTrader.DB_NAME);
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
      this.calculateNetAssetValue();
      LOGGER.debug("Starting portfolio \"{}\" with netAssetValue {} reservedCash {} totalCash {}", name, netAssetValue, reservedCash, totalCash);
   }

   @SuppressWarnings("WeakerAccess")
   Portfolio(String portfolioName, double startingCash) {
      LOGGER.debug("Entering Portfolio constructor Portfolio(String {})", portfolioName);
      this.name = portfolioName;
      this.freeCash = startingCash;
      try {
         if (portfolioInStore()) {
            LOGGER.error("Portfolio {} exists in database", portfolioName);
            LOGGER.error("*** END PROGRAM ***");
            System.exit(1);
         }
      } catch (SQLException sqle) {
         LOGGER.info("SQLException trying to call method portfolioInDb() from constructor Portfolio(String, double)");
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   boolean portfolioInStore() throws SQLException {
      LOGGER.debug("Entering Portfolio.portfolioInDb()");
      Connection dbConnection = dataSource.getConnection();
      PreparedStatement summarySqlStatement = dbConnection.prepareStatement("SELECT DISTINCT name FROM portfolios WHERE name = ?");
      summarySqlStatement.setString(1, name);
      LOGGER.debug("Executing SELECT DISTINCT name FROM portfolios WHERE name = {}", name);
      ResultSet portfolioSummaryResultSet = summarySqlStatement.executeQuery();
      dbConnection.close();
      return (portfolioSummaryResultSet.next());
   }

   void getPortfolioFromStore() throws SQLException {
      LOGGER.debug("Entering Portfolio.getDbPortfolio()");
      getDbPortfolioSummary();
      getDbPortfolioPositions();
      getDbPortfolioOrders();
   }

   void getDbPortfolioSummary() throws SQLException {
      LOGGER.debug("Entering Portfolio.getDbPortfolioSummary()");
      Connection dbConnection = dataSource.getConnection();
      PreparedStatement portfolioSummaryStatement = dbConnection.prepareStatement("SELECT * FROM portfolios WHERE name = ?");
      portfolioSummaryStatement.setString(1, name);
      LOGGER.debug("Executing SELECT * FROM portfolios WHERE name = {}", name);
      ResultSet portfolioSummaryResultSet = portfolioSummaryStatement.executeQuery();
      if (portfolioSummaryResultSet.next()) {
         netAssetValue = portfolioSummaryResultSet.getDouble("net_asset_value");
         freeCash = portfolioSummaryResultSet.getDouble("free_cash");
         reservedCash = portfolioSummaryResultSet.getDouble("reserved_cash");
         calculateTotalCash();
      }
      dbConnection.close();
      LOGGER.info("Returning from DB netAssetValue {} freeCash {} reservedCash {}", netAssetValue, freeCash, reservedCash);
   }

   void getDbPortfolioPositions() throws SQLException {
      LOGGER.debug("Entering Portfolio.getDbPortfolioPositions()");
      Connection dbConnection = dataSource.getConnection();
      Position portfolioPositionEntry;
      PreparedStatement positionSqlStatement = dbConnection.prepareStatement("SELECT * FROM positions WHERE portfolio = ? AND open = true");
      positionSqlStatement.setString(1, name);
      LOGGER.debug("Executing SELECT * FROM positions WHERE portfolio = {} AND open = true", name);
      ResultSet openPositionsResultSet = positionSqlStatement.executeQuery();
      while (openPositionsResultSet.next()) {
         portfolioPositionEntry = dbToPortfolioPosition(openPositionsResultSet);
         if(portfolioPositionEntry.isExpired()) {
            LOGGER.warn("Position {} read from the database has expired", portfolioPositionEntry.getTicker());
            reconcileExpiredOptionPosition(portfolioPositionEntry);
         } else {
            LOGGER.debug("Adding {} {}", portfolioPositionEntry.getPositionId(), portfolioPositionEntry.getTicker());
            portfolioPositions.add(portfolioPositionEntry);
         }
      }
      dbConnection.close();
   }

   void getDbPortfolioOrders() throws SQLException {
      LOGGER.debug("Entering Portfolio.getDbPortfolioOrders()");
      Connection dbConnection = dataSource.getConnection();
      Order portfolioOrderEntry;
      PreparedStatement orderSqlStatement = dbConnection.prepareStatement("SELECT * FROM orders WHERE portfolio = ? AND open = true");
      orderSqlStatement.setString(1, name);
      LOGGER.debug("Executing SELECT * FROM orders WHERE portfolio = {} AND open = true", name);
      ResultSet openOrdersResultSet = orderSqlStatement.executeQuery();
      while (openOrdersResultSet.next()) {
         portfolioOrderEntry = dbToPortfolioOrder(openOrdersResultSet);
         LOGGER.debug("Adding {} {}", portfolioOrderEntry.getOrderId(), portfolioOrderEntry.getTicker());
         portfolioOrders.add(portfolioOrderEntry);
      }
      dbConnection.close();
   }

   public static Position dbToPortfolioPosition(ResultSet dbResult) throws SQLException {
      LOGGER.debug("Entering Portfolio.dbToPortfolioPosition(ResultSet dbResult)");
      Position positionFromDb = new Position();
      positionFromDb.setPositionId(dbResult.getLong("position_id"));
      positionFromDb.setOpen(dbResult.getBoolean("open"));
      positionFromDb.setTicker(dbResult.getString("ticker"));
      positionFromDb.setSecType(dbResult.getString("sec_type"));
      positionFromDb.setUnderlyingTicker(dbResult.getString("underlying_ticker"));
      positionFromDb.setStrikePrice(dbResult.getDouble("strike_price"));
      positionFromDb.setEpochOpened(dbResult.getLong("epoch_opened"));
      positionFromDb.setLongPosition(dbResult.getBoolean("long_position"));
      positionFromDb.setNumberTransacted(dbResult.getInt("number_transacted"));
      positionFromDb.setPriceAtOpen(dbResult.getDouble("price_at_open"));
      positionFromDb.setCostBasis(dbResult.getDouble("cost_basis"));
      positionFromDb.setPrice(dbResult.getDouble("last_tick"));
      positionFromDb.setNetAssetValue(dbResult.getDouble("net_asset_value"));
      positionFromDb.setExpiry(new DateTime(dbResult.getLong("epoch_expiry"), DateTimeZone.forID("America/New_York")));
      positionFromDb.setClaimAgainstCash(dbResult.getDouble("claim_against_cash"));
      positionFromDb.setOriginatingOrderId(dbResult.getLong("originating_order_id"));
      return positionFromDb;
   }

   private static Order dbToPortfolioOrder(ResultSet dbResult) throws SQLException {
      LOGGER.debug("Entering Portfolio.dbToPortfolioOrder(ResultSet dbResult)");
      Order orderFromDb = new Order();
      orderFromDb.setOrderId(dbResult.getLong("order_id"));
      orderFromDb.setOpen(true);
      orderFromDb.setTicker(dbResult.getString("ticker"));
      orderFromDb.setUnderlyingTicker(dbResult.getString("underlying_ticker"));
      orderFromDb.setStrikePrice(dbResult.getDouble("strike_price"));
      orderFromDb.setLimitPrice(dbResult.getDouble("limit_price"));
      orderFromDb.setAction(dbResult.getString("action"));
      orderFromDb.setTotalQuantity(dbResult.getInt("total_quantity"));
      orderFromDb.setSecType(dbResult.getString("sec_type"));
      orderFromDb.setTif(dbResult.getString("tif"));
      orderFromDb.setEpochOpened(dbResult.getLong("epoch_opened"));
      orderFromDb.setExpiry(dbResult.getLong("epoch_expiry"));
      orderFromDb.setClaimAgainstCash(dbResult.getDouble("claim_against_cash"));
      return orderFromDb;
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

   @SuppressWarnings("UnusedDeclaration")
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
         insertDbOrder(orderToAdd);
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

   private void dbPositionsWrite() throws SQLException {
      LOGGER.debug("Entering Portfolio.endOfDayDbPositionsWrite()");
      Connection dbConnection = dataSource.getConnection();
      ResultSet positionResultSet;
      PreparedStatement positionSqlStatement = dbConnection.prepareStatement("SELECT * FROM positions WHERE portfolio = ? AND position_id = ?");
      positionSqlStatement.setString(1, name);
      for (Position portfolioPosition : portfolioPositions) {
         positionSqlStatement.setLong(2, portfolioPosition.getPositionId());
         LOGGER.debug("Executing SELECT * FROM positions WHERE portfolio = {} AND position_id = {}", name, portfolioPosition.getPositionId());
         positionResultSet = positionSqlStatement.executeQuery();
         if (positionResultSet.next()) {
            LOGGER.debug("positionResultSet.next() == true, position {} exists in database. Running update instead of insert", portfolioPosition.getPositionId());
            updateDbPosition(portfolioPosition);
         } else {
            LOGGER.debug("positionResultSet.next() == false, inserting position {} newly into database", portfolioPosition.getPositionId());
            insertDbPosition(portfolioPosition);
         }
      }
      dbConnection.close();
   }

   private void updateSummaryToStore() throws SQLException {
      LOGGER.debug("Entering Portfolio.updateDbSummary()");
      Connection dbConnection = dataSource.getConnection();
      String sqlString = "UPDATE portfolios SET net_asset_value = ?, free_cash = ?, reserved_cash = ?, total_cash = ? WHERE name = ?";
      PreparedStatement updateSummarySqlStatement = dbConnection.prepareStatement(sqlString);
      int updatedRowCount;
      calculateNetAssetValue();
      updateSummarySqlStatement.setDouble(1, netAssetValue);
      updateSummarySqlStatement.setDouble(2, freeCash);
      updateSummarySqlStatement.setDouble(3, reservedCash);
      updateSummarySqlStatement.setDouble(4, totalCash);
      updateSummarySqlStatement.setString(5, name);
      LOGGER.debug("Executing UPDATE portfolios SET net_asset_value = {}, free_cash = {}, reserved_cash = {}, total_cash = {} WHERE name = {}",
         netAssetValue, freeCash, reservedCash, totalCash, name);
      if ((updatedRowCount = updateSummarySqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
      }
      dbConnection.close();
   }

   private void insertSummaryToStore() throws SQLException {
      LOGGER.debug("Entering Portfolio.insertDbSummary()");
      Connection dbConnection = dataSource.getConnection();
      String sqlString = "INSERT INTO portfolios (name, net_asset_value, free_cash, reserved_cash, total_cash) VALUES (?, ?, ?, ?, ?)";
      PreparedStatement newSummarySqlStatement;
      int insertedRowCount;
      newSummarySqlStatement = dbConnection.prepareStatement(sqlString);
      newSummarySqlStatement.setString(1, name);
      newSummarySqlStatement.setDouble(2, netAssetValue);
      newSummarySqlStatement.setDouble(3, freeCash);
      newSummarySqlStatement.setDouble(4, reservedCash);
      newSummarySqlStatement.setDouble(5, totalCash);
      LOGGER.debug("Executing INSERT INTO portfolios (name, net_asset_value, free_cash, reserved_cash, total_cash) VALUES ({}, {}, {}, {}, {})",
         name, netAssetValue, freeCash, reservedCash, totalCash);
      if ((insertedRowCount = newSummarySqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Inserted {} rows. Should have inserted 1 row", insertedRowCount);
      }
      dbConnection.close();
   }

   private void updateDbPosition(Position portfolioPosition) throws SQLException {
      LOGGER.debug("Entering Portfolio.updateDbPosition(Position {})", portfolioPosition.getPositionId());
      Connection dbConnection = dataSource.getConnection();
      String sqlString = "UPDATE positions SET last_tick = ?, net_asset_value = ? WHERE position_id = ?";
      PreparedStatement updatePositionSqlStatement;
      int updatedRowCount;
      double positionPrice = portfolioPosition.getPrice();
      updatePositionSqlStatement = dbConnection.prepareStatement(sqlString);
      updatePositionSqlStatement.setDouble(1, positionPrice);
      updatePositionSqlStatement.setDouble(2, portfolioPosition.calculateNetAssetValue());
      updatePositionSqlStatement.setLong(3, portfolioPosition.getPositionId());
      LOGGER.debug("Executing UPDATE positions SET last_tick = {}, net_asset_value = {} WHERE position_id = {}",
         positionPrice, portfolioPosition.calculateNetAssetValue(), portfolioPosition.getPositionId());
      if ((updatedRowCount = updatePositionSqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
      }
      if (!portfolioPosition.isOpen()) {
         sqlString = "UPDATE positions SET epoch_closed = ?, price_at_close = ?, profit = ?, open = 'false' WHERE position_id = ?";
         updatePositionSqlStatement = dbConnection.prepareStatement(sqlString);
         updatePositionSqlStatement.setLong(1, portfolioPosition.getEpochClosed());
         updatePositionSqlStatement.setDouble(2, portfolioPosition.getPriceAtClose());
         updatePositionSqlStatement.setDouble(3, portfolioPosition.getProfit());
         updatePositionSqlStatement.setLong(4, portfolioPosition.getPositionId());
         LOGGER.debug("Executing UPDATE positions SET epoch_closed = {}, price_at_close = {}, profit = {}, open = 'false' WHERE position_id = {}",
            portfolioPosition.getEpochClosed(), portfolioPosition.getPriceAtClose(), portfolioPosition.getProfit(), portfolioPosition.getPositionId());
      }
      if ((updatedRowCount = updatePositionSqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
      }
      dbConnection.close();
   }

   private void insertDbPosition(Position portfolioPosition) throws SQLException {
      LOGGER.debug("Entering Portfolio.insertDbPosition(Position {})", portfolioPosition.getPositionId());
      Connection dbConnection = dataSource.getConnection();
      String sqlString = "INSERT INTO positions (portfolio, position_id, open, ticker, sec_type, epoch_expiry, " +
         "underlying_ticker, strike_price, epoch_opened, long_position, number_transacted, price_at_open, " +
         "cost_basis, last_tick, net_asset_value, claim_against_cash, originating_order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      PreparedStatement newPositionSqlStatement;
      int insertedRowCount;
      double positionPrice = portfolioPosition.getPrice();
      newPositionSqlStatement = dbConnection.prepareStatement(sqlString);
      newPositionSqlStatement.setString(1, name);
      newPositionSqlStatement.setLong(2, portfolioPosition.getPositionId());
      newPositionSqlStatement.setBoolean(3, portfolioPosition.isOpen());
      newPositionSqlStatement.setString(4, portfolioPosition.getTicker());
      newPositionSqlStatement.setString(5, portfolioPosition.getSecType());
      newPositionSqlStatement.setLong(6, portfolioPosition.getExpiry().getMillis());
      newPositionSqlStatement.setString(7, portfolioPosition.getUnderlyingTicker());
      newPositionSqlStatement.setDouble(8, portfolioPosition.getStrikePrice());
      newPositionSqlStatement.setLong(9, portfolioPosition.getEpochOpened());
      newPositionSqlStatement.setBoolean(10, portfolioPosition.isLong());
      newPositionSqlStatement.setInt(11, portfolioPosition.getNumberTransacted());
      newPositionSqlStatement.setDouble(12, portfolioPosition.getPriceAtOpen());
      newPositionSqlStatement.setDouble(13, portfolioPosition.getCostBasis());
      newPositionSqlStatement.setDouble(14, positionPrice);
      newPositionSqlStatement.setDouble(15, portfolioPosition.calculateNetAssetValue());
      newPositionSqlStatement.setDouble(16, portfolioPosition.getClaimAgainstCash());
      newPositionSqlStatement.setDouble(17, portfolioPosition.getOriginatingOrderId());
      LOGGER.debug("Executing INSERT INTO positions (portfolio, position_id, open, ticker, sec_type, epoch_expiry, underlying_ticker, strike_price, epoch_opened, " +
         "long_position, number_transacted, price_at_open, cost_basis, last_tick, net_asset_value, claim_against_cash, originating_order_id) " +
         "VALUES ({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})",
         name, portfolioPosition.getPositionId(), portfolioPosition.isOpen(), portfolioPosition.getTicker(), portfolioPosition.getSecType(),
         portfolioPosition.getExpiry().getMillis(), portfolioPosition.getTicker(), portfolioPosition.getStrikePrice(), portfolioPosition.getEpochOpened(),
         portfolioPosition.isLong(), portfolioPosition.getNumberTransacted(), portfolioPosition.getPriceAtOpen(), portfolioPosition.getCostBasis(),
         positionPrice, portfolioPosition.calculateNetAssetValue(), portfolioPosition.getClaimAgainstCash(), portfolioPosition.getOriginatingOrderId());
      if ((insertedRowCount = newPositionSqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Inserted {} rows. Should have inserted 1 row", insertedRowCount);
      }
      dbConnection.close();
   }

   private void closeDbPosition(Position positionToClose) throws SQLException {
      int updatedRowCount;
      Connection dbConnection = dataSource.getConnection();
      String sqlString = "UPDATE positions SET epoch_closed = ?, price_at_close = ?, profit = ?, open = 'false' WHERE position_id = ?";
      PreparedStatement newPositionSqlStatement;
      newPositionSqlStatement = dbConnection.prepareStatement(sqlString);
      newPositionSqlStatement.setLong(1, positionToClose.getEpochClosed());
      newPositionSqlStatement.setDouble(2, positionToClose.getPriceAtClose());
      newPositionSqlStatement.setDouble(3, positionToClose.getProfit());
      newPositionSqlStatement.setLong(4, positionToClose.getPositionId());
      LOGGER.debug("Executing UPDATE positions SET epoch_closed = {}, price_at_close = {}, profit = {}, open = 'false' WHERE position_id = {}",
         positionToClose.getEpochClosed(), positionToClose.getPriceAtClose(), positionToClose.getProfit(), positionToClose.getPositionId());
      if ((updatedRowCount = newPositionSqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
      }
      dbConnection.close();
   }

   private void closeDbOrder(Order portfolioOrder) throws SQLException {
	/* Nothing changes in an order unless it is filled (i.e. closed) */
      LOGGER.debug("Entering Portfolio.closeDbOrder(Order {})", portfolioOrder.getOrderId());
      Connection dbConnection = dataSource.getConnection();
      if (!portfolioOrder.isOpen()) {
         String sqlString = "UPDATE orders SET open = false, epoch_closed = ?, close_reason = ?, fill_price = ? WHERE order_id = ?";
         PreparedStatement updateOrderSqlStatement;
         int updatedRowCount;
         updateOrderSqlStatement = dbConnection.prepareStatement(sqlString);
         updateOrderSqlStatement.setLong(1, portfolioOrder.getEpochClosed());
         updateOrderSqlStatement.setString(2, portfolioOrder.getCloseReason());
         updateOrderSqlStatement.setDouble(3, portfolioOrder.getFillPrice());
         updateOrderSqlStatement.setLong(4, portfolioOrder.getOrderId());
         LOGGER.debug("Executing UPDATE orders SET open = false, epoch_closed = {}, close_reason = {}, fill_price = {} WHERE order_id = {}",
            portfolioOrder.getEpochClosed(), portfolioOrder.getCloseReason(), portfolioOrder.getFillPrice(), portfolioOrder.getOrderId());
         if ((updatedRowCount = updateOrderSqlStatement.executeUpdate()) != 1) {
            LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
         }
      }
      dbConnection.close();
   }

   private void insertDbOrder(Order portfolioOrder) throws SQLException {
      LOGGER.debug("Entering Portfolio.insertDbOrder(Order {})", portfolioOrder.getOrderId());
      Connection dbConnection = dataSource.getConnection();
      String sqlString = "INSERT INTO orders (portfolio, order_id, open, ticker, epoch_expiry, underlying_ticker, strike_price, limit_price, " +
         "action, total_quantity, sec_type, tif, epoch_opened, claim_against_cash) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      PreparedStatement newOrderSqlStatement;
      int insertedRowCount;
      int updatedRowCount;
      newOrderSqlStatement = dbConnection.prepareStatement(sqlString);
      newOrderSqlStatement.setString(1, name);
      newOrderSqlStatement.setLong(2, portfolioOrder.getOrderId());
      newOrderSqlStatement.setBoolean(3, portfolioOrder.isOpen());
      newOrderSqlStatement.setString(4, portfolioOrder.getTicker());
      newOrderSqlStatement.setLong(5, portfolioOrder.getExpiry().getMillis());
      newOrderSqlStatement.setString(6, portfolioOrder.getUnderlyingTicker());
      newOrderSqlStatement.setDouble(7, portfolioOrder.getStrikePrice());
      newOrderSqlStatement.setDouble(8, portfolioOrder.getLimitPrice());
      newOrderSqlStatement.setString(9, portfolioOrder.getAction());
      newOrderSqlStatement.setInt(10, portfolioOrder.getTotalQuantity());
      newOrderSqlStatement.setString(11, portfolioOrder.getSecType());
      newOrderSqlStatement.setString(12, portfolioOrder.getTif());
      newOrderSqlStatement.setLong(13, portfolioOrder.getEpochOpened());
      newOrderSqlStatement.setDouble(14, portfolioOrder.getClaimAgainstCash());
      LOGGER.debug("Executing INSERT INTO orders (portfolio, order_id, open, ticker, epoch_expiry, underlying_ticker, strike_price, limit_price, " +
         "action, total_quantity, sec_type, tif, epoch_opened,claim_against_cash) VALUES ({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})",
         name, portfolioOrder.getOrderId(), portfolioOrder.isOpen(), portfolioOrder.getTicker(), portfolioOrder.getExpiry().getMillis(),
         portfolioOrder.getUnderlyingTicker(), portfolioOrder.getStrikePrice(), portfolioOrder.getLimitPrice(), portfolioOrder.getAction(),
         portfolioOrder.getTotalQuantity(), portfolioOrder.getSecType(), portfolioOrder.getTif(), portfolioOrder.getEpochOpened(),
         portfolioOrder.getClaimAgainstCash());
      if ((insertedRowCount = newOrderSqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Inserted {} rows. Should have inserted 1 row", insertedRowCount);
      }
      if (!portfolioOrder.isOpen()) {
         sqlString = "UPDATE orders SET epoch_closed = ?, close_reason = ?, fill_price = ?, open = 'false' WHERE order_id = ?";
         newOrderSqlStatement = dbConnection.prepareStatement(sqlString);
         newOrderSqlStatement.setLong(1, portfolioOrder.getEpochClosed());
         newOrderSqlStatement.setString(2, portfolioOrder.getCloseReason());
         newOrderSqlStatement.setDouble(3, portfolioOrder.getFillPrice());
         newOrderSqlStatement.setLong(4, portfolioOrder.getOrderId());
         LOGGER.debug("Executing UPDATE orders SET epoch_closed = {}, close_reason = {}, fill_price = {}, open = 'false' WHERE order_id = {}",
            portfolioOrder.getEpochClosed(), portfolioOrder.getCloseReason(), portfolioOrder.getFillPrice(), portfolioOrder.getOrderId());
         if ((updatedRowCount = newOrderSqlStatement.executeUpdate()) != 1) {
            LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
         }
      }
      dbConnection.close();
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
