package com.github.uberroot.ncjbot.api;

import com.github.uberroot.ncjbot.NodeState;

/**
 * <p>A public-safe wrapper for {@link com.github.uberroot.ncjbot.LocalNode}.</p>
 * 
 * @author Carter Waxman
 *
 */
public final class LocalNode {
	/**
	 * <p>The actual LocalNode.</p>
	 */
	private com.github.uberroot.ncjbot.LocalNode node;
	
	/**
	 * <p>Initializes this public-safe LocalNode with an actual LocalNode.</p>
	 * 
	 * @param node The actual LocalNode.
	 */
	public LocalNode(com.github.uberroot.ncjbot.LocalNode node){
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
	 * <p>Retrieves the running OverlayManager for this node.</p>
	 * 
	 * @return The running OverlayManager for this node.
	 */
	public OverlayManager getOverlayManager(){
		return node.getOverlayManager().getSafeObject();
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
