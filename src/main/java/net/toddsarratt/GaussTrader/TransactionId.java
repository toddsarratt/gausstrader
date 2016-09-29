package net.toddsarratt.GaussTrader;

/**
 * Represents a transaction ID for tracking Orders and Positions. Upon instantiation calls static method generateNewId()
 * and assigns it to final field id which can be accessed with a getter method getId(). Ids are generated using System
 * epoch time in the most significant digits, providing sequencing, and nano time in the least significant digits,
 * providing enough precision to avoid duplicates.
 * <p>
 * TODO: Make this thread safe so there is no chance of duplicate IDs.
 */
class TransactionId {
	private final long id;

	TransactionId() {
		this.id = generateNewId();
	}

	/**
	 * Returns current epoch time + least significant nano seconds to generate unique order and position ids.
	 * In hex it looks something like this:
	 * <p><pre>
	 *     System.currentTimeMillis() = 15772df8f85 (44 bits)
	 *         15772df8f85 << 20 = 15772df8f8500000 (64 bits)
	 *     System.nanoTime() = 4eea90b4e42b
	 *         4eea90b4e42b & 0x00000000000FFFFFL = 4e42b
	 *     15772df8f8500000 | 4e42b = 15772df8f854e42b
	 * </pre>
	 *
	 * @return unique long of 64 significant bits
	 */
	static long generateNewId() {
		return (System.currentTimeMillis() << 20) | (System.nanoTime() & 0x00000000000FFFFFL);
	}

	public long getId() {
		return id;
	}
}
