package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Class {@code GaussTrader} is the entry class for the GaussTrader application.
 * <p>
 * GaussTrader (abbreviated "GT") is an an algorithm driven security trading simulator. GT monitors blue chip equities
 * and attempts to identify mispriced stocks.
 * <p>
 * GaussTrader is intended to track the DOW 30 and Apple. These are large cap companies which, because of their size,
 * should not have large share price changes. When a share price makes a large move an option is sold against the stock.
 * The option is held until maturity. If the option expires out of the money then GT pockets the premium. If the option
 * expires in the money, then the stock is purchased against the short put or sold against the short call, as
 * appropriate.
 * <p>
 * On initialization
 * <p>
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.1
 */

public class GaussTrader {
   private static final Logger LOGGER = LoggerFactory.getLogger(GaussTrader.class);
   public static WatchList watchList = new WatchList();
   public static DataStore dataStore = new PostgresStore();

   public static void main(String[] args) {
      Instant programStartTime = Instant.now();
      LOGGER.info("*** START PROGRAM ***");
      LOGGER.info("Starting GaussTrader at {}", programStartTime);
      /* Add DJIA and Apple to list of securities to watch */
      watchList.watch(Constants.DOW_JONES_TICKERS);
      watchList.watch("AAPL");
      dataStore.updateStockMetricsToStorage(watchList.getTickers());
      LOGGER.debug("watchList.getTickerCount() = {}", watchList.getTickerCount());
      watchList.reset();
      LOGGER.debug("Creating new TradingSession() with new Portfolio({})", Constants.PORTFOLIO_NAME);
      TradingSession todaysSession = new TradingSession(new Portfolio(Constants.PORTFOLIO_NAME), watchList);
      todaysSession.runTradingDay();
      LOGGER.info("*** END PROGRAM ***");
   }
}