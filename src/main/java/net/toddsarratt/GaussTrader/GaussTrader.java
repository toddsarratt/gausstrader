package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * http://www.gummy-stuff.org/Yahoo-data.htm
 *
 * @author tsarratt
 */

public class GaussTrader {

   protected static String portfolioName = "reboot06132016";
   protected static final String DB_IP = "localhost";
   protected static final String DB_NAME = "postgres";
   protected static final String DB_USER = "postgres";
   protected static final String DB_PASSWORD = "b3llcurv38";
   protected static final int YAHOO_RETRIES = 5;                // Number of ties to retry Yahoo connections
   protected static final double STOCK_PCT_OF_PORTFOLIO = 10.0;
   protected static final double STARTING_CASH = 1_000_000.00;  // Default value for new portfolio
   // TODO : Shouldn't these all be final?
   protected static int bollBandPeriod = 20;
   protected static double bollingerSD1 = 2.0;
   protected static double bollingerSD2 = 2.5;
   protected static double bollingerSD3 = 3.0;
   protected static boolean delayedQuotes = true;   // 20min delay using quotes from Yahoo!
   protected static int delayMs = 60_000;          // Time between each stock price check to reduce (not even real-time) data churn
   private static PGSimpleDataSource dataSource = new PGSimpleDataSource();
   private static final Logger LOGGER = LoggerFactory.getLogger(GaussTrader.class);

   static {
      LOGGER.info("*** START PROGRAM ***");
      LOGGER.info("Starting GaussTrader at {}", new DateTime());
      /** Set up DB connection. Package classes that need DB access can call GaussTrader.getDataSource() */
      LOGGER.debug("Entering GaussTrader.prepareDatabaseConnection()");
      LOGGER.debug("dataSource.setServerName({})", DB_IP);
      dataSource.setServerName(DB_IP);
      LOGGER.debug("dataSource.setDatabaseName({})", DB_NAME);
      dataSource.setDatabaseName(DB_NAME);
      LOGGER.debug("dataSource.setUser({})", DB_USER);
      dataSource.setUser(DB_USER);
      LOGGER.debug("dataSource.setPassword({})", DB_PASSWORD);
      dataSource.setPassword(DB_PASSWORD);
   }

   /** Returns current epoch time + least significant nano seconds to generate unique order and position ids */
   static long getNewId() {
      return ((System.currentTimeMillis() << 20) & 0x7FFFFFFFFFF00000l) | (System.nanoTime() & 0x00000000000FFFFFl);
   }

   static DataSource getDataSource() {
      return dataSource;
   }

   private static void addTickerlistToTradeableList(ArrayList<String> tickerList, ArrayList<Stock> tradeableStockList) {
      LOGGER.debug("Entering GaussTrader.addTickerlistToTradeableList(ArrayList<String> {}, ArrayList<Stock> {})", tickerList.toString(), tradeableStockList.toString());
      Stock stockToAdd;
      for(String candidateTicker : tickerList) {
         try {
            stockToAdd = new Stock(candidateTicker);
            if (!stockToAdd.tickerValid()) {
               LOGGER.warn("Ticker {} invalid. Removing from candidate ticker list.", candidateTicker);
            } else if (stockToAdd.getBollingerBand(0) <= 0.00) {
               LOGGER.warn("Failed to calculate valid Bollinger Bands for {}. Removing from candidate ticker list.", candidateTicker);
            } else {
               tradeableStockList.add(stockToAdd);
               WatchList.updateDbStockMetrics(stockToAdd);
               LOGGER.info("Adding {} to tradeableStockList", candidateTicker);
            }
         } catch (IOException ioe) {
            LOGGER.error("Cannot connect to Yahoo!");
            LOGGER.error("IOException ioe", ioe);
         } catch (NumberFormatException nfe) {
            LOGGER.warn("Bad data from Yahoo! for ticker {}", candidateTicker);
            LOGGER.debug("Caught (NumberFormatException nfe)", nfe);
         }
      }
   }

   public static void main(String[] args) {
      try {
         ArrayList<String> tickerList = new ArrayList<>();
         ArrayList<Stock> tradeableStockList = new ArrayList<>();
       /* Add DJIA to list of stocks to track */
         String[] tickerArray = {"MMM", "NKE", "AXP", "T", "GS", "BA", "CAT", "CVX", "CSCO", "KO", "DD", "XOM", "GE", "V", "HD", "INTC",
            "IBM", "JNJ", "JPM", "MCD", "MRK", "MSFT", "PFE", "PG", "TRV", "UNH", "UTX", "VZ", "WMT", "DIS"};
         tickerList.addAll(Arrays.asList(tickerArray));
	    /* Adding AAPL for Bill */
         tickerList.add("AAPL");
         LOGGER.debug("tickerList.size() = {}", tickerList.size());
         WatchList.resetWatchList();
         addTickerlistToTradeableList(tickerList, tradeableStockList);
         LOGGER.debug("Creating new TradingSession() with new Portfolio({})", portfolioName);
         TradingSession todaysSession = new TradingSession(new Portfolio(portfolioName), tradeableStockList);
         todaysSession.runTradingDay();
         LOGGER.info("*** END PROGRAM ***");
      } catch (Exception e) {
         LOGGER.error("ENCOUNTERED UNEXPECTED FATAL ERROR");
         LOGGER.error("Caught (Exception e)", e);
         LOGGER.error("*** END PROGRAM ***");
         System.exit(1);
      }
   }
}