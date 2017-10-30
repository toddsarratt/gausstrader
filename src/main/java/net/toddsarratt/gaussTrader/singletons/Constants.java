package net.toddsarratt.gaussTrader.singletons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Collected constants for gaussTrader. All members of this class are static final. Java 9 immutable collection
 * factories are used to generate many of the collections.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class Constants {
	/* Fields representing "truths" available to the public */
	public static final BigDecimal BIGDECIMAL_MINUS_ONE = new BigDecimal(-1);
	public static final BigDecimal BIGDECIMAL_ONE_HUNDRED = new BigDecimal(100);
	/* Fields only used by the Constants class */
	private static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);
	private static final String DEFAULT_CONFIG_FILENAME = "GaussTrader.properties";
	private static final Properties PROPERTIES = new Properties();
	private static final String PORTFOLIO_NAME = PROPERTIES.getProperty("PORTFOLIO_NAME");
	private static final String DB_IP = PROPERTIES.getProperty("DB_IP");
	private static final String DB_NAME = PROPERTIES.getProperty("DB_NAME");
	private static final String DB_USER = PROPERTIES.getProperty("DB_USER");
	private static final String DB_PASSWORD = PROPERTIES.getProperty("DB_PASSWORD");
	private static final BigDecimal STOCK_PCT_OF_PORTFOLIO = new BigDecimal(PROPERTIES.getProperty("STOCK_PCT_OF_PORTFOLIO"));
	private static final BigDecimal STARTING_CASH = new BigDecimal(PROPERTIES.getProperty("STARTING_CASH"));
	private static final int BOLL_BAND_PERIOD = Integer.valueOf(PROPERTIES.getProperty("BOLL_BAND_PERIOD"));
	private static final BigDecimal BOLLINGER_SD1 = new BigDecimal(PROPERTIES.getProperty("BOLLINGER_SD1");
	private static final BigDecimal BOLLINGER_SD2 = new BigDecimal(PROPERTIES.getProperty("BOLLINGER_SD2");
	private static final BigDecimal BOLLINGER_SD3 = new BigDecimal(PROPERTIES.getProperty("BOLLINGER_SD3"));
	private static final Boolean DELAYED_QUOTES = Boolean.valueOf(PROPERTIES.getProperty("DELAYED_QUOTES"));
	private static final int DELAY_MS = 1000 *
			Integer.valueOf(
					PROPERTIES.getProperty("DELAY", "60")
			);
	private static final List TICKERS = List.of(
			PROPERTIES.getProperty("TICKERS")
					.replaceAll("\\s", "")
					.split(",")
	);
	/* Hard coded closed and early closure trading days : https://www.nyse.com/markets/hours-calendars */
	private static final List JULIAN_HOLIDAYS_2017 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_HOLIDAYS_2017").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	private static final List JULIAN_HOLIDAYS_2018 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_HOLIDAYS_2018").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	private static final List JULIAN_HOLIDAYS_2019 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_HOLIDAYS_2019").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	private static final List JULIAN_1PM_CLOSE_2017 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_1PM_CLOSE_2017").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	private static final List JULIAN_1PM_CLOSE_2018 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_1PM_CLOSE_2018").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	private static final List JULIAN_1PM_CLOSE_2019 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_1PM_CLOSE_2019").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	private static final Map HOLIDAY_MAP = Map.of(
			2017, JULIAN_HOLIDAYS_2017,
			2018, JULIAN_HOLIDAYS_2018,
			2019, JULIAN_HOLIDAYS_2019);
	private static final Map EARLY_CLOSE_MAP = Map.of(
			2017, JULIAN_1PM_CLOSE_2017,
			2018, JULIAN_1PM_CLOSE_2018,
			2019, JULIAN_1PM_CLOSE_2019);

	static {
		try (FileInputStream defaultsIn = new FileInputStream(DEFAULT_CONFIG_FILENAME)) {
			// create and load default properties
			PROPERTIES.load(defaultsIn);
		} catch (Exception pokemon) {
			LOGGER.error("Problem loading properties", pokemon);
			System.exit(1);
		}
	}

	public static String getDefaultConfigFilename() {
		return DEFAULT_CONFIG_FILENAME;
	}

	public static Properties getPROPERTIES() {
		return PROPERTIES;
	}

	public static String getDowJones30() {
		return DOW_JONES_30;
	}

	public static List getJulianHolidays2017() {
		return JULIAN_HOLIDAYS_2017;
	}

	public static List getJulianHolidays2018() {
		return JULIAN_HOLIDAYS_2018;
	}

	public static List getJulianHolidays2019() {
		return JULIAN_HOLIDAYS_2019;
	}

	public static List getJulian1pmClose2017() {
		return JULIAN_1PM_CLOSE_2017;
	}

	public static List getJulian1pmClose2018() {
		return JULIAN_1PM_CLOSE_2018;
	}

	public static List getJulian1pmClose2019() {
		return JULIAN_1PM_CLOSE_2019;
	}

	public static BigDecimal getBigdecimalMinusOne() {
		return BIGDECIMAL_MINUS_ONE;
	}

	public static BigDecimal getBigdecimalOneHundred() {
		return BIGDECIMAL_ONE_HUNDRED;
	}

	public static String getPortfolioName() {
		return PORTFOLIO_NAME;
	}

	public static String getDbIp() {
		return DB_IP;
	}

	public static String getDbName() {
		return DB_NAME;
	}

	public static String getDbUser() {
		return DB_USER;
	}

	public static String getDbPassword() {
		return DB_PASSWORD;
	}

	public static int getMarketQueryRetries() {
		return MARKET_QUERY_RETRIES;
	}

	public static BigDecimal getStockPctOfPortfolio() {
		return STOCK_PCT_OF_PORTFOLIO;
	}

	public static BigDecimal getStartingCash() {
		return STARTING_CASH;
	}

	public static int getBollBandPeriod() {
		return BOLL_BAND_PERIOD;
	}

	public static BigDecimal getBollingerSd1() {
		return BOLLINGER_SD1;
	}

	public static BigDecimal getBollingerSd2() {
		return BOLLINGER_SD2;
	}

	public static BigDecimal getBollingerSd3() {
		return BOLLINGER_SD3;
	}

	public static Boolean isDelayedQuotes() {
		if (DELAYED_QUOTES == null) {

		}
		return DELAYED_QUOTES;
	}

	public static int getDelayMs() {
		return DELAY_MS;
	}

	public static List getTICKERS() {
		return TICKERS;
	}

	public static Map getHolidayMap() {
		return HOLIDAY_MAP;
	}

	public static Map getEarlyCloseMap() {
		return EARLY_CLOSE_MAP;
	}
}
