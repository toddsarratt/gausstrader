package net.toddsarratt.gaussTrader.portfolio;

import net.toddsarratt.gaussTrader.ActionProcessor;
import net.toddsarratt.gaussTrader.TradingStrategy;
import net.toddsarratt.gaussTrader.market.Market;
import net.toddsarratt.gaussTrader.persistence.dao.PortfolioDao;
import net.toddsarratt.gaussTrader.persistence.entity.InstantPrice;
import net.toddsarratt.gaussTrader.persistence.entity.Order;
import net.toddsarratt.gaussTrader.persistence.entity.Portfolio;
import net.toddsarratt.gaussTrader.persistence.entity.Position;
import net.toddsarratt.gaussTrader.persistence.store.DataStore;
import net.toddsarratt.gaussTrader.singletons.Constants;
import net.toddsarratt.gaussTrader.strategy.OptionWritesAgainstBollingerBands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public class PortfolioManager implements Runnable, Subscriber<Order> {
	private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioManager.class);

	private Market market;
	private Portfolio portfolio;
	private DataStore dataStore;
	private PortfolioAccountant portfolioAccountant;
	private ActionProcessor actionProcessor;
	private TradingStrategy tradingStrategy;

	public PortfolioManager() {
		portfolio = retrievePortfolio();
		market = retrieveMarket();
		dataStore = retrieveDataStore();
		portfolioAccountant = retrievePortfolioAccountant();
		tradingStrategy = retrieveTradingStrategy();
		actionProcessor = retireveActionProcesser();
		actionProcessor.subscribe(this);
		tradingStrategy.subscribe(actionProcessor);
	}

	private DataStore retrieveDataStore() {
		return null;
	}

	private Market retrieveMarket() {
		return null;
	}

	private Portfolio retrievePortfolio() {
		PortfolioDao portfolioDao = new PortfolioDao();
		String portfolioName = Constants.getPortfolioName();
		return portfolioDao.read(portfolioName);
	}

	private TradingStrategy retrieveTradingStrategy() {
//		TradingStrategyDao tradingStrategyDao = new TradingStrategyDao();
//		String tradingStrategyName = Constants.getTradingStrategyName();
//		return tradingStrategyDao.read(tradingStrategyName);
		return new OptionWritesAgainstBollingerBands();
	}

	private PortfolioAccountant retrievePortfolioAccountant() {
		return new PortfolioAccountant(portfolio, market);
	}

	private ActionProcessor retireveActionProcesser() {
		return new ActionProcessor();
	}

	@Override
	public void run() {
		if (Objects.isNull(market)) {
			throw new IllegalStateException("PortfolioManager cannot be started with a null market");
		}
		if (Objects.isNull(portfolio)) {
			throw new IllegalStateException("PortfolioManager cannot be started with a null portfolio");
		}
		if (Objects.isNull(dataStore)) {
			throw new IllegalStateException("PortfolioManager cannot be started with a null dataStore");
		}
		if (market.isOpenToday()) {
			LOGGER.info("Market is open today.");
			sleepUntilMarketOpen();
			tradeUntilMarketClose();
			// End of day tasks
			closeGoodForDayOrders();
			persistClosingPrices();
		} else {
			LOGGER.info("Market is closed today.");
		}
		reconcileExpiringOptions();
		persistPortfolio();
		LOGGER.info("End trading day.");
	}

	private void persistPortfolio() {
//		portfolio.calcPortfolioNav();
//		dataStore.write(portfolio.getSummary());
	}

	private void sleepUntilMarketOpen() {
		Duration durationUntilMarketOpens = market.durationUntilMarketOpens();
		LOGGER.debug("Sleeping until market opens in {}h {}m {}s",
				durationUntilMarketOpens.toHours(),
				durationUntilMarketOpens.toMinutes(),
				durationUntilMarketOpens.toSeconds());
		try {
			Thread.sleep(durationUntilMarketOpens.toMillis());
		} catch (InterruptedException ie) {
			LOGGER.warn("Caught InterruptedException sleeping until market open", ie);
		}
	}

	private void tradeUntilMarketClose() {
		while (market.isOpen()) {
			// TODO: Executor needed here
			portfolio.getTradingStrategy().run();
			portfolio.updateOptionPositions();
			portfolio.updateStockPositions();
			portfolio.calculateNetAssetValue();
			dataStore.write(portfolio.getSummary());
			pauseBetweenCycles();
		}
		checkOpenOrders();
	}


	private void checkOpenOrders() {
		LOGGER.debug("Entering checkOpenOrders()");
		for (Order openOrder : portfolio.getListOfOpenOrders()) {
			LOGGER.debug("Checking current open orderId {} for ticker {}", openOrder.getOrderId(), openOrder.getSecurity().getTicker());
			InstantPrice lastTick = market.lastTick(openOrder.getSecurity());
			LOGGER.debug("{} lastTick == {}", openOrder.getSecurity().getTicker(), lastTick);
			if (openOrder.canBeFilled(lastTick.getPrice())) {
				LOGGER.debug("openOrder.canBeFilled({}) returned true for ticker {}", lastTick, openOrder.getSecurity().getTicker());
				portfolio.fillOrder(openOrder, lastTick.getPrice());
			}
		}
	}

	private void pauseBetweenCycles() {
		LOGGER.debug("Entering pauseBetweenCycles()");
		long msUntilMarketClose = market.getClosingZonedDateTime().toEpochSecond() -
				market.getCurrentZonedDateTime().toEpochSecond();
		LOGGER.debug("Comparing msUntilMarketClose {} with Constants.DELAY_MS {}", msUntilMarketClose, Constants.DELAY_MS);
		long sleepTimeMs = Math.min(msUntilMarketClose, Constants.DELAY_MS);
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
						InstantPrice stockLastTick = market.lastTick(openOptionPosition.getSecurity());
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


	public Market getMarket() {
		return market;
	}

	public void setMarket(Market market) {
		this.market = market;
	}

	public PortfolioAccountant getPortfolio() {
		return portfolio;
	}

	public void setPortfolio(PortfolioAccountant portfolioAccountant) {
		this.portfolio = portfolioAccountant;
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	/**
	 * Method invoked prior to invoking any other Subscriber
	 * methods for the given Subscription. If this method throws
	 * an exception, resulting behavior is not guaranteed, but may
	 * cause the Subscription not to be established or to be cancelled.
	 * <p>
	 * <p>Typically, implementations of this method invoke {@code
	 * subscription.request} to enable receiving items.
	 *
	 * @param subscription a new subscription
	 */
	@Override
	public void onSubscribe(Subscription subscription) {
		LOGGER.debug("onSubscribe()");
	}

	/**
	 * Method invoked with a Subscription's next item.  If this
	 * method throws an exception, resulting behavior is not
	 * guaranteed, but may cause the Subscription to be cancelled.
	 *
	 * @param order the item
	 */
	@Override
	public void onNext(Order order) {
		if (orderMeetsPortfolioRules(order)) {
			portfolio.add(order);
		}
	}

	/**
	 * Method invoked upon an unrecoverable error encountered by a
	 * Publisher or Subscription, after which no other Subscriber
	 * methods are invoked by the Subscription.  If this method
	 * itself throws an exception, resulting behavior is
	 * undefined.
	 *
	 * @param throwable the exception
	 */
	@Override
	public void onError(Throwable throwable) {
		LOGGER.error("onError()", throwable);
	}

	/**
	 * Method invoked when it is known that no additional
	 * Subscriber method invocations will occur for a Subscription
	 * that is not already terminated by error, after which no
	 * other Subscriber methods are invoked by the Subscription.
	 * If this method throws an exception, resulting behavior is
	 * undefined.
	 */
	@Override
	public void onComplete() {
		LOGGER.debug("onComplete()");
	}
}
