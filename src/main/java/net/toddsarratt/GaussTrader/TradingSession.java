package net.toddsarratt.GaussTrader;

import com.google.common.collect.ImmutableMap;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class PriceBasedAction {
   boolean doSomething;
   String optionType;
   int monthsOut;

   PriceBasedAction(boolean doSomething, String optionType, int monthsOut) {
      this.doSomething = doSomething;
      this.optionType = optionType;
      this.monthsOut = monthsOut;
   }
}

public class TradingSession {
   private static final Logger LOGGER = LoggerFactory.getLogger(TradingSession.class);
   private static final PriceBasedAction DO_NOTHING_PRICE_BASED_ACTION = new PriceBasedAction(false, "", 0);
   private ArrayList<Stock> stockList = null;
   private Portfolio portfolio = null;
   /* Hard coded closed and early closure trading days : http://www.nyx.com/en/holidays-and-hours/nyse?sa_campaign=/internal_ads/homepage/08262008holidays */
   static final Integer[] JULIAN_HOLIDAYS_2013 = {1, 21, 49, 88, 147, 185, 245, 332, 359};
   static final Integer[] JULIAN_HOLIDAYS_2014 = {1, 20, 48, 108, 146, 185, 244, 331, 359};
   private static final ImmutableMap<Integer, List<Integer>> HOLIDAY_MAP =
      ImmutableMap.<Integer, List<Integer>>builder()
         .put(2013, Arrays.asList(JULIAN_HOLIDAYS_2013))
         .put(2014, Arrays.asList(JULIAN_HOLIDAYS_2014))
         .build();
   static final Integer[] JULIAN_1PM_CLOSE_2013 = {184, 333, 358};
   static final Integer[] JULIAN_1PM_CLOSE_2014 = {184, 332, 358};
   private static final ImmutableMap<Integer, List<Integer>> EARLY_CLOSE_MAP =
      ImmutableMap.<Integer, List<Integer>>builder()
         .put(2013, Arrays.asList(JULIAN_1PM_CLOSE_2013))
         .put(2014, Arrays.asList(JULIAN_1PM_CLOSE_2014))
         .build();
   private static final DateTimeFormatter LAST_BAC_TICK_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyyhh:mmaa");
   private static long marketOpenEpoch;
   private static long marketCloseEpoch;
   /* Time variables */
   private static DateTime todaysDateTime = new DateTime(DateTimeZone.forID("America/New_York"));
   private static int dayToday = todaysDateTime.getDayOfWeek();

   TradingSession(Portfolio portfolio, ArrayList<Stock> stockList) {
      LOGGER.debug("Creating new TradingSession() with portfolio {} and stocklist {}", portfolio.getName(), stockList);
      this.stockList = stockList;
      this.portfolio = portfolio;
   }

   public void runTradingDay() {
      LOGGER.debug("Start of runTradingDay()");
      if (marketIsOpenToday()) {
         sleepUntilMarketOpen();
         while (marketIsOpenThisInstant()) {
            findMispricedStocks();
            checkOpenOrders();
            pauseBetweenCycles();
         }
         reconcileExpiringOptions();
         closeGoodForDayOrders();
         writePortfolioToDb();
         writeClosingPricesToDb();
      } else {
         LOGGER.info("Market is closed today.");
         return;
      }
      LOGGER.info("End of trading day.");
   }

   static boolean isMarketHoliday(int julianDay, int year) {
      return HOLIDAY_MAP.get(year).contains(julianDay);
   }

   static boolean isMarketHoliday(ReadableDateTime date) {
      return isMarketHoliday(date.getDayOfYear(), date.getYear());
   }

   static boolean isEarlyClose(int julianDay, int year) {
      return EARLY_CLOSE_MAP.get(year).contains(julianDay);
   }

   static boolean isEarlyClose(ReadableDateTime date) {
      return isEarlyClose(date.getDayOfYear(), date.getYear());
   }

   /**
    * This method contains DateTime constructors without TZ arguments to display local time in DEBUG logs
    */
   private static boolean marketIsOpenToday() {
      LOGGER.debug("Entering TradingSession.marketIsOpenToday()");
      LOGGER.debug("julianToday = {}", todaysDateTime.getDayOfYear());
      LOGGER.debug("Comparing to list of holidays {}", HOLIDAY_MAP.entrySet());
      if (isMarketHoliday(todaysDateTime)) {
         LOGGER.debug("Market is on holiday.");
         return false;
      }
      LOGGER.debug("dayToday = todaysDateTime.getDayOfWeek() = {} ({})", dayToday, todaysDateTime.dayOfWeek().getAsText());
      if ((dayToday == DateTimeConstants.SATURDAY) || (dayToday == DateTimeConstants.SUNDAY)) {
         LOGGER.warn("It's the weekend. Market is closed.");
         return false;
      }
      marketOpenEpoch = todaysDateTime.withTime(9, 30, 0, 0).getMillis();
      LOGGER.debug("marketOpenEpoch = {} ({})", marketOpenEpoch, new DateTime(marketOpenEpoch));
      LOGGER.debug("Checking to see if today's julian date {} matches early close list {}", todaysDateTime.getDayOfYear(), EARLY_CLOSE_MAP.entrySet());
      if (isEarlyClose(todaysDateTime)) {
         LOGGER.info("Today the market closes at 1pm New York time");
         marketCloseEpoch = todaysDateTime.withTime(13, 0, 0, 0).getMillis();
         LOGGER.debug("marketCloseEpoch = {}", marketCloseEpoch, new DateTime(marketCloseEpoch));
      } else {
         LOGGER.info("Market closes at usual 4pm New York time");
         marketCloseEpoch = todaysDateTime.withTime(16, 0, 0, 0).getMillis();
         LOGGER.debug("marketCloseEpoch = {} ({})", marketCloseEpoch, todaysDateTime.withHourOfDay(16));
       /* If using Yahoo! quotes, account for 20min delay */
         LOGGER.debug("GaussTrader.delayedQuotes == {}", GaussTrader.delayedQuotes);
         if (GaussTrader.delayedQuotes) {
            LOGGER.debug("Adding Yahoo! quote delay of 20 minutes to market open and close times");
            marketOpenEpoch += (20 * 60 * 1000);
            LOGGER.debug("Updated marketOpenEpoch == {} ({})", marketOpenEpoch, new MutableDateTime(marketOpenEpoch));
            marketCloseEpoch += (20 * 60 * 1000);
            LOGGER.debug("Updated marketCloseEpoch == {} ({})", marketCloseEpoch, new MutableDateTime(marketCloseEpoch));
         }
      }
      return true;
   }

   private void sleepUntilMarketOpen() {
      LOGGER.debug("Entering TradingSession.sleepUntilMarketOpen()");
      LOGGER.debug("Calculating time until market open");
      long msUntilMarketOpen = marketOpenEpoch - System.currentTimeMillis();
      LOGGER.debug("msUntilMarketOpen == {} ({})", msUntilMarketOpen, (new Period(msUntilMarketOpen).toString(PeriodFormat.wordBased())));
      if (msUntilMarketOpen > 0) {
         try {
            LOGGER.debug("Sleeping");
            Thread.sleep(msUntilMarketOpen);
         } catch (InterruptedException ie) {
            LOGGER.info("InterruptedException attempting Thread.sleep in method sleepUntilMarketOpen");
            LOGGER.debug("Caught (InterruptedException ie)", ie);
         }
      }
   }

   private static boolean marketIsOpenThisInstant() {
      LOGGER.debug("Inside marketIsOpenThisInstant()");
      long currentEpoch = System.currentTimeMillis();
      LOGGER.debug("currentEpoch = {}", currentEpoch);
      LOGGER.debug("Comparing currentEpoch {} to marketOpenEpoch {} and marketCloseEpoch {} ", currentEpoch, marketOpenEpoch, marketCloseEpoch);
      if ((currentEpoch >= marketOpenEpoch) && (currentEpoch <= marketCloseEpoch)) {
         LOGGER.debug("marketIsOpenThisInstant() returning true");
         return true;
      }
      LOGGER.debug("marketIsOpenThisInstant() returning false");
      return false;
   }

   public static boolean yahooPricesCurrent() {
	/* Get date/time for last BAC tick. Very liquid, should be representative of how current Yahoo! prices are */
      LOGGER.debug("Inside yahooPricesCurrent()");
      long currentEpoch = System.currentTimeMillis();
      LOGGER.debug("currentEpoch = {}", currentEpoch);
      String[] yahooDateTime;
      try {
         yahooDateTime = Stock.askYahoo("BAC", "d1t1");
         LOGGER.debug("yahooDateTime == {}", Arrays.toString(yahooDateTime));
         long lastBacEpoch = LAST_BAC_TICK_FORMATTER.parseMillis(yahooDateTime[0] + yahooDateTime[1]);
         LOGGER.debug("lastBacEpoch == {}", lastBacEpoch);
         LOGGER.debug("Comparing currentEpoch {} to lastBacEpoch {} ", currentEpoch, lastBacEpoch);
         LOGGER.debug("LAST_BAC_TICK_FORMATTER.print(currentEpoch) {} vs. LAST_BAC_TICK_FORMATTER.print(lastBacEpoch) {}",
            LAST_BAC_TICK_FORMATTER.print(currentEpoch), LAST_BAC_TICK_FORMATTER.print(lastBacEpoch));
         if ((lastBacEpoch < (currentEpoch - 3600000)) || (lastBacEpoch > (currentEpoch + 3600000))) {
            LOGGER.debug("Yahoo! last tick for BAC differs from current time by over an hour.");
            return false;
         }
      } catch (IOException ioe) {
         LOGGER.info("Could not connect to Yahoo! to check for current prices (BAC last tick)");
         LOGGER.debug("Caught (IOException ioe)", ioe);
         return false;
      }
      return true;
   }

   private void findMispricedStocks() {
	/**
	 * Using an Iterator instead of foreach
	 * may need to remove a stock if a suitable option is not found to trade
	 */
      LOGGER.debug("Entering TradingSession.findMispricedStocks()");
      Iterator<Stock> stockIterator = stockList.iterator();
      Stock stock;
      double currentPrice;
      while (stockIterator.hasNext()) {
         stock = stockIterator.next();
         LOGGER.debug("Retrieved stock with ticker {} from stockIterator", stock.getTicker());
         try {
            currentPrice = stock.lastTick();
            LOGGER.debug("stock.lastTick() returns ${}", currentPrice);
            if ((currentPrice == -1)) {
               LOGGER.warn("Could not get valid price for ticker {}", stock.getTicker());
            } else {
               PriceBasedAction actionToTake = priceActionable(stock);
               if (actionToTake.doSomething) {
                  Option optionToSell = Option.getOption(stock.getTicker(), actionToTake.optionType, actionToTake.monthsOut, currentPrice);
                  if (optionToSell == null) {
                     LOGGER.info("Cannot find a valid option for {}", stock.getTicker());
                     LOGGER.info("Removing {} from list of tradable securities", stock.getTicker());
                     stockIterator.remove();
                  } else {
                     try {
                        portfolio.addNewOrder(new Order(optionToSell, optionToSell.lastBid(), "SELL", 1, "GFD"));
                     } catch (InsufficientFundsException ife) {
                        LOGGER.warn("Not enough free cash to initiate order for {} @ ${}", optionToSell.getTicker(), optionToSell.lastBid(), ife);
                     }
                  }
               }
            }
         } catch (IOException ioe) {
            LOGGER.info("IO exception attempting to get information on ticker {}", stock.getTicker());
            LOGGER.debug("Caught (IOException ioe)", ioe);
         }
      }
   }

   private void checkOpenOrders() {
      LOGGER.debug("Entering TradingSession.checkOpenOrders()");
      double lastTick;
      for (Order openOrder : portfolio.getListOfOpenOrders()) {
         LOGGER.debug("Checking current open orderId {} for ticker {}", openOrder.getOrderId(), openOrder.getTicker());
         try {
		/* This code sucks. Should be able to replace if / else with Security.lastTick(ticker) */
            if (openOrder.isOption()) {
               lastTick = Option.lastTick(openOrder.getTicker());
            } else {
		    /* openOrder.isStock() */
               lastTick = Stock.lastTick(openOrder.getTicker());
            }
            LOGGER.debug("{} lastTick == {}", openOrder.getTicker(), lastTick);
            if (openOrder.canBeFilled(lastTick)) {
               LOGGER.debug("openOrder.canBeFilled({}) returned true for ticker {}", lastTick, openOrder.getTicker());
               portfolio.fillOrder(openOrder, lastTick);
            }
         } catch (IOException ioe) {
            LOGGER.warn("Unable to connect to Yahoo! to retrieve the stock price for {}", openOrder.getTicker());
            LOGGER.debug("Caught (IOException ioe)", ioe);
         }
      }
   }

   private void pauseBetweenCycles() {
      LOGGER.debug("Entering TradingSession.pauseBetweenCycles()");
      long sleepTimeMs;
      long msUntilMarketClose = marketCloseEpoch - System.currentTimeMillis();
      LOGGER.debug("Comparing msUntilMarketClose {} with GaussTrader.delayMs {}", msUntilMarketClose, GaussTrader.delayMs);
      sleepTimeMs = (msUntilMarketClose < GaussTrader.delayMs) ? msUntilMarketClose : GaussTrader.delayMs;
      try {
         LOGGER.debug("Sleeping for {} ms ({})", sleepTimeMs, (new Period(sleepTimeMs).toString(PeriodFormat.wordBased())));
         Thread.sleep(sleepTimeMs);
      } catch (InterruptedException ie) {
         LOGGER.warn("Interrupted exception trying to sleep {} ms", sleepTimeMs);
         LOGGER.debug("Caught (InterruptedException ie)", ie);
      }
   }

   private PriceBasedAction priceActionable(Stock stock) {
        /* Decide if a security's current price triggers a predetermined event */
      LOGGER.debug("Entering TradingSession.priceActionable(Stock {})", stock.getTicker());
      double currentStockPrice = stock.getPrice();
      LOGGER.debug("Comparing current price ${} against Bollinger Bands {}", currentStockPrice, stock.describeBollingerBands());
      if (currentStockPrice >= stock.getBollingerBand(1)) {
         return findCallAction(stock);
      }
      if (stock.getFiftyDma() < stock.getTwoHundredDma()) {
         LOGGER.info("Stock {} 50DMA < 200DMA. No further checks.", stock.getTicker());
         return DO_NOTHING_PRICE_BASED_ACTION;
      }
      if (currentStockPrice <= stock.getBollingerBand(3)) {
         return findPutAction(stock);
      }
      LOGGER.info("Stock {} at ${} is within Bollinger Bands", stock.getTicker(), currentStockPrice);
      return DO_NOTHING_PRICE_BASED_ACTION;
   }

   private PriceBasedAction findCallAction(Stock stock) {
      LOGGER.debug("Entering TradingSession.findCallAction(Stock {})", stock.getTicker());
      if (portfolio.countUncoveredLongStockPositions(stock) < 1) {
         LOGGER.info("Open long {} positions is equal or less than current short calls positions. Taking no action.", stock.getTicker());
         return DO_NOTHING_PRICE_BASED_ACTION;
      }
      if (stock.getPrice() >= stock.getBollingerBand(2)) {
         LOGGER.info("Stock {} at ${} is above 2nd Bollinger Band of {}", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(2));
         return new PriceBasedAction(true, "CALL", 2);
      }
      if (stock.getPrice() >= stock.getBollingerBand(1)) {
         LOGGER.info("Stock {} at ${} is above 1st Bollinger Band of {}", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(1));
         return new PriceBasedAction(true, "CALL", 1);
      }
      return DO_NOTHING_PRICE_BASED_ACTION;
   }

   private PriceBasedAction findPutAction(Stock stock) {
      LOGGER.debug("Entering TradingSession.findPutAction(Stock {})", stock.getTicker());
      double currentStockPrice = stock.getPrice();
      if (currentStockPrice <= stock.getBollingerBand(5)) {
         LOGGER.info("Stock {} at ${} is below 3rd Bollinger Band of {}", stock.getTicker(), currentStockPrice, stock.getBollingerBand(5));
         if (portfolio.numberOfOpenPutShorts(stock) <= 2) {
            return new PriceBasedAction(true, "PUT", 6);
         }
         LOGGER.info("Open short put {} positions exceeds 2. Taking no action.", stock.getTicker());
         return DO_NOTHING_PRICE_BASED_ACTION;
      }
      if (currentStockPrice <= stock.getBollingerBand(4)) {
         LOGGER.info("Stock {} at ${} is below 2nd Bollinger Band of {}", stock.getTicker(), currentStockPrice, stock.getBollingerBand(4));
         if (portfolio.numberOfOpenPutShorts(stock) <= 1) {
            return new PriceBasedAction(true, "PUT", 2);
         }
         LOGGER.info("Open short put {} positions exceeds 1. Taking no action.", stock.getTicker());
         return DO_NOTHING_PRICE_BASED_ACTION;
      }
      if (currentStockPrice <= stock.getBollingerBand(3)) {
         LOGGER.info("Stock {} at ${} is below 1st Bollinger Band of {}", stock.getTicker(), currentStockPrice, stock.getBollingerBand(3));
         if (portfolio.numberOfOpenPutShorts(stock) == 0) {
            return new PriceBasedAction(true, "PUT", 1);
         }
         LOGGER.info("Open short put {} positions exceeds 0. Taking no action.", stock.getTicker());
      }
      return DO_NOTHING_PRICE_BASED_ACTION;
   }

   /**
    * If within two days of expiry exercise ITM options. If underlyingTicker < optionStrike then stock is PUT to Portfolio
    * If ticker > strike stock is CALLed away
    * else option expires worthless, close position
    */
   private void reconcileExpiringOptions() {
      LOGGER.debug("Entering TradingSession.reconcileExpiringOptions()");
      LOGGER.info("Checking for expiring options");
      MutableDateTime thisSaturday = new MutableDateTime(todaysDateTime, DateTimeZone.forID("America/New_York"));
      thisSaturday.setDayOfWeek(DateTimeConstants.SATURDAY);
      int thisSaturdayJulian = thisSaturday.getDayOfYear();
      int thisSaturdayYear = thisSaturday.getYear();
      if (dayToday == DateTimeConstants.FRIDAY) {
         LOGGER.debug("Today is Friday, checking portfolio.getListOfOpenOptionPositions() for expiring options");
         for (Position openOptionPosition : portfolio.getListOfOpenOptionPositions()) {
            LOGGER.debug("Examining positionId {} for option ticker {}", openOptionPosition.getPositionId(), openOptionPosition.getTicker());
            LOGGER.debug("Comparing Saturday Julian {} to {} and year {} to {}",
               openOptionPosition.getExpiry().getDayOfYear(), thisSaturdayJulian, openOptionPosition.getExpiry().getYear(), thisSaturdayYear);
            if ((openOptionPosition.getExpiry().getDayOfYear() == thisSaturdayJulian) &&
               (openOptionPosition.getExpiry().getYear() == thisSaturdayYear)) {
               LOGGER.debug("Option expires tomorrow, checking moneyness");
               try {
                  double stockLastTick = Stock.lastTick(openOptionPosition.getUnderlyingTicker());
                  if (openOptionPosition.isPut() &&
                     (stockLastTick <= openOptionPosition.getStrikePrice())) {
                     portfolio.exerciseOption(openOptionPosition);
                  } else if (openOptionPosition.isCall() &&
                     (stockLastTick >= openOptionPosition.getStrikePrice())) {
                     portfolio.exerciseOption(openOptionPosition);
                  } else {
                     portfolio.expireOptionPosition(openOptionPosition);
                  }
               } catch (IOException ioe) {
                  LOGGER.info("Caught IOException attempting to get information on open option position ticker {}", openOptionPosition.getTicker());
                  LOGGER.debug("Caught (IOException ioe)", ioe);
               }
            }
         }
      } else {
         LOGGER.debug("Today is not Friday, no expirations to check");
      }
   }

   private void closeGoodForDayOrders() {
	/* Only call this method if the trading day had ended */
      LOGGER.debug("Entering TradingSession.closeGoodForDayOrders()");
      LOGGER.info("Closing GFD orders");
      for (Order checkExpiredOrder : portfolio.getListOfOpenOrders()) {
         if (checkExpiredOrder.getTif().equals("GFD")) {
            portfolio.expireOrder(checkExpiredOrder);
         }
      }
   }

   private void writePortfolioToDb() {
      LOGGER.debug("Entering TradingSession.writePortfolioToDb()");
      LOGGER.info("Writing portfolio information to DB");
      portfolio.endOfDayDbUpdate();
   }

   private void writeClosingPricesToDb() {
      LOGGER.debug("Entering TradingSession.writeClosingPricesToDb()");
      LOGGER.info("Writing closing prices to DB");
      double closingPrice;
	/*
	 * The last price returned from Yahoo! must be later than market close. 
	 * Removing yahoo quote delay from previous market close calculation in TradingSession.marketIsOpenToday() 
	 */
      long earliestAcceptableLastPriceUpdateEpoch = GaussTrader.delayedQuotes ? (marketCloseEpoch - (20 * 60 * 1000)) : marketCloseEpoch;
	/* Default all closing epochs to 420pm of the closing day, to keep consistant, and because epoch is the key value, not the date (epoch is much more granular)*/
      long closingEpoch = new DateTime(DateTimeZone.forID("America/New_York")).withTime(16, 20, 0, 0).getMillis();
      for (Stock stock : stockList) {
         closingPrice = stock.lastTick();
         if (closingPrice == -1.0) {
            LOGGER.warn("Could not get valid price for ticker {}", stock.getTicker());
            return;
         }
         if (stock.getLastPriceUpdateEpoch() < earliestAcceptableLastPriceUpdateEpoch) {
            LOGGER.warn("stock.getLastPriceUpdateEpoch() {} < earliestAcceptableLastPriceUpdateEpoch {}",
               stock.getLastPriceUpdateEpoch(), earliestAcceptableLastPriceUpdateEpoch);
            return;
         }
         DBHistoricalPrices.addStockPrice(stock.getTicker(), closingEpoch, closingPrice);
      }
   }

   public static void main(String[] args) {
      LOGGER.info("The market is open today : {}", marketIsOpenToday());
      LOGGER.info("The market is open right now : {}", (marketIsOpenToday() && marketIsOpenThisInstant()));
      LOGGER.info("Yahoo! prices are current : {}", yahooPricesCurrent());
   }
}