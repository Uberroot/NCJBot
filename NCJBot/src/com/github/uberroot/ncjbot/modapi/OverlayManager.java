package com.github.uberroot.ncjbot.modapi;

import java.util.List;
import com.github.uberroot.ncjbot.RemoteNode;
import com.github.uberroot.ncjbot.UnsafeObject;

/**
 * <p>An interface for modules that manage the overlay network. Classes implementing this interface are responsible for
 * maintaining the associations between this node and other nodes in the network, as well as monitoring which nodes are
 * available for use.</p>
 * 
 * @author Carter Waxman
 *
 */
public interface OverlayManager extends Exclusive<OverlayManager>, UnsafeObject<com.github.uberroot.ncjbot.api.OverlayManager>{
	/**
	 * <p>Retrieves a copy of the known active nodes for the network.</p>
	 * 
	 * @return The known active nodes for the network.
	 */
	//TODO: This should return unmodifiable RemoteNodes
	public List<RemoteNode> getActiveNodes();
	
	/**
	 * <p>Directly adds a new node to the OverlayManager.</p>
	 * 
	 * @param rn the node to add
	 * @return True if the node was an addition to the OverlayManager, false if the node was already known.
	 */
	public boolean addDiscoveredNode(RemoteNode rn);
	
	/**
	 * <p>Finds a randomly selected replacement node for the node given.
	 * If there are no nodes available to replace the one provided, this method will return null.</p>
	 * 
	 * @param r The node to replace.
	 * @return A randomly selected node from the OverlayManager.
	 */
	//TODO: This should return unmodifiable RemoteNodes
	public RemoteNode getReplacement(RemoteNode r);
	
	/**
	 * <p>Gets requested number of nodes, wrapping the list around when not enough are known to create a list of unique nodes.</p>
	 * 
	 * @param count The number of nodes to retrieve. A value of -1 indicates that all nodes should be retrieved.
	 * @return A list of <i>count</i> remote nodes.
	 */
	//TODO: This should return unmodifiable RemoteNodes
	public List<RemoteNode> getNodes(int count);
}
