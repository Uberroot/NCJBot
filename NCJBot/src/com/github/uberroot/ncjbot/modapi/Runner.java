package com.github.uberroot.ncjbot.modapi;

/**
 * <p>This interface defines the behavior for objects that run tasks. Runner implementors must follow a specific lifecycle
 * to ensure consistency throughout a system.</p>
 * 
 * <p>When first loaded (instantiated), a Runner should start in the NEVER_RUN state. While preparing to start a task,
 * a Runner should enter the STARTING state until it finishes its start process. After starting, the Runner should then enter the
 * RUNNING state.</p>
 * 
 * <p>Once a Runner is RUNNING, it can either be paused or stopped. Pausing a node should only prevent future work on a task,
 * while stopping a node should finalize the running task. As with the startup sequence, a Runner entering the PAUSED
 * or STOPPED state should first enter the PAUSING or STOPPING state until it has actually paused or stopped its tasks.</p>
 * 
 * <p>As with the RUNNING state, a PAUSED Runner can also be stopped, following the same STOPPING to STOPPED transition.
 * A PAUSED Runner should resume by following a similar state sequence, transitioning to the RESUMING state and finally to the RUNNING state.</p>
 * 
 * <p>Summary of valid state transitions:
 * <ul>
 * 	<li>NEVER_RUN &rarr; STARTING</li>
 * 	<li>STARTING &rarr; RUNNING</li>
 * 	<li>RUNNING &rarr; PAUSING, STOPPING</li>
 * 	<li>PAUSING &rarr; PAUSED</li>
 * 	<li>PAUSED &rarr; RESUMING, STOPPING</li>
 * 	<li>RESUMING &rarr; RUNNING</li>
 * 	<li>STOPPING &rarr; STOPPED</li>
 * 	<li>STOPPED &rarr; STARTING</li>
 * </ul></p>
 * <p>Note that as an exception to the above rules, if a Runner enters a transition state (STARTING, PAUSING, RESUMING, STOPPING) and
 * fails during the process, it should revert back to its original state.</p>
 * 
 * @author Carter Waxman
 *
 */
public interface Runner {
	/**
	 * <p>Defines the possible states for a Runner.</p>
	 * 
	 * @author Carter Waxman
	 *
	 */
	public enum RunnerState{ //TODO: Use this more universally
		/**
		 * <p>Indicates the Runner is not running and has never been run.</p>
		 */
		NEVER_RUN,
		
		/**
		 * <p>Indicates the Runner is in transition from a NEVER_RUN or STOPPED state to a RUNNING state.</p>
		 */
		STARTING,
		
		/**
		 * <p>Indicates the Runner is currently running it's task.</p>
		 */
		RUNNING,
		
		/**
		 * <p>Indicates the Runner is in transition from a RUNNING state to a PAUSED state.</p>
		 */
		PAUSING,
		
		/**
		 * <p>Indicates the Runner is currently paused, with the possibility of stopping or resuming.</p>
		 */
		PAUSED,
		
		/**
		 * <p>Indicates the Runner is in transition from a PAUSED state to a RUNNING state.</p>
		 */
		RESUMING,
		
		/**
		 * <p>Indicates the Runner is in transition from a RUNNING or PAUSED state to a STOPPED state.</p>
		 */
		STOPPING,
		
		/**
		 * <p>Indicates the Runner is no longer running its tasks.</p>
		 */
		STOPPED
	}
	
	/**
	 * <p>Starts the task for this Runner.</p>
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception;
	
	/**
	 * <p>Pauses the task with the assumption that it will be resumed or stopped some time in the future.</p>
	 * 
	 * @throws Exception
	 */
	public void pause() throws Exception;
	
	/**
	 * <p>Resumes the previously paused task.</p>
	 * 
	 * @throws Exception
	 */
	public void resume() throws Exception;
	
	/**
	 * <p>Completely stops the task for this Runner.</p>
	 * 
	 * @throws Exception
	 */
	public void stop() throws Exception;
	
	/**
	 * <p>Gets the current state for the Runner.</p>
	 * 
	 * @return the current state for the Runner.
	 */
	public RunnerState getState();
}
