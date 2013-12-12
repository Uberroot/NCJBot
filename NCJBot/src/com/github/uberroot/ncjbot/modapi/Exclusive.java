package com.github.uberroot.ncjbot.modapi;

/**
 * <p>A tagging interface to identify interfaces that must be loaded once and only once into NCJBot. Consequently, this
 * interface should never be implemented directly by any class, but instead extended by other interfaces. 
 * For instance, if a module implements an interface, a second module implementing that interface cannot
 * be loaded into NCJBot. No interface or class should extend or implement multiple Exclusive interfaces.</p>
 * 
 * @author Carter Waxman
 * 
 * @param <T> The interface type that must be exclusive. This must be the interface that extends Exclusive to enforce compile-time constraints.
 *
 */
//TODO: These are currently treated as required. These should be separate concepts.
public interface Exclusive<T> {}