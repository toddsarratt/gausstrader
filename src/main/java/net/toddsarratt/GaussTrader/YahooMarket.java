package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * YahooMarket uses the undocumented Yahoo! stock API and Yahoo! finance web scraping to return market results.
 * <p>
 * Reference: http://www.financialwisdomforum.org/gummy-stuff/Yahoo-data.htm
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
class YahooMarket implements Market {
	private static final String VALID_OPTION_TICKER_FORMAT = "^[A-Z]{1,4}\\d{6}[CP]\\d{8}$";
	private static final DateTimeFormatter YAHOO_API_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyyhh:mmaa");
	private static final Logger LOGGER = LoggerFactory.getLogger(YahooMarket.class);

	/**
	 * Retrieves a past stock closing price.
	 *
	 * @param ticker         string representing stock symbol
	 * @param historicalDate LocalDate for the close price being requested
	 * @return BigDecimal of the closing price of the stock on the date requrested
	 */
	@Override
	public BigDecimal getHistoricalClosingPrice(String ticker, LocalDate historicalDate) {
		MissingPriceDateRange closingDateRange = new MissingPriceDateRange(historicalDate, historicalDate);
		LinkedHashMap<LocalDate, BigDecimal> priceMap = readHistoricalPrices(ticker, closingDateRange);
		LOGGER.debug("Map {}", priceMap.toString());
		return priceMap.get(historicalDate);
	}

	/**
	 * Uses the Yahoo! finance API, referenced here: http://www.financialwisdomforum.org/gummy-stuff/Yahoo-data.htm
	 *
	 * @param ticker    stock symbol
	 * @param arguments requested data, as documented
	 * @return string array
	 */
	private static String[] yahooGummyApi(String ticker, String arguments) {
		LOGGER.debug("Entering Stock.yahooGummyApi(String {}, String {})", ticker, arguments);
		try {
			URL yahooUrl = new URL("http://finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + arguments);
			for (int yahooAttempt = 1; yahooAttempt <= Constants.MARKET_QUERY_RETRIES; yahooAttempt++) {
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
	 * @param year      integer representing year in format YYYY
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
	 * @param year      integer representing year in format YYYY
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
		ZonedDateTime todaysDateTime = ZonedDateTime.now(Constants.MARKET_ZONE);
		return isOpenMarketDate(todaysDateTime.toLocalDate());
	}

	/**
	 * Verifies that today is not a weekend or market holiday and that current NY time is within market trading hours.
	 *
	 * @return true if the market is open right now
	 */
	@Override
	public boolean isOpenRightNow() {
		LOGGER.debug("Inside marketIsOpenThisInstant()");
		ZonedDateTime todaysDateTime = ZonedDateTime.now(Constants.MARKET_ZONE);
		//	Add 20 minutes to market open (9:30am) and close (4pm) to allow for Yahoo! 20 minute delay
		LocalTime marketOpenTime = LocalTime.of(9, 50);
		// 1:20 pm will be used for early close days
		LocalTime marketCloseTime = isEarlyClose(todaysDateTime.toLocalDate()) ?
				LocalTime.of(13, 20) : LocalTime.of(16, 20);
		LOGGER.debug("Current time = {}", todaysDateTime);
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
	 * Returns an InstantPrice of the last tick of the stock represented by ticker. If the ticker is not a valid
	 * stock ticker this method returns InstantPrice.NO_PRICE.
	 * <p>
	 * TODO: Support option tickers
	 *
	 * @param ticker string representing a stock ticker
	 * @return InstantPrice of the last tick of the stock, or InstantPrice.NO_PRICE
	 */
	@Override
	public InstantPrice lastTick(String ticker) {
		LOGGER.debug("Entering lastTick(String {})", ticker);
		String[] tickString = yahooGummyApi(ticker, "sl1d1t1");
		if (ticker.equals(tickString[0])) {
			return InstantPrice.of(tickString[1], tickString[2] + tickString[3], YAHOO_API_FORMATTER, Constants.MARKET_ZONE);
		}
		return InstantPrice.NO_PRICE;
	}

	/**
	 * Returns an InstantPrice of the last tick of the security represented by ticker. If the method is unable to
	 * find a price it will return InstantPrice.NO_PRICE.
	 * <p>
	 * TODO: Move prefix and suffix into the config.properties file
	 *
	 * @param security security whose last tick is to be returned
	 * @return InstantPrice
	 */
	@Override
	public InstantPrice lastTick(Security security) {
		LOGGER.debug("Entering lastTick(Security {})", security);
		if (security.isStock()) {
			return lastTick(security.getTicker());
		} else if (security.isOption()) {
			String prefix = "},\"currency\":\"USD\",\"regularMarketPrice\":{\"raw\":";
			String suffix = ",\"";
			String scrapedPrice = yahooOptionScraper(security.getTicker(), prefix, suffix);
			LOGGER.debug("Received {} from yahooOptionScraper() ", scrapedPrice);
			if (InstantPrice.isNumeric(scrapedPrice)) {
				return InstantPrice.of(scrapedPrice);
			} else {
				return InstantPrice.NO_PRICE;
			}
		} else {
			throw new IllegalArgumentException("Security must be a Stock or an Option");
		}
	}

	/**
	 * Calls Yahoo! API with arguments:
	 * <p><pre>
	 *     s    Symbol
	 *     b    Bid
	 *     d1   Last Trade Date
	 *     t1   Last Trade Time
	 * </pre>
	 *
	 * @param ticker string representing a stock symbol
	 * @return InstantPrice of this stock's last bid, or InstantPrice.NO_PRICE if result is invalid
	 */
	@Override
	public InstantPrice lastBid(String ticker) {
		LOGGER.debug("Entering lastBid(String {})", ticker);
		String[] bidString = yahooGummyApi(ticker, "sbd1t1");
		if (ticker.equals(bidString[0])) {
			return InstantPrice.of(bidString[1], bidString[2] + bidString[3], YAHOO_API_FORMATTER, Constants.MARKET_ZONE);
		}
		return InstantPrice.NO_PRICE;
	}

	/**
	 * Calls Yahoo! API with arguments:
	 * <p><pre>
	 *     s	Symbol
	 *     a    Ask
	 *     d1	Last Trade Date
	 *     t1	Last Trade Time
	 * </pre>
	 *
	 * @param ticker string representing a stock symbol
	 * @return InstantPrice of this stock's last bid, or InstantPrice.NO_PRICE if result is invalid
	 */
	@Override
	public InstantPrice lastAsk(String ticker) {
		LOGGER.debug("Entering lastAsk(String {})", ticker);
		String[] askString = yahooGummyApi(ticker, "sad1t1");
		if (ticker.equals(askString[0])) {
			return InstantPrice.of(askString[1], askString[2] + askString[3], YAHOO_API_FORMATTER, Constants.MARKET_ZONE);
		}
		return InstantPrice.NO_PRICE;
	}

	/**
	 * Retrieves a series of stock closing prices from Yahoo!. Returns a HashMap of the closing epoch and the
	 * adjusted close price.
	 *
	 * @param ticker    string representing the stock represented by this ticker
	 * @param earliestDate LocalDate of the earliest missing prive from the data store
	 * @return HashMap of missing prices
	 */
	@Override
	public HashMap<LocalDate, BigDecimal> readHistoricalPrices(String ticker, LocalDate earliestDate) {
		LOGGER.debug("Entering retrieveYahooHistoricalPrices()");
		LinkedHashMap<LocalDate, BigDecimal> yahooPriceReturns = new LinkedHashMap<>();
		String inputLine;
		try {
			final URL yahooUrl = new URL(createYahooHistUrl(ticker, earliestDate));
			BufferedReader yahooBufferedReader = new BufferedReader(new InputStreamReader(yahooUrl.openStream()));
			/* First line is not added to array : "	Date,Open,High,Low,Close,Volume,Adj Close" so we swallow it
			* in a log entry where it looks nice. */
			LOGGER.debug(yahooBufferedReader.readLine().replace("Date,", "Date         ").replaceAll(",", "    "));
			while ((inputLine = yahooBufferedReader.readLine()) != null) {
				String[] yahooLine = inputLine.replaceAll("[\"+%]", "").split("[,]");
				LOGGER.debug(Arrays.toString(yahooLine));
				LocalDate yahooDate = LocalDate.parse(yahooLine[0]);
				BigDecimal closePrice = new BigDecimal(yahooLine[6]);
				yahooPriceReturns.put(yahooDate, closePrice);
			}
			return yahooPriceReturns;
		} catch (IOException ioe) {
			LOGGER.warn("Caught IOException in readHistoricalPrices()");
			LOGGER.debug("Caught (IOException ioe)", ioe);
		}
		return new LinkedHashMap<>(Collections.EMPTY_MAP);
	}

	/**
	 * Creates an URL that can be used to retrieve a list of historical closing stock prices from Yahoo!.
	 *
	 * @param ticker    stock ticker to retrieve prices for
	 * @param earlyDate MissingPriceDateRange of prices needed
	 * @return string representing an URL to retrieve historical prices
	 */
	private static String createYahooHistUrl(String ticker, LocalDate earlyDate) {
		LOGGER.debug("Entering createYahooHistUrl()");
		LocalDate today = LocalDate.now(Constants.MARKET_ZONE);
		StringBuilder yahooPriceArgs = new StringBuilder("http://ichart.finance.yahoo.com/table.csv?s=");
		yahooPriceArgs.append(ticker)
				.append("&a=").append(earlyDate.getMonthValue() - 1)
				.append("&b=").append(earlyDate.getDayOfMonth())
				.append("&c=").append(earlyDate.getYear())
				.append("&d=").append(today.getMonthValue() - 1)
				.append("&e=").append(today.getDayOfMonth())
				.append("&f=").append(today.getYear())
				.append("&g=d&ignore=.csv");
		LOGGER.debug("yahooPriceArgs = {}", yahooPriceArgs);
		return yahooPriceArgs.toString();
	}

	/**
	 * Scrapes Yahoo! for option ticker. If it can't find it assumes it is not valid.
	 *
	 * @param optionTicker string representing an option ticker to check for validity
	 * @return true if this option ticker is found on Yahoo!
	 */
	private static boolean optionTickerValid(String optionTicker) {
		LOGGER.debug("Entering optionTickerValid(String {})", optionTicker);
		if (!optionTicker.matches(VALID_OPTION_TICKER_FORMAT)) {
			return false;
		}
		String prefix = "$main-0-Quote-Proxy.$main-0-Quote.0.1.0\">";
		String suffix = "</p>";
		String price = yahooOptionScraper(optionTicker, prefix, suffix);
		return InstantPrice.isNumeric(price);
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
		ZonedDateTime currentTime = Instant.now().atZone(Constants.MARKET_ZONE);
		LOGGER.debug("currentTime = {}", currentTime);
		String[] yahooDateTime = yahooGummyApi("BAC", "d1t1");
		LOGGER.debug("yahooDateTime == {}", Arrays.toString(yahooDateTime));
		ZonedDateTime lastBacTick = ZonedDateTime.of(
				LocalDateTime.parse(yahooDateTime[0] + yahooDateTime[1], YAHOO_API_FORMATTER), Constants.MARKET_ZONE);
		LOGGER.debug("lastBacTick == {}", lastBacTick);
		LOGGER.debug("Comparing currentTime {} to lastBacTick {} ", currentTime, lastBacTick);
		if (lastBacTick.isBefore(currentTime.minusHours(1))) {
			LOGGER.debug("Yahoo! last tick for BAC differs from current time by over an hour.");
			return false;
		}
		return true;
	}

	/**
	 * Calls Yahoo! API with arguments:
	 * <p><pre>
	 *     m3   50-day Moving Average
	 *     m4   200-day Moving Average
	 * </pre>
	 * <p>
	 * Returns a BigDecimal array of the form [BigDecimal(50 dma), BigDecimal(200 dma)]
	 *
	 * @param ticker string representing a stock or option symbol
	 * @return BigDecimal array of the 50 and 200 daily moving averages
	 */
	@Override
	public BigDecimal[] getMovingAverages(String ticker) {
		return Arrays.stream(yahooGummyApi(ticker, "m3m4")).toArray(BigDecimal[]::new);
	}

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
	 * While Yahoo! provides a handy (if undocumented) API for stock information it does not appear to offer a similar
	 * API for options. The current solution is to web scrape Yahoo! finance pages for the information. This method
	 * takes the option ticker as its first parameter. The other two parameters define strings surrounding the
	 * information that needs to be scraped out. The programmer (me, as it turns out) must view source of the web page
	 * and tease out these boundaries and then change the code when the boundaries change.
	 *
	 * @param optionTicker ticker of the option we need to web scrape
	 * @param prefix       boundary for the beginning of the information needed
	 * @param suffix       boundary for the end of the information
	 * @return string of the information being web scraped
	 */
	private static String yahooOptionScraper(String optionTicker, String prefix, String suffix) {
		LOGGER.debug("Entering yahooOptionScraper(String {}, String {}, String {})",
				optionTicker, prefix, suffix);
		try {
			URL yahooUrl = new URL("http://finance.yahoo.com/quote/" + optionTicker);
			LOGGER.debug("Calling to URL: {}", yahooUrl);
			try (InputStream inputStream = yahooUrl.openStream();
			     InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			     BufferedReader yahooReader = new BufferedReader(inputStreamReader)
			) {
				StringBuilder yahooSourceBuilder = new StringBuilder();
				String yahooLine;
				while ((yahooLine = yahooReader.readLine()) != null) {
					yahooSourceBuilder.append(yahooLine);
				}
				String yahooSource = yahooSourceBuilder.toString();
				int scrapeStart = yahooSource.indexOf(prefix);
				LOGGER.debug("scrapeStart = {}", scrapeStart);
				int scrapeFrom = scrapeStart + prefix.length();
				LOGGER.debug("scrapeFrom = {}", scrapeFrom);
				int scrapeTo = yahooSource.indexOf(suffix, scrapeFrom);
				LOGGER.debug("scrapeTo = {}", scrapeTo);
				String finalScrape = yahooSource.substring(scrapeFrom, scrapeTo);
				LOGGER.debug("Scraped from Yahoo! : {}", finalScrape);
				return finalScrape;
			} catch (IOException ioe) {
				LOGGER.warn("Attempt {} : Caught IOException in yahooOptionTick()");
				LOGGER.debug("Caught (IOException ioe)", ioe);
				return "";
			}
		} catch (MalformedURLException mue) {
			LOGGER.warn("Caught MalformedURLException in yahooOptionTick()");
			LOGGER.debug("Caught (MalformedURLException)", mue);
			return "";
		}
	}
}
