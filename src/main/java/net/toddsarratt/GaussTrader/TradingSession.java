package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class TradingSession {
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
			dataStore.writeStockPrice(stock.getTicker(), stockPrice);
			PriceBasedAction actionToTake = findActionToTake(stock, stockPrice);
			if (actionToTake.doSomething()) {
				takeActionOnStock(stock, actionToTake);
			}
			try {
				portfolio.updateOptionPositions(stock);
				portfolio.updateStockPositions(stock);
			} catch(IOException | SQLException e) {
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

	private void takeActionOnStock(Stock stock, PriceBasedAction action) {
		LOGGER.debug("Entering takeActionOnStock()");
		String ticker = stock.getTicker();
		if(action.doSomething()) {

				Option optionToSell = Option.getOption(ticker, action.g, currentPrice);
				if (optionToSell == null) {
				/*Handle bad option searching */

				} else {
					try {
						portfolio.addNewOrder(new Order(optionToSell, optionToSell.lastBid(), "SELL", action.contractsToTransact, "GFD"));
					} catch (InsufficientFundsException ife) {
						LOGGER.warn("Not enough free cash to initiate order for {} @ ${}", optionToSell.getTicker(), optionToSell.lastBid(), ife);
					}
				}
			}
		}

	}

	private void checkOpenOrders() {
		LOGGER.debug("Entering TradingSession.checkOpenOrders()");
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

	private PriceBasedAction findActionToTake(Stock stock, BigDecimal stockPrice) {
	    /* Decide if a security's current price triggers a predetermined event */
		LOGGER.debug("Entering findActionToTake(Stock {})", stock.getTicker());
		LOGGER.debug("Comparing current price ${} against Bollinger Bands {}", stockPrice, stock.describeBollingerBands());
		if (stockPrice.compareTo(stock.getBollingerBand(1)) >= 0) {
			return createCallAction(stock, stockPrice);
		}
		if (stock.getFiftyDma().compareTo(stock.getTwoHundredDma()) < 0) {
			LOGGER.info("Stock {} 50DMA < 200DMA. No further checks.", stock.getTicker());
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(3)) <= 0) {
			return createPutAction(stock, stockPrice);
		}
		LOGGER.info("Stock {} at ${} is within Bollinger Bands", stock.getTicker(), stockPrice);
		return PriceBasedAction.DO_NOTHING;
	}

	private PriceBasedAction createCallAction(Stock stock, BigDecimal stockPrice) {
		LOGGER.debug("Entering createCallAction(Stock {}, BigDecimal {})", stock.getTicker(), stockPrice);
		if (portfolio.countUncoveredLongStockPositions(stock) < 1) {
			LOGGER.info("Open long {} positions is equal or less than current short calls positions. Taking no action.", stock.getTicker());
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(2)) >= 0) {
			LOGGER.info("Stock {} at ${} is above 2nd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(2));
			return new PriceBasedAction(true, "SELL", "CALL", Math.min(portfolio.numberOfOpenStockLongs(stock), 5));
		}
      /* TODO : Consider removing this if statement. We should not be in this method if the condition wasn't met */
		if (stockPrice.compareTo(stock.getBollingerBand(1)) >= 0) {
			LOGGER.info("Stock {} at ${} is above 1st Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(1));
			return new PriceBasedAction(true, "SELL", "CALL", Math.min(portfolio.numberOfOpenStockLongs(stock), 5));
		}
		return PriceBasedAction.DO_NOTHING;
	}

	private PriceBasedAction createPutAction(Stock stock, BigDecimal stockPrice) {
		LOGGER.debug("Entering createPutAction(Stock {}, BigDecimal {})", stock.getTicker(), stockPrice);
		int openPutShorts = portfolio.numberOfOpenPutShorts(stock);
		int maximumContracts = Constants.STOCK_PCT_OF_PORTFOLIO.divide(Constants.BIGDECIMAL_ONE_HUNDRED)
				.divide(stockPrice.multiply(Constants.BIGDECIMAL_ONE_HUNDRED))
				.multiply(portfolio.calculateNetAssetValue())
				.intValue();
		if (stockPrice.compareTo(stock.getBollingerBand(5)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 3rd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(5));
			if (openPutShorts < maximumContracts) {
				return new PriceBasedAction(true, "SELL", "PUT", Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(4)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 2nd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(4));
			if (openPutShorts < maximumContracts / 2) {
				return new PriceBasedAction(true, "SELL", "PUT", Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
			return PriceBasedAction.DO_NOTHING;
		}
      /* TODO : Consider removing this if statement. We should not be in this method if the condition wasn't met */
		if (stockPrice.compareTo(stock.getBollingerBand(3)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 1st Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(3));
			if (openPutShorts < maximumContracts / 4) {
				return new PriceBasedAction(true, "SELL", "PUT", Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
		}
		return PriceBasedAction.DO_NOTHING;
	}

	/**
	 * If within two days of expiry exercise ITM options. If underlyingTicker < optionStrike then stock is PUT to Portfolio
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
			if (closingPrice.getInstant().isBefore(market.getClosingTime)) {
				LOGGER.warn("stock.getLastPriceUpdateEpoch() {} < earliestAcceptableLastPriceUpdateEpoch {}",
						stock.getLastPriceUpdateEpoch(), earliestAcceptableLastPriceUpdateEpoch);
				return;
			}
			dataStore.writeStockPrice(stock.getTicker(), closingPrice);
		}
	}
}