package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * This class implements code for carrying out a particular trading strategy. Probably this should be an interface
 * and various strategies sub-classed. For now this is the only trading strategy supported.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */

class TradingStrategy {
	private static final Logger LOGGER = LoggerFactory.getLogger(TradingStrategy.class);

	private static PriceBasedAction createCallAction(Stock stock, BigDecimal stockPrice, Portfolio portfolio) {
		LOGGER.debug("Entering createCallAction(Stock {}, BigDecimal {})", stock.getTicker(), stockPrice);
		if (portfolio.countUncoveredLongStockPositions(stock) < 1) {
			LOGGER.info("Open long {} positions is equal or less than current short calls positions. Taking no action.", stock.getTicker());
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(2)) >= 0) {
			LOGGER.info("Stock {} at ${} is above 2nd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(2));
			return new PriceBasedAction(stockPrice,
					true,
					"SELL",
					SecurityType.CALL,
					Math.min(portfolio.numberOfOpenStockLongs(stock), 5));
		}
	  /* TODO : Consider removing this if statement. We should not be in this method if the condition wasn't met, though
	  * it does protect against misuse of the API. Also, why is the PriceBasedAction exactly the same as above? */
		if (stockPrice.compareTo(stock.getBollingerBand(1)) >= 0) {
			LOGGER.info("Stock {} at ${} is above 1st Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(1));
			return new PriceBasedAction(stockPrice,
					true,
					"SELL",
					SecurityType.CALL,
					Math.min(portfolio.numberOfOpenStockLongs(stock), 5));
		}
		return PriceBasedAction.DO_NOTHING;
	}

	private static PriceBasedAction createPutAction(Stock stock, BigDecimal stockPrice, Portfolio portfolio) {
		LOGGER.debug("Entering createPutAction(Stock {}, BigDecimal {})", stock.getTicker(), stockPrice);
		int openPutShorts = portfolio.numberOfOpenPutShorts(stock);
		int maximumContracts = Constants.STOCK_PCT_OF_PORTFOLIO.divide(Constants.BIGDECIMAL_ONE_HUNDRED, 3, RoundingMode.HALF_UP)
				.divide(stockPrice.multiply(Constants.BIGDECIMAL_ONE_HUNDRED), 3, RoundingMode.HALF_UP)
				.multiply(portfolio.calculateNetAssetValue())
				.intValue();
		if (stockPrice.compareTo(stock.getBollingerBand(5)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 3rd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(5));
			if (openPutShorts < maximumContracts) {
				return new PriceBasedAction(stockPrice, true, "SELL", SecurityType.PUT, Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(4)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 2nd Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(4));
			if (openPutShorts < maximumContracts / 2) {
				return new PriceBasedAction(stockPrice, true, "SELL", SecurityType.PUT, Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
			return PriceBasedAction.DO_NOTHING;
		}
	  /* TODO : Consider removing this if statement. We should not be in this method if the condition wasn't met */
		if (stockPrice.compareTo(stock.getBollingerBand(3)) <= 0) {
			LOGGER.info("Stock {} at ${} is below 1st Bollinger Band of {}", stock.getTicker(), stockPrice, stock.getBollingerBand(3));
			if (openPutShorts < maximumContracts / 4) {
				return new PriceBasedAction(stockPrice, true, "SELL", SecurityType.PUT, Math.max(maximumContracts / 4, 1));
			}
			LOGGER.info("Open short put {} positions equals {}. Taking no action.", stock.getTicker(), openPutShorts);
		}
		return PriceBasedAction.DO_NOTHING;
	}

	/**
	 * Decide if a security's current price triggers a predetermined event. For this trading strategy, a stock price
	 * following below its Bollinger band triggers the sale of a put. A stock price above its Bollinger band triggers
	 * the sale of a covered call.
	 *
	 * @param stock      stock whose price may trigger an action
	 * @param stockPrice last known price to compare against stock's bollinger bands
	 * @param portfolio  portfolio being traded
	 * @return PriceBasedAction based on input parameters and trade strategy
	 */
	static PriceBasedAction findActionToTake(Stock stock, BigDecimal stockPrice, Portfolio portfolio) {
		LOGGER.debug("Entering findActionToTake(Stock {})", stock.getTicker());
		LOGGER.debug("Comparing current price ${} against Bollinger Bands {}", stockPrice, stock.describeBollingerBands());
		if (stockPrice.compareTo(stock.getBollingerBand(1)) >= 0) {
			return createCallAction(stock, stockPrice, portfolio);
		}
		if (stock.getFiftyDma().compareTo(stock.getTwoHundredDma()) < 0) {
			LOGGER.info("Stock {} 50DMA < 200DMA. No further checks.", stock.getTicker());
			return PriceBasedAction.DO_NOTHING;
		}
		if (stockPrice.compareTo(stock.getBollingerBand(3)) <= 0) {
			return createPutAction(stock, stockPrice, portfolio);
		}
		LOGGER.info("Stock {} at ${} is within Bollinger Bands", stock.getTicker(), stockPrice);
		return PriceBasedAction.DO_NOTHING;
	}
}
