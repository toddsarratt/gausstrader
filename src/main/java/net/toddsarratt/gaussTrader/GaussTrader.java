package net.toddsarratt.gaussTrader;

import net.toddsarratt.gaussTrader.singletons.Constants;
import net.toddsarratt.gaussTrader.singletons.DataStore;
import net.toddsarratt.gaussTrader.singletons.Market;
import net.toddsarratt.gaussTrader.singletons.PostgresStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;


/**
 * Class {@code gaussTrader} is the entry class for the gaussTrader application.
 * <p>
 * gaussTrader is an an algorithm driven security trading simulator. gaussTrader monitors blue chip equities and
 * attempts to identify mispriced stocks.
 * <p>
 * gaussTrader is intended to track the DOW 30 and Apple. These are large cap companies which, because of their size,
 * should not have large share price changes. When a share price makes a large move an option is sold against the stock.
 * The option is held until maturity. If the option expires out of the money then GT pockets the premium. If the option
 * expires in the money, then the stock is purchased against the short put or sold against the short call, as
 * appropriate.
 * <p> TODO:
 * On initialization...
 * <p>
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since gaussTrader v0.1
 */

public class GaussTrader {
	private static final Logger LOGGER = LoggerFactory.getLogger(GaussTrader.class);
	// TODO: Inject via static factory method but not in this method
	private static final DataStore DATA_STORE = new PostgresStore();
	private static Market market = new YahooMarket();
	private static Portfolio portfolio = Portfolio.of(Constants.PORTFOLIO_NAME);

	/**
	 * @return DataStore associated with the application.
	 */
	static DataStore getDataStore() {
		return DATA_STORE;
	}

	/**
	 * @return Market associated with the application.
	 */
	static Market getMarket() {
		return market;
	}

	public static void main(String[] args) {
		Instant programStartTime = Instant.now();
		LOGGER.info("*** START PROGRAM ***");
		LOGGER.info("Starting gaussTrader at {}", programStartTime);
		watchList.watch(Constants.TICKERS);
		  /* Past price history is collected from the network when Stock objects are created. Save to the DATA_STORE for
       cheaper future retrieval. TODO: Why is this being done here and not in another class? */
		DATA_STORE.writeStockMetrics(watchList.getStockSet());
		LOGGER.debug("watchList.getTickers() = {}", watchList.getTickerSet());
		/* This has something to do with active / inactive... Or something TODO: WHAT DOES THIS DO? */
		DATA_STORE.resetWatchList();
		LOGGER.debug("Creating new TradingSession() with new Portfolio({})", portfolio.getName());
		TradingSession todaysSession = new TradingSession(portfolio, watchList);
		todaysSession.runTradingDay();
		LOGGER.info("*** END PROGRAM ***");
	}
}