package net.toddsarratt.GaussTrader;

/**
 * The {@code DataStore} class provides
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since GaussTrader v0.2
 */
public interface DataStore {
   void updateStockPriceToStorage(Stock stock);

   void resetWatchList();

   void updateStockMetricsToStorage(Stock stockToUpdate);
}
