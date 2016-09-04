package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Period;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class TradingSession {
	private static final Logger LOGGER = LoggerFactory.getLogger(TradingSession.class);
	private Market market = GaussTrader.getMarket();
	private DataStore dataStore = GaussTrader.getDataStore();
	private LinkedHashSet<Stock> stockSet;
	private Portfolio portfolio;
	/* Time variables */
	private static DateTime todaysDateTime = new DateTime(DateTimeZone.forID("America/New_York"));
	private static int dayToday = todaysDateTime.getDayOfWeek();

	TradingSession(Market market, Portfolio portfolio, LinkedHashSet<Stock> stockSet) {
		LOGGER.debug("Creating new TradingSession() in market {} with portfolio {} and watchlist {}",
				market.getName(), portfolio.getName(), stockSet);
		this.market = market;
		this.portfolio = portfolio;
		this.stockSet = stockSet;
	}

	/**
	 * Main method for the TradingSession class. Runs a loop from the start of the trading day until the end, performing
	 * various stock checks and portfolio updates.
	 */
	void runTradingDay() {
		LOGGER.debug("Start of runTradingDay()");
		if (market.isOpenToday()) {
			sleepUntilMarketOpen();
			tradeUntilMarketClose();
		 /* End of day tasks */
			closeGoodForDayOrders();
			writeClosingPricesToDb();
		} else {
			LOGGER.info("Market is closed today.");
		}
		reconcileExpiringOptions();
		// TODO : portfolio.updatePositionsNetAssetValues() or something similar needs to be written and called here
		portfolio.calculateNetAssetValue();
		dataStore.write(portfolio.getSummary());
		LOGGER.info("End of trading day.");
	}

	private void tradeUntilMarketClose() {
		Stock stock;
		Iterator<Stock> stockIterator = stockSet.iterator();
		while (market.isOpenRightNow()) {
			if (!stockIterator.hasNext()) {
				stockIterator = stockSet.iterator();
				if (!stockIterator.hasNext()) {
					LOGGER.warn("No stocks to trade");
					break;
				}
			}
			stock = stockIterator.next();
			handleMispricedStock(stock);
			pauseBetweenCycles();

		}
		checkOpenOrders();
		portfolio.updateOptionPositions(stock);
		portfolio.updateStockPositions(stock);
		portfolio.calculateNetAssetValue();
		dataStore.write(portfolio.getSummary());
	}

	private void sleepUntilMarketOpen() {
		long msUntilMarketOpen = market.timeUntilMarketOpens().toMillis();
		LOGGER.debug("msUntilMarketOpen == {} ", msUntilMarketOpen);
		if (msUntilMarketOpen > 0) {
			try {
				LOGGER.debug("Sleeping");
				Thread.sleep(msUntilMarketOpen);
			} catch (InterruptedException ie) {
				LOGGER.warn("InterruptedException attempting Thread.sleep in method sleepUntilMarketOpen()");
				LOGGER.debug("", ie);
			}
		}
	}

	private void handleMispricedStock(Stock stock) {
		LOGGER.debug("Entering handleMispricedStock()");
		String ticker = stock.getTicker();
		InstantPrice currentPrice;
		LOGGER.debug("Retrieved stock with ticker {} from stockIterator", ticker);
		currentPrice = market.lastTick(ticker);
		LOGGER.debug("stock.lastTick() returns ${}", currentPrice);
		if ((currentPrice == InstantPrice.NO_PRICE)) {
			LOGGER.warn("Could not get valid price for ticker {}", ticker);
		} else {
			dataStore.writeStockPrice(ticker, currentPrice);
			PriceBasedAction actionToTake = priceActionable(stock);
			if (actionToTake.doSomething) {

				Option optionToSell = Option.getOption(ticker, actionToTake.optionType, currentPrice);
				if (optionToSell == null) {
				/*Handle bad option searching */

				} else {
					try {
						portfolio.addNewOrder(new Order(optionToSell, optionToSell.lastBid(), "SELL", actionToTake.contractsToTransact, "GFD"));
					} catch (InsufficientFundsException ife) {
						LOGGER.warn("Not enough free cash to initiate order for {} @ ${}", optionToSell.getTicker(), optionToSell.lastBid(), ife);
					}
				}
			}
		}

	}

	private void checkOpenOrders() {
		LOGGER.debug("Entering TradingSession.checkOpenOrders()");
		InstantPrice lastTick;
		for (Order openOrder : portfolio.getListOfOpenOrders()) {
			LOGGER.debug("Checking current open orderId {} for ticker {}", openOrder.getOrderId(), openOrder.getTicker());
				lastTick = market.lastTick(openOrder.getTicker());
				LOGGER.debug("{} lastTick == {}", openOrder.getTicker(), lastTick);
				if (openOrder.canBeFilled(lastTick.getPrice())) {
					LOGGER.debug("openOrder.canBeFilled({}) returned true for ticker {}", lastTick, openOrder.getTicker());
					portfolio.fillOrder(openOrder, lastTick.getPrice());
				}
		}
	}

	private void pauseBetweenCycles() {
		LOGGER.debug("Entering TradingSession.pauseBetweenCycles()");
		long sleepTimeMs;
		long msUntilMarketClose = marketCloseEpoch - System.currentTimeMillis();
		LOGGER.debug("Comparing msUntilMarketClose {} with GaussTrader.delayMs {}", msUntilMarketClose, Constants.DELAY_MS);
		sleepTimeMs = (msUntilMarketClose < Constants.DELAY_MS) ? msUntilMarketClose : Constants.DELAY_MS;
		if (sleepTimeMs > 0) {
			try {
				LOGGER.debug("Sleeping for {} ms ({})", sleepTimeMs, (new Period(sleepTimeMs).toString(PeriodFormat.wordBased())));
				Thread.sleep(sleepTimeMs);
			} catch (InterruptedException ie) {
				LOGGER.warn("Interrupted exception trying to sleep {} ms", sleepTimeMs);
				LOGGER.debug("Caught (InterruptedException ie)", ie);
			}
		}
	}

	private PriceBasedAction priceActionable(Stock stock) {
	    /* Decide if a security's current price triggers a predetermined event */
		LOGGER.debug("Entering TradingSession.priceActionable(Stock {})", stock.getTicker());
		double currentStockPrice = stock.getPrice();
		LOGGER.debug("Comparing current price ${} against Bollinger Bands {}", currentStockPrice, stock.describeBollingerBands());
		if (currentStockPrice >= stock.getBollingerBand(1)) {
			return findCallAction(stock);
		}
		if (stock.getFiftyDma() < stock.getTwoHundredDma()) {
			LOGGER.info("Stock {} 50DMA < 200DMA. No further checks.", stock.getTicker());
			return DO_NOTHING_PRICE_BASED_ACTION;
		}
		if (currentStockPrice <= stock.getBollingerBand(3)) {
			return findPutAction(stock);
		}
		LOGGER.info("Stock {} at ${} is within Bollinger Bands", stock.getTicker(), currentStockPrice);
		return DO_NOTHING_PRICE_BASED_ACTION;
	}

	private PriceBasedAction findCallAction(Stock stock) {
		LOGGER.debug("Entering TradingSession.findCallAction(Stock {})", stock.getTicker());
		if (portfolio.countUncoveredLongStockPositions(stock) < 1) {
			LOGGER.info("Open long {} positions is equal or less than current short calls positions. Taking no action.", stock.getTicker());
			return DO_NOTHING_PRICE_BASED_ACTION;
		}
		if (stock.getPrice() >= stock.getBollingerBand(2)) {
			LOGGER.info("Stock {} at ${} is above 2nd Bollinger Band of {}", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(2));
			return new PriceBasedAction(true, "CALL", Math.min(portfolio.numberOfOpenStockLongs(stock), 5));
		}
      /* TODO : Remove this if statement. We would not be in this method if the condition wasn't met */
		if (stock.getPrice() >= stock.getBollingerBand(1)) {
			LOGGER.info("Stock {} at ${} is above 1st Bollinger Band of {}", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(1));
			return new PriceBasedAction(true, "CALL", Math.min(portfolio.numberOfOpenStockLongs(stock), 5));
		}
		return DO_NOTHING_PRICE_BASED_ACTION;
	}

	private PriceBasedAction findPutAction(Stock stock) {
		LOGGER.debug("Entering TradingSession.findPutAction(Stock {})", stock.getTicker());
		double currentStockPrice = stock.getPrice();
		int openPutShorts = portfolio.numberOfOpenPutShorts(stock);
		int maximumContracts = (int) (portfolio.calculateNetAssetValue() * (0.01 * Constants.STOCK_PCT_OF_PORTFOLIO) / (100 * currentStockPrice));
		if (currentStockPrice <= stock.getBollingerBand(5)) {
			LOGGER.info("Stock {} at ${} is below 3rd Bollinger Band of {}", stock.getTicker(), currentStockPrice, stock.getBollingerBand(5));
			if (openPutShorts < maximumContracts) {
				return new PriceBasedAction(true, "PUT", Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
			return DO_NOTHING_PRICE_BASED_ACTION;
		}
		if (currentStockPrice <= stock.getBollingerBand(4)) {
			LOGGER.info("Stock {} at ${} is below 2nd Bollinger Band of {}", stock.getTicker(), currentStockPrice, stock.getBollingerBand(4));
			if (openPutShorts < maximumContracts / 2) {
				return new PriceBasedAction(true, "PUT", Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
			return DO_NOTHING_PRICE_BASED_ACTION;
		}
      /* TODO : Remove this if statement. We would not be in this method if the condition wasn't met */
		if (currentStockPrice <= stock.getBollingerBand(3)) {
			LOGGER.info("Stock {} at ${} is below 1st Bollinger Band of {}", stock.getTicker(), currentStockPrice, stock.getBollingerBand(3));
			if (openPutShorts < maximumContracts / 4) {
				return new PriceBasedAction(true, "PUT", Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
		}
		return DO_NOTHING_PRICE_BASED_ACTION;
	}

	/**
	 * If within two days of expiry exercise ITM options. If underlyingTicker < optionStrike then stock is PUT to Portfolio
	 * If ticker > strike stock is CALLed away
	 * else option expires worthless, close position
	 */
	private void reconcileExpiringOptions() {
		LOGGER.debug("Entering TradingSession.reconcileExpiringOptions()");
		LOGGER.info("Checking for expiring options");
		MutableDateTime thisFriday = new MutableDateTime(todaysDateTime, DateTimeZone.forID("America/New_York"));
		thisFriday.setDayOfWeek(DateTimeConstants.FRIDAY);
		int thisFridayJulian = thisFriday.getDayOfYear();
		int thisFridayYear = thisFriday.getYear();
		if (dayToday == DateTimeConstants.FRIDAY) {
			LOGGER.debug("Today is Friday, checking portfolio.getListOfOpenOptionPositions() for expiring options");
			for (Position openOptionPosition : portfolio.getListOfOpenOptionPositions()) {
				LOGGER.debug("Examining positionId {} for option ticker {}", openOptionPosition.getPositionId(), openOptionPosition.getTicker());
				LOGGER.debug("Comparing Friday Julian {} to {} and year {} to {}",
						openOptionPosition.getExpiry().getDayOfYear(), thisFridayJulian, openOptionPosition.getExpiry().getYear(), thisFridayYear);
				if ((openOptionPosition.getExpiry().getDayOfYear() == thisFridayJulian) &&
						(openOptionPosition.getExpiry().getYear() == thisFridayYear)) {
					LOGGER.debug("Option expires tomorrow, checking moneyness");
					try {
						double stockLastTick = Stock.lastTick(openOptionPosition.getUnderlyingTicker());
						if (stockLastTick < 0.00) {
                     /* TODO : Need logic to handle this condition */
							throw new IOException("Foo");
						}
						if (openOptionPosition.isPut() &&
								(stockLastTick <= openOptionPosition.getStrikePrice())) {
							portfolio.exerciseOption(openOptionPosition);
						} else if (openOptionPosition.isCall() &&
								(stockLastTick >= openOptionPosition.getStrikePrice())) {
							portfolio.exerciseOption(openOptionPosition);
						} else {
							portfolio.expireOptionPosition(openOptionPosition);
						}
					} catch (IOException ioe) {
						LOGGER.info("Caught IOException attempting to get information on open option position ticker {}", openOptionPosition.getTicker());
						LOGGER.debug("Caught (IOException ioe)", ioe);
					}
				}
			}
		} else {
			LOGGER.debug("Today is not Friday, no expirations to check");
		}
	}

	private void closeGoodForDayOrders() {
	/* Only call this method if the trading day had ended */
		LOGGER.debug("Entering TradingSession.closeGoodForDayOrders()");
		LOGGER.info("Closing GFD orders");
		for (Order checkExpiredOrder : portfolio.getListOfOpenOrders()) {
			if (checkExpiredOrder.getTif().equals("GFD")) {
				portfolio.expireOrder(checkExpiredOrder);
			}
		}
	}

	/**
	 * This method is NOT called if options expiration occurs on a Friday when the market is closed
	 * Do not make any change to this logic assuming that it will always be run on a day when
	 * option positions have been opened, closed, or updated
	 */
	private void writeClosingPricesToDb() {
		LOGGER.debug("Entering writeClosingPricesToDb()");
		LOGGER.info("Writing closing prices to DB");
		InstantPrice closingPrice;
		for (Stock stock : stockSet) {
			closingPrice = market.lastTick(stock);
			if (closingPrice == InstantPrice.NO_PRICE) {
				LOGGER.warn("Could not get valid price for ticker {}", stock.getTicker());
				return;
			}
			if (closingPrice.getInstant().isBefore(market.getClosingTime)) {
				LOGGER.warn("stock.getLastPriceUpdateEpoch() {} < earliestAcceptableLastPriceUpdateEpoch {}",
						stock.getLastPriceUpdateEpoch(), earliestAcceptableLastPriceUpdateEpoch);
				return;
			}
			dataStore.writeStockPrice(stock.getTicker(), closingPrice);
		}
	}
}