package net.toddsarratt.GaussTrader;

import com.sun.xml.internal.bind.v2.TODO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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
   /* tickerList<String> is created from data passed into the watch() method. Each ticker is verified and stored in
    * tradeableStockList<Stock>
    */
   private ArrayList<String> tickerList = new ArrayList<>();
   /* tradeableStockList<Stock> contains the Stock objects representing the securities watched by this WatchList object.
      The Stock objects are created based on the tickers passed into tickerList<String>
    */
   private ArrayList<Stock> tradeableStockList = new ArrayList<>();

   /**
    * Adds a ticker or tickers to the list of securities for the application to watch.
    *
    * @param tickers String vararg representing tickers to watch
    */
   public void watch(String... tickers) {
      Arrays.stream(tickers).forEach(tickerList::add);
      addTickerlistToTradeableList();
   }

   /**
    * Used to exist in GaussTrader class.
    */
   private void addTickerlistToTradeableList() {
      LOGGER.debug("Entering addTickerlistToTradeableList()");
      tradeableStockList.addAll(
              tickerList.stream()
                      .map(Stock::of)
                      .filter(Stock::tickerValid)
                      .filter(stock -> stock.getBollingerBand(0) > 0.00)
                      .collect(Collectors.toList())
      );
      tickerList.clear();
               /* Past price history is collected from the network when Stock object is created. Save this to the
               dataStore for cheaper future retrieval */
   }

   // TODO : What the hell did this do? Why don't I document better?
   void reset() {
      LOGGER.debug("Entering reset()");
      DataStore dataStore = new PostgresStore();
      dataStore.resetWatchList();
   }

   /* TODO: Allow a change of data store. Currently there is only one class that implements DataStore so this is just
      a stub
   */
   boolean setDataStore(String dataStoreName) {
      boolean dataStoreSuccessfullySet = false;
      return dataStoreSuccessfullySet;
   }
}
