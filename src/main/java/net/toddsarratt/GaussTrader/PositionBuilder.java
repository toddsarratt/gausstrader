package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger LOGGER = LoggerFactory.getLogger(Position.class);
	private static Market market = GaussTrader.getMarket();
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

	public void setPositionId(TransactionId positionId) {
		position.setPositionId(positionId);
	}

	public void setOriginatingOrderId(TransactionId originatingOrderId) {
		position.setOriginatingOrderId(originatingOrderId);
	}

	public void setOpen(boolean open) {
		position.setOpen(open);
	}

	public void setTicker(String ticker) {
		position.setTicker(ticker);
	}

	public void setSecType(SecurityType secType) {
		position.setSecType(secType);
	}

	public void setExpiry(LocalDate expiry) {
		position.setExpiry(expiry);
	}

	public void setUnderlyingTicker(String underlyingTicker) {
		position.setUnderlyingTicker(underlyingTicker);
	}

	public void setStrikePrice(BigDecimal strikePrice) {
		position.setStrikePrice(strikePrice);
	}

	public void setInstantOpened(Instant instantOpened) {
		position.setInstantOpened(instantOpened);
	}

	public void setLongPosition(boolean longPosition) {
		position.setLongPosition(longPosition);
	}

	public void setNumberTransacted(int numberTransacted) {
		position.setNumberTransacted(numberTransacted);
	}

	public void setPriceAtOpen(BigDecimal priceAtOpen) {
		position.setPriceAtOpen(priceAtOpen);
	}

	public void setCostBasis(BigDecimal costBasis) {
		position.setCostBasis(costBasis);
	}

	public void setClaimAgainstCash(BigDecimal claimAgainstCash) {
		position.setClaimAgainstCash(claimAgainstCash);
	}

	public void setPrice(BigDecimal price) {
		position.setPrice(price);
	}

	public void setNetAssetValue(BigDecimal netAssetValue) {
		position.setNetAssetValue(netAssetValue);
	}

	public void setInstantClosed(Instant instantClosed) {
		position.setInstantClosed(instantClosed);
	}

	public void setPriceAtClose(BigDecimal priceAtClose) {
		position.setPriceAtClose(priceAtClose);
	}

	public void setProfit(BigDecimal profit) {
		position.setProfit(profit);
	}
}
