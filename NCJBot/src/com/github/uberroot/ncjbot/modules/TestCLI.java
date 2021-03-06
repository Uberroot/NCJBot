package com.github.uberroot.ncjbot.modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.github.uberroot.ncjbot.ConfigManager;
import com.github.uberroot.ncjbot.LocalNode;
import com.github.uberroot.ncjbot.RemoteNode;
import com.github.uberroot.ncjbot.modapi.RunningModule;
import com.github.uberroot.ncjbot.modapi.Server;

/**
 * <p>A simple CLI that supports basic operations on the local node.</p>
 * 
 * <p>Available Commands:
 * <ul>
 *	<li>Get Nodes - Shows a listing of all nodes known to exist on the overlay network.</li>
 *	<li>Get Threads - Shows the threads running on the node.</li>
 *	<li>Quit - Stops the node and disconnects from the network.</li>
 *	<li>Set Port - Sets the listening port for the server socket. The server will need to be restarted to put the new port into use.</li>
 *	<li>Start Job - Loads and runs a locally available subclass of LocalJob.</li>
 *	<li>Start Server - Opens the server socket on the configured port and listens for new connections.</li>
 *	<li>Stop Server - Closes the server socket and prevents the node from receiving new communications. Outgoing connections will continue to work.</li>
 * </ul>
 *</p>
 * 
 * @author Carter Waxman
 *
 */
public class TestCLI extends RunningModule {
	private InputStream cin;
	private PrintStream cout;
	private PrintStream cerr;
	private boolean pause;
	private Scanner scan;

	public TestCLI(LocalNode node) {
		super(node);
		pause = false;
	}

	@Override
	public synchronized void link() {	
		cin = System.in;
		cout = System.out;
		cerr = System.err;
		
		//Take control of the input stream
		System.setIn(new InputStream(){
			@Override
			public int read() throws IOException {
				throw new IOException("Stream not available");
			}
		});
		
		//Take control of output streams
		ConfigManager c = node.getConfigManager();
		System.setOut(new TaggedStream(cout, c.getSetting(name, "tagOut", boolean.class)));
		System.setErr(new TaggedStream(cerr, c.getSetting(name, "tagErr", boolean.class)));
	}

	@Override
	public void doStart() {
		//TODO: Look into lanterna (https://code.google.com/p/lanterna/) for creating the console. This should allow switching between panels and I/O splitting without a GUI
		scan = new Scanner(cin);

		executor.execute(new Runnable(){

			@Override
			public void run() {
				cout.println("Console initialized... What would you like to do?");
				while(true){
					while(pause)
						Thread.yield();
					cout.print("> ");
					String command = scan.nextLine().trim();
					if(command.equalsIgnoreCase("GET THREADS")){
						Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
						cout.println(threadSet.size() + " threads are running");
						for(Thread t : threadSet){
							cout.println(t.getName());
						}
					}
					else if(command.equalsIgnoreCase("GET NODES")){
						List<RemoteNode> nodes = node.getOverlayManager().getActiveNodes();
						String out = nodes.size() + " nodes are known\n";
						for(RemoteNode n : nodes)
							out += n.getIpAddress().toString() + ":" + n.getListeningPort() + "\n";
						cout.print(out);
					}
					//TODO: Gracefully disconnect.
					else if(command.equalsIgnoreCase("STOP SERVER")){
						Server s = node.getServer();
						synchronized(s){
							if(s.getState() != RunnerState.RUNNING)
								cerr.println("The server is not running");
							else
								try {
									s.stop();
								} catch (Exception e) {
									cerr.println(e.getMessage());
								}
						}
					}
					//TODO: Should this force a presence announcement / node info update?
					else if(command.equalsIgnoreCase("START SERVER")){
						Server s = node.getServer();
						synchronized(s){
							if(s.getState() != RunnerState.STOPPED && s.getState() != RunnerState.NEVER_RUN)
								cerr.println("Unable to start server. Did you forget to stop it?");
							else
								try {
									s.start();
								} catch (Exception e) {
									cerr.println(e.getMessage());
								}
						}
					}
					else if(command.equalsIgnoreCase("QUIT")){
						node.quit();
					}
					//TODO: Should this force a presence announcement / node info update?
					//TODO: A transitional phase should occur when changing ports to finish running jobs before restarting and accepting new jobs
					else if(command.equalsIgnoreCase("SET PORT")){
						cout.print("Enter the new port (restart server to apply): ");
						node.getServer().setPort(scan.nextInt(), false);
						scan.nextLine();
					}
					else if(command.equalsIgnoreCase("START JOB")){
						cout.println("Enter path to class");
						File path = new File(scan.nextLine().trim());
						try {
							node.startJob(path.getParent(), path.getName().replaceFirst("\\.class$", ""), new RemoteNode(node, "127.0.0.1", node.getServer().getCurrentPort()), null, null, false);
						} catch (UnknownHostException e) {
							//THIS SHOULD NOT HAPPEN
							cout.println("Something happened");
							node.quit();
						}
					}
					else
						cout.println("What?");
				}
			}
			
		});
	}

	@Override
	public synchronized void doPause() {
		pause = true;
	}

	@Override
	public synchronized void doResume() {
		pause = false;
	}

	@Override
	public synchronized void doStop() {
		scan.close();
	}

	@Override
	public synchronized void unlink() {
		//Unlink from the IO streams
		System.setIn(cin);
		System.setOut(cout);
		System.setErr(cerr);
	}

	/**
	 * A PrintStream that tags all outputs with the name of the calling thread.</p>
	 * 
	 * @author Carter Waxman
	 *
	 */
	private class TaggedStream extends PrintStream{
		
		/**
		 * <p>Whether or not to tag each output.</p>
		 */
		private boolean tag;
		
		/**
		 * <p>Creates a new tagged stream.</p>
		 *
		 * @param out The print stream to piggyback
		 * @param tag Whether or not to tag the output.
		 */
		TaggedStream(PrintStream out, boolean tag){
			super(out);
			this.tag = tag;
		}
		
		@Override
		public void write(byte[] buf, int off, int len){
			byte[] s = (" :" + Thread.currentThread().getName() + ": ").getBytes();
			if(tag && buf.length > 0 && !(buf[0] == 13 && buf[1] == 10)) //Ignore calls that only contain newlines
				super.write(s, 0, s.length);
			super.write(buf, off, len);
		}
	}
}
