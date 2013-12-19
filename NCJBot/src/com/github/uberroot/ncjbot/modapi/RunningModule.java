package com.github.uberroot.ncjbot.modapi;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.uberroot.ncjbot.LocalNode;

/**
 * <p>An AbstractClass for modules that actively perform tasks and require their own threadpools.</p>
 *  
 * @author Carter Waxman
 *
 */
//TODO: Module state transitions should throw exceptions on bad states.
public abstract class RunningModule extends AbstractModule implements Runner{
	
	/**
	 * <p>The current state of the module.</p>
	 */
	private RunnerState state;
	
	/**
	 * <p>The thread pool executor assigned to the module.</p>
	 */
	protected final ScheduledThreadPoolExecutor executor;
	
	public RunningModule(LocalNode node) {
		super(node);
		
		//Set the initial state
		state = RunnerState.NEVER_RUN;
		
		//Get the executor for running tasks
		executor = node.getExecutor(node.getConfigManager().getSetting(name, "threadPool", int.class));
	}
	
	/**
	 * <p>Starts the module. This will have no effect unless the module is in the NEVER_RUN or STOPPED state.</p>
	 * @throws Exception 
	 */
	@Override
	public synchronized final void start() throws Exception{
		RunnerState oldState = state;
		if(state == RunnerState.NEVER_RUN || state == RunnerState.STOPPED){
			setState(RunnerState.STARTING);
			try {
				doStart();
			} catch (Exception e) {
				setState(oldState);
				throw e;
			}
			setState(RunnerState.RUNNING);
		}
	}
	
	/**
	 * <p>Pauses the module's task. This will have no effect unless the module is in the RUNNING state.</p>
	 * @throws Exception 
	 */
	@Override
	public synchronized final void pause() throws Exception{
		RunnerState oldState = state;
		if(state == RunnerState.RUNNING){
			setState(RunnerState.PAUSING);
			try {
				doPause();
			} catch (Exception e) {
				setState(oldState);
				throw e;
			}
			setState(RunnerState.PAUSED);
		}
	}
	
	/**
	 * <p>Resumes the module's tasks. This will have no effect unless the module is in the PAUSED state.</p>
	 * @throws Exception 
	 */
	@Override
	public synchronized final void resume() throws Exception{
		RunnerState oldState = state;
		if(state == RunnerState.PAUSED){
			setState(RunnerState.RESUMING);
			try {
				doResume();
			} catch (Exception e) {
				setState(oldState);
				throw e;
			}
			setState(RunnerState.RUNNING);
		}
	}
	
	/**
	 * <p>Stops the module's tasks. This will have no effect unless the module is in the RUNNING or PAUSED.</p>
	 * @throws Exception 
	 */
	@Override
	public synchronized final void stop() throws Exception{
		RunnerState oldState = state;
		if(state == RunnerState.RUNNING || state == RunnerState.PAUSED){
			setState(RunnerState.STOPPING);
			try {
				doStop();
			} catch (Exception e) {
				setState(oldState);
				throw e;
			}
			setState(RunnerState.STOPPED);
		}
	}
	
	/**
	 * <p>Gets the current state of the module.</p>
	 */
	@Override
	public synchronized final RunnerState getState(){
		return state;
	}
	
	/**
	 * <p>Sets the current state to report for the module.</p>
	 * 
	 * @param state The new state.
	 */
	private final void setState(RunnerState state){
		System.out.println(name + ": Switching from " + this.state + " to " + state + " state");
		this.state = state;
	}
	
	/**
	 * <p>Called when the module should start or perform it's primary function. If the module performs a continuous operation,
	 * this method should be used to start the operation on a thread from the thread pool.</p>
	 */
	protected abstract void doStart() throws Exception;
	
	/**
	 * <p>Called when the module needs to suspend it's operations. This method should not return until
	 * the module has fully suspended.</p>
	 */
	protected abstract void doPause() throws Exception;
	
	/**
	 * <p>Called when the module needs to be awakened from a previously paused state.</p>
	 */
	protected abstract void doResume() throws Exception;
	
	/**
	 * <p>Called when the module needs to finish its current tasks so that it can be unloaded.
	 * This method should not return until the module has completely stopped.</p>
	 */
	protected abstract void doStop() throws Exception;
}
