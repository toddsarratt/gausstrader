package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;

/* 	HistoricalPrice(String closeEpoch, double adjClose)  */

public abstract class DBHistoricalPrices {
   private static DataStore dataStore = GaussTrader.getDataStore();
   private static Connection dbConnection;
   private static final Logger LOGGER = LoggerFactory.getLogger(DBHistoricalPrices.class);


}