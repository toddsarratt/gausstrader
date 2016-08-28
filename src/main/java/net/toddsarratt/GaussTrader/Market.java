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

	ZoneId marketZone = ZoneId.of("America/New_York");

	LinkedHashMap<Long, BigDecimal> readHistoricalPrices(String ticker, MissingPriceDateRange dateRange);

	boolean isOpenToday();

	InstantPrice lastTick(String ticker);

	boolean tickerValid(String ticker);

	boolean marketPricesCurrent();

	boolean isOpenMarketDate(LocalDate dateToCheck);

	boolean isOpenRightNow();

	boolean isHoliday(int julianDay, int year);

	boolean isHoliday(LocalDate date);

	boolean isEarlyClose(int julianDay, int year);

	boolean isEarlyClose(LocalDate date);

	InstantPrice lastBid(String ticker);

	InstantPrice lastAsk(String ticker);

	String[] priceMovingAvgs(String ticker);

	/* TODO: NEVER RETURN NULL */
	InstantPrice lastTick(Security security);
}
