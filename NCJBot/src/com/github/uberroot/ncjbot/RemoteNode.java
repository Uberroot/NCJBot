package com.github.uberroot.ncjbot;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>This class is responsible for holding data about remote nodes and handling the underlying socket communication.</p>
 * 
 * @author Carter Waxman
 */
//TODO: This should encapsulate a meta-class that handles ALL client socket code. Meta class methods will not automatically close sockets for efficiency.
//TODO: The method used for node identification should be updated, possibly using asymmetric cryptography.
//TODO: Should InetSocketAddress be used?
//TODO: Throw exceptions on socket errors
//TODO: Add ability to check for info about system a node runs on.
//TODO: A better communication model between RemoteNodes and NetworkManagers needs to be established (a listener model?)
public final class RemoteNode {
	/**
	 * <p>The running ProcessorNode instance.</p>
	 */
	private ProcessorNode node;
	
	/**
	 * The port on which the node accepts new connections.
	 */
	private int listeningPort;
	
	/**
	 * The IP address of the node.
	 */
	private InetAddress ipAddress;
	
	/**
	 * A unique hash for the address-port paring. This is recalculated when either value changes.
	 */
	private int hashCode; //TODO: THIS PROBABLY COLLIDES.
	
	/**
	 * Creates an instance of a RemoteNode with the host/port pair.
	 * @param node The running ProcessorNode instance.
	 * @param ip IPv4 address or hostname of the machine running the node.
	 * @param port TCP Port on which the node listens.
	 * @throws UnknownHostException
	 */
	//TODO: Hostname resolution doesn't seem to work every time.
	public RemoteNode(ProcessorNode node, String ip, int port) throws UnknownHostException{
		this.node = node;
		setIpAddress(ip);
		setListeningPort(port);
	}
	
	//TODO: The following private setters may need to become public if asymmetric cryptography is used for identification, allowing nodes to become aware of ip address changes.
	
	/**
	 * Sets the address used to connect to the remote node.
	 * @param ip The new ip address or hostname.
	 * @throws UnknownHostException
	 */
	private void setIpAddress(String ip) throws UnknownHostException{
		ipAddress = InetAddress.getByName(ip);
	}
	
	/**
	 * Sets the TCP port used to connect to the remote node.
	 * @param port The new TCP port.
	 */
	private void setListeningPort(int port){
		listeningPort = port;
		rehash();
	}

	/**
	 * Gets the IPv4 address of the remote node.
	 * @return The IPv4 address of the remote node.
	 */
	public InetAddress getIpAddress(){
		return ipAddress;
	}

	/**
	 * Gets the listening port of the remote node.
	 * @return The listening port of the remote node
	 */
	public int getListeningPort(){
		return listeningPort;
	}

	/**
	 * Compares this remote node with the one provided to determine if they represent the same instance of a node.
	 * 
	 * @param o The other node.
	 * @return True if the nodes match, false if they do not.
	 */
	@Override
	public boolean equals(Object o){
		if(o.getClass() == RemoteNode.class){
			RemoteNode other = (RemoteNode)o;
			if(other.ipAddress.equals(ipAddress) && other.listeningPort == listeningPort)
				return true;
		}
		return false;
	}
	
	/**
	 * Generates a string describing the remote node in the format <IP Address>:<Listening Port>.
	 * 
	 * @return The string describing the remote node.
	 */
	@Override
	public String toString(){
		return ipAddress.getHostAddress() + ":" + listeningPort;
	}
	
	/**
	 * Gets a hash code used to identify the remote node by IP address and TCP port.
	 * 
	 * @return The hash code generated.
	 */
	@Override
	public int hashCode(){
		return hashCode;
	}
	
	/**
	 * Generates a new hash code based upon the IP address and TCP port of the remote node.
	 */
	//TODO: Collisions are very possible
	private void rehash(){ //TODO This might need to be redone
		MessageDigest md5d = null;
		try {
			md5d = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e2) {
			new Exception("Unable to create unique hashes for RemoteNodes.", e2).printStackTrace();
			node.quit();
		}
		byte hash[] = md5d.digest(toString().getBytes());
		hashCode = 0;
		for(int i = 0; i < hash.length; i += 4){
			int toXor = hash[i] | (hash[i + 1] << 8) | (hash[i + 2] << 16) | (hash[i + 3] << 24);
			hashCode ^= toXor;
		}
	}

	/**
	 * Queries the node for a list of all other nodes it communicates with.
	 * 
	 * @throws ConnectException
	 * @throws IOException 
	 * @throws NodeStateException 
	 */
	public List<RemoteNode> getKnownNodes() throws IOException, NodeStateException{
		ArrayList<RemoteNode> ret = new ArrayList<RemoteNode>();
		
		//Try to create socket
		Socket s = new Socket(ipAddress, listeningPort); //Could throw a ConnectionException

		//See if node is active
		try {
				char buffer[] = new char[1500];
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				s.getOutputStream().write("Are you alive?".getBytes());
				in.read(buffer);
				
				if(String.valueOf(buffer).trim().equals("I'm not dead yet.")){
					System.out.println("Node is active");
					System.out.print("Retreiving node list...\t");
					s.getOutputStream().write("Who do you know?".getBytes());
					buffer = new char[1500];
					in.read(buffer);
					String[] nodeStrings = String.valueOf(buffer).trim().split("\n");
					
					//Parse the node list from this node
					for(String ns : nodeStrings){
						if(!ns.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+"))
							continue; //TODO: Should this throw a malformed data exception?
						String[] pair = ns.split(":");
						RemoteNode rn = new RemoteNode(node, pair[0], Integer.valueOf(pair[1]));
						
						//Add the new node to this node's active node list
						if(!ret.contains(rn)){
							ret.add(rn);
							node.announceFoundNode(rn);
						}
					}
				}
				else if(String.valueOf(buffer).trim().equals("I'm bleeding out."))
					//Node is shutting down
					throw new NodeStateException(NodeState.SHUTTING_DOWN);
				else
					//Unknown node state
					throw new NodeStateException(NodeState.UNKNOWN);
				
				//Allow the server to close the connection
				s.getOutputStream().write("Goodbye.".getBytes());
		} catch (IOException e) {
			//Communication error of some sort. Throw exception and fall through to the socket closure.
			throw e;
		}
		finally{
			//Close the socket
			s.close();
		}
		
		return ret;
	}
	
	/**
	 * Sends a unit of data to a specific job on the remote node.
	 * 
	 * @param destTid The thread id of the job receiving the data.
	 * @param data The data to send.
	 * 
	 * @throws ConnectException
	 * @throws IOException 
	 * @throws NodeStateException 
	 */
	//TODO: Abstract the data storage and account for size and performance issues automatically
	//TODO: This method should be merged with RemoteProcessorJob.sendData(byte[])
	//TODO: Failure here needs to make its way to the network manager
	public void sendData(String destTid, byte[] data) throws IOException, NodeStateException{
		//Try to create socket
		Socket s = new Socket(ipAddress, listeningPort);
		
		try {
			char buffer[] = new char[1500];
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			//See if node is active
			s.getOutputStream().write("Are you alive?".getBytes());
			in.read(buffer);
			
			if(String.valueOf(buffer).trim().equals("I'm not dead yet.")){
				s.getOutputStream().write("I have results.".getBytes());
				in.read(buffer); //What did you find?
				
				//Send the listening port for this node to allow node identification
				s.getOutputStream().write((node.getListenPort() + "\n").getBytes());
				
				//Send the remote(parent) process id, local process id
				s.getOutputStream().write((destTid + "\n").getBytes());
				s.getOutputStream().write((Thread.currentThread().getId() + "\n").getBytes()); //TODO: This assumes that the thread calling this method is the one that runs the ProcessorJob
				
				//Send the result length and data
				s.getOutputStream().write((data.length + "\n").getBytes());
				s.getOutputStream().write(data);
			}
			else if(String.valueOf(buffer).trim().equals("I'm bleeding out."))
				//Node is shutting down
				throw new NodeStateException(NodeState.SHUTTING_DOWN);
			else
				//Unknown node state
				throw new NodeStateException(NodeState.UNKNOWN);
			s.getOutputStream().write("Goodbye.".getBytes());
			s.close();
		} catch (IOException e) {
			//Communication error of some sort. Throw exception and fall through to the socket closure.
			throw e;
		}
		finally{
			//Close the socket
			s.close();
		}
	}
	
	/**
	 * Sends a job to be run on the RemoteNode. This will automatically register the RemoteNode with the Watchdog. Upon completion,
	 * a corresponding call to releaseWatchdogReceiver() should occur with this node.
	 * 
	 * @param ownerTid The thread id of the job that will be the parent of the started job.
	 * @param worker A file pointing to the class file to send.
	 * @param params Initialization parameters for the new ProcessorJob.
	 * @return The remote thread id of the new job.
	 * 
	 * @throws ConnectException
	 * @throws IOException
	 * @throws NodeStateException 
	 */
	//TODO: Automatically derive ownerTid.
	//TODO: Abstract param and worker storage and account for performance and space issues automatically.
	//TODO: This should return a RemoteProcessorJob
	//TODO: An additional parameter should be provided to allow the Watchdog functionality to be toggled
	//TODO: Add job state tracking.
	//TODO: Failure here needs to make its way to the network manager
	public long sendJob(long ownerTid, File worker, byte[] params) throws IOException, NodeStateException{
		long ret = 0;
		
		//Try to create socket
		Socket s = new Socket(ipAddress, listeningPort);
		
		try {
			char buffer[] = new char[1500];
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			//See if node is active
			s.getOutputStream().write("Are you alive?".getBytes());
			in.read(buffer);
			
			if(String.valueOf(buffer).trim().equals("I'm not dead yet.")){
				s.getOutputStream().write("I have a job for you.".getBytes());
				in.read(buffer); //What will I need?
				
				//Send the listening port
				s.getOutputStream().write((node.getListenPort() + "\n").getBytes());
				
				//Send the local process id and worker class name
				s.getOutputStream().write((ownerTid + "\n").getBytes());
				s.getOutputStream().write((worker.getName().replaceFirst("\\.class$", "") + "\n").getBytes());
				
				//Send worker and param size
				int pl = params.length;
				long fl = worker.length();
				s.getOutputStream().write((pl + "\n").getBytes());
				s.getOutputStream().write((fl + "\n").getBytes());
				
				//Send worker and params
				s.getOutputStream().write(params);
				BufferedInputStream fin = new BufferedInputStream(new FileInputStream(worker));
				byte fbuffer[] = new byte[4096];
				int read = 0;
				while((read = fin.read(fbuffer)) != -1)
					s.getOutputStream().write(fbuffer, 0, read);
				fin.close();
				
				//Await remote process id
				ret= Long.valueOf(in.readLine());
				node.getWatchdog().registerReceiver(this);
			}
			else if(String.valueOf(buffer).trim().equals("I'm bleeding out."))
				//Node is shutting down
				throw new NodeStateException(NodeState.SHUTTING_DOWN);
			else
				//Unknown node state
				throw new NodeStateException(NodeState.UNKNOWN);
			s.getOutputStream().write("Goodbye.".getBytes());
		} catch (IOException e) {
			//Communication error of some sort. Throw exception and fall through to the socket closure.
			throw e;
		}
		finally{
			//Close the socket
			s.close();
		}
		return ret;
	}

	/**
	 * <p>Alerts the RemoteNode to the presence of this node.</p>
	 * 
	 * @throws IOException 
	 * @throws NodeStateException 
	 */
	//TODO: Failure here needs to make its way to the network manager
	public void beacon() throws IOException, NodeStateException{
		//Try to create socket
		Socket s = new Socket(ipAddress, listeningPort);
		
		//See if node is active
		try {
				char buffer[] = new char[1500];
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
				s.getOutputStream().write("Are you alive?".getBytes());
				in.read(buffer);
				
				//Announce presence
				if(String.valueOf(buffer).trim().equals("I'm not dead yet.")){
					s.getOutputStream().write(("I'm here.\n" + node.getListenPort()).getBytes());
					
					//TODO: should this actually be read? It tells whether the other node knew of this one.
					in.read(buffer); //To ensure flow control
				}
				else if(String.valueOf(buffer).trim().equals("I'm bleeding out."))
					throw new NodeStateException(NodeState.SHUTTING_DOWN);
				else
					throw new NodeStateException(NodeState.UNKNOWN);
				s.getOutputStream().write("Goodbye.".getBytes());
		} catch (IOException e) {
			//The connection was interrupted for some reason...
			System.out.println("Unable to determine node state");
			//node.announceNodeFailure(activeNodes.remove(i--));	
		}
		finally{
			//Close the socket
			s.close();
		}
	}
	
	
	/**
	 * Determines if it is possible to establish a connection with the node.
	 * 
	 * @deprecated This will be removed soon as it can be replaced entirely with exception handling and listener objects
	 * @return True if a connection could be established, false otherwise.
	 */
	public boolean canConnect(){
		Socket s = null;
		try {
			s = new Socket(getIpAddress(), getListeningPort());
		} catch (IOException e) {
			//If here, either host doesn't exist, or is not listening on the port
			return false;
		}
		try {
			if(s != null)
				s.close();
		} catch (IOException e) {
			
		}
		return true;
	}
}
