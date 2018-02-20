package net.toddsarratt.gaussTrader;


import java.util.concurrent.Flow.Publisher;

/**
 * Interface for carrying out a particular trading strategy.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */

public interface TradingStrategy<T> extends Runnable, Publisher<T> {}
