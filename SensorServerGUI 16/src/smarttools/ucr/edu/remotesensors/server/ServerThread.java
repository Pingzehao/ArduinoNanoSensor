/**
 * This class contains functionality to run a simple socket server which accepts
 * a streaming connection from a single client. The main function is written to
 * allow this program to be used as a command line executable.
 * 
 * @author jhero
 */

package smarttools.ucr.edu.remotesensors.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerThread implements Runnable{

	/** The port the server will listen on */
	private int port;
	
	/** The amount of time(ms) to wait on reads before timing out */
	private int timeout; //ms

	/** Connection status of the thread */
	private boolean connected;
	
	/** Ip of the connected client */
	private String clientAddress;
	
	/** Pipe out to communicate with the drawing thread */
	private PipedWriter mPipeOut;
	
	public ServerThread(int port, int timeout){
		this.port = port;
		this.timeout = timeout;
		mPipeOut = new PipedWriter();
	}
	

	public boolean isConnected()
	{
		return connected; 
	}
	
	public String getIP()
	{
		return clientAddress;
	}

	public PipedWriter getPipedWriter()
	{
		return mPipeOut;
	}
	
	/** 
	 * Runs an instance of the server infinitely awaiting connections and outputting the data transmitted.
	 * Any data that is received from clients is written to standard err. 
	 */
	public void run() {

		/** The socket this server will listen to */
		ServerSocket serverSocket = null;
		
		/** The incoming client socket that will stream data */
		Socket clientSocket = null;

		/** Used to format the date in the log messages */
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		/** Used to keep track of the current date/time */
		Date date;
		
		/** Used to output the current date/time */
		String dateStr;

		/** Loop forever waiting for client connections */
		while(true){
			connected = false;
			/** Create the server socket */
			try{
				serverSocket = new ServerSocket(port);
			}
			catch (IOException e) {
				System.out.println(e);
			}   
			try {
				
				/** Wait for and accept incoming connections */
				date = new Date();
				dateStr = dateFormat.format(date);
				System.err.println("[" + dateStr + "]- Awaiting connections...");
				clientSocket = serverSocket.accept();
				clientSocket.setTcpNoDelay(true);
				serverSocket.setSoTimeout(timeout);
				
			} catch (Exception e){
				date = new Date();
				dateStr = dateFormat.format(date);
				System.err.println("[" + dateStr + "]- Error accepting socket.");
			}
			
			try{
				/** Get basic information about the incoming connection */
				clientAddress = clientSocket.getInetAddress().toString();
				date = new Date();
				dateStr = dateFormat.format(date);
				System.err.println("[" + dateStr + "]- Client accepted: " + clientAddress);
				connected = true;
				/** Create an object to read data coming off the connection */
				BufferedReader bis = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				/** Read data from the client for as long as possible */
				while(true){
					/** Read data from the socket */
					String line = bis.readLine();
					date = new Date();
					dateStr = dateFormat.format(date);
					
					if(line == null)
					{
						System.err.println("[" + dateStr + "]- Client disconnected: " + clientAddress);
						clientSocket.close();
						serverSocket.close();
						connected = false;
						break;
					}
					
					System.err.println("[" + dateStr + "]- Received data: " + line);
					
					/** Write the datagram information to a pipe out */
					line += "\n";
					mPipeOut.write(line);
					mPipeOut.flush();
				}
			} catch (Exception e) {
				date = new Date();
				dateStr = dateFormat.format(date);
				System.err.println("[" + dateStr + "]- Error receiving data.");
			}
		}
	}

}
