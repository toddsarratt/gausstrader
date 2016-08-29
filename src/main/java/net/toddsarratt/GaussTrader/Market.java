package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;

/**
 * Provides market functions such as current and historical prices and access to exchanges for the trading of
 * securities.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.2
 */
interface Market {
	// TODO: Move this to config.properties
	ZoneId marketZone = ZoneId.of("America/New_York");

	BigDecimal getHistoricalClosingPrice(String ticker, LocalDate historicalDate);

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

	String[] priceMovingAvgs(String ticker);

	LinkedHashMap<LocalDate, BigDecimal> readHistoricalPrices(String ticker, MissingPriceDateRange dateRange);

	boolean tickerValid(String ticker);
}
