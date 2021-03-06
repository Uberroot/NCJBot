package com.github.uberroot.ncjbot.api;
import java.io.File;

import com.github.uberroot.ncjbot.RemoteNode;

/**
 * <p>This abstract class provides the interface between external code and the node. All code to be run by nodes
 * must inherit from this class.</p>
 * 
 * @author Carter Waxman
 *
 */
public abstract class LocalJob implements Runnable{
	/**
	 * <p>The wrapper for this job for handling privileged fields.</p>
	 */
	private final JobEnvironment environMent;
	
	/**
	 * <p>Creates a new instance of the LocalJob subclass. This allows subclasses to perform
	 * basic initialization before performing their main methods.</p>
	 * 
	 * @param initData Initialization parameters for the job.
	 */
	//TODO: The data should be delivered in either a byte array/buffer (abstracting whether the data came from memory or the disk) or a stream
	public LocalJob(File initData){
		environMent = (JobEnvironment) Thread.currentThread();
	}
	
	/**
	 * <p>Gets the JobEnvironment that runs this subclass of LocalJob.</p>
	 * 
	 * @return The JobEnvironment that runs this subclass of LocalJob.
	 */
	public final JobEnvironment getEnvironment(){
		return environMent;
	}
	
	/**
	 * <p>This method is called when new data has be received by the node that has been directed at the LocalJob subclass.</p>
	 * 
	 * @param source The node that send the data/
	 * @param remoteTid The thread ID of the job that sent the data.
	 * @param data The data received.
	 */
	//TODO: The first two parameters should be replaced with a RemoteJob
	//TODO: The data should be delivered in either a byte array/buffer (abstracting whether the data came from memory or the disk) or a stream
	public abstract void dataReceived(RemoteNode source, String remoteTid, File data);
	
	/**
	 * <p>This method is called when a new node has been discovered by this node.</p>
	 * @param rn The node discovered.
	 */
	public abstract void nodeFound(RemoteNode rn);
	
	/**
	 * <p>This method is called when the watchdog notices a lapse in beacons, indicating a node failure. All jobs sent to this
	 * node that have note returned should be assumed lost.</p>
	 * @param rn The node that failed.
	 */
	public abstract void nodeFailed(RemoteNode rn);
}
