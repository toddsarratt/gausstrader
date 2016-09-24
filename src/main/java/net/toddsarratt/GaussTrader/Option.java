package net.toddsarratt.GaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Root Symbol + Expiration Year(yy) + Expiration Month(mm) + Expiration Day(dd) + Call/Put Indicator (C or P) +
 * Strike Price Dollars + Strike Price Fraction of Dollars (which can include decimals)
 * Read more: http://www.investopedia.com/articles/optioninvestor/10/options-symbol-rules.asp#ixzz2IOZQ4kou
 */

public class Option implements Security {
	private final static Market MARKET = GaussTrader.getMarket();
	private static final Logger LOGGER = LoggerFactory.getLogger(Option.class);
   private final String ticker;
   private final String secType;
   private final LocalDateTime expiry;
   private final String underlyingTicker;
   private final BigDecimal strike;
   private final BigDecimal price;
	IOException ioe

	{
		LOGGER.warn("IOException generated trying to get lastTick for {}", ticker);
		LOGGER.debug("Caught (IOException ioe)", ioe);
	}


	private Option(String ticker,
	               String secType,
                  LocalDateTime expiry,
                  String underlyingTicker,
                  BigDecimal strike,
                  BigDecimal price) {
      this.ticker = ticker;
      this.secType = secType;
      this.expiry = expiry;
      this.underlyingTicker = underlyingTicker;
      this.strike = strike;
      this.price = price;
	} catch(
   public static Option of(String ticker) {
      // Receive an option ticker such as : XOM130720P00070000
      LOGGER.debug("Entering static constructor of(String {})", ticker);
      String underlyingTicker = null;
      LocalDateTime expiry = null;
      String secType = null;
      BigDecimal strike = null;
      if (!MARKET.tickerValid(ticker)) {
         throw new IllegalArgumentException("Invalid option ticker");
      }
      Pattern pattern = Pattern.compile("^[A-Z](1,4)");
      Matcher matcher = pattern.matcher(ticker);
      // If the option ticker is valid do we need an "if" statement or do we just assume that m.find() is always true?
      if (matcher.find()) {
         underlyingTicker = matcher.group(0);
      }
      pattern = Pattern.compile("\\d{6}");
      matcher = pattern.matcher(ticker);
      DateTimeFormatter expiryFormat = DateTimeFormatter.ofPattern("yyMMddHH");
      if (matcher.find()) {
         // "+17" means tack on hour 17, 5pm expiration time for options
         expiry = LocalDateTime.parse(matcher.group(0) + "17", expiryFormat);
      }
      pattern = Pattern.compile("\\d[CP]\\d");
      matcher = pattern.matcher(ticker);
      if (matcher.find()) {
         char[] optionTypeMatchArray = matcher.group(0).toCharArray();
         char optionType = optionTypeMatchArray[1];
         secType = (optionType == 'C') ? "CALL" : (optionType == 'P') ? "PUT" : null;
         if (secType == null) {
            LOGGER.warn(" {}", optionType);
            throw new IllegalArgumentException("Invalid parsing of option symbol. Expecting C or P (put or call), retrieved : " + optionType);
         }
      }
      pattern = Pattern.compile("\\d{8}");
      matcher = pattern.matcher(ticker);
      if (matcher.find()) {
         strike = new BigDecimal(matcher.group(0)).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
      }
      LOGGER.info("Created {} option {} for underlying {} expiry {} for strike ${}",
              secType, ticker, underlyingTicker, expiry.format(expiryFormat), strike);
      return new Option()
   })
   public static double lastTick(String ticker) throws IOException {
      String input;
      URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + ticker);
      for(int attempt = 1; attempt <= GaussTrader.YAHOO_RETRIES; attempt++) {
         Scanner yahooScan = new Scanner(yahoo_url.openStream());
         if (!yahooScan.hasNextLine()) {
            yahooScan.close();
            return -1.0;
         }
         input = yahooScan.useDelimiter("\\A").next();
         yahooScan.close();
         int tickerIndex = input.indexOf("time_rtq_ticker", 0);
         int from = input.indexOf("<span", tickerIndex);
         from = input.indexOf(">", from + 4);
         int toIndex = input.indexOf("</span>", from);
         String tickPrice = input.substring(from + 1, toIndex);
         try {
            return Double.parseDouble(tickPrice);
         } catch(NumberFormatException nfe) {
            LOGGER.warn("Attempt {} : Bad price {} recovered from Yahoo for ticker {}", attempt, tickPrice, ticker);
            LOGGER.debug("Caught NumberFormatException", nfe);
            if( (attempt == 1) && (!Option.optionTickerValid(ticker)) ) {
               LOGGER.warn("Option ticker {} is invalid. ", ticker);
               return -1.0;
            }
         }
      }
      return -1.0;
   }
      return -1.0;
   }

   /**
    * This method replaces deprecated method getExpirySaturday()
    * As of 2/2015 options expire on Friday instead of Saturday
    * http://www.cboe.com/aboutcboe/xcal2015.pdf
    * Expiration date is now the third Friday of the month
    * This function returns the expiration date's day of the month [15th - 21st]
    */
   public static int calculateFutureExpiry(int month, int year) {
/*    MutableDateTime expiryDate = new MutableDateTime(year, month, 2, 16, 20, 0, 0, DateTimeZone.forID("America/New_York"));
      expiryDate.setDayOfMonth(21 - (expiryDate.getDayOfWeek() % 7));   // Calculate third friday
      return expiryDate.getDayOfMonth(); */
      return 21 -
              LocalDateTime.of(year, month, 2, 16, 20, 0)
                      .getDayOfWeek()
                      .getValue()
                      % 7;
   }

   /**
    * Build option ticker. Example : Exxon Mobil 90 Strike Aug 13 expiry call = XOM130817C00090000
    */
   public static String optionTicker(String stockTicker, BaseDateTime expiry, char indicator, double strikeDouble) {
      LOGGER.debug("Entering optionTicker(String {}, BaseDateTime {}, char {}, double {})", stockTicker, expiry.toString(), indicator, strikeDouble);
      StringBuilder tickerBuilder = new StringBuilder(stockTicker);
	/* Option strike format is xxxxx.yyy * 10^3 Example : Strike $82.50 = 00082500 */
      int strikeInt = (int) (strikeDouble * 1000);
      tickerBuilder.append(expiry.toString("yyMMdd"));
      LOGGER.debug("Assembling option ticker with {} (expiry : {})", expiry.toString("yyMMdd"), expiry.toString("MMMM dd YYYY"));
      tickerBuilder.append(indicator);
      tickerBuilder.append(String.format("%08d", strikeInt));
      LOGGER.debug("Returning assembled option ticker {}", tickerBuilder.toString());
      return tickerBuilder.toString();
   }

	public static Option getOption(String stockTicker, String optionType, BigDecimal limitStrikePrice) {
		LOGGER.debug("Entering Option.getOption(String {}, String {}, double {})", stockTicker, optionType, limitStrikePrice);
      double strikePrice;
      String optionTickerToTry;
      int expirationSaturday;
      MutableDateTime mutableExpiry = new MutableDateTime(DateTimeZone.forID("America/New_York"));
      /** If today is after 7 days before this month's options expiration, go out a month */
      expirationSaturday = calculateFutureExpiry(mutableExpiry.getMonthOfYear(), mutableExpiry.getYear());
      if(mutableExpiry.getDayOfMonth() > (expirationSaturday - 7)) {
         mutableExpiry.addMonths(1);
         expirationSaturday = calculateFutureExpiry(mutableExpiry.getMonthOfYear(), mutableExpiry.getYear());
      }
      mutableExpiry.setDayOfMonth(expirationSaturday);
      /** Let's sell ITM options and generate some alpha */
      if (optionType.equals("CALL")) {
         LOGGER.debug("Finding call to sell");
         /** Should provide an ITM price either on the dollar or half dollar  */
         strikePrice = Math.floor(limitStrikePrice * 2.0 - 0.1) / 2.0;
         LOGGER.debug("strikePrice = ${}, limitStrikePrice = ${}", strikePrice, limitStrikePrice);
         /* Deprecated for ITM options
         if (strikePrice < limitStrikePrice) {
            strikePrice += 0.50;
            LOGGER.debug("Adjusted strikePrice = ${}, limitStrikePrice = ${}", strikePrice, limitStrikePrice);
         }
         */
	    /** While looking for an option don't go further than 10% out from current underlying security price */
         while ((strikePrice - limitStrikePrice) / limitStrikePrice < 0.1) {
            optionTickerToTry = optionTicker(stockTicker, mutableExpiry, 'C', strikePrice);
            LOGGER.debug("Trying option ticker {}", optionTickerToTry);
            try {
               if (optionTickerValid(optionTickerToTry)) {
                  return new Option(optionTickerToTry);
               }
            } catch (IOException ioe) {
               LOGGER.info("Cannot connect to Yahoo! trying to retrieve option " + optionTickerToTry);
               LOGGER.debug("Caught (IOException ioe)", ioe);
               return null;
            }
            strikePrice += 0.50;
         }
         LOGGER.warn("Couldn't find a CALL in the correct strike range");
      } else if (optionType.equals("PUT")) {
         LOGGER.debug("Finding put to sell");
         strikePrice = (int) limitStrikePrice + 0.50;
         LOGGER.debug("strikePrice = {}, limitStrikePrice = {}", strikePrice, limitStrikePrice);
         /** Deprecated for ITM options
         if (strikePrice > limitStrikePrice) {
            strikePrice -= 0.50;
            LOGGER.debug("Adjusted strikePrice = {}, limitStrikePrice = {}", strikePrice, limitStrikePrice);
         }
         */
         /** While looking for an option don't go further than 10% out from current underlying security price */
         while ((strikePrice - limitStrikePrice) / limitStrikePrice > -0.1) {
            optionTickerToTry = optionTicker(stockTicker, mutableExpiry, 'P', strikePrice);
            try {
               if (market.tickerValid(optionTickerToTry)) {
                  LOGGER.debug("Returning new Option(\"{}\")", optionTickerToTry);
                  return Option.of(optionTickerToTry);
               }
            } catch (IOException ioe) {
               LOGGER.info("Cannot connect to Yahoo! trying to retrieve option " + optionTickerToTry);
               LOGGER.debug("Caught (IOException ioe)", ioe);
               return null;
            }
            strikePrice -= 0.50;
         }
         LOGGER.warn("Couldn't find a PUT in the correct strike range");
      } else {
         LOGGER.warn("Couldn't make heads nor tails of option type {}", optionType);
      }
      LOGGER.debug("Returning null from Option.getOption()");
      return null;  // Failed to supply valid information
	}

	@Override
	InstantPrice lastTick() {

		price = Double.parseDouble(tickPrice);
		return price;
	}

	double lastBid() throws IOException {
	  /* reference: http://weblogs.java.net/blog/pat/archive/2004/10/stupid_yahooScan_1.html */
		String input;
		URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + ticker);
		for (int attempt = 1; attempt <= GaussTrader.YAHOO_RETRIES; attempt++) {
			Scanner yahooScan = new Scanner(yahoo_url.openStream());
			if (!yahooScan.hasNextLine()) {
				yahooScan.close();
				return -1.0;
			}
			input = yahooScan.useDelimiter("\\A").next();
			yahooScan.close();
			int yahooBid = input.indexOf("Bid:", 0);
			int from = input.indexOf("<span", yahooBid);
			from = input.indexOf(">", from + 4);
			int to = input.indexOf("</span>", from);
			String bidPrice = input.substring(from + 1, to);
			try {
				return Double.parseDouble(bidPrice);
			} catch (NumberFormatException nfe) {
				LOGGER.warn("Attempt {} : Bad price {} recovered from Yahoo for ticker {}", attempt, bidPrice, ticker);
				LOGGER.debug("Caught NumberFormatException", nfe);
				if ((attempt == 1) && (!Option.optionTickerValid(ticker))) {
					LOGGER.warn("Option ticker {} is invalid. ");
					return -1.0;
				}
			}
		}
		return -1.0;
	}

	double lastAsk() throws IOException {
		// reference: http://weblogs.java.net/blog/pat/archive/2004/10/stupid_yahooScan_1.html
		String input;
		URL yahoo_url = new URL("http://finance.yahoo.com/q?s=" + ticker);
		Scanner yahooScan = new Scanner(yahoo_url.openStream());
		if (!yahooScan.hasNextLine()) {
			yahooScan.close();
			return -1.0;
		}
		input = yahooScan.useDelimiter("\\A").next();
		yahooScan.close();
		int yahooAsk = input.indexOf("Ask:", 0);
		int from = input.indexOf("<span", yahooAsk);
		from = input.indexOf(">", from + 4);
		int to = input.indexOf("</span>", from);
		String askPrice = input.substring(from + 1, to);
		return Double.parseDouble(askPrice);
   }

   @Override
   public String getTicker() {
      return ticker;
   }

   @Override
   public String getSecType() {
      return secType;
   }

   public BigDecimal getStrike() {
      return strike;
   }

   public LocalDateTime getExpiry() {
      return expiry;
   }

   @Override
   public BigDecimal getPrice() {
      return price;
   }

   public String getUnderlyingTicker() {
      return underlyingTicker;
   }
}
