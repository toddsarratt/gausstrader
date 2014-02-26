package net.toddsarratt.GaussTrader
import org.testng.annotations.Test
/**
 * Created with IntelliJ IDEA.
 * User: tsarratt
 * Date: 2/26/14
 */
class PauseBetweenCyclesTest {
    @Test
    public void testNegativeSleepValue() {
        def testSession = new TradingSession(new Portfolio("pauseTest"), new ArrayList<Stock>())
        /* Set marketCloseEpoch to a time in the very recent past */
        testSession.marketCloseEpoch = System.currentTimeMillis() - 10_000
        /* Call method and verify it does not throw an Exception
            Specifically looking for :
            2014-02-25 16:23:20.069 EST [main] DEBUG TradingSession - Entering TradingSession.pauseBetweenCycles()
            2014-02-25 16:23:20.069 EST [main] DEBUG TradingSession - Comparing msUntilMarketClose -200069 with GaussTrader.delayMs 3600000
            2014-02-25 16:23:20.070 EST [main] DEBUG TradingSession - Sleeping for -200069 ms (-3 minutes, -20 seconds and -69 milliseconds)
            2014-02-25 16:23:20.071 EST [main] ERROR GaussTrader - ENCOUNTERED UNEXPECTED FATAL ERROR
            2014-02-25 16:23:20.072 EST [main] ERROR GaussTrader - Caught (Exception e)
            java.lang.IllegalArgumentException: timeout value is negative
         */
        testSession.pauseBetweenCycles()
    }
}
