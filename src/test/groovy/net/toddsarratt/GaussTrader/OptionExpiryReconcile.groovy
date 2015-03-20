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
        Position expiringItmShortPut = new Position(ticker: "CSCO150320P00030000", expiry: new DateTime(1426885200000, DateTimeZone.forID("America/New_York")), underlyingTicker: "CSCO", strikePrice: 30)
        Position expiringOtmShortPut = new Position(ticker: "CSCO150320P00021000", expiry: new DateTime(1426885200000, DateTimeZone.forID("America/New_York")), underlyingTicker: "CSCO", strikePrice: 21)
        Position nonExpiringShortPut = new Position(ticker: "CSCO160115P00020000", expiry: new DateTime(1452895200000, DateTimeZone.forID("America/New_York")), underlyingTicker: "CSCO", strikePrice: 20)
        expiringOptionsPort.portfolioPositions.add(expiringItmShortPut)
        expiringOptionsPort.portfolioPositions.add(expiringOtmShortPut)
        expiringOptionsPort.portfolioPositions.add(nonExpiringShortPut)

        assertEquals(expiringOptionsPort.getListOfOpenPositions().size(), 3)
        assertEquals(expiringOptionsPort.getListOfOpenOptionPositions().size(), 3)
        assertEquals(expiringOptionsPort.numberOfOpenPutShorts(cisco), 3)
        assertEquals(expiringOptionsPort.numberOfOpenStockLongs(cisco), 0)

        TradingSession expiryFridaySession = new TradingSession(expiringOptionsPort, new ArrayList<Stock>())
        expiryFridaySession.todaysDateTime = new DateTime(1426867200, DateTimeZone.forID("America/New_York"))
        expiryFridaySession.dayToday = expiryFridaySession.todaysDateTime.getDayOfWeek()
        expiryFridaySession.reconcileExpiringOptions()

        assertEquals(expiringOptionsPort.getListOfOpenPositions().size(), 2)
        assertEquals(expiringOptionsPort.getListOfOpenOptionPositions().size(), 1)
        assertEquals(expiringOptionsPort.numberOfOpenPutShorts(cisco), 1)
        assertEquals(expiringOptionsPort.numberOfOpenStockLongs(cisco), 1)
    }
    @Test
    public void testPortfolioShortCalls() {
        Stock cisco = new Stock("CSCO")
        Portfolio expiredOptionsPortfolio = new Portfolio("expiredOptionsPortfolio", 1_000_000.00)
        Position nonExpiredShortCall = new Position(
                ticker: "CSCO200118C00022000",
                expiry: new DateTime(2020, 1, 18, 17, 00, DateTimeZone.forID("America/New_York")),
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
    @Test
    public void testCoveredExpiredShortCall() {
        Stock mmmStock = new Stock("MMM")
        Portfolio expiredCoveredShortCallPortfolio = new Portfolio("expiredShortCallPortfolio", 1_000_000.00)
        Position expiredCoveredShortCall = new Position(
                ticker: "MMM140118C00120000",
                expiry: new DateTime(1390021200000, DateTimeZone.forID("America/New_York")),
                underlyingTicker: "MMM",
                secType: "CALL",
                numberTransacted: 5,
                strikePrice: 120)
        Position longMmmStock = new Position(ticker: "MMM",
                secType: "STOCK",
                underlyingTicker: "MMM",
                numberTransacted: 500,
                longPosition: true,
                priceAtOpen: 100,
                costBasis: 50_000)
        expiredCoveredShortCallPortfolio.portfolioPositions.add(expiredCoveredShortCall)
        expiredCoveredShortCallPortfolio.portfolioPositions.add(longMmmStock)

        assertEquals(expiredCoveredShortCallPortfolio.getListOfOpenPositions().size(), 2)
        assertEquals(expiredCoveredShortCallPortfolio.getListOfOpenOptionPositions().size(), 1)
        assertEquals(expiredCoveredShortCallPortfolio.numberOfOpenCallShorts(mmmStock), 5)
        assertEquals(expiredCoveredShortCallPortfolio.numberOfOpenStockLongs(mmmStock), 5)
        assertEquals(expiredCoveredShortCallPortfolio.freeCash.doubleValue(), 1_000_000.00, 0.01)
        assertEquals(expiredCoveredShortCallPortfolio.reservedCash.doubleValue(), 0.00, 0.01)

        assertTrue(expiredCoveredShortCall.isExpired())
        expiredCoveredShortCallPortfolio.reconcileExpiredOptionPosition(expiredCoveredShortCall)

        assertEquals(expiredCoveredShortCallPortfolio.getListOfOpenPositions().size(), 0)
        assertEquals(expiredCoveredShortCallPortfolio.getListOfOpenOptionPositions().size(), 0)
        assertEquals(expiredCoveredShortCallPortfolio.numberOfOpenCallShorts(mmmStock), 0)
        assertEquals(expiredCoveredShortCallPortfolio.numberOfOpenStockLongs(mmmStock), 0)
        /* Short call, someone is buying our 500 shares of MMM at $120. Should be up $60,000 */
        assertEquals(expiredCoveredShortCallPortfolio.freeCash.doubleValue(), 1_060_000.0, 0.01)
        assertEquals(expiredCoveredShortCallPortfolio.reservedCash.doubleValue(), 0.0, 0.01)
    }

    public void testOverCoveredExpiredShortCall() {
        Stock attStock = new Stock("T")
        Portfolio expiredOverCoveredShortCallPortfolio = new Portfolio("expiredShortCallPortfolio", 1_000_000.00)
        Position expiredOverCoveredShortCall = new Position(
                ticker: "T140118C00020000",
                expiry: new DateTime(1390021200000, DateTimeZone.forID("America/New_York")),
                underlyingTicker: "T",
                secType: "CALL",
                numberTransacted: 4,
                strikePrice: 30)
        Position longAttStock = new Position(ticker: "T",
                secType: "STOCK",
                underlyingTicker: "T",
                numberTransacted: 500,
                longPosition: true,
                priceAtOpen: 25,
                costBasis: 12_500)
        expiredOverCoveredShortCallPortfolio.portfolioPositions.add(expiredOverCoveredShortCall)
        expiredOverCoveredShortCallPortfolio.portfolioPositions.add(longAttStock)

        assertEquals(expiredOverCoveredShortCallPortfolio.getListOfOpenPositions().size(), 2)
        assertEquals(expiredOverCoveredShortCallPortfolio.getListOfOpenOptionPositions().size(), 1)
        assertEquals(expiredOverCoveredShortCallPortfolio.numberOfOpenCallShorts(attStock), 4)
        assertEquals(expiredOverCoveredShortCallPortfolio.numberOfOpenStockLongs(attStock), 5)
        assertEquals(expiredOverCoveredShortCallPortfolio.freeCash.doubleValue(), 1_000_000.00, 0.01)
        assertEquals(expiredOverCoveredShortCallPortfolio.reservedCash.doubleValue(), 0.00, 0.01)

        assertTrue(expiredOverCoveredShortCall.isExpired())
        expiredOverCoveredShortCallPortfolio.reconcileExpiredOptionPosition(expiredItmShortCall)

        assertEquals(expiredOverCoveredShortCallPortfolio.getListOfOpenPositions().size(), 1)
        assertEquals(expiredOverCoveredShortCallPortfolio.getListOfOpenOptionPositions().size(), 0)
        assertEquals(expiredOverCoveredShortCallPortfolio.numberOfOpenCallShorts(attStock), 0)
        assertEquals(expiredOverCoveredShortCallPortfolio.numberOfOpenStockLongs(attStock), 1)
        /* 500 shares claimed by 4 contracts, 100 shares left */
        assertEquals(longAttStock.getNumberTransacted(), 100)
        /* Short call, someone is buying our 400 shares of T at $30. Should be up $12,000 */
        assertEquals(expiredOverCoveredShortCallPortfolio.freeCash.doubleValue(), 1_012_000.0, 0.01)
        assertEquals(expiredOverCoveredShortCallPortfolio.reservedCash.doubleValue(), 0.0, 0.01)
    }
    @Test
    public void testUnderCoveredExpiredShortCall() {
        Stock catStock = new Stock("CAT")
        Portfolio expiredUnderCoveredShortCallPortfolio = new Portfolio("expiredShortCallPortfolio", 1_000_000.00)
        Position expiredUnderCoveredShortCall = new Position(
                ticker: "CAT140118C00020000",
                expiry: new DateTime(1390021200000, DateTimeZone.forID("America/New_York")),
                underlyingTicker: "CAT",
                secType: "CALL",
                numberTransacted: 6,
                strikePrice: 80)
        Position longCatStock = new Position(ticker: "CAT",
                secType: "STOCK",
                underlyingTicker: "CAT",
                numberTransacted: 500,
                longPosition: true,
                priceAtOpen: 25,
                costBasis: 2500)
        expiredUnderCoveredShortCallPortfolio.portfolioPositions.add(expiredUnderCoveredShortCall)
        expiredUnderCoveredShortCallPortfolio.portfolioPositions.add(longCatStock)

        assertEquals(expiredUnderCoveredShortCallPortfolio.getListOfOpenPositions().size(), 2)
        assertEquals(expiredUnderCoveredShortCallPortfolio.getListOfOpenOptionPositions().size(), 1)
        assertEquals(expiredUnderCoveredShortCallPortfolio.numberOfOpenCallShorts(catStock), 6)
        assertEquals(expiredUnderCoveredShortCallPortfolio.numberOfOpenStockLongs(catStock), 5)
        assertEquals(expiredUnderCoveredShortCallPortfolio.freeCash.doubleValue(), 1_000_000.00, 0.01)
        assertEquals(expiredUnderCoveredShortCallPortfolio.reservedCash.doubleValue(), 0.00, 0.01)

        assertTrue(expiredUnderCoveredShortCall.isExpired())
        expiredUnderCoveredShortCallPortfolio.reconcileExpiredOptionPosition(expiredUnderCoveredShortCall)

        assertEquals(expiredUnderCoveredShortCallPortfolio.getListOfOpenPositions().size(), 0)
        assertEquals(expiredUnderCoveredShortCallPortfolio.getListOfOpenOptionPositions().size(), 0)
        assertEquals(expiredUnderCoveredShortCallPortfolio.numberOfOpenCallShorts(catStock), 0)
        assertEquals(expiredUnderCoveredShortCallPortfolio.numberOfOpenStockLongs(catStock), 0)
        /* All 500 shares called away @ $80. Up $40,000
        Had to buy 100 shares in the market to deliver @ $80. CAT = $80.09 on 3/19/15
        100 * ($80 - $80.09) = -$9
        $40,000 - $9 = $39,991
         */
        assertEquals(expiredUnderCoveredShortCallPortfolio.freeCash.doubleValue(), 1_039_991.0, 500)
        assertEquals(expiredUnderCoveredShortCallPortfolio.reservedCash.doubleValue(), 0.0, 0.01)
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