package net.toddsarratt.GaussTrader;

import java.io.IOException;
import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

public class TestYahoo {
    @Test public void testStockAskYahoo() {
	try {
	    String[] testResponse = Stock.askYahoo("XOM", "sl1");
	    assertTrue(testResponse[0].equals("XOM"));
	    assertTrue(Double.parseDouble(testResponse[1]) > 0.00);
	} catch(IOException ioe) {
	    System.out.println("Cannot connect to yahoo!");
	    ioe.printStackTrace();
	}
    }
}
