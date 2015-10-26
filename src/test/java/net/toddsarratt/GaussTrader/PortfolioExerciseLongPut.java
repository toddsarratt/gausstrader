package net.toddsarratt.GaussTrader;

// import java.io.IOException;
// import java.net.MalformedURLException;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Deprecated
/* Better unit testing coming in v0.2 */
public class PortfolioExerciseLongPut {
   @Test
   public void testPortfolioExerciseLongPut() {
      Stock testStock = new Stock();
      String testTicker = "LONGPUT";

      testStock.setTicker(testTicker);

      Portfolio portfolio = new Portfolio();
      portfolio.setName("exerciseLongPut");

      Position longStockPosition = new Position();
      longStockPosition.setTicker(testTicker);
      longStockPosition.setSecType("STOCK");
      longStockPosition.setLongPosition(true);
      longStockPosition.setNumberTransacted(100);
      longStockPosition.setPriceAtOpen(100.00);
      longStockPosition.calculateCostBasis();
      longStockPosition.setNetAssetValue(15_000.00);
      portfolio.addNewPosition(longStockPosition);

      assertEquals(portfolio.calculateTotalCash(), 990_000.00);
      assertEquals(portfolio.getReservedCash(), 0.00);
      assertEquals(portfolio.getFreeCash(), 990_000.00);
      assertEquals(portfolio.countUncoveredLongStockPositions(testStock), 1);
      assertEquals(portfolio.numberOfOpenStockLongs(testStock), 1);
      assertEquals(portfolio.numberOfOpenPutLongs(testStock), 0);

      Position longPutPosition = new Position();
      longPutPosition.setTicker(testTicker);
      longPutPosition.setSecType("PUT");
      longPutPosition.setUnderlyingTicker(testTicker);
      longPutPosition.setStrikePrice(100.00);
      longPutPosition.setLongPosition(true);
      longPutPosition.setNumberTransacted(2);
      longPutPosition.setPriceAtOpen(0.75);
      //	longPutPosition.setCostBasis(75.00);
      longPutPosition.calculateCostBasis();
      longPutPosition.calculateClaimAgainstCash();
      portfolio.addNewPosition(longPutPosition);

      assertEquals(portfolio.calculateTotalCash(), 989_850.00);
      assertEquals(portfolio.getReservedCash(), 0.00);
      assertEquals(portfolio.getFreeCash(), 989_850.00);
      assertEquals(portfolio.countUncoveredLongStockPositions(testStock), 1);
      assertEquals(portfolio.numberOfOpenStockLongs(testStock), 1);
      assertEquals(portfolio.numberOfOpenPutLongs(testStock), 2);

      portfolio.exerciseOption(longPutPosition);

      assertEquals(portfolio.calculateTotalCash(), 1_009_850.00);
      assertEquals(portfolio.getReservedCash(), 0.00);
      assertEquals(portfolio.getFreeCash(), 1_009_850.00);
      assertEquals(portfolio.countUncoveredLongStockPositions(testStock), 0);
      assertEquals(portfolio.numberOfOpenStockLongs(testStock), 0);
      assertEquals(portfolio.numberOfOpenPutShorts(testStock), 0);
   }
}