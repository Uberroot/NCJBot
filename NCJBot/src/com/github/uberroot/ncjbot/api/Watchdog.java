package com.github.uberroot.ncjbot.api;

import com.github.uberroot.ncjbot.RemoteNode;

/**
 * <p>A public-safe wrapper for {@link com.github.uberroot.ncjbot.modapi.Watchdog}.</p>
 * 
 * @deprecated This entire class will be removed from the public API and it's functionality will be provided automatically.
 * @author Carter Waxman
 *
 */
public final class Watchdog {
	
	/**
	 * <p>The actual Watchdog.</p>
	 */
	private com.github.uberroot.ncjbot.modapi.Watchdog watchdog;
	
	/**
	 * <p>Initializes this public-safe Watchdog with an actual Watchdog.</p>
	 * 
	 * @param node The actual Watchdog.
	 */
	public Watchdog(com.github.uberroot.ncjbot.modapi.Watchdog watchdog){
		this.watchdog = watchdog;
	}
	
	/**
	 * <p>Registers a node to watch with this Watchdog. The first registration will add the watch,
	 * with all subsequent registrations incrementing the watch retain count. A watch will continue to
	 * operate until its retain count reaches 0.</p>
	 * 
	 * @param rn The node to watch.
	 */
	public void registerReceiver(RemoteNode rn){
		watchdog.registerReceiver(rn);
	}
	
	/**
	 * <p>Decreases the retain count for a watch on a particular node. A watch will continue to
	 * operate until its retain count reaches 0. </p>
	 * 
	 * @param rn The node being watched.
	 */
	public void releaseReceiver(RemoteNode rn){
		watchdog.releaseReceiver(rn);
	}
}
