package net.toddsarratt.gaussTrader.technicals;

import net.toddsarratt.gaussTrader.singletons.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public class BollingerBands {
	private static final Logger LOGGER = LoggerFactory.getLogger(BollingerBands.class);
	private BigDecimal simpleMovingAverage;
	private BigDecimal oneStandardDeviation;

	private BollingerBands() {}

	public static BollingerBands generateBollingerBands(Map<LocalDate, BigDecimal> priceMap) {
		LOGGER.debug("Entering generateBollingerBands()");
		BollingerBands bollingerBands = new BollingerBands();

		if (priceMap.containsValue(Constants.BIGDECIMAL_MINUS_ONE)) {
			LOGGER.warn("Not enough historical data to calculate Bollinger Bands");
			return null;
		}

		Collection<BigDecimal> historicalPriceArray = priceMap.values();

		BigDecimal sigmaSimpleMovingAverage = historicalPriceArray
				.stream()
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		LOGGER.debug("sigmaSimpleMovingAverage = {}", sigmaSimpleMovingAverage);

		BigDecimal period = new BigDecimal(Constants.getBollBandPeriod());

		BigDecimal simpleMovingAverage = sigmaSimpleMovingAverage.divide(period, 3, RoundingMode.HALF_UP);
		LOGGER.debug("simpleMovingAverage = {}", simpleMovingAverage);

		BigDecimal sigmaStandardDeviation = historicalPriceArray
				.stream()
				.map(adjClose -> adjClose.subtract(simpleMovingAverage))
				.map(distance -> distance.pow(2))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		LOGGER.debug("sigmaStandardDeviation = {}", sigmaStandardDeviation);

		BigDecimal standardDeviation = BigDecimal.valueOf(Math.sqrt(sigmaStandardDeviation.divide(period, 3, RoundingMode.HALF_UP).doubleValue()));
		LOGGER.debug("standardDeviation = {}", standardDeviation);

		bollingerBands.setSimpleMovingAverage(simpleMovingAverage);
		bollingerBands.setOneStandardDeviation(standardDeviation);
		return bollingerBands;
	}

	public BigDecimal calcSdFromSma(int deviations) {
		return getSimpleMovingAverage().add(getOneStandardDeviation().multiply(BigDecimal.valueOf(deviations)));
	}

	public BigDecimal getSimpleMovingAverage() {
		return simpleMovingAverage;
	}

	public void setSimpleMovingAverage(BigDecimal simpleMovingAverage) {
		this.simpleMovingAverage = simpleMovingAverage;
	}

	public BigDecimal getOneStandardDeviation() {
		return oneStandardDeviation;
	}

	public void setOneStandardDeviation(BigDecimal oneStandardDeviation) {
		this.oneStandardDeviation = oneStandardDeviation;
	}
}
