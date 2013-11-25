package com.github.uberroot.ncjbot;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * <p>This class keeps track of other nodes within the network. The nodes within a network only know of each
 * others status through direct communication. Network updates are not currently relayed to other nodes; rather a node
 * figures out the status of a remote node when it needs something from that remote node. The network manager maintains a timer
 * to rebroadcast its presence to the rest of the network. This allows the network to recover this node
 * in the event a connection error resulted in this node being removed from the active list of other nodes.
 * Currently, this timer is set to announce the presence of the node once per hour (non-configurable).</p>
 * 
 * <p>Currently, there is little functionality regarding monitoring node network health; remote node health is implemented using a lazy algorithm.
 * Nodes alert all other nodes when they join the network. Any time one node needs something from another node, it will try to connect. If
 * it is unsuccessful, it will be assumed to be down. This only changes the internal state of the requesting node; other nodes will need to
 * learn of such failures on their own. In order to remedy situations in which nodes are falsely assumed down (i.e. due to an unstable network),
 * all nodes broadcast their presence to all nodes known.</p>
 * <br>
 * 
 * <b>NOTE: THIS NEXT SECTION HAS NOT YET BEEN IMPLEMENTED / MAY NOT BE IMPLEMENTED AS DESCRIBED</b>
 * <p>The NetworkManager allows for two modes of operation: "Full" and "Gossip".</p>
 * 
 * <p>In full mode, this node will establish its presence with ALL other nodes. All network change messages will be broadcast
 * to the entire network. Each node running under full mode that receives a change message will either repeat the message to
 * the rest of the network, or discard the message if it is already aware of the change (preventing a flood).
 * This mode of operation is intended for smaller networks, as it can create a great deal of traffic. The primary
 * objective of this mode is to create a minimal delay between the time a network change occurs and the time
 * the entire network compensates for the event. This mode may not be implemented.</p>
 * 
 * <p>Under gossip mode, the initial presence announcement will only be made to a random subset of nodes, the number of which is configurable (or may be fixed at 2).
 * Message broadcasts will then be relayed to a random subset of nodes within the network, the size of which is configurable (or may be fixed at 2).
 * Each receiving node running in gossip mode will then relay the message to another random subset of the network,
 * or discard the message if it is already aware of the event. A more structured implementation may be used to form links between the
 * nodes that are always used rather than select a new random subset on each relay, creating a more consistent network structure.
 * A variation of this mode will likely be implemented.</p>
 * 
 * 
 * <p>Under either mode, messages are relayed at a configurable rate. Propagation may either stop after a TTL expiration or utilize an event resolution technique that may work as follows:
 * <ul>
 * 	<li>If a node receives information on a node and has never received the type of data before, assume it's correct and propagate.</li>
 * 	<li>If a node receives information on a node and has identical information, assume it's correct and ignore.</li>
 * 	<li>If a node receives information on a node and the information conflicts with current information and the new information has a later timestamp, assume it's correct and propagate.</li>
 * 	<li>If a node receives information on a node and the information conflicts with current information and the new information has an earlier timestamp, assume it's incorrect and propagate the correct information only to the node that sent the incorrect information.</li> 
 * </ul>
 * A method for synchronization still needs to be developed.</p>
 * 
 * @author Carter Waxman
 *
 */
//TODO: This should be a singleton class
//TODO: This functionality will be moved away from it's own thread and into centralized timing
//TODO: This shouldn't be a Thread subclass as it exposes public methods through thread enumeration
//TODO: All of these methods need the synchronized specifier (Must not be a thread subclass). This will make EventListener logic less complex (requires testing)
//TODO: There should be a method for internal removal of nodes
public final class NetworkManager extends Thread implements UnsafeObject<com.github.uberroot.ncjbot.api.NetworkManager>, RemoteNode.EventListener{
	/**
	 * <p>The running ProcessorNode instance.</p>
	 */
	private ProcessorNode node;
	
	/**
	 * <p>The list of all nodes known to be active on the network.</p>
	 */
	private ArrayList<RemoteNode> activeNodes;
	
	
	/**
	 * <p>Initializes the NetworkManager with the given seed nodes. Each of the seed nodes will be contacted and queried for a
	 * list of known nodes. The results of the queries will be combined into the active node list, including those
	 * seeds that responded.</p>
	 * 
	 * @param node The running ProcessorNode instance.
	 * @param seedNodes The initial list of nodes to query. These should be considered the entry points for the network.
	 */
	public NetworkManager(ProcessorNode node, ArrayList<RemoteNode> seedNodes){
		this.node = node;
		activeNodes = new ArrayList<RemoteNode>();
		
		//Query seed nodes for node lists
		System.out.println("Attempting to connect to seed nodes...");
		for(int i = 0; i < seedNodes.size(); i++){
			RemoteNode n = seedNodes.get(i);
			
			System.out.print("Attempting " + n.getIpAddress() + ":" + n.getListeningPort() + "...\t");
			List<RemoteNode> l = null;
			try{
				l = n.getKnownNodes();
			}
			catch(ConnectException e){
				//Unable to connect to seed node. Don't add it.
				continue;
			} catch (IOException e) {
				//Communication is not reliable. Ignore this node.
				continue;
			} catch (NodeStateException e) {
				switch(e.getState()){
					case SHUTTING_DOWN:{
						//Don't add it, it won't be useful
						break;
					}
					case RUNNING:
					case UNKNOWN:{
						//Unknown or running. Either way, track it.
						activeNodes.add(n);
						
						//Register as the RemoteNode.EventListener
						n.addEventListener(this);
						break;
					}
				}
				continue;
			}
			
			//No exceptions, add the seed node
			activeNodes.add(n);
			
			//Add the retrieved nodes
			for(RemoteNode n1 : l)
				if(!activeNodes.contains(n1)){
					activeNodes.add(n1);
					node.announceFoundNode(n1);
					
					//Register as the RemoteNode.EventListener
					n1.addEventListener(this);
				}
			
			//Register as the RemoteNode.EventListener
			n.addEventListener(this);
		}
		
		//Are there nodes?
		if(activeNodes.size() == 0)
			System.out.println("No active nodes found. Starting as lone node.");
		
		setName("Network Manager");
	}
	
	/**
	 * <p>This method runs an hourly beacon for alerting nodes in the network of this node's presence. </p>
	 * <b>THIS IMPLEMENTATION WILL BE REPLACED WITH A CALLBACK-BASED CENTRALIZED TIMING THREAD.</b>
	 */
	@Override
	public void run(){
		int minute = 0;
		while(true){
			try {
				//Run once per hour
				if(minute % 60 == 0){
					if(minute >= 60)
						minute -= 60;
					System.out.println("Announcing presence...");
					for(int i = 0; i < activeNodes.size(); i++){
						RemoteNode n = activeNodes.get(i);
						try {
							//Temporarily remove self from the listener.
							//TODO: This is to fix synchronization issues.
							n.removeEventListener(this);
							n.beacon();
							//TODO: This is to fix synchronization issues.
							n.addEventListener(this);
						}
						catch (ConnectException e) { //Could not connect
							//If here, either host doesn't exist, or is not listening on the port
							System.out.println("Unable to connect");
							node.announceNodeFailure(activeNodes.remove(i--));
						}
						catch (IOException e) {
							//If here, communication is not reliable
							System.out.println("Unreliable");
							node.announceNodeFailure(activeNodes.remove(i--));
						}
						catch (NodeStateException e) {
							switch(e.getState()){
								case SHUTTING_DOWN:{
									//TODO: This isn't a failure, but should this be announced via ProcessorNode?
									activeNodes.remove(i--);
									break;
								}
								case RUNNING:
								case UNKNOWN:{
									//TODO: This isn't a failure, but should this be announced via ProcessorNode?
									//System.out.println("Unknown node state: " + String.valueOf(buffer).trim());
									break;
								}
							}
						}
					}
				}
				Thread.sleep(60000); //Wait a minute
			} catch (InterruptedException e) { //This is used to kill the thread by calling interrupt().
				break;
			}
			minute++; //TODO: This minute counter is a relic of an older implementation, but will be included in a new one so it remains.
		}
		System.out.println("The network manager has stopped");
	}

	/**
	 * <p>Retrieves a copy of the known active nodes for the network.</p>
	 * 
	 * @return The known active nodes for the network.
	 */
	//TODO: Randomization for load balancing (this could be done in getNodes() instead)
	//TODO: This should return unmodifiable RemoteNodes
	public List<RemoteNode> getActiveNodes() {
		return Collections.unmodifiableList(activeNodes);
	}
	
	/**
	 * <p>Directly adds a new node to the active node list. If the node was not previously known,
	 * its presence will be announced to all running jobs and services of this node.</p>
	 * 
	 * @param rn the node to add
	 * @return True if the node was an addition to the list, false if the node was already known.
	 */
	public boolean addDiscoveredNode(RemoteNode rn){
		if(!activeNodes.contains(rn)){
			activeNodes.add(rn);
			rn.addEventListener(this);
			node.announceFoundNode(rn);
			return true;
		}
		return false;
	}
	
	/**
	 * <p>Finds a replacement node for the node given. In the event the given node cannot be reached, it will be
	 * removed from the active node list. Otherwise, a different node will be randomly selected and returned.
	 * If there are no nodes available to replace the one provided, this method will return null.</p>
	 * 
	 * @param r The node to replace.
	 * @return A randomly selected node from the active node list.
	 */
	//TODO: This should return unmodifiable RemoteNodes
	public RemoteNode getReplacement(RemoteNode r){
		//See if r is offline
		//TODO: This is to fix synchronization issues.
		r.removeEventListener(this);
		if(!r.canConnect()){
			//If here, either host doesn't exist, or is not listening on the port
			System.out.println("Removing unresponsive node");
			activeNodes.remove(r);
		}
		//TODO: This is to fix synchronization issues.
		r.addEventListener(this);
		
		//Select a new node
		Random rand = new Random();
		ArrayList<RemoteNode> pool = new ArrayList<RemoteNode>(activeNodes);
		pool.remove(r);
		if(pool.size() > 0)
			return pool.get(rand.nextInt(pool.size()));
		else
			return null;
	}
	
	/**
	 * <p>Gets requested number of nodes, wrapping the list around when not enough are known to create a list of unique nodes.</p>
	 * 
	 * @param count The number of nodes to retrieve. A value of -1 indicates the entire node list should be retrieved.
	 * @return A list of <i>count</i> remote nodes.
	 */
	//TODO: This should return unmodifiable RemoteNodes
	public List<RemoteNode> getNodes(int count){
		if(count == -1)
			return getActiveNodes();
		List<RemoteNode> ans = getActiveNodes();
		ArrayList<RemoteNode> ret = new ArrayList<RemoteNode>();
		if(ans.size() == 0){
			RemoteNode self = null;
			try {
				self = new RemoteNode(node, "127.0.0.1", node.getListenPort());
			} catch (UnknownHostException e) {
				//THIS WILL NEVER HAPPEN
				node.quit();
			}
			for(int i = 0; i < count; i++)
				ret.add(self);
		}
		else{
			for(int i = 0, added = 0; added < count; i++, added++){
				if(i >= ans.size()){
					i = -1;
					try {
						ret.add(new RemoteNode(node, "127.0.0.1", node.getListenPort()));
					} catch (UnknownHostException e) {
						//THIS WILL NEVER HAPPEN
						node.quit();
					}
					continue;
				}
				ret.add(ans.get(i));
			}
		}
		return Collections.unmodifiableList(ret);
	}

	@Override
	public com.github.uberroot.ncjbot.api.NetworkManager getSafeObject() {
		return new com.github.uberroot.ncjbot.api.NetworkManager(this);
	}

	@Override
	//TODO: Handling of these state changes should be re-examined.
	public void nodeStateChanged(RemoteNode node, NodeState state) {
		switch(state){
			case SHUTTING_DOWN:
				node.removeEventListener(this);
				activeNodes.remove(node);
				break;
			case RUNNING:
			case UNKNOWN:
			default:
				break;
		}
	}

	@Override
	public void nodeConnectionFailed(RemoteNode node) {
		// TODO A connection error could be indicative of a node changing IP / Port numbers. Should there be a grace period before removing? (Shouldn't matter until node ID's are implemented)
		node.removeEventListener(this);
		activeNodes.remove(node);
	}
}
