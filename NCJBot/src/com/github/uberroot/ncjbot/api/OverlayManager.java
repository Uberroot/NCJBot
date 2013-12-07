package com.github.uberroot.ncjbot.api;

import java.util.List;

import com.github.uberroot.ncjbot.RemoteNode;

/**
 * <p>A public-safe wrapper for {@link com.github.uberroot.ncjbot.modapi.OverlayManager}.</p>
 * 
 * @author Carter Waxman
 *
 */
public final class OverlayManager {
	/**
	 * <p>The actual OverlayManager.</p>
	 */
	private final com.github.uberroot.ncjbot.modapi.OverlayManager manager;
	
	/**
	 * <p>Initializes this public-safe OverlayManager with an actual OverlayManager.</p>
	 * 
	 * @param manager The actual OverlayManager.
	 */
	public OverlayManager(com.github.uberroot.ncjbot.modapi.OverlayManager manager){
		this.manager = manager;
	}

	/**
	 * <p>Retrieves a copy of the known active nodes for the network.</p>
	 * 
	 * @return The known active nodes for the network.
	 */
	public List<RemoteNode> getActiveNodes() {
		return manager.getActiveNodes();
	}
	
	/**
	 * <p>Finds a replacement node for the node given. In the event the given node cannot be reached, it will be
	 * removed from the active node list. Otherwise, a different node will be randomly selected and returned.
	 * If there are no nodes available to replace the one provided, this method will return null.</p>
	 * 
	 * @param r The node to replace.
	 * @return A randomly selected node from the active node list.
	 */
	public RemoteNode getReplacement(RemoteNode r){
		return manager.getReplacement(r);
	}
	
	/**
	 * <p>Gets requested number of nodes, wrapping the list around when not enough are known to create a list of unique nodes.</p>
	 * 
	 * @param count The number of nodes to retrieve. A value of -1 indicates the entire node list should be retrieved.
	 * @return A list of <i>count</i> remote nodes.
	 */
	public List<RemoteNode> getNodes(int count){
		return manager.getNodes(count);
	}
}
