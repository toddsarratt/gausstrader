package net.toddsarratt.GaussTrader;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Deprecated
/* Better unit testing coming in v0.2 */
public class PortfolioExerciseShortCall {
   @Test
   public void testPortfolioExerciseShortCall() {
      Stock testStock = new Stock();
      String testTicker = "SHORTCALL";

      testStock.setTicker(testTicker);

      Portfolio portfolio = new Portfolio();
      portfolio.setName("exerciseShortCall");
      Position longStockPosition = new Position();

      longStockPosition.setTicker(testTicker);
      longStockPosition.setSecType("STOCK");
      longStockPosition.setLongPosition(true);
      longStockPosition.setNumberTransacted(100);
      longStockPosition.setPriceAtOpen(100.00);
      longStockPosition.calculateCostBasis();
   /*
	 * getLastTick() is always read from Yahoo! when called
	 * So this setter is useless
	 *	longStockPosition.setLastTick(150.00);
	*/
      longStockPosition.setNetAssetValue(10_000.00);
      portfolio.addNewPosition(longStockPosition);

      assertEquals(portfolio.calculateTotalCash(), 990_000.00);
      assertEquals(portfolio.getReservedCash(), 0.00);
      assertEquals(portfolio.getFreeCash(), 990_000.00);
      assertEquals(portfolio.countUncoveredLongStockPositions(testStock), 1);
      assertEquals(portfolio.numberOfOpenStockLongs(testStock), 1);
      assertEquals(portfolio.numberOfOpenCallShorts(testStock), 0);

      Position shortCallPosition = new Position();
      shortCallPosition.setTicker(testTicker);
      shortCallPosition.setSecType("CALL");
      shortCallPosition.setUnderlyingTicker(testTicker);
      shortCallPosition.setStrikePrice(120.00);
      shortCallPosition.setLongPosition(false);
      shortCallPosition.setNumberTransacted(2);
      shortCallPosition.setPriceAtOpen(0.75);
      shortCallPosition.calculateCostBasis();
      shortCallPosition.calculateClaimAgainstCash();

      portfolio.addNewPosition(shortCallPosition);
      assertEquals(portfolio.calculateTotalCash(), 990_150.00);
      assertEquals(portfolio.getReservedCash(), 0.00);
      assertEquals(portfolio.getFreeCash(), 990_150.00);
      assertTrue(portfolio.countUncoveredLongStockPositions(testStock) < 1);
      assertEquals(portfolio.numberOfOpenStockLongs(testStock), 1);
      assertEquals(portfolio.numberOfOpenCallShorts(testStock), 2);
      assertEquals(portfolio.findStockPositionToDeliver(testTicker), longStockPosition);
      assertEquals(portfolio.findStockPositionToDeliver(longStockPosition.getTicker()), longStockPosition);

	/* Adjust cash flow to cover one stock position and fill the other naked short call with cash */
      portfolio.exerciseOption(shortCallPosition);
      assertEquals(portfolio.findStockPositionToDeliver(testTicker), null);
	/* 
	 * lastTick() is used to calculate the cost of being short an uncovered call that expires ITM
	 * and deliver the contract at today's market price to the caller
	 * As such lastTick() on ticker "SHORTCALL" == 0 because the security does not exist
	 * Hence the more expansive cash calculation due to not being able to force the program to purchase
	 * the stock at the higher current market price and sell it to the caller for a loss.
	 */
      assertEquals(portfolio.calculateTotalCash(), 1_014_150.00);
      assertEquals(portfolio.getReservedCash(), 0.00);
      assertEquals(portfolio.getFreeCash(), 1_014_150.00);
      assertEquals(portfolio.countUncoveredLongStockPositions(testStock), 0);
      assertEquals(portfolio.numberOfOpenStockLongs(testStock), 0);
      assertEquals(portfolio.numberOfOpenCallShorts(testStock), 0);
   }
}