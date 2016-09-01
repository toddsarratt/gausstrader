package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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
   private static final Logger LOGGER = LoggerFactory.getLogger(WatchList.class);
   /* tickerSet<String> is created from data passed into the watch() method. A Set is used to prevent duplicate tickers.
    * Each ticker is used to create a Stock object which is stored in tradeableStockSet<Stock> */
   private Set<String> tickerSet = new HashSet<>();
   private Set<Stock> tradeableStockSet = new HashSet<>();

   /**
    * Adds a ticker or tickers to the list of securities for the application to watch, which is held by tickerSet.
    *
    * @param tickers String vararg representing tickers to watch
    */
   public void watch(String... tickers) {
      Arrays.stream(tickers).forEach(tickerSet::add);
      addTickerSetToTradeableSet();
   }

   /**
    * Uses map to create a Stock object for each ticker. Clears tickerSet.
    * TODO: But is it necessary to do so? Can using Sets make ticker/stock adds idempotent?
    */
   private void addTickerSetToTradeableSet() {
      LOGGER.debug("Entering addTickerSetToTradeableSet()");
      tradeableStockSet.addAll(
              tickerSet.stream()
                      .map(Stock::of)
                      .filter(Stock::tickerValid)
                      // Make sure Bollinger bands have been calculated TODO: There is probably a better way to do this
                      .filter(stock -> stock.getBollingerBand(0).compareTo(BigDecimal.ZERO) > 0)
                      .collect(Collectors.toList())
      );
      tickerSet.clear();
   }

   /**
    * @return a Set<String> containing all the tickers in the watchlist
    */
   public Set<String> getTickerSet() {
      return tradeableStockSet.stream()
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
      dataStore.resetWatchList();
   }
}
