package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Root Symbol + Expiration Year(yy) + Expiration Month(mm) + Expiration Day(dd) + Call/Put Indicator (C or P) +
 * Strike Price Dollars + Strike Price Fraction with Dollars (which can include decimals)
 * Read more: http://www.investopedia.com/articles/optioninvestor/10/options-symbol-rules.asp#ixzz2IOZQ4kou
 */

abstract class Option implements Security {
	private final static Market MARKET = GaussTrader.getMarket();
	private static final Logger LOGGER = LoggerFactory.getLogger(Option.class);
	private final String ticker;
	private final SecurityType secType;
	private final LocalDate expiry;
	private final String underlyingTicker;
	private final BigDecimal strike;

	Option(String ticker,
	       SecurityType secType,
	       LocalDate expiry,
	       String underlyingTicker,
	       BigDecimal strike) {
		this.ticker = ticker;
		this.secType = secType;
		this.expiry = expiry;
		this.underlyingTicker = underlyingTicker;
		this.strike = strike;
	}

	public static Option with(String ticker) {
		// Receive an option ticker such as : XOM130720P00070000
		LOGGER.debug("Entering static constructor with(String {})", ticker);
		if (!MARKET.tickerValid(ticker)) {
			throw new IllegalArgumentException("Invalid option ticker");
		}
		Pattern pattern = Pattern.compile("^[A-Z](1,4)");
		Matcher matcher = pattern.matcher(ticker);
		String underlyingTicker = matcher.group(0);
		pattern = Pattern.compile("\\d{6}");
		matcher = pattern.matcher(ticker);
		DateTimeFormatter expiryFormat = DateTimeFormatter.ofPattern("yyMMddHH");
		// "+17" means tack on hour 17, 5pm expiration time for options
		LocalDate expiry = LocalDate.parse(matcher.group(0) + "17", expiryFormat);
		pattern = Pattern.compile("\\d[CP]\\d");
		matcher = pattern.matcher(ticker);
		char[] optionTypeMatchArray = matcher.group(0).toCharArray();
		char optionType = optionTypeMatchArray[1];
		SecurityType secType = (optionType == 'C') ? SecurityType.CALL : (optionType == 'P') ? SecurityType.PUT : null;
		if (secType == null) {
			LOGGER.warn(" {}", optionType);
			throw new IllegalArgumentException("Invalid parsing of option symbol. Expecting C or P (put or call), retrieved : " + optionType);
		}
		pattern = Pattern.compile("\\d{8}");
		matcher = pattern.matcher(ticker);
		BigDecimal strike = new BigDecimal(matcher.group(0)).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
		LOGGER.info("Created {} option {} for underlying {} expiry {} for strike ${}",
				secType, ticker, underlyingTicker, expiry.format(expiryFormat), strike);
		if (secType.equals(SecurityType.PUT)) {
			return new Put(ticker, expiry, underlyingTicker, strike);
		} else {
			return new Call(ticker, expiry, underlyingTicker, strike);
		}
	}

	/**
	 * This method replaces deprecated method getExpirySaturday(). As of 2/2015 options expire on Friday instead of
	 * Saturday. See http://www.cboe.com/aboutcboe/xcal2015.pdf;  expiration date is now the third Friday of the month.
	 *
	 * @param month month with the expiration
	 * @param year  year with the expiration
	 * @return the expiration date's day with the month [15 - 21]
	 */
	private static int calculateFutureExpiry(int month, int year) {
		return 21 - LocalDate.of(year, month, 2)
				.getDayOfWeek()
				.getValue()
				% 7;
	}

	/**
	 * Build option ticker. Example : Exxon Mobil Aug 17 2013 expiry call 90 Strike = XOM130817C00090000
	 */
	private static String createOptionTicker(String stockTicker, LocalDate expiry, char indicator, BigDecimal strike) {
		LOGGER.debug("Entering createOptionTicker(String {}, BaseDateTime {}, char {}, BigDecimal {})",
				stockTicker, expiry.toString(), indicator, strike);
		StringBuilder tickerBuilder = new StringBuilder(stockTicker);
	/* Option strike format is xxxxx.yyy * 10^3 Example : Strike $82.50 = 00082500 */
		tickerBuilder.append(expiry.format(DateTimeFormatter.ofPattern("yyMMdd")));
		LOGGER.debug("Assembling option ticker with {} (expiry : {})",
				expiry.format(DateTimeFormatter.ofPattern("yyMMdd")),
				expiry.format(DateTimeFormatter.ofPattern("MMMM dd YYYY")));
		tickerBuilder.append(indicator);
		int strikeInt = (strike.multiply(BigDecimal.valueOf(1000))).intValue();
		tickerBuilder.append(String.format("%08d", strikeInt));
		LOGGER.debug("Returning assembled option ticker {}", tickerBuilder.toString());
		return tickerBuilder.toString();
	}

	static Option with(String stockTicker, SecurityType optionType, BigDecimal limitStrikePrice) {
		LOGGER.debug("Entering with(String {}, String {}, double {})", stockTicker, optionType, limitStrikePrice);
		String optionTickerToTry;
		LocalDate expiry;
		ZonedDateTime currentZonedDateTime = MARKET.getCurrentZonedDateTime();
		int expirationSaturday = calculateFutureExpiry(currentZonedDateTime.getMonth().getValue(),
				currentZonedDateTime.getYear());
		/* If today is less than 7 days before this month's options expiration, go out a month */
		if (currentZonedDateTime.getDayOfMonth() > (expirationSaturday - 7)) {
			ZonedDateTime todayPlusOneMonth = currentZonedDateTime.plusMonths(1);
			expirationSaturday = calculateFutureExpiry(
					todayPlusOneMonth.getMonth().getValue(), todayPlusOneMonth.getYear());
			expiry = LocalDate.of(
					todayPlusOneMonth.getYear(), todayPlusOneMonth.getMonth(), expirationSaturday);
		} else {
			expiry = LocalDate.of(
					currentZonedDateTime.getYear(), currentZonedDateTime.getMonth(), expirationSaturday);
		}
		/* Let's sell ITM options and generate some alpha. Or let's not, maybe OTM has more alpha. */
		if (optionType.equals(SecurityType.CALL)) {
			LOGGER.debug("Finding valid call");
			/* Should provide a slightly OTM price either on the dollar or half dollar
			   98.99 + (0.5 - 98.99 mod 0.5)
			 = 98.99 + (0.5 - 0.49)
			 = 98.99 + 0.01
			 = 99.00
			 or 67.27 + (0.5 - 67.27 mod 0.5)
			 = 67.27 + (0.5 - 0.27)
			 = 67.27 + 0.23
			 = 67.50
			*/
			BigDecimal strikePrice = limitStrikePrice.add(
					BigDecimal.valueOf(0.5).subtract(limitStrikePrice.remainder(BigDecimal.valueOf(0.5))));
			LOGGER.debug("strikePrice = ${}, limitStrikePrice = ${}", strikePrice, limitStrikePrice);
			/* While looking for an option don't go further than 10% out from current underlying security price */
			while ((strikePrice.subtract(limitStrikePrice)
					.divide(limitStrikePrice, 3, RoundingMode.HALF_UP)
					.compareTo(BigDecimal.valueOf(0.1))) < 0) {
				optionTickerToTry = createOptionTicker(stockTicker, expiry, 'C', strikePrice);
				LOGGER.debug("Trying option ticker {}", optionTickerToTry);
				if (MARKET.tickerValid(optionTickerToTry)) {
					return Option.with(optionTickerToTry);
				}
				strikePrice = strikePrice.add(BigDecimal.valueOf(0.50));
			}
			LOGGER.warn("Couldn't find a CALL in the correct strike range");
		} else if (optionType.equals(SecurityType.PUT)) {
			LOGGER.debug("Finding put to sell");
			/* This should round down to the nearest half dollar. i.e.
			   98.99 - (98.99 mod 0.5)
			 = 98.99 - 0.49
			 = 98.50
			 or 67.27 - (67.27 mod 0.5)
			 = 67.27 - 0.27
			 = 67.00
			*/
			BigDecimal strikePrice = limitStrikePrice.subtract(limitStrikePrice.remainder(BigDecimal.valueOf(0.5)));
			LOGGER.debug("strikePrice = {}, limitStrikePrice = {}", strikePrice, limitStrikePrice);
			/* While looking for an option don't go further than 10% out from current underlying security price */
			while ((strikePrice.subtract(limitStrikePrice)
					.divide(limitStrikePrice, 3, RoundingMode.HALF_UP)
					.compareTo(BigDecimal.valueOf(0.1))) > 0) {
				optionTickerToTry = createOptionTicker(stockTicker, expiry, 'P', strikePrice);
				if (MARKET.tickerValid(optionTickerToTry)) {
					LOGGER.debug("Returning new Option(\"{}\")", optionTickerToTry);
					return Option.with(optionTickerToTry);
				}
				strikePrice = strikePrice.subtract(BigDecimal.valueOf(0.50));
			}
			LOGGER.warn("Couldn't find a PUT in the correct strike range");
		} else {
			LOGGER.warn("Couldn't make heads nor tails of option type {}", optionType);
		}
		LOGGER.debug("Returning null from Option.with()");
		return null;  // Failed to supply valid information
	}

	@Override
	public String getTicker() {
		return ticker;
	}

	@Override
	public SecurityType getSecType() {
		return secType;
	}

	@Override
	public boolean isStock() {
		return false;
	}

	@Override
	public boolean isOption() {
		return true;
	}

	boolean isPut() {
		return secType.equals(SecurityType.PUT);
	}

	boolean isCall() {
		return secType.equals(SecurityType.CALL);
	}

	BigDecimal getStrike() {
		return strike;
	}

	LocalDate getExpiry() {
		return expiry;
	}

	String getUnderlyingTicker() {
		return underlyingTicker;
	}
}
