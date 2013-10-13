package net.toddsarratt.GaussTrader;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.sql.*;
import javax.sql.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 	HistoricalPrice(String closeEpoch, double adjClose)  */

public abstract class DBHistoricalPrices {
    private static DataSource dataSource = GaussTrader.getDataSource();
    private static Connection dbConnection;
    private static final Logger LOGGER = LoggerFactory.getLogger(DBHistoricalPrices.class);

    static {
        try {
            dbConnection = dataSource.getConnection();
        } catch(SQLException sqle) {
            LOGGER.warn("SQLException : dataSource.getConnection()", sqle);
        }
    }
	
    static boolean marketWasOpen(MutableDateTime histDateTime) {	
	return !( (Arrays.asList(TradingSession.JULIAN_HOLIDAYS).contains(histDateTime.getDayOfYear())) ||
		  (histDateTime.getDayOfWeek() == DateTimeConstants.SATURDAY) || 
		  (histDateTime.getDayOfWeek() == DateTimeConstants.SUNDAY) );
    }

    static LinkedHashMap<Long, Double> getDBPrices(String ticker, DateTime earliestCloseDate) {
	LOGGER.debug("Entering DBHistoricalPrices.getDBPrices(String {}, DateTime {} ({})", 
		     ticker, earliestCloseDate.getMillis(), earliestCloseDate.toString());
	HistoricalPrice histPrice = null;
	LinkedHashMap<Long, Double> queriedPrices = new LinkedHashMap<>();
	int pricesNeeded = GaussTrader.bollBandPeriod;
	int dbPricesFound = 0;
	ResultSet historicalPriceResultSet = null;
	PreparedStatement sqlStatement = null;
	long closeEpoch = 0l;
	double adjClose = 0.0;
	DateTime dateTimeForLogging;
	try {
	    sqlStatement = dbConnection.prepareStatement("SELECT * FROM prices WHERE ticker = ? AND close_epoch >= ?");
	    sqlStatement.setString(1, ticker);
	    sqlStatement.setLong(2, earliestCloseDate.getMillis());
	    LOGGER.debug("SELECT * FROM prices WHERE ticker = {} AND close_epoch >= {}", ticker, earliestCloseDate.getMillis());
	    historicalPriceResultSet = sqlStatement.executeQuery();
            while(historicalPriceResultSet.next() && (dbPricesFound < pricesNeeded) ) {
		closeEpoch = historicalPriceResultSet.getLong("close_epoch");
		adjClose = historicalPriceResultSet.getDouble("adj_close");
		dateTimeForLogging = new DateTime(closeEpoch, DateTimeZone.forID("America/New_York"));
                LOGGER.debug("Adding {} ({}), {}", closeEpoch, dateTimeForLogging.toString(), adjClose);
                queriedPrices.put(closeEpoch, adjClose);
                dbPricesFound++;
	    }
        } catch(SQLException sqle) {
            LOGGER.info("Unable to get connection to {}", GaussTrader.DB_NAME);
            LOGGER.debug("Caught (SQLException sqle)", sqle);
        } finally {
	    try {
		if(dbConnection != null) {
		    dbConnection.close();
		}
	    } catch (SQLException sqle) {
		LOGGER.info("SQLException trying to close {}", GaussTrader.DB_NAME);
		LOGGER.debug("Caught (SQLException sqle)", sqle);
	    } finally {
		LOGGER.debug("Found {} prices in db", dbPricesFound);
		LOGGER.debug("Returning from database LinkedHashMap of size {}", queriedPrices.size());
		if(dbPricesFound != queriedPrices.size()) {
		    LOGGER.warn("Mismatch between prices found {} and number returned {}, possible duplication?", dbPricesFound, queriedPrices.size());
		}
		return queriedPrices;
	    }
	}
    }

    public static void addStockPrice(String ticker, long dateEpoch, double adjClose) {
	LOGGER.debug("Entering DBHistoricalPrices.addStockPrice(String {}, long {}, double {})", ticker, dateEpoch, adjClose);
	PreparedStatement sqlStatement = null;
	int insertedRowCount;
	try {
            LOGGER.debug("Getting connection to {}", GaussTrader.DB_NAME);
            LOGGER.debug("Inserting historical stock price data for ticker {} into the database.", ticker);
            sqlStatement = dbConnection.prepareStatement("INSERT INTO prices (ticker, adj_close, close_epoch) VALUES (?, ?, ?)");
            sqlStatement.setString(1, ticker);
	    sqlStatement.setDouble(2, adjClose);
            sqlStatement.setLong(3, dateEpoch);
	    LOGGER.debug("Executing INSERT INTO prices (ticker, adj_close, close_epoch) VALUES ({}, {}, {})", ticker, adjClose, dateEpoch);
	    if( (insertedRowCount = sqlStatement.executeUpdate()) != 1) {
		LOGGER.warn("Inserted {} rows. Should have inserted 1 row.", insertedRowCount);
	    }
	} catch(SQLException sqle) {
	    LOGGER.info("Unable to get connection to {}", GaussTrader.DB_NAME);
	    LOGGER.debug("Caught (SQLException sqle)", sqle);
	}
    }
}