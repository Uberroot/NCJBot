package com.github.uberroot.ncjbot;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.uberroot.ncjbot.api.LocalJob;
import com.github.uberroot.ncjbot.api.JobEnvironment;

/**
 * <p>This is the core of the NCJBot. It is responsible for connecting to the network,
 * managing the various supporting threads, and shutting down the node / gracefully disconnecting from the network.</p>
 * 
 * <p>Additionally, this class is responsible for starting, monitoring, and signaling jobs running on this node. 
 * For more on the loading and running of individual jobs, see {@link JobEnvironment}.</p>
 * 
 * <p>The supporting classes use this class as a proxy for all communication, retrieving the instances of each other
 * from this class.</p>
 * 
 * @author Carter Waxman
 */
//TODO: Should the LocalJob management functionality be in its own class?
public final class LocalNode implements UnsafeObject<com.github.uberroot.ncjbot.api.LocalNode>{
	/**
	 * <p>Used to ensure the main method cannot be called more than once.</p>
	 */
	private static boolean mutex = false;
	
	/**
	 * <p>The current state of this node.</p>
	 */
	//TODO: This needs to be implemented more completely
	private NodeState state = NodeState.UNKNOWN;
	
	/**
	 * <p>The ConfigManager for this node.</p>
	 */
	private ConfigManager configManager;
	
	/**
	 * <p>The OverlayManager for this node.</p>
	 */
	private OverlayManager netMgr;
	
	/**
	 * <p>The port on which to listen for new connections.</p>
	 */
	//TODO: This should be moved to ServerThread
	private int listenPort;
	
	/**
	 * <p>A Hashtable of all running jobs, keyed by thread id.</p>
	 */
	private Hashtable<Long, LocalJob> jobs = new Hashtable<Long, LocalJob>();
	
	/**
	 * <p>The Watchdog for monitoring remote nodes running locally initiated jobs.</p>
	 */
	private Watchdog watchdog;
	
	/**
	 * <p>The thread that handles incoming socket communication.</p>
	 */
	private ServerThread servThread;
	
	/**
	 * <p>The ScheduledThreadPoolExecutor used for centralized delayed callbacks.</p>
	 */
	private ScheduledThreadPoolExecutor executor;
	
	/**
	 * <p>Entry point for the program. This loads the sole LocalNode instance.</p>
	 * 
	 * @see LocalNode#LocalNode()
	 * @param args Command line arguments. This is ignored.
	 * @throws IOException
	 */
	public static void main(String args[]){
		if(mutex)	//Can't let anything call this method (especially a LocalJob).
			return;
		mutex = true;
		try {
			new LocalNode();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * <p>Initializes the node. This method initializes supporting threads for the node and runs the
	 * command line interface for utilizing the node.</p>
	 * 
	 * @throws IOException
	 */
	private LocalNode() throws IOException{
		//Load the configuration
		configManager = new ConfigManager(this, new File("config.Properties"));

		//Create the executor service
		executor = new ScheduledThreadPoolExecutor(2); //TODO: This should be configurable.
		
		//Start the watchdog
		System.out.print("Starting watchdog...");
		watchdog = new Watchdog(this);
		watchdog.start();
		System.out.println("Success");
		
		//Load the seed nodes
		ArrayList<RemoteNode> seedNodes = new ArrayList<RemoteNode>();
		String seedStrings[] = configManager.getSetting("LocalNode", "seedNodes").split(",");
		for(String s : seedStrings){
			String seed[] = s.trim().split(":");
			try{
				seedNodes.add(new RemoteNode(this, seed[0], Integer.valueOf(seed[1])));
			}
			catch(UnknownHostException e){
				//Ignore the seed
				System.err.println("Bad seed hostname in configuration: " + s);
			}
		}
		
		//Setup the overlay manager
		netMgr = new OverlayManager(this, seedNodes);//TODO: The network should not be joined until the server is running
		
		//Open socket and respond to requests
		//TODO: This access pattern should change. The socket should be created outside of the thread that queries it.
		listenPort = Integer.valueOf(configManager.getSetting("LocalNode", "minListenPort"));
		int maxPort = Integer.valueOf(configManager.getSetting("LocalNode", "maxListenPort"));
		int portIncrement = Integer.valueOf(configManager.getSetting("LocalNode", "listenPortIncrement"));
		while(servThread == null && listenPort <= maxPort){
			try{
				servThread = new ServerThread(this, listenPort);
				servThread.start();
			} catch(IOException e){
				servThread = null;
				listenPort += portIncrement;
				System.err.println(e.getMessage());
			}
		}
		
		//Join the network if ready
		if(servThread != null){
			//Set the status to ready
			state = NodeState.RUNNING;
			
			//Join the network
			netMgr.startBeacon();
		}
		else{
			System.err.print("Unable to start server and join network. Please adjust your configuration.\n");
			quit();
		}
		
		//Handle console input
		//TODO: This is a rudimentary console for testing. This functionality should be moved to its own class.
		//TODO: Look into lanterna (https://code.google.com/p/lanterna/) for creating the console. This should allow switching between panels and I/O splitting without a GUI
		Scanner scan = new Scanner(System.in);
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
				List<RemoteNode> nodes = netMgr.getActiveNodes();
				String out = nodes.size() + " nodes are known\n";
				for(RemoteNode n : nodes)
					out += n.getIpAddress().toString() + ":" + n.getListeningPort() + "\n";
				System.out.print(out);
			}
			//TODO: Gracefully disconnect.
			else if(command.equalsIgnoreCase("STOP SERVER")){
				if(servThread != null){
					servThread.kill();
					servThread = null;
				}
				else
					System.err.println("The server is not running");
			}
			//TODO: Should this force a presence announcement / node info update?
			else if(command.equalsIgnoreCase("START SERVER")){
				if(servThread != null && servThread.isRunning()){
					System.err.println("The server is already running");
					continue;
				}
				try{
					servThread = new ServerThread(this, listenPort);
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
			//TODO: Should this force a presence announcement / node info update?
			//TODO: A transitional phase should occur when changing ports to finish running jobs before restarting and accepting new jobs
			else if(command.equalsIgnoreCase("SET PORT")){
				System.out.print("Enter the new port (restart server to apply): ");
				listenPort = scan.nextInt();
				scan.nextLine();
			}
			else if(command.equalsIgnoreCase("START JOB")){
				System.out.println("Enter path to class");
				File path = new File(scan.nextLine().trim());
				startJob(path.getParent(), path.getName().replaceFirst("\\.class$", ""), new RemoteNode(this, "127.0.0.1", listenPort), null, null, false);
			}
			else
				System.out.println("What?");
		}
		scan.close();
	}
	
	/**
	 * Closes all connections and threads and allows the node to gracefully quit.
	 */
	public void quit(){
		//TODO: Graceful network removal should occur, but may not be necessary for the current lazy communication model
		if(servThread != null)
			servThread.kill();
		netMgr.stopBeacon();
		watchdog.kill();
		executor.shutdown();
		System.exit(0);
	}
	
	/**
	 * <p>Gets the current status of the node.</p>

	 * @return The current status of the node.
	 */
	public NodeState getState(){
		return state;
	}
	
	/**
	 * <p>Gets the current port on which the server socket should listen.</p>
	 * 
	 * @return The current port on which the server socket should listen.
	 */
	//TODO: This should be moved to ServerThread.
	//TODO: A distinction should be made between the current port and the configured port.
	public int getListenPort(){
		return listenPort;
	}
	
	/**
	 * <p>Refreshes the status of the given node and adds it to the active node list if it is not already there.</p>
	 * 
	 * @param rn The node to add / refresh.
	 * @return False if the node already existed in the active node list, true if it did not.
	 */
	//TODO:This may be removed in favor of direct communication.
	public boolean addDiscoveredNode(RemoteNode rn){
		watchdog.beaconed(rn);
		return netMgr.addDiscoveredNode(rn);
	}
	
	/**
	 * <p> Starts a new job on its own thread with the given parameters. </p>
	 * 
	 * @param classPath The path to the running directory for the job to be created. This is also the path to the class file for the job.
	 * @param className The name of the LocalJob subclass to run.
	 * @param source The node that sent the job, or null if it was locally created.
	 * @param remoteTid The thread id of the remote job that sent the job, or null if it was locally created.
	 * @param initData Initialization parameters for the job to be created.
	 * @param cleanup Whether to delete the class files and directory after it finishes running.
	 * @return The thread id of the new job, or -1 on failure.
	 */
	//TODO: This should throw an exception on failure
	//TODO: classPath and className should be combined for readability
	public long startJob(String classPath, String className, RemoteNode source, String remoteTid, File initData, boolean cleanup){
		JobEnvironment jobThread = null;
		try {
			jobThread = new JobEnvironment(this, className, new File(classPath), source, remoteTid, initData, watchdog, cleanup, new JobEnvironment.JobStateListener(){
				@Override
				public void jobLoaded(JobEnvironment wrapper, LocalJob job) {
					jobs.put(wrapper.getId(), job);
				}

				@Override
				public void jobFailedToLoad(JobEnvironment wrapper, Exception ex) {
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
	 * <p>Delivers data from a remote job to a job running on this node.</p>
	 * 
	 * @param destTid The thread id of the destination job running on this node.
	 * @param sourceTid The thread id of the source job for the data.
	 * @param source The node from which the data originated
	 * @param data The data to deliver.
	 */
	//TODO: sourcePID and source should be combined into a RemoteJob
	//TODO: File use should be replaced with an abstraction to a stream or to a byte buffer or array
	public void sendData(String destTid, String sourceTid, RemoteNode source, File data){
		LocalJob job = jobs.get(Long.valueOf(destTid));
		if(job != null)
			job.dataReceived(source, sourceTid, data);
	}
	
	/**
	 * <p>Alerts the jobs currently running on this node to a new node on the network.</p>
	 * 
	 * @param rn The discovered node.
	 */
	public void announceFoundNode(RemoteNode rn){
		Set<Long> keys = jobs.keySet();
		for(Long key : keys){
			LocalJob j = jobs.get(key);
			if(j != null && j.getEnvironment().isAlive())
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
	public void announceNodeFailure(RemoteNode rn){
		System.out.println("Node Failure: " + rn.getIpAddress().getHostAddress() + ":" + rn.getListeningPort());
		Set<Long> keys = jobs.keySet();
		for(Long key : keys){
			LocalJob
			j = jobs.get(key);
			if(j != null && j.getEnvironment().isAlive())
				j.nodeFailed(rn);
		}
	}
	
	/**
	 * <p>Retrieves the running OverlayManager for this node.</p>
	 * 
	 * @return The running OverlayManager for this node.
	 */
	public OverlayManager getOverlayManager(){
		return netMgr;
	}
	
	/**
	 * <p>Retrieves the running Watchdog for this node.</p>
	 * 
	 * @return The running Watchdog for this node.
	 */
	public Watchdog getWatchdog(){
		return watchdog;
	}
	
	/**
	 * <p>Gets the current configuration manager.</p>
	 * 
	 * @return the current configuration manager.
	 */
	public ConfigManager getConfigManager(){
		return configManager;
	}
	
	/**
	 * Gets the ScheduledThreadPoolExecutor used for timed callbacks.
	 * 
	 * @return The ScheduledThreadPoolExecutor used for timed callbacks.
	 */
	//TODO: This will be replaced with a multiple thread pool design
	public ScheduledThreadPoolExecutor getExecutor(){
		return executor;
	}

	@Override
	public com.github.uberroot.ncjbot.api.LocalNode getSafeObject() {
		return new com.github.uberroot.ncjbot.api.LocalNode(this);
	}
}