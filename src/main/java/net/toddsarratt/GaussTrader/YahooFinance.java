package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Created with IntelliJ IDEA on 1/31/14. Not sure why and I didn't update this JavaDoc until 8/28/16. Foo.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.1
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
