package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

/**
 * YahooMarket
 * <p>
 * http://www.gummy-stuff.org/Yahoo-data.htm
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class YahooMarket implements Market {
   private static final Logger LOGGER = LoggerFactory.getLogger(YahooMarket.class);

   @Override
   public boolean marketPricesCurrent() {
   /* Get date/time for last BAC tick. Very liquid, should be representative of how current Yahoo! prices are */
      LOGGER.debug("Inside yahooPricesCurrent()");
      long currentEpoch = System.currentTimeMillis();
      LOGGER.debug("currentEpoch = {}", currentEpoch);
      String[] yahooDateTime;
      try {
         yahooDateTime = askYahoo("BAC", "d1t1");
         LOGGER.debug("yahooDateTime == {}", Arrays.toString(yahooDateTime));
         long lastBacEpoch = Constants.LAST_BAC_TICK_FORMATTER.parseMillis(yahooDateTime[0] + yahooDateTime[1]);
         LOGGER.debug("lastBacEpoch == {}", lastBacEpoch);
         LOGGER.debug("Comparing currentEpoch {} to lastBacEpoch {} ", currentEpoch, lastBacEpoch);
         LOGGER.debug("LAST_BAC_TICK_FORMATTER.print(currentEpoch) {} vs. LAST_BAC_TICK_FORMATTER.print(lastBacEpoch) {}",
                 Constants.LAST_BAC_TICK_FORMATTER.print(currentEpoch), Constants.LAST_BAC_TICK_FORMATTER.print(lastBacEpoch));
         if ((lastBacEpoch < (currentEpoch - 3600000)) || (lastBacEpoch > (currentEpoch + 3600000))) {
            LOGGER.debug("Yahoo! last tick for BAC differs from current time by over an hour.");
            return false;
         }
      } catch (IOException ioe) {
         LOGGER.info("Could not connect to Yahoo! to check for current prices (BAC last tick)");
         LOGGER.debug("Caught (IOException ioe)", ioe);
         return false;
      }
      return true;
   }

   private static String[] askYahoo(String ticker, String arguments) throws IOException {
      LOGGER.debug("Entering Stock.askYahoo(String {}, String {})", ticker, arguments);
      for (int yahooAttempt = 1; yahooAttempt <= Constants.YAHOO_RETRIES; yahooAttempt++) {
         try {
            final URL YAHOO_URL = new URL("http://finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + arguments);
            BufferedReader br = new BufferedReader(new InputStreamReader(YAHOO_URL.openStream()));
            String[] yahooResults = br.readLine().replaceAll("[\"+%]", "").split("[,]");
            LOGGER.debug("Retrieved from Yahoo! for ticker {} with arguments {} : {}", ticker, arguments, Arrays.toString(yahooResults));
            return yahooResults;
         } catch (IOException ioe) {
            LOGGER.warn("Attempt {} : Caught IOException in .askYahoo() with args {} for ticker {}", yahooAttempt, arguments, ticker);
            LOGGER.debug("Caught (IOException ioe)", ioe);
         } catch (NullPointerException npe) {                            /* Found 3/10/14 */
            LOGGER.warn("Attempt {} : Caught NullPointerException in .askYahoo() with args {} for ticker {}", yahooAttempt, arguments, ticker);
            LOGGER.debug("Caught (NullPointerException)", npe);
         }
      }
      throw new IOException();
   }

   @Override
   public boolean isHoliday(int julianDay, int year) {
      return Constants.HOLIDAY_MAP.get(year).contains(julianDay);
   }

   @Override
   public boolean isHoliday(ReadableDateTime date) {
      return isHoliday(date.getDayOfYear(), date.getYear());
   }

   @Override
   public boolean isEarlyClose(int julianDay, int year) {
      return Constants.EARLY_CLOSE_MAP.get(year).contains(julianDay);
   }

   @Override
   public boolean isEarlyClose(ReadableDateTime date) {
      return isEarlyClose(date.getDayOfYear(), date.getYear());
   }

   /**
    * This method contains DateTime constructors without TZ arguments to display local time in DEBUG logs
    */
   private static boolean isOpenToday() {
      LOGGER.debug("Entering TradingSession.marketIsOpenToday()");
      LOGGER.debug("julianToday = {}", todaysDateTime.getDayOfYear());
      LOGGER.debug("Comparing to list of holidays {}", Constants.HOLIDAY_MAP.entrySet());
      if (isMarketHoliday(todaysDateTime)) {
         LOGGER.debug("Market is on holiday.");
         return false;
      }
      LOGGER.debug("dayToday = todaysDateTime.getDayOfWeek() = {} ({})", dayToday, todaysDateTime.dayOfWeek().getAsText());
      if ((dayToday == DateTimeConstants.SATURDAY) || (dayToday == DateTimeConstants.SUNDAY)) {
         LOGGER.warn("It's the weekend. Market is closed.");
         return false;
      }
      marketOpenEpoch = todaysDateTime.withTime(9, 30, 0, 0).getMillis();
      LOGGER.debug("marketOpenEpoch = {} ({})", marketOpenEpoch, new DateTime(marketOpenEpoch));
      LOGGER.debug("Checking to see if today's julian date {} matches early close list {}", todaysDateTime.getDayOfYear(), EARLY_CLOSE_MAP.entrySet());
      if (isEarlyClose(todaysDateTime)) {
         LOGGER.info("Today the market closes at 1pm New York time");
         marketCloseEpoch = todaysDateTime.withTime(13, 0, 0, 0).getMillis();
         LOGGER.debug("marketCloseEpoch = {}", marketCloseEpoch, new DateTime(marketCloseEpoch));
      } else {
         LOGGER.info("Market closes at usual 4pm New York time");
         marketCloseEpoch = todaysDateTime.withTime(16, 0, 0, 0).getMillis();
         LOGGER.debug("marketCloseEpoch = {} ({})", marketCloseEpoch, todaysDateTime.withHourOfDay(16));
      }
      /* If using Yahoo! quotes, account for 20min delay */
      LOGGER.debug("GaussTrader.delayedQuotes == {}", Constants.DELAYED_QUOTES);
      if (Constants.DELAYED_QUOTES) {
         LOGGER.debug("Adding Yahoo! quote delay of 20 minutes to market open and close times");
         marketOpenEpoch += (20 * 60 * 1000);
         LOGGER.debug("Updated marketOpenEpoch == {} ({})", marketOpenEpoch, new MutableDateTime(marketOpenEpoch));
         marketCloseEpoch += (20 * 60 * 1000);
         LOGGER.debug("Updated marketCloseEpoch == {} ({})", marketCloseEpoch, new MutableDateTime(marketCloseEpoch));
      }
      return true;
   }

   private static boolean isOpenThisInstant() {
      LOGGER.debug("Inside marketIsOpenThisInstant()");
      long currentEpoch = System.currentTimeMillis();
      LOGGER.debug("currentEpoch = {}", currentEpoch);
      LOGGER.debug("Comparing currentEpoch {} to marketOpenEpoch {} and marketCloseEpoch {} ", currentEpoch, marketOpenEpoch, marketCloseEpoch);
      if ((currentEpoch >= marketOpenEpoch) && (currentEpoch <= marketCloseEpoch)) {
         LOGGER.debug("marketIsOpenThisInstant() returning true");
         return true;
      }
      LOGGER.debug("marketIsOpenThisInstant() returning false");
      return false;
   }

   @Override
   public InstantPrice lastTick(String ticker) throws IOException {
      LOGGER.debug("Entering lastTick(String {})", ticker);
      String[] tickString = askYahoo(ticker, "sl1d1t1");
      if (ticker.equals(tickString[0])) {
         return InstantPrice.of(tickString[1], System.currentTimeMillis());
         return Double.parseDouble(tickString[1]);
      }
      return -1;
   }
}
