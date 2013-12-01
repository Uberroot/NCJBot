package com.github.uberroot.ncjbot.api;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import com.github.uberroot.ncjbot.RemoteNode;
import com.github.uberroot.ncjbot.RemoteJob;

/**
 * <p>This class is used to run and manage individual LocalJobs. Each LocalJob is run by it its own unique
 * JobEnvironment. The JobEnvironment is used to load the LocalJob subclass, prepare its running environment,
 * and clean up after the LocalJob when it has finished running.</p>
 * 
 * <p>This class also provides an essential interface between the node and individual jobs, allowing jobs to access the public API.</p> 
 * 
 * @author Carter Waxman
 *
 */
public final class JobEnvironment extends Thread {
	/**
	 * <p>The running LocalNode instance.</p>
	 */
	private com.github.uberroot.ncjbot.LocalNode node;
	
	/**
	 * <p>The LocalJob this JobEnvironment runs.</p>
	 */
	private LocalJob job;
	
	/**
	 * <p>A listener that will respond to updates to the state of the LocalJob.</p>
	 */
	private final JobStateListener listener;
	
	/**
	 * <p>The Watchdog for the current node. Used to create and destroy beacons when running the LocalJob.</p>
	 */
	private final com.github.uberroot.ncjbot.modules.Watchdog watchdog;
	
	/**
	 * <p>Whether the class and associated files should automatically be cleaned after termination. This will only be true
	 * for remote jobs.</p>
	 */
	//TODO: should this be renamed to isRemote?
	private final boolean cleanup;
	
	/**
	 * <p>This is the dynamically loaded subclass. An instance of this will be created when this JobEnvironment is started.</p>
	 */
	private final Class<? extends LocalJob> type;
	
	
	//To be accessible by jobs
	/**
	 * <p>The parent / creator of this JobEnvironment's LocalJob. It will be null for locally created jobs.</p>
	 */
	private final RemoteJob parent;
	
	/**
	 * <p>The folder under which the LocalJob may create files.</p>
	 */
	private final File classPath;
	
	/**
	 * <p>The initialization parameters for the LocalJob (defined by the LocalJob).</p>
	 */
	//TODO: Initialization data should be abstracted away from files into streams or byte arrays/buffers
	private final File initData; //TODO: This could probably be removed after initialization
	
	/**
	 * This interface provides callbacks for events related to the state of the LocalJob.
	 * 
	 * @author Carter Waxman
	 *
	 */
	//TODO: This could be more complete. It currently provides core functionality
	public interface JobStateListener{
		/**
		 * Called when the LocalJob has successfully loaded and has been instantiated.
		 * 
		 * @param wrapper The wrapper that triggered the event.
		 * @param job The LocalJob that loaded.
		 */
		public void jobLoaded(JobEnvironment env, LocalJob job);
		
		/**
		 * Called in the event the LocalJob could not be instantiated.
		 * 
		 * @param wrapper The wrapper that triggered the event.
		 * @param ex The exception thrown indicating the cause of the failure.
		 */
		public void jobFailedToLoad(JobEnvironment env, Exception ex);
	}
	
	/**
	 * <p>Instantiates a ProccessorJobWrapper with the parameters needed to start a new LocalJob.</p>
	 * 
	 * @param node The running LocalNode instance.
	 * @param className The name of the LocalJob subclass to load.
	 * @param classPath The path to the directory that contains the LocalJob subclass class file.
	 * @param source The node that sent the LocalJob to this node, or null if the job is to be created locally.
	 * @param remoteTid The thread id of the LocalJob that sent the LocalJob to load to this node.
	 * @param initData The initialization parameters for the LocalJob.
	 * @param watchdog The Watchdog for this node.
	 * @param cleanup Whether the files created should be deleted after the job has completed
	 * @param listener A ProcessorJobStateListener to handle events related to startup.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * 
	 */
	//TODO: className and classPath could be combined
	//TODO: a second constructor could be used (one without the source parameters), which could be used to infer that the job is local
	//TODO: source and remotePID could be combined into a RemoteJob
	//TODO: This should only be callable by LocalNode
	public JobEnvironment(com.github.uberroot.ncjbot.LocalNode node, String className, File classPath, RemoteNode parent, String remoteTid, File initData, com.github.uberroot.ncjbot.modules.Watchdog watchdog, boolean cleanup, JobStateListener listener) throws IOException, ClassNotFoundException{
		this.node = node;
		
		URL[] u = new URL[1];
		u[0] = classPath.toURI().toURL();
		URLClassLoader cl = new URLClassLoader(u);

		type = cl.loadClass(className).asSubclass(LocalJob.class);
		cl.close();
		
		this.setName("Processor Job (localTid = " + this.getId() + ")");
		
		this.watchdog = watchdog;
		this.cleanup =  cleanup;
		this.listener = listener;
		
		this.parent = new RemoteJob(node, parent, remoteTid);
		this.classPath = classPath;
		this.initData = initData;
	}


	/**
	 * <p>Instantiates, prepares, runs, and cleans up after the LocalJob.</p>
	 */
	@Override
	//TODO: After the first call, this method should no longer work
	public void run() {
		//TODO: Cleanup after failures
		//Instantiate job
		try {
			job = type.getConstructor(File.class).newInstance(initData); //TODO: By this point, the securitymanager should be in effect
			//job = type.newInstance();
		} catch (Exception ex){
			listener.jobFailedToLoad(this, ex);
			return;
		}
		
		//Report initialization
		if(listener != null)
			listener.jobLoaded(this, job);

		//Start the job
		if(cleanup)
			//TODO: Should this be moved to the listener?
			watchdog.registerBeacon(parent.getRemoteNode()); //TODO: Registering when a local job causes a deadlock. Figure out why.
		job.run();
		if(cleanup){
			File toDel[] = classPath.listFiles();
			for(File f : toDel)
				f.delete();
			classPath.delete();
		}
		if(cleanup)
			//TODO: Should this be moved to the listener?
			watchdog.releaseBeacon(parent.getRemoteNode());
	}
	
	/**
	 * <p>Gets the path under which the LocalJob should create and access files.</p>
	 * 
	 * @return The path under which the LocalJob should create and access files.
	 */
	public File getClassPath(){
		return classPath;	//TODO: Enforce this
	}
	
	/**
	 * <p>Gets the job that sent the running job to this node.</p>
	 * 
	 * @return The job that sent the running job to this node.
	 */
	public RemoteJob getSourceJob(){
		return parent; //TODO: Make sure this is immutable, get rid of methods here that access this
	}
	
	/**
	 * <p>Retrieves a limited-access version of the running LocalNode instance.</p>
	 * 
	 * @return the running LocalNode instance
	 */
	public LocalNode getNode(){
		return node.getSafeObject();
	}
}
