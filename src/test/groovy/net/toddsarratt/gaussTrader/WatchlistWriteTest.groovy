package net.toddsarratt.gaussTrader

import org.testng.annotations.Test

/**
 * Created by tsarratt on 10/7/2014.
 */

@Deprecated
/* Better unit testing coming in v0.2 */
class WatchlistWriteTest {
    /* This just checks to make sure the method executes
    It make no guarantee that its logical performance is correct
     */
    @Test
    public void testDbResetWatchList() {
        Stock testStock = new Stock("XOM")
        testStock.populateStockInfo()
        testStock.calculateBollingerBands()
        WatchList.updateDbStockMetrics(testStock)
        /* Now go check the database manually */
    }
    @Test
    public void testWatchlistDeactivate() {
        WatchList.deactivateStock("XOM");
        /* Should not throw exception */
    }
}
