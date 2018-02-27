package net.toddsarratt.gaussTrader.persistence.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table
public class Order implements Serializable {

	@Id
	public Long id;
	@ManyToOne
	Security security;
	Boolean open;
	@ManyToOne(fetch = FetchType.LAZY)
	private Portfolio portfolio;
	private Instant instantOpened;
	// PriceBasedAction fields
	private BigDecimal triggerPrice;
	private boolean isActionable;
	private String buyOrSell;
	private String securityType;
	private int numberToTransact;
	private BigDecimal limitPrice;
	private BigDecimal claimAgainstCash;
	private String tif;
	private Instant instantClosed;
	private String closeReason;
	private BigDecimal fillPrice;
	private String underlyingTicker;
	private int totalQuantity;

	public Order() {}
}
