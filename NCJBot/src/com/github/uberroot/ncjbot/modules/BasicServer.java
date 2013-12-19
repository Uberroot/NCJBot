package com.github.uberroot.ncjbot.modules;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import com.github.uberroot.ncjbot.LocalNode;
import com.github.uberroot.ncjbot.ServerSessionHandler;
import com.github.uberroot.ncjbot.modapi.RunningModule;
import com.github.uberroot.ncjbot.modapi.Server;
import com.github.uberroot.ncjbot.modapi.ConnectionFactory.Connection;

/**
 * <p>A simple server to accept connections and dispatch them to session handlers.</p>
 * 
 * @author Carter Waxman
 *
 */
//TODO: Look into using SocketChannel for non-blocking socket handling
public class BasicServer extends RunningModule implements Server {
	/**
	 * <p>The default port to use for accepting connections.</p>
	 */
	private int port;
	
	/**
	 * <p>In the event the default port could not be used, the lowest port in the backup pool to try.</p>
	 */
	private int minPort;
	
	/**
	 * <p>In the even the default port could not be used, the highest port in the backup pool to try.</p>
	 */
	private int maxPort;
	
	/**
	 * <p>In the even the default port could not be used, the increment to use while iterating ports in the backup pool.
	 */
	private int listenPortIncrement;
	
	/**
	 * <p>The running server socket.</p>
	 */
	private ServerSocket socket;
	
	/**
	 * <p>Whether the BasicServer should continue to accept connections.</p>
	 */
	private boolean shouldAccept;
	
	/**
	 * <p>Whether the BasicServer should temporarily stop accepting connections.</p>
	 */
	private boolean shouldPause;
	
	/**
	 * <p>Whether the BasicServer successfully paused.</p>
	 */
	private boolean paused;
	
	/**
	 * <p>Whether the BasicServer is currently accepting connections.</p>
	 */
	private boolean accepting;
	 
	public BasicServer(LocalNode node) {
		super(node);
		port = node.getConfigManager().getSetting(name, "listenPort", int.class);
		minPort = node.getConfigManager().getSetting(name, "minListenPort", int.class);
		maxPort = node.getConfigManager().getSetting(name, "maxListenPort", int.class);
		listenPortIncrement = node.getConfigManager().getSetting(name, "listenPortIncrement", int.class);
		socket = null;
	}

	@Override
	public synchronized void setPort(int port, boolean immediate) {
		if(port >= 0){
			this.port = port;
			if(immediate){
				try {
					stop();
					start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public synchronized int getConfiguredPort() {
		return port;
	}

	@Override
	public synchronized int getCurrentPort() {
		if(socket == null)
			return -1;
		return socket.getLocalPort();
	}

	@Override
	protected void doStart() throws IOException {
		//Try to create socket
		try {
			socket = new ServerSocket(port);
			System.out.println("The server socket has been opened on port " + port);
		} catch (IOException e) {
			System.err.println("Unable to create server socket on port " + port);
			//Try the range if the default port failed
			int listenPort = minPort;
			while(socket == null && port <= maxPort){
				try {
					socket = new ServerSocket(listenPort);
					System.out.println("The server socket has been opened on port " + listenPort);
				} catch (IOException e1) {
					System.err.println("Unable to create server socket on port " + listenPort);
					if(listenPort < maxPort)
						listenPort += listenPortIncrement;
					else
						throw new IOException("Unable to create server socket on ports " + port + ", " + listenPort, e1);
				}
			}
		}
		
		socket.setSoTimeout(1000);
		shouldAccept = true;
		shouldPause = false;
		paused = false;
		accepting = true;
		
		//Run the server
		executor.execute(new Runnable(){
			@Override
			public void run() {
				while(shouldAccept){
					accepting = true;
					while(shouldPause && shouldAccept){
						paused = true;
						Thread.yield();
					}
					paused = false;
					if(!shouldAccept)
						break;
					
					//Wait for an incoming connection
					Socket clientSock = null;
					try {
						clientSock = socket.accept();
					}
					catch(SocketTimeoutException e){
						continue;
					}
					catch (IOException e) {
						System.err.println("Unable to accept connection from client.");
						continue;
					}
					
					//Dispatch the incoming connection to a new session handler
					Connection c = null;
					try{
						c = node.getConnectionFactory().registerConnection(clientSock);
						ServerSessionHandler handler = new ServerSessionHandler(node, clientSock);
						handler.start();
					} catch(OutOfMemoryError e){//Autorecover from resource consumption
						System.err.println("Unable to create thread for new connection");
						c.release();
					}
				}
				accepting = false;
			}
		});
	}

	@Override
	protected void doPause() {
		shouldPause = true;
		while(!paused)
			Thread.yield();
	}

	@Override
	protected void doResume() {
		shouldPause = false;
		while(paused)
			Thread.yield();
	}

	@Override
	protected void doStop() throws IOException {
		shouldAccept = false;
		while(accepting)
			Thread.yield();
		socket.close();
		socket = null;
	}

	@Override
	public void link() {
		
	}

	@Override
	public void unlink() {
		
	}

}
