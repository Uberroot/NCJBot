package com.github.uberroot.ncjbot.modapi;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.uberroot.ncjbot.LocalNode;

/**
 * <p>An AbstractClass for modules that actively perform tasks and require their own threadpools.</p>
 *  
 * @author Carter Waxman
 *
 */
public abstract class RunningModule extends AbstractModule implements Runnable{
	
	/**
	 * <p>The thread pool executor assigned to the module.</p>
	 */
	protected final ScheduledThreadPoolExecutor executor;
	

	public RunningModule(LocalNode node) {
		super(node);
		
		//Get the executor for running tasks
		executor = node.getExecutor(node.getConfigManager().getSetting(name, "threadPool", int.class));
	}
	
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

}
