package net.toddsarratt.GaussTrader

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.testng.annotations.Test
import static org.testng.Assert.*

/**
 * Created by tsarratt on 5/16/2014.
 */

@Deprecated
/* Better unit testing coming in v0.2 */
class PositionPandL {
    @Test
    public void testPandL() {
        Position profitableShortPutPosition = new Position(ticker: "UNH140517P00075000",expiry: new DateTime(2014, 5, 16, 0, 0,DateTimeZone.forID("America/New_York")),
                underlyingTicker: "UNH", strikePrice: 75.00, numberTransacted: 5, priceAtOpen: 1.29, costBasis: -645.00, claimAgainstCash: 37500,
                netAssetValue: -25)
        assertEquals(profitableShortPutPosition.getProfit(), 620, 1)
    }
}
