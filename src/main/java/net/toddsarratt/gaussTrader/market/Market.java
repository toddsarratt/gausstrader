package net.toddsarratt.gaussTrader.market;

import net.toddsarratt.gaussTrader.InstantPrice;
import net.toddsarratt.gaussTrader.persistence.entity.Security;
import net.toddsarratt.gaussTrader.persistence.entity.Stock;
import net.toddsarratt.gaussTrader.singletons.Constants;
import net.toddsarratt.gaussTrader.technicals.MovingAverages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.*;
import java.util.HashMap;

/**
 * Provides market functions such as current and historical prices and access to exchanges for the trading with
 * securities.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since gaussTrader v0.2
 */
public abstract class Market implements Runnable {
	final Logger LOGGER = LoggerFactory.getLogger(Market.class);

	/**
	 * Checks if the day and year supplied is a market holiday, per the map with holidays created at application runtime.
	 *
	 * @param julianDay integer representing the day with the year (Julian day)
	 * @param year      integer representing year in format YYYY
	 * @return true if the day specified is found in the holiday map
	 */
	static boolean isHoliday(int julianDay, int year) {
		return Constants.getHolidayMap().get(year).contains(julianDay);
	}

	/**
	 * Checks if the LocalDate supplied are market holidays, per the map with holidays created at application runtime.
	 *
	 * @param date LocalDate to compare against holiday map
	 * @return true if the date specified is found in the holiday map
	 */
	static boolean isHoliday(LocalDate date) {
		return isHoliday(date.getDayOfYear(), date.getYear());
	}

	/**
	 * Checks if the day and year supplied is an early close day for the market, per the map with early closings created
	 * at application runtime.
	 *
	 * @param julianDay integer representing the day with the year (Julian day)
	 * @param year      integer representing year in format YYYY
	 * @return true if the day specified is found in the early close map
	 */
	static boolean isEarlyClose(int julianDay, int year) {
		return Constants.getEarlyCloseMap().get(year).contains(julianDay);
	}

	/**
	 * Checks if the date supplied is an early close day for the market, per the map with early closings created
	 * at application runtime.
	 *
	 * @param date LocalDate to compare against early close map
	 * @return true if the date specified is found in the holiday map
	 */
	static boolean isEarlyClose(LocalDate date) {
		return isEarlyClose(date.getDayOfYear(), date.getYear());
	}

	abstract LocalTime getClosingTime();

	abstract LocalDateTime getClosingDateTime();

	public abstract LocalDateTime getCurrentDateTime();

	public abstract ZonedDateTime getClosingZonedDateTime();

	public abstract ZonedDateTime getCurrentZonedDateTime();

	/**
	 * Retrieves a past stock closing price.
	 *
	 * @param ticker         string representing stock symbol
	 * @param historicalDate LocalDate for the close price being requested
	 * @return BigDecimal with the closing price with the stock on the date requested
	 */
	public BigDecimal getHistoricalClosingPrice(String ticker, LocalDate historicalDate) {
		HashMap<LocalDate, BigDecimal> priceMap = readHistoricalPrices(ticker, historicalDate);
		LOGGER.debug("Map {}", priceMap.toString());
		return priceMap.get(historicalDate);
	}

	abstract ZoneId getMarketZone();

	public abstract MovingAverages getMovingAverages(String ticker);

	abstract String getName();

	/**
	 * Checks if the specified date is a weekend or market holiday, in which case the market is closed on that date.
	 *
	 * @return true if the market is open on the specified date
	 */
	public boolean isOpenMarketDate(LocalDate dateToCheck) {
		LOGGER.debug("Entering isOpenMarketDate()");
		LOGGER.debug("Comparing to list of holidays {}", Constants.getHolidayMap().entrySet());
		if (isHoliday(dateToCheck)) {
			LOGGER.debug("{} is a market holiday.", dateToCheck);
			return false;
		}
		if ((dateToCheck.getDayOfWeek() == DayOfWeek.SATURDAY) || (dateToCheck.getDayOfWeek() == DayOfWeek.SUNDAY)) {
			LOGGER.warn("Market is closed the weekend day of {}", dateToCheck);
			return false;
		}
		return true;
	}

	public abstract boolean isOpen();

	public abstract boolean isOpenToday();

	abstract InstantPrice lastAsk(String ticker);

	abstract InstantPrice lastBid(String ticker);

	public abstract InstantPrice getLastTick(Security security);

	abstract InstantPrice getLastTick(String ticker);

	abstract boolean marketPricesCurrent();

	public abstract HashMap<LocalDate, BigDecimal> readHistoricalPrices(String ticker, LocalDate earliestDate);

	public abstract boolean tickerValid(String ticker);

	public abstract Duration durationUntilMarketOpens();


	/**
	 * This method should NOT be called if options expiration occurs on a Friday when the market is closed//.
	 * Do not make any change to this logic assuming that it will always be run on a day when
	 * option positions have been opened, closed, or updated
	 */
	private void persistClosingPrices() {
		LOGGER.debug("Entering persistClosingPrices()");
		LOGGER.info("Writing closing prices to DB");
		InstantPrice closingPrice;
		for (Stock stock : portfolio.getWatchList()) {
			closingPrice = market.lastTick(stock);
			if (closingPrice == InstantPrice.NO_PRICE) {
				LOGGER.warn("Could not get valid price for ticker {}", stock.getTicker());
				return;
			}
			if (closingPrice.getInstant().isBefore(getClosingZonedDateTime().toInstant())) {
				LOGGER.warn("closingPrice.getInstant() {} is before market.getClosingZonedDateTime() {}",
						closingPrice.getInstant(), getClosingZonedDateTime());
				return;
			}
			dataStore.writeStockPrice(stock.getTicker(), closingPrice);
		}
	}
}
