package edu.ucr.arduinoioio;

public class Movement
{
	private int mLSpeed;
	/** Time at that speed in mS **/
	private int mLTime;
	private int mRSpeed;
	private int mRTime;
	
	public Movement(int lSpeed, int lTime, int rSpeed, int rTime)
	{
		mLSpeed = lSpeed;
		mLTime = lTime;
		mRSpeed = rSpeed;
		mRTime = rTime;
	}
	
	public void reverse()
	{
		mLSpeed = -mLSpeed;
		mRSpeed = -mRSpeed;
	}
	
	public int getLSpeed()
	{
		return mLSpeed;
	}
	
	public int getRSpeed()
	{
		return mRSpeed;
	}
	
	public int getLTime()
	{
		return mLTime;
	}
	
	public int getRTime()
	{
		return mRTime;
	}
	
	public void setLTime(int lTime)
	{
		mLTime = lTime;
	}
	
	public void setRTime(int rTime)
	{
		mRTime = rTime;
	}
}
