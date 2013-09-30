package edu.ucr.sensorservergui;

public class Coordinate
{
	private double mLatitude;
	private double mLongitude;
	
	public Coordinate(double latitude, double longitude)
	{
		mLatitude = latitude;
		mLongitude = longitude;
	}
	
	public double getLatitude()
	{
		return mLatitude;
	}
	
	public double getLongitude()
	{
		return mLongitude;
	}
	
	public String toString()
	{
		return Double.toString(mLatitude) + "," + Double.toString(mLongitude); 
	}
}
