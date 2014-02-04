package net.toddsarratt.GaussTrader
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.testng.annotations.Test

import static org.testng.Assert.*
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
        /* Adjust the prices in the new Positions below against the current price of CSCO */
        Position expiringItmShortPut = new Position(ticker: "CSCO131116P00030000", expiry: new DateTime(1384578000000, DateTimeZone.forID("America/New_York")), underlyingTicker: "CSCO", strikePrice: 30)
        Position expiringOtmShortPut = new Position(ticker: "CSCO131116P00021000", expiry: new DateTime(1384578000000, DateTimeZone.forID("America/New_York")), underlyingTicker: "CSCO", strikePrice: 21)
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
    @Test
    public void testPortfolioExerciseExpiredOptions() {
        Stock cisco = new Stock("CSCO")
        Portfolio expiredOptionsPortfolio = new Portfolio("expiredOptionsPortfolio", 1_000_000.00)
        Position nonExpiredShortCall = new Position(
                ticker: "CSCO200118C00022000",
                expiry: new DateTime(2020, 1, 18, 16, 20, DateTimeZone.forID("America/New_York")),
                underlyingTicker: "CSCO",
                secType: "CALL",
                strikePrice: 22)
        Position expiredItmShortCall = new Position(
                ticker: "CSCO140118C00022000",
                expiry: new DateTime(1390021200000, DateTimeZone.forID("America/New_York")),
                underlyingTicker: "CSCO",
                secType: "CALL",
                strikePrice: 22)
        Position longCiscoStock = new Position(ticker: "CSCO",
                secType: "STOCK",
                underlyingTicker: "CSCO",
                numberTransacted: 100,
                longPosition: true,
                priceAtOpen: 22,
                costBasis: 2200
                )
        expiredOptionsPortfolio.portfolioPositions.add(expiredItmShortCall)
        expiredOptionsPortfolio.portfolioPositions.add(nonExpiredShortCall)
        expiredOptionsPortfolio.portfolioPositions.add(longCiscoStock)

        assertEquals(expiredOptionsPortfolio.getListOfOpenPositions().size(), 3)
        assertEquals(expiredOptionsPortfolio.getListOfOpenOptionPositions().size(), 2)
        assertEquals(expiredOptionsPortfolio.numberOfOpenCallShorts(cisco), 2)
        assertEquals(expiredOptionsPortfolio.numberOfOpenStockLongs(cisco), 1)
        assertEquals(expiredOptionsPortfolio.freeCash.doubleValue(), 1_000_000.00, 0.01)
        assertEquals(expiredOptionsPortfolio.reservedCash.doubleValue(), 0.00, 0.01)

        assertTrue(expiredItmShortCall.isExpired())
        assertFalse(nonExpiredShortCall.isExpired())
        expiredOptionsPortfolio.reconcileExpiredOptionPosition(expiredItmShortCall)

        assertEquals(expiredOptionsPortfolio.getListOfOpenPositions().size(), 1)
        assertEquals(expiredOptionsPortfolio.getListOfOpenOptionPositions().size(), 1)
        assertEquals(expiredOptionsPortfolio.numberOfOpenCallShorts(cisco), 1)
        assertEquals(expiredOptionsPortfolio.numberOfOpenStockLongs(cisco), 0)
        assertEquals(expiredOptionsPortfolio.freeCash.doubleValue(), 1_002_200.0, 0.01)
        assertEquals(expiredOptionsPortfolio.reservedCash.doubleValue(), 0.0, 0.01)
    }
    /* Tried to expire a stock position. Verify this now fails */
    @Test
    public void testPortfolioExpireOptionFailsAgainstStock() {
        Stock cisco = new Stock("CSCO")
        Portfolio oneStockPortfolio = new Portfolio("oneStockPortfolio", 1_000.00)
        Position longCiscoStock = new Position(ticker: "CSCO",
                secType: "STOCK",
                underlyingTicker: "CSCO",
                numberTransacted: 100,
                longPosition: true,
                priceAtOpen: 22,
                costBasis: 2200
        )
        oneStockPortfolio.portfolioPositions.add(longCiscoStock)
        assertEquals(oneStockPortfolio.getListOfOpenPositions().size(), 1)
        assertEquals(oneStockPortfolio.getListOfOpenOptionPositions().size(), 0)
        assertEquals(oneStockPortfolio.numberOfOpenCallShorts(cisco), 0)
        assertEquals(oneStockPortfolio.numberOfOpenStockLongs(cisco), 1)
        assertEquals(oneStockPortfolio.freeCash, 1_000.00, 0.01)
        assertEquals(oneStockPortfolio.reservedCash, 0.0, 0.01)

        oneStockPortfolio.expireOptionPosition(longCiscoStock)
        assertEquals(oneStockPortfolio.getListOfOpenPositions().size(), 1)
        assertEquals(oneStockPortfolio.getListOfOpenOptionPositions().size(), 0)
        assertEquals(oneStockPortfolio.numberOfOpenCallShorts(cisco), 0)
        assertEquals(oneStockPortfolio.numberOfOpenStockLongs(cisco), 1)
        assertEquals(oneStockPortfolio.freeCash, 1_000.00, 0.01)
        assertEquals(oneStockPortfolio.reservedCash, 0.0, 0.01)
    }
}