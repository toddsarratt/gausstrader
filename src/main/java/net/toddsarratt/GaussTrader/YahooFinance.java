package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 1/31/14
 */
public class YahooFinance {
   private static final Logger LOGGER = LoggerFactory.getLogger(YahooFinance.class);

   public static double getHistoricalClosingPrice(String ticker, MutableDateTime expiryFriday) {
      MissingPriceDateRange closingMpdr = new MissingPriceDateRange();
      closingMpdr.earliest = new DateTime(expiryFriday);
      closingMpdr.latest = closingMpdr.earliest;
      try {
         LinkedHashMap<Long, Double> ryhp = retrieveYahooHistoricalPrices(ticker, closingMpdr);
         LOGGER.debug("Map {}", ryhp.toString());
         LOGGER.debug("Comparing against {}", expiryFriday.getMillis());
         return retrieveYahooHistoricalPrices(ticker, closingMpdr).get(expiryFriday.getMillis());
      } catch (FileNotFoundException fnfe) {
         LOGGER.warn("File not found exception from Yahoo! indicating invalid ticker.");
         LOGGER.debug("Caught exception", fnfe);
      } catch (IOException ioe) {
         LOGGER.warn("Attempt to get historical price failed");
         LOGGER.debug("Caught exception", ioe);
      }
      return 0.0;
   }
}
