package net.toddsarratt.gaussTrader.securities;

import net.toddsarratt.gaussTrader.singletons.SecurityType;

interface Security {

	String getTicker();

	SecurityType getSecType();

	boolean isStock();

	boolean isOption();
}
