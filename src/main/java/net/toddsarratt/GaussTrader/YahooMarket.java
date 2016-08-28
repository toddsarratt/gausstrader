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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * YahooMarket
 * <p>
 * http://www.gummy-stuff.org/Yahoo-data.htm moved to
 * http://www.financialwisdomforum.org/gummy-stuff/Yahoo-data.htm
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
class YahooMarket implements Market {
	private static final DateTimeFormatter LAST_BAC_TICK_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyyhh:mm aa");
	private static final Logger LOGGER = LoggerFactory.getLogger(YahooMarket.class);

	/**
	 * If ticker is 4 characters or under it is assumed to represent a stock symbol. Yahoo! API is called with the
	 * following argument:
	 * <p><pre>
	 *     e1   Error Indication (returned for symbol changed / invalid)
	 * </pre></p>
	 * If this call returns "N/A" then the stock exists (a double negative).
	 * <p>
	 * If ticker is greater than 4 characters it is assumed to represent an option. optionTickerValid() is called and
	 * its result returned.
	 *
	 * @param ticker string representing a stock or option symbol, to be checked for validity
	 * @return true if ticker represents a stock or option
	 */
	@Override
	public boolean tickerValid(String ticker) {
		LOGGER.debug("Entering tickerValid(String ticker)");
		if (ticker.length() <= 4) {
			return yahooGummyApi(ticker, "e1")[0].equals("N/A");
		} else {
			return optionTickerValid(ticker);
		}
	}

	/**
	 * Calls Yahoo! API with arguments:
	 * <p><pre>
	 *     l1   Last Trade (Price Only)
	 *     d1   Last Trade Date
	 *     t1   Last Trade Time
	 *     m3   50-day Moving Average
	 *     m4   200-day Moving Average
	 * </pre>
	 *
	 * Returns a string array in the format:
	 * TODO: fill this out
	 *
	 * @param ticker string representing a stock or option symbol
	 * @return string array with the last trade price, date, and time, along with the 50 and 200 dma
	 */
	@Override
	public String[] priceMovingAvgs(String ticker) {
		return yahooGummyApi(ticker, "l1d1t1m3m4");
	}

	/**
	 * Calls Yahoo! and finds the last time Bank of America (BAC) recorded a tick. BAC is far and away the most actively
	 * traded stock with the highest daily volume and should be representative of how current Yahoo! prices are.
	 * Returns false if last tick for BAC was over one hour ago.
	 * 
	 * @return true if Yahoo! updated BAC last tick within the last hour
	 */
	@Override
	public boolean marketPricesCurrent() {
   /* Get date/time for last BAC tick. Very liquid, should be representative of how current Yahoo! prices are */
		LOGGER.debug("Inside yahooPricesCurrent()");
		ZonedDateTime currentTime = Instant.now().atZone(marketZone);
		LOGGER.debug("currentTime = {}", currentTime);
		String[] yahooDateTime = yahooGummyApi("BAC", "d1t1");
		LOGGER.debug("yahooDateTime == {}", Arrays.toString(yahooDateTime));
		ZonedDateTime lastBacTick = ZonedDateTime.of(
				LocalDateTime.parse(yahooDateTime[0] + yahooDateTime[1], LAST_BAC_TICK_FORMATTER), marketZone);
		LOGGER.debug("lastBacTick == {}", lastBacTick);
		LOGGER.debug("Comparing currentTime {} to lastBacTick {} ", currentTime, lastBacTick);
		if (lastBacTick.isBefore(currentTime.minusHours(1))) {
			LOGGER.debug("Yahoo! last tick for BAC differs from current time by over an hour.");
			return false;
		}
		return true;
	}

	/**
	 * Uses the Yahoo! finance API, referenced here: http://www.financialwisdomforum.org/gummy-stuff/Yahoo-data.htm
	 *
	 * @param ticker stock symbol
	 * @param arguments requested data, as documented
	 * @return string array
	 */
	private static String[] yahooGummyApi(String ticker, String arguments) {
		LOGGER.debug("Entering Stock.yahooGummyApi(String {}, String {})", ticker, arguments);
		try {
			URL yahooUrl = new URL("http://finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + arguments);
			for (int yahooAttempt = 1; yahooAttempt <= Constants.YAHOO_RETRIES; yahooAttempt++) {
				try (InputStream inputStream = yahooUrl.openStream();
				     InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				     BufferedReader yahooReader = new BufferedReader(inputStreamReader)
				) {
					String[] yahooResults = yahooReader.readLine().replaceAll("[\"+%]", "").split("[,]");
					LOGGER.debug("Retrieved from Yahoo! for ticker {} with arguments {} : {}", ticker, arguments, Arrays.toString(yahooResults));
					return yahooResults;
				} catch (IOException ioe) {
					LOGGER.warn("Attempt {} : Caught IOException in yahooGummyApi()", yahooAttempt);
					LOGGER.debug("Caught (IOException ioe)", ioe);
				} catch (NullPointerException npe) {
				/* Added this logic after catching NullPointer 3/10/14 */
					LOGGER.warn("Attempt {} : Caught NullPointerException in yahooGummyApi()", yahooAttempt);
					LOGGER.debug("Caught (NullPointerException)", npe);
				}
			}
		} catch (MalformedURLException mue) {
			LOGGER.warn("Caught MalformedURLException in yahooGummyApi()");
			LOGGER.debug("Caught (MalformedURLException)", mue);
		}
		return new String[]{"No valid response from Yahoo! market"};
	}

	/**
	 * Checks if the specified date is a weekend or market holiday, in which case the market is closed on that date.
	 *
	 * @return true if the market is open on the specified date
	 */
	@Override
	public boolean isOpenMarketDate(LocalDate dateToCheck) {
		LOGGER.debug("Entering isOpenMarketDate()");
		LOGGER.debug("Comparing to list of holidays {}", Constants.HOLIDAY_MAP.entrySet());
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

	/**
	 * Checks if the day and year supplied is a market holiday, per the map of holidays created at application runtime.
	 *
	 * @param julianDay integer representing the day of the year (Julian day)
	 * @param year integer representing year in format YYYY
	 * @return true if the day specified is found in the holiday map
	 */
	@Override
	public boolean isHoliday(int julianDay, int year) {
		return Constants.HOLIDAY_MAP.get(year).contains(julianDay);
	}

	/**
	 * Checks if the LocalDate supplied are market holidays, per the map of holidays created at application runtime.
	 * 
	 * @param date LocalDate to compare against holiday map 
	 * @return true if the date specified is found in the holiday map
	 */
	@Override
	public boolean isHoliday(LocalDate date) {
		return isHoliday(date.getDayOfYear(), date.getYear());
	}

	/**
	 * Checks if the day and year supplied is an early close day for the market, per the map of early closings created 
	 * at application runtime.
	 * 
	 * @param julianDay integer representing the day of the year (Julian day)
	 * @param year integer representing year in format YYYY
	 * @return true if the day specified is found in the early close map
	 */
	@Override
	public boolean isEarlyClose(int julianDay, int year) {
		return Constants.EARLY_CLOSE_MAP.get(year).contains(julianDay);
	}

	/**
	 * Checks if the date supplied is an early close day for the market, per the map of early closings created
	 * at application runtime.
	 * 
	 * @param date LocalDate to compare against early close map
	 * @return true if the date specified is found in the holiday map
	 */
	@Override
	public boolean isEarlyClose(LocalDate date) {
		return isEarlyClose(date.getDayOfYear(), date.getYear());
	}

	/**
	 * Takes the current day in New York and checks to see if the market is open today. It may be after market close
	 * as this is a date and not time based check. If time is a consideration use isOpenRightNow()
	 *
	 * @return true if the market is open on today's date
	 */
	@Override
	public boolean isOpenToday() {
		// This method contains some DateTime objects without TZ arguments to display local time in DEBUG logs
		LOGGER.debug("Entering TradingSession.marketIsOpenToday()");
		ZonedDateTime todaysDateTime = ZonedDateTime.now(marketZone);
		return isOpenMarketDate(todaysDateTime.toLocalDate());
	}

	/**
	 * Verifies that today is not a weekend or market holiday and that it is within market trading hours.
	 *
	 * @return true if the market is open right now
	 */
	@Override
	public boolean isOpenRightNow() {
		LOGGER.debug("Inside marketIsOpenThisInstant()");
		ZonedDateTime todaysDateTime = ZonedDateTime.now(marketZone);
		//	Add 20 minutes to market open (9:30am) and close (4pm) to allow for Yahoo! 20 minute delay
		LocalTime marketOpenTime = LocalTime.of(9, 50);
		// 1:20 pm will be used for early close days
		LocalTime marketCloseTime = isEarlyClose(todaysDateTime.toLocalDate()) ?
				LocalTime.of(13, 20) : LocalTime.of(16, 20);
		LOGGER.debug("Current time = {}", );
		LOGGER.debug("Comparing currentEpoch {} to marketOpenEpoch {} and marketCloseEpoch {} ",
				todaysDateTime, marketOpenTime, marketCloseTime);
		if ((todaysDateTime.toLocalTime().isBefore(marketOpenTime))
				|| (todaysDateTime.toLocalTime().isAfter(marketCloseTime))) {
			LOGGER.debug("Outside market trading hours");
			return false;
		}
		LOGGER.debug("Within market trading hours");
		return true;
	}

	/**
	 * Gets the last tick of the stock represented by ticker.
	 *
	 * @param ticker string representing a stock ticker
	 * @return InstantPrice 
	 */
	/* TODO: NEVER RETURN NULL */
	@Override
	public InstantPrice lastTick(String ticker) {
		LOGGER.debug("Entering lastTick(String {})", ticker);
		String[] tickString = yahooGummyApi(ticker, "sl1d1t1");
		if (ticker.equals(tickString[0])) {
			return InstantPrice.of(tickString[1]);
		}
		return InstantPrice.NO_PRICE;
	}

	@Override
	public InstantPrice lastTick(Security security) {
		LOGGER.debug("Entering lastTick(Security {})", security);
		if (security.isStock()) {
			return lastTick(security.getTicker());
		} else if (security.isOption()) {
			return yahooOptionTick(security.getTicker());
		} else {
			throw new IllegalArgumentException("Security must be a Stock or an Option");
		}
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
