package com.github.uberroot.ncjbot;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

/**
 * <p>Handles communications from a client and closes the session when finished.</p>
 * 
 * @author Carter Waxman
 *
 */
//TODO: Implement SO_TIMEOUTs and SO_KEEPALIVEs
//TODO: The multithreaded approach may be replaced with socket channels
//TODO: This shouldn't be a Thread subclass as it exposes public methods through thread enumeration
public final class ServerSessionHandler extends Thread{
	/**
	 * <p>The running ProcessorNode instance.</p>
	 */
	private ProcessorNode node;
	
	/**
	 * <p>A simple counter for the handlers that spawn, allowing for identification of individual server sessions.</p>
	 */
	private static int idcount = 0;
	
	/**
	 * <p>A socket allowing communication with the client</p>
	 */
	private Socket clientSock;
	
	/**
	 * <p>Instantiates a ServerSessionHandler with a socket to use for client communication.</p>
	 * 
	 * @param node The running ProcessorNode instance.
	 * @param clientSock A socket for client communication.
	 */
	public ServerSessionHandler(ProcessorNode node, Socket clientSock){
		this.node = node;
		this.clientSock = clientSock;
		setName("Server Session Handler " + idcount++ + " (" + clientSock.getInetAddress() + ":" + clientSock.getPort() + ")");
	}
	
	/**
	 * <p>Runs the protocol handling loop for accepting commands and responding accordingly.</p>
	 */
	//TODO: The text-based protocol will likely be replaced with a leaner binary protocol
	@Override
	public void run(){
		InputStream in = null;
		try {
			in = clientSock.getInputStream();//new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
		} catch (IOException e2) {}
		while(true){
			char cBuffer[] = new char[1500];
			byte buffer[] = new byte[1500];
			try {
				int r = in.read(buffer);
				if(r == -1)
					break;
				for(int i = 0; i < 1500; i++)
					cBuffer[i] = (char)buffer[i];
			} catch (IOException e1) {}
			//System.out.println("..." + String.valueOf(buffer).trim() + "...");
			if(String.valueOf(cBuffer).trim().equals("Goodbye."))
				break;
			if(String.valueOf(cBuffer).trim().equals("Are you alive?")){
				try {
					switch(node.getState()){
						case RUNNING:{
							clientSock.getOutputStream().write("I'm not dead yet.".getBytes());
							break;
						}
						case SHUTTING_DOWN:{
							clientSock.getOutputStream().write("I'm bleeding out.".getBytes());
							break;
						}
						case UNKNOWN:{
							clientSock.getOutputStream().write("I'm not okay.".getBytes());
							break;
						}
					}
				} catch (IOException e) {
					System.err.println("Unable to respond.");
				}
			}
			else if(String.valueOf(cBuffer).trim().equals("Who do you know?")){
				try {
					List<RemoteNode> nodes = node.getNetworkManager().getActiveNodes();
					String toSend = "\n";
					for(RemoteNode n : nodes)
						toSend += n.getIpAddress().getHostAddress() + ":" + n.getListeningPort() + "\n";
					clientSock.getOutputStream().write(toSend.getBytes());
					System.out.println("Active node list retreived for " + clientSock.getInetAddress().getHostAddress() + ":" + clientSock.getPort());
				} catch (IOException e) {
					System.err.println("Unable to respond.");
				}
			}
			else if(String.valueOf(cBuffer).trim().matches("I'm here.\n\\d+")){
				String port = String.valueOf(cBuffer).trim().split("\n")[1];
				try {
					RemoteNode rn = new RemoteNode(node, clientSock.getInetAddress().getHostAddress(), Integer.valueOf(port));
					if(node.addDiscoveredNode(rn)){
						System.out.println("Found new node: " + rn.getIpAddress().toString() + ":" + port);
						clientSock.getOutputStream().write("Got it.".getBytes());
					}
					else
						clientSock.getOutputStream().write("Hey I know you.".getBytes());
				} catch (NumberFormatException e) {
				} catch (UnknownHostException e) {
				} catch (IOException e) {
					System.err.println("Unable to respond.");
				}
			}
			else if(String.valueOf(cBuffer).trim().matches("I just met\n\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d+")){
				String connString = String.valueOf(cBuffer).trim().split("\n")[1];
				try {
					String[] pair = connString.split(":");
					RemoteNode rn = new RemoteNode(node, pair[0], Integer.valueOf(pair[1]));
					if(node.addDiscoveredNode(rn)){
						System.out.println("Found new node: " + pair[0] + ":" + pair[1]);
						clientSock.getOutputStream().write("Got it.".getBytes());
					}
					else
						clientSock.getOutputStream().write("Hey I know you.".getBytes());
				} catch (NumberFormatException e) {
				} catch (UnknownHostException e) {
				} catch (IOException e) {
					System.err.println("Unable to respond.");
				}
			}
			else if(String.valueOf(cBuffer).trim().equals("I have a job for you.")){
				try {
					clientSock.getOutputStream().write("What will I need?".getBytes());
					
					//Get the remote port
					int remotePort = Integer.valueOf(readLine(in).trim());
					
					//Add the remote node to the known list if it does not exist
					RemoteNode rn = new RemoteNode(node, clientSock.getInetAddress().getHostAddress(), remotePort);
					node.addDiscoveredNode(rn);
					
					//Get the remote process id
					long remoteId = Long.valueOf(readLine(in).trim());
					
					//Get the name of the worker class
					String workerName = readLine(in).trim();
					
					//Make a place for the class
					Random rand = new Random();
					String dirLoc = "workers/" + System.currentTimeMillis() + "_" + remoteId + "_" + rand.nextLong() + "/";
					if(!(new File(dirLoc)).mkdirs())
						System.err.println("unable to make " + dirLoc);
					
					//Get the lengths
					int paramLen = Integer.valueOf(readLine(in).trim());
					long workerLen = Long.valueOf(readLine(in).trim());
					
					//Download the initialization data
					byte params[] = new byte[paramLen];
					//BufferedInputStream byteIn = new BufferedInputStream(clientSock.getInputStream());
					in.read(params, 0, paramLen);
					FileOutputStream fos = new FileOutputStream(dirLoc + "initData");
					fos.write(params);
					fos.close();
					
					//Download the class
					File classFile = new File(dirLoc + workerName + ".class");
					fos = new FileOutputStream(classFile);
					byte fbuffer[] = new byte[4096];
					int read = 0;
					for(long total = 0; total < workerLen; total += read){
						if(workerLen - total < 4096)
							read = in.read(fbuffer, 0, (int)(workerLen - total));
						else
							read = in.read(fbuffer, 0, 4096);
						if(read == -1){
							read = 0;
							continue;
						}
						fos.write(fbuffer, 0, read);
					}
					fos.flush();
					fos.close();
					
					//Run the job
					
					long id = node.startJob(dirLoc, workerName, rn, Long.toString(remoteId), new File(dirLoc + "initData"), true);
					
					//Return the id
					clientSock.getOutputStream().write((id + "\n").getBytes());
				} catch (IOException e) {
					System.err.println("Unable to respond.");
				}
			}
			else if(String.valueOf(cBuffer).trim().equals("I have results.")){
				try {
					clientSock.getOutputStream().write("What did you find?".getBytes());
					
					//Get the remote port
					int remotePort = Integer.valueOf(readLine(in).trim());
					
					//Add the remote node to the known list if it does not exist
					RemoteNode rn = new RemoteNode(node, clientSock.getInetAddress().getHostAddress(), remotePort);
					node.addDiscoveredNode(rn);
					
					//Get the destination processes id
					String destPid = readLine(in).trim();
					
					//Get the remote's process id
					String sourcePid = readLine(in).trim();
					
					//Make a place for the returned data
					Random rand = new Random();
					String dirLoc = "results/" + rn.getIpAddress().getHostAddress() + "_" + rn.getListeningPort() + "/";
					if(!new File(dirLoc).exists())
						if(!(new File(dirLoc)).mkdirs())
							System.err.println("unable to make " + dirLoc);
					
					//Get the length of the returned data
					long dataLen = Long.valueOf(readLine(in).trim());
					
					//Download the data
					File dataFile = new File(dirLoc + System.currentTimeMillis() + "_" + destPid + "_" + sourcePid + "_" + rand.nextLong());
					FileOutputStream fos = new FileOutputStream(dataFile);
					byte fbuffer[] = new byte[4096];
					int read = 0;
					for(long total = 0; total < dataLen; total += read){
						if(dataLen - total < 4096)
							read = in.read(fbuffer, 0, (int)(dataLen - total));
						else
							read = in.read(fbuffer, 0, 4096);
						if(read == -1){
							read = 0;
							continue;
						}
						fos.write(fbuffer, 0, read);
					}
					fos.flush();
					fos.close();
					
					//Send the data to the process
					node.sendData(destPid, sourcePid, rn, dataFile);
					
					//Cleanup the file
					dataFile.delete();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			clientSock.close();
		} catch (IOException e) {}
	}
	
	/**
	 * <p>A helper function for reading a line of input from the given stream. The position of the stream
	 * will be that of the byte immediately following the first newline character.
	 * 
	 * @param is The stream to read.
	 * @return A String of all characters up to and including the first newline encountered.
	 * @throws IOException See {@link InputStream#read(byte[], int, int)}.
	 */
	//TODO: Although this method may no longer be necessary with a new protocol, it would be more efficient to read blocks using mark/reset on the stream.
	private String readLine(InputStream is) throws IOException{
		String tempString = "";
		char c;
		byte b[] = new byte[1];
		do{
			is.read(b, 0, 1);
			c = (char) b[0];
			tempString += c;
		} while(c != '\n');
		return tempString;
	}
}
