package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.LinkedHashSet;

class TradingSession {
	private static final Logger LOGGER = LoggerFactory.getLogger(TradingSession.class);
	private Market market = GaussTrader.getMarket();
	private DataStore dataStore = GaussTrader.getDataStore();
	private LinkedHashSet<Stock> stockSet;
	private Portfolio portfolio;

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
		LOGGER.debug("Start runTradingDay()");
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
		LOGGER.info("End trading day.");
	}

	private void tradeUntilMarketClose() {
		Iterator<Stock> stockIterator = stockSet.iterator();
		while (market.isOpenRightNow()) {
			if (!stockIterator.hasNext()) {
				stockIterator = stockSet.iterator();
				if (!stockIterator.hasNext()) {
					LOGGER.warn("No stocks to trade");
					break;
				}
			}
			Stock stock = stockIterator.next();
			InstantPrice currentInstantPrice = market.lastTick(stock.getTicker());
			BigDecimal stockPrice = currentInstantPrice.getPrice();
			dataStore.writeStockPrice(stock.getTicker(), currentInstantPrice);
			PriceBasedAction actionToTake = TradingStrategy.findActionToTake(stock, stockPrice, portfolio);
			if (actionToTake.isActionable()) {
				takeActionOnStock(stock, actionToTake);
			}
			try {
				portfolio.updateOptionPositions(stock);
				portfolio.updateStockPositions(stock);
			} catch (IOException | SQLException e) {
				LOGGER.warn("Caught exception attempting to update portfolio");
			}
			portfolio.calculateNetAssetValue();
			dataStore.write(portfolio.getSummary());
			pauseBetweenCycles();
		}
		checkOpenOrders();
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

	/**
	 * This is the heart with the matter. All trading strategy lives in this gold nugget. Too bad it currently looks
	 * more like a polished turd. TODO: Turn shit into shinola.
	 *
	 * @param stock  stock that we're buying or selling or transacting a derivative against
	 * @param action action to take based on the stock price that triggered the action
	 */
	private void takeActionOnStock(Stock stock, PriceBasedAction action) {
		LOGGER.debug("Entering takeActionOnStock()");
		String stockTicker = stock.getTicker();
		Option optionToTrade;
		try {
			if (action.isActionable()) {
				switch (action.getBuyOrSell()) {
					case "SELL":
						switch (action.getSecurityType()) {
							case CALL:
							case PUT:
								optionToTrade = Option.with(stockTicker,
										action.getSecurityType(),
										action.getTriggerPrice());
								if (optionToTrade == null) {
									LOGGER.warn("Couldn't find valid option");
									break;
								}
								portfolio.addNewOrder(
										Order.of(optionToTrade,
												market.lastTick(optionToTrade).getPrice(),
												action,
												"GFD"));
								break;
							case STOCK:
								// TODO: This.
								break;
						}
					case "BUY":
						//TODO: This.
						break;
				}
			} else {
				LOGGER.error("Do not call this method with a non-actionable action. This is an improper use of the API");
				LOGGER.error("Don't make me throw an IllegalArgumentException. Really. Don't.");
			}
		} catch (InsufficientFundsException ife) {
			LOGGER.warn("Not enough free cash to initiate order for {}, {} @ ${}",
					stockTicker, action.getBuyOrSell(), action.getNumberToTransact(), ife);
		}
	}

	private void checkOpenOrders() {
		LOGGER.debug("Entering checkOpenOrders()");
		for (Order openOrder : portfolio.getListOfOpenOrders()) {
			LOGGER.debug("Checking current open orderId {} for ticker {}", openOrder.getOrderId(), openOrder.getTicker());
			InstantPrice lastTick = market.lastTick(openOrder.getTicker());
			LOGGER.debug("{} lastTick == {}", openOrder.getTicker(), lastTick);
			if (openOrder.canBeFilled(lastTick.getPrice())) {
				LOGGER.debug("openOrder.canBeFilled({}) returned true for ticker {}", lastTick, openOrder.getTicker());
				portfolio.fillOrder(openOrder, lastTick.getPrice());
			}
		}
	}

	private void pauseBetweenCycles() {
		LOGGER.debug("Entering pauseBetweenCycles()");
		long msUntilMarketClose = market.getClosingZonedDateTime().toEpochSecond() -
				market.getCurrentZonedDateTime().toEpochSecond();
		LOGGER.debug("Comparing msUntilMarketClose {} with Constants.DELAY_MS {}", msUntilMarketClose, Constants.DELAY_MS);
		long sleepTimeMs = (msUntilMarketClose < Constants.DELAY_MS) ? msUntilMarketClose : Constants.DELAY_MS;
		if (sleepTimeMs > 0) {
			try {
				LOGGER.debug("Sleeping for {}ms ({}min)", sleepTimeMs, sleepTimeMs / 60_000);
				Thread.sleep(sleepTimeMs);
			} catch (InterruptedException ie) {
				LOGGER.warn("Interrupted exception trying to sleep {} ms", sleepTimeMs);
				LOGGER.debug("Caught (InterruptedException ie)", ie);
			}
		}
	}




	/**
	 * If within two days with expiry exercise ITM options. If underlyingTicker < optionStrike then stock is PUT to Portfolio
	 * If ticker > strike stock is CALLed away
	 * else option expires worthless, close position
	 */
	private void reconcileExpiringOptions() {
		LOGGER.debug("Entering reconcileExpiringOptions()");
		LOGGER.info("Checking for expiring options");
		LocalDate today = LocalDate.now();
		LocalDate thisFriday = today.plusDays(DayOfWeek.FRIDAY.getValue() - today.getDayOfWeek().getValue());
		int thisFridayJulian = thisFriday.getDayOfYear();
		int thisFridayYear = thisFriday.getYear();
		if (today.getDayOfWeek() == DayOfWeek.FRIDAY) {
			LOGGER.debug("Today is Friday, checking portfolio for expiring options");
			for (Position openOptionPosition : portfolio.getListOfOpenOptionPositions()) {
				LOGGER.debug("Examining positionId {} for option ticker {}", openOptionPosition.getPositionId(), openOptionPosition.getTicker());
				LOGGER.debug("Comparing Friday Julian {} to {} and year {} to {}",
						openOptionPosition.getExpiry().getDayOfYear(), thisFridayJulian, openOptionPosition.getExpiry().getYear(), thisFridayYear);
				if ((openOptionPosition.getExpiry().getDayOfYear() == thisFridayJulian) &&
						(openOptionPosition.getExpiry().getYear() == thisFridayYear)) {
					LOGGER.debug("Option expires tomorrow, checking moneyness");
					try {
						InstantPrice stockLastTick = market.lastTick(openOptionPosition.getUnderlyingTicker());
						BigDecimal stockPrice = stockLastTick.getPrice();
						if (stockPrice.compareTo(BigDecimal.ZERO) < 0) {
                     /* TODO : Need logic to handle this condition */
							throw new IOException("Foo");
						}
						if (openOptionPosition.isPut() &&
								(stockPrice.compareTo(openOptionPosition.getStrikePrice()) <= 0)) {
							portfolio.exerciseOption(openOptionPosition);
						} else if (openOptionPosition.isCall() &&
								(stockPrice.compareTo(openOptionPosition.getStrikePrice()) >= 0)) {
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
	/* Only call this method if the trading day has ended */
		LOGGER.debug("Entering TradingSession.closeGoodForDayOrders()");
		LOGGER.info("Closing GFD orders");
		for (Order checkExpiredOrder : portfolio.getListOfOpenOrders()) {
			if (checkExpiredOrder.getTif().equals("GFD")) {
				portfolio.expireOrder(checkExpiredOrder);
			}
		}
	}

	/**
	 * This method should NOT be called if options expiration occurs on a Friday when the market is closed//.
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
			if (closingPrice.getInstant().isBefore(market.getClosingZonedDateTime().toInstant())) {
				LOGGER.warn("closingPrice.getInstant() {} is before market.getClosingZonedDateTime() {}",
						closingPrice.getInstant(), market.getClosingZonedDateTime());
				return;
			}
			dataStore.writeStockPrice(stock.getTicker(), closingPrice);
		}
	}
}