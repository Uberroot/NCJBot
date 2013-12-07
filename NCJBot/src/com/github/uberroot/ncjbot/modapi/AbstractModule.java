package com.github.uberroot.ncjbot.modapi;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.uberroot.ncjbot.LocalNode;

/**
 * <p>The abstract class that all NCJBot modules must inherit from.</p>
 * 
 * @author Carter Waxman
 *
 */
//TODO: A distinction should be made between active (thread consuming) and passive modules
public abstract class AbstractModule implements Runnable{
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
	 * <p>The thread pool executor assigned to the module.</p>
	 */
	protected final ScheduledThreadPoolExecutor executor;
	
	/**
	 * <p>Instantiates the module. This should perform initialization that does <b>not</b> require the cooperation of other
	 * modules.</p>
	 * 
	 * @param node The running LocalNode instance.
	 */
	public AbstractModule(LocalNode node) {
		this.node = node;
		this.name = this.getClass().getSimpleName();
		
		//Get the executor for running tasks
		executor = node.getExecutor(Integer.valueOf(node.getConfigManager().getSetting(name, "threadPool")));
	}
	
	/**
	 * <p>Called after all of the modules have been loaded, indicating that this module should install
	 * listeners to NCJBot and other modules as necessary.</p>
	 */
	public abstract void link();
	
	/**
	 * <p>Called when the module should start or perform it's primary function. If the module performs a continuous operation,
	 * this method should be used to start the operation on a thread from the thread pool.</p>
	 */
	@Override
	public abstract void run();
	
	/**
	 * <p>Called when the module needs to suspend it's operations. This method should not return until
	 * the module has fully suspended.</p>
	 */
	public abstract void pause();
	
	/**
	 * <p>Called when the module needs to be awakened from a previously paused state.</p>
	 */
	public abstract void resume();
	
	/**
	 * <p>Called when the module needs to finish its current tasks so that it can be unloaded.
	 * This method should not return until the module has completely stopped.</p>
	 */
	public abstract void stop();
	
	/**
	 * <p>Called when the module needs to remove the listeners that it set in {@link AbstractModule#link()}.</p>
	 */
	public abstract void unlink();

}
