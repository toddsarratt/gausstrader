package net.toddsarratt.GaussTrader.Security;

/**
 * Enum for Security Type. Currently only stocks and options are supported by the application. Bonds, commodities,
 * currencies, and other securities are not.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public enum SecurityType {
	STOCK, CALL, PUT;

	/**
	 * Case-insensitive string representation of a Security Type
	 *
	 * @param secType case-insensitive string of "STOCK", "CALL", or "PUT"
	 * @return appropriate SecrutityType enum value
	 */
	public static SecurityType of(String secType) {
		switch (secType.toUpperCase()) {
			case "STOCK":
				return STOCK;
			case "PUT":
				return PUT;
			case "CALL":
				return CALL;
		}
		throw new IllegalArgumentException(secType + " is not a valid security type for this application");
	}
}


