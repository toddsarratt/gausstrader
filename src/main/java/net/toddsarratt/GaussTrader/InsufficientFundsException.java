package net.toddsarratt.GaussTrader;

import java.math.BigDecimal;

public class InsufficientFundsException extends Exception {

   InsufficientFundsException() {
      super();
   }

   InsufficientFundsException(BigDecimal requiredFreeCash, BigDecimal currentFreeCash) {
   	super("Funds required = " + requiredFreeCash +
		    " : Funds available " + currentFreeCash
    );
   }
}
