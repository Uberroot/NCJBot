package com.github.uberroot.ncjbot.modules;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;

import com.github.uberroot.ncjbot.LocalNode;
import com.github.uberroot.ncjbot.RemoteNode;
import com.github.uberroot.ncjbot.modapi.*;

public class BasicConnector extends AbstractModule implements ConnectionFactory {
	
	public class BasicConnection implements Connection{
		private Socket socket;
		private int retainCount;
		
		/**
		 * <p>The vector for holding all event listeners</p>
		 */
		private Vector<EventListener> listeners;
		
		private BasicConnection(Socket s){
			retainCount = 1;
			socket = s;
			listeners = new Vector<EventListener>();
		}
		
		private BasicConnection(InetAddress addr, int port) throws IOException{
			retainCount = 1;
			socket = new Socket(addr, port);
			listeners = new Vector<EventListener>();
		}

		@Override
		public synchronized void write(byte[] data) throws IOException {
			try{
				socket.getOutputStream().write(data);
			}
			catch(IOException ex){
				socket.close();
				retainCount = 0;
				
				Vector<EventListener> temp = new Vector<EventListener>(listeners);
				for(EventListener l : temp)
					l.connectionFailed(this);
				
				throw ex;
			}
		}

		@Override
		public synchronized int read(byte[] data) throws IOException {
			try{
				return socket.getInputStream().read(data);
			}
			catch(IOException ex){
				socket.close();
				retainCount = 0;
				
				Vector<EventListener> temp = new Vector<EventListener>(listeners);
				for(EventListener l : temp)
					l.connectionFailed(this);
				
				throw ex;
			}
		}

		@Override
		public synchronized void addListener(EventListener listener) {
			listeners.add(listener);
		}

		@Override
		public synchronized void removeListener(EventListener listener) {
			listeners.remove(listener);
		}

		@Override
		public void retain() {
			retainCount++;
		}

		@Override
		//TODO: Should this throw an exception if overreleased?
		public void release() {
			retainCount--;
			if(retainCount < 1){
				try {
					socket.close();
				} catch (IOException e) {}
				Vector<EventListener> temp = new Vector<EventListener>(listeners);
				for(EventListener l : temp)
					l.connectionClosed(this);
			}
		}

		@Override
		public synchronized void write(byte[] data, int off, int len) throws IOException {
			try{
				socket.getOutputStream().write(data, off, len);
			}
			catch(IOException ex){
				socket.close();
				retainCount = 0;
				
				Vector<EventListener> temp = new Vector<EventListener>(listeners);
				for(EventListener l : temp)
					l.connectionFailed(this);
				
				throw ex;
			}
		}
		
	}

	public BasicConnector(LocalNode node) {
		super(node);
	}

	@Override
	public Connection registerConnection(Socket s) {
		return new BasicConnection(s);
	}

	@Override
	public Connection getConnection(RemoteNode node) throws IOException {
		return new BasicConnection(node.getIpAddress(), node.getListeningPort());
	}

	@Override
	public void link() {}

	@Override
	public void unlink() {}
}
