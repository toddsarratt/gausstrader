package net.toddsarratt.gaussTrader

import org.testng.annotations.Test

import static org.testng.Assert.*

/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 2/4/14
 */

/* Long dated option MSFT160115C00030000 should be good through 2016 */

@Deprecated
/* Better unit testing coming in v0.2 */
class OptionPriceTest {
    @Test
    public void testOptionLastBid() {
        def validOption = new Option("MSFT160115C00030000")
        assertTrue(validOption.lastBid() > 0.00)
    }
}
