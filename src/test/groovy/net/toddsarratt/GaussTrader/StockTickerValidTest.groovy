package net.toddsarratt.GaussTrader
import org.testng.annotations.Test

import static org.testng.Assert.assertFalse
import static org.testng.Assert.assertTrue
/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 1/21/14
 */

@Deprecated
/* Better unit testing coming in v0.2 */
class StockTickerValidTest {
    Stock stockInDb = new Stock(ticker: "XOM")
    Stock stockAtYahoo = new Stock(ticker: "ARRS")
    Stock invalidStock = new Stock(ticker: "VOID_NULL")

    @Test
    public void testTickerValidDb() {
        assertTrue stockInDb.tickerValidDb()
        assertFalse stockAtYahoo.tickerValidDb()
        assertFalse invalidStock.tickerValidDb()
    }
    @Test
    public void testTickerValidYahoo() {
        assertTrue stockInDb.tickerValidYahoo()
        assertTrue stockAtYahoo.tickerValidYahoo()
        assertFalse invalidStock.tickerValidYahoo()
    }
    @Test
    public void testTickerValid() {
        assertTrue stockInDb.tickerValid()
        assertTrue stockAtYahoo.tickerValid()
        assertFalse invalidStock.tickerValid()
    }
}
