package edu.ucr.sensorserver;

import java.util.ArrayList;

import smarttools.ucr.edu.remotesensors.server.ServerThread;

public class SensorServer
{
	private static final int START_PORT = 8080;
	private static int threadCount = 0;
	
	private static ArrayList<Thread> mThreads = new ArrayList<Thread>();
	private static ArrayList<ServerThread> mServerThreads = new ArrayList<ServerThread>();
	
	/**
	 * @param argv
	 */
	public static void main(String[] argv)
	{
		mServerThreads.add(new ServerThread(START_PORT, 1000));
		mThreads.add(new Thread(mServerThreads.get(0), "T" + threadCount));
		mThreads.get(0).start();
		threadCount++;
		
		while(true)
		{
			
			/** Check to see if all Threads are busy and spawn a new one if it is */
			int connectionCount = 0;
			for(int i = 0; i < threadCount; ++i)
			{
				ServerThread tempServerThread = mServerThreads.get(i);
				if(tempServerThread.isConnected())
				{
					connectionCount++;
				}
			}
			if(connectionCount >= threadCount)
			{
				ServerThread tempServerThread = new ServerThread(START_PORT + threadCount, 1000);
				mServerThreads.add(tempServerThread);
				mThreads.add(new Thread(tempServerThread, "T" + threadCount));
				mThreads.get(threadCount).start();
				threadCount++;
			}
			
			/** Read from stdout */

			
		}
		
		//Setup so there's always one ServerThread open with the next port up waiting on new connections.
		
		//Initially create one serverThread and start it.
		//Once it is connected add a new one to the arraylist
		//Continue.
		//If it's disconnected, remove it from the list.
	}

}
