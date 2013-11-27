package com.github.uberroot.ncjbot;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * <p>This class handles all incoming requests. Once a session is established,
 * it is passed to a handler to run in its own thread.</p>
 * 
 * @author Carter Waxman
 *
 */
//TODO: Look into using SocketChannel for non-blocking socket handling
//TODO: It should not be possible to instantiate this from any class but LocalNode. (Security)
//TODO: This shouldn't be a Thread subclass as it exposes public methods through thread enumeration
public final class ServerThread extends Thread{
	/**
	 * <p>The running LocalNode instance.</p>
	 */
	private LocalNode node;
	
	/**
	 * <p>The port on which the server listens.</p>
	 */
	private int port;
	
	/**
	 * <p>The socket used to listen for incoming connections.</p>
	 */
	private ServerSocket serverSock = null;
	
	/**
	 * <p>Whether the server is listening for and accepting connections.</p>
	 */
	private boolean listening = false;
	
	/**
	 * <p>Creates a ServerThread for handling incoming connections and automatically starts the 
	 * listening on the port specified.</p>
	 * 
	 * @param node The running LocalNode instance.
	 * @param port The port on which to listen.
	 * @throws IOException
	 */
	public ServerThread(LocalNode node, int port) throws IOException{
		this.node = node;
		this.port = port;
		setName("Server Connection Dispatcher - " + port);
		makeServerSock();
		listening = true;
	}
	
	/**
	 * <p>Creates a ServerSocket and begins listening on the listening port for this ServerThread.</p>
	 * 
	 * @throws IOException
	 */
	private void makeServerSock()throws IOException{
		//Try to create socket
		try {
			serverSock = new ServerSocket(port);
			System.out.println("The server socket has been opened on port " + port);
		} catch (IOException e) {
			throw new IOException("Unable to create server socket on port " + port);
		}
	}
	
	/**
	 * <p>Continuously listens for incoming connections and dispatches them to ServerSessionHandlers.</p>
	 */
	@Override
	public void run(){
		while(listening){
			//Create the socket if it isn't already listening
			//TODO: Is this even necessary?
			if(serverSock == null){
				try {
					makeServerSock();
				} catch (IOException e3) {
					serverSock = null;
					continue;
				}
			}
			
			//Wait for an incoming connection
			Socket clientSock = null;
			try {
				clientSock = serverSock.accept();
			}
			catch(SocketException e){
				if(serverSock.isClosed())
					serverSock = null;
				continue;
			}
			catch (IOException e) {
				System.err.println("Unable to accept connection from client.");
				continue;
			}
			
			//Dispatch the incoming connection to a new session handler
			try{
				ServerSessionHandler handler = new ServerSessionHandler(node, clientSock);
				handler.start();
			} catch(OutOfMemoryError e){//Autorecover from resource consumption
				System.err.println("Unable to create thread for new connection");
				try {
					clientSock.close();
				} catch (IOException e1) {
					//This is bad...
					e1.printStackTrace();
					System.exit(-1);
				}
				
			}
		}
	}
	
	/**
	 * <p>Checks if the server socket is currently listening for connections.</p>
	 * 
	 * @return True if the server socket is listening for incoming connections, false if it is not.
	 */
	public boolean isRunning(){
		return listening;
	}
	
	/**
	 * <p>Shuts down the server socket, preventing new connections from being established. Previous requests that
	 * were running at the time of shutdown will not be affected.</p>
	 */
	public void kill(){
		listening = false;
		try {
			serverSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("The server socket has closed");
	}
}
