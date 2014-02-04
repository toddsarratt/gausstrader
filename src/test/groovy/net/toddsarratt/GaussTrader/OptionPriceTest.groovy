package net.toddsarratt.GaussTrader

import org.testng.annotations.Test
import static org.testng.Assert.*

/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 2/4/14
 */

/* Long dated option MSFT160115C00030000 should be good through 2016 */
class OptionPriceTest {
    @Test
    public void testOptionLastBid() {
        def validOption = new Option("MSFT160115C00030000")
        /* Received NumberFormatException: For input string: "<span id="yfs_g00_ba140322p00120000">2.85" on 2/4/14
        when calling lastBid on BA option below
         */
        def optionPoopedOn = new Option("BA140322P00120000")
        assertTrue(validOption.lastBid() > 0.00)
        assertTrue(optionPoopedOn.lastBid() > 0.00)
    }
}
