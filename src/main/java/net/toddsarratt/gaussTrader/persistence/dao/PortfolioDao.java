package net.toddsarratt.gaussTrader.persistence.dao;

import net.toddsarratt.gaussTrader.persistence.entity.Portfolio;

public class PortfolioDao implements GenericDao<Portfolio, String> {

	public PortfolioDao() {}

	/**
	 * Persist the newInstance object into database
	 *
	 * @param newInstance
	 */
	@Override
	public String create(Portfolio newInstance) {
		return null;
	}

	/**
	 * Retrieve an Portfolio that was previously persisted to the database using
	 * the indicated id as primary key
	 *
	 * @param id
	 */
	@Override
	public Portfolio read(String id) {
		return null;
	}

	/**
	 * Save changes made to a persistent object.
	 *
	 * @param transientObject
	 */
	@Override
	public void update(Portfolio transientObject) {

	}

	/**
	 * Remove an object from persistent storage in the database
	 *
	 * @param persistentObject
	 */
	@Override
	public void delete(Portfolio persistentObject) {

	}
}
