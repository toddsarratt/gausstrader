package net.toddsarratt.GaussTrader;

import java.io.IOException;

public abstract class Security {
	
    abstract double lastTick() throws IOException;

    //    public abstract static double lastTick(String ticker) throws IOException;
	
    abstract double lastBid() throws IOException;
	
    abstract double lastAsk() throws IOException;

    abstract String getTicker();

    abstract double getPrice();

    abstract String getSecType();
}
