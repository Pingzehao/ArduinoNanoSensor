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
	
	/**
	 * Command line executable. Start a server running on a specified port with a specified timeout.
	 * @param args[0]: the port number the server will listen to
	 * @param args[1]: the amount of time to wait before timing out on a socket read
	 */
	public static void main(String[] args){
		/** If an incorrect number of parameters are specified print the usage string. */
		if(args.length != 2){
			System.err.println("Usage: java -jar ServerThread.jar <port> <timeout>");
			System.err.println("port: the port number that the server will listen to");
			System.err.println("timeout: the amount of time the server will wait for clients to send data");
			System.exit(1);
		}
		
		/** Parse the command line arguments */
		int port = Integer.parseInt(args[0]);
		int timeout = Integer.parseInt(args[1]);
		
		/** Create and run the server */
		ServerThread st = new ServerThread(port, timeout);
		st.run(); // Currently not running the server as a separate thread
	}

	public ServerThread(int port, int timeout){
		this.port = port;
		this.timeout = timeout;
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

		/** Create the server socket */
		try{
			serverSocket = new ServerSocket(port);
		}
		catch (IOException e) {
			System.out.println(e);
		}   

		/** Loop forever waiting for client connections */
		while(true){
			try {
				
				/** Wait for and accept incoming connections */
				date = new Date();
				dateStr = dateFormat.format(date);
				System.err.println("[" + dateStr + "]- Awaiting connections...");
				clientSocket = serverSocket.accept();
				serverSocket.setSoTimeout(timeout);
				
			} catch (Exception e){
				date = new Date();
				dateStr = dateFormat.format(date);
				System.err.println("[" + dateStr + "]- Error accepting socket.");
			}
			
			try{
				/** Get basic information about the incoming connection */
				String clientAddress = clientSocket.getInetAddress().toString();
				date = new Date();
				dateStr = dateFormat.format(date);
				System.err.println("[" + dateStr + "]- Client accepted: " + clientAddress);
				
				/** Create an object to read data coming off the connection */
				BufferedReader bis = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				/** Read data from the client for as long as possible */
				while(true){
					
					/** Read data from the socket */
					String line = bis.readLine();
					date = new Date();
					dateStr = dateFormat.format(date);
					System.err.println("[" + dateStr + "]- Received data: " + line);
					
					/** Write the datagram information to standard out */
					if(line != null)
						System.out.println(line);
					
				}
				
			} catch (Exception e) {
				date = new Date();
				dateStr = dateFormat.format(date);
				System.err.println("[" + dateStr + "]- Error receiving data.");
			}
		}
	}

}
