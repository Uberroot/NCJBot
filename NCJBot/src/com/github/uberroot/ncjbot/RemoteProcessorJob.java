package com.github.uberroot.ncjbot;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import com.github.uberroot.ncjbot.api.ProcessorJob;
import com.github.uberroot.ncjbot.api.ProcessorJobEnvironment;

/**
 * <p>This class provides access to ProcessorJobs running on remote nodes.</p>
 * 
 * <b>THE FOLLOWING HAS NOT YET BEEN IMPLEMENTED</b>
 * <p>By default, a ProcessorJob will only be allowed to communicate with its direct parent and its direct children.
 * Additional communication allowances can be specified by the receiving jobs. A suggested model for providing communication
 * between peer jobs would be to first dispatch child jobs to the other nodes, send the full list of peer jobs to the children,
 * and have the children register the peers as jobs authorized for interaction.</p>
 * 
 * @author Carter Waxman
 *
 */
public final class RemoteProcessorJob {
	/**
	 * <p>The running ProcessorNode instance.</p>
	 */
	private ProcessorNode node;
	
	/**
	 * <p>The remote node that is running the job.</p>
	 */
	//TODO: This may need to be mutable to implement replication-based failover.
	//TODO: This will probably be renamed
	private final RemoteNode source; //TODO: Make sure source data is immutable.
	
	/**
	 * <p>The thread id of the remote job.</p>
	 */
	//TODO: This may need to be mutable to implement replication-based failover.
	private final String remoteTid; //TODO: Will this be affected by using a thread pool for jobs?
	
	/**
	 * <p>Instantiates a new RemoteProcessorJob with the node running the job, as well as the thread id of the running job.</p>
	 * 
	 * @param node The running ProcessorNode instance.
	 * @param source The RemoteNode that is running the job.
	 * @param remoteTid The thread id of the remote job.
	 */
	public RemoteProcessorJob(ProcessorNode node, RemoteNode source, String remoteTid){
		this.node = node;
		this.source = source;
		this.remoteTid = remoteTid;
	}
	
	/**
	 * <p>Retrieves the RemoteNode currently running the job.</p>
	 * 
	 * @return The RemoteNode currently running the job.
	 */
	//TODO: This will probably be renamed
	public RemoteNode getSource(){
		return source;
	}
	
	/**
	 * <p>Retrieves the thread id of the remote job.</p>
	 * 
	 * @return The thread id of the remote job.
	 */
	public String getRemoteTid(){
		return remoteTid;
	}
	
	/**
	 * <p>Sends a chunk of data to the remote job. Upon receipt, this should trigger a call to the
	 * {@link ProcessorJob#dataReceived(RemoteNode, String, java.io.File)} method for the remote ProcessorJob class.</p>
	 * 
	 * @param data The data to send.
	 * 
	 * @throws IOException 
	 */
	//TODO: The socket code will probably be moved to RemoteNode or its proposed meta-class.
	//TODO: The type for data will likely become an abstraction, allowing direct use of files and memory cached resources.
	//TODO: This is functionality is repeated in RemoteNode
	public void sendData(byte data[]) throws IOException{ //TODO: Enforce linkages between jobs and remote jobs to prevent spoofing
		ProcessorJobEnvironment t = (ProcessorJobEnvironment) Thread.currentThread();
		
		//Create socket
		Socket s = null;
		try {
			s = new Socket(t.getSourceJob().getSource().getIpAddress(), t.getSourceJob().getSource().getListeningPort());
		} catch (IOException e) {
			//If here, either host doesn't exist, or is not listening on the port
			//System.err.println("Unable to connect");
			throw e;
		}
		
		try {
			char buffer[] = new char[1500];
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			//See if node is active
			s.getOutputStream().write("Are you alive?".getBytes());
			in.read(buffer);
			
			if(!String.valueOf(buffer).trim().equals("I'm not dead yet.")){
				System.err.println("Unable to return result");
			}
			else{
				s.getOutputStream().write("I have results.".getBytes());
				in.read(buffer); //What did you find?
				
				//Send the listening port for this node to allow node identification
				s.getOutputStream().write((node.getListenPort() + "\n").getBytes());
				
				//Send the remote(parent) process id, local process id
				s.getOutputStream().write((remoteTid + "\n").getBytes());
				s.getOutputStream().write((t.getId() + "\n").getBytes());
				
				//Send the result length and data
				s.getOutputStream().write((data.length + "\n").getBytes());
				s.getOutputStream().write(data);
			}
			s.getOutputStream().write("Goodbye.".getBytes());
			s.close();
		} catch (IOException e1) {
			throw e1;
		}
		finally{
			s.close();
		}
	}
}
