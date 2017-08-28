package net.toddsarratt.GaussTrader;

import net.toddsarratt.GaussTrader.Security.Security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
class YahooMarket extends Market {
	//	Add 20 minutes to market open (9:30am) to allow for Yahoo! 20 minute delay
	private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 50);
	private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
	private static final String VALID_OPTION_TICKER_FORMAT = "^[A-Z]{1,4}\\d{6}[CP]\\d{8}$";
	private static final DateTimeFormatter YAHOO_API_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyyhh:mmaa");
	private static LocalTime marketClosingTime;

	static {
		if (isEarlyClose(LocalDate.now())) {
			marketClosingTime = LocalTime.of(13, 20);
		} else {
			marketClosingTime = LocalTime.of(16, 20);
		}
	}

	/**
	 * Returns the market closing time. Generally 4pm America/New_York plus 20min for Yahoo! delay. A few days each
	 * year the market closes early at 1pm America/New_York (usually July 3rd and the day after Thanksgiving).
	 *
	 * @return market close time
	 */
	@Override
	public LocalTime getClosingTime() {
		return marketClosingTime;
	}

	/**
	 * Adds the current date to the market closing time to return a LocalDateTime
	 *
	 * @return LocalDateTime of today's date and market close time
	 */
	@Override
	public LocalDateTime getClosingDateTime() {
		return LocalDateTime.now().with(getClosingTime());
	}

	/**
	 * Calls Yahoo! and finds the last trade date and time of Bank with America (BAC) stock. BAC is far and away the most
	 * actively traded stock with the highest daily volume and should be representative of current Yahoo! market time.
	 * Yahoo! API arguments:
	 * <p><pre>
	 *     d1   Last Trade Date
	 *     t1   Last Trade Time
	 * </pre>
	 *
	 * @return LocalDateTime parsed from last trade date and time of BAC
	 */
	@Override
	public LocalDateTime getCurrentDateTime() {
		String[] yahooDateTime = yahooGummyApi("BAC", "d1t1");
		logger.debug("yahooDateTime == {}", Arrays.toString(yahooDateTime));
		return LocalDateTime.parse(yahooDateTime[0] + yahooDateTime[1], YAHOO_API_FORMATTER);
	}

	@Override
	public ZonedDateTime getClosingZonedDateTime() {
		return getClosingDateTime().atZone(MARKET_ZONE);
	}

	@Override
	public ZonedDateTime getCurrentZonedDateTime() {
		return getCurrentDateTime().atZone(MARKET_ZONE);
	}

	@Override
	public ZoneId getMarketZone() {
		return MARKET_ZONE;
	}

	/**
	 * Calls Yahoo! API with arguments:
	 * <p><pre>
	 *     m3   50-day Moving Average
	 *     m4   200-day Moving Average
	 * </pre>
	 * <p>
	 * Returns a BigDecimal array with the form [BigDecimal(50 dma), BigDecimal(200 dma)]
	 *
	 * @param ticker string representing a stock or option symbol
	 * @return BigDecimal array with the 50 and 200 daily moving averages
	 */
	@Override
	public BigDecimal[] getMovingAverages(String ticker) {
		return Arrays.stream(yahooGummyApi(ticker, "m3m4")).toArray(BigDecimal[]::new);
	}

	/**
	 * This class contains methods to access Yahoo! specifically for market information and returns a market name
	 * of "Yahoo! market"
	 *
	 * @return string with market name, "Yahoo! market"
	 */
	@Override
	public String getName() {
		return "Yahoo! market";
	}

	/**
	 * Verifies that today is not a weekend or market holiday and that current NY time is within market trading hours.
	 *
	 * @return true if the market is open right now
	 */
	@Override
	public boolean isOpenRightNow() {
		logger.debug("Inside marketIsOpenThisInstant()");
		ZonedDateTime todaysDateTime = ZonedDateTime.now(MARKET_ZONE);
		//	Add 20 minutes to market close (4pm) to allow for Yahoo! 20 minute delay
		// 1:20 pm will be used for early close days
		LocalTime marketCloseTime = isEarlyClose(todaysDateTime.toLocalDate()) ?
				LocalTime.of(13, 20) : LocalTime.of(16, 20);
		logger.debug("Current time = {}", todaysDateTime);
		logger.debug("Comparing currentEpoch {} to marketOpenEpoch {} and marketCloseEpoch {} ",
				todaysDateTime, MARKET_OPEN_TIME, marketCloseTime);
		if ((todaysDateTime.toLocalTime().isBefore(MARKET_OPEN_TIME))
				|| (todaysDateTime.toLocalTime().isAfter(marketCloseTime))) {
			logger.debug("Outside market trading hours");
			return false;
		}
		logger.debug("Within market trading hours");
		return true;
	}

	/**
	 * Takes the current day in New York and checks to see if the market is open today. It may be after market close
	 * as this is a date and not time based check. If time is a consideration use isOpenRightNow()
	 *
	 * @return true if the market is open on today's date
	 */
	@Override
	boolean isOpenToday() {
		ZonedDateTime todaysDateTime = ZonedDateTime.now(MARKET_ZONE);
		return isOpenMarketDate(todaysDateTime.toLocalDate());
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
	 * @return InstantPrice with this stock's last bid, or InstantPrice.NO_PRICE if result is invalid
	 */
	@Override
	public InstantPrice lastAsk(String ticker) {
		logger.debug("Entering lastAsk(String {})", ticker);
		String[] askString = yahooGummyApi(ticker, "sad1t1");
		if (ticker.equals(askString[0])) {
			return InstantPrice.of(askString[1], askString[2] + askString[3], YAHOO_API_FORMATTER, MARKET_ZONE);
		}
		return InstantPrice.NO_PRICE;
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
	 * @return InstantPrice with this stock's last bid, or InstantPrice.NO_PRICE if result is invalid
	 */
	@Override
	public InstantPrice lastBid(String ticker) {
		logger.debug("Entering lastBid(String {})", ticker);
		String[] bidString = yahooGummyApi(ticker, "sbd1t1");
		if (ticker.equals(bidString[0])) {
			return InstantPrice.of(bidString[1], bidString[2] + bidString[3], YAHOO_API_FORMATTER, MARKET_ZONE);
		}
		return InstantPrice.NO_PRICE;
	}

	/**
	 * Returns an InstantPrice with the last tick with the security represented by ticker. If the method is unable to
	 * find a price it will return InstantPrice.NO_PRICE.
	 * <p>
	 * TODO: Move prefix and suffix into the config.properties file
	 *
	 * @param security security whose last tick is to be returned
	 * @return InstantPrice
	 */
	@Override
	public InstantPrice lastTick(Security security) {
		logger.debug("Entering lastTick(Security {})", security);
		if (security.isStock()) {
			return lastTick(security.getTicker());
		} else if (security.isOption()) {
			String prefix = "},\"currency\":\"USD\",\"regularMarketPrice\":{\"raw\":";
			String suffix = ",\"";
			String scrapedPrice = yahooOptionScraper(security.getTicker(), prefix, suffix);
			logger.debug("Received {} from yahooOptionScraper() ", scrapedPrice);
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
	 * Returns an InstantPrice with the last tick with the stock represented by ticker. If the ticker is not a valid
	 * stock ticker this method returns InstantPrice.NO_PRICE.
	 * <p>
	 * TODO: Support option tickers
	 *
	 * @param ticker string representing a stock ticker
	 * @return InstantPrice with the last tick with the stock, or InstantPrice.NO_PRICE
	 */
	@Override
	public InstantPrice lastTick(String ticker) {
		logger.debug("Entering lastTick(String {})", ticker);
		String[] tickString = yahooGummyApi(ticker, "sl1d1t1");
		if (ticker.equals(tickString[0])) {
			return InstantPrice.of(tickString[1], tickString[2] + tickString[3], YAHOO_API_FORMATTER, MARKET_ZONE);
		}
		return InstantPrice.NO_PRICE;
	}

	/**
	 * Returns false if last tick for BAC was over one hour ago.
	 *
	 * @return true if Yahoo! updated BAC last tick within the last hour
	 */
	@Override
	public boolean marketPricesCurrent() {
   /* Get date/time for last BAC tick. Very liquid, should be representative of how current Yahoo! prices are */
		logger.debug("Inside yahooPricesCurrent()");
		ZonedDateTime currentTime = Instant.now().atZone(MARKET_ZONE);
		logger.debug("currentTime = {}", currentTime);
		ZonedDateTime lastBacTick = ZonedDateTime.of(getCurrentDateTime(), MARKET_ZONE);
		logger.debug("lastBacTick == {}", lastBacTick);
		logger.debug("Comparing currentTime {} to lastBacTick {} ", currentTime, lastBacTick);
		if (lastBacTick.isBefore(currentTime.minusHours(1))) {
			logger.debug("Yahoo! last tick for BAC differs from current time by over an hour.");
			return false;
		}
		return true;
	}

	/**
	 * Retrieves a series of stock closing prices from Yahoo!. Returns a HashMap with the closing epoch and the
	 * adjusted close price.
	 *
	 * @param ticker       string representing the stock represented by this ticker
	 * @param earliestDate LocalDate of the earliest missing price from the data store
	 * @return HashMap of <date, missing price>
	 */
	@Override
	public HashMap<LocalDate, BigDecimal> readHistoricalPrices(String ticker, LocalDate earliestDate) {
		logger.debug("Entering retrieveYahooHistoricalPrices()");
		LinkedHashMap<LocalDate, BigDecimal> yahooPriceReturns = new LinkedHashMap<>();
		String inputLine;
		try {
			final URL yahooUrl = new URL(createYahooHistUrl(ticker, earliestDate));
			BufferedReader yahooBufferedReader = new BufferedReader(new InputStreamReader(yahooUrl.openStream()));
			/* First line is not added to array : "	Date,Open,High,Low,Close,Volume,Adj Close" so we swallow it
			* in a log entry where it looks nice. */
			logger.debug(yahooBufferedReader.readLine().replace("Date,", "Date         ").replaceAll(",", "    "));
			while ((inputLine = yahooBufferedReader.readLine()) != null) {
				String[] yahooLine = inputLine.replaceAll("[\"+%]", "").split("[,]");
				logger.debug(Arrays.toString(yahooLine));
				LocalDate yahooDate = LocalDate.parse(yahooLine[0]);
				BigDecimal closePrice = new BigDecimal(yahooLine[6]);
				yahooPriceReturns.put(yahooDate, closePrice);
			}
			return yahooPriceReturns;
		} catch (IOException ioe) {
			logger.warn("Caught IOException in readHistoricalPrices()");
			logger.debug("Caught (IOException ioe)", ioe);
		}
		return new LinkedHashMap<>(Collections.EMPTY_MAP);
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
		logger.debug("Entering tickerValid(String ticker)");
		if (ticker.length() <= 4) {
			return yahooGummyApi(ticker, "e1")[0].equals("N/A");
		} else {
			return optionTickerValid(ticker);
		}
	}

	@Override
	public Duration timeUntilMarketOpens() {
		logger.debug("Entering timeUntilMarketOpens()");
		return Duration.between(MARKET_OPEN_TIME, LocalTime.from(Instant.now().atZone(MARKET_ZONE)));
	}

	/**
	 * Uses the Yahoo! finance API, referenced here: http://www.financialwisdomforum.org/gummy-stuff/Yahoo-data.htm
	 *
	 * @param ticker    stock symbol
	 * @param arguments requested data, as documented
	 * @return string array of Yahoo! results
	 */
	private String[] yahooGummyApi(String ticker, String arguments) {
		logger.debug("Entering yahooGummyApi(String {}, String {})", ticker, arguments);
		try {
			URL yahooUrl = new URL("http://finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + arguments);
			for (int yahooAttempt = 1; yahooAttempt <= Constants.MARKET_QUERY_RETRIES; yahooAttempt++) {
				try (InputStream inputStream = yahooUrl.openStream();
				     InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				     BufferedReader yahooReader = new BufferedReader(inputStreamReader)
				) {
					String[] yahooResults = yahooReader.readLine().replaceAll("[\"+%]", "").split("[,]");
					logger.debug("Retrieved from Yahoo! for ticker {} with arguments {} : {}",
							ticker, arguments, Arrays.toString(yahooResults));
					return yahooResults;
				} catch (IOException ioe) {
					logger.warn("Attempt {} : Caught IOException in yahooGummyApi()", yahooAttempt);
					logger.debug("", ioe);
				} catch (NullPointerException npe) {
				/* yahooReader.readLine() may return null */
					logger.warn("Attempt {} : Caught NullPointerException in yahooGummyApi()", yahooAttempt);
					logger.debug("", npe);
				}
			}
		} catch (MalformedURLException mue) {
			logger.warn("Caught MalformedURLException in yahooGummyApi()");
			logger.debug("", mue);
		}
		return new String[]{"No valid response from Yahoo! market"};
	}

	/**
	 * Creates an URL that can be used to retrieve a list of historical closing stock prices from Yahoo!.
	 *
	 * @param ticker    stock ticker to retrieve prices for
	 * @param earlyDate LocalDate of earliest price needed
	 * @return string representing an URL to retrieve historical prices
	 */
	private String createYahooHistUrl(String ticker, LocalDate earlyDate) {
		logger.debug("Entering createYahooHistUrl()");
		LocalDate today = LocalDate.now(MARKET_ZONE);
		StringBuilder yahooPriceArgs = new StringBuilder("http://ichart.finance.yahoo.com/table.csv?s=");
		yahooPriceArgs.append(ticker)
				.append("&a=").append(earlyDate.getMonthValue() - 1)
				.append("&b=").append(earlyDate.getDayOfMonth())
				.append("&c=").append(earlyDate.getYear())
				.append("&d=").append(today.getMonthValue() - 1)
				.append("&e=").append(today.getDayOfMonth())
				.append("&f=").append(today.getYear())
				.append("&g=d&ignore=.csv");
		logger.debug("yahooPriceArgs = {}", yahooPriceArgs);
		return yahooPriceArgs.toString();
	}

	/**
	 * Scrapes Yahoo! for option ticker. If it can't find it assumes it is not valid.
	 *
	 * @param optionTicker string representing an option ticker to check for validity
	 * @return true if this option ticker is found on Yahoo!
	 */
	private boolean optionTickerValid(String optionTicker) {
		logger.debug("Entering optionTickerValid(String {})", optionTicker);
		if (!optionTicker.matches(VALID_OPTION_TICKER_FORMAT)) {
			return false;
		}
		String prefix = "$main-0-Quote-Proxy.$main-0-Quote.0.1.0\">";
		String suffix = "</p>";
		String price = yahooOptionScraper(optionTicker, prefix, suffix);
		return InstantPrice.isNumeric(price);
	}

	/**
	 * While Yahoo! provides a handy (if undocumented) API for stock information it does not appear to offer a similar
	 * API for options. The current solution is to web scrape Yahoo! finance pages for the information. This method
	 * takes the option ticker as its first parameter. The other two parameters define strings surrounding the
	 * information that needs to be scraped out. The programmer (me, as it turns out) must view source of the web page
	 * and tease out these boundaries and then change the code when the boundaries change.
	 *
	 * @param optionTicker ticker of the option to web scrape
	 * @param prefix       boundary for the beginning of the information needed
	 * @param suffix       boundary for the end of the information
	 * @return string of the information being web scraped
	 */
	private String yahooOptionScraper(String optionTicker, String prefix, String suffix) {
		logger.debug("Entering yahooOptionScraper(String {}, String {}, String {})",
				optionTicker, prefix, suffix);
		try {
			URL yahooUrl = new URL("http://finance.yahoo.com/quote/" + optionTicker);
			logger.debug("Calling to URL: {}", yahooUrl);
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
				logger.debug("scrapeStart = {}", scrapeStart);
				int scrapeFrom = scrapeStart + prefix.length();
				logger.debug("scrapeFrom = {}", scrapeFrom);
				int scrapeTo = yahooSource.indexOf(suffix, scrapeFrom);
				logger.debug("scrapeTo = {}", scrapeTo);
				String finalScrape = yahooSource.substring(scrapeFrom, scrapeTo);
				logger.debug("Scraped from Yahoo! : {}", finalScrape);
				return finalScrape;
			} catch (IOException ioe) {
				logger.warn("Attempt {} : Caught IOException in yahooOptionTick()");
				logger.debug("Caught (IOException ioe)", ioe);
				return "";
			}
		} catch (MalformedURLException mue) {
			logger.warn("Caught MalformedURLException in yahooOptionTick()");
			logger.debug("Caught (MalformedURLException)", mue);
			return "";
		}
	}
}
