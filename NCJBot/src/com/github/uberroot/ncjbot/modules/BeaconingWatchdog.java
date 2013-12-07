package com.github.uberroot.ncjbot.modules;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.github.uberroot.ncjbot.LocalNode;
import com.github.uberroot.ncjbot.NodeStateException;
import com.github.uberroot.ncjbot.RemoteNode;
import com.github.uberroot.ncjbot.UnsafeObject;
import com.github.uberroot.ncjbot.modapi.AbstractModule;
import com.github.uberroot.ncjbot.modapi.Watchdog;

/**
 * 
 * <p>This class performs a watchdog functionality to allow the detection of node failure, especially those nodes that are 
 * running jobs. When this node starts a job on another node, it should register the remote node with its watchdog,
 * after which a beacon must be received from the remote node within every 20 seconds. Similarly, the remote node should
 * also register with the watchdog so that it will automatically send a beacon every 10 seconds. In the event of a beacon
 * lapse, the node is assumed to be dead, and all jobs running on this node will be alerted to the failure so that they
 * can recover lost work.</p>
 * 
 * <p>For efficiency, all beacons sent to a particular node are lumped together as a single beacon and all beacon receivers.</p>
 * <b>NOTE THAT THIS FUNCTIONALITY MAY BE AT LEAST PARTIALLY REPLACED WITH A TCP KEEPALIVE IMPLEMENTATION.</b>
 * 
 * @author Carter Waxman
 *
 */
//TODO: It should be possible for the Watchdog to listen for RemoteNodes being added and removed from the OverlayManager
public final class BeaconingWatchdog extends AbstractModule implements Watchdog{
	/**
	 * <p>The ScheduledFuture for handling the beacon timer</p>
	 */
	private ScheduledFuture<?> future;
	
	/**
	 * <p>A table of the times remaining before a node should be beaconed, keyed by the node.</p>
	 */
	private final Hashtable<RemoteNode, Integer> toNotify;
	
	/**
	 * <p>A table of retain counts for beacon registrations, keyed by node.</p>
	 */
	private final Hashtable<RemoteNode, Integer> tnRetCount;
	
	/**
	 * <p>A table of the times remaining before the watchdog for a node lapses, keyed by the node.</p>
	 */
	private final Hashtable<RemoteNode, Integer> expecting;
	
	/**
	 * <p>A table of the retain counts for nodes that should be beaconing, keyed by node.</p>
	 */
	private final Hashtable<RemoteNode, Integer> exRetCount;
	
	/**
	 * <p>Whether the watchdog is running.</p>
	 */
	private boolean running;
	
	/**
	 * <p>Instantiates a new Watchdog.</p>
	 * 
	 * @param node The running LocalNode instance.
	 */
	public BeaconingWatchdog(LocalNode node){
		super(node);
		toNotify = new Hashtable<RemoteNode, Integer>();
		tnRetCount = new Hashtable<RemoteNode, Integer>();
		expecting =  new Hashtable<RemoteNode, Integer>();
		exRetCount =  new Hashtable<RemoteNode, Integer>();
	}
	
	/**
	 * <p>Performs an incremental Watchdog operation. Each time this is called,
	 * the relevant counters will be decremented. Upon reaching 0, the counters will trigger events,
	 * including beaconing, alerting of potential failures, and reporting failures. This remains in its own
	 * method for thread locking purposes.</p> 
	 */
	private synchronized void tick(){
		//Send beacons
		Set<RemoteNode> keys = toNotify.keySet();
		for(RemoteNode rn : keys){
			if(toNotify.get(rn) == 0){
				//Beacon
				try {
					rn.beacon();
				} catch (NodeStateException e) { //TODO: Should this stop trying after so many failures?
					continue; //Try again on next tick
				} catch (IOException e) {
					continue; //Try again on next tick
				}
				
				//Reset
				toNotify.put(rn, 10);
				System.out.println("Beaconed " + rn);
			}
			else
				toNotify.put(rn, toNotify.get(rn) - 1);
		}
		
		//Check Warnings
		keys = expecting.keySet();
		RemoteNode keys2[] = new RemoteNode[keys.size()];
		keys.toArray(keys2);
		for(RemoteNode rn : keys2){
			if(expecting.get(rn) == 0){
				//Announce the failure
				//TODO: This should use the listener model instead of direct calls. The network manager needs to know about this too.
				node.announceNodeFailure(rn);
				
				//Remove from watchdog
				expecting.remove(rn);
				exRetCount.remove(rn);
			}
		}
		
		//Check expecting
		keys = expecting.keySet();
		for(RemoteNode rn : keys){
			if(expecting.get(rn) == 10){
				//Move to warning list
				System.out.println("Potentially unresponsive node: " + rn.getIpAddress().getHostAddress() + ":" + rn.getListeningPort());
			}
			expecting.put(rn, expecting.get(rn) - 1);
		}
	}
	
	/**
	 * <p>Registers a beacon to send to a RemoteNode. The first registration will add the beacon,
	 * with all subsequent registrations incrementing the beacon retain count. A beacon will continue to
	 * operate until its retain count reaches 0.</p>
	 * 
	 * @param rn The node to beacon.
	 */
	public synchronized void registerBeacon(RemoteNode rn){
		if(rn == null)
			return;
		if(!toNotify.keySet().contains(rn)){
			toNotify.put(rn, 10);
			tnRetCount.put(rn, 1);
		}
		else
			tnRetCount.put(rn, tnRetCount.get(rn) + 1);
	}
	
	/**
	 * <p>Registers a node to watch with this Watchdog. The first registration will add the watch,
	 * with all subsequent registrations incrementing the watch retain count. A watch will continue to
	 * operate until its retain count reaches 0.</p>
	 * 
	 * @param rn The node to watch.
	 */
	public synchronized void registerReceiver(RemoteNode rn){
		if(!expecting.containsKey(rn) || !exRetCount.containsKey(rn)){
			expecting.put(rn, 20);
			exRetCount.put(rn, 1);
		}
		else
			exRetCount.put(rn, exRetCount.get(rn) + 1);
	}
	
	/**
	 * <p>Decreases the retain count for beaconing a particular node. A beacon will continue to
	 * operate until its retain count reaches 0. </p>
	 * 
	 * @param rn The node being beaconed.
	 */
	public synchronized void releaseBeacon(RemoteNode rn){
		if(rn == null)
			return;
		tnRetCount.put(rn, tnRetCount.get(rn) - 1);
		if(tnRetCount.get(rn) == 0){
			toNotify.remove(rn);
			tnRetCount.remove(rn);
		}
	}
	
	/**
	 * <p>Decreases the retain count for a watch on a particular node. A watch will continue to
	 * operate until its retain count reaches 0. </p>
	 * 
	 * @param rn The node being watched.
	 */
	public synchronized void releaseReceiver(RemoteNode rn){
		if(!exRetCount.containsKey(rn))
			return;
		exRetCount.put(rn, exRetCount.get(rn) - 1);
		if(exRetCount.get(rn) == 0){
			expecting.remove(rn);
			exRetCount.remove(rn);
		}
	}
	
	/**
	 * <p>Indicates that the given node beaconed the current node and is still alive.</p>
	 * 
	 * @param source The source of the beacon.
	 */
	//TODO: This should be a system hook
	public synchronized void beaconed(RemoteNode source){
		Set<RemoteNode> keys = expecting.keySet();
		for(RemoteNode key : keys){
			if(key.equals(source)){
				expecting.put(source, 20);
				break;
			}
		}
	}
	
	/**
	 * <p>Runs the Watchdog, updating and running the necessary checks every second.</p>
	 */
	@Override
	public synchronized void run(){
		if(future == null)
			future = executor.scheduleAtFixedRate(new Runnable(){
	
				@Override
				public void run() {
					tick();
				}}, 0, 1, TimeUnit.SECONDS);
	}

	@Override
	public com.github.uberroot.ncjbot.api.Watchdog getSafeObject() {
		return new com.github.uberroot.ncjbot.api.Watchdog(this);
	}

	@Override
	public synchronized void link() {
	}

	@Override
	public synchronized void pause() {
		stop();
	}

	@Override
	public synchronized void resume() {
		run();
	}

	@Override
	public synchronized void stop() {
		synchronized(future){
			if(future != null)
				future.cancel(false);
			future = null;
		}
	}

	@Override
	public synchronized void unlink() {		
	}
}
