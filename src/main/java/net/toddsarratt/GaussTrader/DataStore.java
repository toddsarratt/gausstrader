package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * The {@code DataStore} class provides
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.2
 */
public interface DataStore {
   void updateStockPriceToStorage(Stock stock);

   void resetWatchList();

   LinkedHashMap<Long, Double> getStoredPrices(String ticker, DateTime earliestCloseDate);

   void updateStockMetricsToStorage(Stock stockToUpdate);

   void updateStockMetricsToStorage(Set<Stock> stockSetToUpdate);

   void addStockPriceToStore(String ticker, long dateEpoch, double adjClose);

   boolean tickerPriceInStore(String ticker);

   void deactivateStock(String tickerToRemove);

   boolean portfolioInStore(String name);

   PortfolioSummary getPortfolioSummary(String portfolioName);

   Set<Position> getPortfolioPositions();

   Set<Order> getPortfolioOrders();

   void addOrder(Order orderToAdd);

   void addPosition(Position position);

   void insertPosition(Position positionTakenByOrder);

   void closeOrder(Order orderToFill);

   void closePosition(Position optionPositionToExercise);
}
