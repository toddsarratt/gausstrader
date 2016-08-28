package net.toddsarratt.GaussTrader;

/**
 * The class provides a workaround for Java's lack of tuples.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.1
 */

public class PriceBasedAction {
	private static final PriceBasedAction DO_NOTHING = new PriceBasedAction(false, "", 0);
	boolean doSomething;
	String optionType;
	int contractsToTransact;

	PriceBasedAction(boolean doSomething, String optionType, int contractsToTransact) {
		this.doSomething = doSomething;
		this.optionType = optionType;
		this.contractsToTransact = contractsToTransact;
	}
}
