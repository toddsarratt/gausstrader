package net.toddsarratt.gaussTrader.persistence.store;

import net.toddsarratt.gaussTrader.Position;
import net.toddsarratt.gaussTrader.domain.Stock;
import net.toddsarratt.gaussTrader.orders.Order;
import net.toddsarratt.gaussTrader.persistence.entity.InstantPrice;
import net.toddsarratt.gaussTrader.portfolio.PortfolioSummary;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Set;

/**
 * The root interface for persistent data storage. The only current subclass implements a PostgreSQL database.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since gaussTrader v0.2
 */
public interface DataStore {

	void resetWatchList();

//   LinkedHashMap<Long, BigDecimal> readPrices(String ticker, Instant earliestCloseDate);

//   LinkedHashMap<Long, BigDecimal> getStoredPrices(String ticker, DateTime earliestCloseDate);

	HashMap<LocalDate, BigDecimal> readHistoricalPrices(String ticker, LocalDate earliestCloseDate);

	void writeStockMetrics(Stock stockToUpdate);

	void writeStockMetrics(Set<Stock> stocksToUpdate);

	void writeStockPrice(String ticker, LocalDate date, BigDecimal adjClose);

	void writeStockPrice(String ticker, InstantPrice instantPrice);

	void writeStockPrice(Stock stock, InstantPrice instantPrice);

	boolean tickerPriceInStore(String ticker);

	void deactivateStock(String tickerToRemove);

	boolean portfolioInStore(String name);

	PortfolioSummary getPortfolioSummary(String portfolioName);

	Set<Position> getPortfolioPositions() throws SQLException;

	Set<Order> getPortfolioOrders();

	void write(Order order);

	void write(Position position);

	void write(PortfolioSummary summary);

	void close(Order orderToFill);

	void close(Position optionPositionToExercise);

}
