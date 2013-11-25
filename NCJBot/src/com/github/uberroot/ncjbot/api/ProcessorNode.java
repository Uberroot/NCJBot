package com.github.uberroot.ncjbot.api;

import com.github.uberroot.ncjbot.NodeState;

/**
 * <p>A public-safe wrapper for {@link com.github.uberroot.ncjbot.ProcessorNode}.</p>
 * 
 * @author Carter Waxman
 *
 */
public final class ProcessorNode {
	/**
	 * <p>The actual ProcessorNode.</p>
	 */
	private com.github.uberroot.ncjbot.ProcessorNode node;
	
	/**
	 * <p>Initializes this public-safe ProcessorNode with an actual ProcessorNode.</p>
	 * 
	 * @param node The actual ProcessorNode.
	 */
	public ProcessorNode(com.github.uberroot.ncjbot.ProcessorNode node){
		this.node = node;
	}
	
	/**
	 * <p>Gets the current status of the node.</p>

	 * @return The current status of the node.
	 */
	public NodeState getState(){
		return node.getState();
	}
	
	/**
	 * <p>Gets the current port on which the server socket should listen.</p>
	 * 
	 * @deprecated This should not be accessable to non-privalged jobs as it breaks the abstraction between the network and job.
	 * @return The current port on which the server socket should listen.
	 */
	public int getListenPort(){
		return node.getListenPort();
	}
	
	/**
	 * <p>Retrieves the running NetworkManager for this node.</p>
	 * 
	 * @return The running NetworkManager for this node.
	 */
	public NetworkManager getNetworkManager(){
		return node.getNetworkManager().getSafeObject();
	}
	
	/**
	 * <p>Retrieves the running Watchdog for this node.</p>
	 * 
	 * @return The running Watchdog for this node.
	 */
	public Watchdog getWatchdog(){
		return node.getWatchdog().getSafeObject();
	}
}
