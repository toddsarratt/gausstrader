package net.toddsarratt.GaussTrader

import groovy.util.GroovyTestCase

class EndOfDayDbUpdateTest extends GroovyTestCase {
    /** Do not specify a name because name is a key value */
    def testPortfolio = new Portfolio();

    void testInsertDbSummary() {
        testPortfolio.insertDbSummary()

        /** Portfolio already exists, should get primary key error */
	shouldFail {testPortfolio.insertDbSummary()}    
    }
    void testUpdateDbSummary() {
        testPortfolio.freeCash = 9000.38.doubleValue()
	testPortfolio.reservedCash = 5000.38.doubleValue()
	assertEquals(testPortfolio.calculateTotalCash(), 14000.76.doubleValue(), 0.001)
	testPortfolio.updateDbSummary()
        testPortfolio.updateDbSummary()
        testPortfolio.updateDbSummary()
        testPortfolio.updateDbSummary()
    }
}