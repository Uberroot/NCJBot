package com.github.uberroot.ncjbot.modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import com.github.uberroot.ncjbot.LocalNode;
import com.github.uberroot.ncjbot.RemoteNode;

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
public class TestCLI extends AbstractModule {
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
		
		//Take control of output stream
		System.setOut(new TaggedStream(cout, Boolean.valueOf(node.getConfigManager().getSetting(name, "tagOut"))));
		System.setErr(new TaggedStream(cerr, Boolean.valueOf(node.getConfigManager().getSetting(name, "tagErr"))));
	}

	@Override
	public void run() {
		//TODO: This is a rudimentary console for testing. This functionality should be moved to its own class.
		//TODO: Look into lanterna (https://code.google.com/p/lanterna/) for creating the console. This should allow switching between panels and I/O splitting without a GUI
		scan = new Scanner(cin);

		executor.execute(new Runnable(){

			@Override
			public void run() {
				cout.println("Console initialized... What would you like to do?");
				while(true){
					cout.print("> ");
					String command = scan.nextLine().trim();
					while(pause)
						try {
							this.wait();
						} catch (InterruptedException e1) {}
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
						if(!node.stopServer())
							cerr.println("The server is not running");
					}
					//TODO: Should this force a presence announcement / node info update?
					else if(command.equalsIgnoreCase("START SERVER")){
						try{
							if(!node.startServer())
								cout.println("Unable to start server. Did you forget to stop it?");
						}
						catch(IOException e){
							cerr.println(e.getMessage());
						}
					}
					else if(command.equalsIgnoreCase("QUIT")){
						node.quit();
					}
					//TODO: Should this force a presence announcement / node info update?
					//TODO: A transitional phase should occur when changing ports to finish running jobs before restarting and accepting new jobs
					else if(command.equalsIgnoreCase("SET PORT")){
						cout.print("Enter the new port (restart server to apply): ");
						node.setListenPort(scan.nextInt());
						scan.nextLine();
					}
					else if(command.equalsIgnoreCase("START JOB")){
						cout.println("Enter path to class");
						File path = new File(scan.nextLine().trim());
						try {
							node.startJob(path.getParent(), path.getName().replaceFirst("\\.class$", ""), new RemoteNode(node, "127.0.0.1", node.getListenPort()), null, null, false);
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
	public synchronized void pause() {
		pause = true;
	}

	@Override
	public synchronized void resume() {
		pause = false;
		this.notifyAll();
	}

	@Override
	public synchronized void stop() {
		cout.println("stopping");
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
			if(tag && buf.length > 0 && !(buf[0] == 13 && buf[1] == 10)) //Ignore only newlines
				super.write(s, 0, s.length);
			super.write(buf, off, len);
		}
	}
}
