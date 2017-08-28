package net.toddsarratt.GaussTrader;

import java.util.Random;

/**
 * Represents a unique ID for tracking Orders and Positions. Upon instantiation calls static method generateNewId()
 * and assigns it to final field id which can be queried with getter method getId(). Ids are generated using System
 * epoch time as the most significant digits, providing sequencing, and a randomly generated long to provide uniqueness.
 * The generateNewId() method is synchronized for thread safety.
 */
class TransactionId {
	private static Random random = new Random();
	private final long id;

	TransactionId() {
		this.id = generateNewId();
	}

	TransactionId(long id) {
		this.id = id;
	}

	/**
	 * Returns bit shifted epoch + (random int and 22 bit bitmask) to generate unique numbers to use as order and
	 * position ids. It looks something like this, in hex:
	 * <p><pre>
	 *     System.currentTimeMillis() = 0x15781ecc60e (41 significant bits)
	 *         0x15772df8f85 << 22 = 0x55e07b3183800000 (63 significant bits)
	 *     Random.nextInt() = 0xa93e5d86
	 *     Random.nextInt() & 0x003FFFFF = 0x3e5d86 (22 significant bits)
	 *     0x55e07b3183800000 | 0x3e5d86 = 0x55e07b3183be5d86
	 * </pre>
	 *
	 * This should work until epoch time equals 0x40000000000ms, at which point shifting left 22 bits will place a "1"
	 * in the 64th bit, creating a negative number. Luckily this won't happen until 5/15/2109.
	 *
	 * @return unique long of 63 significant bits
	 */
	public static synchronized long generateNewId() {
		return (System.currentTimeMillis() << 22) | (random.nextInt() & 0x003FFFFF);
	}

	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return Long.toString(id);
	}
}
