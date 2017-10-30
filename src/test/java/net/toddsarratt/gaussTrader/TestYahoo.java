package net.toddsarratt.gaussTrader;

import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertTrue;

@Deprecated
/* Better unit testing coming in v0.2 */
public class TestYahoo {
   @Test
   public void testStockyahooGummyApi() {
      try {
         String[] testResponse = Stock.yahooGummyApi("XOM", "sl1");
         assertTrue(testResponse[0].equals("XOM"));
         assertTrue(Double.parseDouble(testResponse[1]) > 0.00);
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
   }
}
