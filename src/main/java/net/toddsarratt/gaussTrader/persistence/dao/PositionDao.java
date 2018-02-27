package net.toddsarratt.gaussTrader.persistence.dao;

import net.toddsarratt.gaussTrader.persistence.entity.Position;

public class PositionDao implements GenericDao<Position, Long> {

	public PositionDao() {}

	/**
	 * Persist the newInstance object into database
	 *
	 * @param newInstance
	 */
	@Override
	public Long create(Position newInstance) {
		return null;
	}

	/**
	 * Retrieve an object that was previously persisted to the database using
	 * the indicated id as primary key
	 *
	 * @param id
	 */
	@Override
	public Position read(Long id) {
		return null;
	}

	/**
	 * Save changes made to a persistent object.
	 *
	 * @param transientObject
	 */
	@Override
	public void update(Position transientObject) {

	}

	/**
	 * Remove an object from persistent storage in the database
	 *
	 * @param persistentObject
	 */
	@Override
	public void delete(Position persistentObject) {

	}
}
