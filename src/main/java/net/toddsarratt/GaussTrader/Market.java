package net.toddsarratt.GaussTrader;

import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

/**
 * Provides market functions such as current and historical prices and access to exchanges for the trading of
 * securities.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.2
 */
public interface Market {

   LinkedHashMap<Long, BigDecimal> readHistoricalPrices(String ticker, MissingPriceDateRange dateRange);

   boolean isOpenToday();

   InstantPrice lastTick(String ticker);

   boolean tickerValid(String ticker);

   boolean marketPricesCurrent();

   boolean wasOpen(MutableDateTime histDateTime);

   boolean isHoliday(int julianDay, int year);

   boolean isHoliday(ReadableDateTime date);

   boolean isEarlyClose(int julianDay, int year);

   boolean isEarlyClose(ReadableDateTime date);

   InstantPrice lastBid(String ticker);

   InstantPrice lastAsk(String ticker);

   String[] priceMovingAvgs(String ticker);

   /* TODO: NEVER RETURN NULL */
   InstantPrice lastTick(Security security);
}
