package smarttools.ucr.edu.remotesensors.server.test;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientThreadTest implements Runnable{

	private String serverIpAddress;

	private int serverPort;

	Socket serverSocket;

	public static void main(String args[]){
		ClientThreadTest ct = new ClientThreadTest("10.0.0.126", 8080);
		
		for(int i = 0; i < 100; i++){
			ct.run();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ct.close();

	}

	public ClientThreadTest(String serverIpAddress, int serverPort){
		this.serverIpAddress = serverIpAddress;
		this.serverPort = serverPort;


		InetAddress serverAddr = null;
		try {
			serverAddr = InetAddress.getByName(this.serverIpAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		try {
			this.serverSocket = new Socket(serverAddr, this.serverPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			System.out.println("C: Sending command.");
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream())), true);

			out.println("unique_id,location,1234");

			System.out.println("C: Closed.");
		} catch (Exception e) {
			System.out.println("S: Error");	
		}
	}
	
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}