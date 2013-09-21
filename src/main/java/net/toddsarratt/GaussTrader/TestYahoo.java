package net.toddsarratt.GaussTrader;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestYahoo {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestYahoo.class);
    public static void main(String[] args) {
	try {
	    String[] test = Stock.askYahoo("XOM", "sl1d1t1m3m4");
	    for(String s : test) {
		System.out.print(s + " | ");
	    }
	    LOGGER.info("IBM130222C00200000 is a valid options ticker : {}", Option.optionTickerValid("IBM130222C00200000"));
	    LOGGER.info("XOM130720P00070000 is a valid options ticker : {}", Option.optionTickerValid("XOM130720P00070000"));	    
	    LOGGER.info("IBM130222C00200001 is a valid options ticker : {}", Option.optionTickerValid("IBM130222C00200001"));
	} catch(IOException ioe) {
	    LOGGER.info("Cannot connect to yahoo!");
	    LOGGER.debug("Caught (IOException ioe)", ioe);
	}
    }
}
