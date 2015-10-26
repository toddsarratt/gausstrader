package net.toddsarratt.GaussTrader;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

   void deactivateStock(String tickerToRemove) {
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
