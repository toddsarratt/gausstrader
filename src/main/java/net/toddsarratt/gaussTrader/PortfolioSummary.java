package net.toddsarratt.gaussTrader;

import java.math.BigDecimal;

/**
 * PortfolioSummary is an immutable tuple representing the following values of a Portfolio object :
 * name, netAssetValue, freeCash, reservedCash, totalCash
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class PortfolioSummary {
   private final String name;
   private final BigDecimal netAssetValue;
   private final BigDecimal freeCash;
   private final BigDecimal reservedCash;
   private final BigDecimal totalCash;

   public PortfolioSummary(String name, BigDecimal netAssetValue, BigDecimal freeCash, BigDecimal reservedCash, BigDecimal totalCash) {
      this.name = name;
      this.netAssetValue = netAssetValue;
      this.freeCash = freeCash;
      this.reservedCash = reservedCash;
      this.totalCash = totalCash;
   }

   public String getName() {
      return name;
   }

   public BigDecimal getNetAssetValue() {
      return netAssetValue;
   }

   public BigDecimal getFreeCash() {
      return freeCash;
   }

   public BigDecimal getReservedCash() {
      return reservedCash;
   }

   public BigDecimal getTotalCash() {
      return totalCash;
   }
}
