package net.toddsarratt.gaussTrader;

import net.toddsarratt.gaussTrader.singletons.SecurityType;

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

	public PositionBuilder() {
		position = new Position();
	}

	public Position build() {
		return position;
	}

	public PositionBuilder positionId(long positionId) {
		position.setPositionId(positionId);
		return this;
	}

	public PositionBuilder originatingOrderId(TransactionId originatingOrderId) {
		position.setOriginatingOrderId(originatingOrderId);
		return this;
	}

	public PositionBuilder open(boolean open) {
		position.setOpen(open);
		return this;
	}

	public PositionBuilder ticker(String ticker) {
		position.setTicker(ticker);
		return this;
	}

	public PositionBuilder securityType(SecurityType secType) {
		position.setSecType(secType);
		return this;
	}

	public PositionBuilder expiry(LocalDate expiry) {
		position.setExpiry(expiry);
		return this;
	}

	public PositionBuilder underlyingTicker(String underlyingTicker) {
		position.setUnderlyingTicker(underlyingTicker);
		return this;
	}

	public PositionBuilder strikePrice(BigDecimal strikePrice) {
		position.setStrikePrice(strikePrice);
		return this;
	}

	public PositionBuilder instantOpened(Instant instantOpened) {
		position.setInstantOpened(instantOpened);
		return this;
	}

	public PositionBuilder longPosition(boolean longPosition) {
		position.setBuyOrSell(longPosition);
		return this;
	}

	public PositionBuilder numberTransacted(int numberTransacted) {
		position.setNumberTransacted(numberTransacted);
		return this;
	}

	public PositionBuilder priceAtOpen(BigDecimal priceAtOpen) {
		position.setPriceAtOpen(priceAtOpen);
		return this;
	}

	public PositionBuilder costBasis(BigDecimal costBasis) {
		position.setCostBasis(costBasis);
		return this;
	}

	public PositionBuilder claimAgainstCash(BigDecimal claimAgainstCash) {
		position.setClaimAgainstCash(claimAgainstCash);
		return this;
	}

	public PositionBuilder price(InstantPrice price) {
		position.setLastTick(price);
		return this;
	}

	public PositionBuilder netAssetValue(BigDecimal netAssetValue) {
		position.setNetAssetValue(netAssetValue);
		return this;
	}

	public PositionBuilder instantClosed(Instant instantClosed) {
		position.setInstantClosed(instantClosed);
		return this;
	}

	public PositionBuilder priceAtClose(BigDecimal priceAtClose) {
		position.setPriceAtClose(priceAtClose);
		return this;
	}

	public PositionBuilder profit(BigDecimal profit) {
		position.setProfit(profit);
		return this;
	}
}
