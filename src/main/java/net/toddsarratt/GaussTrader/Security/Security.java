package net.toddsarratt.GaussTrader.Security;

public interface Security {

	String getTicker();

	SecurityType getSecType();

	boolean isStock();

	boolean isOption();
}
