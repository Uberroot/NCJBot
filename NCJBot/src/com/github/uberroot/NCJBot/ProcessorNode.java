package com.github.uberroot.NCJBot;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Set;


/**
 * <p>This is the main portion of the program. It is responsible for connecting to the network,
 * managing the various supporting threads, and shutting down the node / gracefully disconnecting from the network.</p>
 * 
 * <p>Additionally, this class is responsible for starting, monitoring, and signaling jobs running on this node. This
 * class is not intended to perform operations on individual jobs, but rather enumerate jobs and perform operations on the set of jobs
 * as a whole. For more on managing individual jobs, see {@link ProcessorJobWrapper}.</p>
 * 
 * <p><b>THIS MAY CHANGE</b> The supporting classes use this class as a proxy for all communication.</p>
 * 
 * @author Carter Waxman
 */
//TODO: A single instance of ProcessorNode will be created and shared amongst the other classes to enforce encapsulation
//TODO: Should the ProcessorJob management functionality be in its own class?
public class ProcessorNode {
	/**
	 * <p>The current status string of this node.</p>
	 */
	//TODO: This should be replaced with an enumeration or other similar construct
	//TODO: This needs to be implemented more completely
	private static String status;
	
	/**
	 * <p>The NetworkManager for this node.</p>
	 */
	private static NetworkManager netMgr;
	
	/**
	 * <p>The port on which to listen for new connections.</p>
	 */
	//TODO: This should be moved to ServerThread
	private static int LISTEN_PORT;
	
	/**
	 * <p>A Hashtable of all running jobs, keyed by thread id.</p>
	 */
	private static Hashtable<Long, ProcessorJob> jobs = new Hashtable<Long, ProcessorJob>();
	
	/**
	 * <p>The Watchdog for monitoring remote nodes running locally initiated jobs.</p>
	 */
	private static Watchdog watchdog;
	
	/**
	 * <p>The thread that handles incoming socket communication.</p>
	 */
	private static ServerThread servThread;
	
	/**
	 * <p>Initializes the node. This method initializes supporting threads for the node and runs the
	 * command line interface for utilizing the node.</p>
	 * 
	 * @param args Command line arguements. This is ignored.
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException{
		//TODO: This setup will be replaced with a configuration file
		
		//Seed nodes. This will be replaced with a configuration file
		ArrayList<RemoteNode> seedNodes = new ArrayList<RemoteNode>();
		
		Scanner scan = new Scanner(System.in);
		System.out.print("Enter seed host: ");
		String ip = scan.nextLine().trim();
		System.out.print("Enter seed port: ");
		seedNodes.add(new RemoteNode(ip, scan.nextInt()));
		scan.nextLine();
		
		//Get params
		System.out.print("Enter listening port: ");
		LISTEN_PORT = scan.nextInt();
		scan.nextLine();
		
		//Start the watchdog
		System.out.print("Start watchdog...");
		watchdog = new Watchdog();
		watchdog.start();
		System.out.println("done");
		
		//Run the network manager
		netMgr = new NetworkManager(seedNodes);
		netMgr.start();
		
		//Open socket and respond to requests
		servThread = null;
		try{
			servThread = new ServerThread(LISTEN_PORT);
			servThread.start();
		} catch(IOException e){
			System.err.println(e.getMessage());
		}
		
		//Set the status to ready
		status = "I'm not dead yet.";
		
		//Handle console input
		//TODO: This is a rudimentary console for testing. This functionality should be moved to its own class.
		//TODO: Look into lanterna (https://code.google.com/p/lanterna/) for creating the console. This should allow switching between panels and I/O splitting without a GUI
		System.out.println("Console initialized... What would you like to do?");
		while(true){
			System.out.print("> ");
			String command = scan.nextLine().trim();
			if(command.equalsIgnoreCase("GET THREADS")){
				Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
				System.out.println(threadSet.size() + " threads are running");
				for(Thread t : threadSet){
					System.out.println(t.getName());
				}
			}
			else if(command.equalsIgnoreCase("GET NODES")){
				List<RemoteNode> nodes = ProcessorNode.getActiveNodes();
				String out = nodes.size() + " nodes are known\n";
				for(RemoteNode n : nodes)
					out += n.getIpAddress().toString() + ":" + n.getListeningPort() + "\n";
				System.out.print(out);
			}
			else if(command.equalsIgnoreCase("STOP SERVER")){
				if(servThread != null){
					servThread.kill();
					servThread = null;
				}
				else
					System.err.println("The server is not running");
			}
			else if(command.equalsIgnoreCase("START SERVER")){
				if(servThread != null && servThread.isRunning()){
					System.err.println("The server is already running");
					continue;
				}
				try{
					servThread = new ServerThread(LISTEN_PORT);
					servThread.start();
				}
				catch(IOException e){
					System.err.println(e.getMessage());
					System.err.println("Did you forget to stop the server?");
				}
			}
			else if(command.equalsIgnoreCase("QUIT")){
				quit();
				break;
			}
			else if(command.equalsIgnoreCase("SET PORT")){
				System.out.print("Enter the new port (restart server to apply): ");
				LISTEN_PORT = scan.nextInt();
				scan.nextLine();
			}
			else if(command.equalsIgnoreCase("START JOB")){
				System.out.println("Enter path to class");
				File path = new File(scan.nextLine().trim());
				startJob(path.getParent(), path.getName().replaceFirst("\\.class$", ""), new RemoteNode("127.0.0.1", LISTEN_PORT), null, null, false);
			}
			else
				System.out.println("What?");
		}
		scan.close();
	}
	
	/**
	 * Closes all connections and threads and allows the node to gracefully quit.
	 */
	public static void quit(){
		//TODO: Graceful network removal should occur, but may not be necessary for the current lazy communication model
		if(servThread != null)
			servThread.kill();
		netMgr.interrupt();
		watchdog.kill();
		System.exit(0);
	}
	
	/**
	 * <p>Gets the current status string of the node.</p>
	 * <b>Valid Values:</b>
	 * <ul>
	 * <li><b>I'm not dead yet.</b> The node is active.</li>
	 * <li><b>I'm bleeding out.</b> The node is shutting down.</li>
	 * </ul>
	 * @return The current status string of the node.
	 */
	public static String getStatus(){
		return status;
	}
	
	/**
	 * <p>Gets a list of nodes known to be active on the network.</p>
	 * 
	 * @return A list of nodes known to be active on the network.
	 */
	//TODO: This will be removed in later versions in favor of direct access.
	public static List<RemoteNode> getActiveNodes(){
		return netMgr.getActiveNodes();
	}
	
	/**
	 * <p>Gets the current port on which the server socket should listen.</p>
	 * 
	 * @return The current port on which the server socket should listen.
	 */
	//TODO: This should be moved to ServerThread
	public static int getListenPort(){
		return LISTEN_PORT;
	}
	
	/**
	 * <p>Refreshes the status of the given node and adds it to the active node list if it is not already there.</p>
	 * 
	 * @param rn The node to add / refresh.
	 * @return False if the node already existed in the active node list, true if it did not.
	 */
	//TODO:This may be removed in favor of direct communication.
	public static boolean addDiscoveredNode(RemoteNode rn){
		watchdog.beaconed(rn);
		return netMgr.addDiscoveredNode(rn);
	}
	
	/**
	 * <p>Gets requested number of nodes, wrapping the list around when not enough are known to create a list of unique nodes.</p>
	 * 
	 * @param count The number of nodes to retrieve. A value of -1 indicates the entire node list should be retrieved.
	 * @return A list of <i>count</i> remote nodes.
	 */
	//TODO: This should return a List<RemoteNode>, but that signature results in a NoSuchMethodError from jobs, even after recompiling against the new class
	//TODO: This may be removed in favor of direct communication with NetworkManager.
	public static List<RemoteNode> getNodes(int count){
		if(count == -1)
			return getActiveNodes();
		List<RemoteNode> ans = getActiveNodes();
		ArrayList<RemoteNode> ret = new ArrayList<RemoteNode>();
		if(ans.size() == 0){
			RemoteNode self = null;
			try {
				self = new RemoteNode("127.0.0.1", LISTEN_PORT);
			} catch (UnknownHostException e) {
				//THIS WILL NEVER HAPPEN
			}
			for(int i = 0; i < count; i++)
				ret.add(self);
		}
		else{
			for(int i = 0, added = 0; added < count; i++, added++){
				if(i >= ans.size()){
					i = -1;
					try {
						ret.add(new RemoteNode("127.0.0.1", LISTEN_PORT));
					} catch (UnknownHostException e) {}
					continue;
				}
				ret.add(ans.get(i));
			}
		}
		return Collections.unmodifiableList(ret);
	}
	
	/**
	 * <p> Starts a new job on its own thread with the given parameters. </p>
	 * 
	 * @param classPath The path to the running directory for the job to be created. This is also the path to the class file for the job.
	 * @param className The name of the ProcessorJob subclass to run.
	 * @param source The node that sent the job, or null if it was locally created.
	 * @param remotePid The thread id of the remote job that sent the job, or null if it was locally created.
	 * @param initData Initialization parameters for the job to be created.
	 * @param cleanup Whether to delete the class files and directory after it finishes running.
	 * @return The thread id of the new job, or -1 on failure.
	 */
	//TODO: This should throw an exception on failure
	//TODO: classPath and className should be combined for readability
	public static long startJob(String classPath, String className, RemoteNode source, String remotePid, File initData, boolean cleanup){
		ProcessorJobWrapper jobThread = null;
		try {
			jobThread = new ProcessorJobWrapper(className, new File(classPath), source, remotePid, initData, watchdog, cleanup, new ProcessorJobWrapper.ProcessorJobWrapperStateListener(){
				@Override
				public void jobLoaded(ProcessorJobWrapper wrapper, ProcessorJob job) {
					jobs.put(wrapper.getId(), job);
				}

				@Override
				public void jobFailedToLoad(ProcessorJobWrapper wrapper, Exception ex) {
					ex.printStackTrace();
				}
				
			});
			jobThread.start();
		} catch (Exception e) {
			//Either a bad path was provided, or the classloader cannot load the class.
			e.printStackTrace();
			return -1;
		}
		return jobThread.getId();
	}
	
	/**
	 * <p>Finds a replacement node for the node given. In the event the given node cannot be reached, it will be
	 * removed from the active node list. Otherwise, a node (possibly the one provided) will be randomly selected and returned.</p>
	 * 
	 * @see NetworkManager#getReplacement(RemoteNode)
	 * @param r The node to replace.
	 * @return The new node.
	 */
	//TODO:This may be removed in favor of direct communication.
	public static RemoteNode getReplacement(RemoteNode r){
		return netMgr.getReplacement(r);
	}
	
	/**
	 * <p>Delivers data from a remote job to a job running on this node.</p>
	 * 
	 * @param destPid The thread id of the destination job running on this node.
	 * @param sourcePid The thread id of the source job for the data.
	 * @param source The node from which the data originated
	 * @param data The data to deliver.
	 */
	//TODO: sourcePID and source should be combined into a RemoteProcessorJob
	//TODO: File use should be replaced with an abstraction to a stream or to a byte buffer or array
	public static void sendData(String destPid, String sourcePid, RemoteNode source, File data){
		ProcessorJob job = jobs.get(Long.valueOf(destPid));
		if(job != null)
			job.dataReceived(source, sourcePid, data);
	}
	
	/**
	 * <p>Alerts the jobs currently running on this node to a new node on the network.</p>
	 * 
	 * @param rn The discovered node.
	 */
	public static void announceFoundNode(RemoteNode rn){
		Set<Long> keys = jobs.keySet();
		for(Long key : keys){
			ProcessorJob j = jobs.get(key);
			if(j != null && j.getOwner().isAlive())
				j.nodeFound(rn);
		}
	}
	
	/**
	 * <p>Alerts the jobs currently running on this node to a failure of a node on the network. This indicates that the job
	 * should assume everything running on that node was lost.</p>
	 * 
	 * @param rn The node that failed.
	 */
	//TODO: Should a separate method be created for handling nodes that are found to be unreachable or shutting down?
	public static void announceNodeFailure(RemoteNode rn){
		System.out.println("Node Failure: " + rn.getIpAddress().getHostAddress() + ":" + rn.getListeningPort());
		Set<Long> keys = jobs.keySet();
		for(Long key : keys){
			ProcessorJob j = jobs.get(key);
			if(j != null && j.getOwner().isAlive())
				j.nodeFailed(rn);
		}
	}
	
	/**
	 * <p>Registers a remote node with the watchdog so that this node will be able to detect if the remote node fails.
	 * For each time a remote node is registered, it will need to be released in order to truly be released from the watchdog.</p>
	 * 
	 * @param rn The node to monitor
	 * @see ProcessorNode#releaseWatchdogReceiver(RemoteNode)
	 */
	//TODO:This may be removed in favor of direct communication.
	public static void registerWatchdogReceiver(RemoteNode rn){
		watchdog.registerReceiver(rn);
	}
	
	/**
	 * <p>Releases a remote node from the watchdog so that failure may no longer be reported.
	 * For each time a remote node is registered, it will need to be released in order to truly be released from the watchdog.</p>
	 * 
	 * @param rn The node to release from the watchdog.
	 * @see ProcessorNode#registerWatchdogReceiver(RemoteNode)
	 */
	//TODO:This may be removed in favor of direct communication.
	public static void releaseWatchdogReceiver(RemoteNode rn){
		watchdog.releaseReceiver(rn);
	}
}
