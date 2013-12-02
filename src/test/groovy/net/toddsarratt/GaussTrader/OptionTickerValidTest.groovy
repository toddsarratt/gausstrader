package net.toddsarratt.GaussTrader
import org.testng.annotations.Test
/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 11/26/13
 */

import static org.testng.Assert.*

class OptionTickerValidTest {
    /**
     * Yahoo! changed the web response on 11/25/13 when an option is not found on their site
     */

    @Test
    public void testOptionTickerValid() {
        assertTrue Option.optionTickerValid('INTC160115C00025000')
        assertTrue Option.optionTickerValid('VZ140222P00045000')
        assertFalse Option.optionTickerValid('VZ120222P00049000')
        assertFalse Option.optionTickerValid('VZ140222P00049000')
        assertFalse Option.optionTickerValid('INVALID_option_name')
    }
}
