package net.toddsarratt.GaussTrader;

import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

/**
 * YahooMarket
 * <p>
 * http://www.gummy-stuff.org/Yahoo-data.htm moved to
 * http://www.financialwisdomforum.org/gummy-stuff/Yahoo-data.htm
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class YahooMarket implements Market {
   private static DateTime todaysDateTime = new DateTime(DateTimeZone.forID("America/New_York"));
   private static int dayToday = todaysDateTime.getDayOfWeek();
   private static long marketOpenEpoch;
   private static long marketCloseEpoch;
   private static final Logger LOGGER = LoggerFactory.getLogger(YahooMarket.class);

   @Override
   public boolean tickerValid(String ticker) {
      LOGGER.debug("Entering tickerValid(String ticker)");
      return askYahoo(ticker, "e1")[0].equals("N/A");
   }

   @Override
   public String[] priceMovingAvgs(String ticker) {
      return askYahoo(ticker, "l1d1t1m3m4");
   }


   @Override
   public boolean marketPricesCurrent() {
   /* Get date/time for last BAC tick. Very liquid, should be representative of how current Yahoo! prices are */
      LOGGER.debug("Inside yahooPricesCurrent()");
      long currentEpoch = System.currentTimeMillis();
      LOGGER.debug("currentEpoch = {}", currentEpoch);
      String[] yahooDateTime;
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
      return true;
   }

   private static String[] askYahoo(String ticker, String arguments) {
      LOGGER.debug("Entering Stock.askYahoo(String {}, String {})", ticker, arguments);
      for (int yahooAttempt = 1; yahooAttempt <= Constants.YAHOO_RETRIES; yahooAttempt++) {
         try {
            URL yahooUrl = new URL("http://finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + arguments);
            BufferedReader br = new BufferedReader(new InputStreamReader(yahooUrl.openStream()));
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
      return new String[]{"No valid response from Yahoo! market"};
   }

   @Override
   public boolean wasOpen(MutableDateTime histDateTime) {
      return !(isHoliday(histDateTime) ||
              (histDateTime.getDayOfWeek() == DateTimeConstants.SATURDAY) ||
              (histDateTime.getDayOfWeek() == DateTimeConstants.SUNDAY));
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
   @Override
   public boolean isOpenToday() {
      LOGGER.debug("Entering TradingSession.marketIsOpenToday()");
      LOGGER.debug("julianToday = {}", todaysDateTime.getDayOfYear());
      LOGGER.debug("Comparing to list of holidays {}", Constants.HOLIDAY_MAP.entrySet());
      if (isHoliday(todaysDateTime)) {
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
      LOGGER.debug("Checking to see if today's julian date {} matches early close list {}",
              todaysDateTime.getDayOfYear(), Constants.EARLY_CLOSE_MAP.entrySet());
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

   /* TODO: NEVER RETURN NULL */
   @Override
   public InstantPrice lastTick(String ticker) {
      LOGGER.debug("Entering lastTick(String {})", ticker);
      String[] tickString = askYahoo(ticker, "sl1d1t1");
      if (ticker.equals(tickString[0])) {
         return InstantPrice.of(tickString[1], java.time.Instant.now());
      }
      return InstantPrice.NO_PRICE;
   }

   @Override
   public InstantPrice lastBid(String ticker) {
      LOGGER.debug("Entering lastBid(String {})", ticker);
      String[] bidString = askYahoo(ticker, "sb2d1t1");
      if (ticker.equals(bidString[0])) {
         return InstantPrice.of(bidString[1], java.time.Instant.now());
      }
      return InstantPrice.NO_PRICE;
   }

   @Override
   public InstantPrice lastAsk(String ticker) {
      LOGGER.debug("Entering lastAsk(String {})", ticker);
      String[] askString = askYahoo(ticker, "sb3d1t1");
      if (ticker.equals(askString[0])) {
         return InstantPrice.of(askString[1], java.time.Instant.now());
      }
      return InstantPrice.NO_PRICE;
   }

   public HashMap<Long, BigDecimal> readHistoricalPrices(Stock stock) {
   /* historicalPriceMap was built from PRICE_TRACKING_MAP which contains all necessary epochs as keys and -1.0 for every value,
    * which should be replaced first from the local database first and then supplemented by Yahoo!
	 * TODO: The incomplete statement below will not stand, man
	 * This method returns a map of blahblahblah
	 */
      LOGGER.debug("Entering populateHistoricalPricesYahoo()");
      MissingPriceDateRange priceRangeToDownload;
      LinkedHashMap<Long, BigDecimal> retrievedYahooPriceMap;
      HashMap<Long, BigDecimal> pricesMissingFromDB = new HashMap<>();

      if (historicalPriceMap.containsValue(BigDecimal.valueOf(-1.0))) {
         LOGGER.debug("Calculating date range for missing stock prices.");
         priceRangeToDownload = getMissingPriceDateRange();
         if (!priceRangeToDownload.earliest.isAfter(priceRangeToDownload.latest.toInstant())) {
            try {

            } catch (IOException ioe) {
               LOGGER.warn("Could not connect to Yahoo! to get historical prices");
               LOGGER.debug("Caught (IOException ioe) {}", ioe);
            }
         } else {
            LOGGER.warn("historicalPriceMap.containsValue(-1.0) but " +
                            "priceRangeToDownload.earliest.isAfter(priceRangeToDownload.latest.toInstant() ({} after {})",
                    priceRangeToDownload.earliest.toString(), priceRangeToDownload.latest.toString());
         }
      } else {
         LOGGER.debug("historicalPriceMap.containsValue(-1.0) is false, all needed prices have been retrieved from the database. Not calling Yahoo!");
      }
      return pricesMissingFromDB;
   }

   @Override
   public LinkedHashMap<Long, BigDecimal> readHistoricalPrices(String ticker, MissingPriceDateRange dateRange) {
      LOGGER.debug("Entering YahooFinance.retrieveYahooHistoricalPrices(MissingPriceDateRange dateRange)");
      LinkedHashMap<Long, BigDecimal> yahooPriceReturns = new LinkedHashMap<>();
      String inputLine;
      try {
         final URL YAHOO_URL = new URL(createYahooHistUrl(ticker, dateRange));
         BufferedReader yahooBufferedReader = new BufferedReader(new InputStreamReader(YAHOO_URL.openStream()));
         /* First line is not added to array : "	Date,Open,High,Low,Close,Volume,Adj Close" */
         LOGGER.debug(yahooBufferedReader.readLine().replace("Date,", "Date         ").replaceAll(",", "    "));
         while ((inputLine = yahooBufferedReader.readLine()) != null) {
            String[] yahooLine = inputLine.replaceAll("[\"+%]", "").split("[,]");
            LOGGER.debug(Arrays.toString(yahooLine));
            HistoricalPrice yahooHistPrice = new HistoricalPrice(yahooLine[0], yahooLine[6]);
            yahooPriceReturns.put(yahooHistPrice.getDateEpoch(), yahooHistPrice.getAdjClose());
         }
         return yahooPriceReturns;
      } catch (IOException ioe) {
         //TODO : HANDLE THIS!
      }
      return new LinkedHashMap<>(Collections.EMPTY_MAP);
   }

   /**
    * http://ichart.finance.yahoo.com/table.csv?s=INTC&a=11&b=1&c=2012&d=00&e=21&f=2013&g=d&ignore=.csv
    * where month January = 00
    */
   private static String createYahooHistUrl(String ticker, MissingPriceDateRange dateRange) {
      LOGGER.debug("Entering YahooFinance.createYahooHistUrl(MissingPriceDateRange dateRange)");
      StringBuilder yahooPriceArgs = new StringBuilder("http://ichart.finance.yahoo.com/table.csv?s=");
      yahooPriceArgs.append(ticker).append("&a=").append(dateRange.earliest.getMonthOfYear() - 1).append("&b=").append(dateRange.earliest.getDayOfMonth());
      yahooPriceArgs.append("&c=").append(dateRange.earliest.getYear()).append("&d=").append(dateRange.latest.getMonthOfYear() - 1);
      yahooPriceArgs.append("&e=").append(dateRange.latest.getDayOfMonth()).append("&f=").append(dateRange.latest.getYear());
      yahooPriceArgs.append("&g=d&ignore=.csv");
      LOGGER.debug("yahooPriceArgs = {}", yahooPriceArgs);
      return yahooPriceArgs.toString();
   }

}
