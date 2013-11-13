package net.toddsarratt.GaussTrader

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.testng.annotations.Test
import static org.testng.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 11/13/13
 */
public class OptionExpiryReconcile {
    @Test
    public void testPortfolioExerciseLongPut() {
        Stock cisco = new Stock("CSCO")
        Portfolio expiringOptionsPort = new Portfolio("expiringOptionsPort", 1_000_000.00)
        Position expiringItmShortPut = new Position(ticker: "CSCO131116P00030000", expiry: new DateTime(1384578000000, DateTimeZone.forID("America/New_York")), underlyingTicker: "CSCO", strikePrice: 30)
        Position expiringOtmShortPut = new Position(ticker: "CSCO131116P00022000", expiry: new DateTime(1384578000000, DateTimeZone.forID("America/New_York")), underlyingTicker: "CSCO", strikePrice: 22)
        Position nonExpiringShortPut = new Position(ticker: "CSCO131221P00200000", expiry: new DateTime(1387602000000, DateTimeZone.forID("America/New_York")), underlyingTicker: "CSCO", strikePrice: 20)
        expiringOptionsPort.portfolioPositions.add(expiringItmShortPut)
        expiringOptionsPort.portfolioPositions.add(expiringOtmShortPut)
        expiringOptionsPort.portfolioPositions.add(nonExpiringShortPut)

        assertEquals(expiringOptionsPort.getListOfOpenPositions().size(), 3)
        assertEquals(expiringOptionsPort.getListOfOpenOptionPositions().size(), 3)
        assertEquals(expiringOptionsPort.numberOfOpenPutShorts(cisco), 3)
        assertEquals(expiringOptionsPort.numberOfOpenStockLongs(cisco), 0)

        TradingSession expiryFridaySession = new TradingSession(expiringOptionsPort, new ArrayList<Stock>())
        expiryFridaySession.todaysDateTime = new DateTime(1384534800000, DateTimeZone.forID("America/New_York"))
        expiryFridaySession.dayToday = expiryFridaySession.todaysDateTime.getDayOfWeek()
        expiryFridaySession.reconcileExpiringOptions()

        assertEquals(expiringOptionsPort.getListOfOpenPositions().size(), 2)
        assertEquals(expiringOptionsPort.getListOfOpenOptionPositions().size(), 1)
        assertEquals(expiringOptionsPort.numberOfOpenPutShorts(cisco), 1)
        assertEquals(expiringOptionsPort.numberOfOpenStockLongs(cisco), 1)
    }
}
