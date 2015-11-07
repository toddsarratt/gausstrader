package net.toddsarratt.GaussTrader;

import com.google.common.collect.ImmutableMap;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Collected constants for GaussTrader. All members of this class are immutable.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class Constants {
   static final String PORTFOLIO_NAME = "shortStrat2014Aug07";
   /* All 30 stock tickers in the Dow Jones Industrial Average */
   static final String[] DOW_JONES_TICKERS = {"MMM", "NKE", "AXP", "T", "GS", "BA", "CAT", "CVX", "CSCO", "KO", "DD",
           "XOM", "GE", "V", "HD", "INTC", "IBM", "JNJ", "JPM", "MCD", "MRK", "MSFT", "PFE", "PG", "TRV", "UNH", "UTX",
           "VZ", "WMT", "DIS"};
   static final String DB_IP = "localhost";
   static final String DB_NAME = "postgres";
   static final String DB_USER = "postgres";
   static final String DB_PASSWORD = "b3llcurv38";
   static final int YAHOO_RETRIES = 5;                // Number of tiMes to retry Yahoo connections
   static final BigDecimal STOCK_PCT_OF_PORTFOLIO = BigDecimal.valueOf(10.0);
   static final BigDecimal STARTING_CASH = BigDecimal.valueOf(1_000_000.00);  // Default value for new portfolio
   static final int BOLL_BAND_PERIOD = 20;
   static final BigDecimal BOLLINGER_SD1 = BigDecimal.valueOf(2.0);
   static final BigDecimal BOLLINGER_SD2 = BigDecimal.valueOf(2.5);
   static final BigDecimal BOLLINGER_SD3 = BigDecimal.valueOf(3.0);
   static final boolean DELAYED_QUOTES = true;   // 20min delay using quotes from Yahoo!
   static final int DELAY_MS = 60_000;          // Time between each stock price check to reduce (not even real-time) data churn
   static final Integer[] JULIAN_HOLIDAYS_2014 = {1, 20, 48, 108, 146, 185, 244, 331, 359};
   static final Integer[] JULIAN_HOLIDAYS_2015 = {1, 19, 47, 93, 145, 184, 250, 330, 359};
   static final ImmutableMap<Integer, List<Integer>> HOLIDAY_MAP =
           ImmutableMap.<Integer, List<Integer>>builder()
                   .put(2014, Arrays.asList(JULIAN_HOLIDAYS_2014))
                   .put(2015, Arrays.asList(JULIAN_HOLIDAYS_2015))
                   .build();
   static final Integer[] JULIAN_1PM_CLOSE_2014 = {184, 332, 358};
   static final Integer[] JULIAN_1PM_CLOSE_2015 = {331, 358};
   static final ImmutableMap<Integer, List<Integer>> EARLY_CLOSE_MAP =
           ImmutableMap.<Integer, List<Integer>>builder()
                   .put(2014, Arrays.asList(JULIAN_1PM_CLOSE_2014))
                   .put(2015, Arrays.asList(JULIAN_1PM_CLOSE_2015))
                   .build();
   static final DateTimeFormatter LAST_BAC_TICK_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyyhh:mmaa");
}
