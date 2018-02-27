package net.toddsarratt.gaussTrader.technicals;

import java.math.BigDecimal;

public class MovingAverages {

	private BigDecimal twentyDma;
	private BigDecimal fiftyDma;
	private BigDecimal twoHundredDma;

	public MovingAverages(BigDecimal twentyDma, BigDecimal fiftyDma, BigDecimal twoHundredDma) {
		this.twentyDma = twentyDma;
		this.fiftyDma = fiftyDma;
		this.twoHundredDma = twoHundredDma;
	}

	public BigDecimal getTwentyDma() {
		return twentyDma;
	}

	public void setTwentyDma(BigDecimal twentyDma) {
		this.twentyDma = twentyDma;
	}

	public BigDecimal getFiftyDma() {
		return fiftyDma;
	}

	public void setFiftyDma(BigDecimal fiftyDma) {
		this.fiftyDma = fiftyDma;
	}

	public BigDecimal getTwoHundredDma() {
		return twoHundredDma;
	}

	public void setTwoHundredDma(BigDecimal twoHundredDma) {
		this.twoHundredDma = twoHundredDma;
	}
}
