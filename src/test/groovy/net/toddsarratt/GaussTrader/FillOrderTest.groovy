package net.toddsarratt.GaussTrader

import org.testng.annotations.*
import static org.testng.Assert.*

class FillShortPutOrderTest {

    @Test
    public void testFillShortPutOrder() {
        def shortPutOrder = new Order() /** No fields need to be set, defaults work fine for this test */
        assertTrue shortPutOrder.isShort()
	def shortPutPosition = new Position(shortPutOrder, 5.00)
        assertTrue shortPutPosition.isShort()
    }
}

