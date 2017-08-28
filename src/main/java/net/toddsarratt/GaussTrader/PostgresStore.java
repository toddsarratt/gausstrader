package net.toddsarratt.GaussTrader;

import net.toddsarratt.GaussTrader.Order.Order;
import net.toddsarratt.GaussTrader.Order.OrderBuilder;
import net.toddsarratt.GaussTrader.Security.SecurityType;
import net.toddsarratt.GaussTrader.Security.Stock;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * The {@code PostgresStore} class implements the DataStore interface
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.2
 */
public class PostgresStore implements DataStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(PostgresStore.class);
	private static PGSimpleDataSource pgDataSource = new PGSimpleDataSource();

	/**
	 * Set up Postgres database connection
	 */
	static {
		LOGGER.debug("Entering connect()");
		LOGGER.debug("pgDataSource.setServerName({})", Constants.DB_IP);
		pgDataSource.setServerName(Constants.DB_IP);
		LOGGER.debug("pgDataSource.setDatabaseName({})", Constants.DB_NAME);
		pgDataSource.setDatabaseName(Constants.DB_NAME);
		LOGGER.debug("pgDataSource.setUser({})", Constants.DB_USER);
		pgDataSource.setUser(Constants.DB_USER);
		LOGGER.debug("pgDataSource.setPassword({})", Constants.DB_PASSWORD);
		pgDataSource.setPassword(Constants.DB_PASSWORD);
	}

	public static Position dbToPortfolioPosition(ResultSet dbResult) throws SQLException {
		LOGGER.debug("Entering Portfolio.dbToPortfolioPosition(ResultSet dbResult)");
		PositionBuilder positionBuilder = new PositionBuilder();
		positionBuilder.positionId(new TransactionId(dbResult.getLong("position_id")))
				.open(dbResult.getBoolean("open"))
				.ticker(dbResult.getString("ticker"))
				.securityType(SecurityType.of(dbResult.getString("sec_type")))
				.underlyingTicker(dbResult.getString("underlying_ticker"))
				.strikePrice(BigDecimal.valueOf(dbResult.getDouble("strike_price")))
				.instantOpened(Instant.ofEpochMilli(dbResult.getLong("epoch_opened")))
				.longPosition(dbResult.getBoolean("long_position"))
				.numberTransacted(dbResult.getInt("number_transacted"))
				.priceAtOpen(BigDecimal.valueOf(dbResult.getDouble("price_at_open")))
				.costBasis(BigDecimal.valueOf(dbResult.getDouble("cost_basis")))
				.price(BigDecimal.valueOf(dbResult.getDouble("last_tick")))
				.netAssetValue(BigDecimal.valueOf(dbResult.getDouble("net_asset_value")))
				// TODO: Add field "expiry" to take the place of "epoch_expiry" in database
				.expiry(dbResult.getDate("expiry").toLocalDate())
				.claimAgainstCash(BigDecimal.valueOf(dbResult.getDouble("claim_against_cash")))
				.originatingOrderId(new TransactionId(dbResult.getLong("originating_order_id")));
		return positionBuilder.build();
	}

	public static Order dbToPortfolioOrder(ResultSet dbResult) throws SQLException {
		LOGGER.debug("Entering Portfolio.dbToPortfolioOrder(ResultSet dbResult)");
		SecurityType secType = SecurityType.of(dbResult.getString("sec_type"));
		OrderBuilder orderBuilder = OrderBuilder.of(secType);
		orderBuilder.orderId(dbResult.getLong("order_id"))
				.open(true)
				.ticker(dbResult.getString("ticker"))
				.underlyingTicker(dbResult.getString("underlying_ticker"))
				.strikePrice(dbResult.getDouble("strike_price"))
				.limitPrice(dbResult.getDouble("limit_price"))
				.action(dbResult.getString("action"))
				.totalQuantity(dbResult.getInt("total_quantity"))
				.secType(secType)
				.tif(dbResult.getString("tif"))
				.epochOpened(dbResult.getLong("epoch_opened"))
				.expiry(dbResult.getLong("epoch_expiry"))
				.claimAgainstCash(dbResult.getDouble("claim_against_cash"));
		return orderBuilder.build();
	}

	/**
	 * Writes the last known price of a stock to the database
	 *
	 * @param stock stock whose price needs to be written to the database
	 */
	@Override
	public void updateStockPriceToStorage(Stock stock) {
		String ticker = stock.getTicker();
		LOGGER.debug("Entering updateStockPriceToStorage(Stock {})", ticker);
		String sqlCommand = "UPDATE watchlist SET last_tick = ?, last_tick_epoch = ? WHERE ticker = ?";
		PreparedStatement sqlStatement;
		int insertedRowCount;
		try {
			LOGGER.debug("Getting connection to {}", Constants.DB_NAME);
			LOGGER.debug("Inserting current stock price for ticker {} into database.", ticker);
			Connection dbConnection = pgDataSource.getConnection();
			sqlStatement = dbConnection.prepareStatement(sqlCommand);
			BigDecimal currentPrice = stock.getPrice();
			long lastTickEpoch = stock.getLastPriceUpdateEpoch();
			sqlStatement.setDouble(1, currentPrice.doubleValue());
			sqlStatement.setLong(2, lastTickEpoch);
			sqlStatement.setString(3, ticker);
			LOGGER.debug("Executing UPDATE watchlist SET last_tick = {}, last_tick_epoch = {} WHERE ticker = {}", currentPrice, lastTickEpoch, ticker);
			if ((insertedRowCount = sqlStatement.executeUpdate()) != 1) {
				LOGGER.warn("Inserted {} rows. Should have inserted 1 row.", insertedRowCount);
			}
			dbConnection.close();
		} catch (SQLException sqle) {
			LOGGER.info("Unable to get connection to {}", Constants.DB_NAME);
			LOGGER.debug("Caught (SQLException sqle)", sqle);
		}
	}

	/**
	 * TODO : I'm not sure what problem this solves
	 */
	@Override
	public void resetWatchList() {
		PreparedStatement sqlStatement;
		try {
			LOGGER.debug("Getting connection to {}", Constants.DB_NAME);
			Connection dbConnection = pgDataSource.getConnection();
			sqlStatement = dbConnection.prepareStatement("UPDATE watchlist SET active = FALSE");
			LOGGER.debug("Executing UPDATE watchlist SET active = FALSE");
			sqlStatement.executeUpdate();
			dbConnection.close();
		} catch (SQLException sqle) {
			LOGGER.info("Unable to get connection to {}", Constants.DB_NAME);
			LOGGER.debug("Caught (SQLException sqle)", sqle);
		}
	}

	@Override
	public void writeStockMetrics(Stock stockToUpdate) {
		LOGGER.debug("Entering WatchList.updateDb(Stock {})", stockToUpdate.getTicker());
		PreparedStatement sqlUniquenessStatement;
		PreparedStatement sqlUpdateStatement;
		try {
			LOGGER.debug("Getting connection to {}", Constants.DB_NAME);
			Connection dbConnection = pgDataSource.getConnection();
			String ticker = stockToUpdate.getTicker();
			sqlUniquenessStatement = dbConnection.prepareStatement("SELECT DISTINCT ticker FROM watchlist WHERE ticker = ?");
			sqlUniquenessStatement.setString(1, ticker);
			LOGGER.debug("Executing SELECT DISTINCT ticker FROM watchlist WHERE ticker = {}", ticker);
			ResultSet tickerInDbResultSet = sqlUniquenessStatement.executeQuery();
			if (tickerInDbResultSet.next()) {
				sqlUpdateStatement = dbConnection.prepareStatement("UPDATE watchlist SET twenty_dma = ?, first_low_boll = ?, first_high_boll = ?, " +
						"second_low_boll = ?, second_high_boll = ?, last_tick = ?, last_tick_epoch = ?, active = TRUE WHERE ticker = ?");
			} else {
				sqlUpdateStatement = dbConnection.prepareStatement("INSERT INTO watchlist (twenty_dma, first_low_boll, first_high_boll, " +
						"second_low_boll, second_high_boll, last_tick, last_tick_epoch, active, ticker) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, ?)");
			}
			sqlUpdateStatement.setDouble(1, stockToUpdate.getTwentyDma().doubleValue());
			sqlUpdateStatement.setDouble(2, stockToUpdate.getBollingerBand(3).doubleValue());
			sqlUpdateStatement.setDouble(3, stockToUpdate.getBollingerBand(1).doubleValue());
			sqlUpdateStatement.setDouble(4, stockToUpdate.getBollingerBand(4).doubleValue());
			sqlUpdateStatement.setDouble(5, stockToUpdate.getBollingerBand(2).doubleValue());
			sqlUpdateStatement.setDouble(6, stockToUpdate.getPrice().doubleValue());
			sqlUpdateStatement.setLong(7, stockToUpdate.getLastPriceUpdateEpoch());
			sqlUpdateStatement.setString(8, stockToUpdate.getTicker());
			LOGGER.debug("Executing SQL insert into watchlist table");
			sqlUpdateStatement.executeUpdate();
			dbConnection.close();
		} catch (SQLException sqle) {
			LOGGER.info("SQLException attempting to update DB table watchlist for {}", stockToUpdate.getTicker());
			LOGGER.debug("Exception", sqle);
		}
	}

	@Override
	public void writeStockMetrics(Set<Stock> stockSet) {
		stockSet.stream().forEach(this::writeStockMetrics);
	}

	@Override
	public boolean tickerPriceInStore(String ticker) {
		LOGGER.debug("Entering DBHistoricalPrices.tickerPriceInDb()");
		Connection dbConnection;
		try {
			dbConnection = pgDataSource.getConnection();
			PreparedStatement summarySqlStatement = dbConnection.prepareStatement("SELECT DISTINCT ticker FROM prices WHERE ticker = ?");
			summarySqlStatement.setString(1, ticker);
			LOGGER.debug("Executing SELECT DISTINCT ticker FROM prices WHERE ticker = {}", ticker);
			ResultSet tickerInDbResultSet = summarySqlStatement.executeQuery();
			dbConnection.close();
			return (tickerInDbResultSet.next());
		} catch (SQLException sqle) {
			LOGGER.info("SQLException attempting to find historical price for {}", ticker);
			LOGGER.debug("Exception", sqle);
		}
		return false;
	}

	@Override
	public void deactivateStock(String tickerToRemove) {
		PreparedStatement sqlUpdateStatement;
		try {
			LOGGER.debug("Getting connection to {}", Constants.DB_NAME);
			Connection dbConnection = pgDataSource.getConnection();
			sqlUpdateStatement = dbConnection.prepareStatement("UPDATE watchlist SET active = FALSE where ticker = ?");
			sqlUpdateStatement.setString(1, tickerToRemove);
			sqlUpdateStatement.executeUpdate();
			dbConnection.close();
		} catch (SQLException sqle) {
			LOGGER.info("SQLException attempting to update DB table watchlist for {}", tickerToRemove);
			LOGGER.debug("Exception", sqle);
		}
	}

	@Override
	public Set<Position> getPortfolioPositions() throws SQLException {
		LOGGER.debug("Entering Portfolio.getDbPortfolioPositions()");
		Connection dbConnection = pgDataSource.getConnection();
		Position portfolioPositionEntry;
		PreparedStatement positionSqlStatement = dbConnection.prepareStatement("SELECT * FROM positions WHERE portfolio = ? AND open = true");
		positionSqlStatement.setString(1, name);
		LOGGER.debug("Executing SELECT * FROM positions WHERE portfolio = {} AND open = true", name);
		ResultSet openPositionsResultSet = positionSqlStatement.executeQuery();
		while (openPositionsResultSet.next()) {
			portfolioPositionEntry = dbToPortfolioPosition(openPositionsResultSet);
			if (portfolioPositionEntry.isExpired()) {
				LOGGER.warn("Position {} read from the database has expired", portfolioPositionEntry.getTicker());
				reconcileExpiredOptionPosition(portfolioPositionEntry);
			} else {
				LOGGER.debug("Adding {} {}", portfolioPositionEntry.getPositionId(), portfolioPositionEntry.getTicker());
				portfolioPositions.add(portfolioPositionEntry);
			}
		}
		dbConnection.close();
	}

	@Override
	public void write(Order order) {
		try {
			throw new SQLException();
		} catch (SQLException sqle) {
			LOGGER.warn("Unable to add order {} to DB", order.getOrderId());
			LOGGER.debug("Caught (SQLException sqle)", sqle);
		}
	}

	@Override
	public void write(Position position) {
		try {
			throw new SQLException();
		} catch (SQLException sqle) {
			LOGGER.warn("Unable to add position {} to DB", position.getPositionId());
			LOGGER.debug("Caught (SQLException sqle)", sqle);
		}
	}

	@Override
	public void write(PortfolioSummary summary) {
		if (portfolioInDb()) {
			LOGGER.debug("Portfolio \"{}\" exists in database. Running updates instead of inserts", name);
			updateDbSummary();
		} else {
			LOGGER.debug("Inserting portfolio \"{}\" newly into database", name);
			insertDbSummary();
		}
	}

	@Override
	public void close(Position positionToClose) throws SQLException {
		int updatedRowCount;
		Connection dbConnection = pgDataSource.getConnection();
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

	@Override
	public LinkedHashMap<Long, BigDecimal> readHistoricalPrices(String ticker, DateTime earliestCloseDate) {
		LOGGER.debug("Entering getDBPrices(String {}, DateTime {} ({})",
				ticker, earliestCloseDate.getMillis(), earliestCloseDate.toString());
		LinkedHashMap<Long, BigDecimal> queriedPrices = new LinkedHashMap<>();
		int pricesNeeded = Constants.BOLL_BAND_PERIOD;
		int dbPricesFound = 0;
		try {
			LOGGER.debug("Getting connection to {}", Constants.DB_NAME);
			Connection dbConnection = pgDataSource.getConnection();
			PreparedStatement sqlStatement = dbConnection.prepareStatement("SELECT * FROM prices WHERE ticker = ? AND close_epoch >= ?");
			sqlStatement.setString(1, ticker);
			sqlStatement.setLong(2, earliestCloseDate.getMillis());
			LOGGER.debug("SELECT * FROM prices WHERE ticker = {} AND close_epoch >= {}", ticker, earliestCloseDate.getMillis());
			ResultSet historicalPriceResultSet = sqlStatement.executeQuery();
			while (historicalPriceResultSet.next() && (dbPricesFound < pricesNeeded)) {
				long closeEpoch = historicalPriceResultSet.getLong("close_epoch");
				double adjClose = historicalPriceResultSet.getDouble("adj_close");
				LOGGER.debug("Adding {}, {}", closeEpoch, adjClose);
				queriedPrices.put(closeEpoch, BigDecimal.valueOf(adjClose));
				dbPricesFound++;
			}
			dbConnection.close();
		} catch (SQLException sqle) {
			LOGGER.info("Unable to get connection to {}", Constants.DB_NAME);
			LOGGER.debug("Caught (SQLException sqle)", sqle);
		}
		LOGGER.debug("Found {} prices in db", dbPricesFound);
		LOGGER.debug("Returning from database LinkedHashMap with size {}", queriedPrices.size());
		if (dbPricesFound != queriedPrices.size()) {
			LOGGER.warn("Mismatch between prices found {} and number returned {}, possible duplication?", dbPricesFound, queriedPrices.size());
		}
		return queriedPrices;
	}

	@Override
	public void addStockPriceToStore(String ticker, long dateEpoch, double adjClose) {
		LOGGER.debug("Entering DBHistoricalPrices.addStockPrice(String {}, long {}, double {})", ticker, dateEpoch, adjClose);
		Connection dbConnection;
		int insertedRowCount;
		try {
			LOGGER.debug("Getting connection to {}", Constants.DB_NAME);
			LOGGER.debug("Inserting historical stock price data for ticker {} into the database.", ticker);
			dbConnection = pgDataSource.getConnection();
			PreparedStatement sqlStatement = dbConnection.prepareStatement("INSERT INTO prices (ticker, adj_close, close_epoch) VALUES (?, ?, ?)");
			sqlStatement.setString(1, ticker);
			sqlStatement.setDouble(2, adjClose);
			sqlStatement.setLong(3, dateEpoch);
			LOGGER.debug("Executing INSERT INTO prices (ticker, adj_close, close_epoch) VALUES ({}, {}, {})", ticker, adjClose, dateEpoch);
			if ((insertedRowCount = sqlStatement.executeUpdate()) != 1) {
				LOGGER.warn("Inserted {} rows. Should have inserted 1 row.", insertedRowCount);
			}
			dbConnection.close();
		} catch (SQLException sqle) {
			LOGGER.info("Unable to get connection to {}", Constants.DB_NAME);
			LOGGER.debug("Caught (SQLException sqle)", sqle);
		}
	}

	@Override
	private void insertOrder(Order portfolioOrder) throws SQLException {
		LOGGER.debug("Entering Portfolio.insertDbOrder(Order {})", portfolioOrder.getOrderId());
		Connection dbConnection = pgDataSource.getConnection();
		String sqlString = "INSERT INTO orders (portfolio, order_id, open, ticker, epoch_expiry, underlying_ticker, strike_price, limit_price, " +
				"action, total_quantity, sec_type, tif, epoch_opened, claim_against_cash) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement newOrderSqlStatement;
		int insertedRowCount;
		int updatedRowCount;
		newOrderSqlStatement = dbConnection.prepareStatement(sqlString);
		newOrderSqlStatement.setString(1, Constants.PORTFOLIO_NAME);
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
				Constants.PORTFOLIO_NAME, portfolioOrder.getOrderId(), portfolioOrder.isOpen(), portfolioOrder.getTicker(), portfolioOrder.getExpiry().getMillis(),
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

	@Override
	public void closeOrder(Order portfolioOrder) throws SQLException {
   /* Nothing changes in an order unless it is filled (i.e. closed) */
		LOGGER.debug("Entering Portfolio.closeDbOrder(Order {})", portfolioOrder.getOrderId());
		Connection dbConnection = pgDataSource.getConnection();
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

	@Override
	public void insertPosition(Position portfolioPosition) throws SQLException {
		LOGGER.debug("Entering Portfolio.insertDbPosition(Position {})", portfolioPosition.getPositionId());
		Connection dbConnection = pgDataSource.getConnection();
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

	@Override
	public void updatePosition(Position portfolioPosition) throws SQLException {
		LOGGER.debug("Entering Portfolio.updateDbPosition(Position {})", portfolioPosition.getPositionId());
		Connection dbConnection = pgDataSource.getConnection();
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

	@Override
	public void insertSummary() throws SQLException {
		LOGGER.debug("Entering Portfolio.insertDbSummary()");
		Connection dbConnection = pgDataSource.getConnection();
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

	@Override
	public void updateSummary(Portfolio portfolio) throws SQLException {
		LOGGER.debug("Entering Portfolio.updateDbSummary()");
		Connection dbConnection = pgDataSource.getConnection();
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

	@Override
	Set<Order> getPortfolioOrders(String portfolioName) throws SQLException {
		LOGGER.debug("Entering Portfolio.getDbPortfolioOrders()");
		Connection dbConnection = pgDataSource.getConnection();
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

	@Override
	public void dbPositionsWrite() throws SQLException {
		LOGGER.debug("Entering Portfolio.endOfDayDbPositionsWrite()");
		Connection dbConnection = pgDataSource.getConnection();
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

	@Override
	void getDbPortfolioSummary() throws SQLException {
		LOGGER.debug("Entering Portfolio.getDbPortfolioSummary()");
		Connection dbConnection = pgDataSource.getConnection();
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
}