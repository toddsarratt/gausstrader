package net.toddsarratt.gaussTrader;

@SuppressWarnings("serial")
public class InsufficientFundsException extends Exception {
	String ticker = null;
	double requiredFreeCash = 0.00;
	double currentFreeCash = 0.00;

	InsufficientFundsException() {
		super();
		ticker = "unknown";
	}

	public InsufficientFundsException(String ticker, double requiredFreeCash, double currentFreeCash) {
		super();
		this.ticker = ticker;
		this.requiredFreeCash = requiredFreeCash;
		this.currentFreeCash = currentFreeCash;
	}
}
