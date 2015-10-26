package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

   static LinkedHashMap<Long, Double> retrieveYahooHistoricalPrices(String ticker, MissingPriceDateRange dateRange) throws IOException {
      LOGGER.debug("Entering YahooFinance.retrieveYahooHistoricalPrices(MissingPriceDateRange dateRange)");
      LinkedHashMap<Long, Double> yahooPriceReturns = new LinkedHashMap<>();
      String inputLine;
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
   }

   /** http://ichart.finance.yahoo.com/table.csv?s=INTC&a=11&b=1&c=2012&d=00&e=21&f=2013&g=d&ignore=.csv
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

   public static double getHistoricalClosingPrice(String ticker, MutableDateTime expiryFriday) throws IOException {
      MissingPriceDateRange closingMpdr = new MissingPriceDateRange();
      closingMpdr.earliest = new DateTime(expiryFriday);
      closingMpdr.latest = closingMpdr.earliest;

      LinkedHashMap<Long, Double> ryhp = retrieveYahooHistoricalPrices(ticker, closingMpdr);
      LOGGER.debug("Map {}", ryhp.toString());
      LOGGER.debug("Comparing against {}", expiryFriday.getMillis());
      return retrieveYahooHistoricalPrices(ticker, closingMpdr).get(expiryFriday.getMillis());
   }

}
