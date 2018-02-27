package net.toddsarratt.gaussTrader.persistence.entity;

import net.toddsarratt.gaussTrader.singletons.Sentiment;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table
public class Position implements Serializable {

	@Id
	public Long id;
	@ManyToOne(fetch = FetchType.LAZY)
	private Portfolio portfolio;
	@OneToOne
	private Order originatingOrder;
	@ManyToOne
	private Security security;
	private Sentiment sentiment;
	private Boolean open;
	private Integer numberTransacted;
	private Instant instantOpened;
	private String buyOrSell;
	private BigDecimal priceAtOpen;
	private BigDecimal costBasis;
	private BigDecimal claimAgainstCash;
	private BigDecimal netAssetValue;
	private Instant instantClosed;
	private BigDecimal priceAtClose;
	private BigDecimal profit;

	public Position() {}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Portfolio getPortfolio() {
		return portfolio;
	}

	public void setPortfolio(Portfolio portfolio) {
		this.portfolio = portfolio;
	}

	public Order getOriginatingOrder() {
		return originatingOrder;
	}

	public void setOriginatingOrder(Order originatingOrder) {
		this.originatingOrder = originatingOrder;
	}

	public Security getSecurity() {
		return security;
	}

	public void setSecurity(Security security) {
		this.security = security;
	}

	public Sentiment getSentiment() {
		return sentiment;
	}

	public void setSentiment(Sentiment sentiment) {
		this.sentiment = sentiment;
	}

	public Boolean isOpen() {
		return open;
	}

	public void setOpen(Boolean open) {
		this.open = open;
	}

	public Integer getNumberTransacted() {
		return numberTransacted;
	}

	public void setNumberTransacted(Integer numberTransacted) {
		this.numberTransacted = numberTransacted;
	}

	public Instant getInstantOpened() {
		return instantOpened;
	}

	public void setInstantOpened(Instant instantOpened) {
		this.instantOpened = instantOpened;
	}

	public String getBuyOrSell() {
		return buyOrSell;
	}

	public void setBuyOrSell(String buyOrSell) {
		this.buyOrSell = buyOrSell;
	}

	public BigDecimal getPriceAtOpen() {
		return priceAtOpen;
	}

	public void setPriceAtOpen(BigDecimal priceAtOpen) {
		this.priceAtOpen = priceAtOpen;
	}

	public BigDecimal getCostBasis() {
		return costBasis;
	}

	public void setCostBasis(BigDecimal costBasis) {
		this.costBasis = costBasis;
	}

	public BigDecimal getClaimAgainstCash() {
		return claimAgainstCash;
	}

	public void setClaimAgainstCash(BigDecimal claimAgainstCash) {
		this.claimAgainstCash = claimAgainstCash;
	}

	public BigDecimal getNetAssetValue() {
		return netAssetValue;
	}

	public void setNetAssetValue(BigDecimal netAssetValue) {
		this.netAssetValue = netAssetValue;
	}

	public Instant getInstantClosed() {
		return instantClosed;
	}

	public void setInstantClosed(Instant instantClosed) {
		this.instantClosed = instantClosed;
	}

	public BigDecimal getPriceAtClose() {
		return priceAtClose;
	}

	public void setPriceAtClose(BigDecimal priceAtClose) {
		this.priceAtClose = priceAtClose;
	}

	public BigDecimal getProfit() {
		return profit;
	}

	public void setProfit(BigDecimal profit) {
		this.profit = profit;
	}
}
