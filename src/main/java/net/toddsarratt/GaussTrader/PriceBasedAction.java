package net.toddsarratt.GaussTrader;

/**
 * The class provides a workaround for Java's lack of tuples.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.1
 */

public class PriceBasedAction {
	static final PriceBasedAction DO_NOTHING = new PriceBasedAction(false, "", "", 0);
	private final boolean doSomething;
	private final String buyOrSell;
	private final String securityType;
	private final int contractsToTransact;

	PriceBasedAction(boolean doSomething, String buyOrSell, String securityType, int contractsToTransact) {
		this.doSomething = doSomething;
		this.buyOrSell = buyOrSell;
		this.securityType = securityType;
		this.contractsToTransact = contractsToTransact;
	}

	public boolean doSomething() {
		return doSomething;
	}

	public String getBuyOrSell() {
		return buyOrSell;
	}

	public String getSecurityType() {
		return securityType;
	}

	public int getContractsToTransact() {
		return contractsToTransact;
	}
}
