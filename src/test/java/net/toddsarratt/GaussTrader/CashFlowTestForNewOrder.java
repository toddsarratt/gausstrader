package net.toddsarratt.GaussTrader;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class CashFlowTestForNewOrder {
    Portfolio testPortfolio = new Portfolio();

    @Test public void testAddNewOrder() {
	Order sellPutOrder = new Order();
	sellPutOrder.setClaimAgainstCash(1_000.00);
	sellPutOrder.setTicker("TEST");
	sellPutOrder.setOrderId(System.currentTimeMillis());
	try {
	    testPortfolio.addNewOrder(sellPutOrder);
	} catch(Exception e) {
	    e.printStackTrace();
	}
	assertEquals(999_000.00, testPortfolio.getFreeCash());
	assertEquals(1_000.00, testPortfolio.getReservedCash());
	assertEquals(1_000_000.00, testPortfolio.calculateTotalCash());
    }
    @Test (enabled=false) public void testFillOrderWithNewPositionCashAdjust() {
	// TODO : Make this work
    }
    @Test (enabled=false) public void testExpireOrderCashAdjust() {
	// TODO : Make this work
    }
    @Test (enabled=false) public void testHelloWorld() {
	assertEquals(1_000_000.0, testPortfolio.getFreeCash());
    }
}