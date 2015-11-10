package net.toddsarratt.GaussTrader;

/*	
 * bollingerBand[0] = moving average (period determined by command line args, default 20DMA)
 * SD multiple determined by command line, defaults below
 * bollingerBand[1] = upperBoll1 = MA + 2 standard deviation
 * bollingerBand[2] = upperBoll2 = MA + 2.5 standard deviations
 * bollingerBand[3] = lowerBoll1 = MA - 2 standard deviation
 * bollingerBand[4] = lowerBoll2 = MA - 2.5 standard deviations
 * bollingerBand[5] = lowerBoll3 = MA - 3 standard deviations
 */

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Stock implements Security {

   /*	private class Dividend {
  DateTime dividendDate;
  double dividendPayment;
  }
   */
   private final String ticker;
   private BigDecimal price;
   private long lastPriceUpdateEpoch;
   private BigDecimal twentyDma;
   private BigDecimal fiftyDma;
   private BigDecimal twoHundredDma;
   private LinkedHashMap<Long, BigDecimal> historicalPriceMap = new LinkedHashMap<>(PRICE_TRACKING_MAP);
   private Boolean tickerValid;
   //	public LinkedList<Dividend> dividendsPaid = null;
   private BigDecimal[] bollingerBand = new BigDecimal[6];
   private static Market market = new YahooMarket();
   private static DataStore dataStore = GaussTrader.getDataStore();
   private static final DateTimeFormatter LAST_TICK_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy hh:mmaa");
   private static final Logger LOGGER = LoggerFactory.getLogger(Stock.class);
   private static final Map<Long, BigDecimal> PRICE_TRACKING_MAP = new HashMap<>();
   private static final DateTime EARLIEST_PRICE_DATE = earliestHistoricalPriceNeeded();

   /**
    * Private constructor for Stock class. Use static factory method of() to create objects of this class
    *
    * @param ticker a String representing the ticker
    */
   private Stock(String ticker) {
      LOGGER.debug("Entering constructor Stock(String {})", ticker);
      this.ticker = ticker;
   }

   public static Stock of(String ticker) {
      LOGGER.debug("Entering of(String {})", ticker);
      Stock newStock = new Stock(ticker);
      newStock.tickerValid = market.tickerValid(newStock.ticker);
      if (newStock.tickerValid) {
         fetchPriceMovingAvgs(newStock);
         dataStore.writeStockMetrics(newStock);
         fetchStoredHistoricalPrices(newStock);
         if (newStock.historicalPriceMap.containsValue(Constants.BIGDECIMAL_MINUS_ONE)) {
            Set<Long> missingPriceEpochs = newStock.historicalPriceMap.keySet().stream()
                    .filter(epoch -> newStock.historicalPriceMap.get(epoch).equals(Constants.BIGDECIMAL_MINUS_ONE))
                    .collect(Collectors.toSet());
            fetchMissingPricesFromMarket(newStock);
            updateStoreMissingPrices(newStock, missingPriceEpochs);
         }
         // Calculate BollingerBands
         calculateBollingerBands(newStock);
      }
      return newStock;
   }

   private static void fetchPriceMovingAvgs(Stock stock) {
      LOGGER.debug("Entering Stock.populateStockInfo()");
      String[] priceMovingAvgs;
      priceMovingAvgs = market.priceMovingAvgs(stock.getTicker());
      stock.price = new BigDecimal(priceMovingAvgs[0]);
      stock.lastPriceUpdateEpoch = jodaLastTickToEpoch(priceMovingAvgs[1] + " " + priceMovingAvgs[2]);
      stock.fiftyDma = new BigDecimal(priceMovingAvgs[3]);
      stock.twoHundredDma = new BigDecimal(priceMovingAvgs[4]);
   }

   private static void fetchStoredHistoricalPrices(Stock stock) {
      // Build price map (formerly Stock.populateHistoricalPricesDB -> DBHistoricalPrices.getDBPrices)
      LinkedHashMap<Long, BigDecimal> priceMapFromStore = dataStore.readHistoricalPrices(stock.ticker, EARLIEST_PRICE_DATE);
      priceMapFromStore.keySet().stream()
              .forEach(epochInStore -> {
                 LOGGER.debug("Required price from epoch {} returned from db, updating price in historicalPriceMap", epochInStore);
                 stock.historicalPriceMap.replace(epochInStore, priceMapFromStore.get(epochInStore));
              });
   }

   private static void fetchMissingPricesFromMarket(Stock stock) {
         LOGGER.debug("Calculating date range for missing stock prices.");
         MissingPriceDateRange priceRangeToDownload = getMissingPriceDateRange(stock);
      LinkedHashMap<Long, BigDecimal> priceMapFromMarket =
              market.readHistoricalPrices(stock.ticker, priceRangeToDownload);
      priceMapFromMarket.keySet().stream()
              .forEach(epochInMarket -> {
                 LOGGER.debug("Required price from epoch {} returned from db, updating price in historicalPriceMap", epochInMarket);
                 stock.historicalPriceMap.replace(epochInMarket, Constants.BIGDECIMAL_MINUS_ONE, priceMapFromMarket.get(epochInMarket));
              });
   }

   private static void updateStoreMissingPrices(Stock newStock, Set<Long> missingPriceEpochs) {
      missingPriceEpochs.stream()
              .forEach(epoch -> dataStore.writeStockPrice(newStock.ticker, epoch, newStock.historicalPriceMap.get(epoch)));
   }

   private static DateTime earliestHistoricalPriceNeeded() {
	/* 
	 * Count backwards starting from yesterday the number of days the market was open
	 * Stop when the number of days needed to calculate bollinger bands (usually 20 days) is reached
	 * Set time to 420pm (Usual 4pm market close + 20min yahoo! price delay)
	 * Using long(epoch) in database to avoid MutableDateTime to sql.Date conversion hackery
	 * 420pm in milliseconds = 16th hour * 60min/hour + 20min * 60sec/min * 1000ms/sec
	 */
      LOGGER.debug("Entering Stock.earliestHistoricalPriceNeeded()");
      MutableDateTime earliestDatePriceNeeded = new MutableDateTime(DateTimeZone.forID("America/New_York"));
      earliestDatePriceNeeded.setMillisOfDay((16 * 60 + 20) * 60 * 1000);
      LOGGER.debug("Looking for valid open market dates backwards from {} ({})", earliestDatePriceNeeded.getMillis(), earliestDatePriceNeeded.toString());
      for (int checkedDates = 0; checkedDates < Constants.BOLL_BAND_PERIOD; checkedDates++) {
         earliestDatePriceNeeded.addDays(-1);
         while (!market.wasOpen(earliestDatePriceNeeded)) {
            earliestDatePriceNeeded.addDays(-1);
         }
         PRICE_TRACKING_MAP.put(earliestDatePriceNeeded.getMillis(), Constants.BIGDECIMAL_MINUS_ONE);
         LOGGER.debug("PRICE_TRACKING_MAP.put({}, {}) == {}", earliestDatePriceNeeded.getMillis(), -1.0, earliestDatePriceNeeded.toString());
      }
      LOGGER.debug("Returning earliest date required for adjusted close {} ({})", earliestDatePriceNeeded.getMillis(), earliestDatePriceNeeded.toString());
      LOGGER.debug("PRICE_TRACKING_MAP == {}", PRICE_TRACKING_MAP.toString());
      return new DateTime(earliestDatePriceNeeded, DateTimeZone.forID("America/New_York"));
   }

   private static MissingPriceDateRange getMissingPriceDateRange(Stock stock) {
      LOGGER.debug("Entering Stock.getMissingPriceDateRange()");
      MissingPriceDateRange mpdr = new MissingPriceDateRange();
      MutableDateTime histMutDateTime = new MutableDateTime(DateTimeZone.forID("America/New_York"));
      DateTime missingDateTimeToCheck;

        /* Adjust for 4pm close + 20min Yahoo! delay and subtract one day  */
      histMutDateTime.setMillisOfDay((16 * 60 + 20) * 60 * 1000);
      histMutDateTime.addDays(-1);

      LOGGER.debug("Setting mpdr.latest to {} ({})", EARLIEST_PRICE_DATE.getMillis(), EARLIEST_PRICE_DATE.toString());
      mpdr.latest = new DateTime(EARLIEST_PRICE_DATE, DateTimeZone.forID("America/New_York"));
      LOGGER.debug("mpdr.latest = {} ({})", mpdr.latest.getMillis(), mpdr.latest.toString());
      LOGGER.debug("mpdr.earliest = {} ({})", mpdr.earliest.getMillis(), mpdr.earliest.toString());
      for (long epochPriceRequired : stock.historicalPriceMap.keySet()) {
       /* Map defaults all prices == -1.00 which is updated to a real price returned from the DB */
	    /* Any remaining price of -1.00 needs to be retrieved from Yahoo! */
         if (stock.historicalPriceMap.get(epochPriceRequired).compareTo(BigDecimal.ZERO) < 0) {
            missingDateTimeToCheck = new DateTime(epochPriceRequired, DateTimeZone.forID("America/New_York"));
            LOGGER.debug("historicalPriceMap.get({}) < 0.00 : {}", epochPriceRequired, missingDateTimeToCheck.toString());
            if (mpdr.earliest.isAfter(missingDateTimeToCheck.toInstant())) {
               LOGGER.debug("mpdr.earliest.isAfter(missingDateTimeToCheck.toInstant()) : ({}).isAfter({})",
                       mpdr.earliest.toString(), missingDateTimeToCheck.toString());
               mpdr.earliest = new DateTime(missingDateTimeToCheck, DateTimeZone.forID("America/New_York")).withTime(16, (Constants.DELAYED_QUOTES ? 20 : 0), 0, 0);
               LOGGER.debug("Updating mpdr.earliest = {} ({})", mpdr.earliest.getMillis(), mpdr.earliest.toString());
            }
            if (mpdr.latest.isBefore(missingDateTimeToCheck.toInstant())) {
               LOGGER.debug("mpdr.latest.isBefore(missingDateTimeToCheck.toInstant()) : ({}).isBefore({})", mpdr.latest.toString(), missingDateTimeToCheck.toString());
               mpdr.latest = missingDateTimeToCheck;
               LOGGER.debug("Updating mpdr.latest = {} ({})", mpdr.latest.getMillis(), mpdr.latest.toString());
            }
         }
      }
   /* Not sure why this is commented out. Will replace the for loop above with a lambda and moving to java.time
	so... change is coming!
	for(int checkedPrices = 0; checkedPrices < PRICES_NEEDED; checkedPrices++) {
	    LOGGER.debug("Looking for valid open market date starting with {} ({})", histMutDateTime.getMillis(), histMutDateTime.toString());
	    while(!DBHistoricalPrices.marketWasOpen(histMutDateTime)) {
		histMutDateTime.addDays(-1);
	    }
	    LOGGER.debug("historicalPriceMap.isEmpty() {}", historicalPriceMap.isEmpty());
	    LOGGER.debug("historicalPriceMap.containsKey {} = {}", histMutDateTime.getMillis(),
	    		historicalPriceMap.containsKey(histMutDateTime.getMillis()) );
	    if(!historicalPriceMap.containsKey(histMutDateTime.getMillis())) {
		if(mpdr.latest.isBefore(mpdr.earliest)) {
		    LOGGER.debug("Setting latest date to {} ({})", histMutDateTime.getMillis(), histMutDateTime.toString());
		    mpdr.latest.setDate(histMutDateTime);
		}
		mpdr.earliest.setDate(histMutDateTime);
		LOGGER.debug("Setting earliest date to {} ({})", histMutDateTime.getMillis(), histMutDateTime.toString());
	    }
	    histMutDateTime.addDays(-1);
	}
	*/
      if (stock.historicalPriceMap.isEmpty()) {
         LOGGER.info("No current prices stored so need to get everything from Yahoo!");
         LOGGER.info("Setting earliest date to {} ({})", EARLIEST_PRICE_DATE.getMillis(), EARLIEST_PRICE_DATE.toString());
         mpdr.latest = new DateTime(DateTimeZone.forID("America/New_York"));
         mpdr.earliest = new DateTime(EARLIEST_PRICE_DATE, DateTimeZone.forID("America/New_York"));
      }
      LOGGER.debug("Returning latest date {} ({})", mpdr.latest.getMillis(), mpdr.latest.toString());
      LOGGER.debug("Returning earliest date {} ({})", mpdr.earliest.getMillis(), mpdr.earliest.toString());
      return mpdr;
   }

   public LinkedHashMap<Long, BigDecimal> getHistoricalPriceMap() {
      return historicalPriceMap;
   }

   private static void calculateBollingerBands(Stock stock) {
      LOGGER.debug("Entering calculateBollingerBands()");
      BigDecimal period = new BigDecimal(Constants.BOLL_BAND_PERIOD);
      Collection<BigDecimal> historicalPriceArray;
      if (stock.getHistoricalPriceMap().containsValue(Constants.BIGDECIMAL_MINUS_ONE)) {
         LOGGER.warn("Not enough historical data to calculate Bollinger Bands for {}", stock.getTicker());
         stock.bollingerBand[0] = Constants.BIGDECIMAL_MINUS_ONE;
         stock.bollingerBand[1] = Constants.BIGDECIMAL_MINUS_ONE;
         stock.bollingerBand[2] = Constants.BIGDECIMAL_MINUS_ONE;
         stock.bollingerBand[3] = Constants.BIGDECIMAL_MINUS_ONE;
         stock.bollingerBand[4] = Constants.BIGDECIMAL_MINUS_ONE;
         stock.bollingerBand[5] = Constants.BIGDECIMAL_MINUS_ONE;
      } else {
         BigDecimal currentSMASum;
         BigDecimal currentSMA;
         BigDecimal currentSDSum;
         BigDecimal currentSD;
         historicalPriceArray = stock.getHistoricalPriceMap().values();
         currentSMASum = historicalPriceArray
                 .stream()
                 .reduce(BigDecimal.ZERO, BigDecimal::add);
         LOGGER.debug("currentSMASum = {}", currentSMASum);
         currentSMA = currentSMASum.divide(period, 3, RoundingMode.HALF_EVEN);
         stock.twentyDma = currentSMA;
         LOGGER.debug("currentSMA = {}", currentSMA);
         currentSDSum = historicalPriceArray
                 .stream()
                 .map(adjClose -> adjClose.subtract(currentSMA))
                 .map(distance -> distance.pow(2))
                 .reduce(BigDecimal.ZERO, BigDecimal::add);
         LOGGER.debug("currentSDSum = {}", currentSDSum);
         // Seriously, is there no square root method in BigDecimal?
         currentSD = BigDecimal.valueOf(Math.sqrt(currentSDSum.divide(period, 3, RoundingMode.HALF_EVEN).doubleValue()));
         LOGGER.debug("currentSD = {}", currentSD);
         stock.bollingerBand[0] = currentSMA;
         stock.bollingerBand[1] = currentSMA.add(currentSD).multiply(Constants.BOLLINGER_SD1);
         stock.bollingerBand[2] = currentSMA.add(currentSD).multiply(Constants.BOLLINGER_SD2);
         stock.bollingerBand[3] = currentSMA.subtract(currentSD).multiply(Constants.BOLLINGER_SD1);
         stock.bollingerBand[4] = currentSMA.subtract(currentSD).multiply(Constants.BOLLINGER_SD2);
         stock.bollingerBand[5] = currentSMA.subtract(currentSD).multiply(Constants.BOLLINGER_SD3);
      }
   }

   private static long jodaLastTickToEpoch(String yahooDateTime) {
      LOGGER.debug("Entering Stock.jodaLastTickToEpoch(String {})", yahooDateTime);
      DateTime dateTimeEasternTZ = LAST_TICK_FORMATTER.withZone(DateTimeZone.forID("America/New_York")).parseDateTime(yahooDateTime);
      LOGGER.debug("Returning {}", dateTimeEasternTZ.getMillis());
      return dateTimeEasternTZ.getMillis();
   }

   /*
  private long jodaYahooHistPriceDateTimeToEpoch(String yahooDate) throws ParseException {
  DateTime dateTimeEasternTZ = YAHOO_HIST_FORMATTER.withZone(DateTimeZone.forID("America/New_York")).parseDateTime(yahooDate + " 4:00pm");
  return dateTimeEasternTZ.getMillis();
  }
   */

   //	public void addDividend(Dividend dividendPayment) {
    /* http://ichart.finance.yahoo.com/table.csv?s=INTC&a=11&b=1&c=2011&d=00&e=21&f=2013&g=v&ignore=.csv */

   //		dividendsPaid.add(dividendPayment);
   //	}


   @Override
   public InstantPrice lastTick() {
      LOGGER.debug("Entering lastTick()");
      InstantPrice instantPrice = market.lastTick(ticker);
      price = instantPrice.getPrice();
      lastPriceUpdateEpoch = instantPrice.getEpoch();
      return instantPrice;
   }

   @Override
   public InstantPrice lastBid() {
      return market.lastBid(ticker);
   }

   @Override
   public InstantPrice lastAsk() {
      return market.lastAsk(ticker);
   }

   @Override
   public String getTicker() {
      return ticker;
   }

   @Override
   public BigDecimal getPrice() {
      return price;
   }

   public BigDecimal getBollingerBand(int index) {
      return bollingerBand[index];
   }

   public BigDecimal getTwentyDma() {
      return twentyDma;
   }

   public BigDecimal getFiftyDma() {
      return fiftyDma;
   }

   public BigDecimal getTwoHundredDma() {
      return twoHundredDma;
   }

   @Override
   public String getSecType() {
      return "STOCK";
   }

   public long getLastPriceUpdateEpoch() {
      return lastPriceUpdateEpoch;
   }

   public boolean tickerStored() {
      LOGGER.debug("Entering Stock.tickerValidDb()");
      return dataStore.tickerPriceInStore(ticker);
   }


   public boolean tickerValid() {
      LOGGER.debug("Entering Stock.tickerValid()");
      if (tickerValid == null) {
         return (tickerValid = (tickerStored() || market.tickerValid(ticker)));
      }
      return tickerValid;
   }

   String describeBollingerBands() {
      return "SMA " + bollingerBand[0] +
              " Upper 1st " + bollingerBand[1] +
              " 2nd " + bollingerBand[2] +
              " Lower 1st " + bollingerBand[3] +
              " 2nd " + bollingerBand[4] +
              " 3rd " + bollingerBand[5];
   }

   // TODO : This is a sorry toString()
   @Override
   public String toString() {
      return ticker;
   }
}
