package net.toddsarratt.gaussTrader;

import net.toddsarratt.gaussTrader.market.Market;
import net.toddsarratt.gaussTrader.market.YahooMarket;
import net.toddsarratt.gaussTrader.persistence.store.DataStore;
import net.toddsarratt.gaussTrader.persistence.store.PostgresStore;
import net.toddsarratt.gaussTrader.portfolio.Portfolio;
import net.toddsarratt.gaussTrader.portfolio.PortfolioManager;
import net.toddsarratt.gaussTrader.singletons.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class {@code gaussTrader} is the entry class for the GaussTrader application.
 * <p>
 * GaussTrader is an an algorithm driven security trading simulator.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since gaussTrader v0.1
 */

public class GaussTrader {
	private static final Logger LOGGER = LoggerFactory.getLogger(GaussTrader.class);
	// TODO: Inject via static factory method but not in this method
	private static final EntityManagerFactory ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory("net.toddsarratt.gaussTrader.jpa");
	private static final EntityManager ENTITY_MANAGER = ENTITY_MANAGER_FACTORY.createEntityManager();
	private static final DataStore DATA_STORE = new PostgresStore();
	private static final Market MARKET = new YahooMarket();
	private static final Portfolio PORTFOLIO = Portfolio.of(Constants.getPortfolioName());
	// This guy runs the show. Start him, make him do his thing
	private static final PortfolioManager PORTFOLIO_MANAGER = new PortfolioManager(MARKET, PORTFOLIO, DATA_STORE);
	private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();


	/**
	 * @return DataStore associated with the application.
	 */
	public static DataStore getDataStore() {
		return DATA_STORE;
	}

	/**
	 * @return Market associated with the application.
	 */
	public static Market getMarket() {
		return MARKET;
	}

	private static void setUp() {
		ENTITY_MANAGER.getTransaction().begin();
	}

	public static void main(String[] args) {
		try {
			Instant programStartTime = Instant.now();
			LOGGER.info("*** START PROGRAM ***");
			LOGGER.info("Starting gaussTrader at {}", programStartTime);
			setUp();
			EXECUTOR_SERVICE.execute(MARKET);
			EXECUTOR_SERVICE.execute(PORTFOLIO_MANAGER);
		} catch (Exception pokemon) {
			LOGGER.error("Exception caught by main thread", pokemon);
		}
		LOGGER.info("*** END PROGRAM ***");
	}
}