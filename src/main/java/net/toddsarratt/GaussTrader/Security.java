package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;

public interface Security {

	InstantPrice lastTick();

	//    public abstract static double lastTick(String ticker) throws IOException;

	InstantPrice lastBid();

	InstantPrice lastAsk();

	String getTicker();

	BigDecimal getPrice();

	String getSecType();

	boolean isStock();

	boolean isOption();
}
