package com.github.uberroot.ncjbot;

/**
 * <p>A enumeration type for the states of nodes.</p>
 * 
 * @author Carter Waxman
 *
 */
public enum NodeState{
	/**
	 * <p>The node is currently running and accepting jobs.</p>
	 */
	RUNNING,
	
	/**
	 * <p>The node is in the process of shutting down. It will finish current activities, but it will not accept new tasks.</p>
	 */
	SHUTTING_DOWN,
	
	/**
	 * <p>The node is in an unknown state.</p>
	 */
	UNKNOWN
}