package net.toddsarratt.GaussTrader;

import org.joda.time.ReadableDateTime;

import java.io.IOException;

/**
 * Provides market functions such as current and historical prices and access to exchanges for the trading of
 * securities.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.2
 */
public interface Market {

   InstantPrice lastTick(String ticker) throws IOException;

   boolean marketPricesCurrent();

   boolean isHoliday(int julianDay, int year);

   boolean isHoliday(ReadableDateTime date);

   boolean isEarlyClose(int julianDay, int year);

   boolean isEarlyClose(ReadableDateTime date);
}
