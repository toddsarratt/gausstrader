package net.toddsarratt.gaussTrader;

@Deprecated
/* Better unit testing coming in v0.2 */
public class StockTestTickerAAPL {/*
   @Test(enabled = true)
   public void testRetrieveAAPLFromYahoo() {
      String testTicker = "AAPL";
      try {
         Stock testStock = new Stock();
         testStock.setTicker(testTicker);
       * Verify price retrieval methods return positive values *
         assertTrue(testStock.lastTick() > 0.00);
         assertTrue(testStock.lastBid() > 0.00);
         assertTrue(testStock.lastAsk() > 0.00);

	    * Below methods are called by Stock(String) constructor as called by gaussTrader.addTickerlistToTradeableList() on each ticker *
	      testStock.populateStockInfo();
         testStock.populateHistoricalPricesYahoo();
         testStock.calculateBollingerBands();

	    * Make sure at least the bollinger bands have SOME data, and in the correct ascending or descending orders, as they were *
         assertTrue(testStock.getBollingerBand(2) > testStock.getBollingerBand(1));
         assertTrue(testStock.getBollingerBand(1) > testStock.getBollingerBand(0));
         assertTrue(testStock.getBollingerBand(3) < testStock.getBollingerBand(0));
         assertTrue(testStock.getBollingerBand(4) < testStock.getBollingerBand(3));
         assertTrue(testStock.getBollingerBand(5) < testStock.getBollingerBand(4));

	    * Test code that was moved out of the constructor and into its own method *
         assertTrue(testStock.tickerValid());

      } catch (MalformedURLException mue) {
         mue.printStackTrace();
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }*/
}