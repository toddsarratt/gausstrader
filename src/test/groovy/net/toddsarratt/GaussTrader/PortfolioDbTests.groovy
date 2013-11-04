package net.toddsarratt.GaussTrader

import org.joda.time.DateTime
import org.postgresql.util.PSQLException
import org.testng.annotations.Test

import static org.testng.Assert.*

class PortfolioDbTests {
    /**
     * Do not specify a name because name is a key value. 
     * Value initialized by the class to Long.toString(System.currentTimeMillis())
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

    @Test
    public void testDbReadWriteOrder() {
        def testOrderToInsert = new Order(ticker: "TESTINSERT", underlyingTicker: "TESTME", strikePrice: 500.0, limitPrice: 1.00, action: "BUY", totalQuantity: 10, secType: "CALL", tif: "GTC")
        testOrderToInsert.setExpiry(new DateTime(2013, 11, 16, 0, 0))
        testPortfolio.insertDbOrder(testOrderToInsert)
        testPortfolio.getDbPortfolioOrders()
        def testOrderFromDb = testPortfolio.portfolioOrders.find {
	        order -> order.getOrderId() == testOrderToInsert.getOrderId()
        }
        assertNotSame testOrderFromDb, testOrderToInsert
        List<MetaProperty> groovyProperties = Order.metaClass.properties
        groovyProperties.each {
            println "Comparing field " + it.name
            assertEquals it.getProperty(testOrderFromDb), it.getProperty(testOrderToInsert)
            println "${it.getProperty(testOrderFromDb)} = ${it.getProperty(testOrderToInsert)}"
        }
    }

    @Test
    public void testDbReadWritePosition() {
        def testPositionToInsert = new Position(ticker: "TESTINSERT", underlyingTicker: "TESTME", strikePrice: 500.0, longPosition: false, numberTransacted: 10, secType: "CALL", priceAtOpen: 1.50)
        testPortfolio.insertDbPosition(testPositionToInsert)
        testPortfolio.getDbPortfolioPositions()
        def testPositionFromDb = testPortfolio.portfolioPositions.find {
            position -> position.getPositionId() == testPositionToInsert.getPositionId()
        }
        assertNotSame testPositionFromDb, testPositionToInsert
        List<MetaProperty> groovyProperties = Position.metaClass.properties
        groovyProperties.each {
            println "Comparing field " + it.name
            assertEquals it.getProperty(testPositionFromDb), it.getProperty(testPositionToInsert)
            println "${it.getProperty(testPositionFromDb)} = ${it.getProperty(testPositionToInsert)}"
        }
    }
}