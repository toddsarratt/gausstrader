package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * The {@code DataStore} interface provides
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.2
 */
public interface DataStore {

   void resetWatchList();

   LinkedHashMap<Long, BigDecimal> readPrices(String ticker, Instant earliestCloseDate);

   LinkedHashMap<Long, BigDecimal> getStoredPrices(String ticker, DateTime earliestCloseDate);

   LinkedHashMap<Long, BigDecimal> readHistoricalPrices(String ticker, DateTime earliestCloseDate);

   void writeStockMetrics(Stock stockToUpdate);

   void writeStockMetrics(Set<Stock> stocksToUpdate);

   void writeStockPrice(String ticker, long dateEpoch, BigDecimal adjClose);

   void writeStockPrice(Stock stock);

   boolean tickerPriceInStore(String ticker);

   void deactivateStock(String tickerToRemove);

   boolean portfolioInStore(String name);

   PortfolioSummary getPortfolioSummary(String portfolioName);

   Set<Position> getPortfolioPositions();

   Set<Order> getPortfolioOrders();

   void write(Order order);

   void write(Position position);

   void write(PortfolioSummary summary);

   void close(Order orderToFill);

   void close(Position optionPositionToExercise);

}
