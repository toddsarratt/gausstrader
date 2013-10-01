package net.toddsarratt.GaussTrader;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import javax.sql.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Portfolio {
    private String name;
    private double netAssetValue = 0.00;
    private double freeCash = 0.00;
    private double reservedCash = 0.00;
    private double totalCash = 0.00;
    private int entryCount = 0;
    private List<Position> portfolioPositions = new ArrayList<>();
    private List<Order> portfolioOrders = new ArrayList<>();
    private static DataSource dataSource = null;
    private static Connection dbConnection = null;
    private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);

    Portfolio() {
	LOGGER.debug("Entering Portfolio default constructor Portfolio()");
    }

    Portfolio(String portfolioName, DataSource dataSource) {
	LOGGER.debug("Entering Portfolio constructor Portfolio(String {}, DataSource {})", portfolioName, dataSource);
        name = portfolioName;
	this.dataSource = dataSource;
        try {
            LOGGER.debug("Getting connection to {}", GaussTrader.DB_NAME);
            dbConnection = dataSource.getConnection();
	    LOGGER.info("Starting with portfolio named \"{}\"", name);
	    if(portfolioInDb()) {
		getDbPortfolio();
	    } else {
		LOGGER.info("Could not find portfolio \"{}\" in database", name);
		freeCash = GaussTrader.STARTING_CASH;
		LOGGER.info("Creating portfolio \"{}\" with {} free cash", name, freeCash);
	    }
	} catch(SQLException sqle) {
	    LOGGER.info("Unable to get connection to {}", GaussTrader.DB_NAME);
	    LOGGER.debug("Caught (SQLException sqle)", sqle);
	} 
	/* Leave connection open for further writes from Portfolio ... ?
	finally {
	    if(dbConnection != null) {
		try {
		    dbConnection.close();
		} catch (SQLException sqle) {
		    LOGGER.info("SQLException trying to close {}", GaussTrader.DB_NAME);
		    LOGGER.debug("Caught (SQLException sqle)", sqle);
		} 
	    }
	}
	*/
	this.calculateNetAssetValue();
	LOGGER.debug("Starting portfolio \"{}\" with netAssetValue {} reservedCash {} totalCash {} entryCount {}", name, netAssetValue, reservedCash, totalCash, entryCount);
    }

    Portfolio(String portfolioName, double startingCash) {
	this.name = portfolioName;
	this.freeCash = startingCash;
	try {
	    if(portfolioInDb()) {
		LOGGER.error("Portfolio {} exists in database", portfolioName);
		LOGGER.error("*** END PROGRAM ***");
		System.exit(1);
	    }
	} catch(SQLException sqle) {
	    LOGGER.info("SQLException trying to call method portfolioInDb() from constructor Portfolio(String, double)");
	    LOGGER.debug("Caught (SQLException sqle)", sqle);
	}
    }
    private boolean portfolioInDb() throws SQLException {
	LOGGER.debug("Entering Portfolio.portfolioInDb()");
	PreparedStatement summarySqlStatement = dbConnection.prepareStatement("SELECT * FROM portfolios WHERE name = ?");
	summarySqlStatement.setString(1, name);
	LOGGER.debug("Executing SELECT * FROM portfolios WHERE name = {}", name);
	ResultSet portfolioSummaryResultSet = summarySqlStatement.executeQuery();
	if(!portfolioSummaryResultSet.next()) {
	    return false;
	} else {
	    netAssetValue = portfolioSummaryResultSet.getDouble("net_asset_value");
	    freeCash = portfolioSummaryResultSet.getDouble("free_cash");
	    reservedCash = portfolioSummaryResultSet.getDouble("reserved_cash");
	    LOGGER.info("Returning from DB netAssetValue {} freeCash {} reservedCash {}", netAssetValue, freeCash, reservedCash);
	    return true;
	}
    }

    private void getDbPortfolio() throws SQLException {
        Position portfolioPositionEntry = null;
        Order portfolioOrderEntry = null;
        PreparedStatement positionSqlStatement = dbConnection.prepareStatement("SELECT * FROM positions WHERE portfolio = ? AND open = true");
	positionSqlStatement.setString(1, name);
	LOGGER.debug("Executing SELECT * FROM positions WHERE portfolio = {} AND open = true", name);
        ResultSet openPositionsResultSet = positionSqlStatement.executeQuery();
	while(openPositionsResultSet.next()) {
	    portfolioPositionEntry = dbToPortfolioPosition(openPositionsResultSet);
	    LOGGER.debug("Adding {} {}", portfolioPositionEntry.getPositionId(), portfolioPositionEntry.getTicker());
	    portfolioPositions.add(portfolioPositionEntry);
	}
	PreparedStatement orderSqlStatement = dbConnection.prepareStatement("SELECT * FROM orders WHERE portfolio = ? AND open = true");
	orderSqlStatement.setString(1, name);
	LOGGER.debug("Executing SELECT * FROM orders WHERE portfolio = {} AND open = true", name);
        ResultSet openOrdersResultSet = orderSqlStatement.executeQuery();
	while(openOrdersResultSet.next()) {
	    portfolioOrderEntry = dbToPortfolioOrder(openOrdersResultSet);
            LOGGER.debug("Adding {} {}", portfolioOrderEntry.getOrderId(), portfolioOrderEntry.getTicker());
	    portfolioOrders.add(portfolioOrderEntry);
	}
    }

    private static Position dbToPortfolioPosition(ResultSet dbResult) throws SQLException {
	LOGGER.debug("Entering Portfolio.dbToPortfolioPosition(ResultSet dbResult)");
        Position positionFromDb = new Position();
        positionFromDb.setPositionId(dbResult.getInt("position_id"));
        positionFromDb.setOpen(true);
        positionFromDb.setTicker(dbResult.getString("ticker"));
        positionFromDb.setSecType(dbResult.getString("sec_type"));
	positionFromDb.setExpiry(new DateTime(dbResult.getLong("epoch_expiry"), DateTimeZone.forID("America/New_York")));
        positionFromDb.setEpochOpened(dbResult.getLong("epoch_opened"));
        positionFromDb.setLongPosition(dbResult.getBoolean("long_position"));
        positionFromDb.setNumberTransacted(dbResult.getInt("number_transacted"));
        positionFromDb.setPriceAtOpen(dbResult.getDouble("price_at_open"));
        positionFromDb.setCostBasis(dbResult.getDouble("cost_basis"));
	positionFromDb.setLastTick(dbResult.getDouble("last_tick"));
	positionFromDb.setNetAssetValue(dbResult.getDouble("net_asset_value"));
        return positionFromDb;
    }
    private static Order dbToPortfolioOrder(ResultSet dbResult) throws SQLException {
	LOGGER.debug("Entering Portfolio.dbToPortfolioOrder(ResultSet dbResult)");
        Order orderFromDb = new Order();
        orderFromDb.setOrderId(dbResult.getLong("order_id"));
	orderFromDb.setOpen(true);
        orderFromDb.setTicker(dbResult.getString("ticker"));
        orderFromDb.setLimitPrice(dbResult.getDouble("limit_price"));
        orderFromDb.setAction(dbResult.getString("action"));
	orderFromDb.setTotalQuantity(dbResult.getInt("total_quantity"));
        orderFromDb.setSecType(dbResult.getString("sec_type"));
        orderFromDb.setTif(dbResult.getString("tif"));
        orderFromDb.setEpochOpened(dbResult.getLong("epoch_opened"));
        return orderFromDb;
    }
    public String getName() {
	return name;
    }
    public double calculateNetAssetValue() {
	double openPositionNavs = 0.00;
	this.totalCash = calculateTotalCash();
	for(Position positionToUpdate : portfolioPositions) {
	    if(positionToUpdate.isOpen()) {
		openPositionNavs += positionToUpdate.calculateNetAssetValue();
	    }
	}
	netAssetValue = this.totalCash + openPositionNavs;
	return netAssetValue;
    }
    public double getFreeCash() {
	return freeCash;
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
    public int numberOfOpenStockLongs(Security security) { 
	int openLongCount = 0;
	
	for(Position portfolioPosition : portfolioPositions) {
	    if( (security.getTicker().equals(portfolioPosition.getTicker())) && portfolioPosition.isOpen() && 
		portfolioPosition.isLong() && (portfolioPosition.getSecType().equals("STOCK")) ) {
		openLongCount++;
	    }
	}
	LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenStockLongs(Security {})", openLongCount, security.getTicker());
	return openLongCount; 
    }
    public int numberOfOpenStockShorts(Security security) {
	int openShortCount = 0;
		
	for(Position portfolioPosition : portfolioPositions) {
	    if( (security.getTicker().equals(portfolioPosition.getTicker())) && portfolioPosition.isOpen() && 
		portfolioPosition.isShort() && (portfolioPosition.getSecType().equals("STOCK")) ) {
		openShortCount++;
	    }
	}
        LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenStockLongs(Security {})", openShortCount, security.getTicker());
	return openShortCount; 
    }
    public int numberOfOpenCallLongs(Security security) { 
	int openLongCount = 0;
		
	for(Position portfolioPosition : portfolioPositions) {
	    if( (security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) && portfolioPosition.isOpen() && 
		portfolioPosition.isLong() && (portfolioPosition.getSecType().equals("CALL")) ) {
		openLongCount++;
	    }
	}
        LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenStockLongs(Security {})", openLongCount, security.getTicker());
	return openLongCount; 
    }
    public int numberOfOpenCallShorts(Security security) {
	int openShortCount = 0;
		
	for(Position portfolioPosition : portfolioPositions) {
	    if( (security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) && portfolioPosition.isOpen() && 
		portfolioPosition.isShort() && (portfolioPosition.getSecType().equals("CALL")) ) {
		openShortCount++;
	    }
	}
        LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenStockLongs(Security {})", openShortCount, security.getTicker());
	return openShortCount; 
    }
    public int numberOfOpenPutLongs(Security security) { 
	int openLongCount = 0;
		
	for(Position portfolioPosition : portfolioPositions) {
	    if( (security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) && portfolioPosition.isOpen() && 
		portfolioPosition.isLong() && (portfolioPosition.getSecType().equals("PUT")) ) {
		openLongCount++;
	    }
	}
        LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenStockLongs(Security {})", openLongCount, security.getTicker());
	return openLongCount; 
    }
    public int numberOfOpenPutShorts(Security security) {
	int openShortCount = 0;
		
	for(Position portfolioPosition : portfolioPositions) {
	    if( (security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) && portfolioPosition.isOpen() && 
		portfolioPosition.isShort() && (portfolioPosition.getSecType().equals("PUT")) ) {
		openShortCount++;
	    }
	}
        LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenStockLongs(Security {})", openShortCount, security.getTicker());
	return openShortCount; 
    }
    public void addOrder(Order orderToAdd) throws InsufficientFundsException, SecurityNotFoundException {
	LOGGER.debug("Entering Portfolio.addOrder(Order {})", orderToAdd);

	double requiredCash = 0.00;
	if(orderToAdd.getAction().equals("SELL")) {
	    LOGGER.debug("orderToAdd.getAction().equals(\"SELL\")");
	    if(orderToAdd.getSecType().equals("PUT")) {
		LOGGER.debug("orderToAdd.getSecType().equals(\"PUT\")");
		requiredCash = orderToAdd.getStrikePrice() * orderToAdd.getTotalQuantity() * 100;
		LOGGER.debug("requiredCash = orderToAdd.getStrikePrice() ${} * orderToAdd.getTotalQuantity() ({}) * 100 = {}",
			     orderToAdd.getStrikePrice(), orderToAdd.getTotalQuantity(), requiredCash);
		if(freeCash < requiredCash) {
		    LOGGER.debug("freeCash {} < requiredCash {}", freeCash, requiredCash);
		    throw new InsufficientFundsException(orderToAdd.getTicker(), requiredCash, freeCash);
		}
	    } else if(orderToAdd.getSecType().equals("CALL")) {
		LOGGER.debug("orderToAdd.getSecType().equals(\"CALL\")");
		requiredCash = orderToAdd.getStrikePrice() * orderToAdd.getTotalQuantity() * 100;
		LOGGER.debug("requiredCash = orderToAdd.getStrikePrice() ${} * orderToAdd.getTotalQuantity() ({}) * 100 = {}",
                             orderToAdd.getStrikePrice(), orderToAdd.getTotalQuantity(), requiredCash);
		for(Position positionForCallCover : getListOfOpenPositions()) {
		    if(positionForCallCover.getTicker().equals(orderToAdd.getUnderlyingTicker()) &&
		       positionForCallCover.isLong()) {
			LOGGER.debug("Found open long stock position {} to write call against", positionForCallCover.getPositionId()); 
			requiredCash -= orderToAdd.getStrikePrice() * positionForCallCover.getNumberTransacted();
			LOGGER.debug("requiredCash -= orderToAdd.getStrikePrice() ${} * orderToAdd.getTotalQuantity() ({}) * 100 = {}",
				     orderToAdd.getStrikePrice(), orderToAdd.getTotalQuantity(), requiredCash);
		    }
		}
		for(Order competingOpenOrder : getListOfOpenOrders()) {
		    if(competingOpenOrder.getUnderlyingTicker().equals(orderToAdd.getUnderlyingTicker()) &&
		       competingOpenOrder.getAction().equals("SELL") &&
		       competingOpenOrder.getSecType().equals("CALL") ) {
			requiredCash += orderToAdd.getStrikePrice() * competingOpenOrder.getTotalQuantity() * 100;
		    }
		}
		if(requiredCash < 0.00) {
		    LOGGER.debug("requiredCash {} < $0.00, setting to $0.00", requiredCash);
		    requiredCash = 0.00;
		}
                if(freeCash < requiredCash) {
		    LOGGER.warn("freeCash {} < requiredCash {}", freeCash, requiredCash);
                    throw new InsufficientFundsException(orderToAdd.getTicker(), requiredCash, freeCash);
                }
	    } else { /* If neither PUT nor CALL must be a STOCK */
		LOGGER.debug("orderToAdd.getSecType().equals neither PUT nor CALL, so must be a STOCK");
		int shareCountNeededToCover = orderToAdd.getTotalQuantity();
		LOGGER.debug("shareCountNeededToCover = orderToAdd.getTotalQuantity() ({})", orderToAdd.getTotalQuantity());
		for(Position positionToSell : getListOfOpenPositions()) {
		    if(positionToSell.getTicker().equals(orderToAdd.getTicker())) {
			LOGGER.debug("Found position {} with getNumberTransacted() {} to match sell order against", 
				     positionToSell.getPositionId(), positionToSell.getNumberTransacted()); 
			shareCountNeededToCover -= positionToSell.getNumberTransacted();
		    }
		}
		if(shareCountNeededToCover > 0) {
		    LOGGER.warn("Still need {} shares in the portfolio to cover order", shareCountNeededToCover);
		    throw new SecurityNotFoundException(orderToAdd.getTicker());
		}
	    }
	} else { /* else orderToAdd.getAction().equals("BUY") */
	    requiredCash = orderToAdd.getLimitPrice() * orderToAdd.getTotalQuantity() * ((orderToAdd.getSecType().equals("STOCK")) ? 1 : 100);
	    LOGGER.debug("requiredCash = orderToAdd.getLimitPrice() {} * orderToAdd.getTotalQuantity() {} * {}", orderToAdd.getLimitPrice(),
			 orderToAdd.getTotalQuantity(), (orderToAdd.getSecType().equals("STOCK")) ? 1 : 100);
	    if(freeCash < requiredCash) {
		LOGGER.warn("freeCash {} < requiredCash {}", freeCash, requiredCash);
		throw new InsufficientFundsException(orderToAdd.getTicker(), requiredCash, freeCash);
	    }
	}
	reservedCash += requiredCash;
	freeCash -= requiredCash;
	LOGGER.info("reservedCash + requiredCash {} = {}, freeCash - requiredCash = {}", requiredCash, reservedCash, freeCash);
	portfolioOrders.add(orderToAdd);
	entryCount++;
	LOGGER.info("Added order id {} to portfolio {}", orderToAdd.getOrderId(), name);
	try {
	    insertDbOrder(orderToAdd);
	} catch(SQLException sqle) {
	    LOGGER.warn("Unable to add order {} to DB", orderToAdd.getOrderId());
	    LOGGER.debug("Caught (SQLException sqle)", sqle);
	}
    }
    public void addNewPosition(Position position) {
	LOGGER.debug("Entering Portfolio.addNewPosition(Position {})", position.getPositionId());
	portfolioPositions.add(position);
        try {
            insertDbPosition(position);
        } catch(SQLException sqle) {
            LOGGER.warn("Unable to add position {} to DB", position.getPositionId());
            LOGGER.debug("Caught (SQLException sqle)", sqle);
        }
	entryCount++;
	LOGGER.debug("entryCount incremented to {}", entryCount);
    }
    public List<Order> getListOfOpenOrders() {
	LOGGER.debug("Entering Portfolio.getListOfOpenOrders()");
	List<Order> openOrderList = new ArrayList<>();
	for(Order portfolioOrder : portfolioOrders) {
	    if(portfolioOrder.isOpen()) {
		openOrderList.add(portfolioOrder);
	    }
	}
	LOGGER.debug("Returning {}", Arrays.toString(openOrderList.toArray()) );
     	return openOrderList;
    }
    public List<Position> getListOfOpenPositions() {
	LOGGER.debug("Entering Portfolio.getListOfOpenPositions()");
	List<Position> openPositionList = new ArrayList<>();
	for(Position portfolioPosition : portfolioPositions) {
	    if(portfolioPosition.isOpen()) {
		openPositionList.add(portfolioPosition);
	    }
	}
	LOGGER.debug("Returning {}", Arrays.toString(openPositionList.toArray()));
	return openPositionList;
    }
    public List<Position> getListOfOpenOptionPositions() {
	LOGGER.debug("Entering Portfolio.getListOfOpenOptionPositions()");
        List<Position> openOptionPositionList = new ArrayList<>();
        for(Position portfolioPosition : portfolioPositions) {
            if(portfolioPosition.isOpen() && 
	       ( (portfolioPosition.getSecType().equals("CALL")) || 
		 (portfolioPosition.getSecType().equals("PUT")) ) ) {
                openOptionPositionList.add(portfolioPosition);
            }
        }
	LOGGER.debug("Returning {}", openOptionPositionList.toString());
        return openOptionPositionList;
    }
    public void fillOrder(Order orderToFill, double fillPrice) {
	LOGGER.debug("Entering Portfolio.fillOrder(Order {}, double {})", orderToFill.getOrderId(), fillPrice);
	boolean orderIsLong = orderToFill.getAction().equals("BUY");
	boolean orderIsStock = orderToFill.getSecType().equals("STOCK");
	LOGGER.debug("orderIsLong = {}, orderIsStock = {}", orderIsLong, orderIsStock);
	double costBasis = fillPrice * orderToFill.getTotalQuantity() * (orderIsStock ? 1.0 : 100.0) * (orderIsLong ? 1.0 : -1.0);
	LOGGER.debug("costBasis {} = fillPrice {} * orderToFill.getTotalQuantity() {} * (orderIsStock ? 1.0 : 100.0) {} * (orderIsLong ? 1.0 : -1.0) {}",
		     costBasis, fillPrice, orderToFill.getTotalQuantity(), (orderIsStock ? 1.0 : 100.0),  (orderIsLong ? 1.0 : -1.0));
	addNewPosition(new Position(orderToFill, fillPrice));
	LOGGER.debug("Decrementing freeCash ${} by costBasis ${}", freeCash, costBasis);
	freeCash -= costBasis;
	if(orderIsStock) {
	    LOGGER.debug("freeCash = ${}", freeCash);
	    /* TODO : Finish this stub */
	}
	totalCash = freeCash + reservedCash;
	orderToFill.fill(fillPrice);
        try {
            updateDbOrder(orderToFill);
        } catch(SQLException sqle) {
            LOGGER.warn("Unable to update filled order {} in DB", orderToFill.getOrderId());
            LOGGER.debug("Caught (SQLException sqle)", sqle);
        }
    }
    void exerciseOption(Position optionPositionToExercise) {
	/* If short put
	 * buy the stock at the strike price
	 * if short call
	 * find a position in the stock to sell at strike price
	 * if long an option
	 * stub out as TODO later as current algo only sells options
	 */
	LOGGER.debug("Entering Portfolio.exerciseOption(Position {})", optionPositionToExercise.getPositionId());
	if(!optionPositionToExercise.isLong()) {
	    if(optionPositionToExercise.getSecType().equals("PUT")) {
		Position optionToStockPosition = Position.exerciseOptionPosition(optionPositionToExercise);
		addNewPosition(optionToStockPosition);
		optionPositionToExercise.close(0.00);
		reservedCash -= optionToStockPosition.getStrikePrice() * optionToStockPosition.getNumberTransacted() * 100.0;
	    } else {
		/* optionPositionToExercise.getSecType.equals("CALL") */
		Position calledAwayStockPosition = findStockPositionToDeliver(optionPositionToExercise.getUnderlyingTicker());
		if(calledAwayStockPosition != null) {
		    calledAwayStockPosition.close(optionPositionToExercise.getStrikePrice());
		    freeCash += optionPositionToExercise.getStrikePrice() * calledAwayStockPosition.getNumberTransacted() * 100.0;
		} else {
                    /* Buy the stock at market price and deliver it */
		    Position buyStockToDeliverPosition = Position.exerciseOptionPosition(optionPositionToExercise);
		    buyStockToDeliverPosition.close(optionPositionToExercise.getStrikePrice());
		}
	    }
	} else {
	    /* Call a stock away from or put a stock to someone */
	}
    }

    Position findStockPositionToDeliver(String tickerToDeliver) {
	LOGGER.debug("Entering Portfolio.findStockPositionToDeliver(String {})", tickerToDeliver);
	double lowestCostBasis = Double.MAX_VALUE;
	Position positionToDeliver = null;
	for(Position openPosition : getListOfOpenPositions()) {
	    if(openPosition.getTicker().equals(tickerToDeliver) && (openPosition.getCostBasis() < lowestCostBasis)) {
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
	PreparedStatement orderSqlStatement = null;
	ResultSet orderResultSet = null;
	try {
            LOGGER.info("Getting connection to {}", GaussTrader.DB_NAME);
            dbConnection = dataSource.getConnection();
	    summarySqlStatement = dbConnection.prepareStatement("SELECT * FROM portfolios WHERE name = ?");
	    summarySqlStatement.setString(1, name);
	    LOGGER.debug("Executing SELECT * FROM portfolios WHERE name = {}", name);
	    summaryResultSet = summarySqlStatement.executeQuery();
	    if(summaryResultSet.next()) {
		LOGGER.debug("summaryResultSet.next() == true, portfolio \"{}\" exists in database. Running updates instead of inserts", name);
		updateDbSummary();
	    } else {
		LOGGER.debug("summaryResultSet.next() == false, inserting portfolio \"{}\" newly into database", name);
		insertDbSummary();
	    }
	    positionSqlStatement = dbConnection.prepareStatement("SELECT * FROM positions WHERE portfolio = ? AND position_id = ?");
	    positionSqlStatement.setString(1, name);
	    for(Position portfolioPosition : portfolioPositions) {
		positionSqlStatement.setLong(2, portfolioPosition.getPositionId());
		LOGGER.debug("Executing SELECT * FROM positions WHERE portfolio = {} AND position_id = {}", name, portfolioPosition.getPositionId());
		positionResultSet = positionSqlStatement.executeQuery();
		if(positionResultSet.next()) {
		    LOGGER.debug("positionResultSet.next() == true, position {} exists in database. Running update instead of insert", portfolioPosition.getPositionId());
		    updateDbPosition(portfolioPosition);
		} else {
                    LOGGER.debug("positionResultSet.next() == false, inserting position {} newly into database", portfolioPosition.getPositionId());
		    insertDbPosition(portfolioPosition);
		}
	    }
	    orderSqlStatement = dbConnection.prepareStatement("SELECT * FROM orders WHERE portfolio = ? AND order_id = ?");
	    orderSqlStatement.setString(1, name);
	    for(Order portfolioOrder : portfolioOrders) {
		orderSqlStatement.setLong(2, portfolioOrder.getOrderId());
		LOGGER.debug("Executing SELECT * FROM orders WHERE portfolio = {} AND order_id = {}", name, portfolioOrder.getOrderId());
		orderResultSet = orderSqlStatement.executeQuery(); 
		if(orderSqlStatement.execute()) {
                    LOGGER.debug("orderResultSet.next() == true, order {} exists in database. Running update instead of insert", portfolioOrder.getOrderId());
		    updateDbOrder(portfolioOrder);
		} else {
                    LOGGER.debug("orderResultSet.next() == false, inserting position {} newly into database", portfolioOrder.getOrderId());
		    insertDbOrder(portfolioOrder);
		}
	    }
	} catch(SQLException sqle) {
	    LOGGER.info("Unable to get connection to {}", GaussTrader.DB_NAME);
	    LOGGER.debug("Caught (SQLException sqle) ", sqle);
	} 
	/*
	finally {
	    if(dbConnection != null) {
		try {
		    dbConnection.close();
		} catch (SQLException sqle) {
		    LOGGER.info("SQLException trying to close {}", GaussTrader.DB_NAME);
		    LOGGER.debug("Caught (SQLException sqle) ", sqle);
		}
	    }
	}
	*/
    }
    private void updateDbSummary() throws SQLException {
	LOGGER.debug("Entering Portfolio.updateDbSummary()");
	String sqlString = new String("UPDATE portfolios SET net_asset_value = ?, free_cash = ?, reserved_cash = ?, total_cash = ? WHERE name = ?");
        PreparedStatement updateSummarySqlStatement = null;
        int updatedRowCount;
        updateSummarySqlStatement = dbConnection.prepareStatement(sqlString);
        updateSummarySqlStatement.setDouble(1, netAssetValue);
        updateSummarySqlStatement.setDouble(2, freeCash);
        updateSummarySqlStatement.setDouble(3, reservedCash);
	updateSummarySqlStatement.setDouble(4, totalCash);
	updateSummarySqlStatement.setString(5, name);
	LOGGER.debug("Executing UPDATE portfolios SET net_asset_value = {}, free_cash = {}, reserved_cash = {}, total_cash = {} WHERE name = {}", 
		     netAssetValue, freeCash, reservedCash, totalCash, name);
        if( (updatedRowCount = updateSummarySqlStatement.executeUpdate()) != 1) {
            LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
        }
    }

    private void insertDbSummary() throws SQLException {
	LOGGER.debug("Entering Portfolio.insertDbSummary()");
        String sqlString = new String("INSERT INTO portfolios (name, net_asset_value, free_cash, reserved_cash, total_cash) VALUES (?, ?, ?, ?, ?)");
        PreparedStatement newSummarySqlStatement = null;
        int insertedRowCount;
        newSummarySqlStatement = dbConnection.prepareStatement(sqlString);
        newSummarySqlStatement.setString(1, name);
        newSummarySqlStatement.setDouble(2, netAssetValue);
        newSummarySqlStatement.setDouble(3, freeCash);
        newSummarySqlStatement.setDouble(4, reservedCash);
        newSummarySqlStatement.setDouble(5, totalCash);
	LOGGER.debug("Executing INSERT INTO portfolios (name, net_asset_value, free_cash, reserved_cash, total_cash) VALUES ({}, {}, {}, {}, {})",
		     name, netAssetValue, freeCash, reservedCash, totalCash);
        if( (insertedRowCount = newSummarySqlStatement.executeUpdate()) != 1) {
            LOGGER.warn("Inserted {} rows. Should have inserted 1 row", insertedRowCount);
        }
    }
    
    private void updateDbPosition(Position portfolioPosition) throws SQLException {
	LOGGER.debug("Entering Portfolio.updateDbPosition(Position {})", portfolioPosition.getPositionId());
	String sqlString = new String("UPDATE positions SET last_tick = ?, net_asset_value = ? WHERE position_id = ?");
	PreparedStatement updatePositionSqlStatement = null;
	int updatedRowCount;
	updatePositionSqlStatement = dbConnection.prepareStatement(sqlString);
	updatePositionSqlStatement.setDouble(1, portfolioPosition.getLastTick());
	updatePositionSqlStatement.setDouble(2, portfolioPosition.calculateNetAssetValue());
	updatePositionSqlStatement.setLong(3, portfolioPosition.getPositionId());
	LOGGER.debug("Executing UPDATE positions SET last_tick = {}, net_asset_value = {} WHERE position_id = {}", 
		     portfolioPosition.getLastTick(), portfolioPosition.calculateNetAssetValue(), portfolioPosition.getPositionId());
	if( (updatedRowCount = updatePositionSqlStatement.executeUpdate()) != 1) {
	    LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
	}
	if(!portfolioPosition.isOpen()) {
	    sqlString = "UPDATE positions SET epoch_closed = ?, price_at_close = ?, profit = ?, open = 'false' WHERE position_id = ?";
	    updatePositionSqlStatement = dbConnection.prepareStatement(sqlString);
	    updatePositionSqlStatement.setLong(1, portfolioPosition.getEpochClosed());
	    updatePositionSqlStatement.setDouble(2, portfolioPosition.getPriceAtClose());
	    updatePositionSqlStatement.setDouble(3, portfolioPosition.getProfit());
	    updatePositionSqlStatement.setLong(4, portfolioPosition.getPositionId());
	    LOGGER.debug("Executing UPDATE positions SET epoch_closed = {}, price_at_close = {}, profit = {}, open = 'false' WHERE position_id = {}",
			 portfolioPosition.getEpochClosed(), portfolioPosition.getPriceAtClose(), portfolioPosition.getProfit(), portfolioPosition.getPositionId());
	}
	if( (updatedRowCount = updatePositionSqlStatement.executeUpdate()) != 1) {
	    LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
	}
    }
    private void insertDbPosition(Position portfolioPosition) throws SQLException {
	LOGGER.debug("Entering Portfolio.insertDbPosition(Position {})", portfolioPosition.getPositionId());
	String sqlString = new String("INSERT INTO positions (portfolio, position_id, open, ticker, sec_type, epoch_expiry, " +
				      "underlying_ticker, strike_price, epoch_opened, long_position, number_transacted, price_at_open, " + 
				      "cost_basis, last_tick, net_asset_value) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	PreparedStatement newPositionSqlStatement = null;
	int insertedRowCount;
	int updatedRowCount;
	newPositionSqlStatement = dbConnection.prepareStatement(sqlString);
	newPositionSqlStatement.setString(1, name);
	newPositionSqlStatement.setLong(2, portfolioPosition.getPositionId());
	newPositionSqlStatement.setBoolean(3, portfolioPosition.isOpen());
	newPositionSqlStatement.setString(4, portfolioPosition.getTicker());
	newPositionSqlStatement.setString(5, portfolioPosition.getSecType());
	newPositionSqlStatement.setLong(6, portfolioPosition.getExpiry().getMillis());
	newPositionSqlStatement.setString(7, portfolioPosition.getTicker());
	newPositionSqlStatement.setDouble(8, portfolioPosition.getStrikePrice());
	newPositionSqlStatement.setLong(9, portfolioPosition.getEpochOpened());
	newPositionSqlStatement.setBoolean(10, portfolioPosition.isLong());
	newPositionSqlStatement.setInt(11, portfolioPosition.getNumberTransacted());
	newPositionSqlStatement.setDouble(12, portfolioPosition.getPriceAtOpen());
	newPositionSqlStatement.setDouble(13, portfolioPosition.getCostBasis());
	newPositionSqlStatement.setDouble(14, portfolioPosition.getLastTick());
	newPositionSqlStatement.setDouble(15, portfolioPosition.calculateNetAssetValue());
	LOGGER.debug("Executing INSERT INTO positions (portfolio, position_id, open, ticker, sec_type, epoch_expiry, underlying_ticker, strike_price, epoch_opened, " + 
		     "long_position, number_transacted, price_at_open, cost_basis, last_tick, net_asset_value) " + 
		     "VALUES ({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})", 
		     name, portfolioPosition.getPositionId(), portfolioPosition.isOpen(), portfolioPosition.getTicker(), portfolioPosition.getSecType(), 
		     portfolioPosition.getExpiry().getMillis(), portfolioPosition.getTicker(), portfolioPosition.getStrikePrice(), portfolioPosition.getEpochOpened(), 
		     portfolioPosition.isLong(), portfolioPosition.getNumberTransacted(), portfolioPosition.getPriceAtOpen(), portfolioPosition.getCostBasis(), 
		     portfolioPosition.getLastTick(), portfolioPosition.calculateNetAssetValue());
	if( (insertedRowCount = newPositionSqlStatement.executeUpdate()) != 1) {
	    LOGGER.warn("Inserted {} rows. Should have inserted 1 row", insertedRowCount);
	}
	if(!portfolioPosition.isOpen()) {
	    sqlString = "UPDATE positions SET epoch_closed = ?, price_at_close = ?, profit = ?, open = 'false' WHERE position_id = ?";
	    newPositionSqlStatement = dbConnection.prepareStatement(sqlString);
	    newPositionSqlStatement.setLong(1, portfolioPosition.getEpochClosed());
	    newPositionSqlStatement.setDouble(2, portfolioPosition.getPriceAtClose());
	    newPositionSqlStatement.setDouble(3, portfolioPosition.getProfit());
	    newPositionSqlStatement.setLong(4, portfolioPosition.getPositionId());
	    LOGGER.debug("Executing UPDATE positions SET epoch_closed = {}, price_at_close = {}, profit = {}, open = 'false' WHERE position_id = {}",
			 portfolioPosition.getEpochClosed(), portfolioPosition.getPriceAtClose(), portfolioPosition.getProfit(), portfolioPosition.getPositionId());
	}
	if( (updatedRowCount = newPositionSqlStatement.executeUpdate()) != 1) {
	    LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
	}
    }
    private void updateDbOrder(Order portfolioOrder) throws SQLException {
	/* Nothing changes in an order unless it is filled (i.e. closed) */
	LOGGER.debug("Entering Portfolio.updateDbOrder(Order {})", portfolioOrder.getOrderId());
	if(!portfolioOrder.isOpen()) {
	    String sqlString = new String("UPDATE orders SET epoch_closed = ?, close_reason = ?, fill_price = ? WHERE order_id = ?");
	    PreparedStatement updateOrderSqlStatement = null;
	    int updatedRowCount;
	    updateOrderSqlStatement = dbConnection.prepareStatement(sqlString);
	    updateOrderSqlStatement.setLong(1, portfolioOrder.getEpochClosed());
	    updateOrderSqlStatement.setString(2, portfolioOrder.getCloseReason());
	    updateOrderSqlStatement.setDouble(3, portfolioOrder.getFillPrice());
	    updateOrderSqlStatement.setLong(4, portfolioOrder.getOrderId());
	    LOGGER.debug("Executing UPDATE orders SET epoch_closed = {}, close_reason = {}, fill_price = {} WHERE order_id = {}",
			 portfolioOrder.getEpochClosed(), portfolioOrder.getCloseReason(), portfolioOrder.getFillPrice(), portfolioOrder.getOrderId());
	    if( (updatedRowCount = updateOrderSqlStatement.executeUpdate()) != 1) {
		LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
	    }
	}
    }
    private void insertDbOrder(Order portfolioOrder) throws SQLException {
	LOGGER.debug("Entering Portfolio.insertDbOrder(Order {})", portfolioOrder.getOrderId());
	String sqlString = new String("INSERT INTO orders (portfolio, order_id, open, ticker, epoch_expiry, underlying_ticker, strike_price, limit_price, " +
				      "action, total_quantity, sec_type, tif, epoch_opened) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	PreparedStatement newOrderSqlStatement = null;
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
	LOGGER.debug("Executing INSERT INTO orders (portfolio, order_id, open, ticker, epoch_expiry, underlying_ticker, strike_price, limit_price, " +
		     "action, total_quantity, sec_type, tif, epoch_opened) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
		     name, portfolioOrder.getOrderId(), portfolioOrder.isOpen(), portfolioOrder.getTicker(), portfolioOrder.getExpiry().getMillis(), 
		     portfolioOrder.getUnderlyingTicker(), portfolioOrder.getStrikePrice(), portfolioOrder.getLimitPrice(), portfolioOrder.getAction(),
		     portfolioOrder.getTotalQuantity(), portfolioOrder.getSecType(), portfolioOrder.getTif(), portfolioOrder.getEpochOpened());
	if( (insertedRowCount = newOrderSqlStatement.executeUpdate()) != 1) {
	    LOGGER.warn("Inserted {} rows. Should have inserted 1 row", insertedRowCount);
	}
	if(!portfolioOrder.isOpen()) {
	    sqlString = "UPDATE orders SET epoch_closed = ?, close_reason = ?, fill_price = ?, open = 'false' WHERE order_id = ?";
	    newOrderSqlStatement = dbConnection.prepareStatement(sqlString);
	    newOrderSqlStatement.setLong(1, portfolioOrder.getEpochClosed());
	    newOrderSqlStatement.setString(2, portfolioOrder.getCloseReason());
	    newOrderSqlStatement.setDouble(3, portfolioOrder.getFillPrice());
	    newOrderSqlStatement.setLong(4, portfolioOrder.getOrderId());
	    LOGGER.debug("Executing UPDATE orders SET epoch_closed = {}, close_reason = {}, fill_price = {}, open = 'false' WHERE order_id = {}",
			 portfolioOrder.getEpochClosed(), portfolioOrder.getCloseReason(), portfolioOrder.getFillPrice(), portfolioOrder.getOrderId());
	}
	if( (updatedRowCount = newOrderSqlStatement.executeUpdate()) != 1) {
	    LOGGER.warn("Updated {} rows. Should have updated 1 row", updatedRowCount);
	}
    }

    public static void main(String[] args) {
	LOGGER.info("Generating new portfolio");
	//		Portfolio p = new Portfolio("Test");
	Portfolio p = new Portfolio();
    }
}
