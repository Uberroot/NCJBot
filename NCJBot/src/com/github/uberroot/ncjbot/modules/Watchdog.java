package com.github.uberroot.ncjbot.modules;

import com.github.uberroot.ncjbot.RemoteNode;
import com.github.uberroot.ncjbot.UnsafeObject;

/**
 * <p>This interface defines the functionality required to actively detect node failures.</p>
 *  
 * @author Carter Waxman
 *
 */
//TODO: The beacon and receiver may be split into separate modules
//TODO: This interface may not be needed with the proper system hooks
public interface Watchdog extends UnsafeObject<com.github.uberroot.ncjbot.api.Watchdog> {
	
	/**
	 * <p>Registers a beacon to send to a RemoteNode. The first registration will add the beacon,
	 * with all subsequent registrations incrementing the beacon retain count. A beacon will continue to
	 * operate until its retain count reaches 0.</p>
	 * 
	 * @param rn The node to beacon.
	 */
	public void registerBeacon(RemoteNode rn);
	
	/**
	 * <p>Registers a node to watch with this Watchdog. The first registration will add the watch,
	 * with all subsequent registrations incrementing the watch retain count. A watch will continue to
	 * operate until its retain count reaches 0.</p>
	 * 
	 * @param rn The node to watch.
	 */
	public void registerReceiver(RemoteNode rn);
	
	/**
	 * <p>Decreases the retain count for beaconing a particular node. A beacon will continue to
	 * operate until its retain count reaches 0. </p>
	 * 
	 * @param rn The node being beaconed.
	 */
	public void releaseBeacon(RemoteNode rn);
	
	/**
	 * <p>Decreases the retain count for a watch on a particular node. A watch will continue to
	 * operate until its retain count reaches 0. </p>
	 * 
	 * @param rn The node being watched.
	 */
	public void releaseReceiver(RemoteNode rn);
	
	/**
	 * <p>Indicates that the given node beaconed the current node and is still alive.</p>
	 * 
	 * @param source The source of the beacon.
	 */
	public void beaconed(RemoteNode source);
}
