package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Quote {
    public String ticker = null;
    public String name = null;
    public Double price = 0.00;
    public double fiftyDMA = 0.00;
    public double twoHundredDMA = 0.00;
    public double diffFiftyDMA = 0.00;
    public double diffTwoHundredDMA = 0.00;
	
    private static final Logger LOGGER = LoggerFactory.getLogger(Quote.class);

    Quote() {}
	
    Quote(String[] quoteString) {
	this.ticker = quoteString[0];
	this.name = quoteString[1];
	this.price = Double.parseDouble(quoteString[2]);
	this.fiftyDMA = Double.parseDouble(quoteString[3]);
	this.twoHundredDMA = Double.parseDouble(quoteString[4]);
	this.diffFiftyDMA = Double.parseDouble(quoteString[5]);
	this.diffTwoHundredDMA = Double.parseDouble(quoteString[6]);
	LOGGER.info("New quote created");
    }    
}
