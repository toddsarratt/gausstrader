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
	public static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);
	public static final String DEFAULT_CONFIG_FILENAME = "GaussTrader.properties";
	public static final Properties PROPERTIES = new Properties();
	public static final String PORTFOLIO_NAME = PROPERTIES.getProperty("PORTFOLIO_NAME");
	public static final String DB_IP = PROPERTIES.getProperty("DB_IP");
	public static final String DB_NAME = PROPERTIES.getProperty("DB_NAME");
	public static final String DB_USER = PROPERTIES.getProperty("DB_USER");
	public static final String DB_PASSWORD = PROPERTIES.getProperty("DB_PASSWORD");
	public static final BigDecimal STOCK_PCT_OF_PORTFOLIO = new BigDecimal(PROPERTIES.getProperty("STOCK_PCT_OF_PORTFOLIO"));
	public static final BigDecimal STARTING_CASH = new BigDecimal(PROPERTIES.getProperty("STARTING_CASH"));
	public static final int BOLL_BAND_PERIOD = Integer.valueOf(PROPERTIES.getProperty("BOLL_BAND_PERIOD"));
	public static final BigDecimal BOLLINGER_SD1 = new BigDecimal(PROPERTIES.getProperty("BOLLINGER_SD1"));
	public static final BigDecimal BOLLINGER_SD2 = new BigDecimal(PROPERTIES.getProperty("BOLLINGER_SD2"));
	public static final BigDecimal BOLLINGER_SD3 = new BigDecimal(PROPERTIES.getProperty("BOLLINGER_SD3"));
	public static final Boolean DELAYED_QUOTES = Boolean.valueOf(PROPERTIES.getProperty("DELAYED_QUOTES"));
	public static final int DELAY_MS = 1000 *
			Integer.valueOf(
					PROPERTIES.getProperty("DELAY", "60")
			);
	public static final List<String> TICKERS = List.of(
			PROPERTIES.getProperty("TICKERS")
					.replaceAll("\\s", "")
					.split(",")
	);
	/* Hard coded closed and early closure trading days : https://www.nyse.com/markets/hours-calendars */
	public static final List<Integer> JULIAN_HOLIDAYS_2017 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_HOLIDAYS_2017").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	public static final List<Integer> JULIAN_HOLIDAYS_2018 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_HOLIDAYS_2018").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	public static final List<Integer> JULIAN_HOLIDAYS_2019 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_HOLIDAYS_2019").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	public static final List<Integer> JULIAN_1PM_CLOSE_2017 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_1PM_CLOSE_2017").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	public static final List<Integer> JULIAN_1PM_CLOSE_2018 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_1PM_CLOSE_2018").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	public static final List<Integer> JULIAN_1PM_CLOSE_2019 =
			Arrays.stream(
					PROPERTIES.getProperty("JULIAN_1PM_CLOSE_2019").split(","))
					.map(Integer::valueOf)
					.collect(Collectors.toCollection(List::of)
					);
	public static final Map<Integer, List<Integer>> HOLIDAY_MAP = Map.of(
			2017, JULIAN_HOLIDAYS_2017,
			2018, JULIAN_HOLIDAYS_2018,
			2019, JULIAN_HOLIDAYS_2019);
	public static final Map<Integer, List<Integer>> EARLY_CLOSE_MAP = Map.of(
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

	public static Properties getProperties() {
		return PROPERTIES;
	}

	public static String getPortfolioName() {
		return PORTFOLIO_NAME;
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

	public static Map<Integer, List<Integer>> getHolidayMap() {
		return HOLIDAY_MAP;
	}

	public static Map<Integer, List<Integer>> getEarlyCloseMap() {
		return EARLY_CLOSE_MAP;
	}
}
