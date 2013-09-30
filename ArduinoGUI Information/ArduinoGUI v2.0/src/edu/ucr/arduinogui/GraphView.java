package edu.ucr.arduinogui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphView extends SurfaceView implements SurfaceHolder.Callback, Serializable
{
	private static final long serialVersionUID = 1615397792410581830L;

	/** Flag for turning on averaging of data points*/
	private static final boolean AVERAGE_DATA = false;
	
	/** Number of data points to average */
	private static final int POINTS_TO_AVERAGE = 5;
	
	/** Acceptance ratio is the max change in percentage from the last data point. Used to
	 * reject random spikes read from the device
	 */
	private static final double ACCEPTANCE_RATIO = 0.25;
	
	/** Number of ADC pins on the Arduino */
	private static final int ANALOG_PINS = 8;

	/** Constants used to determine which data points to draw, based on the AnalogIn pin number */
	private static final int CHIP = 4;
	private static final int MQ2 = 5;
	private static final int HUMIDITY = 6;
	private static final int TEMPERATURE = 7;
	private static final int GPS = 8;

	/** Voltage put into the voltage divider */
	private static final double INPUT_VOLTAGE = 5.0;
	
	/** Reference voltage used by the Arduino for the ADC */
	private static final double REFERENCE_VOLTAGE = 5.0;
	
	private static final double MS_PER_MIN = 60000;

	/** Max resistance range for the graph to draw, used to determine the drawing scale */
	private static final double MAX_RESISTANCE = 100;

	/** Divider resistance on the commercial sensor */
	private static final double MQ2_DIVIDER_RESISTANCE = 10.0;
	
	/** Constants used to set the scale for the temperature graph */
	private static final int TEMP_OFFSET = 80;
	private static final double TEMP_WINDOW_MAX = 246.0;
	private static final double TEMP_WINDOW_MIN = 62.0;
	private static final float TEMP_GRAPH_SCALE = (float) ((float) 1023.0 / (TEMP_WINDOW_MAX - TEMP_WINDOW_MIN));

	/** Maximum percentage to be drawn, used to scale the graph */
	public static final double PERCENT_SCALE = 2.0;

	/** Email address to send an email to when the threshold is reached */
	private String mAutoEmail = "";
	
	/** Threshold percentage for an email to be automatically sent */
	private double mThresholdPercentage = 0.0;
	
	/** Flag indicating whether or not an email has been sent */
	private boolean mEmailSent = false;

	/** LocationManager object used to get GPS coordinates */
	private LocationManager mLocManager = null;
	private LocationListener mLocListener;

	public class Data
	{
		/** BitVoltage reading from the Arduino */
		public int mVoltage;
		
		/** Time in ms since the first data point */
		public double mTime;
		
		/** Resistance value calculated in kOhms */
		public double mResistance;
		
		/** Bit resistance value used for the scaling when drawing */
		public int mBitResistance;

		Data(int voltage, double time, int pin)
		{
			mVoltage = voltage;
			calculateResistance(pin);
			mTime = time;
		}

		/**
		 * Calculates the resistance and sets the mResistance value for the chip pins
		 * @param pin - Pin that the bit voltage was read on
		 */
		private void calculateResistance(int pin)
		{
			if(pin > 4)
				return;
			double outputVoltage = (double) mVoltage / 1023.0 * REFERENCE_VOLTAGE;
			mResistance = outputVoltage * mDividerResistance[pin] / (INPUT_VOLTAGE - outputVoltage);
			mBitResistance = (int) ((mResistance / MAX_RESISTANCE) * 1023);
		}
	}

	/** Root file path of the SD card */
	private static final File ROOT = Environment.getExternalStorageDirectory();

	/** Main thread used for the GraphView */
	private GraphThread mThread = null;

	/** Flag indicating the Graph should be reset */
	private boolean doReset = false;

	/** Boolean array indicating which pins to hide and not draw */
	private boolean[] mHiddenPins = new boolean[ANALOG_PINS];

	/** Current display mode of the view */
	private static int mDisplayMode = CHIP;

	/** Flag indicating whether auto scaling should be used */
	private boolean mAutoScaling = false;
	
	/** Arrays keeping track of the min and max values for each pin */
	private int[] mMinValues = new int[ANALOG_PINS];
	private int[] mMaxValues = new int[ANALOG_PINS];
	
	/** Resistance of the digital potentiometer used as a divider in the voltage divider */
	private int[] mDividerResistance = new int[CHIP + 1];
	
	/** Polling rate of the device */
	private int mPollingRate = 100;
	
	/** Values used to keep track of the scale of the window */
	private double mWindowScale = 1;
	private double mWindowMin = 0;
	private double mWindowMax = 1023;
	
	/** Ms since last draw used for calculating FPS */
	private long lastDraw = System.currentTimeMillis();
	
	/** Values used to keep track of the gps coordinates */
	private double mLatitude;
	private double mLongitude;
	private long mGPSTime;
	private String mTimeString = "Start Polling for GPS Data";

	/** Initial resistance of the commercial H2S sensor */
	private double mMQ2InitialResistance = -1;
	
	/** ArrayLists for storing the data */
	private ArrayList<ArrayList<Data>> mDataPoints = new ArrayList<ArrayList<Data>>();
	private ArrayList<ArrayList<Data>> mAveragedData = new ArrayList<ArrayList<Data>>();

	/** Paint objects for different data colors */
	private ArrayList<Paint> mPinPaints = new ArrayList<Paint>();
	Paint mAxisPaint = new Paint();
	Paint mTextPaint = new Paint();

	/** File objects for writing the data points to text files */
	private ArrayList<File> mDataFiles = new ArrayList<File>();
	private ArrayList<FileWriter> mFilesWriters = new ArrayList<FileWriter>();
	private ArrayList<BufferedWriter> mBufferedWriters = new ArrayList<BufferedWriter>();
	private File mNetworkFile;
	private File mGPSData;
	private FileWriter mNetworkWriter;
	private FileWriter mGPSDataWriter;
	private BufferedWriter mBufferedNetworkWriter;
	private BufferedWriter mBufferedGPSDataWriter;

	public GraphView(Context context)
	{
		super(context);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		mThread = new GraphThread(holder, context, new Handler());
		setFocusable(true); // need to get the key events
		initializeValues();
	}

	public GraphView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		SurfaceHolder holder = getHolder();	
		holder.addCallback(this);
		mThread = new GraphThread(holder, context, new Handler());
		setFocusable(true);
		initializeValues();
	}

	public GraphView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		mThread = new GraphThread(holder, context, new Handler());
		setFocusable(true);
		initializeValues();
	}

	public GraphThread getThread()
	{
		return mThread;
	}

	public void hidePin(int n)
	{
		mHiddenPins[n] = true;
	}

	public void unhidePin(int n)
	{
		mHiddenPins[n] = false;
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		if(mThread.getState() == Thread.State.TERMINATED)
		{
			mThread = new GraphThread(holder, null, null);
			mThread.start();
		}
		else
			mThread.start();
		mThread.setRunning(true);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		boolean retry = true;
		mThread.setRunning(false);
		while(retry)
		{
			try
			{
				mThread.join();
				retry = false;
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	class GraphThread extends Thread
	{
		private SurfaceHolder mSurfaceHolder;
		private boolean mRun = false;

		public GraphThread(SurfaceHolder holder, Context context, Handler handler)
		{
			mSurfaceHolder = holder;
		}

		public void setRunning(boolean b)
		{
			mRun = b;
		}

		public void run()
		{
			Canvas c;
			while(mRun)
			{
				c = null;
				try
				{
					c = mSurfaceHolder.lockCanvas(null);
					if(c != null)
					{
						synchronized(mSurfaceHolder)
						{
							onDraw(c);
						}
					}
				}
				finally
				{
					if(c != null)
					{
						synchronized(mSurfaceHolder)
						{
							mSurfaceHolder.unlockCanvasAndPost(c);
						}
					}
				}
			}
		}
	}

	public void initializeValues()
	{
		mTextPaint.setColor(Color.BLACK);
		mAxisPaint.setColor(Color.BLACK);
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			mPinPaints.add(new Paint());
			mMinValues[i] = 1023;
			mMaxValues[i] = 0;
		}
		mPinPaints.get(0).setColor(Color.RED);
		/** Orange */
		mPinPaints.get(1).setColor(Color.rgb(255, 165, 0));
		mPinPaints.get(2).setColor(Color.YELLOW);
		mPinPaints.get(3).setColor(Color.GREEN);
		mPinPaints.get(4).setColor(Color.BLUE);
		/** Inidigo */
		mPinPaints.get(5).setColor(Color.rgb(111, 0, 255));
		/** Violet */
		mPinPaints.get(HUMIDITY).setColor(Color.rgb(238, 130, 238));
		mPinPaints.get(TEMPERATURE).setColor(Color.BLACK);
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			mDataPoints.add(new ArrayList<Data>());
			mAveragedData.add(new ArrayList<Data>());
		}
	}

	/**
	 * Creates the files for writing the data points to
	 * @param timeStarted - time the polling was started, used for the file names
	 */
	public void createFiles(Calendar timeStarted)
	{
		try
		{
			int month = timeStarted.get(Calendar.MONTH) + 1;
			int day = timeStarted.get(Calendar.DAY_OF_MONTH);
			int hour = timeStarted.get(Calendar.HOUR_OF_DAY);
			int minute = timeStarted.get(Calendar.MINUTE);
			int millis = timeStarted.get(Calendar.MILLISECOND);
			File folder = new File(ROOT + "/ArduinoGUI/");
			folder.mkdirs();

			mDataFiles = new ArrayList<File>();
			mFilesWriters = new ArrayList<FileWriter>();
			mBufferedWriters = new ArrayList<BufferedWriter>();

			if(ROOT.canWrite())
			{
				for(int i = 0; i < ANALOG_PINS; ++i)
				{
					mDataFiles.add(new File(ROOT + "/ArduinoGUI/", month + "_" + day + "_" + hour + "_" + minute + "_" + millis + "p" + i + ".txt"));
					mFilesWriters.add(new FileWriter(mDataFiles.get(i), true));
					mBufferedWriters.add(new BufferedWriter(mFilesWriters.get(i)));
				}
				for(int i = 0; i < CHIP; ++i)
				{
					mBufferedWriters.get(i).write("Time (min)\tResistance (kOhms)");
					mBufferedWriters.get(i).newLine();
					mBufferedWriters.get(i).flush();
				}
				
				/** MQ2 file header */
				mBufferedWriters.get(MQ2).write("Time (min)\tConcentration (ppm)");
				mBufferedWriters.get(MQ2).newLine();
				mBufferedWriters.get(MQ2).flush();
				
				/** Humidity file header */
				mBufferedWriters.get(HUMIDITY).write("Time (min)\tRelative Humidity (%)");
				mBufferedWriters.get(HUMIDITY).newLine();
				mBufferedWriters.get(HUMIDITY).flush();

				/** Temperature file header */
				mBufferedWriters.get(TEMPERATURE).write("Time (min)\tTemperature (C)");
				mBufferedWriters.get(TEMPERATURE).newLine();
				mBufferedWriters.get(TEMPERATURE).flush();
			}
			
			mGPSData = new File(ROOT + "/ArduinoGUI/", month + "_" + day + "_" + hour + "_" + minute + "_" + millis + "GPSData.txt");
			mGPSDataWriter = new FileWriter(mGPSData, true);
			mBufferedGPSDataWriter = new BufferedWriter(mGPSDataWriter);
			mBufferedGPSDataWriter.write("Time\tLatitude\tLongitude");
			mBufferedGPSDataWriter.newLine();
			mBufferedGPSDataWriter.flush();
			
			mNetworkFile = new File(ROOT + "/ArduinoGUI/", "networkData");
			mNetworkWriter = new FileWriter(mNetworkFile, false);
			mBufferedNetworkWriter = new BufferedWriter(mNetworkWriter);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public void reset()
	{
		doReset = true;
	}

	/**
	 * Clears the data for the graph and resets it
	 */
	public void deleteData()
	{
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			mDataPoints.get(i).clear();
			mAveragedData.get(i).clear();
			mMaxValues[i] = 0;
			mMinValues[i] = 1023;
		}
		mMQ2InitialResistance = -1;
		mTimeString = "Start Polling for GPS Data";
		mEmailSent = false;
		doReset = false;
	}

	protected void onDraw(Canvas c)
	{
		c.drawColor(Color.WHITE);
		drawFPS(c);
		if(mAutoScaling)
			setWindow();
		drawData(c);
		drawAxis(c);
		/** Required since the data can't be deleted while accessing it */
		if(doReset)
		{
			deleteData();
		}
	}

	/**
	 * Sets the scaling of the window automatically
	 */
	public void setWindow()
	{
		switch(mDisplayMode)
		{
			case CHIP:
				int tempMax = 0;
				int tempMin = 1023;
				for(int i = 0; i < CHIP; ++i)
				{
					if(!mHiddenPins[i])
					{
						if(tempMin > mMinValues[i])
							tempMin = mMinValues[i];
						if(tempMax < mMaxValues[i])
							tempMax = mMaxValues[i];
					}
				}
				if(tempMin == tempMax)
				{
					tempMin--;
					tempMax++;
					if(tempMax > 1023)
						tempMax = 1023;
					if(tempMin < 0)
						tempMin = 0;
				}
				setWindow(tempMin, tempMax);
				break;
			case MQ2:
				setWindow(mMinValues[MQ2], mMaxValues[MQ2]);
				break;
			case HUMIDITY:
				setWindow(mMinValues[HUMIDITY], mMaxValues[HUMIDITY]);
				break;
			case TEMPERATURE:
				break;
			case GPS:
				break;
		}
	}

	public void drawFPS(Canvas c)
	{
		int frames = (int) (1000 / (System.currentTimeMillis() - lastDraw));
		lastDraw = System.currentTimeMillis();
		c.drawText("FPS: " + frames, getWidth() - 50, 20, mTextPaint);
	}

	public void drawAxis(Canvas c)
	{
		DecimalFormat df = new DecimalFormat("#.###");
		switch(mDisplayMode)
		{
			case CHIP:
				/** Draw the graph axis */
				c.drawLine(15, 5, 15, getHeight() - 5, mAxisPaint);
				c.drawLine(5, getHeight() - 15, getWidth() / 2 - 5, getHeight() - 15, mAxisPaint);
				c.drawText("" + df.format(mWindowMax / 1023 * MAX_RESISTANCE) + "kOhms", 20, 10, mAxisPaint);
				c.drawText("" + df.format(mWindowMin / 1023 * MAX_RESISTANCE) + "kOhms", 20, getHeight() - 5, mAxisPaint);

				/** Draw the delta axis */
				/** Vertical Bar, Height = (getHeight() - 20) / 2 */
				c.drawLine(getWidth() / 2, 5, getWidth() / 2, getHeight() - 15, mAxisPaint);
				/** Horizontal Bar, Length = (getWidth() - 5) - getWidth() / 2 */
				c.drawLine(getWidth() / 2, (getHeight() - 15) / 2 + 5, getWidth() - 5, (getHeight() - 15) / 2 + 5, mAxisPaint);
				c.drawText("200%", (float) (getWidth() / 2), 10, mAxisPaint);
				c.drawText("-200%", (float) (getWidth() / 2), getHeight() - 5, mAxisPaint);
				break;
			case MQ2:
				//TODO Change to use ppm instead of mWindowMax and mWindowMin
				c.drawLine(15, 5, 15, getHeight() - 5, mAxisPaint);
				c.drawLine(5, getHeight() - 15, getWidth() - 5, getHeight() - 15, mAxisPaint);
				c.drawText("" + mWindowMax, 20, 10, mAxisPaint);
				c.drawText("" + mWindowMin, 20, getHeight() - 5, mAxisPaint);
				
				c.drawLine(getWidth() / 1.5f + 5, 5, getWidth() / 1.5f + 5, getHeight() - 15, mAxisPaint);
				break;
			case HUMIDITY:
				//TODO Change to draw humidity % also update in other version
				c.drawLine(15, 5, 15, getHeight() - 5, mAxisPaint);
				c.drawLine(5, getHeight() - 15, getWidth() - 5, getHeight() - 15, mAxisPaint);
				c.drawText("" + mWindowMax, 20, 10, mAxisPaint);
				c.drawText("" + mWindowMin, 20, getHeight() - 5, mAxisPaint);
				
				c.drawLine(getWidth() / 1.5f + 5, 5, getWidth() / 1.5f + 5, getHeight() - 15, mAxisPaint);
				break;
			case TEMPERATURE:
				c.drawLine(15, 5, 15, getHeight() - 5, mAxisPaint);
				c.drawLine(5, getHeight() - 15, getWidth() - 5, getHeight() - 15, mAxisPaint);
				c.drawText(df.format(TEMP_WINDOW_MAX / 1023 * 5 * 100 - TEMP_OFFSET) + "C", 20, 10, mAxisPaint);
				c.drawText(df.format(TEMP_WINDOW_MIN / 1023 * 5 * 100 - TEMP_OFFSET) + "C", 20, getHeight() - 5, mAxisPaint);
				
				c.drawLine(getWidth() / 1.5f +5, 5, getWidth() / 1.5f + 5, getHeight() - 15, mAxisPaint);
				break;
			case GPS:
				drawGPSData(c);
				break;
		}

		int lastIndex;

		if(AVERAGE_DATA)
		{
			lastIndex = mAveragedData.get(TEMPERATURE).size() - 1;
			if(lastIndex < 0)
				return;
			c.drawText("" + df.format(mAveragedData.get(0).get(lastIndex).mTime / 2) + "m", (float) ((getWidth() - 50) / 4.1), getHeight() - 5, mAxisPaint);
			c.drawText("" + df.format(mAveragedData.get(0).get(lastIndex).mTime) + "m", (float) ((getWidth() - 50) / 2.1), getHeight() - 5, mAxisPaint);
		}
		else
		{
			lastIndex = mDataPoints.get(TEMPERATURE).size() - 1;
			if(lastIndex < 0)
				return;
		}
		
		switch(mDisplayMode)
		{
			case CHIP:
				c.drawText("" + df.format(mDataPoints.get(0).get(lastIndex).mTime / 2) + "m", (float) ((getWidth() - 50) / 4.1), getHeight() - 5, mAxisPaint);
				c.drawText("" + df.format(mDataPoints.get(0).get(lastIndex).mTime) + "m", (float) ((getWidth() - 50) / 2.1), getHeight() - 5, mAxisPaint);
				break;
			case MQ2:
				c.drawText("" + df.format(mDataPoints.get(0).get(lastIndex).mTime / 2) + "m", (float) ((getWidth() - 50) / 3.0), getHeight() - 5, mAxisPaint);
				c.drawText("" + df.format(mDataPoints.get(0).get(lastIndex).mTime) + "m", (float) ((getWidth() - 50) / 1.5), getHeight() - 5, mAxisPaint);
				break;
			case HUMIDITY:
				c.drawText("" + df.format(mDataPoints.get(0).get(lastIndex).mTime / 2) + "m", (float) ((getWidth() - 50) / 3.0), getHeight() - 5, mAxisPaint);
				c.drawText("" + df.format(mDataPoints.get(0).get(lastIndex).mTime) + "m", (float) ((getWidth() - 50) / 1.5), getHeight() - 5, mAxisPaint);
				break;
			case TEMPERATURE:
				c.drawText("" + df.format(mDataPoints.get(0).get(lastIndex).mTime / 2) + "m", (float) ((getWidth() - 50) / 3.0), getHeight() - 5, mAxisPaint);
				c.drawText("" + df.format(mDataPoints.get(0).get(lastIndex).mTime) + "m", (float) ((getWidth() - 50) / 1.5), getHeight() - 5, mAxisPaint);
				break;
			case GPS:
				break;
		}
	}

	/**
	 * Uses the provided polling rate to set the seconds for the x-axis label
	 * 
	 * @param rate
	 */
	public void setPollingRate(int pollingRate)
	{
		mPollingRate = pollingRate;
	}

	public void setWindow(int windowMin, int windowMax)
	{
		if(windowMin >= windowMax)
		{
			return;
		}
		else if(windowMax > 1023 || windowMax < 0)
		{
			return;
		}
		else if(windowMin > 1023 || windowMin < 0)
		{
			return;
		}
		mWindowMin = windowMin;
		mWindowMax = windowMax;
		mWindowScale = 1023 / (mWindowMax - mWindowMin);
	}

	public int[] getMin()
	{
		return mMinValues;
	}

	public int[] getMax()
	{
		return mMaxValues;
	}

	public void drawChipData(Canvas c, ArrayList<ArrayList<Data>> dataToDraw)
	{
		float height = getHeight() - 15;
		float width = (float) (getWidth() / 2.2);
		float startX = 0;
		float stopX = 0;
		float startY = 0;
		float stopY = 0;
		float scaleConstant = (float) (mWindowScale / 1023 * (height - 5));
		int lastIndex = dataToDraw.get(TEMPERATURE).size() - 1;
		if(lastIndex < 0)
			return;

		/** Can't draw a line with less than 2 points. Temp is used since its the last 
		 * one that would get added to */
		if(dataToDraw.get(TEMPERATURE).size() < 2)
		{
			return;
		}

		double axisEnd = getWidth() - 5;
		double axisStart = getWidth() / 2;
		double rectWidth = (axisEnd - axisStart) / CHIP;
		double axisHeight = (getHeight() - 20);
		DecimalFormat df = new DecimalFormat("#.###");

		for(int i = 0; i < CHIP; ++i)
		{
			if(!mHiddenPins[i])
			{
				/** Draw Left Graph */
				for(int j = 0; j < dataToDraw.get(i).size() - 1; ++j)
				{
					startX = 15 + width / (dataToDraw.get(i).size() - 1) * j;
					stopX = 15 + width / (dataToDraw.get(i).size() - 1) * (j + 1);
					startY = (float) (height - (dataToDraw.get(i).get(j).mBitResistance - mWindowMin) * scaleConstant);
					stopY = (float) (height - (dataToDraw.get(i).get(j + 1).mBitResistance - mWindowMin) * scaleConstant);
					c.drawLine(startX, startY, stopX, stopY, mPinPaints.get(i));
				}

				/** Draw Delta Graph */
				double currentResistance = (double) (dataToDraw.get(i).get(dataToDraw.get(i).size() - 1).mBitResistance);
				double initialResistance = (double) (dataToDraw.get(i).get(0).mBitResistance);
				if(currentResistance > 0)
				{
					double percentDelta = currentResistance / initialResistance;
					if(percentDelta < 1)
					{
						percentDelta = 1.0 / percentDelta;
						percentDelta--;
						percentDelta = -percentDelta;
					}
					else
						percentDelta--;
					int rectLeft = (int) (axisStart + rectWidth * i);
					int rectRight = (int) (rectLeft + rectWidth);
					int rectY = (int) (getHeight() - 15) / 2 + 5;
					int rectTop = rectY;
					int rectBot = rectY;
					int rectDelta = (int) (axisHeight / 2 * percentDelta / PERCENT_SCALE);
					if(percentDelta < 0)
						rectTop += Math.abs(rectDelta);
					else
						rectBot -= Math.abs(rectDelta);
					c.drawRect(rectLeft, rectTop, rectRight, rectBot, mPinPaints.get(i));

					/** Draw Delta Graph Label */
					float labelY = (float) (rectTop * 0.95);
					c.drawText(df.format(percentDelta * 100) + "%", rectLeft, labelY, mAxisPaint);

					//TODO: Change email threshold stuff
					if(percentDelta > mThresholdPercentage && !mEmailSent)
					{
						Log.d("Testing", "Calling sendEmail()");
						sendEmail();
						mEmailSent = true;
					}
				}
			}
		}

		/** Draws the resistance of the last value at the right end of the graph */
		startX = getWidth() - 75;

		for(int i = 0; i < CHIP; ++i)
		{
			if(!mHiddenPins[i])
			{
				startY = (float) (height - (dataToDraw.get(i).get(lastIndex).mBitResistance - mWindowMin) * scaleConstant);
				c.drawText(df.format(dataToDraw.get(i).get(lastIndex).mResistance) + "kOhms", (float) (startX / 2.2), startY, mPinPaints.get(i));
			}
		}
	}

	public void writeGpsData(String time)
	{
		try
		{
			mBufferedGPSDataWriter.write(time + "\t" + mLatitude + "\t" + mLongitude);
			mBufferedGPSDataWriter.newLine();
			mBufferedGPSDataWriter.flush();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}		
	}
	
	public void startGPS()
	{
		mLocManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
		mLocListener = new LocationListener()
        {

			@Override
			public void onLocationChanged(Location location)
			{
				mLatitude = location.getLatitude();
				mLongitude = location.getLongitude();
				mGPSTime = location.getTime();
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ssZZZZ");
				mTimeString = dateFormatter.format(mGPSTime);
				writeGpsData(mTimeString);
				Log.d("GPS", "Time: " + mTimeString + "\nLatitude: " + mLatitude + "\nLongitude: " + mLongitude);
			}

			@Override
			public void onProviderDisabled(String provider)
			{
			}

			@Override
			public void onProviderEnabled(String provider)
			{
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras)
			{
			}
        	
        };
		mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mLocListener);
		
	}

	public void sendEmail()
	{
		try
		{
			GMailSender sender = new GMailSender("arduinogui@gmail.com", "achen052");
			// TODO Change subject add in multiple reports/more info
			sender.sendMail("Gas Sensor", "PPM: 10000" + "\nTime: " + mTimeString + "\nLongitude: " + mLongitude + "\nLatitude: " + mLatitude,
					"arduinogui@gmail.com", mAutoEmail);
		}
		catch(Exception e)
		{
			Log.e("SendMail", e.getMessage(), e);
			e.printStackTrace();
		}
	}

	public double calculateHumidity(int bitVoltage, double tempCelcius)
	{
		double voltage = REFERENCE_VOLTAGE * (bitVoltage / 1023.0);
		double humidityPercentage = (voltage / REFERENCE_VOLTAGE - 0.16) / 0.0062;
		double correctedPercentage = humidityPercentage / (1.0546 - 0.00216 * tempCelcius);

		if(correctedPercentage > 100)
			correctedPercentage = 100;
		if(correctedPercentage < 0)
			correctedPercentage = 0;
		
		return correctedPercentage;
	}
	
	public void drawHumidityData(Canvas c, ArrayList<Data> dataToDraw, double tempCelcius)
	{
		//TODO Change to fixed scale like temp, use non RH, just regular humid?
		float height = getHeight() - 15;
		float width = (float) (getWidth() / 1.5) - 5;
		float startX = 0;
		float stopX = 0;
		float startY = 0;
		float stopY = 0;
		float scaleConstant = (float) (mWindowScale / 1023 * (height - 5));
		int lastIndex = dataToDraw.size() - 1;
		Paint graphPaint = mPinPaints.get(HUMIDITY);
		
		for(int i = 0; i < dataToDraw.size() - 2; ++i)
		{
			startX = 15 + width / dataToDraw.size() * i;
			stopX = 15 + width / dataToDraw.size() * (i + 1);
			startY = (float) (height - (dataToDraw.get(i).mVoltage - mWindowMin) * scaleConstant);
			stopY = (float) (height - (dataToDraw.get(i + 1).mVoltage - mWindowMin) * scaleConstant);
			c.drawLine(startX, startY, stopX, stopY, graphPaint);
		}

		/**  Draws the humidity percentage at the right end of the graph */
		DecimalFormat humidityFormat = new DecimalFormat("#.###");
		startX = width - 50;
		
		double humidityPercentage = calculateHumidity(dataToDraw.get(lastIndex).mVoltage, tempCelcius);
		
		startY = (float) (height - (dataToDraw.get(lastIndex).mVoltage - mWindowMin) * scaleConstant);
		String humidityString = humidityFormat.format(humidityPercentage) + "%";
		c.drawText(humidityString, startX, startY, graphPaint);
		
		float oldTextSize = graphPaint.getTextSize();
		float oldTextScale = graphPaint.getTextScaleX();
		float targetHeight = height * 0.25f;
		float targetWidth = (getWidth() - width) * 0.8f;
		setTextSize(humidityString, graphPaint, 36, targetHeight);
		setTextScale(humidityString, graphPaint, 1, targetWidth);
		
		float textHeight = getTextHeight(humidityString, graphPaint);
		float textWidth = getTextWidth(humidityString, graphPaint);
		
		startX = width + (getWidth() - width - textWidth) / 2;
		startY = height / 2 + textHeight / 2;
		
		c.drawText(humidityString, startX, startY, graphPaint);
		graphPaint.setTextSize(oldTextSize);
		graphPaint.setTextScaleX(oldTextScale);
	}

	public double calculateTemp(int bitVoltage)
	{
		double tempCelcius = REFERENCE_VOLTAGE * (bitVoltage / 1023.0) * 100 - TEMP_OFFSET;
		return tempCelcius;
	}
	
	public void drawTempData(Canvas c, ArrayList<Data> dataToDraw, double temp)
	{
		float height = getHeight() - 15;
		float width = (float) (getWidth() / 1.5) - 5;
		float startX = 0;
		float stopX = 0;
		float startY = 0;
		float stopY = 0;
		int lastIndex = dataToDraw.size() - 1;
		float tempScale = (float) (TEMP_GRAPH_SCALE / 1023.0 * (height - 5));
		Paint graphPaint = mPinPaints.get(TEMPERATURE);

		for(int i = 0; i < dataToDraw.size() - 2; ++i)
		{
			startX = 15 + width / dataToDraw.size() * i;
			stopX = 15 + width / dataToDraw.size() * (i + 1);
			startY = (float) (height - (dataToDraw.get(i).mVoltage - TEMP_WINDOW_MIN) * tempScale);
			stopY = (float) (height - (dataToDraw.get(i + 1).mVoltage - TEMP_WINDOW_MIN) * tempScale);
			c.drawLine(startX, startY, stopX, stopY, graphPaint);
			
		}

		DecimalFormat tempFormat = new DecimalFormat("#.###");
		startX = width - 50;
		double tempCelcius = calculateTemp(dataToDraw.get(lastIndex).mVoltage);

		startY = (float) (height - (dataToDraw.get(lastIndex).mVoltage - TEMP_WINDOW_MIN) * tempScale);
		String tempString = tempFormat.format(tempCelcius) + "C";
		c.drawText(tempString, startX, startY, graphPaint);
		
		float oldTextSize = graphPaint.getTextSize();
		float oldTextScale = graphPaint.getTextScaleX();
		float targetHeight = height * 0.25f;
		float targetWidth = (getWidth() - width) * 0.8f;
		setTextSize(tempString, graphPaint, 36, targetHeight);
		setTextScale(tempString, graphPaint, 1, targetWidth);
		
		float textHeight = getTextHeight(tempString, graphPaint);
		float textWidth = getTextWidth(tempString, graphPaint);
		
		startX = width + (getWidth() - width - textWidth) / 2;
		startY = height / 2 + textHeight / 2;
		
		c.drawText(tempString, startX, startY, graphPaint);
		graphPaint.setTextSize(oldTextSize);
		graphPaint.setTextScaleX(oldTextScale);
	}
	
	public int getTextHeight(String text, Paint textPaint)
	{
		Rect bounds = new Rect();
		textPaint.getTextBounds(text, 0, text.length(), bounds);
		return bounds.bottom - bounds.top;
	}
	
	public int getTextWidth(String text, Paint textPaint)
	{
		Rect bounds = new Rect();
		textPaint.getTextBounds(text, 0, text.length(), bounds);
		return bounds.right - bounds.left;
	}
	
	public void drawGPSData(Canvas c)
	{
		Paint textPaint = new Paint();
		textPaint.setColor(Color.BLACK);
		DecimalFormat coordinateFormat = new DecimalFormat("#.####");
		
		float targetHeight = getHeight() / 6;
		
		String latitudeText = "Latitude: " + coordinateFormat.format(mLatitude);
		setTextSize(latitudeText, textPaint, 36, targetHeight);
		int textWidth = getTextWidth(latitudeText, textPaint);
		float drawX = (getWidth() - textWidth) / 2;
		float drawY = targetHeight * 2.33f;
		c.drawText(latitudeText, drawX, drawY, textPaint);
		
		String longitudeText = "Longitude: " + coordinateFormat.format(mLongitude);
		textWidth = getTextWidth(longitudeText, textPaint);
		drawX = (getWidth() - textWidth) / 2;
		drawY = targetHeight * 3.66f;
		c.drawText(longitudeText, drawX, drawY, textPaint);
		
		textWidth = getTextWidth(mTimeString, textPaint);
		drawX = (getWidth() - textWidth) / 2;
		drawY = targetHeight * 5;
		c.drawText(mTimeString, drawX, drawY, textPaint);
	}
	
	/**
	 * Sets the text size based on the passed in height value using setTextFont
	 * @param text
	 * @param textPaint
	 * @param fontSize
	 * @param height
	 */
	public void setTextSize(String text, Paint textPaint, int fontSize, float height)
	{
		textPaint.setTextSize(fontSize);
		int textHeight = getTextHeight(text, textPaint);
		float targetSize = height;
		float textSize = (targetSize / textHeight) * fontSize;
		textPaint.setTextSize(textSize);
	}
	
	/**
	 * Sets the width of the text to the passed in value using setTextScaleX
	 * @param text
	 * @param textPaint
	 * @param fontScale
	 * @param width
	 * @return
	 */
	public void setTextScale(String text, Paint textPaint, int fontScale, float width)
	{
		textPaint.setTextScaleX(fontScale);
		Rect bounds = new Rect();
		textPaint.getTextBounds(text, 0, text.length(), bounds);
		int textWidth = getTextWidth(text, textPaint);
		float targetWidth = width;
		float textScale = targetWidth / textWidth;
		textPaint.setTextScaleX(textScale);
	}
	
	public double calculateMQ2Resistance(int bitVoltage)
	{
		double outputVoltage = (double) bitVoltage * REFERENCE_VOLTAGE / 1023.0;
		double MQ2Resistance = (REFERENCE_VOLTAGE / outputVoltage - 1) * MQ2_DIVIDER_RESISTANCE;
		return MQ2Resistance;
	}
	
	public double calculateMQ2Ppm(int bitVoltage)
	{
		double MQ2Resistance = calculateMQ2Resistance(bitVoltage);
		double deltaResistance = (MQ2Resistance - mMQ2InitialResistance) / mMQ2InitialResistance;
		double ppm = -63.99 * Math.log(deltaResistance) + 60.802;
		
		if(ppm < 0)
			ppm = 0;
		
		return ppm;
	}
	
	public void drawMQ2Data(Canvas c, ArrayList<Data> dataToDraw)
	{	
		float height = getHeight() - 15;
		float width = (float) (getWidth() / 1.5) - 5;
		float startX = 0;
		float stopX = 0;
		float startY = 0;
		float stopY = 0;
		float scaleConstant = (float) (mWindowScale / 1023 * (height - 5));
		int lastIndex = dataToDraw.size() - 1;
		Paint graphPaint = mPinPaints.get(MQ2);
		
		if(lastIndex < 1)
		{
			return;
		}
		else if(mMQ2InitialResistance == -1)
		{
			mMQ2InitialResistance = calculateMQ2Resistance(dataToDraw.get(lastIndex).mVoltage);
		}

		for(int i = 0; i < dataToDraw.size() - 1; ++i)
		{
			startX = 15 + width / dataToDraw.size() * i;
			stopX = 15 + width / dataToDraw.size() * (i + 1);
			startY = (float) (height - (dataToDraw.get(i).mVoltage - mWindowMin) * scaleConstant);
			stopY = (float) (height - (dataToDraw.get(i + 1).mVoltage - mWindowMin) * scaleConstant);
			c.drawLine(startX, startY, stopX, stopY, graphPaint);
		}

		DecimalFormat MQ2Format = new DecimalFormat("#.###");
		startX = width - 50;
		double ppm = calculateMQ2Ppm(dataToDraw.get(lastIndex).mVoltage);
		
		startY = (float) (height - (dataToDraw.get(lastIndex).mVoltage - mWindowMin) * scaleConstant);
		String ppmString = MQ2Format.format(ppm) + "ppm";
		c.drawText(ppmString, startX, startY, graphPaint);
		
		if(ppm < 5)
		{
			ppmString = "Below detection limit";
		}
		
		/** Save the old values and adjust the size */
		float oldTextSize = graphPaint.getTextSize();
		float oldTextScale = graphPaint.getTextScaleX();
		float targetHeight = height * 0.25f;
		float targetWidth = (getWidth() - width) * 0.8f;
		setTextSize(ppmString, graphPaint, 36, targetHeight);
		setTextScale(ppmString, graphPaint, 1, targetWidth);
		
		/** Get the height and width of the text in order to center it */
		float textHeight = getTextHeight(ppmString, graphPaint);
		float textWidth = getTextWidth(ppmString, graphPaint);
		
		startX = width + (getWidth() - width - textWidth) / 2;
		startY = height / 2 + textHeight / 2;
		
		c.drawText(ppmString, startX, startY, graphPaint);
		graphPaint.setTextSize(oldTextSize);
		graphPaint.setTextScaleX(oldTextScale);
	}

	public void drawData(Canvas c)
	{
		int lastIndex = 0;
		double tempCelcius = 0;
		if(AVERAGE_DATA)
		{
			lastIndex = mAveragedData.get(TEMPERATURE).size() - 1;
			if(lastIndex < 0)
				return;
			tempCelcius = calculateTemp(mAveragedData.get(TEMPERATURE).get(lastIndex).mVoltage);
		}
		else
		{
			lastIndex = mDataPoints.get(TEMPERATURE).size() - 1;
			if(lastIndex < 0)
				return;
			tempCelcius = calculateTemp(mDataPoints.get(TEMPERATURE).get(lastIndex).mVoltage);
		}

		switch(mDisplayMode)
		{
			case CHIP:
				if(AVERAGE_DATA)
					drawChipData(c, mAveragedData);
				else
					drawChipData(c, mDataPoints);
				break;
			case MQ2:
				if(AVERAGE_DATA)
					drawMQ2Data(c, mAveragedData.get(MQ2));
				else
					drawMQ2Data(c, mDataPoints.get(MQ2));
				break;
			case HUMIDITY:
				if(AVERAGE_DATA)
					drawHumidityData(c, mAveragedData.get(HUMIDITY), tempCelcius);
				else
					drawHumidityData(c, mDataPoints.get(HUMIDITY), tempCelcius);
				break;
			case TEMPERATURE:
				if(AVERAGE_DATA)
					drawTempData(c, mAveragedData.get(TEMPERATURE), tempCelcius);
				else
					drawTempData(c, mDataPoints.get(TEMPERATURE), tempCelcius);
				break;
			case GPS:
				drawGPSData(c);
				break;
		}
	}

	/**
	 * Closes the files, called when done writing to the files
	 */
	public void closeFiles()
	{
		try
		{
			for(int i = 0; i < ANALOG_PINS; ++i)
			{
				mBufferedWriters.get(i).close();
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Returns the file paths to pin data txt files
	 * 
	 * @return
	 */
	public ArrayList<Uri> getFilePaths()
	{
		ArrayList<Uri> filePaths = new ArrayList<Uri>();
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			filePaths.add(Uri.fromFile(mDataFiles.get(i)));
		}
		return filePaths;
	}

	public Data calculateAverage(int pin)
	{
		int averageVoltage = 0;
		int correctedAverageVoltage = 0;
		ArrayList<Data> dataGroup = new ArrayList<Data>();
		for(int i = 1; i <= 5; ++i)
		{
			Data temp = mDataPoints.get(pin).get(mDataPoints.get(pin).size() - i);
			dataGroup.add(temp);
			averageVoltage += temp.mVoltage;
		}

		averageVoltage /= 5;

		int keepCount = 0;
		for(int i = 0; i < 5; ++i)
		{
			if(dataGroup.get(i).mVoltage < averageVoltage * 1 + ACCEPTANCE_RATIO && dataGroup.get(i).mVoltage > averageVoltage * 1 - ACCEPTANCE_RATIO)
				;
			{
				correctedAverageVoltage += dataGroup.get(i).mVoltage;
				keepCount++;
			}
		}

		correctedAverageVoltage /= keepCount;
		return new Data(correctedAverageVoltage, dataGroup.get(0).mTime, pin);
	}

	public void writeChipData(ArrayList<ArrayList<Data>> dataToWrite)
	{
		int lastIndex = 0;
		for(int i = 0; i < CHIP; ++i)
		{
			lastIndex = dataToWrite.get(i).size() - 1;
			if(lastIndex < 0)
				return;

			try
			{
				// TODO: Change to use GPS to get time and position
				mBufferedWriters.get(i).write("" + dataToWrite.get(i).get(lastIndex).mTime + "\t" + dataToWrite.get(i).get(lastIndex).mResistance);
				mBufferedWriters.get(i).newLine();
				mBufferedWriters.get(i).flush();
			}
			catch(IOException e)
			{
				Log.e("ArduinoGUI", "Error writing Chip Data to File ");
				e.printStackTrace();
			}
		}
	}

	public void writeMQ2Data(ArrayList<Data> dataToWrite)
	{
		int lastIndex = dataToWrite.size() - 1;
		if(lastIndex < 0)
			return;
		try
		{
			double ppm = calculateMQ2Ppm(dataToWrite.get(lastIndex).mVoltage);
			mBufferedWriters.get(MQ2).write("" + dataToWrite.get(lastIndex).mTime + "\t" + ppm);
			mBufferedWriters.get(MQ2).newLine();
			mBufferedWriters.get(MQ2).flush();
		}
		catch(IOException e)
		{
			Log.e("ArduinoGUI", "Error writing MQ2 Data to File");
			e.printStackTrace();
		}
	}

	public void writeHumidityData(ArrayList<Data> dataToWrite, ArrayList<Data> temperatureData)
	{
		int lastIndex = temperatureData.size() - 1;
		if(lastIndex < 0)
			return;
		try
		{
			double tempCelcius = calculateTemp(temperatureData.get(lastIndex).mVoltage);
			double humidityPercentage = calculateHumidity(dataToWrite.get(lastIndex).mVoltage, tempCelcius);
			mBufferedWriters.get(HUMIDITY).write("" + dataToWrite.get(lastIndex).mTime + "\t" + humidityPercentage);
			mBufferedWriters.get(HUMIDITY).newLine();
			mBufferedWriters.get(HUMIDITY).flush();
		}
		catch(IOException e)
		{
			Log.e("ArduinoGUI", "Error writing Humidity Data to File ");
			e.printStackTrace();
		}
	}

	/**
	 * Writes temperature data to a file
	 * @param dataToWrite
	 */
	public void writeTempData(ArrayList<Data> dataToWrite)
	{
		int lastIndex = dataToWrite.size() - 1;
		if(lastIndex < 0)
			return;
		try
		{
			double tempCelcius = calculateTemp(dataToWrite.get(lastIndex).mVoltage);
			mBufferedWriters.get(TEMPERATURE).write("" + dataToWrite.get(lastIndex).mTime + "\t" + tempCelcius);
			mBufferedWriters.get(TEMPERATURE).newLine();
			mBufferedWriters.get(TEMPERATURE).flush();
		}
		catch(IOException e)
		{
			Log.e("ArduinoGUI", "Error writing Temperature Data to File ");
			e.printStackTrace();
		}
	}
	
		/**
	 * Adds the data received from the Arduino to the graph and Data ArrayList
	 * 
	 * @param values
	 * @param pollRate
	 * @param timeStarted
	 */
	public void addData(int[] values, Calendar timeStarted)
	{
		if(values[TEMPERATURE] == 1024)
		{
			setDividerResistances(values);
			return;
		}
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			mDataPoints.get(i).add(new Data(values[i], mDataPoints.get(i).size() * mPollingRate / MS_PER_MIN, i));

			if(AVERAGE_DATA)
				if(mDataPoints.get(i).size() > POINTS_TO_AVERAGE)
					mAveragedData.get(i).add(calculateAverage(i));
		}

		if(AVERAGE_DATA)
		{
			if(mDataPoints.get(TEMPERATURE).size() > POINTS_TO_AVERAGE)
			{
				writeChipData(mAveragedData);
				writeMQ2Data(mAveragedData.get(MQ2));
				writeHumidityData(mAveragedData.get(HUMIDITY), mAveragedData.get(TEMPERATURE));
				writeTempData(mAveragedData.get(TEMPERATURE));
			}
		}
		else
		{
			writeChipData(mDataPoints);
			writeMQ2Data(mDataPoints.get(MQ2));
			writeHumidityData(mDataPoints.get(HUMIDITY), mDataPoints.get(TEMPERATURE));
			writeTempData(mDataPoints.get(TEMPERATURE));
		}

		checkWindowLimits(values);
		writeData(values);
	}
	
	/**
	 * Writes raw voltage values to a file
	 * @param values
	 */
	public void writeData(int[] values)
	{
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			try
			{
				//TODO: Write actual values instead of bitVoltage
				mBufferedNetworkWriter.write("" + values[i] + "\t");
				mBufferedNetworkWriter.newLine();
				mBufferedNetworkWriter.flush();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks the range of values and stores the max and min values for autoscaling
	 * @param values
	 */
	public void checkWindowLimits(int[] values)
	{
		int lastIndex = 0;
		for(int i = 0; i < CHIP; ++i)
		{
			int bitResistance;
			if(AVERAGE_DATA)
			{
				lastIndex = mAveragedData.get(i).size() - 1;
				if(lastIndex < 0)
					return;
				bitResistance = mAveragedData.get(i).get(lastIndex).mBitResistance;
			}
			else
			{
				lastIndex = mDataPoints.get(i).size() - 1;
				if(lastIndex < 0)
					return;
				bitResistance = mDataPoints.get(i).get(lastIndex).mBitResistance;
			}
			
			
			if(bitResistance > 1023)
				bitResistance = 1023;
			if(bitResistance < 0)
				bitResistance = 1023;
			if(bitResistance > mMaxValues[i])
				mMaxValues[i] = bitResistance;
			if(bitResistance < mMinValues[i])
				mMinValues[i] = bitResistance;
		}
		
		for(int i = CHIP; i < TEMPERATURE; ++i)
		{
			int bitVoltage = values[i];
			if(bitVoltage > 1023)
				bitVoltage = 1023;
			if(bitVoltage < 0)
				bitVoltage = 0;
			if(bitVoltage > mMaxValues[i])
				mMaxValues[i] = bitVoltage;
			if(bitVoltage < mMinValues[i])
				mMinValues[i] = bitVoltage;
		}
	}

	/**
	 * Sets the display from chip, MQ2, humidity, and temp. Called by the GraphActivity onTouch
	 * @param displayMode
	 */
	public void setDisplayMode(int displayMode)
	{
		mDisplayMode = displayMode;
	}

	public int getDisplayMode()
	{
		return mDisplayMode;
	}

	/**
	 * Sets auto scaling for the graph on or off. Called from the options activity when checked
	 * @param autoScaling
	 */
	public void setAutoScaling(boolean autoScaling)
	{
		mAutoScaling = autoScaling;
	}

	/**
	 * Saves the values for the digital potentiometer. Used to calculate sensor chip resistance based on the voltage.
	 * Resistances are initially matched to the same as the sensor.
	 * @param dataToAdd
	 */
	public void setDividerResistances(int[] dataToAdd)
	{
		for(int i = 0; i < CHIP; ++i)
		{
			mDividerResistance[i] = dataToAdd[i] * 100000 / 255;
		}
	}

	/**
	 * Sets the email for automatic notification when the threshold reached. Called from the
	 * options activity when polling is started
	 * @param email
	 */
	public void setAutoEmail(String email)
	{
		mAutoEmail = email;
	}

	/**
	 * Sets the threshold for automatic notification. Called from the options activity.
	 * @param percentage
	 */
	public void setThreshold(double percentage)
	{
		mThresholdPercentage = percentage;
	}
}
