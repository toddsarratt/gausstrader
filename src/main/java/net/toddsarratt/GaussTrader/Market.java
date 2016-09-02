package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;

/**
 * Provides market functions such as current and historical prices and access to exchanges for the trading of
 * securities.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.2
 */
interface Market {
	BigDecimal getHistoricalClosingPrice(String ticker, LocalDate historicalDate);

	BigDecimal[] getMovingAverages(String ticker);

	boolean isEarlyClose(int julianDay, int year);

	boolean isEarlyClose(LocalDate date);

	boolean isHoliday(int julianDay, int year);

	boolean isHoliday(LocalDate date);

	boolean isOpenMarketDate(LocalDate dateToCheck);

	boolean isOpenRightNow();

	boolean isOpenToday();

	InstantPrice lastAsk(String ticker);

	InstantPrice lastBid(String ticker);

	InstantPrice lastTick(Security security);

	InstantPrice lastTick(String ticker);

	boolean marketPricesCurrent();

	HashMap<LocalDate, BigDecimal> readHistoricalPrices(String ticker, LocalDate earliestDate);

	boolean tickerValid(String ticker);

	Duration timeUntilMarketOpens();
}
