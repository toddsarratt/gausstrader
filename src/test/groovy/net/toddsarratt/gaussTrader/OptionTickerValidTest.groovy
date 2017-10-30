package net.toddsarratt.gaussTrader

import org.testng.annotations.Test

import static org.testng.Assert.*

/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 11/26/13
 */
@Deprecated
/* Better unit testing coming in v0.2 */
class OptionTickerValidTest {
    /**
     * Yahoo! changed the web response on 11/25/13 when an option is not found on their site
     */

    @Test
    public void testOptionTickerValid() {
        assertTrue Option.optionTickerValid('INTC160115C00025000')
        assertTrue Option.optionTickerValid('VZ160115C00050000')
        assertFalse Option.optionTickerValid('VZ120222P00049000')
        assertFalse Option.optionTickerValid('CAT140322P00087000')
        assertFalse Option.optionTickerValid('INVALID_option_name')
    }
}
