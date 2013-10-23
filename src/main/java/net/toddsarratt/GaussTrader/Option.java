package net.toddsarratt.GaussTrader;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.base.BaseDateTime;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.DateTimeConstants;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 
 * Root Symbol + Expiration Year(yy) + Expiration Month(mm) + Expiration Day(dd) + Call/Put Indicator (C or P) +
 * Strike Price Dollars + Strike Price Fraction of Dollars (which can include decimals)
 * Read more: http://www.investopedia.com/articles/optioninvestor/10/options-symbol-rules.asp#ixzz2IOZQ4kou
 */

public class Option extends Security {
    private String ticker;
    private double price;
    private String secType;
    private DateTime expiry = null;
    private String underlyingTicker = null;
    private double strike = 0.00;
    private double last = 0.00;
    private double bid = 0.00;
    private double ask = 0.00;
    private static final Logger LOGGER = LoggerFactory.getLogger(Option.class);
	
    Option(String optionTickerArg) {
	// Receive an option ticker such as : XOM130720P00070000
	LOGGER.debug("Entering constructor Option(String {})", optionTickerArg);		
	ticker = optionTickerArg;
		
	Pattern p = Pattern.compile("^\\D+");
	Matcher m = p.matcher(optionTickerArg);
	if(m.find()) {
	    underlyingTicker = m.group(0);
	}
	p = Pattern.compile("\\d{6}");
	m = p.matcher(optionTickerArg);
	if(m.find()) {
	    DateTimeFormatter expiryFormat = DateTimeFormat.forPattern("yyMMdd");
	    expiry = expiryFormat.parseDateTime(m.group(0));
	}
	p = Pattern.compile("\\d[CP]\\d");
	m = p.matcher(optionTickerArg);
	if(m.find()) {
	    char[] optionTypeMatchArray = m.group(0).toCharArray();
	    char optionType = optionTypeMatchArray[1];
	    secType = (optionType == 'C') ? "CALL" : (optionType == 'P') ? "PUT" : null;
	    if(secType == null) {
		LOGGER.warn("Invalid parsing of option symbol. Expecting C or P (put or call), retrieved : {}", optionType);
	    }
	}
	p = Pattern.compile("\\d{8}");
	m = p.matcher(optionTickerArg);
	if(m.find()) {
	    strike = Double.parseDouble(m.group(0)) / 1000.0;
	}
	LOGGER.info("Created {} option {} for underlying {} expiry {} for strike ${}", 
		    secType, ticker, underlyingTicker, expiry.toString("MMMM dd YYYY"), strike);
    }
	
    public static boolean optionTickerValid(String optionTicker) throws IOException {		
	/* Yahoo! returns 'There are no All Markets results for XOM130216P00077501' in the web page of an invalid option quote */
	LOGGER.debug("Entering optionTickerValid(String {})", optionTicker);	
	String input = null;
	URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + optionTicker);
        Scanner yahooScan = new Scanner(yahoo_url.openStream());
        if (!yahooScan.hasNextLine()) {
	    yahooScan.close();
	    LOGGER.debug("{} is not a valid option ticker", optionTicker);
	    return false;
        }
        input = yahooScan.useDelimiter("\\A").next();
    	yahooScan.close();
        int from  = input.indexOf("There are no All Markets");
	LOGGER.debug("{} is a valid option ticker : {}", optionTicker, (from == -1));
        return (from == -1); 
    }
	
    double lastTick() {
        /* reference: http://weblogs.java.net/blog/pat/archive/2004/10/stupid_yahooScan_1.html */
	String input = null;
	try {
	    URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + ticker);
	    Scanner yahooScan = new Scanner(yahoo_url.openStream());
	    if (!yahooScan.hasNextLine()) {
		yahooScan.close();
		return -1.0;
	    }
	    input = yahooScan.useDelimiter("\\A").next();
	    yahooScan.close();
	    int tickerIndex = input.indexOf("time_rtq_ticker", 0);
	    int from  = input.indexOf("<span", tickerIndex);
	    from = input.indexOf(">", from + 4);
	    int toIndex = input.indexOf("</span>", from);
	    String price = input.substring(from + 1, toIndex);
	    last = Double.parseDouble(price);
	    return last;
	} catch(IOException ioe) {
            LOGGER.warn("IOException generated trying to get lastTick for {}", ticker);
            LOGGER.debug("Caught (IOException ioe)", ioe);
            return -1;
        }
    }

    public static double lastTick(String ticker) throws IOException {
	String input = null;
        URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + ticker);
        Scanner yahooScan = new Scanner(yahoo_url.openStream());
        if (!yahooScan.hasNextLine()) {
            yahooScan.close();
            return -1.0;
        }
        input = yahooScan.useDelimiter("\\A").next();
        yahooScan.close();
	int tickerIndex = input.indexOf("time_rtq_ticker", 0);
        int from  = input.indexOf("<span", tickerIndex);
        from = input.indexOf(">", from + 4);
        int toIndex = input.indexOf("</span>", from);
        String price = input.substring(from + 1, toIndex);
        return Double.parseDouble(price);
    }
	
    double lastBid() throws IOException {
        /* reference: http://weblogs.java.net/blog/pat/archive/2004/10/stupid_yahooScan_1.html */
	String input = null;
	URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + ticker);
        Scanner yahooScan = new Scanner(yahoo_url.openStream());
        if (!yahooScan.hasNextLine()) {
	    yahooScan.close();
	    return -1.0;
        }
        input = yahooScan.useDelimiter("\\A").next();
    	yahooScan.close();
        int yahooBid = input.indexOf("Bid:", 0);
        int from  = input.indexOf("<span", yahooBid);
        from = input.indexOf(">", from + 4);
        int to = input.indexOf("</span>", from);
        String price = input.substring(from + 1, to); 
        bid = Double.parseDouble(price);
        return bid;
    }
	
    double lastAsk() throws IOException {
        // reference: http://weblogs.java.net/blog/pat/archive/2004/10/stupid_yahooScan_1.html
	String input = null;
	URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + ticker);
        Scanner yahooScan = new Scanner(yahoo_url.openStream());
        if (!yahooScan.hasNextLine()) {
	    yahooScan.close();
	    return -1.0;
        }
        input = yahooScan.useDelimiter("\\A").next();
    	yahooScan.close();
        int yahooAsk = input.indexOf("Ask:", 0);
        int from  = input.indexOf("<span", yahooAsk);
        from = input.indexOf(">", from + 4);
        int to = input.indexOf("</span>", from);
        String price = input.substring(from + 1, to); 
        ask = Double.parseDouble(price);
        return ask;
    }
    /**
     * @deprecated
     * All monthly options expire on a Saturday through the 2015 calendar year
     */
    @Deprecated	public static int getExpirySaturday(int month, int year) {
	DateMidnight secondOfexpiryMonth = new DateMidnight(year, month, 2, DateTimeZone.forID("America/New_York"));
	int expiryFriday = 21 - (secondOfexpiryMonth.getDayOfWeek() % 7);
	return expiryFriday + 1;
    }
    /**
     * This method replaces deprecated method getExpirySaturday()
     * Expiration date is the Saturday that follows the third Friday of the month
     * This function returns the expiration date's day of the month [15th - 21st]
     */
    public static int calculateFutureExpiry(int month, int year) {
	MutableDateTime expiryDate = new MutableDateTime(year, month, 2, 16, 20, 0, 0, DateTimeZone.forID("America/New_York"));
        expiryDate.setDayOfMonth(21 - (expiryDate.getDayOfWeek() % 7));   // Calculate third friday
	expiryDate.addDays(1);                                            // Add one for Saturday
	return expiryDate.getDayOfMonth();
    }
    /**
     * Build option ticker. Example : Exxon Mobil 90 Strike Aug 13 expiry call = XOM130817C00090000 
     */
    public static String optionTicker(String stockTicker, BaseDateTime expiry, char indicator, double strikeDouble) {
	LOGGER.debug("Entering optionTicker(String {}, BaseDateTime {}, char {}, double {})", stockTicker, expiry.toString(), indicator, strikeDouble);
	StringBuilder tickerBuilder = new StringBuilder(stockTicker);
	/* Option strike format is xxxxx.yyy * 10^3 Example : Strike $82.50 = 00082500 */
	int strikeInt = (int)(strikeDouble * 1000);
	tickerBuilder.append(expiry.toString("yyMMdd"));
	LOGGER.debug("Assembling option ticker with {} (expiry : {})", expiry.toString("yyMMdd"), expiry.toString("MMMM dd YYYY"));
	tickerBuilder.append(indicator);
	tickerBuilder.append(String.format("%08d", strikeInt));
	LOGGER.debug("Returning assembled option ticker {}", tickerBuilder.toString());
	return tickerBuilder.toString();
    }
	
    public static Option getOption(String stockTicker, String optionType, int monthsOut, double limitStrikePrice) {
	LOGGER.debug("Entering Option.getOption(String {}, String {}, int {}, double {})", stockTicker, optionType, monthsOut, limitStrikePrice);
	double strikePrice = 0.0;
	String optionTickerToTry = null;
	MutableDateTime expiryMutableDateTime = new MutableDateTime();
	int monthOfYear = new MutableDateTime().getMonthOfYear();
	if(monthsOut == 6) {
	    expiryMutableDateTime.addMonths(6 - (int)(monthOfYear / 6));
	} else {
	    expiryMutableDateTime.addMonths(monthsOut);
	}
	expiryMutableDateTime.setDayOfMonth(calculateFutureExpiry(expiryMutableDateTime.getMonthOfYear(), expiryMutableDateTime.getYear()));

	if(optionType.equals("CALL")) {
	    LOGGER.debug("Finding call to sell");
	    strikePrice = (int)limitStrikePrice + 0.50;
	    LOGGER.debug("strikePrice = ${}, limitStrikePrice = ${}", strikePrice, limitStrikePrice);
	    if(strikePrice < limitStrikePrice) {
		strikePrice += 0.50;
                LOGGER.debug("Adjusted strikePrice = ${}, limitStrikePrice = ${}", strikePrice, limitStrikePrice);
	    }
	    /* While looking for an option don't go further than 10% out from current underlying security price */
	    while( (strikePrice - limitStrikePrice) / limitStrikePrice < 0.1 ) {
		optionTickerToTry = optionTicker(stockTicker, expiryMutableDateTime, 'C', strikePrice);
		LOGGER.debug("Trying option ticker {}", optionTickerToTry);
		try {
		    if(optionTickerValid(optionTickerToTry)) {
			return new Option(optionTickerToTry);
		    }
		} catch(IOException ioe) {
		    LOGGER.info("Cannot connect to Yahoo! trying to retrieve option " + optionTickerToTry);
		    LOGGER.debug("Caught (IOException ioe)", ioe);
		    return null;
		}
		strikePrice += 0.50;
	    }
	    LOGGER.warn("Couldn't find a CALL in the correct strike range");
	} else if(optionType.equals("PUT")) {
	    LOGGER.debug("Finding put to sell");
	    strikePrice = (int)limitStrikePrice + 0.50;
	    LOGGER.debug("strikePrice = {}, limitStrikePrice = {}", strikePrice, limitStrikePrice);
	    if(strikePrice > limitStrikePrice) {
		strikePrice -= 0.50;
		LOGGER.debug("Adjusted strikePrice = {}, limitStrikePrice = {}", strikePrice, limitStrikePrice);
	    }
            /* While looking for an option don't go further than 10% out from current underlying security price */
	    while( (strikePrice - limitStrikePrice) / limitStrikePrice > -0.1 ) {
		optionTickerToTry = optionTicker(stockTicker, expiryMutableDateTime, 'P', strikePrice);
		try {
		    if(optionTickerValid(optionTickerToTry)) {
			LOGGER.debug("Returning new Option(\"{}\")", optionTickerToTry);
			return new Option(optionTickerToTry);
		    }
		} catch(IOException ioe) {
		    LOGGER.info("Cannot connect to Yahoo! trying to retrieve option " + optionTickerToTry);
		    LOGGER.debug("Caught (IOException ioe)", ioe);
		    return null;
		}
		strikePrice -= 0.50;
	    }
	    LOGGER.warn("Couldn't find a PUT in the correct strike range");
	} else {
	    LOGGER.warn("Couldn't make heads nor tails of option type {}", optionType);
	}
	LOGGER.debug("Returning null from Option.getOption()");
	return null;  // Failed to supply valid information
    }
    String getTicker() {
	return ticker;
    }
    double getPrice() {
	return price;
    }	
    String getSecType() {
	return secType;
    }
    double getStrike() {
	return strike;
    }
    DateTime getExpiry() {
	return expiry;
    }
    String getUnderlyingTicker() {
	return underlyingTicker;
    }
}
