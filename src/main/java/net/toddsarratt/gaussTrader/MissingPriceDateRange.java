package net.toddsarratt.gaussTrader;

import java.time.LocalDate;

public class MissingPriceDateRange {
	private final LocalDate latest;
	private final LocalDate earliest;

	MissingPriceDateRange(final LocalDate latest, final LocalDate earliest) {
		this.latest = latest;
		this.earliest = earliest;
	}

	public LocalDate getLatest() {
		return latest;
	}

	public LocalDate getEarliest() {
		return earliest;
	}
}
