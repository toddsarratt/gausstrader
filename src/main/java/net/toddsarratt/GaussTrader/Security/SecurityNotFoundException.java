package net.toddsarratt.GaussTrader.Security;

@SuppressWarnings("serial")
public class SecurityNotFoundException extends Exception {
   String ticker = null;

   SecurityNotFoundException() {
      super();
      ticker = "unknown";
   }

   SecurityNotFoundException(String ticker) {
      super();
      this.ticker = ticker;
   }
}
