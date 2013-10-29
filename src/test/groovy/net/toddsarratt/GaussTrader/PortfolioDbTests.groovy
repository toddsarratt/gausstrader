package net.toddsarratt.GaussTrader

import org.postgresql.util.PSQLException
import org.testng.annotations.*
import static org.testng.Assert.*

class PortfolioDbTests {
    /**
     * Do not specify a name because name is a key value. 
     * Value initiallized by the class to Long.toString(System.currentTimeMillis())
     */
    def testPortfolio = new Portfolio()

    @Test 
    public void testInsertDbSummary() {
        testPortfolio.insertDbSummary()
	assertTrue testPortfolio.portfolioInDb().booleanValue()
    }
    @Test(dependsOnMethods = ["testInsertDbSummary"], expectedExceptions = PSQLException.class)
    public void testDuplicateInsertDbSummary() {
        /** Portfolio already exists, should get primary key error */
        testPortfolio.insertDbSummary()
    }
    @Test
    public void testUpdateDbSummary() {
        testPortfolio.setFreeCash(9000.38.doubleValue())
	testPortfolio.setReservedCash(5000.38.doubleValue())
	assertEquals(testPortfolio.calculateTotalCash(), 14000.76.doubleValue(), 0.001.doubleValue())
	testPortfolio.updateDbSummary()

	def readDbPortfolio = new Portfolio(testPortfolio.getName())
	assertEquals(readDbPortfolio.getFreeCash(), testPortfolio.getFreeCash())
        assertEquals(readDbPortfolio.getReservedCash(), testPortfolio.getReservedCash())
        assertEquals(readDbPortfolio.calculateTotalCash(), testPortfolio.calculateTotalCash())
    }
}