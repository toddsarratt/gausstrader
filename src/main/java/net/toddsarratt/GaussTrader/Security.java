package net.toddsarratt.GaussTrader;

public interface Security {

	String getTicker();

	String getSecType();

	boolean isStock();

	boolean isOption();
}
