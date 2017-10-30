package net.toddsarratt.gaussTrader
import org.testng.annotations.Test

import static org.testng.Assert.assertTrue
/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 11/4/13
 */

@Deprecated
/* Better unit testing coming in v0.2 */
class FillShortPutOrderTest {

    @Test
    public void testFillShortPutOrder() {
        def shortPutOrder = new Order() /** No fields need to be set, defaults work fine for this test */
        assertTrue shortPutOrder.isShort()
        def shortPutPosition = new Position(shortPutOrder, 5.00)
        assertTrue shortPutPosition.isShort()
    }
}