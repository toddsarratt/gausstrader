package net.toddsarratt.gaussTrader;

import net.toddsarratt.gaussTrader.domain.Security;
import net.toddsarratt.gaussTrader.market.Market;
import net.toddsarratt.gaussTrader.orders.OptionOrder;
import net.toddsarratt.gaussTrader.orders.Order;
import net.toddsarratt.gaussTrader.singletons.Constants;
import net.toddsarratt.gaussTrader.singletons.SecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class Position {
	private static final Logger LOGGER = LoggerFactory.getLogger(Position.class);
	private static Market market = GaussTrader.getMarket();
	private TransactionId positionId;
	private TransactionId originatingOrderId;
	private boolean open;
	private Security security;
	private String ticker;
	private SecurityType secType;
	private LocalDate expiry;
	private String underlyingTicker;
	private BigDecimal strikePrice;
	private Instant instantOpened;
	private boolean longPosition;
	private int numberTransacted;
	private BigDecimal priceAtOpen;
	private BigDecimal costBasis;
	private BigDecimal claimAgainstCash;
	private InstantPrice price;
	private BigDecimal netAssetValue;
	private Instant instantClosed;
	private BigDecimal priceAtClose;
	private BigDecimal profit;

	Position() {
		LOGGER.debug("Entering Position default constructor");
		this.positionId = new TransactionId();
		this.instantOpened = Instant.now();
		this.open = true;
	}

	public Position(Order orderToFill, BigDecimal priceAtOpen) {
		LOGGER.debug("Entering Position constructor Position(Order {}, price {})", orderToFill.getOrderId(), priceAtOpen);
		this.positionId = new TransactionId();
		this.originatingOrderId = orderToFill.getOrderId();
		this.open = true;
		this.security = orderToFill.getSecurity();
		this.secType = orderToFill.getAction().getSecurityType();
		switch (orderToFill.getAction().getSecurityType()) {
			case PUT:
			case CALL:
				this.expiry = ((OptionOrder) orderToFill).getOption().getExpiry();
				this.underlyingTicker = ((OptionOrder) orderToFill).getOption().getUnderlyingTicker();
				this.strikePrice = ((OptionOrder) orderToFill).getOption().getStrike();
				break;
			default:
				this.underlyingTicker = ticker;
				this.expiry = null;
		}
		this.instantOpened = Instant.now();
		this.longPosition = orderToFill.getAction().getBuyOrSell().equals("BUY");
		this.numberTransacted = orderToFill.getAction().getNumberToTransact();
		this.priceAtOpen = priceAtOpen;
		this.costBasis = calculateCostBasis();
		this.claimAgainstCash = calculateClaimAgainstCash();
		LOGGER.debug("claimAgainstCash = ${}", claimAgainstCash);
		this.price = InstantPrice.of(priceAtOpen, instantOpened);
		this.netAssetValue = costBasis;
		LOGGER.info("New position created with positionId " + positionId + " ticker " + ticker +
				" secType " + secType + " open " + open + " instantOpened " + instantOpened);
		LOGGER.info("longPosition " + longPosition + " numberTransacted " + numberTransacted +
				" priceAtOpen " + priceAtOpen + " costBasis " + costBasis);
	}

	public static Position exerciseOptionPosition(Position exercisingOptionPosition) {
		LOGGER.debug("Entering Position.exerciseOptionPosition(Position {})", exercisingOptionPosition.getPositionId());
		Position newStockPosition = new Position();
		String ticker = exercisingOptionPosition.getUnderlyingTicker();
		BigDecimal lastTick;
		newStockPosition.setTicker(ticker);
		newStockPosition.setUnderlyingTicker(ticker);
		newStockPosition.setSecType(SecurityType.STOCK);
		newStockPosition.setLongPosition(true);
		newStockPosition.setNumberTransacted(exercisingOptionPosition.getNumberTransacted() * 100);
		newStockPosition.setPriceAtOpen(exercisingOptionPosition.getStrikePrice());
		newStockPosition.setCostBasis(newStockPosition.getPriceAtOpen()
				.multiply(new BigDecimal(newStockPosition.getNumberTransacted())));
		newStockPosition.setPrice(lastTick = market.lastTick(exercisingOptionPosition.security).getPrice());
/*      } catch (IOException ioe) {
         LOGGER.info("Could not connect to yahoo! to get lastTick() for {} when exercising option position {}", ticker, exercisingOptionPosition.getPositionId());
         LOGGER.info("lastTick and netAssetValue are incorrect for current open position {}", newStockPosition.getPositionId());
         LOGGER.debug("Caught (IOException ioe)", ioe);
         newStockPosition.setPrice(lastTick = newStockPosition.getPriceAtOpen());
      } */
		newStockPosition.setNetAssetValue(lastTick.multiply(new BigDecimal(newStockPosition.getNumberTransacted())));
		return newStockPosition;
	}

	public void close(BigDecimal closePrice) {
		open = false;
		instantClosed = Instant.now();
		priceAtClose = closePrice;
		profit = priceAtClose
				.multiply(new BigDecimal(numberTransacted * (isStock() ? 1 : 100) * (isLong() ? 1 : -1)))
				.subtract(costBasis);
	}

	public TransactionId getPositionId() {
		return positionId;
	}

	void setPositionId(TransactionId positionId) {
		this.positionId = positionId;
	}

	TransactionId getOriginatingOrderId() {
		return originatingOrderId;
	}

	void setOriginatingOrderId(TransactionId originatingOrderId) {
		this.originatingOrderId = originatingOrderId;
	}

	public String getTicker() {
		return ticker;
	}

	void setTicker(String ticker) {
		this.ticker = ticker;
	}

	SecurityType getSecType() {
		return secType;
	}

	void setSecType(SecurityType secType) {
		this.secType = secType;
	}

	public boolean isCall() {
		return secType.equals(SecurityType.CALL);
	}

	public boolean isPut() {
		return secType.equals(SecurityType.PUT);
	}

	public boolean isStock() {
		return secType.equals(SecurityType.STOCK);
	}

	public boolean isOption() {
		return (isCall() || isPut());
	}

	public LocalDate getExpiry() {
		return expiry;
	}

	void setExpiry(LocalDate expiry) {
		this.expiry = expiry;
	}

	public String getUnderlyingTicker() {
		return underlyingTicker;
	}

	void setUnderlyingTicker(String underlyingTicker) {
		this.underlyingTicker = underlyingTicker;
	}

	public BigDecimal getStrikePrice() {
		return strikePrice;
	}

	void setStrikePrice(BigDecimal strikePrice) {
		this.strikePrice = strikePrice;
	}

	public Instant getInstantOpened() {
		return instantOpened;
	}

	void setInstantOpened(Instant instantOpened) {
		this.instantOpened = instantOpened;
	}

	public boolean isLong() {
		return longPosition;
	}

	void setLongPosition(boolean longPosition) {
		this.longPosition = longPosition;
	}

	public boolean isShort() {
		return !longPosition;
	}

	public int getNumberTransacted() {
		return numberTransacted;
	}

	public void setNumberTransacted(int numberTransacted) {
		this.numberTransacted = numberTransacted;
	}

	public BigDecimal getPriceAtOpen() {
		return priceAtOpen;
	}

	void setPriceAtOpen(BigDecimal priceAtOpen) {
		this.priceAtOpen = priceAtOpen;
	}

	public BigDecimal getCostBasis() {
		return costBasis;
	}

	void setCostBasis(BigDecimal costBasis) {
		this.costBasis = costBasis;
	}

	BigDecimal calculateCostBasis() {
		return priceAtOpen.multiply(new BigDecimal(numberTransacted * (isStock() ? 1 : 100) * (isLong() ? 1 : -1)));
	}

	public InstantPrice getLastTick() {
		// TODO: Market needs to return an InstantPrice and not have one created here
		return price = InstantPrice.of(market.lastTick(security).getPrice(), Instant.now());
	}

	public InstantPrice getPrice() {
		return price;
	}

	void setPrice(InstantPrice lastTick) {
		this.price = lastTick;
	}

	void setNetAssetValue(BigDecimal netAssetValue) {
		this.netAssetValue = netAssetValue;
	}

	public Instant getInstantClosed() {
		if (!open) {
			return instantClosed;
		}
		return Instant.MIN;
	}

	void setInstantClosed(Instant instantClosed) {
		this.instantClosed = instantClosed;
	}

	BigDecimal getPriceAtClose() {
		if (!open) {
			return priceAtClose;
		}
		throw new IllegalStateException("Position is not closed, cannot return priceAtClose");
	}

	void setPriceAtClose(BigDecimal priceAtClose) {
		this.priceAtClose = priceAtClose;
	}

	BigDecimal calculateProfit() {
		return netAssetValue.subtract(costBasis);
	}

	void setProfit(BigDecimal profit) {
		this.profit = profit;
	}

	public boolean isOpen() {
		return open;
	}

	void setOpen(boolean open) {
		this.open = open;
	}

	public BigDecimal getClaimAgainstCash() {
		return claimAgainstCash;
	}

	void setClaimAgainstCash(BigDecimal requiredCash) {
		claimAgainstCash = requiredCash;
	}

	boolean isExpired() {
		return isOption() && expiry.isBefore(LocalDate.from(market.getCurrentDateTime()));
	}

	public Security getSecurity() {
		return security;
	}

	public void setSecurity(Security security) {
		this.security = security;
	}

	/* Position.claimAgainstCash() is a bit disingenuous. Selling a call or shorting a stock
	 * could result in an infinite liability. Only calculating for selling a put which has
	 * a fixed obligation.
	 */
	BigDecimal calculateClaimAgainstCash() {
		return (isPut() && isShort()) ? strikePrice.multiply(new BigDecimal(numberTransacted * 100)) : BigDecimal.ZERO;
	}

	public BigDecimal calculateNetAssetValue() {
		netAssetValue = price.getPrice().multiply(new BigDecimal(numberTransacted))
				.multiply(isStock() ? BigDecimal.ONE : Constants.BIGDECIMAL_ONE_HUNDRED)
				.multiply(isLong() ? BigDecimal.ONE : Constants.BIGDECIMAL_MINUS_ONE);
		return netAssetValue;
	}

	@Override
	public String toString() {
		return (positionId + " | " + ticker + " | " + secType + " | " + open + " | " + instantOpened +
				" | " + longPosition + " | " + numberTransacted + " | " + priceAtOpen + " | " +
				costBasis + " | " + instantClosed + " | " + priceAtClose + " | " + profit);
	}
}
