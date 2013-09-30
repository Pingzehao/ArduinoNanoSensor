package edu.ucr.virtualsensorclient16;

import java.util.Random;

import smarttools.ucr.edu.remotesensors.ClientConnect;
import smarttools.ucr.edu.remotesensors.common.Datagram;

public class VirtualSensorClient16
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
		Datagram init = new Datagram("init", 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 100, 0, 0, 0, 0);
		ct.send(init);
		if(iterations == 0)
		{
			Random r = new Random();
			double latDeviation = (double) (r.nextInt(20) - 10) / 10000;
			double longDeviation = (double) (r.nextInt(20) - 10) / 10000;
			double latitude = 33.9752 + latDeviation;
			double longitude = -117.3267 + longDeviation;
			while(true)
			{
				int d1 = r.nextInt(10) + 50;
				int d2 = r.nextInt(10) + 100;
				int d3 = r.nextInt(10) + 150;
				int d4 = r.nextInt(10) + 200;
				int d5 = r.nextInt(10) + 250;
				int d6 = r.nextInt(10) + 300;
				int d7 = r.nextInt(10) + 350;
				int d8 = r.nextInt(10) + 400;
				int d9 = r.nextInt(10) + 450;
				int d10 = r.nextInt(10) + 500;
				int d11 = r.nextInt(10) + 550;
				int d12 = r.nextInt(10) + 600;
				int d13 = r.nextInt(10) + 650;
				int d14 = r.nextInt(10) + 700;
				int d15 = r.nextInt(10) + 750;
				int d16 = r.nextInt(10) + 800;
				int d17 = r.nextInt(10) + 850;
				int d18 = 512;
				int d19 = 512;
				Datagram d = new Datagram(argv[2], d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, latitude, longitude);
				ct.send(d);
				Thread.sleep(100);
			}
		}
		for(int i = 0; i < iterations; ++i)
		{
			Random r = new Random();
			Datagram d = new Datagram(argv[2], r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), 
					r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), 
					r.nextInt(1023), r.nextInt(1023), r.nextInt(1023), r.nextInt(1023),  r.nextInt(1023), r.nextDouble(), r.nextDouble());
			ct.send(d);
			Thread.sleep(100);
		}
		ct.close();
	}

}
