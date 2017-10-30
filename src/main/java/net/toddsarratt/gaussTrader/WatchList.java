package net.toddsarratt.gaussTrader;

import net.toddsarratt.gaussTrader.securities.Stock;
import net.toddsarratt.gaussTrader.singletons.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This represents a list of stocks being watched by gaussTrader. Only one watch list is currently supported per
 * application instance. Use getInstance() to reference this singleton.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since gaussTrader v0.1
 */

class WatchList {
	private static final WatchList INSTANCE = new WatchList();
	private static final Logger LOGGER = LoggerFactory.getLogger(WatchList.class);
	private static final DataStore DATA_STORE = GaussTrader.getDataStore();
	/* tickerSet<String> is created from data passed into the watch() method. A Set is used to prevent duplicate tickers.
	 * Each ticker is used to create a Stock object which is stored in tradeableStockSet<Stock> */
	private Set<String> tickerSet = new HashSet<>();
	private Set<Stock> tradeableStockSet = new HashSet<>();

	/* Though shalt not instantiate outside the singleton declaration */
	private WatchList() {}

	/**
	 * Supports singleton pattern.
	 * @return singleton object of the WatchList class
	 */
	static WatchList getInstance() {
		return INSTANCE;
	}

	/**
	 * Adds a ticker or tickers to the list of securities for the application to watch, which is held by tickerSet.
	 *
	 * @param tickers String vararg representing tickers to watch
	 */
	public void watch(List<String> tickers) {
		tradeableStockSet.addAll(
				tickers.stream()
						.map(Stock::of)
						// If "ticker" is not valid, Stock.of(ticker) might return null TODO: Handle invalid ticker
						.filter(stock -> stock != null)
						// Make sure Bollinger bands have been calculated TODO: There is probably a better way to do this
						.filter(stock -> stock.getBollingerBand(0).compareTo(BigDecimal.ZERO) > 0)
						.collect(Collectors.toList())
		);
	}

	/**
	 * @return a Set<String> containing all the tickers in the watchlist
	 */
	Set<String> getTickerSet() {
		return tradeableStockSet.stream()
				.map(Stock::getTicker)
				.collect(Collectors.toSet());
	}

	/**
	 * @return a Set<Stock> containing all the Stock objects in the watchlist
	 */
	Set<Stock> getStockSet() {
		return tradeableStockSet;
	}

	// TODO : What the hell did this do? Why don't I document better?
	void reset() {
		LOGGER.debug("Entering reset()");
	}
}
