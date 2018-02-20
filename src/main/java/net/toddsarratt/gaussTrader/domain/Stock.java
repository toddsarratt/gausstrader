package net.toddsarratt.gaussTrader.domain;

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

import net.toddsarratt.gaussTrader.GaussTrader;
import net.toddsarratt.gaussTrader.market.Market;
import net.toddsarratt.gaussTrader.persistence.entity.InstantPrice;
import net.toddsarratt.gaussTrader.persistence.store.DataStore;
import net.toddsarratt.gaussTrader.singletons.Constants;
import net.toddsarratt.gaussTrader.singletons.SecurityType;
import net.toddsarratt.gaussTrader.technicals.BollingerBands;
import net.toddsarratt.gaussTrader.technicals.MovingAverages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static net.toddsarratt.gaussTrader.singletons.SecurityType.STOCK;

public class Stock implements Security {
	private static final Market MARKET = GaussTrader.getMarket();
	private static final DataStore DATA_STORE = GaussTrader.getDataStore();
	private static final Logger LOGGER = LoggerFactory.getLogger(Stock.class);
	private final String ticker;
	private MovingAverages movingAverages;
	//	public LinkedList<Dividend> dividendsPaid = null;
	private BollingerBands bollingerBands;
	private InstantPrice lastPrice;

	/**
	 * Private constructor for Stock class. Use static factory method of() to create objects of this class
	 *
	 * @param ticker a String representing the ticker
	 */
	private Stock(String ticker,
	              MovingAverages movingAverages,
	              BollingerBands bollingerBands
	) {
		LOGGER.debug("Entering constructor Stock(String {})", ticker);
		this.ticker = ticker;
		this.movingAverages = movingAverages;
		this.bollingerBands = bollingerBands;
	}

	public static Stock of(String ticker) {
		LOGGER.debug("Entering factory method of(\"{}\")", ticker);
		if (MARKET.tickerValid(ticker)) {
			MovingAverages movingAverages = MARKET.getMovingAverages(ticker);
			HashMap<LocalDate, BigDecimal> historicalPriceMap = fetchHistoricalPrices(ticker);
			BollingerBands bollingerBands = BollingerBands.generateBollingerBands(historicalPriceMap);
			return new Stock(ticker, movingAverages, bollingerBands);
		}
		throw new IllegalArgumentException("Ticker invalid");
	}

	private static HashMap<LocalDate, BigDecimal> fetchHistoricalPrices(String ticker) {
		int datesNeededCount = Constants.getBollBandPeriod();
		Set<LocalDate> datesNeeded = priorOpenMarketDates(datesNeededCount);
		HashMap<LocalDate, BigDecimal> historicalPriceMap = fetchStoredHistoricalPrices(ticker, datesNeeded);
		if (historicalPriceMap.size() < datesNeededCount) {
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
		// TODO: Not the right way to get the TZ
		LocalDate earliestDate = LocalDate.now(MARKET.getClosingZonedDateTime().getZone());
		for (LocalDate date : dates) {
			if (date.isBefore(earliestDate)) {
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
		// TODO: Not the right way to get the TZ
		LocalDate dateToCheck = LocalDate.now(MARKET.getClosingZonedDateTime().getZone());
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

	@Override
	public String getTicker() {
		return ticker;
	}

	@Override
	public SecurityType getSecType() {
		return STOCK;
	}

	public boolean isStock() {
		return true;
	}

	public boolean isOption() {
		return false;
	}

	public MovingAverages getMovingAverages() {
		return movingAverages;
	}

	public BollingerBands getBollingerBands() {
		return bollingerBands;
	}

	@Override
	public InstantPrice getLastPrice() {
		return lastPrice;
	}

	public void setLastPrice(InstantPrice lastPrice) {
		this.lastPrice = lastPrice;
	}

	@Override
	public String toString() {
		return "Stock{" +
				"ticker='" + ticker + '\'' +
				", movingAverages=" + movingAverages +
				", bollingerBands=" + bollingerBands +
				", lastPrice=" + lastPrice +
				'}';
	}
}
