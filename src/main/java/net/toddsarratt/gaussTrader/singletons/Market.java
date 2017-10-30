package net.toddsarratt.gaussTrader.singletons;

import net.toddsarratt.gaussTrader.InstantPrice;
import net.toddsarratt.gaussTrader.Security;
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
public abstract class Market {
	final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Checks if the day and year supplied is a market holiday, per the map with holidays created at application runtime.
	 *
	 * @param julianDay integer representing the day with the year (Julian day)
	 * @param year      integer representing year in format YYYY
	 * @return true if the day specified is found in the holiday map
	 */
	static boolean isHoliday(int julianDay, int year) {
		return Constants.HOLIDAY_MAP.get(year).contains(julianDay);
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
		return Constants.EARLY_CLOSE_MAP.get(year).contains(julianDay);
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

	abstract LocalDateTime getCurrentDateTime();

	abstract ZonedDateTime getClosingZonedDateTime();

	abstract ZonedDateTime getCurrentZonedDateTime();

	/**
	 * Retrieves a past stock closing price.
	 *
	 * @param ticker         string representing stock symbol
	 * @param historicalDate LocalDate for the close price being requested
	 * @return BigDecimal with the closing price with the stock on the date requrested
	 */
	BigDecimal getHistoricalClosingPrice(String ticker, LocalDate historicalDate) {
		HashMap<LocalDate, BigDecimal> priceMap = readHistoricalPrices(ticker, historicalDate);
		logger.debug("Map {}", priceMap.toString());
		return priceMap.get(historicalDate);
	}

	abstract ZoneId getMarketZone();

	abstract BigDecimal[] getMovingAverages(String ticker);

	abstract String getName();

	/**
	 * Checks if the specified date is a weekend or market holiday, in which case the market is closed on that date.
	 *
	 * @return true if the market is open on the specified date
	 */
	public boolean isOpenMarketDate(LocalDate dateToCheck) {
		logger.debug("Entering isOpenMarketDate()");
		logger.debug("Comparing to list of holidays {}", Constants.HOLIDAY_MAP.entrySet());
		if (isHoliday(dateToCheck)) {
			logger.debug("{} is a market holiday.", dateToCheck);
			return false;
		}
		if ((dateToCheck.getDayOfWeek() == DayOfWeek.SATURDAY) || (dateToCheck.getDayOfWeek() == DayOfWeek.SUNDAY)) {
			logger.warn("Market is closed the weekend day of {}", dateToCheck);
			return false;
		}
		return true;
	}

	abstract boolean isOpenRightNow();

	abstract boolean isOpenToday();

	abstract InstantPrice lastAsk(String ticker);

	abstract InstantPrice lastBid(String ticker);

	abstract InstantPrice lastTick(Security security);

	abstract InstantPrice lastTick(String ticker);

	abstract boolean marketPricesCurrent();

	abstract HashMap<LocalDate, BigDecimal> readHistoricalPrices(String ticker, LocalDate earliestDate);

	abstract boolean tickerValid(String ticker);

	abstract Duration timeUntilMarketOpens();
}
