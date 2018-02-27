package net.toddsarratt.gaussTrader.persistence.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table
public class Portfolio implements Serializable {

	@Id
	private String name;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio")
	private Set<Position> positions;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio")
	private Set<Order> orders;

	private BigDecimal netAssetValue;
	private BigDecimal freeCash;
	private BigDecimal reservedCash;
	private BigDecimal totalCash;

	/**
	 * See <a href="https://docs.oracle.com/javaee/5/tutorial/doc/bnbqa.html">
	 * Introduction to the Java Persistence API</a>
	 * <p>
	 * <quote>
	 * The class must have a public or protected, no-argument constructor. The class may have other constructors.
	 * </quote>
	 */
	public Portfolio() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Position> getPositions() {
		return positions;
	}

	public void setPositions(Set<Position> positions) {
		this.positions = positions;
	}

	public Set<Order> getOrders() {
		return orders;
	}

	public void setOrders(Set<Order> orders) {
		this.orders = orders;
	}

	public BigDecimal getNetAssetValue() {
		return netAssetValue;
	}

	public void setNetAssetValue(BigDecimal netAssetValue) {
		this.netAssetValue = netAssetValue;
	}

	public BigDecimal getFreeCash() {
		return freeCash;
	}

	public void setFreeCash(BigDecimal freeCash) {
		this.freeCash = freeCash;
	}

	public BigDecimal getReservedCash() {
		return reservedCash;
	}

	public void setReservedCash(BigDecimal reservedCash) {
		this.reservedCash = reservedCash;
	}

	public BigDecimal getTotalCash() {
		return totalCash;
	}

	public void setTotalCash(BigDecimal totalCash) {
		this.totalCash = totalCash;
	}
}
