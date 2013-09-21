package net.toddsarratt.GaussTrader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DateTimeConstants;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradingSession.class);
	
    private ArrayList<Stock> stockList = null;
    private Portfolio portfolio = null;
    private static boolean marketOpen = false;
    /* Hard coded closed and early closure trading days : http://www.nyx.com/en/holidays-and-hours/nyse?sa_campaign=/internal_ads/homepage/08262008holidays */
    static final Integer[] JULIAN_HOLIDAYS = {1, 21, 49, 88, 147, 185, 245, 332, 359};
    static final Integer[] JULIAN_1PM_CLOSE = {184, 333, 358};
    private static final DateTimeFormatter LAST_BAC_TICK_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyyhh:mmaa");
    private static long marketOpenEpoch;
    private static long marketCloseEpoch;
    /* Time variables */
    private static DateTime todaysDateTime = new DateTime(DateTimeZone.forID("America/New_York"));
    private static int julianToday;
    private static int dayToday;
    private static int hourToday;
    private static int minuteToday;

    TradingSession(Portfolio portfolio, ArrayList<Stock> stockList) {
	LOGGER.debug("Creating new TradingSession() with portfolio {} and stocklist {}", portfolio.getName(), stockList);
	this.stockList = stockList;
	this.portfolio = portfolio;
    }

    public void runTradingDay() {
	LOGGER.debug("Start of runTradingDay()");
        if(marketIsOpenToday()) {
            sleepUntilMarketOpen();
	    while(marketIsOpenThisInstant()) {
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

    private static boolean marketIsOpenToday() {
	LOGGER.debug("Entering TradingSession.marketIsOpenToday()");
        julianToday = todaysDateTime.getDayOfYear();
	LOGGER.debug("julianToday = {}", julianToday);
	LOGGER.debug("Comparing to list of holidays {}", Arrays.asList(JULIAN_HOLIDAYS));
        if(Arrays.asList(JULIAN_HOLIDAYS).contains(julianToday)) {
	    LOGGER.debug("Arrays.asList(JULIAN_HOLIDAYS).contains(julianToday) == true. Market is on holiday.");
            return false;
        }
        dayToday = todaysDateTime.getDayOfWeek();
	LOGGER.debug("dayToday = todaysDateTime.getDayOfWeek() = {} ({})", dayToday, todaysDateTime.dayOfWeek().getAsText());
        if( (dayToday == DateTimeConstants.SATURDAY) || (dayToday == DateTimeConstants.SUNDAY) ) {
	    LOGGER.warn("Today is a weekend day. Market is closed.");
            return false;
        }
        marketOpenEpoch = todaysDateTime.withTime(9, 30, 0, 0).getMillis();
	LOGGER.debug("marketOpenEpoch = {} ({})", marketOpenEpoch, new DateTime(marketOpenEpoch));
	LOGGER.debug("Checking to see if today's julian date {} matches early close list {}", julianToday, JULIAN_1PM_CLOSE);
        if(Arrays.asList(JULIAN_1PM_CLOSE).contains(julianToday)) {
	    LOGGER.info("Today the market closes at 1pm New York time");
            marketCloseEpoch = todaysDateTime.withTime(13, 0, 0, 0).getMillis();
	    LOGGER.debug("marketCloseEpoch = {}", marketCloseEpoch, new DateTime(marketCloseEpoch));
        } else {
	    LOGGER.info("Market closes at usual 4pm New York time");
	    marketCloseEpoch = todaysDateTime.withTime(16, 0, 0, 0).getMillis();
	    LOGGER.debug("marketCloseEpoch = {} ({})", marketCloseEpoch, todaysDateTime.withHourOfDay(16));
	    /* If using Yahoo! quotes, account for 20min delay */
	    LOGGER.debug("GaussTrader.delayedQuotes == {}", GaussTrader.delayedQuotes);
	    if(GaussTrader.delayedQuotes) {
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
	LOGGER.debug("msUntilMarketOpen == {} ({} hours {} minutes {} sec)", msUntilMarketOpen, (int)(msUntilMarketOpen/3600000), (int)(msUntilMarketOpen/60000), (int)(msUntilMarketOpen/1000));
	if(msUntilMarketOpen > 0) {
	    try {
		LOGGER.debug("Sleeping");
		Thread.sleep(msUntilMarketOpen);
	    } catch(InterruptedException ie) {
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
	if( (currentEpoch >= marketOpenEpoch) && (currentEpoch <= marketCloseEpoch)) {
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
	    long lastBacEpoch = LAST_BAC_TICK_FORMATTER.parseMillis(yahooDateTime[0]+yahooDateTime[1]);
	    LOGGER.debug("lastBacEpoch == {}", lastBacEpoch);
	    LOGGER.debug("Comparing currentEpoch {} to lastBacEpoch {} ", currentEpoch, lastBacEpoch);
	    LOGGER.debug("LAST_BAC_TICK_FORMATTER.print(currentEpoch) {} vs. LAST_BAC_TICK_FORMATTER.print(lastBacEpoch) {}", 
			 LAST_BAC_TICK_FORMATTER.print(currentEpoch), LAST_BAC_TICK_FORMATTER.print(lastBacEpoch));
	    if( (lastBacEpoch < (currentEpoch - 3600000)) || (lastBacEpoch > (currentEpoch + 3600000)) ) {
		LOGGER.debug("Yahoo! last tick for BAC differs from current time by over an hour.");
		return false;
	    }
	} catch(IOException ioe) {
	    LOGGER.info("Could not connect to Yahoo! to check for current prices (BAC last tick)");
	    LOGGER.debug("Caught (IOException ioe)", ioe);
	    return false;
	}
	return true;
    }

    private void findMispricedStocks() {
	/* 
	 * Using an Iterator instead of foreach
	 * may need to remove a stock if a suitable option is not found to trade
	 */
	LOGGER.debug("Entering TradingSession.findMispricedStocks()");
	Iterator<Stock> stockIterator;
	Stock stock;
	double currentPrice = 0.00;
	stockIterator = stockList.iterator();
	while(stockIterator.hasNext()) {
	    stock = stockIterator.next();
	    LOGGER.debug("Retrieved stock with ticker {} from stockIterator", stock.getTicker());
	    try {
		currentPrice = stock.lastTick();
		LOGGER.debug("stock.lastTick() returns {}", currentPrice);
		if( (currentPrice == -1) ) {
		    LOGGER.warn("Could not get valid price for ticker {}", stock.getTicker());
		} else {
		    switch(priceActionable(portfolio, stock)) {
		    case 0 : LOGGER.debug("No action to take."); 
			break;

		    case 1 : LOGGER.debug("Sell 1 mo call above 1st band.");
                        LOGGER.debug("Calling Option.getOption({}, \"CALL\", 1, {})", stock.getTicker(), currentPrice);
			Option callToSell = Option.getOption(stock.getTicker(), "CALL", 1, currentPrice);
			if(callToSell == null) {
			    LOGGER.info("Cannot find a valid option for {}", stock.getTicker());
			    LOGGER.info("Removing {} from list of tradable securities", stock.getTicker());
			    stockIterator.remove();
			} else {
			    LOGGER.debug("Assigning callToSell.lastBid() to optionLastBid");
			    double optionLastBid = callToSell.lastBid();
			    LOGGER.debug("callToSell.lastBid() returned {}", optionLastBid);
			    LOGGER.debug("Reducing requiredFreeCash by {}", optionLastBid * 100);
			    double requiredFreeCash = optionLastBid * 100;
			    LOGGER.debug("requiredFreeCash == {}", requiredFreeCash);
			    LOGGER.debug("Comparing requiredFreeCash with portfolio.getFreeCash()");
			    if(requiredFreeCash < portfolio.getFreeCash() ) {
				LOGGER.debug("Portfolio has enough free cash to execute this order for {}", callToSell.getTicker());
				portfolio.addOrder(new Order(callToSell, optionLastBid, "SELL", 1, "GFD"));
			    } else {
                                LOGGER.warn("Not enough free cash to initiate order for {} @ ${}", callToSell.getTicker(), requiredFreeCash);
			    }
			}
			break; 

		    case 2 : LOGGER.debug("Sell 2 mo call above 2nd band.");
                        LOGGER.debug("Calling Option.getOption({}, \"CALL\", 2, {})", stock.getTicker(), currentPrice);
			callToSell = Option.getOption(stock.getTicker(), "CALL", 2, currentPrice);
			if(callToSell == null) {
			    LOGGER.info("Cannot find a valid option for " + stock.getTicker());
			    LOGGER.info("Removing {} from list of tradable securities", stock.getTicker());
			    stockIterator.remove();
			} else {
			    LOGGER.debug("Assigning callToSell.lastBid() to optionLastBid");
			    double optionLastBid = callToSell.lastBid();
			    LOGGER.debug("callToSell.lastBid() returned {}", optionLastBid);
                            LOGGER.debug("Reducing requiredFreeCash by {}", optionLastBid * 100);
			    double requiredFreeCash = optionLastBid * 100;
			    LOGGER.debug("requiredFreeCash == {}", requiredFreeCash);
                            LOGGER.debug("Comparing requiredFreeCash with portfolio.getFreeCash()");
			    if(requiredFreeCash < portfolio.getFreeCash() ) {
                                LOGGER.debug("Portfolio has enough free cash to execute this order for {}", callToSell.getTicker());
				portfolio.addOrder(new Order(callToSell, callToSell.lastBid(), "SELL", 1, "GFD"));
			    } else {
                                LOGGER.warn("Not enough free cash to initiate order for {} @ ${}", callToSell.getTicker(), requiredFreeCash);
			    }
			}
			break;

		    case 3 : LOGGER.debug("Sell 1 mo put below 1st band.");
                        LOGGER.debug("Calling Option.getOption({}, \"PUT\", 1, {})", stock.getTicker(), currentPrice);
			Option putToSell = Option.getOption(stock.getTicker(), "PUT", 1, currentPrice);
			if(putToSell == null) {
			    LOGGER.info("Cannot find a valid option for " + stock.getTicker());
			    LOGGER.info("Removing {} from list of tradable securities", stock.getTicker());
			    stockIterator.remove();
			} else {
                            LOGGER.debug("Assigning callToSell.lastBid() to optionLastBid");
			    double optionLastBid = putToSell.lastBid();
			    LOGGER.debug("callToSell.lastBid() returned {}", optionLastBid);
                            LOGGER.debug("Reducing requiredFreeCash by {}", optionLastBid * 100);
			    double requiredFreeCash = optionLastBid * 100;
			    LOGGER.debug("requiredFreeCash == {}", requiredFreeCash);
                            LOGGER.debug("Comparing requiredFreeCash with portfolio.getFreeCash()");
			    if(requiredFreeCash < portfolio.getFreeCash() ) {
                                LOGGER.debug("Portfolio has enough free cash to execute this order for {}", putToSell.getTicker());
				portfolio.addOrder(new Order(putToSell, putToSell.lastBid(), "SELL", 1, "GFD"));
			    } else {
                                LOGGER.warn("Not enough free cash to initiate order for {} @ ${}", putToSell.getTicker(), requiredFreeCash);
			    }
			}
			break;

		    case 4 : LOGGER.debug("Sell 2 mo put below 2nd band."); 
                        LOGGER.debug("Calling Option.getOption({}, \"PUT\", 2, {})", stock.getTicker(), currentPrice);
			putToSell = Option.getOption(stock.getTicker(), "PUT", 2, currentPrice);
			if(putToSell == null) {
			    LOGGER.info("Cannot find a valid option for " + stock.getTicker());
			    LOGGER.info("Removing " + stock.getTicker() + " from list of tradable securities");
			    stockIterator.remove();
			} else {
                            LOGGER.debug("Assigning callToSell.lastBid() to optionLastBid");
			    double optionLastBid = putToSell.lastBid();
			    LOGGER.debug("callToSell.lastBid() returned {}", optionLastBid);
                            LOGGER.debug("Reducing requiredFreeCash by {}", optionLastBid * 100);
			    double requiredFreeCash = optionLastBid * 100;
			    LOGGER.debug("requiredFreeCash == {}", requiredFreeCash);
                            LOGGER.debug("Comparing requiredFreeCash with portfolio.getFreeCash()");
			    if(requiredFreeCash < portfolio.getFreeCash() ) {
                                LOGGER.debug("Portfolio has enough free cash to execute this order for {}", putToSell.getTicker());
				portfolio.addOrder(new Order(putToSell, putToSell.lastBid(), "SELL", 1, "GFD"));
			    } else {
                                LOGGER.warn("Not enough free cash to initiate order for {} @ ${}", putToSell.getTicker(), requiredFreeCash);
                            }
			}
			break;

		    case 5 : LOGGER.debug("Sell 6 mo put below 3rd band."); 
                        LOGGER.debug("Calling Option.getOption({}, \"PUT\", 6, {})", stock.getTicker(), currentPrice);
			putToSell = Option.getOption(stock.getTicker(), "PUT", 6, currentPrice);
			if(putToSell == null) {
			    LOGGER.info("Cannot find a valid option for " + stock.getTicker());
			    LOGGER.info("Removing " + stock.getTicker() + " from list of tradable securities");
			    stockIterator.remove();
			} else {
                            LOGGER.debug("Assigning callToSell.lastBid() to optionLastBid");
			    double optionLastBid = putToSell.lastBid();
			    LOGGER.debug("callToSell.lastBid() returned {}", optionLastBid);
                            LOGGER.debug("Reducing requiredFreeCash by {}", optionLastBid * 100);
			    double requiredFreeCash = optionLastBid * 100;
			    LOGGER.debug("requiredFreeCash == {}", requiredFreeCash);
                            LOGGER.debug("Comparing requiredFreeCash with portfolio.getFreeCash()");
			    if(requiredFreeCash < portfolio.getFreeCash() ) {
                                LOGGER.debug("Portfolio has enough free cash to execute this order for {}", putToSell.getTicker());
				portfolio.addOrder(new Order(putToSell, putToSell.lastBid(), "SELL", 1, "GFD"));
			    } else {
                                LOGGER.warn("Not enough free cash to initiate order for {} @ ${}", putToSell.getTicker(), requiredFreeCash);
                            }
			}
			break;

		    default : LOGGER.error("Something is wrong. Should not be in default switch of TradingSession.trade()"); 
			LOGGER.error("*** END PROGRAM ***");
			System.exit(1);
			break;
		    }
		}
	    } catch(IOException ioe) {
		LOGGER.info("IO exception attempting to get information on ticker {}", stock.getTicker());
                LOGGER.debug("Caught (IOException ioe)", ioe); 
	    }
	}
    }

    private void checkOpenOrders() {
        LOGGER.debug("Entering TradingSession.checkOpenOrders()");
	String openOrderSecType;
	for(Order openOrder : portfolio.getListOfOpenOrders()) {
	    openOrderSecType = openOrder.getSecType();
	    LOGGER.debug("Checking current open orderId {} for ticker {}", openOrder.getOrderId(), openOrder.getTicker());
	    try {
		LOGGER.debug("Deciding if order is an option order by looking at {}", openOrderSecType);
		if(openOrderSecType.equals("CALL") || openOrderSecType.equals("PUT")) {
		    LOGGER.debug("Assigning Option.lastTick({}) to lastTick", openOrder.getTicker());
		    double lastTick = Option.lastTick(openOrder.getTicker());
		    LOGGER.debug("lastTick == {}", lastTick);
		    if(openOrder.getAction().equals("SELL")) {
			LOGGER.debug("Comparing lastTick {} to openOrder.getLimitPrice() {} for \"SELL\" order", lastTick, openOrder.getLimitPrice());
			if(lastTick >= openOrder.getLimitPrice()) {
			    LOGGER.debug("Calling portfolio.fillOrder() for orderId {}", openOrder.getOrderId());
			    portfolio.fillOrder(openOrder, lastTick);
			}
		    } else if(openOrder.getAction().equals("BUY")) {
                        LOGGER.debug("Comparing lastTick {} to openOrder.getLimitPrice() {} for \"BUY\" order", lastTick, openOrder.getLimitPrice());
			if(lastTick <= openOrder.getLimitPrice()) {
                            LOGGER.debug("Calling portfolio.fillOrder() for orderId {}", openOrder.getOrderId());
			    portfolio.fillOrder(openOrder, lastTick);
			} 
		    } else {
			LOGGER.warn("orderId {} is of action type {}, must be \"BUY\" or \"SELL\"", openOrder.getOrderId(), openOrder.getAction());
		    }
		} else {              
		    /* openOrder.getSecType().equals("STOCK") */
		    LOGGER.debug("Assuming openOrder.getSecType().equals(\"STOCK\")");
		    LOGGER.debug("Assigning Stock.lastTick({}) to lastTick", openOrder.getTicker());
		    double lastTick = Stock.lastTick(openOrder.getTicker());
                    LOGGER.debug("lastTick == {}", lastTick);
		    if(openOrder.getAction().equals("SELL")) {
                        LOGGER.debug("Comparing lastTick {} to openOrder.getLimitPrice() {} for \"SELL\" order", lastTick, openOrder.getLimitPrice());
			if(lastTick >= openOrder.getLimitPrice()) {
                            LOGGER.debug("Calling portfolio.fillOrder() for orderId {}", openOrder.getOrderId());
			    portfolio.fillOrder(openOrder, lastTick);
			}
		    } else if(openOrder.getAction().equals("BUY")) {
                        LOGGER.debug("Comparing lastTick {} to openOrder.getLimitPrice() {} for \"BUY\" order", lastTick, openOrder.getLimitPrice());
			if(lastTick <= openOrder.getLimitPrice()) {
                            LOGGER.debug("Calling portfolio.fillOrder() for orderId {}", openOrder.getOrderId());
			    portfolio.fillOrder(openOrder, lastTick);
			}
		    }
		}
	    } catch(IOException ioe) {
		LOGGER.warn("Unable to connect to Yahoo! to retrieve the stock price for " + openOrder.getTicker());
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
	    LOGGER.debug("Sleeping for {} ms ({} hours, {} minutes, {} seconds)", sleepTimeMs, (int)(sleepTimeMs/3600000), (int)(sleepTimeMs/60000), (int)(sleepTimeMs/1000));
	    Thread.sleep(sleepTimeMs);
	} catch (InterruptedException ie) {
	    LOGGER.warn("Interrupted exception trying to sleep {} ms", sleepTimeMs);
	    LOGGER.debug("Caught (InterruptedException ie)", ie);
	}
    }
	
    private static int priceActionable(Portfolio portfolio, Stock stock) {
        /* Decide if a security's current price triggers a predetermined event */
	LOGGER.debug("Entering TradingSession.priceActionable(Portfolio {}, Stock {})", portfolio.getName(), stock.getTicker());
	LOGGER.debug("Comparing current price {} against Bollinger Bands {}", stock.getPrice(), stock.describeBollingerBands());
	if(stock.getPrice() >= stock.getBollingerBand(2)) {
	    LOGGER.info("Stock {} at {} is above 2nd Bollinger Band of {}", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(2));
	    if(portfolio.numberOfOpenStockLongs(stock) > portfolio.numberOfOpenCallShorts(stock) ) {
		return 2;
	    } else {
                LOGGER.info("Open long stock positions is equal or less than current short calls positions. Taking no action.");
	    }
	    return 0;
	}
		
	if(stock.getPrice() >= stock.getBollingerBand(1)) {
            LOGGER.info("Stock {} at {} is above 1st Bollinger Band of {}", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(1));
	    if(portfolio.numberOfOpenStockLongs(stock) > portfolio.numberOfOpenCallShorts(stock) ) {
		return 1;
	    } else {
		LOGGER.info("Open long stock positions is equal or less than current short calls positions. Taking no action.");
	    }
	    return 0;
	}
		
	if(stock.getPrice() <= stock.getBollingerBand(5)) {
            LOGGER.info("Stock {} at {} is below 3rd Bollinger Band of {}", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(5));
	    if(stock.getFiftyDma() < stock.getTwoHundredDma()) {
		LOGGER.info("50DMA < 200DMA. Taking no action.");
		return 0;
	    }
	    if(portfolio.numberOfOpenPutLongs(stock) <= 2 ) {
		return 5;
	    } else {
                LOGGER.info("Open short put positions exceeds 2. Taking no action.");
	    }
	    return 0;
	}
		
	if(stock.getPrice() <= stock.getBollingerBand(4)) {
	    LOGGER.info("Stock {} at {} is below 2nd Bollinger Band of {}", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(4));
	    if(stock.getFiftyDma() < stock.getTwoHundredDma()) {
		LOGGER.info("50DMA < 200DMA. Taking no action.");
		return 0;
	    }
	    if(portfolio.numberOfOpenPutLongs(stock) <= 1 ) {
		return 4;
	    } else {
                LOGGER.info("Open short put positions exceeds 1. Taking no action.");
	    }
	    return 0;
	}
		
	if(stock.getPrice() <= stock.getBollingerBand(3)) {
	    LOGGER.info("Stock {} at {} is below 1st Bollinger Band of ", stock.getTicker(), stock.getPrice(), stock.getBollingerBand(3));
	    if(stock.getFiftyDma() < stock.getTwoHundredDma()) {
		LOGGER.info("50DMA < 200DMA. Taking no action.");
		return 0;
	    }			
	    if(portfolio.numberOfOpenPutShorts(stock) == 0 ) {
		return 3;
	    } else {
                LOGGER.info("Open short put positions exceeds 0. Taking no action.");
	    }
	    return 0;
	}
	return 0;
    }

    private void reconcileExpiringOptions() {
	/* If within two days of expiry exercise ITM options */
	LOGGER.debug("Entering TradingSession.reconcileExpiringOptions()");
	MutableDateTime thisSaturday = new MutableDateTime();
	thisSaturday.setDayOfWeek(DateTimeConstants.SATURDAY);
	int thisSaturdayJulian = thisSaturday.getDayOfYear();
	int thisSaturdayYear = thisSaturday.getYear();
        if(dayToday == DateTimeConstants.FRIDAY) {
	    LOGGER.debug("Today is Friday, checking portfolio.getListOfOpenOptionPositions() for expiring options");
	    for(Position openOptionPosition : portfolio.getListOfOpenOptionPositions()) {
		LOGGER.debug("Examining optionId {} for option ticker {}", openOptionPosition.getPositionId(), openOptionPosition.getTicker());
		LOGGER.debug("Comparing Saturday Julian {} to {} and year {} to {}", 
			     openOptionPosition.getExpiry().getDayOfYear(), thisSaturdayJulian, openOptionPosition.getExpiry().getYear(), thisSaturdayYear);
		if( (openOptionPosition.getExpiry().getDayOfYear() == thisSaturdayJulian) &&
		    (openOptionPosition.getExpiry().getYear() == thisSaturdayYear)) {
		    /* If underlyingTicker < optionStrike
		     * then stock is PUT to us 
		     * if ticker > strike 
		     * stock is CALLed away
		     * else
		     * option expires worthless, close position
		     */
		    LOGGER.debug("Option expires tomorrow, checking moneyness");
		    try {
			if( (openOptionPosition.getSecType().equals("PUT")) && 
			    (Stock.lastTick(openOptionPosition.getUnderlyingTicker()) <= openOptionPosition.getStrikePrice()) ) {
			    portfolio.exerciseOption(openOptionPosition);
			} else if( (openOptionPosition.getSecType().equals("CALL")) && 
				   (Stock.lastTick(openOptionPosition.getUnderlyingTicker()) >= openOptionPosition.getStrikePrice()) ) {
			    portfolio.exerciseOption(openOptionPosition);
			}
		    } catch(IOException ioe) {
			LOGGER.info("Caught IOException attempting to get information on open option position ticker {}", openOptionPosition.getTicker());
			LOGGER.debug("Caught (IOException ioe)", ioe);
		    }
		}
	    }
	}
    }
    
    private void closeGoodForDayOrders() {
	/* Only call this method if the trading day had ended */
	LOGGER.debug("Entering TradingSession.closeGoodForDayOrders()");
	for(Order checkExpiredOrder : portfolio.getListOfOpenOrders()) {
	    if(checkExpiredOrder.getTif().equals("GFD")) {
		checkExpiredOrder.closeExpired();
	    }
	}
    }

    private void writePortfolioToDb() {
	LOGGER.debug("Entering TradingSession.writePortfolioToDb()");
	portfolio.endOfDayDbUpdate();
    }

    private void writeClosingPricesToDb() {
        LOGGER.debug("Entering TradingSession.writeClosingPricesToDb()");
        double closingPrice = 0.00;
	/* The last price returned from Yahoo! must be later than market close. Removing yahoo quote delay from previous market close calculation in TradingSession.marketIsOpenToday() */
	long earliestAcceptableLastPriceUpdateEpoch = GaussTrader.delayedQuotes ? (marketCloseEpoch - (20 * 60 * 1000)) : marketCloseEpoch;
	/* Default all closing epochs to 420pm of the closing day, to keep consistant, and because epoch is the key value, not the date (epoch is much more granular)*/
	long closingEpoch = new DateTime(DateTimeZone.forID("America/New_York")).withTime(16, 20, 0, 0).getMillis();
	for(Stock stock : stockList) {
	    LOGGER.debug("Getting closing price for {}", stock.getTicker());
	    closingPrice = stock.lastTick();
	    LOGGER.debug("stock.lastTick() returns {}", closingPrice);
	    if(closingPrice == -1.0) {
		LOGGER.warn("Could not get valid price for ticker {}", stock.getTicker());
		return;
	    }
	    if(stock.getLastPriceUpdateEpoch() < earliestAcceptableLastPriceUpdateEpoch) {
		LOGGER.warn("stock.getLastPriceUpdateEpoch() {} < earliestAcceptableLastPriceUpdateEpoch {}", stock.getLastPriceUpdateEpoch(), earliestAcceptableLastPriceUpdateEpoch);
		return;
	    }
	    DBHistoricalPrices.addStockPrice(stock.getTicker(), closingEpoch, closingPrice, GaussTrader.getDataSource());
	}
    }

    public static void main(String[] args) {
	LOGGER.info("The market is open today : {}", marketIsOpenToday());
	LOGGER.info("The market is open right now : {}", (marketIsOpenToday() && marketIsOpenThisInstant()) );
	LOGGER.info("Yahoo! prices are current : {}", yahooPricesCurrent());
    }
}