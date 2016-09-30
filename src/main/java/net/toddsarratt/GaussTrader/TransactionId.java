package net.toddsarratt.GaussTrader;

import java.util.Random;

/**
 * Represents a unique ID for tracking Orders and Positions. Upon instantiation calls static method generateNewId()
 * and assigns it to final field id which can be queried with getter method getId(). Ids are generated using System
 * epoch time as the most significant digits, providing sequencing, and nano time in the least significant digits,
 * providing enough precision to attempt to avoid duplicates. The nano time component goes through a bitwise operation
 * against a randomly generated long to provide another layer of uniqueness. Finally the generateNewId() method is
 * synchronized for thread safety.
 */
class TransactionId {
	private static Random random = new Random();
	private final long id;

	TransactionId() {
		this.id = generateNewId();
	}

	/**
	 * Returns bit shifted epoch + (least significant nano seconds bitwise or with a random long and 22 bit bitmask)
	 * to generate unique numbers to use as order and position ids. It looks something like this, in hex:
	 * <p><pre>
	 *     System.currentTimeMillis() = 0x1577cd303c5 (41 significant bits)
	 *         0x15772df8f85 << 22 = 0x55df34c0f1400000 (63 significant bits)
	 *     System.nanoTime() = 0xe6c1a1522fd7
	 *     Random.nextLong() = 0x416ef6b123c83dd4
	 *     (System.nanoTime() | Random.nextLong()) & 0x00000000003FFFFFL = 0x1a3fd7 (21 significant bits)
	 *     0x55df34c0f1400000 | 0x1a3fd7 = 0x55df34c0f15a3fd7
	 * </pre>
	 *
	 * This should work until epoch time equals 0x40000000000ms, at which point shifting left 22 bits will place a "1"
	 * in the 64th bit, creating a negative number. Luckily this won't happen until 5/15/2109.
	 *
	 * @return unique long of 63 significant bits
	 */
	public static synchronized long generateNewId() {
		return (System.currentTimeMillis() << 22) | (System.nanoTime() | random.nextLong() & 0x00000000003FFFFFL);
	}

	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return Long.toString(id);
	}
}
