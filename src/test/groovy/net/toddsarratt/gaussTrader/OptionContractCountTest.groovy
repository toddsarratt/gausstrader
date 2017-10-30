package net.toddsarratt.gaussTrader

import org.testng.annotations.Test

/**
 * Created by tsarratt on 11/24/2014.
 */

@Deprecated
/* Better unit testing coming in v0.2 */
class OptionContractCountTest {
    @Test
    public void testOptionLastBid() {
        def testPortfolio = new Portfolio(name:"ContractCount", netAssetValue: 1_000_000)
        def testStock = new Stock(ticker: "VZ", price: 49.5, bollingerBand: [0.0,0.0,0.0,0.0,0.0,120.0])
        def testTradingSession = new TradingSession(testPortfolio, new ArrayList<Stock>([testStock]))
        /*
        Claim against cash : 100 contracts * strike = some % of NAV
        Where % = gaussTrader.STOCK_PCT_OF_PORTFOLIO (currently 10.0. To convert to pct multiply by 0.01)
        100 shares * x contracts * $49.50 stock price (strike) = 10% of NAV $1,000,000
        x = 0.1 * 1,000,000 / 100 * 49.5
        x = 20.2020...
        Each action will trade 1/4 that amount
        x' = x / 4 = 5.050505...
        int(x') = 5
        */
        def contracts = (int)(0.01 * GaussTrader.STOCK_PCT_OF_PORTFOLIO * 1_000_000 / (100 * 49.5 * 4))
        assert testTradingSession.findPutAction(testStock).contractsToTransact == contracts
    }
}
