package net.toddsarratt.GaussTrader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.MalformedURLException;
import java.sql.*;
import javax.sql.DataSource;
import org.joda.time.DateTime;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http://www.gummy-stuff.org/Yahoo-data.htm
 * @author tsarratt
 *
 */

public class GaussTrader {
	
    protected static String portfolioName = "test";
    protected static final String DB_IP = "localhost";
    protected static final String DB_NAME = "postgres";
    protected static final String DB_USER = "postgres";
    protected static final String DB_PASSWORD = "b3llcurv38";
    protected static final double STARTING_CASH = 1_000_000.00;  // Default value for new portfolio
    protected static int bollBandPeriod = 20;
    protected static double bollingerSD1 = 2.0;
    protected static double bollingerSD2 = 2.5;
    protected static double bollingerSD3 = 3.0;
    protected static boolean delayedQuotes = true;	// 20min delay using quotes from Yahoo!
    protected static int delayMs = 3_600_000;		// Build in delay so we're not churning tons of (not even real-time) data
    private static PGSimpleDataSource dataSource = new PGSimpleDataSource();	
    private static final Logger LOGGER = LoggerFactory.getLogger(GaussTrader.class);

    public static void printGTUsage() {
	LOGGER.info("GaussTrader by Todd Sarratt");
	LOGGER.info("Usage : GaussTrader <PortfolioName> <StandardDevs> <StandardDevIncrements> <ticker1> ... <tickern>");
	LOGGER.info("No arguments uses default values .");
    }
	
    public static boolean argsOk(String[] clArgs) {
	return( (clArgs.length == 0) || (clArgs.length > 4) );
    }

    /* Returns current epoch time + least significant nano seconds to generate unique order and position ids */	
    static long getNewId() {
	return ( (System.currentTimeMillis() << 20) & 0x7FFFFFFFFFF00000l) | (System.nanoTime() & 0x00000000000FFFFFl);
    }

    static DataSource getDataSource() {
	return (DataSource)dataSource;
    }

    public static void main(String[] args) {
	Stock stockToAdd;
	try {
	    LOGGER.info("*** START PROGRAM ***");
	    LOGGER.info("Starting GaussTrader at {}", new DateTime());
	    ArrayList<String> tickerList = new ArrayList<>();
	    ArrayList<Stock> tradeableStockList = new ArrayList<>();

	    if(!argsOk(args)) {
		printGTUsage();
		System.exit(1);
	    }		
	    // TODO : Replace code below with fetch of ticker list from command line
	    // Currently using DJIA
	    LOGGER.info("Adding manually entered list of stocks");
	    String[] tickerArray = {"MMM", "NKE", "AXP", "T", "GS", "BA", "CAT", "CVX", "CSCO", "KO", "DD", "XOM", "GE", "V", "HD", "INTC", 
				    "IBM", "JNJ", "JPM", "MCD", "MRK", "MSFT", "PFE", "PG", "TRV", "UNH", "UTX", "VZ", "WMT", "DIS"};
	    tickerList.addAll(Arrays.asList(tickerArray));
	    /* Adding AAPL for Bill */
	    tickerList.add("AAPL");
	    LOGGER.debug("tickerList.size() = {}", tickerList.size());
	    /* Set up DB connection. Pass DataSource to methods that need DB access via DataSource.getConnection() */
	    LOGGER.debug("dataSource.setServerName({})", DB_IP);
	    dataSource.setServerName(DB_IP);
	    LOGGER.debug("dataSource.setDatabaseName({})", DB_NAME);
	    dataSource.setDatabaseName(DB_NAME);
	    LOGGER.debug("dataSource.setUser({})", DB_USER);
	    dataSource.setUser(DB_USER);
	    LOGGER.debug("dataSource.setPassword({})", DB_PASSWORD);
	    dataSource.setPassword(DB_PASSWORD);
			
	    for(String candidateTicker : tickerList) {
		try {
		    LOGGER.info("Adding {} to tradeableStockList", candidateTicker);
		    stockToAdd = new Stock(candidateTicker, dataSource);
		    if(stockToAdd.getBollingerBand(0) > 0.00) {
			tradeableStockList.add(stockToAdd);
		    } else {
			LOGGER.warn("Failed to calculate valid Bollinger Bands for {}", candidateTicker);
		    }
		} catch(SecurityNotFoundException snfe) {
		    LOGGER.warn("Security {} does not exist in Yahoo! database.", candidateTicker);
		    LOGGER.debug("Caught (SecurityNotFoundException snfe)", snfe);
		} catch(MalformedURLException mue) {
		    LOGGER.error("MalformedURLException *** END PROGRAM ***", mue);
		    System.exit(1);
		} catch(IOException ioe) {
		    LOGGER.error("Cannot connect to Yahoo!");
		    LOGGER.error("IOException ioe *** END PROGRAM ***", ioe);
		    System.exit(1);
		} catch(NumberFormatException nfe) {
		    LOGGER.warn("Bad data from Yahoo! for ticker {}", candidateTicker);
		    LOGGER.debug("Caught (NumberFormatException nfe)", nfe);
		}
	    }
	    LOGGER.info("Creating new TradingSession() with new Portfolio({})", portfolioName);
	    TradingSession todaysSession = new TradingSession(new Portfolio(portfolioName, dataSource), tradeableStockList);
	    todaysSession.runTradingDay();
	    LOGGER.info("*** END PROGRAM ***");
	} catch(Exception e) {
	    LOGGER.error("ENCOUNTERED UNEXPECTED FATAL ERROR");
	    LOGGER.error("Caught (Exception e)", e);
	    LOGGER.error("*** END PROGRAM ***");
	    System.exit(1);
	}
    }
}