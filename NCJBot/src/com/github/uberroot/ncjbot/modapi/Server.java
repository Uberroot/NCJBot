package com.github.uberroot.ncjbot.modapi;

/**
 * <p>This interface defines the behavior required to implement a server for handling and dispatching network connection requests.</p>
 * 
 * @author Carter Waxman
 *
 */
public interface Server extends Runner, Exclusive<Server>{
	/**
	 * <p>Sets the listening port for the server. The server must restart for this to take effect.</p>
	 * 
	 * @param port The new port number.
	 * @param immediate True to restart the server immediately, false to restart it manually at a later time.
	 */
	public void setPort(int port, boolean immediate);
	
	/**
	 * <p>Gets the port the server is configured to use. This may be different from the actual listening port
	 * if a new port is set without restarting the server.</p>
	 * 
	 * @return The configured listening port.
	 */
	public int getConfiguredPort();
	
	/**
	 * <p>Gets the port that the server is currently using.</p>
	 * 
	 * @return The current listening port or -1 if the server is not running.
	 */
	public int getCurrentPort();
	
	/**
	 * <p>Determines whether the server is currently listening for connections.</p>
	 * 
	 * @return True if the server is running.
	 */
	@Override
	public RunnerState getState();
}
