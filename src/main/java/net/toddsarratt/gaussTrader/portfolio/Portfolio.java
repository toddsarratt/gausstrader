package net.toddsarratt.gaussTrader.portfolio;

import net.toddsarratt.gaussTrader.GaussTrader;
import net.toddsarratt.gaussTrader.InstantPrice;
import net.toddsarratt.gaussTrader.InsufficientFundsException;
import net.toddsarratt.gaussTrader.Position;
import net.toddsarratt.gaussTrader.domain.Security;
import net.toddsarratt.gaussTrader.domain.Stock;
import net.toddsarratt.gaussTrader.orders.Order;
import net.toddsarratt.gaussTrader.persistence.store.DataStore;
import net.toddsarratt.gaussTrader.singletons.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static net.toddsarratt.gaussTrader.singletons.BuyOrSell.BUY;
import static net.toddsarratt.gaussTrader.singletons.BuyOrSell.SELL;
import static net.toddsarratt.gaussTrader.singletons.Constants.BIGDECIMAL_ONE_HUNDRED;

public class Portfolio {
	private static final Logger LOGGER = LoggerFactory.getLogger(Portfolio.class);
	private static final PortfolioSummary INITIAL_SUMMARY = new PortfolioSummary(
			"INITIAL",
			Constants.STARTING_CASH,
			Constants.STARTING_CASH,
			BigDecimal.ZERO,
			Constants.STARTING_CASH
	);
	private static final Set<Position> NO_POSITIONS = Collections.EMPTY_SET;
	private static final Set<Order> NO_ORDERS = Collections.EMPTY_SET;
	private static DataStore dataStore = GaussTrader.getDataStore();
	private String name;
	private BigDecimal netAssetValue;
	private BigDecimal freeCash;
	private BigDecimal reservedCash;
	private BigDecimal totalCash;
	private Set<Position> positions;
	private Set<Order> orders;

	/**
	 * Use static factory method .of() to create objects of the Portfolio class.
	 */
	private Portfolio(String portfolioName,
	                  BigDecimal netAssetValue,
	                  BigDecimal freeCash,
	                  BigDecimal reservedCash,
	                  BigDecimal totalCash,
	                  Set<Position> positions,
	                  Set<Order> orders) {
		LOGGER.debug("Entering private constructor");
		this.name = portfolioName;
		this.netAssetValue = netAssetValue;
		this.freeCash = freeCash;
		this.reservedCash = reservedCash;
		this.positions = positions;
		this.orders = orders;
	}

	public static Portfolio of(String portfolioName) {
		Portfolio newPortfolio;
		LOGGER.debug("Entering Portfolio constructor Portfolio(String {})", portfolioName);
		if (dataStore.portfolioInStore(portfolioName)) {
			LOGGER.info("Starting with portfolio named \"{}\"", portfolioName);
			newPortfolio = getPortfolioFromStore(portfolioName);
		} else {
			LOGGER.info("Could not find portfolio \"{}\"", portfolioName);
			newPortfolio = new Portfolio(portfolioName,
					INITIAL_SUMMARY.getNetAssetValue(),
					INITIAL_SUMMARY.getFreeCash(),
					INITIAL_SUMMARY.getReservedCash(),
					INITIAL_SUMMARY.getTotalCash(),
					NO_POSITIONS,
					NO_ORDERS
			);
			LOGGER.info("Created portfolio \"{}\" with ${} free cash", portfolioName, newPortfolio.getFreeCash());
		}
		newPortfolio.calculateNetAssetValue();
		LOGGER.debug("Starting portfolio \"{}\" with netAssetValue {} reservedCash {} totalCash {}",
				portfolioName, newPortfolio.getNetAssetValue(), newPortfolio.getReservedCash(), newPortfolio.getTotalCash());
		return newPortfolio;
	}

	public static Portfolio of(String portfolioName, BigDecimal startingCash) {
		LOGGER.debug("Entering of(String {}, BigDecimal {})", portfolioName, startingCash);
		if (dataStore.portfolioInStore(portfolioName)) {
			LOGGER.error("Portfolio {} already exists", portfolioName);
			throw new IllegalArgumentException("Portfolio already exists.");
		}
		Portfolio newPortfolio = getPortfolioFromStore(portfolioName);
		newPortfolio.calculateNetAssetValue();
		LOGGER.debug("Starting portfolio \"{}\" with netAssetValue {} reservedCash {} totalCash {}",
				portfolioName, newPortfolio.getNetAssetValue(), newPortfolio.getReservedCash(), newPortfolio.getTotalCash());
		return newPortfolio;
	}

	static Portfolio getPortfolioFromStore(String portfolioName) {
		LOGGER.debug("Entering Portfolio.getDbPortfolio()");
		PortfolioSummary summary = dataStore.getPortfolioSummary(portfolioName);
		Set<Position> positions = dataStore.getPortfolioPositions();
		Set<Order> orders = dataStore.getPortfolioOrders();
		return new Portfolio(portfolioName,
				summary.getNetAssetValue(),
				summary.getFreeCash(),
				summary.getReservedCash(),
				summary.getTotalCash(),
				positions,
				orders);
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	public BigDecimal getNetAssetValue() {
		return netAssetValue;
	}

	@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
	public BigDecimal calculateNetAssetValue() {
		calculateTotalCash();
		BigDecimal openPositionNavs = positions.stream()
				.filter(Position::isOpen)
				.map(Position::calculateNetAssetValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		netAssetValue = totalCash.add(openPositionNavs);
		return netAssetValue;
	}

	public BigDecimal getFreeCash() {
		return freeCash;
	}

	private void setFreeCash(BigDecimal freeCash) {
		this.freeCash = freeCash;
	}

	public BigDecimal getReservedCash() {
		return reservedCash;
	}

	private void setReservedCash(BigDecimal reservedCash) {
		this.reservedCash = reservedCash;
	}

	public BigDecimal calculateTotalCash() {
		totalCash = freeCash.add(reservedCash);
		return totalCash;
	}

	// TODO : This should not cause state change
	public BigDecimal getTotalCash() {
		return calculateTotalCash();
	}

	int countUncoveredLongStockPositions(Stock stock) {
		return (numberOfOpenStockLongs(stock) - numberOfOpenCallShorts(stock));
	}

	public int numberOfOpenStockLongs(Security security) {
		int openLongCount = 0;
		// TODO : Lamda
		for (Position portfolioPosition : positions) {
			if ((security.getTicker().equals(portfolioPosition.getTicker())) &&
					portfolioPosition.isOpen() &&
					portfolioPosition.isLong() &&
					(portfolioPosition.isStock())) {
				openLongCount += portfolioPosition.getNumberTransacted();
			}
		}
		openLongCount /= 100;
		LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenStockLongs(Security {})", openLongCount, security.getTicker());
		return openLongCount;
	}

	public int numberOfOpenStockShorts(Security security) {
		int openShortCount = 0;
		// TODO : Lamda
		for (Position portfolioPosition : positions) {
			if ((security.getTicker().equals(portfolioPosition.getTicker())) && portfolioPosition.isOpen() &&
					portfolioPosition.isShort() && portfolioPosition.isStock()) {
				openShortCount += portfolioPosition.getNumberTransacted();
			}
		}
		openShortCount /= 100;
		LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenStockShorts(Security {})", openShortCount, security.getTicker());
		return openShortCount;
	}

	public int numberOfOpenCallLongs(Security security) {
		int openLongCount = 0;
		// TODO : Lamda
		for (Position portfolioPosition : positions) {
			if ((security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) && portfolioPosition.isOpen() &&
					portfolioPosition.isLong() && portfolioPosition.isCall()) {
				openLongCount++;
			}
		}
		LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenCallLongs(Security {})", openLongCount, security.getTicker());
		return openLongCount;
	}

	public int numberOfOpenCallShorts(Security security) {
		int openShortCount = 0;
		// TODO : Lamda
		for (Position portfolioPosition : positions) {
			if ((security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) &&
					portfolioPosition.isOpen() &&
					portfolioPosition.isShort() &&
					portfolioPosition.isCall()) {
				openShortCount += portfolioPosition.getNumberTransacted();
			}
		}
		// TODO : Lamda
		for (Order portfolioOrder : orders) {
			if ((security.getTicker().equals(portfolioOrder.getUnderlyingTicker())) &&
					portfolioOrder.isOpen() &&
					portfolioOrder.getBuyOrSell() == SELL &&
					portfolioOrder.isCall()) {
				openShortCount += portfolioOrder.getTotalQuantity();
			}
		}
		LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenCallShorts(Security {})", openShortCount, security.getTicker());
		return openShortCount;
	}

	public int numberOfOpenPutLongs(Security security) {
		int openLongCount = 0;
		// TODO : Lamda
		for (Position portfolioPosition : positions) {
			if ((security.getTicker().equals(portfolioPosition.getUnderlyingTicker())) &&
					portfolioPosition.isOpen() &&
					portfolioPosition.isLong() &&
					portfolioPosition.isPut()) {
				openLongCount += portfolioPosition.getNumberTransacted();
			}
		}
		// TODO : Lamda
		for (Order portfolioOrder : orders) {
			if ((security.getTicker().equals(portfolioOrder.getUnderlyingTicker())) &&
					portfolioOrder.isOpen() &&
					portfolioOrder.getBuyOrSell() == BUY &&
					portfolioOrder.isPut()) {
				openLongCount += portfolioOrder.getTotalQuantity();
			}
		}
		LOGGER.debug("Returning openLongCount = {} from Portfolio.numberOfOpenPutLongs(Security {})", openLongCount, security.getTicker());
		return openLongCount;
	}

	public int numberOfOpenPutShorts(Security security) {
		String securityTicker = security.getTicker();
		int openShortCount = 0;
		// TODO : Lamda
		for (Position portfolioPosition : positions) {
			if ((securityTicker.equals(portfolioPosition.getUnderlyingTicker())) &&
					portfolioPosition.isOpen() &&
					portfolioPosition.isShort() &&
					portfolioPosition.isPut()) {
				openShortCount += portfolioPosition.getNumberTransacted();
			}
		}
		// TODO : Lamda
		for (Order portfolioOrder : orders) {
			if ((securityTicker.equals(portfolioOrder.getUnderlyingTicker())) &&
					portfolioOrder.isOpen() &&
					portfolioOrder.getBuyOrSell() == SELL &&
					portfolioOrder.isPut()) {
				openShortCount += portfolioOrder.getTotalQuantity();
			}
		}
		LOGGER.debug("Returning openShortCount = {} from Portfolio.numberOfOpenPutShorts(Security {})", openShortCount, security.getTicker());
		return openShortCount;
	}

	public void addNewOrder(Order orderToAdd) throws InsufficientFundsException {
		LOGGER.debug("Entering Portfolio.addNewOrder(Order {})", orderToAdd);
		BigDecimal orderRequiredCash = orderToAdd.getClaimAgainstCash();
		LOGGER.debug("orderRequiredCash = orderToAdd.getClaimAgainstCash() = ${}", orderRequiredCash);
		// The suggested idiom for performing these comparisons is: (x.compareTo(y) <op> 0), where <op> is one of the six comparison operators.
		if (freeCash.compareTo(orderRequiredCash) < 0) {
			LOGGER.debug("freeCash {} < orderRequiredCash {}", freeCash, orderRequiredCash);
			throw new InsufficientFundsException(orderToAdd.getSecurity().getTicker(), orderRequiredCash.doubleValue(), freeCash.doubleValue());
		}
		LOGGER.debug("reservedCash ${} += orderRequiredCash ${} == ${}", reservedCash, orderRequiredCash, reservedCash.add(orderRequiredCash));
		reservedCash = reservedCash.add(orderRequiredCash);
		LOGGER.debug("freeCash ${} -= orderRequiredCash ${} == ${}", freeCash, orderRequiredCash, freeCash.subtract(orderRequiredCash));
		freeCash = freeCash.subtract(orderRequiredCash);
		LOGGER.info("orderRequiredCash == ${}, freeCash == ${}, reservedCash == ${}", orderRequiredCash, freeCash, reservedCash);
		orders.add(orderToAdd);
		LOGGER.info("Added order id {} to portfolio {}", orderToAdd.getOrderId(), name);
		dataStore.write(orderToAdd);
	}

	public void addNewPosition(Position position) {
		LOGGER.debug("Entering Portfolio.addNewPosition(Position {})", position.getPositionId());
		positions.add(position);
		LOGGER.debug("freeCash ${} -= position.getCostBasis() ${} == ${}", freeCash, position.getCostBasis(), freeCash.subtract(position.getCostBasis()));
		freeCash = freeCash.subtract(position.getCostBasis());
		LOGGER.debug("freeCash ${} -= position.getClaimAgainstCash() ${} == ${}", freeCash, position.getClaimAgainstCash(), freeCash.subtract(position.getClaimAgainstCash()));
		freeCash = freeCash.subtract(position.getClaimAgainstCash());
		LOGGER.debug("reservedCash ${} += position.getClaimAgainstCash() ${} == ${}", reservedCash, position.getClaimAgainstCash(), reservedCash.add(position.getClaimAgainstCash()));
		reservedCash = reservedCash.add(position.getClaimAgainstCash());
		dataStore.write(position);
		/** TODO : Move try catch to called method which should write to a file if dbwrite fails */
	}

	public List<Order> getListOfOpenOrders() {
		LOGGER.debug("Entering Portfolio.getListOfOpenOrders()");
		List<Order> openOrderList = new ArrayList<>();
		// TODO : Lambda
		for (Order portfolioOrder : orders) {
			if (portfolioOrder.isOpen()) {
				openOrderList.add(portfolioOrder);
			}
		}
		LOGGER.debug("Returning {}", Arrays.toString(openOrderList.toArray()));
		return openOrderList;
	}

	@SuppressWarnings("WeakerAccess")
	public List<Position> getListOfOpenPositions() {
		LOGGER.debug("Entering Portfolio.getListOfOpenPositions()");
		List<Position> openPositionList = new ArrayList<>();
		// TODO : Lambda
		for (Position portfolioPosition : positions) {
			if (portfolioPosition.isOpen()) {
				openPositionList.add(portfolioPosition);
			}
		}
		LOGGER.debug("Returning {}", Arrays.toString(openPositionList.toArray()));
		return openPositionList;
	}

	public List<Position> getListOfOpenOptionPositions() {
		LOGGER.debug("Entering Portfolio.getListOfOpenOptionPositions()");
		List<Position> openOptionPositionList = new ArrayList<>();
		// TODO : Lambda
		for (Position portfolioPosition : positions) {
			if (portfolioPosition.isOpen() && portfolioPosition.isOption()) {
				openOptionPositionList.add(portfolioPosition);
			}
		}
		LOGGER.debug("Returning {}", openOptionPositionList.toString());
		return openOptionPositionList;
	}

	public List<Position> getListOfOpenStockPositions() {
		LOGGER.debug("Entering Portfolio.getListOfOpenStockPositions()");
		List<Position> openStockPositionList = new ArrayList<>();
		// TODO : Lambda
		for (Position portfolioPosition : positions) {
			if (portfolioPosition.isOpen() && portfolioPosition.isStock()) {
				openStockPositionList.add(portfolioPosition);
			}
		}
		LOGGER.debug("Returning {}", openStockPositionList.toString());
		return openStockPositionList;
	}

	public void fillOrder(Order orderToFill, BigDecimal fillPrice) {
		LOGGER.debug("Entering Portfolio.fillOrder(Order {}, BigDecimal {})", orderToFill.getOrderId(), fillPrice);
		Position positionTakenByOrder = new Position(orderToFill, fillPrice);
		positions.add(positionTakenByOrder);
		/* Unreserve cash to fill order */
		LOGGER.debug("freeCash ${} += orderToFill.getClaimAgainstCash() ${} == ${}", freeCash, orderToFill.getClaimAgainstCash(), freeCash.add(orderToFill.getClaimAgainstCash()));
		freeCash = freeCash.add(orderToFill.getClaimAgainstCash());
		LOGGER.debug("reservedCash ${} -= orderToFill.getClaimAgainstCash() ${} == ${}", reservedCash, orderToFill.getClaimAgainstCash(), reservedCash.subtract(orderToFill.getClaimAgainstCash()));
		reservedCash = reservedCash.subtract(orderToFill.getClaimAgainstCash());
		/* Reserve cash if position creates liability (selling an option or shorting a stock) */
		LOGGER.debug("freeCash ${} -= positionTakenByOrder.getClaimAgainstCash() ${} == ${}", freeCash, positionTakenByOrder.getClaimAgainstCash(), freeCash.subtract(positionTakenByOrder.getClaimAgainstCash()));
		freeCash = freeCash.subtract(positionTakenByOrder.getClaimAgainstCash());
		LOGGER.debug("reservedCash ${} -= positionTakenByOrder.getClaimAgainstCash() ${} == ${}", reservedCash, positionTakenByOrder.getClaimAgainstCash(), reservedCash.add(positionTakenByOrder.getClaimAgainstCash()));
		reservedCash = reservedCash.add(positionTakenByOrder.getClaimAgainstCash());
		/* Adjust free cash based on position cost basis */
		LOGGER.debug("freeCash ${} -= positionTakenByOrder.getCostBasis() ${} == ${}", freeCash, positionTakenByOrder.getCostBasis(), freeCash.subtract(positionTakenByOrder.getCostBasis()));
		freeCash = freeCash.subtract(positionTakenByOrder.getCostBasis());
		calculateTotalCash();
		orderToFill.fill(fillPrice);
		dataStore.write(positionTakenByOrder);
		dataStore.close(orderToFill);
	}

	private void reconcileExpiredOptionPosition(Position expiredOptionPosition) {

		ZonedDateTime expiryFriday = expiredOptionPosition.getExpiry().minusDays(1).atTime(16, 20).atZone(ZoneId.of("America/New_York"));
		InstantPrice expirationPrice = expiredOptionPosition.getSecurity().getLastPrice();
		if (expirationPrice.getInstant().isBefore(expiryFriday.toInstant()))
			if (expiredOptionPosition.isPut() &&
					(expirationPrice.getPrice().compareTo(expiredOptionPosition.getStrikePrice()) <= 0)) {
				exerciseOption(expiredOptionPosition);
			} else if (expiredOptionPosition.isCall() &&
					(expirationPrice.getPrice().compareTo(expiredOptionPosition.getStrikePrice()) >= 0)) {
				exerciseOption(expiredOptionPosition);
			} else {
				expireOptionPosition(expiredOptionPosition);
			}
	}

	void exerciseOption(Position optionPositionToExercise) {
		/* If short put buy the stock at the strike price
		 * if short call find a position in the stock to sell at strike price or buy the stock and then deliver
		 * If long put find position to put, or take the cash
		 * If long call buy stock at strike price, or take the cash
		 */
		LOGGER.debug("Entering Portfolio.exerciseOption(Position {})", optionPositionToExercise.getPositionId());
		if (optionPositionToExercise.isShort()) {
			if (optionPositionToExercise.isPut()) {
				exerciseShortPut(optionPositionToExercise);
			} else {
				exerciseShortCall(optionPositionToExercise);
			}
		} else {
			if (optionPositionToExercise.isPut()) {
				exerciseLongPut(optionPositionToExercise);
			} else {
				exerciseLongCall(optionPositionToExercise);
			}
		}
		optionPositionToExercise.close(BigDecimal.ZERO);
		dataStore.close(optionPositionToExercise);
		LOGGER.debug("reservedCash ${} -= optionPositionToExercise.getClaimAgainstCash() ${} == ${}",
				reservedCash, optionPositionToExercise.getClaimAgainstCash(),
				reservedCash.subtract(optionPositionToExercise.getClaimAgainstCash()));
		reservedCash = reservedCash.subtract(optionPositionToExercise.getClaimAgainstCash());
	}

	private void exerciseShortPut(Position optionPositionToExercise) {
		LOGGER.debug("Entering Portfolio.exerciseShortPut(Position {})", optionPositionToExercise.getPositionId());
		Position optionToStockPosition = Position.exerciseOptionPosition(optionPositionToExercise);
		positions.add(optionToStockPosition);
		dataStore.write(optionToStockPosition);
	}

	private void exerciseShortCall(Position optionPositionToExercise) {
		LOGGER.debug("Entering Portfolio.exerciseShortCall(Position {})", optionPositionToExercise.getPositionId());
		int contractsToHonor = optionPositionToExercise.getNumberTransacted();
		while (contractsToHonor > 0) {
			Position calledAwayStockPosition = findStockPositionToDeliver(optionPositionToExercise.getUnderlyingTicker());
			if (calledAwayStockPosition != null) {
				while ((calledAwayStockPosition.getNumberTransacted() >= 100) && (contractsToHonor > 0)) {
					/* Exercise 100 shares / 1 contract per loop */
					calledAwayStockPosition.setNumberTransacted(calledAwayStockPosition.getNumberTransacted() - 100);
					contractsToHonor--;
					LOGGER.debug("freeCash {} += optionPositionToExercise.getStrikePrice() {} * 100",
							freeCash, optionPositionToExercise.getStrikePrice());
					freeCash = freeCash.add(optionPositionToExercise.getStrikePrice().multiply(BIGDECIMAL_ONE_HUNDRED));
					LOGGER.debug("freeCash == {}", freeCash);
				}
				if (calledAwayStockPosition.getNumberTransacted() == 0) {
					calledAwayStockPosition.close(optionPositionToExercise.getStrikePrice());
				}
			} else {
				/* Buy the stock at market price and deliver it */
				optionPositionToExercise.setNumberTransacted(contractsToHonor);
				Position buyStockToDeliverPosition = Position.exerciseOptionPosition(optionPositionToExercise);
				BigDecimal positionLastTick = buyStockToDeliverPosition.getLastTick();
				LOGGER.debug("freeCash ${} -= buyStockToDeliverPosition.getLastTick() ${} * buyStockToDeliverPosition.getNumberTransacted() ${}",
						freeCash, positionLastTick, buyStockToDeliverPosition.getNumberTransacted());
				freeCash = freeCash.subtract(positionLastTick).multiply(new BigDecimal(buyStockToDeliverPosition.getNumberTransacted()));
				LOGGER.debug("freeCash == ${}", freeCash);
				buyStockToDeliverPosition.close(optionPositionToExercise.getStrikePrice());
				contractsToHonor--;
				LOGGER.debug("freeCash ${} += optionPositionToExercise.getStrikePrice() ${} * 100.00", freeCash, optionPositionToExercise.getStrikePrice());
				freeCash = freeCash.add(optionPositionToExercise.getStrikePrice()).multiply(BIGDECIMAL_ONE_HUNDRED);
				LOGGER.debug("freeCash == ${}", freeCash);
			}
		}
	}

	private void exerciseLongPut(Position optionPositionToExercise) {
		LOGGER.debug("Entering Portfolio.exerciseLongPut(Position {})", optionPositionToExercise.getPositionId());
		for (int contractsToHonor = 1; contractsToHonor <= optionPositionToExercise.getNumberTransacted(); contractsToHonor++) {
			Position puttingToStockPosition = findStockPositionToDeliver(optionPositionToExercise.getUnderlyingTicker());
			if (puttingToStockPosition != null) {
				puttingToStockPosition.close(optionPositionToExercise.getStrikePrice());
				dataStore.close(puttingToStockPosition);
				BigDecimal newFreeCash = freeCash.add(optionPositionToExercise.getStrikePrice().multiply(
						new BigDecimal(optionPositionToExercise.getNumberTransacted()).multiply(BIGDECIMAL_ONE_HUNDRED)));
				LOGGER.debug("freeCash ${} += optionPositionToExercise.getStrikePrice() ${} * optionPositionToExercise.getNumberTransacted() ${} * 100.0== ${}",
						freeCash, optionPositionToExercise.getStrikePrice(), optionPositionToExercise.getNumberTransacted(),
						newFreeCash);
				freeCash = newFreeCash;
			} else {
				/* Buy the stock at market price and deliver it */
				Position buyStockToDeliverPosition = Position.exerciseOptionPosition(optionPositionToExercise);
				BigDecimal newFreeCash = freeCash.subtract(buyStockToDeliverPosition.getCostBasis());
				LOGGER.debug("freeCash ${} -= buyStockToDeliverPosition.getCostBasis() ${} = ${}", freeCash, buyStockToDeliverPosition.getCostBasis());
				freeCash = newFreeCash;
				buyStockToDeliverPosition.close(optionPositionToExercise.getStrikePrice());
				newFreeCash = buyStockToDeliverPosition.getPriceAtOpen().multiply(
						new BigDecimal(buyStockToDeliverPosition.getNumberTransacted()));
				LOGGER.debug("freeCash ${} += buyStockToDeliverPosition.getPriceAtOpen() ${} * buyStockToDeliverPosition.getNumberTransacted() ${} = ${}",
						freeCash, buyStockToDeliverPosition.getPriceAtOpen(), buyStockToDeliverPosition.getNumberTransacted(), newFreeCash);
				freeCash = newFreeCash;
			}
		}
	}

	private void exerciseLongCall(Position optionPositionToExercise) {
		LOGGER.debug("Entering exerciseLongCall(Position {})", optionPositionToExercise.getPositionId());
		Position optionToStockPosition = Position.exerciseOptionPosition(optionPositionToExercise);
		positions.add(optionToStockPosition);
		BigDecimal newFreeCash = freeCash.subtract(optionToStockPosition.getCostBasis());
		LOGGER.debug("freeCash ${} -= optionToStockPosition.getCostBasis() ${} = ${}",
				freeCash, optionToStockPosition.getCostBasis(), newFreeCash);
		freeCash = newFreeCash;
		dataStore.write(optionToStockPosition);
	}

	void expireOptionPosition(Position optionPositionToExercise) {
		LOGGER.debug("Entering Portfolio.expireOptionPosition(Position {})", optionPositionToExercise.getPositionId());
		if (optionPositionToExercise.isStock()) {
			LOGGER.warn("Attempted to expire a stock position");
			return;
		}
		BigDecimal newFreeCash = freeCash.add(optionPositionToExercise.getClaimAgainstCash());
		LOGGER.debug("freeCash ${} += optionPositionToExercise.getClaimAgainstCash() ${} = ${}",
				freeCash, optionPositionToExercise.getClaimAgainstCash(), newFreeCash);
		freeCash = newFreeCash;
		BigDecimal newReservedCash = reservedCash.subtract(optionPositionToExercise.getClaimAgainstCash());
		LOGGER.debug("reservedCash -= optionPositionToExercise.getClaimAgainstCash()",
				reservedCash, optionPositionToExercise.getClaimAgainstCash(), newReservedCash);
		reservedCash = newReservedCash;
		optionPositionToExercise.close(BigDecimal.ZERO);
		dataStore.close(optionPositionToExercise);
	}

	/* When exercising a short call this method returns a position that can fulfill delivery
	Must be a stock position with the same ticker as the short call. There may be multiple positions, so deliver
	the position with the lowest cost basis. Make sure there are at least 100 shares.
	 */
	Position findStockPositionToDeliver(String tickerToDeliver) {
		LOGGER.debug("Entering Portfolio.findStockPositionToDeliver(String {})", tickerToDeliver);
		BigDecimal lowestCostBasis = BigDecimal.ZERO;
		Position positionToDeliver = null;
		for (Position openPosition : getListOfOpenPositions()) {
			if (openPosition.isStock() &&
					openPosition.getTicker().equals(tickerToDeliver) &&
					(openPosition.getCostBasis().compareTo(lowestCostBasis) < 0) &&
					(openPosition.getNumberTransacted() >= 100)) {
				lowestCostBasis = openPosition.getCostBasis();
				positionToDeliver = openPosition;
			}
		}
		return positionToDeliver;
	}

	void endOfDayDataStoreWrite() {
		LOGGER.debug("Entering Portfolio.endOfDayDbWrite()");
		dataStore.write(getSummary());
		positions.stream().forEach(dataStore::write);
	}


	public PortfolioSummary getSummary() {
		return new PortfolioSummary(name, netAssetValue, freeCash, reservedCash, totalCash);
	}

	public void expireOrder(Order expiredOrder) {
		LOGGER.debug("Entering Portfolio.expireOrder(Order {}) with freeCash {} and reservedCash {}",
				expiredOrder.getOrderId(), freeCash, reservedCash);
		freeCash = freeCash.add(expiredOrder.getClaimAgainstCash());
		reservedCash = reservedCash.subtract(expiredOrder.getClaimAgainstCash());
		LOGGER.debug("claimAgainstCash() ${}, freeCash ${}, reservedCash ${}", expiredOrder.getClaimAgainstCash(), freeCash, reservedCash);
		calculateTotalCash();
		expiredOrder.closeExpired();
		dataStore.close(expiredOrder);
	}

	void updateOptionPositions(Stock stock) throws IOException, SQLException {
		// TODO : Load returning options tickers into a set and only getLastTick() once
		LOGGER.debug("Entering Portfolio.updateOptionPositions");
		for (Position optionPositionToUpdate : getListOfOpenOptionPositions()) {
			if (stock.getTicker().equals(optionPositionToUpdate.getUnderlyingTicker())) {
				optionPositionToUpdate.setLastPrice(optionPositionToUpdate.getLastTick());
				optionPositionToUpdate.calculateNetAssetValue();
				dataStore.write(optionPositionToUpdate);
			}
		}
	}

	void updateStockPositions(Stock stock) throws IOException, SQLException {
		LOGGER.debug("Entering Portfolio.updateStockPositions");
		for (Position stockPositionToUpdate : getListOfOpenStockPositions()) {
			if (stockPositionToUpdate.getTicker().equals(stock.getTicker())) {
				stockPositionToUpdate.setPrice(stock.getPrice());
				stockPositionToUpdate.calculateNetAssetValue();
				dataStore.write(stockPositionToUpdate);
			}
		}
	}

	public List<Stock> getWatchList() {
		return Collections.EMPTY_LIST;
	}
}
