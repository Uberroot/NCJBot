package com.github.uberroot.ncjbot.modapi;

import com.github.uberroot.ncjbot.LocalNode;

/**
 * <p>The abstract class that all NCJBot modules must inherit from.</p>
 * 
 * @author Carter Waxman
 *
 */
public abstract class AbstractModule{
	/**
	 * <p>The running LocalNode instance.</p>
	 */
	protected final LocalNode node;
	
	/**
	 * <p>The name of the class. This is the section name used in the configuration
	 * file for general settings with this module.</p>
	 */
	protected final String name;
	
	/**
	 * <p>Instantiates the module. This should perform initialization that does <b>not</b> require the cooperation of other
	 * modules.</p>
	 * 
	 * @param node The running LocalNode instance.
	 */
	public AbstractModule(LocalNode node) {
		this.node = node;
		this.name = this.getClass().getSimpleName();
	}
	
	/**
	 * <p>Called after all of the modules have been loaded, indicating that this module should install
	 * listeners to NCJBot and other modules as necessary.</p>
	 */
	public abstract void link();
	
	/**
	 * <p>Called when the module needs to remove the listeners that it set in {@link AbstractModule#link()}.</p>
	 */
	public abstract void unlink();
}
