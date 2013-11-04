package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
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
   private double freeCash = GaussTrader.STARTING_CASH;
   private double reservedCash = 0.00;
   private double totalCash = 0.00;
   private int entryCount = 0;
   private List<Position> portfolioPositions = new ArrayList<>();
   private List<Order> portfolioOrders = new ArrayList<>();
   private static DataSource dataSource = GaussTrader.getDataSource();
   private static Connection dbConnectionTODO;
   private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);

   static {
      try {
         dbConnectionTODO = dataSource.getConnection();
      } catch (SQLException sqle) {
         LOGGER.warn("SQLException : dataSource.getConnection()", sqle);
      }
   }

   Portfolio() {
      LOGGER.debug("Entering Portfolio default constructor Portfolio()");
   }

   Portfolio(String portfolioName) {
      LOGGER.debug("Entering Portfolio constructor Portfolio(String {})", portfolioName);
      name = portfolioName;
      try {
         LOGGER.info("Starting with portfolio named \"{}\"", name);
         if (portfolioInDb()) {
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
      LOGGER.debug("Starting portfolio \"{}\" with netAssetValue {} reservedCash {} totalCash {} entryCount {}", name, netAssetValue, reservedCash, totalCash, entryCount);
   }

   Portfolio(String portfolioName, double startingCash) {
      this.name = portfolioName;
      this.freeCash = startingCash;
      try {
         if (portfolioInDb()) {
            LOGGER.error("Portfolio {} exists in database", portfolioName);
            LOGGER.error("*** END PROGRAM ***");
            System.exit(1);
         }
      } catch (SQLException sqle) {
         LOGGER.info("SQLException trying to call method portfolioInDb() from constructor Portfolio(String, double)");
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   boolean portfolioInDb() throws SQLException {
      LOGGER.debug("Entering Portfolio.portfolioInDb()");
      Connection dbConnection = dataSource.getConnection();
      PreparedStatement summarySqlStatement = dbConnection.prepareStatement("SELECT DISTINCT name FROM portfolios WHERE name = ?");
      summarySqlStatement.setString(1, name);
      LOGGER.debug("Executing SELECT DISTINCT name FROM portfolios WHERE name = {}", name);
      ResultSet portfolioSummaryResultSet = summarySqlStatement.executeQuery();
      return (portfolioSummaryResultSet.next());
   }

   void getDbPortfolio() throws SQLException {
      getDbPortfolioSummary();
      getDbPortfolioPositions();
      getDbPortfolioOrders();
   }

   void getDbPortfolioSummary() throws SQLException {
      Connection dbConnection = dataSource.getConnection();
      dbConnection = dataSource.getConnection();
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
      LOGGER.info("Returning from DB netAssetValue {} freeCash {} reservedCash {}", netAssetValue, freeCash, reservedCash);
   }

   void getDbPortfolioPositions() throws SQLException {
      Connection dbConnection = dataSource.getConnection();
      Position portfolioPositionEntry;
      PreparedStatement positionSqlStatement = dbConnection.prepareStatement("SELECT * FROM positions WHERE portfolio = ? AND open = true");
      positionSqlStatement.setString(1, name);
      LOGGER.debug("Executing SELECT * FROM positions WHERE portfolio = {} AND open = true", name);
      ResultSet openPositionsResultSet = positionSqlStatement.executeQuery();
      while (openPositionsResultSet.next()) {
         portfolioPositionEntry = dbToPortfolioPosition(openPositionsResultSet);
         LOGGER.debug("Adding {} {}", portfolioPositionEntry.getPositionId(), portfolioPositionEntry.getTicker());
         portfolioPositions.add(portfolioPositionEntry);
      }
   }

   void getDbPortfolioOrders() throws SQLException {
      Connection dbConnection = dataSource.getConnection();
      Order portfolioOrderEntry = null;
      PreparedStatement orderSqlStatement = dbConnection.prepareStatement("SELECT * FROM orders WHERE portfolio = ? AND open = true");
      orderSqlStatement.setString(1, name);
      LOGGER.debug("Executing SELECT * FROM orders WHERE portfolio = {} AND open = true", name);
      ResultSet openOrdersResultSet = orderSqlStatement.executeQuery();
      while (openOrdersResultSet.next()) {
         portfolioOrderEntry = dbToPortfolioOrder(openOrdersResultSet);
         LOGGER.debug("Adding {} {}", portfolioOrderEntry.getOrderId(), portfolioOrderEntry.getTicker());
         portfolioOrders.add(portfolioOrderEntry);
      }
   }

   private static Position dbToPortfolioPosition(ResultSet dbResult) throws SQLException {
      LOGGER.debug("Entering Portfolio.dbToPortfolioPosition(ResultSet dbResult)");
      Position positionFromDb = new Position();
      positionFromDb.setPositionId(dbResult.getLong("position_id"));
      positionFromDb.setOpen(true);
      positionFromDb.setTicker(dbResult.getString("ticker"));
      positionFromDb.setSecType(dbResult.getString("sec_type"));
      positionFromDb.setUnderlyingTicker(dbResult.getString("underlying_ticker"));
      positionFromDb.setStrikePrice(dbResult.getDouble("strike_price"));
      positionFromDb.setEpochOpened(dbResult.getLong("epoch_opened"));
      positionFromDb.setLongPosition(dbResult.getBoolean("long_position"));
      positionFromDb.setNumberTransacted(dbResult.getInt("number_transacted"));
      positionFromDb.setPriceAtOpen(dbResult.getDouble("price_at_open"));
      positionFromDb.setCostBasis(dbResult.getDouble("cost_basis"));
      positionFromDb.setLastTick(dbResult.getDouble("last_tick"));
      positionFromDb.setNetAssetValue(dbResult.getDouble("net_asset_value"));
      positionFromDb.setExpiry(new DateTime(dbResult.getLong("epoch_expiry"), DateTimeZone.forID("America/New_York")));
      positionFromDb.setClaimAgainstCash(dbResult.getDouble("claim_against_cash"));
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

   public int getEntryCount() {
      return entryCount;
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

   public void addNewOrder(Order orderToAdd) throws InsufficientFundsException, SecurityNotFoundException {
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
      entryCount++;
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
      freeCash -= position.getCostBasis();
      freeCash -= position.getClaimAgainstCash();
      reservedCash += position.getClaimAgainstCash();
      try {
         insertDbPosition(position);
      } catch (SQLException sqle) {
         LOGGER.warn("Unable to add position {} to DB", position.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
      entryCount++;
      LOGGER.debug("entryCount incremented to {}", entryCount);
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
         if (portfolioPosition.isOpen() && (portfolioPosition.isCall() || portfolioPosition.isPut())) {
            openOptionPositionList.add(portfolioPosition);
         }
      }
      LOGGER.debug("Returning {}", openOptionPositionList.toString());
      return openOptionPositionList;
   }

   public void fillOrder(Order orderToFill, double fillPrice) {
      LOGGER.debug("Entering Portfolio.fillOrder(Order {}, double {})", orderToFill.getOrderId(), fillPrice);
      Position positionTakenByOrder = new Position(orderToFill, fillPrice);
      portfolioPositions.add(positionTakenByOrder);
   /* Unreserve cash to fill order */
      freeCash += orderToFill.getClaimAgainstCash();
      reservedCash -= orderToFill.getClaimAgainstCash();
	/* Reserve cash if position creates liability (selling an option or shorting a stock) */
      freeCash -= positionTakenByOrder.getClaimAgainstCash();
      reservedCash += positionTakenByOrder.getClaimAgainstCash();
	/* Adjust free cash based on position cost basis */
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
      reservedCash -= optionPositionToExercise.getClaimAgainstCash();
   }

   private void exerciseShortPut(Position optionPositionToExercise) {
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
      //	LOGGER.debug("Entering Portfolio.exerciseShortCall(Position {})", optionPositionToExercise.getPositionId());
      //	LOGGER.debug("optionPositionToExercise.getNumberTransacted() = {}", optionPositionToExercise.getNumberTransacted());
      for (int contractsToHonor = 1; contractsToHonor <= optionPositionToExercise.getNumberTransacted(); contractsToHonor++) {
         //	    LOGGER.debug("contractsToHonor = {}", contractsToHonor);
         Position calledAwayStockPosition = findStockPositionToDeliver(optionPositionToExercise.getUnderlyingTicker());
         if (calledAwayStockPosition != null) {
            //		LOGGER.debug("calledAwayStockPosition != null");
            calledAwayStockPosition.close(optionPositionToExercise.getStrikePrice());
            //		LOGGER.debug("freeCash {} += optionPositionToExercise.getStrikePrice() {} * calledAwayStockPosition.getNumberTransacted() {}",
            //		freeCash, optionPositionToExercise.getStrikePrice(), calledAwayStockPosition.getNumberTransacted());
            freeCash += optionPositionToExercise.getStrikePrice() * calledAwayStockPosition.getNumberTransacted();
            //		LOGGER.debug("freeCash == {}", freeCash);
         } else {
		/* Buy the stock at market price and deliver it */
            //		LOGGER.debug("calledAwayStockPosition == null");
            Position buyStockToDeliverPosition = Position.exerciseOptionPosition(optionPositionToExercise);
            //		LOGGER.debug("Buying 100 shares at market price");
            //		LOGGER.debug("freeCash {} -= buyStockToDeliverPosition.getLastTick() {} * buyStockToDeliverPosition.getNumberTransacted() {}",
            //			     freeCash, buyStockToDeliverPosition.getLastTick(), buyStockToDeliverPosition.getNumberTransacted());
            freeCash -= buyStockToDeliverPosition.getLastTick() * buyStockToDeliverPosition.getNumberTransacted();
            //		LOGGER.debug("freeCash == {}", freeCash);
            //		LOGGER.debug("Selling 100 shares at strike price (delivering to call holder)");
            buyStockToDeliverPosition.close(optionPositionToExercise.getStrikePrice());
            //		LOGGER.debug("freeCash {} += optionPositionToExercise.getStrikePrice() {} * 100.00", freeCash, optionPositionToExercise.getStrikePrice());
            freeCash += optionPositionToExercise.getStrikePrice() * 100.00;
            //		LOGGER.debug("freeCash == {}", freeCash);
         }
      }
   }

   private void exerciseLongPut(Position optionPositionToExercise) {
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
            freeCash += optionPositionToExercise.getStrikePrice() * optionPositionToExercise.getNumberTransacted() * 100.0;
         } else {
                /* Buy the stock at market price and deliver it */
            Position buyStockToDeliverPosition = Position.exerciseOptionPosition(optionPositionToExercise);
            freeCash -= buyStockToDeliverPosition.getCostBasis();
            buyStockToDeliverPosition.close(optionPositionToExercise.getStrikePrice());
            freeCash += buyStockToDeliverPosition.getPriceAtOpen() * buyStockToDeliverPosition.getNumberTransacted();
         }
      }
   }

   private void exerciseLongCall(Position optionPositionToExercise) {
      Position optionToStockPosition = Position.exerciseOptionPosition(optionPositionToExercise);
      portfolioPositions.add(optionToStockPosition);
      freeCash -= optionToStockPosition.getCostBasis();
      try {
         insertDbPosition(optionToStockPosition);
      } catch (SQLException sqle) {
         LOGGER.warn("Error writing to database position {} ", optionToStockPosition.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   void expireOptionPosition(Position optionPositionToExercise) {
      freeCash += optionPositionToExercise.getClaimAgainstCash();
      reservedCash -= optionPositionToExercise.getClaimAgainstCash();
      optionPositionToExercise.close(0.00);
      try {
         closeDbPosition(optionPositionToExercise);
      } catch (SQLException sqle) {
         LOGGER.warn("Unable to update closed position {} in DB", optionPositionToExercise.getPositionId());
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }

   Position findStockPositionToDeliver(String tickerToDeliver) {
      LOGGER.debug("Entering Portfolio.findStockPositionToDeliver(String {})", tickerToDeliver);
      double lowestCostBasis = Double.MAX_VALUE;
      Position positionToDeliver = null;
      for (Position openPosition : getListOfOpenPositions()) {
         if (openPosition.isStock() && openPosition.getTicker().equals(tickerToDeliver) && (openPosition.getCostBasis() < lowestCostBasis)) {
            lowestCostBasis = openPosition.getCostBasis();
            positionToDeliver = openPosition;
         }
      }
      return positionToDeliver;
   }

   void endOfDayDbUpdate() {
      LOGGER.debug("Entering Portfolio.endOfDayDbUpdate()");
      PreparedStatement summarySqlStatement = null;
      ResultSet summaryResultSet = null;
      PreparedStatement positionSqlStatement = null;
      ResultSet positionResultSet = null;
      Connection dbConnection = null;
      try {
         dbConnection = dataSource.getConnection();
         if (portfolioInDb()) {
            LOGGER.debug("Portfolio \"{}\" exists in database. Running updates instead of inserts", name);
            updateDbSummary();
         } else {
            LOGGER.debug("Inserting portfolio \"{}\" newly into database", name);
            insertDbSummary();
         }
         positionSqlStatement = dbConnection.prepareStatement("SELECT * FROM positions WHERE portfolio = ? AND position_id = ?");
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
      } catch (SQLException sqle) {
         LOGGER.warn("Database error");
         LOGGER.debug("Caught (SQLException sqle) ", sqle);
         /** TODO : If cannot write to database write to file to preserve data */
      }
   }

   private void updateDbSummary() throws SQLException {
      LOGGER.debug("Entering Portfolio.updateDbSummary()");
      Connection dbConnection = dataSource.getConnection();
      String sqlString = new String("UPDATE portfolios SET net_asset_value = ?, free_cash = ?, reserved_cash = ?, total_cash = ? WHERE name = ?");
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
   }

   private void insertDbSummary() throws SQLException {
      LOGGER.debug("Entering Portfolio.insertDbSummary()");
      String sqlString = new String("INSERT INTO portfolios (name, net_asset_value, free_cash, reserved_cash, total_cash) VALUES (?, ?, ?, ?, ?)");
      PreparedStatement newSummarySqlStatement = null;
      int insertedRowCount;
      newSummarySqlStatement = dbConnectionTODO.prepareStatement(sqlString);
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
   }

   private void updateDbPosition(Position portfolioPosition) throws SQLException {
      LOGGER.debug("Entering Portfolio.updateDbPosition(Position {})", portfolioPosition.getPositionId());
      String sqlString = new String("UPDATE positions SET last_tick = ?, net_asset_value = ? WHERE position_id = ?");
      PreparedStatement updatePositionSqlStatement = null;
      int updatedRowCount;
      updatePositionSqlStatement = dbConnectionTODO.prepareStatement(sqlString);
      updatePositionSqlStatement.setDouble(1, portfolioPosition.getLastTick());
      updatePositionSqlStatement.setDouble(2, portfolioPosition.calculateNetAssetValue());
      updatePositionSqlStatement.setLong(3, portfolioPosition.getPositionId());
      LOGGER.debug("Executing UPDATE positions SET last_tick = {}, net_asset_value = {} WHERE position_id = {}",
         portfolioPosition.getLastTick(), portfolioPosition.calculateNetAssetValue(), portfolioPosition.getPositionId());
      if ((updatedRowCount = updatePositionSqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
      }
      if (!portfolioPosition.isOpen()) {
         sqlString = "UPDATE positions SET epoch_closed = ?, price_at_close = ?, profit = ?, open = 'false' WHERE position_id = ?";
         updatePositionSqlStatement = dbConnectionTODO.prepareStatement(sqlString);
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
   }

   private void insertDbPosition(Position portfolioPosition) throws SQLException {
      LOGGER.debug("Entering Portfolio.insertDbPosition(Position {})", portfolioPosition.getPositionId());
      String sqlString = new String("INSERT INTO positions (portfolio, position_id, open, ticker, sec_type, epoch_expiry, " +
         "underlying_ticker, strike_price, epoch_opened, long_position, number_transacted, price_at_open, " +
         "cost_basis, last_tick, net_asset_value, claim_against_cash) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      PreparedStatement newPositionSqlStatement = null;
      int insertedRowCount;
      int updatedRowCount;
      newPositionSqlStatement = dbConnectionTODO.prepareStatement(sqlString);
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
      newPositionSqlStatement.setDouble(14, portfolioPosition.getLastTick());
      newPositionSqlStatement.setDouble(15, portfolioPosition.calculateNetAssetValue());
      newPositionSqlStatement.setDouble(16, portfolioPosition.getClaimAgainstCash());
      LOGGER.debug("Executing INSERT INTO positions (portfolio, position_id, open, ticker, sec_type, epoch_expiry, underlying_ticker, strike_price, epoch_opened, " +
         "long_position, number_transacted, price_at_open, cost_basis, last_tick, net_asset_value, claim_against_cash) " +
         "VALUES ({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {} {})",
         name, portfolioPosition.getPositionId(), portfolioPosition.isOpen(), portfolioPosition.getTicker(), portfolioPosition.getSecType(),
         portfolioPosition.getExpiry().getMillis(), portfolioPosition.getTicker(), portfolioPosition.getStrikePrice(), portfolioPosition.getEpochOpened(),
         portfolioPosition.isLong(), portfolioPosition.getNumberTransacted(), portfolioPosition.getPriceAtOpen(), portfolioPosition.getCostBasis(),
         portfolioPosition.getLastTick(), portfolioPosition.calculateNetAssetValue(), portfolioPosition.getClaimAgainstCash());
      if ((insertedRowCount = newPositionSqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Inserted {} rows. Should have inserted 1 row", insertedRowCount);
      }
   }

   private void closeDbPosition(Position positionToClose) throws SQLException {
      int updatedRowCount = 0;
      String sqlString = "UPDATE positions SET epoch_closed = ?, price_at_close = ?, profit = ?, open = 'false' WHERE position_id = ?";
      PreparedStatement newPositionSqlStatement = null;
      newPositionSqlStatement = dbConnectionTODO.prepareStatement(sqlString);
      newPositionSqlStatement.setLong(1, positionToClose.getEpochClosed());
      newPositionSqlStatement.setDouble(2, positionToClose.getPriceAtClose());
      newPositionSqlStatement.setDouble(3, positionToClose.getProfit());
      newPositionSqlStatement.setLong(4, positionToClose.getPositionId());
      LOGGER.debug("Executing UPDATE positions SET epoch_closed = {}, price_at_close = {}, profit = {}, open = 'false' WHERE position_id = {}",
         positionToClose.getEpochClosed(), positionToClose.getPriceAtClose(), positionToClose.getProfit(), positionToClose.getPositionId());
      if ((updatedRowCount = newPositionSqlStatement.executeUpdate()) != 1) {
         LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
      }
   }

   private void closeDbOrder(Order portfolioOrder) throws SQLException {
	/* Nothing changes in an order unless it is filled (i.e. closed) */
      LOGGER.debug("Entering Portfolio.closeDbOrder(Order {})", portfolioOrder.getOrderId());
      if (!portfolioOrder.isOpen()) {
         String sqlString = new String("UPDATE orders SET open = false, epoch_closed = ?, close_reason = ?, fill_price = ? WHERE order_id = ?");
         PreparedStatement updateOrderSqlStatement = null;
         int updatedRowCount;
         updateOrderSqlStatement = dbConnectionTODO.prepareStatement(sqlString);
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
   }

   private void insertDbOrder(Order portfolioOrder) throws SQLException {
      LOGGER.debug("Entering Portfolio.insertDbOrder(Order {})", portfolioOrder.getOrderId());
      String sqlString = new String("INSERT INTO orders (portfolio, order_id, open, ticker, epoch_expiry, underlying_ticker, strike_price, limit_price, " +
         "action, total_quantity, sec_type, tif, epoch_opened, claim_against_cash) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      PreparedStatement newOrderSqlStatement = null;
      int insertedRowCount;
      int updatedRowCount;
      newOrderSqlStatement = dbConnectionTODO.prepareStatement(sqlString);
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
         newOrderSqlStatement = dbConnectionTODO.prepareStatement(sqlString);
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
}
