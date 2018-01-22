package net.toddsarratt.gaussTrader;

@Deprecated
/* Better unit testing coming in v0.2 */
public class OptionTickerGeneration {/*
   @Test
   public void testOptionTickerGeneration() {
      DateTime midDecemberDay = new DateTime(2014, 12, 20, 14, 0, 0, 0, DateTimeZone.forID("America/New_York"));
      assertEquals(Option.optionTicker("XOM", midDecemberDay, 'C', 90.00), "XOM141220C00090000");
   }

   @Test
   public void testExpiryCalc() {
      Option calculatedOption = Option.getOption("KO", "PUT", 42.01);
      assertEquals(calculatedOption.getExpiry(), new DateTime(2015, 4, 17, 0, 0, 0, 0, DateTimeZone.forID("America/New_York")));
   }

   @Test
   public void testOptionCreation() {
      Option testIbmCall = new Option("IBM130222C00200000");
      assertEquals(testIbmCall.getTicker(), "IBM130222C00200000", "Expected IBM130222C00200000 but got " + testIbmCall.getTicker());
      assertEquals(testIbmCall.getSecType(), "CALL");
      assertTrue(testIbmCall.getExpiry() != null);
      assertEquals(testIbmCall.getUnderlyingTicker(), "IBM");

      Option testXomPut = new Option("XOM130720P00070000");
      assertEquals(testXomPut.getTicker(), "XOM130720P00070000", "Expected XOM130720P00070000 but got " + testXomPut.getTicker());
      assertEquals(testXomPut.getSecType(), "PUT");
      assertTrue(testXomPut.getExpiry() != null);
      assertEquals(testXomPut.getUnderlyingTicker(), "XOM");
   }*/
}