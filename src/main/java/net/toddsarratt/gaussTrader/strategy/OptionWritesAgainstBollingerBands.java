package net.toddsarratt.gaussTrader.strategy;

import net.toddsarratt.gaussTrader.InsufficientFundsException;
import net.toddsarratt.gaussTrader.PriceBasedAction;
import net.toddsarratt.gaussTrader.TradingStrategy;
import net.toddsarratt.gaussTrader.domain.Option;
import net.toddsarratt.gaussTrader.domain.Stock;
import net.toddsarratt.gaussTrader.persistence.entity.InstantPrice;
import net.toddsarratt.gaussTrader.persistence.entity.Order;
import net.toddsarratt.gaussTrader.portfolio.PortfolioAccountant;
import net.toddsarratt.gaussTrader.singletons.Constants;
import net.toddsarratt.gaussTrader.singletons.SecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public class OptionWritesAgainstBollingerBands implements TradingStrategy {
	private static final Logger LOGGER = LoggerFactory.getLogger(OptionWritesAgainstBollingerBands.class);

	private static final String SHORT_NAME = "BigMoney";

	private static PriceBasedAction createCallAction(Stock stock, BigDecimal stockPrice, PortfolioAccountant portfolioAccountant) {
		LOGGER.debug("Entering createCallAction(Stock {}, BigDecimal {})", stock.getTicker(), stockPrice);
		if (portfolioAccountant.countUncoveredLongStockPositions(stock) < 1) {
			LOGGER.info("Open long {} positions is equal or less than current short calls positions. Taking no action.", stock.getTicker());
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(2)) >= 0) {
			LOGGER.info("Stock {} at ${} is above 2nd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(2));
			return new PriceBasedAction(stockPrice,
					true,
					"SELL",
					SecurityType.CALL,
					Math.min(portfolioAccountant.countOfOpenLongStockShares(stock), 5));
		}
		/* TODO : Consider removing this if statement. We should not be in this method if the condition wasn't met, though
		 * it does protect against misuse of the API. Also, why is the PriceBasedAction exactly the same as above? */
		if (stockPrice.compareTo(stock.getBollingerBand(1)) >= 0) {
			LOGGER.info("Stock {} at ${} is above 1st Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(1));
			return new PriceBasedAction(stockPrice,
					true,
					"SELL",
					SecurityType.CALL,
					Math.min(portfolioAccountant.countOfOpenLongStockShares(stock), 5));
		}
		return PriceBasedAction.DO_NOTHING;
	}

	private static PriceBasedAction createPutAction(Stock stock, BigDecimal stockPrice, PortfolioAccountant portfolioAccountant) {
		LOGGER.debug("Entering createPutAction(Stock {}, BigDecimal {})", stock.getTicker(), stockPrice);
		int openPutShorts = portfolioAccountant.numberOfOpenPutShorts(stock);
		int maximumContracts = Constants.STOCK_PCT_OF_PORTFOLIO.divide(Constants.BIGDECIMAL_ONE_HUNDRED, 3, RoundingMode.HALF_UP)
				.divide(stockPrice.multiply(Constants.BIGDECIMAL_ONE_HUNDRED), 3, RoundingMode.HALF_UP)
				.multiply(portfolioAccountant.calcPortfolioNav())
				.intValue();
		if (stockPrice.compareTo(stock.getBollingerBand(5)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 3rd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(5));
			if (openPutShorts < maximumContracts) {
				return new PriceBasedAction(stockPrice, true, "SELL", SecurityType.PUT, Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(4)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 2nd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(4));
			if (openPutShorts < maximumContracts / 2) {
				return new PriceBasedAction(stockPrice, true, "SELL", SecurityType.PUT, Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
			return PriceBasedAction.DO_NOTHING;
		}
		/* TODO : Consider removing this if statement. We should not be in this method if the condition wasn't met */
		if (stockPrice.compareTo(stock.getBollingerBand(3)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 1st Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(3));
			if (openPutShorts < maximumContracts / 4) {
				return new PriceBasedAction(stockPrice, true, "SELL", SecurityType.PUT, Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
		}
		return PriceBasedAction.DO_NOTHING;
	}

	/**
	 * Decide if a security's current price triggers a predetermined event. For this trading strategy, a stock price
	 * following below its Bollinger band triggers the sale of a put. A stock price above its Bollinger band triggers
	 * the sale of a covered call.
	 *
	 * @param stock               stock whose price may trigger an action
	 * @param stockPrice          last known price to compare against stock's bollinger bands
	 * @param portfolioAccountant portfolio being traded
	 * @return PriceBasedAction based on input parameters and trade strategy
	 */
	public static PriceBasedAction findActionToTake(Stock stock, BigDecimal stockPrice, PortfolioAccountant portfolioAccountant) {
		LOGGER.debug("Entering findActionToTake(Stock {})", stock.getTicker());
		LOGGER.debug("Comparing current price ${} against Bollinger Bands {}", stockPrice, stock.describeBollingerBands());
		if (stockPrice.compareTo(stock.getBollingerBand(1)) >= 0) {
			return createCallAction(stock, stockPrice, portfolioAccountant);
		}
		if (stock.getFiftyDma().compareTo(stock.getTwoHundredDma()) < 0) {
			LOGGER.info("Stock {} 50DMA < 200DMA. No further checks.", stock.getTicker());
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(3)) <= 0) {
			return createPutAction(stock, stockPrice, portfolioAccountant);
		}
		LOGGER.info("Stock {} at ${} is within Bollinger Bands", stock.getTicker(), stockPrice);
		return PriceBasedAction.DO_NOTHING;
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
		try {
			if (action.isActionable()) {
				switch (action.getBuyOrSell()) {
					case SELL:
						switch (action.getSecurityType()) {
							case CALL:
							case PUT:
								Option optionToTrade = Option.with(stock, action);
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
					case BUY:
						//TODO: This.
						break;
				}
			} else {
				LOGGER.error("Do not call this method with a non-actionable action. This is an improper use of the API");
			}
		} catch (InsufficientFundsException ife) {
			LOGGER.warn("Not enough free cash to initiate order for {}, {} @ ${}",
					stock.getTicker(), action.getBuyOrSell(), action.getNumberToTransact(), ife);
		}
	}

	/**
	 * When an object implementing interface <code>Runnable</code> is used
	 * to create a thread, starting the thread causes the object's
	 * <code>run</code> method to be called in that separately executing
	 * thread.
	 * <p>
	 * The general contract of the method <code>run</code> is that it may
	 * take any action whatsoever.
	 *
	 * @see Thread#run()
	 */
	@Override
	public void run() {
		Iterator<Stock> stockIterator = portfolio.getWatchList().iterator();
		// if the end of the list is reached, start from the beginning with a new iterator
		if (!stockIterator.hasNext()) {
			stockIterator = portfolio.getWatchList().iterator();
			if (!stockIterator.hasNext()) {
				LOGGER.warn("No stocks to trade");
				break;
			}
		}
		Stock stock = stockIterator.next();
		InstantPrice currentInstantPrice = market.lastTick(stock);
		BigDecimal stockPrice = currentInstantPrice.getPrice();
		dataStore.writeStockPrice(stock.getTicker(), currentInstantPrice);
		PriceBasedAction actionToTake = TradingStrategy.findActionToTake(stock, stockPrice, portfolio);
		if (actionToTake.isActionable()) {
			takeActionOnStock(stock, actionToTake);
		}

	}

	/**
	 * Adds the given Subscriber if possible.  If already
	 * subscribed, or the attempt to subscribe fails due to policy
	 * violations or errors, the Subscriber's {@code onError}
	 * method is invoked with an {@link IllegalStateException}.
	 * Otherwise, the Subscriber's {@code onSubscribe} method is
	 * invoked with a new {@link Subscription}.  Subscribers may
	 * enable receiving items by invoking the {@code request}
	 * method of this Subscription, and may unsubscribe by
	 * invoking its {@code cancel} method.
	 *
	 * @param subscriber the subscriber
	 * @throws NullPointerException if subscriber is null
	 */
	@Override
	public void subscribe(Subscriber subscriber) {

	}
}
