package net.toddsarratt.gaussTrader.securities;

interface Security {

	String getTicker();

	SecurityType getSecType();

	boolean isStock();

	boolean isOption();
}
