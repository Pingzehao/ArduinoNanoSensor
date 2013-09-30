package edu.ucr.sensorservergui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class MapRequest
{
	private String mMapRequestURL = "http://maps.google.com/maps/api/staticmap?size=500x500&zoom=18&format=jpg&sensor=true";
	
	private ArrayList<Coordinate> mCoordinates = new ArrayList<Coordinate>();
	
	public MapRequest()
	{
	}
	
	public void addMarkers(ArrayList<Coordinate> markerCoordinates)
	{
		mCoordinates = markerCoordinates;
	}
	
	public BufferedImage getMapImage()
	{
		String URLString = generateURL();
		BufferedImage image = null;
		try
		{
			image = ImageIO.read(new URL(URLString));
		}
		catch(MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return image;
	}
	
	private String generateURL()
	{ 
		String mapMarkers = "";
		char labelChar = '0';
		for(int i = 0; i < mCoordinates.size(); ++i)
		{
			mapMarkers += "&markers=label:" + labelChar + '|' + mCoordinates.get(i).toString();
			labelChar++;
			if(labelChar > '9' && labelChar < 'A')
			{
				labelChar = 'A';
			}
		}
		return mMapRequestURL + mapMarkers;
	}
}	
