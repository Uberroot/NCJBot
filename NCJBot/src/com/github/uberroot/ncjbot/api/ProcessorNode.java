package com.github.uberroot.ncjbot.api;

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
	 * <p>Gets the current status string of the node.</p>
	 * <b>Valid Values:</b>
	 * <ul>
	 * <li><b>I'm not dead yet.</b> The node is active.</li>
	 * <li><b>I'm bleeding out.</b> The node is shutting down.</li>
	 * </ul>
	 * @return The current status string of the node.
	 */
	public String getStatus(){
		return node.getStatus();
	}
	
	/**
	 * <p>Gets the current port on which the server socket should listen.</p>
	 * 
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
