package net.toddsarratt.GaussTrader;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class CashFlowTestForNewOrder {
    Portfolio testPortfolio = new Portfolio();
    Order sellPutOrder = new Order();
    Stock testStock = new Stock();

    @Test public void testAddNewOrder() throws Exception {
	testStock.setTicker("TEST");
        sellPutOrder.setOrderId(System.currentTimeMillis());
	sellPutOrder.setOpen(true);
        sellPutOrder.setTicker("TEST");
	sellPutOrder.setUnderlyingTicker("TEST");
	sellPutOrder.setStrikePrice(100.00);
	sellPutOrder.setLimitPrice(1.00);
	sellPutOrder.setAction("SELL");
	sellPutOrder.setTotalQuantity(1);
	sellPutOrder.setSecType("PUT");
	sellPutOrder.calculateClaimAgainstCash();

	testPortfolio.addNewOrder(sellPutOrder);

    	assertEquals(testPortfolio.getFreeCash(), 990_100.00);
	assertEquals(testPortfolio.getReservedCash(), 9_900.00);
	assertEquals(testPortfolio.calculateTotalCash(), 1_000_000.00);

	assertEquals(testPortfolio.numberOfOpenPutShorts(testStock), 1);
    }
    @Test(dependsOnMethods = { "testAddNewOrder" }) public void testFillOrderWithNewPositionCashAdjust() {
	assertEquals(sellPutOrder.getClaimAgainstCash(), 9_900.00);

	testPortfolio.fillOrder(sellPutOrder, 1.05);

        assertEquals(System.currentTimeMillis(), sellPutOrder.getEpochClosed(), 5_000);
        assertEquals("FILLED", sellPutOrder.getCloseReason());
	assertEquals(testPortfolio.getFreeCash(), 990_105.00);
        assertEquals(testPortfolio.getReservedCash(), 10_000.00);
        assertEquals(testPortfolio.calculateTotalCash(), 1_000_105.00);
        assertEquals(testPortfolio.numberOfOpenPutShorts(testStock), 1);
    }
    @Test (enabled=false) public void testExpireOrderCashAdjust() {
	Portfolio testPortfolio = new Portfolio();
	// TODO : Make this work
    }
}