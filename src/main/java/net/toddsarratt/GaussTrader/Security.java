package net.toddsarratt.GaussTrader;

interface Security {

	String getTicker();

	SecurityType getSecType();

	boolean isStock();

	boolean isOption();
}
