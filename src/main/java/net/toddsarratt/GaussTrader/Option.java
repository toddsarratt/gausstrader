package net.toddsarratt.GaussTrader;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.base.BaseDateTime;
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
	this.ticker = optionTickerArg;
		
	Pattern p = Pattern.compile("^\\D+");
	Matcher m = p.matcher(optionTickerArg);
	if(m.find()) {
	    this.underlyingTicker = m.group(0);
	}
	p = Pattern.compile("\\d{6}");
	m = p.matcher(optionTickerArg);
	if(m.find()) {
	    //	    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
	    DateTimeFormatter expiryFormat = DateTimeFormat.forPattern("yyMMdd");
	    //	    try {
	    //		this.expiration = sdf.parse(m.group(0));
	    expiry = expiryFormat.parseDateTime(m.group(0));
	    //	    } catch (ParseException e) {
	    //		LOGGER.info("Failed to parse date for option " + optionTickerArg);
	    //		e.printStackTrace();
	    //	    }
	}
		
	p = Pattern.compile("\\d[CP]\\d");
	m = p.matcher(optionTickerArg);
	if(m.find()) {
	    char[] optionTypeMatchArray = m.group(0).toCharArray();
	    char optionType = optionTypeMatchArray[1];
	    if(optionType == 'C') {
		secType = "CALL";
	    } else if(optionType == 'P') {
		secType = "PUT";
	    } else {
		LOGGER.info("Invalid parsing of option symbol. Expecting C or P (put or call), retrieved : {}", optionType);
	    }
	}
		
	p = Pattern.compile("\\d{8}");
	m = p.matcher(optionTickerArg);
	if(m.find()) {
	    this.strike = Double.parseDouble(m.group(0)) / 1000.0;
	}
	LOGGER.info("Created {} option {} for underlying {} expiry {} for strike {}", 
		    secType, this.ticker, this.underlyingTicker, expiry.toString("MMMM dd YYYY"), this.strike);
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
	
    public static int getExpirySaturday(int month, int year) {
	/*
	 * Calendar cal = Calendar.getInstance();
	 * cal.set(year, month, 1);
	 * cal.add(Calendar.DAY_OF_MONTH, (21 - (cal.get(Calendar.DAY_OF_WEEK )) % 7) );
	 * return cal.get(Calendar.DAY_OF_MONTH);
	 */
	MutableDateTime expiryCalc = new MutableDateTime(year, month, 1, 0, 0, 0, 0, DateTimeZone.forID("America/New_York"));
	expiryCalc.addDays(20 - (expiryCalc.getDayOfWeek() % 7) );
	return expiryCalc.getDayOfMonth();
    }
    /* Build option ticket. Example : Exxon Mobil 90 Strike Aug 13 expiry call = XOM130817C00090000 */
    public static String optionTicker(String stockTicker, BaseDateTime expiry, char indicator, double strikeDouble) {
	LOGGER.debug("Entering optionTicker(String {}, BaseDateTime {}, char {}, double {})", stockTicker, expiry.toString(), indicator, strikeDouble);
	StringBuilder tickerBuilder = new StringBuilder(stockTicker);
	/* Option strike format is xxxxx.yyy * 10^3 Example : Strike $82.50 = 00082500 */
	int strikeInt = (int)(strikeDouble * 1000);
	/* Two lines below replaced with single line following comments
	 * DateTimeFormatter expiryDtf = DateTimeFormat.forPattern("yyMMdd");
	 * tickerBuilder.append(expiryDtf.print(expiry));
	 */
	tickerBuilder.append(expiry.toString("yyMMdd"));
	LOGGER.debug("Assembling option ticker with {} (humand readable : {})", expiry.toString("yyMMdd"), expiry.toString("MMMM dd YYYY"));
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
	expiryMutableDateTime.setDayOfMonth(getExpirySaturday(expiryMutableDateTime.getMonthOfYear(), expiryMutableDateTime.getYear()));

	if(optionType.equals("CALL")) {
	    LOGGER.info("Finding call to sell");
	    strikePrice = (int)(limitStrikePrice + 1.00);
	    LOGGER.info("strikePrice = {}, limitStrikePrice = {}", strikePrice, limitStrikePrice);
	    if(strikePrice < limitStrikePrice) {
		strikePrice += 0.50;
	    }
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
	    LOGGER.info("Finding put to sell");
	    strikePrice = (int)limitStrikePrice - 0.50;
	    LOGGER.info("strikePrice = {}, limitStrikePrice = {}", strikePrice, limitStrikePrice);
	    if(strikePrice > limitStrikePrice) {
		strikePrice -= 0.50;
		LOGGER.info("Adjusted strikePrice = {}, limitStrikePrice = {}", strikePrice, limitStrikePrice);
	    }
	    while( (strikePrice - limitStrikePrice) / limitStrikePrice > -0.1 ) {
		optionTickerToTry = optionTicker(stockTicker, expiryMutableDateTime, 'P', strikePrice);
		/* Add comments to optionTicker() and optionTickerValid() */
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
    public static void main(String[] args) {
	MutableDateTime rightNow = new MutableDateTime();
	LOGGER.info(optionTicker("XOM", rightNow, 'C', 82.50));
	LOGGER.info("Expiration Friday for month 0 (January) for year 2013 should be 19, we calculate : " + getExpirySaturday(0, 2013));
	LOGGER.info("Expiration Friday for month 1 (February) for year 2013 should be 16, we calculate : " + getExpirySaturday(1, 2013));
	LOGGER.info("Expiration Friday for month 2 (March) for year 2013 should be 15, we calculate : " + getExpirySaturday(2, 2013));
	LOGGER.info("Expiration Friday for month 3 (April) for year 2013 should be 20, we calculate : " + getExpirySaturday(3, 2013));
	try {
	    Option testIbmCall = new Option("IBM130222C00200000");
	    LOGGER.info("Last bid for IBM130222C00200000 " + testIbmCall.lastBid());
	    Option testXomPut = new Option("XOM130720P00070000");
	    LOGGER.info("Last bid for XOM130720P00070000 " + testXomPut.lastBid());
	} catch(IOException ioe) {
	    LOGGER.info("Cannot connect to Yahoo!");
	    LOGGER.info("Caught (IOException ioe)", ioe);
	}
    }
}
