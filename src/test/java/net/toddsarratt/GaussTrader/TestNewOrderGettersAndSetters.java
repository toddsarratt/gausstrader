package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Deprecated
/* Better unit testing coming in v0.2 */
public class TestNewOrderGettersAndSetters {
   Order testOrder = new Order();
   Order testOrderFilled;
   Order testOrderExpired;
   Order testOrderCancelled;

   @Test
   public void testOrderSetters() {
      testOrder.setOrderId(1123581321l);
      testOrder.setOpen(true);
      testOrder.setExpiry(new DateTime(DateTimeZone.forID("America/New_York")));
      testOrder.setUnderlyingTicker("TEST123");
      testOrder.setStrikePrice(54.69);
      testOrder.setLimitPrice(0.66);
      testOrder.setAction("BUY");
      testOrder.setSecType("CALL");
      testOrder.calculateClaimAgainstCash();
      testOrder.setTif("GTC");
      testOrder.setEpochOpened(System.currentTimeMillis());
   }

   @Test(dependsOnMethods = {"testOrderSetters"})
   public void testOrderGetters() {
      assertEquals(testOrder.getOrderId(), 1123581321l);
      assertTrue(testOrder.isOpen());
      assertEquals(testOrder.getExpiry().getMillis(), System.currentTimeMillis(), 500_000);
      assertEquals(testOrder.getUnderlyingTicker(), "TEST123");
      assertEquals(testOrder.getStrikePrice(), 54.69);
      assertEquals(testOrder.getLimitPrice(), 0.66);
      assertEquals(testOrder.getAction(), "BUY");
      assertTrue(testOrder.isLong());
      assertFalse(testOrder.isShort());
      assertTrue(testOrder.isOption());
      assertTrue(testOrder.isCall());
      assertFalse(testOrder.isPut());
      assertFalse(testOrder.isStock());
      assertEquals(testOrder.getClaimAgainstCash(), 66.00);
      assertEquals(testOrder.getTif(), "GTC");
      assertEquals(testOrder.getEpochOpened(), System.currentTimeMillis(), 500_000);
   }

   @Test(enabled = false)
   public void testOrderClosure() {
      // TODO : Close clones of original order in available ways and test
   }
}
