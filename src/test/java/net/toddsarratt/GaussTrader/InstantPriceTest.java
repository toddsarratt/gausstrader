package net.toddsarratt.GaussTrader;

import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.*;

/**
 * InstantPriceTest
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class InstantPriceTest {
   private static final String NULL_STRING = null;
   private static final CharSequence GOOD_DATE_STRING = "2022-12-18T23:37:00-05:00";
   private static final Instant GOOD_PARSED_INSTANT = Instant.parse(GOOD_DATE_STRING);
   private static final long GOOD_EPOCH = GOOD_PARSED_INSTANT.toEpochMilli();
   private static final String GOOD_PRICE_STRING = "112.35";
   private static final BigDecimal GOOD_PRICE_BIGDECIMAL = new BigDecimal(GOOD_PRICE_STRING);

   @Test(expectedExceptions = IllegalArgumentException.class,
           expectedExceptionsMessageRegExp = "priceString may not be null")
   public void of_StringCharSequence_nullPrice_throwException() {
      InstantPrice nullPrice = InstantPrice.of(NULL_STRING, GOOD_DATE_STRING);
   }

   @Test(expectedExceptions = IllegalArgumentException.class,
           expectedExceptionsMessageRegExp = "date may not be null")
   public void of_stringCharSequence_nullDate_throwException() {
      InstantPrice nullDate = InstantPrice.of(GOOD_PRICE_STRING, NULL_STRING);
   }

   @Test
   public void of_stringCharSequence_returnsObject() {
      InstantPrice instantPrice = InstantPrice.of(GOOD_PRICE_STRING, GOOD_DATE_STRING);
      assertEquals(instantPrice.getClass(), InstantPrice.class);
   }

   @Test
   public void of_stringCharSequence_goodConstructorSetsAndGetters() {
      InstantPrice instantPrice = InstantPrice.of(GOOD_PRICE_STRING, GOOD_DATE_STRING);
      assertEquals(instantPrice.getPrice(), GOOD_PRICE_BIGDECIMAL);
      assertEquals(instantPrice.getPriceAsDouble(), GOOD_PRICE_BIGDECIMAL.doubleValue());
      assertEquals(instantPrice.getInstant(), GOOD_PARSED_INSTANT);
      assertEquals(instantPrice.getEpoch(), GOOD_EPOCH);
   }

   @Test(expectedExceptions = IllegalArgumentException.class,
           expectedExceptionsMessageRegExp = "priceString may not be null")
   public void of_stringNoDate_nullPrice_throwException() {
      InstantPrice nullPrice = InstantPrice.of(NULL_STRING);
   }

   @Test
   public void of_stringNoDate_returnsObject() {
      InstantPrice instantPrice = InstantPrice.of(GOOD_PRICE_STRING);
      assertEquals(instantPrice.getClass(), InstantPrice.class);
   }

   @Test
   public void of_stringNoDate_goodConstructorSetsAndGetters() {
      Instant approximatelyNow = Instant.now();
      Instant fiveSecondsAgo = approximatelyNow.minusSeconds(5);
      long fiveSecondsAgoEpoch = fiveSecondsAgo.toEpochMilli();
      Instant fiveSecondsFromNow = approximatelyNow.plusSeconds(5);
      long fiveSecondsFromNowEpoch = fiveSecondsFromNow.toEpochMilli();
      InstantPrice instantPrice = InstantPrice.of(GOOD_PRICE_STRING);
      assertEquals(instantPrice.getPrice(), GOOD_PRICE_BIGDECIMAL);
      assertEquals(instantPrice.getPriceAsDouble(), GOOD_PRICE_BIGDECIMAL.doubleValue());
      assertTrue(instantPrice.getInstant().isAfter(fiveSecondsAgo) &&
              instantPrice.getInstant().isBefore(fiveSecondsFromNow));
      assertTrue(instantPrice.getEpoch() > fiveSecondsAgoEpoch &&
              instantPrice.getEpoch() < fiveSecondsFromNowEpoch);
   }

   @Test(expectedExceptions = IllegalArgumentException.class,
           expectedExceptionsMessageRegExp = "priceString may not be null")
   public void of_stringLong_nullPrice_throwException() {
      InstantPrice nullPrice = InstantPrice.of(NULL_STRING, GOOD_EPOCH);
   }

   @Test(expectedExceptions = IllegalArgumentException.class,
           expectedExceptionsMessageRegExp = "date may not be null")
   public void of_stringLong_nullDate_throwException() {
      InstantPrice nullDate = InstantPrice.of(GOOD_PRICE_STRING, NULL_STRING);
   }

   @Test
   public void of_stringLong_returnsObject() {
      InstantPrice instantPrice = InstantPrice.of(GOOD_PRICE_STRING, GOOD_EPOCH);
      assertEquals(instantPrice.getClass(), InstantPrice.class);
   }

   @Test
   public void of_stringLong_goodConstructorSetsAndGetters() {
      InstantPrice instantPrice = InstantPrice.of(GOOD_PRICE_STRING, GOOD_EPOCH);
      assertEquals(instantPrice.getPrice(), GOOD_PRICE_BIGDECIMAL);
      assertEquals(instantPrice.getPriceAsDouble(), GOOD_PRICE_BIGDECIMAL.doubleValue());
      assertEquals(instantPrice.getInstant(), GOOD_PARSED_INSTANT);
      assertEquals(instantPrice.getEpoch(), GOOD_EPOCH);
   }
}