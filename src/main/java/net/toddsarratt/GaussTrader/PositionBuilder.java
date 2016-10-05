package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Builder class for Position objects. Useful when recreating Positions saved to persistent storage.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class PositionBuilder {
	private Position position;
	private TransactionId positionId;
	private TransactionId originatingOrderId;
	private boolean open;
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
	private BigDecimal price;
	private BigDecimal netAssetValue;
	private Instant instantClosed;
	private BigDecimal priceAtClose;
	private BigDecimal profit;

	public PositionBuilder() {
		position = new Position();
	}

	public Position build() {
		return position;
	}

	public PositionBuilder setPositionId(TransactionId positionId) {
		position.setPositionId(positionId);
		return this;
	}

	public PositionBuilder setOriginatingOrderId(TransactionId originatingOrderId) {
		position.setOriginatingOrderId(originatingOrderId);
		return this;
	}

	public PositionBuilder setOpen(boolean open) {
		position.setOpen(open);
		return this;
	}

	public PositionBuilder setTicker(String ticker) {
		position.setTicker(ticker);
		return this;
	}

	public PositionBuilder setSecType(SecurityType secType) {
		position.setSecType(secType);
		return this;
	}

	public PositionBuilder setExpiry(LocalDate expiry) {
		position.setExpiry(expiry);
		return this;
	}

	public PositionBuilder setUnderlyingTicker(String underlyingTicker) {
		position.setUnderlyingTicker(underlyingTicker);
		return this;
	}

	public PositionBuilder setStrikePrice(BigDecimal strikePrice) {
		position.setStrikePrice(strikePrice);
		return this;
	}

	public PositionBuilder setInstantOpened(Instant instantOpened) {
		position.setInstantOpened(instantOpened);
		return this;
	}

	public PositionBuilder setLongPosition(boolean longPosition) {
		position.setLongPosition(longPosition);
		return this;
	}

	public PositionBuilder setNumberTransacted(int numberTransacted) {
		position.setNumberTransacted(numberTransacted);
		return this;
	}

	public PositionBuilder setPriceAtOpen(BigDecimal priceAtOpen) {
		position.setPriceAtOpen(priceAtOpen);
		return this;
	}

	public PositionBuilder setCostBasis(BigDecimal costBasis) {
		position.setCostBasis(costBasis);
		return this;
	}

	public PositionBuilder setClaimAgainstCash(BigDecimal claimAgainstCash) {
		position.setClaimAgainstCash(claimAgainstCash);
		return this;
	}

	public PositionBuilder setPrice(BigDecimal price) {
		position.setPrice(price);
		return this;
	}

	public PositionBuilder setNetAssetValue(BigDecimal netAssetValue) {
		position.setNetAssetValue(netAssetValue);
		return this;
	}

	public PositionBuilder setInstantClosed(Instant instantClosed) {
		position.setInstantClosed(instantClosed);
		return this;
	}

	public PositionBuilder setPriceAtClose(BigDecimal priceAtClose) {
		position.setPriceAtClose(priceAtClose);
		return this;
	}

	public PositionBuilder setProfit(BigDecimal profit) {
		position.setProfit(profit);
		return this;
	}
}
