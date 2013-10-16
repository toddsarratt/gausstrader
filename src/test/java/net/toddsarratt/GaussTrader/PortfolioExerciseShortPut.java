package net.toddsarratt.GaussTrader;

// import java.io.IOException;
// import java.net.MalformedURLException;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class PortfolioExerciseShortPut {
    @Test public void testPortfolioExerciseShortPut() {
	Stock testStock = new Stock();
        String testTicker = "SHORTPUT";

	testStock.setTicker(testTicker);

	Portfolio portfolio = new Portfolio();
	portfolio.setName("exerciseShortPut");
	Position shortPutPosition = new Position();
	shortPutPosition.setTicker(testTicker);
	shortPutPosition.setSecType("PUT");
	shortPutPosition.setUnderlyingTicker(testTicker);
	shortPutPosition.setStrikePrice(100.00);
	shortPutPosition.setLongPosition(false);
	shortPutPosition.setNumberTransacted(1);
	shortPutPosition.setPriceAtOpen(0.75);
	shortPutPosition.setCostBasis(-75.00);
	shortPutPosition.setClaimAgainstCash(shortPutPosition.calculateClaimAgainstCash());

	portfolio.addNewPosition(shortPutPosition);
	assertEquals(portfolio.calculateTotalCash(), 1_000_000.00);
	assertEquals(portfolio.getReservedCash(), 10_000.00);
	assertEquals(portfolio.getFreeCash(), 990_000.00);
        assertEquals(portfolio.countUncoveredLongStockPositions(testStock), 0);
        assertEquals(portfolio.numberOfOpenStockLongs(testStock), 0);
	assertEquals(portfolio.numberOfOpenPutShorts(testStock), 1);

	portfolio.exerciseOption(shortPutPosition);

	assertEquals(portfolio.calculateTotalCash(), 990_000.00);
        assertEquals(portfolio.getReservedCash(), 0.00);
        assertEquals(portfolio.getFreeCash(), 990_000.00);
	assertEquals(portfolio.countUncoveredLongStockPositions(testStock), 1);
	assertEquals(portfolio.numberOfOpenStockLongs(testStock), 1);
	assertEquals(portfolio.numberOfOpenPutShorts(testStock), 0);
    }
}