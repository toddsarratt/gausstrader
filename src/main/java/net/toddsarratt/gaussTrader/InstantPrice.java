package net.toddsarratt.gaussTrader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * InstantPrice is a tuple representing a price and the instant that price was quoted. The static factory method
 * {@code .of()} is used to create object of this class and is overloaded to accept dates of string or
 * epoch in milliseconds.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
public class InstantPrice {
	public static final InstantPrice NO_PRICE = new InstantPrice(BigDecimal.ZERO, Instant.MIN);
	private static final Logger LOGGER = LoggerFactory.getLogger(InstantPrice.class);
	private final BigDecimal price;
	private final Instant instant;

	/**
	 * Private constructor for InstantPrice class. Use static factory method of() to create objects of this class
	 *
	 * @param price   {@code BigDecimal} representing the price value of the quote
	 * @param instant {@code Instant} representing the moment in time the price quoted was assumed to be valid
	 */
	private InstantPrice(BigDecimal price, Instant instant) {
		this.price = price;
		this.instant = instant;
	}

	/**
	 * Static factory method for creating InstantPrice objects. The CharSequence date (of which String is a subclass)
	 * understood must represent a valid instant in UTC and is parsed using {@code DateTimeFormatter.ISO_INSTANT}
	 *
	 * @param priceString string representing the price quoted
	 * @param date        charSequence(usually a string) representing the date and time the price quote was assumed to be valid
	 * @return InstantPrice object
	 */
	public static InstantPrice of(String priceString, String date) {
		if (Objects.isNull(priceString)) {
			throw new IllegalArgumentException("priceString may not be null");
		}
		if (Objects.isNull(date)) {
			throw new IllegalArgumentException("date may not be null");
		}
		try {
			BigDecimal price = new BigDecimal(priceString);
			Instant instant = Instant.parse(date);
			return new InstantPrice(price, instant);
		} catch (NumberFormatException | DateTimeParseException ex) {
			LOGGER.error("Attempting to construct an InstantPrice with bad data", ex);
		}
		return InstantPrice.NO_PRICE;
	}

	/**
	 * Static factory method for creating InstantPrice objects. Uses current system clock to populate instance field,
	 * which is probably not best practice and this method may be deprecated without notice. If the consuming API does
	 * not have a timestamp for the quote it should pass in its own clock value into one of the other static factory
	 * methods.
	 *
	 * @param priceString string representing the price value of the quote
	 * @return InstantPrice object
	 */
	public static InstantPrice of(String priceString) {
		if (Objects.isNull(priceString)) {
			throw new IllegalArgumentException("priceString may not be null");
		}
		BigDecimal price = new BigDecimal(priceString);
		return new InstantPrice(price, Instant.now());
	}

	/**
	 * Static factory method for creating InstantPrice objects. The epoch value of the moment the price quoted was
	 * assumed to be valid must be represented in milliseconds. Passing in the epoch time value in seconds will not be
	 * corrected by the method and will break your code.
	 *
	 * @param priceString string representing the price value of the quote
	 * @param epoch       Long representing in milliseconds the epoch value of the moment the price quote was assumed to
	 *                    be valid
	 * @return InstantPrice object
	 */
	public static InstantPrice of(String priceString, Long epoch) {
		if (Objects.isNull(priceString)) {
			throw new IllegalArgumentException("priceString may not be null");
		}
		if (Objects.isNull(epoch)) {
			throw new IllegalArgumentException("epoch may not be null");
		}
		BigDecimal price = new BigDecimal(priceString);
		Instant instant = Instant.ofEpochMilli(epoch);
		return new InstantPrice(price, instant);
	}

	/**
	 * Static factory method for creating InstantPrice objects.
	 *
	 * @param priceString string representing the price value of the quote
	 * @param instant     Instant representing the moment the price quote was assumed to be valid
	 * @return InstantPrice object
	 */
	public static InstantPrice of(String priceString, Instant instant) {
		if (Objects.isNull(priceString)) {
			throw new IllegalArgumentException("priceString may not be null");
		}
		if (Objects.isNull(instant)) {
			throw new IllegalArgumentException("instant may not be null");
		}
		BigDecimal price = new BigDecimal(priceString);
		return new InstantPrice(price, instant);
	}

	/**
	 * Static factory method for creating InstantPrice objects.
	 *
	 * @param price   BigDecimal representing the price value of the quote
	 * @param instant Instant representing the moment the price quote was assumed to be valid
	 * @return InstantPrice object
	 */
	public static InstantPrice of(BigDecimal price, Instant instant) {
		if (Objects.isNull(price)) {
			throw new IllegalArgumentException("price may not be null");
		}
		if (Objects.isNull(instant)) {
			throw new IllegalArgumentException("instant may not be null");
		}
		return new InstantPrice(price, instant);
	}

	/**
	 * Static factory method for creating InstantPrice objects.
	 *
	 * @param priceString string representing the price value of the quote
	 * @param dateString  date and time representing the moment the price quote was assumed to be valid
	 * @param formatter   DateTimeFormatter for converting dateString to LocalDateTime
	 * @param tz          ZoneId for the date and time representing the market time zone where price quote originated
	 * @return InstantPrice object
	 */
	public static InstantPrice of(String priceString, String dateString, DateTimeFormatter formatter, ZoneId tz) {
		if (Objects.isNull(priceString)) {
			throw new IllegalArgumentException("priceString may not be null");
		}
		if (Objects.isNull(dateString)) {
			throw new IllegalArgumentException("dateString may not be null");
		}
		BigDecimal price = new BigDecimal(priceString);
		LocalDateTime ldt = LocalDateTime.parse(dateString, formatter);
		ZonedDateTime zdt = ZonedDateTime.of(ldt, tz);
		return new InstantPrice(price, Instant.from(zdt));
	}

	/**
	 * Static method which uses regex to verify that a String is a valid decimal number. Handy method to use before
	 * attempting to create an InstantPrice object.
	 * Source : http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-a-numeric-type-in-java
	 *
	 * @param stringToCheck string representing a numeric value, usually a price
	 * @return true if the string represents a valid number.
	 */
	public static boolean isNumeric(String stringToCheck) {
		return stringToCheck.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}

	/**
	 * @return BigDecimal representing the price value of the quote
	 */
	public BigDecimal getPrice() {
		return price;
	}

	/**
	 * @return Double representing the price value of the quote
	 */
	public Double getPriceAsDouble() {
		return price.doubleValue();
	}

	/**
	 * @return Instant representing the moment the price quote was assumed to be valid
	 */
	public Instant getInstant() {
		return instant;
	}

	/**
	 * @return Long representing in milliseconds the epoch value of the moment the price quote was assumed to be valid
	 */
	public long getEpoch() {
		return instant.toEpochMilli();
	}
}
