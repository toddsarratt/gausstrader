package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   private static PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
   private static final Logger LOGGER = LoggerFactory.getLogger(PostgresStore.class);

   /**
    * Set up Postgres database connection
    */
   static {
      LOGGER.debug("Entering connect()");
      LOGGER.debug("dataSource.setServerName({})", Constants.DB_IP);
      pgDataSource.setServerName(Constants.DB_IP);
      LOGGER.debug("dataSource.setDatabaseName({})", Constants.DB_NAME);
      pgDataSource.setDatabaseName(Constants.DB_NAME);
      LOGGER.debug("dataSource.setUser({})", Constants.DB_USER);
      pgDataSource.setUser(Constants.DB_USER);
      LOGGER.debug("dataSource.setPassword({})", Constants.DB_PASSWORD);
      pgDataSource.setPassword(Constants.DB_PASSWORD);
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
         double currentPrice = stock.getPrice();
         long lastTickEpoch = stock.getLastPriceUpdateEpoch();
         sqlStatement.setDouble(1, currentPrice);
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
   public LinkedHashMap<Long, Double> getStoredPrices(String ticker, DateTime earliestCloseDate) {
      LOGGER.debug("Entering getDBPrices(String {}, DateTime {} ({})",
              ticker, earliestCloseDate.getMillis(), earliestCloseDate.toString());
      LinkedHashMap<Long, Double> queriedPrices = new LinkedHashMap<>();
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
            queriedPrices.put(closeEpoch, adjClose);
            dbPricesFound++;
         }
         dbConnection.close();
      } catch (SQLException sqle) {
         LOGGER.info("Unable to get connection to {}", Constants.DB_NAME);
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
      LOGGER.debug("Found {} prices in db", dbPricesFound);
      LOGGER.debug("Returning from database LinkedHashMap of size {}", queriedPrices.size());
      if (dbPricesFound != queriedPrices.size()) {
         LOGGER.warn("Mismatch between prices found {} and number returned {}, possible duplication?", dbPricesFound, queriedPrices.size());
      }
      return queriedPrices;
   }

   @Override
   public void updateStockMetricsToStorage(Stock stockToUpdate) {
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
         sqlUpdateStatement.setDouble(1, stockToUpdate.getTwentyDma());
         sqlUpdateStatement.setDouble(2, stockToUpdate.getBollingerBand(3));
         sqlUpdateStatement.setDouble(3, stockToUpdate.getBollingerBand(1));
         sqlUpdateStatement.setDouble(4, stockToUpdate.getBollingerBand(4));
         sqlUpdateStatement.setDouble(5, stockToUpdate.getBollingerBand(2));
         sqlUpdateStatement.setDouble(6, stockToUpdate.getPrice());
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
   public void updateStockMetricsToStorage(Set<Stock> stockSet) {
      stockSet.parallelStream().forEach(this::updateStockMetricsToStorage);
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
}