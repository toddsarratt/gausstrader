package net.toddsarratt.gaussTrader;

import net.toddsarratt.gaussTrader.persistence.entity.Order;
import net.toddsarratt.gaussTrader.persistence.entity.Position;

import java.util.Random;

/**
 * Generates a unique ID for {@link Order} and {@link Position} objects. Static method generateNewId() uses System
 * epoch time as the most significant digits, providing sequencing, and a randomly generated long to provide uniqueness.
 * The generateNewId() method is synchronized for thread safety.
 */
public class TransactionId {

	private static Random random = new Random();

	/* Thou shall not instantiate. */
	TransactionId() {
	}

	/**
	 * Returns bit shifted epoch + (random int and 22 bit bitmask) to generate unique numbers to use as order and
	 * position ids. Example:
	 * <p><pre>
	 *     System.currentTimeMillis() = 0x15781ecc60e (41 significant bits)
	 *         0x15772df8f85 << 22 = 0x55e07b3183800000 (63 significant bits)
	 *     Random.nextInt() = 0xa93e5d86
	 *     Random.nextInt() & 0x003FFFFF = 0x3e5d86 (22 significant bits)
	 *     0x55e07b3183800000 | 0x3e5d86 = 0x55e07b3183be5d86
	 * </pre>
	 * <p>
	 * This should work until epoch time equals 0x40000000000ms, at which point shifting left 22 bits will place a "1"
	 * in the 64th bit, creating a negative number. This will happen some time on 5/15/2109.
	 *
	 * @return unique Long of 63 significant bits
	 */
	public static synchronized Long generateNewId() {
		return (System.currentTimeMillis() << 22) | (random.nextInt() & 0x003FFFFF);
	}
}
