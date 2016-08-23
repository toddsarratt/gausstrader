package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
   private static todaysDateTime=new

   DateTime(DateTimeZone.forID("America/New_York")

   );
   private static int dayToday = todaysDateTime.getDayOfWeek();
   private static long marketOpenEpoch;
   private static long marketCloseEpoch;
	private static final DateTimeFormatter LAST_BAC_TICK_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyyhh:mm aa");
	private static final Logger LOGGER = LoggerFactory.getLogger(YahooMarket.class);

   @Override
   public boolean tickerValid(String ticker) {
      LOGGER.debug("Entering tickerValid(String ticker)");
      if (ticker.length() <= 4) {
         return yahooGummyApi(ticker, "e1")[0].equals("N/A");
      } else {
         return optionTickerValid(ticker);
      }
   }

   @Override
   public String[] priceMovingAvgs(String ticker) {
      return yahooGummyApi(ticker, "l1d1t1m3m4");
   }


   @Override
   public boolean marketPricesCurrent() {
   /* Get date/time for last BAC tick. Very liquid, should be representative of how current Yahoo! prices are */
      LOGGER.debug("Inside yahooPricesCurrent()");
      Instant currently = Instant.now();
      LOGGER.debug("currently = {}", currently);
      String[] yahooDateTime;
      yahooDateTime = yahooGummyApi("BAC", "d1t1");
      LOGGER.debug("yahooDateTime == {}", Arrays.toString(yahooDateTime));
      long lastBacEpoch = LAST_BAC_TICK_FORMATTER.parse(yahooDateTime[0] + yahooDateTime[1]);
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

   private static String[] yahooGummyApi(String ticker, String arguments) {
      LOGGER.debug("Entering Stock.yahooGummyApi(String {}, String {})", ticker, arguments);
      for (int yahooAttempt = 1; yahooAttempt <= Constants.YAHOO_RETRIES; yahooAttempt++) {
         try {
            URL yahooUrl = new URL("http://finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + arguments);
            BufferedReader br = new BufferedReader(new InputStreamReader(yahooUrl.openStream()));
            String[] yahooResults = br.readLine().replaceAll("[\"+%]", "").split("[,]");
            LOGGER.debug("Retrieved from Yahoo! for ticker {} with arguments {} : {}", ticker, arguments, Arrays.toString(yahooResults));
            return yahooResults;
         } catch (IOException ioe) {
            LOGGER.warn("Attempt {} : Caught IOException in .yahooGummyApi() with args {} for ticker {}", yahooAttempt, arguments, ticker);
            LOGGER.debug("Caught (IOException ioe)", ioe);
         } catch (NullPointerException npe) {                            /* Found 3/10/14 */
            LOGGER.warn("Attempt {} : Caught NullPointerException in .yahooGummyApi() with args {} for ticker {}", yahooAttempt, arguments, ticker);
            LOGGER.debug("Caught (NullPointerException)", npe);
         }
      }
      return new String[]{"No valid response from Yahoo! market"};
   }

   @Override
   public boolean wasOpen(histDateTime) {
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
      String[] tickString = yahooGummyApi(ticker, "sl1d1t1");
      if (ticker.equals(tickString[0])) {
         return InstantPrice.of(tickString[1], java.time.Instant.now());
      }
      return InstantPrice.NO_PRICE;
   }

   @Override
   public InstantPrice lastTick(Security security) {
      LOGGER.debug("Entering lastTick(Security {})", security);
      if (security.getClass().equals(Stock.class)) {
         String[] tickString = yahooGummyApi(security.getTicker(), "sl1d1t1");
         if (security.getTicker().equals(tickString[0])) {
            return InstantPrice.of(tickString[1], java.time.Instant.now());
         }
      } else if (security.getClass().equals(Option.class)) {
         return yahooOptionTick(security.getTicker());
      } else {
         throw new IllegalArgumentException("Security must be a Stock or an Option");
      }
      return InstantPrice.NO_PRICE;
   }

   private static InstantPrice yahooOptionTick(String ticker) {
      LOGGER.debug("Entering yahooOptionTick(String {})", ticker);
      /** reference: http://weblogs.java.net/blog/pat/archive/2004/10/stupid_yahooScan_1.html */
      String input;
      try {
         URL yahooUrl = new URL("http://finance.yahoo.com/q?s=" + ticker);
         URLConnection yahooUrlConnection = yahooUrl.openConnection();
         yahooUrlConnection.setReadTimeout(10);
         try (InputStream yahooInputStream = yahooUrlConnection.getInputStream();
              Scanner yahooScan = new Scanner(yahooInputStream)) {
            if (!yahooScan.hasNextLine()) {
               return InstantPrice.NO_PRICE;
            }
            input = yahooScan.useDelimiter("\\A").next();
            int tickerIndex = input.indexOf("time_rtq_ticker", 0);
            int fromIndex = input.indexOf(">", input.indexOf("<span", tickerIndex) + 4);
            int toIndex = input.indexOf("</span>", fromIndex);
            String tickPrice = input.substring(fromIndex + 1, toIndex);
            LOGGER.debug("Received last tick from Yahoo! of {}", tickPrice);
            int timeIndex = input.indexOf("time_rtq", toIndex);
            int colonIndex = input.indexOf(":", timeIndex);
            String timeString = input.substring(colonIndex - 2, colonIndex + 9)
                    .replaceAll("[<>]", "");
            LOGGER.debug("Received time from Yahoo! of {}", timeString);
            String todaysDate = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String dateTime = todaysDate + timeString;
            LOGGER.debug("Created String timeDate of {}", dateTime);
            return InstantPrice.of(tickPrice, dateTime);
         } catch (IOException | IllegalArgumentException | DateTimeParseException iid) {
            LOGGER.error("Caught exception ", iid);
            return InstantPrice.NO_PRICE;
         }
      } catch (IOException ioe2) {
         LOGGER.error("Caught exception", ioe2);
         return InstantPrice.NO_PRICE;
      }
   }

   @Override
   public InstantPrice lastBid(String ticker) {
      LOGGER.debug("Entering lastBid(String {})", ticker);
      String[] bidString = yahooGummyApi(ticker, "sb2d1t1");
      if (ticker.equals(bidString[0])) {
         return InstantPrice.of(bidString[1], java.time.Instant.now());
      }
      return InstantPrice.NO_PRICE;
   }

   @Override
   public InstantPrice lastAsk(String ticker) {
      LOGGER.debug("Entering lastAsk(String {})", ticker);
      String[] askString = yahooGummyApi(ticker, "sb3d1t1");
      if (ticker.equals(askString[0])) {
         return InstantPrice.of(askString[1], java.time.Instant.now());
      }
      return InstantPrice.NO_PRICE;
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

   private static boolean optionTickerValid(String optionTicker) {
      /**
       *  Yahoo! returns 'There are no All Markets results for [invalid_option]' in the web page of an invalid option quote
       *
       *  Changed on 11/25/13 : 'There are no results for the given search term.'
       *
       *  Additional logic 12/2/13 : Some future options will show N/A for all fields and not the
       *  error message above.
       *
       *  Change 2/4/14 : Wrote YahooFinance.isNumeric(String) to handle bad Yahoo! responses
       *
       *  Change 11/17/15 : Source code will contain "(optionTicker)" only if optionTicker is valid
       */
      LOGGER.debug("Entering optionTickerValid(String {})", optionTicker);
      String validOptionTickerFormat = "^[A-Z]{1,4}\\d{6}[CP]\\d{8}$";
      if (!optionTicker.matches(validOptionTickerFormat)) {
         return false;
      }
      String input;
      try {
         URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + optionTicker);
         try (Scanner yahooScan = new Scanner(yahoo_url.openStream())) {
            if (!yahooScan.hasNextLine()) {
               LOGGER.debug("{} is NOT a valid option ticker", optionTicker);
               return false;
            }
            input = yahooScan.useDelimiter("\\A").next();
            if (input.indexOf("(" + optionTicker + ")") > 0) {
               LOGGER.debug("{} is a valid option ticker", optionTicker);
               return true;
            }
            LOGGER.debug("{} is NOT a valid option ticker", optionTicker);
            return false;
         } catch (IOException ioe) {
            //TODO : Something!
         }
      } catch (MalformedURLException mue) {
         //TODO : Something!
      }
      return false;
   }
}
