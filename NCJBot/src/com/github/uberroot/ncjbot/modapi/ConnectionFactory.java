package com.github.uberroot.ncjbot.modapi;

import java.io.IOException;
import java.net.Socket;

import com.github.uberroot.ncjbot.RemoteNode;

/**
 * <p>This interface defines the API for establishing and utilizing connections with other nodes, abstracting
 * the underlying socket usage.</p>
 * 
 * @author Carter Waxman
 *
 */
public interface ConnectionFactory {
	
	/**
	 * <p>This interface defines the functionality for utilizing a connection with a RemoteNode. Connections use the concept
	 * of a retain count. When a connection is created, it should have a retain count of 1 (indicating a single user). When a connection
	 * needs to be used, it should be retained, incrementing the retain count. When finished, the user should release the connection, decrementing
	 * the retain count. A connection with a retain count more than 0 is guaranteed to remain open until it fails or is fully released.</p>
	 * 
	 * @author Carter Waxman
	 *
	 */
	//TODO: The protocol handling code will be separated from this interface
	public interface Connection{
		
		/**
		 * <p>A listener for events that may occur with the connection.</p>
		 * 
		 * @author Carter Waxman
		 */
		public interface EventListener extends java.util.EventListener{
			/**
			 * <p>Indicates the connection closed prematurely.</p>
			 * 
			 * @param connection The connection.
			 */
			public void connectionFailed(Connection connection);
			
			/**
			 * <p>Indicates the connection closed gracefully.</p>
			 * 
			 * @param connection The connection.
			 */
			public void connectionClosed(Connection connection);
		};
		
		/**
		 * <p>Writes the entire array of data to the stream destined for the RemoteNode.</p>
		 * 
		 * @param data The data to write.
		 * @throws IOException 
		 */
		public void write(byte[] data) throws IOException;
		
		/**
		 * <p>Writes the array of data to the stream destined for the RemoteNode.</p>
		 * 
		 * @param data The data to write.
		 * @param off The offset of the data in the array.
		 * @param len The length of the data.
		 * @throws IOException
		 */
		public void write(byte[] data, int off, int len) throws IOException;
		
		/**
		 * <p>Reads data from the stream into the provided byte array. This method may return before the byte array is filled.</p>
		 * 
		 * @param data The destination for the stream data.
		 * @return The number of bytes of data read.
		 * @throws IOException 
		 */
		public int read(byte[] data) throws IOException;
		
		/**
		 * <p>Adds a listener for connection events to the connection.</p>
		 * 
		 * @param listener The listener to add.
		 */
		public void addListener(EventListener listener);
		
		/**
		 * <p>Removes a listener for connection events from the connection.</p>
		 * 
		 * @param listener The listener to remove.
		 */
		public void removeListener(EventListener listener);
		
		/**
		 * <p>Retains the connection for an additional user.</p>
		 */
		public void retain();
		
		/**
		 * <p>Releases the connection so it may be closed if necessary.</p>
		 */
		public void release();
	};
	
	/**
	 * <p>Registers an existing socket connection to the connection manager.
	 * This is provided to allow incoming connections to register so that sockets may be reused.</p>
	 * 
	 * @param s The open socket.
	 * @return The new connection object utilizing the socket.
	 */
	public Connection registerConnection(Socket s);

	/**
	 * <p>Retrieves a Connection that can be used for communication with the RemoteNode.</p>
	 * 
	 * @param node The node to connect to.
	 * @return A Connection that can be used for communication with the RemoteNode.
	 * @throws IOException 
	 */
	public Connection getConnection(RemoteNode node) throws IOException;
}
