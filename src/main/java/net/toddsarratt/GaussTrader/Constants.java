package net.toddsarratt.GaussTrader;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Collected constants for GaussTrader. All members of this class are immutable.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
class Constants {
   private static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);
   static private PropertiesConfiguration config;

   static {
      try {
         Configurations configs = new Configurations();
         config = configs.properties(new File("config.properties"));
      } catch (ConfigurationException ce) {
         LOGGER.error("Problem loading config.properties", ce);
         System.exit(1);
      }
   }

   static final List<String> DOW_JONES_30 = Arrays.asList("MMM", "NKE", "AXP", "T", "GS", "BA", "CAT", "CVX", "CSCO", "KO", "DD",
           "XOM", "GE", "V", "HD", "INTC", "IBM", "JNJ", "JPM", "MCD", "MRK", "MSFT", "PFE", "PG", "TRV", "UNH", "UTX",
           "VZ", "WMT", "DIS");
   /* Read from properties file */
   static final String PORTFOLIO_NAME = config.getString("PORTFOLIO_NAME", "DefaultPortfolio");

   static final List<String> TICKERS = config.getList(String.class, "TICKERS", DOW_JONES_30);
   static final String DB_IP = config.getString("DB_IP", "localhost");
   static final String DB_NAME = config.getString("DB_NAME", "postgres");
   static final String DB_USER = config.getString("DB_USER", "postgres");
   static final String DB_PASSWORD = config.getString("DB_PASSWORD", "b3llcurv38");
   static final int YAHOO_RETRIES = 5;                // Number of times to retry Yahoo connections
   static final BigDecimal STOCK_PCT_OF_PORTFOLIO =
           config.getBigDecimal("STOCK_PCT_OF_PORTFOLIO", BigDecimal.valueOf(10.0));
   static final BigDecimal STARTING_CASH = config.getBigDecimal("STARTING_CASH", BigDecimal.valueOf(1_000_000.00));
   static final int BOLL_BAND_PERIOD = config.getInt("BOLL_BAND_PERIOD", 20);
   static final BigDecimal BOLLINGER_SD1 = config.getBigDecimal("BOLLINGER_SD1",BigDecimal.valueOf(2.0));
   static final BigDecimal BOLLINGER_SD2 = config.getBigDecimal("BOLLINGER_SD2",BigDecimal.valueOf(2.5));
   static final BigDecimal BOLLINGER_SD3 = config.getBigDecimal("BOLLINGER_SD3",BigDecimal.valueOf(3.0));
   static final boolean DELAYED_QUOTES = config.getBoolean("DELAYED_QUOTES", true);
   static final int DELAY_MS = config.getInt("DELAY", 60) * 1000;
   static final List<Integer> JULIAN_HOLIDAYS_2016 = config.getList(Integer.class, "JULIAN_HOLIDAYS_2016");
   static final List<Integer> JULIAN_HOLIDAYS_2017 = config.getList(Integer.class, "JULIAN_HOLIDAYS_2017");
   static final ImmutableMap<Integer, List<Integer>> HOLIDAY_MAP =
           ImmutableMap.<Integer, List<Integer>>builder()
                   .put(2016, JULIAN_HOLIDAYS_2016)
                   .put(2017, JULIAN_HOLIDAYS_2017)
                   .build();
   static final List<Integer> JULIAN_1PM_CLOSE_2016 = config.getList(Integer.class, "JULIAN_1PM_CLOSE_2016");
   static final List<Integer> JULIAN_1PM_CLOSE_2017 = config.getList(Integer.class, "JULIAN_1PM_CLOSE_2017");
   static final ImmutableMap<Integer, List<Integer>> EARLY_CLOSE_MAP =
           ImmutableMap.<Integer, List<Integer>>builder()
                   .put(2016, JULIAN_1PM_CLOSE_2016)
                   .put(2017, JULIAN_1PM_CLOSE_2017)
                   .build();
   /* Move to appropriate class */
   static final DateTimeFormatter LAST_BAC_TICK_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyyhh:mmaa");
   /* These are not read from the config.properties file because they represent non-fungible truth */
   static final BigDecimal BIGDECIMAL_MINUS_ONE = new BigDecimal(-1);
   static final BigDecimal BIGDECIMAL_ONE_HUNDRED = new BigDecimal(100);
}
