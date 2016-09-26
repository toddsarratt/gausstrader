package net.toddsarratt.GaussTrader;

/*	
 * bollingerBand[0] = moving average (period determined by command line args, default 20DMA)
 * SD multiple determined by command line, defaults below
 * bollingerBand[1] = upperBoll1 = MA + 2 standard deviation
 * bollingerBand[2] = upperBoll2 = MA + 2.5 standard deviations
 * bollingerBand[3] = lowerBoll1 = MA - 2 standard deviation
 * bollingerBand[4] = lowerBoll2 = MA - 2.5 standard deviations
 * bollingerBand[5] = lowerBoll3 = MA - 3 standard deviations
 * TODO: Handle dividends
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Stock implements Security {
	private static final Market MARKET = GaussTrader.getMarket();
	private static final DataStore DATA_STORE = GaussTrader.getDataStore();
	private static final Logger LOGGER = LoggerFactory.getLogger(Stock.class);
	private final String ticker;
	private BigDecimal fiftyDma;
	private BigDecimal twoHundredDma;
	//	public LinkedList<Dividend> dividendsPaid = null;
	private BigDecimal[] bollingerBands = new BigDecimal[6];

	/**
	 * Private constructor for Stock class. Use static factory method with() to create objects with this class
	 *
	 * @param ticker a String representing the ticker
	 */
	private Stock(String ticker,
	              BigDecimal fiftyDma,
	              BigDecimal twoHundredDma,
	              BigDecimal[] bollingerBands
	) {
		LOGGER.debug("Entering constructor Stock(String {})", ticker);
		this.ticker = ticker;
		this.fiftyDma = fiftyDma;
		this.twoHundredDma = twoHundredDma;
		this.bollingerBands = bollingerBands;
	}

	public static Stock of(String ticker) {
		LOGGER.debug("Entering factory method with(\"{}\")", ticker);
		if (MARKET.tickerValid(ticker)) {
			BigDecimal[] movingAverages = MARKET.getMovingAverages(ticker);
			BigDecimal fiftyDma = movingAverages[0];
			BigDecimal twoHundredDma = movingAverages[1];
			// TODO: Move this to WatchList
			// dataStore.writeStockMetrics(newStock);
			HashMap<LocalDate, BigDecimal> historicalPriceMap = fetchHistoricalPrices(ticker);
			BigDecimal[] bollingerBands = calculateBollingerBands(historicalPriceMap);
			return new Stock(ticker, fiftyDma, twoHundredDma, bollingerBands);
		}
		throw new IllegalArgumentException("Ticker invalid");
	}

	private static HashMap<LocalDate, BigDecimal> fetchHistoricalPrices(String ticker) {
		int datesNeededCount = Constants.BOLL_BAND_PERIOD;
		Set<LocalDate> datesNeeded = priorOpenMarketDates(datesNeededCount);
		HashMap<LocalDate, BigDecimal> historicalPriceMap = fetchStoredHistoricalPrices(ticker, datesNeeded);
		if(historicalPriceMap.size() < datesNeededCount) {
			Set<LocalDate> missingPriceDates = datesNeeded.stream()
					.filter(dateNeeded -> !historicalPriceMap.containsKey(dateNeeded))
					.collect(Collectors.toSet());
			HashMap<LocalDate, BigDecimal> missingPriceMap = fetchMissingPricesFromMarket(ticker, missingPriceDates);
			missingPriceDates.forEach(missingDate -> historicalPriceMap.put(missingDate, missingPriceMap.get(missingDate)));
			updateStoreMissingPrices(ticker, missingPriceDates, historicalPriceMap);
		}
		return historicalPriceMap;
	}

	// TODO: Replace with stream
	private static LocalDate findEarliestDate(Set<LocalDate> dates) {
		LocalDate earliestDate = LocalDate.now(Constants.MARKET_ZONE);
		for(LocalDate date: dates) {
			if(date.isBefore(earliestDate)) {
				earliestDate = date;
			}
		}
		return earliestDate;
	}

	private static HashMap<LocalDate, BigDecimal> fetchStoredHistoricalPrices(String ticker, Set<LocalDate> datesToRetrieve) {
		return DATA_STORE.readHistoricalPrices(ticker, findEarliestDate(datesToRetrieve));
	}

	private static HashMap<LocalDate, BigDecimal> fetchMissingPricesFromMarket(String ticker, Set<LocalDate> datesToRetrieve) {
		LOGGER.debug("Calculating date range for missing stock prices.");
		return MARKET.readHistoricalPrices(ticker, findEarliestDate(datesToRetrieve));
	}

	private static void updateStoreMissingPrices(String ticker, Set<LocalDate> missingPriceDates, HashMap<LocalDate, BigDecimal> priceMap) {
		missingPriceDates.forEach(date -> DATA_STORE.writeStockPrice(ticker, date, priceMap.get(date)));
	}

	private static Set<LocalDate> priorOpenMarketDates(int datesNeeded) {
		LOGGER.debug("Entering openMarketDates()");
	    Set<LocalDate> openMarketDates = new HashSet<>();
		LocalDate dateToCheck = LocalDate.now(Constants.MARKET_ZONE);
		LOGGER.debug("Looking for valid open market dates backwards from {} ", dateToCheck);
		for (int checkedDates = 0; checkedDates < datesNeeded; checkedDates++) {
			dateToCheck = dateToCheck.minusDays(1);
			while (!MARKET.isOpenMarketDate(dateToCheck)) {
				dateToCheck = dateToCheck.minusDays(1);
			}
			openMarketDates.add(dateToCheck);
			LOGGER.debug("openMarketDates.add({})", dateToCheck);
		}
		LOGGER.debug("openMarketDates == {}", openMarketDates);
		return openMarketDates;
	}


	private static BigDecimal[] calculateBollingerBands(Map<LocalDate, BigDecimal> priceMap) {
		LOGGER.debug("Entering calculateBollingerBands()");
		BigDecimal[] bollingerBands = new BigDecimal[6];
		Arrays.fill(bollingerBands, Constants.BIGDECIMAL_MINUS_ONE);
		if (priceMap.containsValue(Constants.BIGDECIMAL_MINUS_ONE)) {
			LOGGER.warn("Not enough historical data to calculate Bollinger Bands");
			return bollingerBands;
		}
			BigDecimal currentSMASum;
			BigDecimal currentSMA;
			BigDecimal currentSDSum;
			BigDecimal currentSD;
			Collection<BigDecimal> historicalPriceArray = priceMap.values();
			currentSMASum = historicalPriceArray
					.stream()
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			LOGGER.debug("currentSMASum = {}", currentSMASum);
			BigDecimal period = new BigDecimal(Constants.BOLL_BAND_PERIOD);
			currentSMA = currentSMASum.divide(period, 3, RoundingMode.HALF_UP);
			LOGGER.debug("currentSMA = {}", currentSMA);
			currentSDSum = historicalPriceArray
					.stream()
					.map(adjClose -> adjClose.subtract(currentSMA))
					.map(distance -> distance.pow(2))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			LOGGER.debug("currentSDSum = {}", currentSDSum);
			// Seriously, is there no square root method in BigDecimal?
			currentSD = BigDecimal.valueOf(Math.sqrt(currentSDSum.divide(period, 3, RoundingMode.HALF_UP).doubleValue()));
			LOGGER.debug("currentSD = {}", currentSD);
			bollingerBands[0] = currentSMA;
			bollingerBands[1] = currentSMA.add(currentSD.multiply(Constants.BOLLINGER_SD1));
			bollingerBands[2] = currentSMA.add(currentSD.multiply(Constants.BOLLINGER_SD2));
			bollingerBands[3] = currentSMA.subtract(currentSD.multiply(Constants.BOLLINGER_SD1));
			bollingerBands[4] = currentSMA.subtract(currentSD.multiply(Constants.BOLLINGER_SD2));
			bollingerBands[5] = currentSMA.subtract(currentSD.multiply(Constants.BOLLINGER_SD3));
		return bollingerBands;
	}

	@Override
	public String getTicker() {
		return ticker;
	}

	@Override
	public String getSecType() {
		return "STOCK";
	}

	public boolean isStock() {
		return true;
	}

	public boolean isOption() {
		return false;
	}

	public BigDecimal getBollingerBand(int index) {
		return bollingerBands[index];
	}

	public BigDecimal getFiftyDma() {
		return fiftyDma;
	}

	public BigDecimal getTwoHundredDma() {
		return twoHundredDma;
	}

	String describeBollingerBands() {
		return "SMA " + bollingerBands[0] +
				" Upper 1st " + bollingerBands[1] +
				" 2nd " + bollingerBands[2] +
				" Lower 1st " + bollingerBands[3] +
				" 2nd " + bollingerBands[4] +
				" 3rd " + bollingerBands[5];
	}

	// TODO : This is a sorry toString()
	@Override
	public String toString() {
		return ticker;
	}
}
