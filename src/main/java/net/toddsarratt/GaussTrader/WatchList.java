package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This represents a list of stocks being watched by GaussTrader.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.1
 */

public class WatchList {
   private static final Logger LOGGER = LoggerFactory.getLogger(DBHistoricalPrices.class);
   private static DataStore dataStore = new PostgresStore();
   /* tickerSet<String> is created from data passed into the watch() method. A Set is used to prevent duplicate tickers.
    * Each ticker is used to create a Stock object which is stored in tradeableStockSet<Stock> */
   private Set<String> tickerSet = new HashSet<>();
   private Set<Stock> tradeableStockSet = new HashSet<>();

   /**
    * Adds a ticker or tickers to the list of securities for the application to watch.
    *
    * @param tickers String vararg representing tickers to watch
    */
   public void watch(String... tickers) {
      Arrays.stream(tickers).forEach(tickerSet::add);
      addTickerSetToTradeableSet();
   }

   /**
    * Uses map to create a Stock object for each ticker. Clears tickerSet.
    */
   private void addTickerSetToTradeableSet() {
      LOGGER.debug("Entering addTickerlistToTradeableList()");
      tradeableStockSet.addAll(
              tickerSet.parallelStream()
                      .map(Stock::of)
                      .filter(Stock::tickerValid)
                      .filter(stock -> stock.getBollingerBand(0) > 0.00)
                      .collect(Collectors.toList())
      );
      tickerSet.clear();
   }

   /**
    * @return a Set<String> containing all the tickers in the watchlist
    */
   public Set<String> getTickerSet() {
      return tradeableStockSet.parallelStream()
              .map(Stock::getTicker)
              .collect(Collectors.toSet());
   }

   /**
    * @return a Set<Stock> containing all the Stock objects in the watchlist
    */
   public Set<Stock> getStockSet() {
      return tradeableStockSet;
   }

   // TODO : What the hell did this do? Why don't I document better?
   void reset() {
      LOGGER.debug("Entering reset()");
      DataStore dataStore = new PostgresStore();
      dataStore.resetWatchList();
   }
}
