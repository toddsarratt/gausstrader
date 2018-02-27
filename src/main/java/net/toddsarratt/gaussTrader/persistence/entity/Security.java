package net.toddsarratt.gaussTrader.persistence.entity;

import net.toddsarratt.gaussTrader.singletons.SecurityType;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table
public abstract class Security implements Serializable {

	@Id
	private String ticker;
	private SecurityType securityType;

	public Security() {}

	public SecurityType getSecurityType() {
		return securityType;
	}

	public void setSecurityType(SecurityType securityType) {
		this.securityType = securityType;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}
}
