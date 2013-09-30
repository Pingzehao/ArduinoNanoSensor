package edu.ucr.virtualsensorclient;

import java.util.Random;

import smarttools.ucr.edu.remotesensors.ClientConnect;
import smarttools.ucr.edu.remotesensors.common.Datagram;

public class VirtualSensorClient
{
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] argv) throws InterruptedException
	{
		if(argv.length != 4)
		{
			System.err.println("args[0] - Server IP");
			System.err.println("args[1] - Server Port");
			System.err.println("args[2] - Client ID");
			System.err.println("args[3] - Number of data samples to send");
			System.exit(1);
		}
		
		int port = Integer.parseInt(argv[1]);
		ClientConnect ct = new ClientConnect(argv[0], port);

		int iterations = Integer.parseInt(argv[3]);
		Datagram init = new Datagram("init", 128, 128, 128, 128, 100, 0, 0, 0, 0);
		ct.send(init);
		if(iterations == 0)
		{
			
			while(true)
			{
				Random r = new Random();
				int d1 = r.nextInt(10) + 50;
				int d2 = r.nextInt(10) + 150;
				int d3 = r.nextInt(10) + 250;
				int d4 = r.nextInt(10) + 350;
				int d5 = r.nextInt(10) + 150;
				int d6 = 512;
				int d7 = 512;
				double latitude = 33.9752;
				double longitude = -117.3267;
				
				Datagram d = new Datagram(argv[2], d1, d2, d3, d4, d5, d6, d7, latitude, longitude);
				ct.send(d);
				Thread.sleep(100);
			}
		}
		for(int i = 0; i < iterations; ++i)
		{
			Random r = new Random();
			Datagram d = new Datagram(argv[2], r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextDouble(), r.nextDouble());
			ct.send(d);
			Thread.sleep(100);
		}
		ct.close();
	}

}
