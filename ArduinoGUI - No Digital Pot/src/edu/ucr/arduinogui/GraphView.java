package edu.ucr.arduinogui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphView extends SurfaceView implements SurfaceHolder.Callback, Serializable 
{
	private static final int VOLTAGE_DIVIDER = 2;
	private static final int WHEATSTONE_BRIDGE = 4;
	private static final long serialVersionUID = 1L;

	//Average the data, take the last 5 points and use that average, rejecting
	//ones that are past the threshold.
	private static final boolean AVERAGE_DATA = true;
	//Number of points to average to determine the rejection threshold
	private static final int POINTS_TO_AVERAGE = 5;
	//Data points are rejected if they are more than 1 +/- the acceptance ratio times the
	//average of the last POINTS_TO_AVERAGE points.
	private static final double ACCEPTANCE_RATIO = 0.25;
	//Number of A/D converter inputs (Analog Input) on the Arduino 
	private static final int ANALOG_PINS = 8;
	
	//Constants used to determine which data points to draw, based on Analog In pin number
	private static final int CHIP = 5;
	private static final int HUMIDITY = 6;
	private static final int TEMPERATURE = 7;
	
	//Voltage put into the voltage divider/wheatstone bridge
	private static final double INPUT_VOLTAGE = 5.0;
	//Reference voltage of the Arduino that the A/D Converter (Analog Inputs) are using
	private static final double REFERENCE_VOLTAGE = 5.0;
	private static final double MS_PER_MIN = 60000;
	
	//Resistance of the reference resistor on Ohms
	//private static final double DIVIDER_RESISTANCE = 100000;
	//Used to determine the drawing scale of the resistance
	private static final double MAX_RESISTANCE = 100000;
	//Resistance of the resistors in Ohms of the Wheatstone Bridge
	private static final double R1 = 15000.0;
	private static final double R2 = 15000.0;
	//TODO Change to adjustable
	private static final double R3 = 15000.0;
	
	//Constants used to set the window of the Temperature graph
	private static final double TEMP_SCALE = 1.1168;
	private static final double TEMP_CONSTANT = -19.448;
	private static final double TEMP_WINDOW_MAX = 320.0;
	private static final double TEMP_WINDOW_MIN = 255.0;
	private static final float TEMP_GRAPH_SCALE = (float) ((float) 1023.0 / (TEMP_WINDOW_MAX - TEMP_WINDOW_MIN));

	private int mode = VOLTAGE_DIVIDER;
	
	public class Data
	{
		public int mVoltage;
		public double mTime;
		//Resistance in kOhms
		public double mResistance;
		public int mBitResistance;
		Data(int voltage, double time, int pin)
		{
			mVoltage = voltage;
			calculateResistance(mode, pin);
			mTime = time;
		}
	
		private void calculateResistance(int mode, int pin)
		{
			if(pin > 4)
				return;
			if(mode == VOLTAGE_DIVIDER)
			{
				mResistance = ((double) mVoltage / 1023 * INPUT_VOLTAGE * DIVIDER_RESISTANCE) / 
								((1023 - mVoltage) * INPUT_VOLTAGE);
			}
			else if(mode == WHEATSTONE_BRIDGE)
			{
				double bridgeVoltage = (double) mVoltage / 1023 * 5.0;
				mResistance = (R2 * R3 + R3 * (R1 + R2) * bridgeVoltage / INPUT_VOLTAGE) 
								/ (R1 - (R1 + R2) * bridgeVoltage / INPUT_VOLTAGE) / 1000;
				Log.v("mResistance", "mResistance: " + mResistance);
				if(bridgeVoltage != 0)
					 Log.v("mVoltage", "mVoltage: " + bridgeVoltage);
			}
			mBitResistance = (int) ((mResistance * 1000 / MAX_RESISTANCE) * 1023);
		}
	}

	//Root file path for saving the text files
	private static final File ROOT = Environment.getExternalStorageDirectory();

	//Main thread
	private GraphThread mThread = null;
	
	//Flag indicating the graphView be reset
	private boolean doReset = false;
	
	//Boolean array that allows for the drawing of just certain pins
	private boolean[] mHiddenPins = new boolean[ANALOG_PINS];
	
	private static int mDisplayMode = CHIP;
	
	//Window scaling values
	private boolean mAutoScaling = false;
	private int[] mMinValues = new int[ANALOG_PINS];
	private int[] mMaxValues = new int[ANALOG_PINS];
	private int mPollingRate = 100;
	private double mWindowScale = 1;
	private double mWindowMin = 0;
	private double mWindowMax = 1023;
	private long lastDraw = System.currentTimeMillis();
	
	//ArrayLists that store the data for each pin
	private ArrayList<ArrayList<Data>> dataPoints = new ArrayList<ArrayList<Data>>();
	private ArrayList<ArrayList<Data>> averagedData = new ArrayList<ArrayList<Data>>();
	
	//Paint objects for different data colors
	private ArrayList<Paint> pinPaints = new ArrayList<Paint>();
	Paint axisPaint = new Paint();
	Paint textPaint = new Paint();
	
	//File objects for writing to txt files
	private ArrayList<File> dataFiles = new ArrayList<File>();
	private ArrayList<FileWriter> fileWriters = new ArrayList<FileWriter>();
	private ArrayList<BufferedWriter> bufferedWriters = new ArrayList<BufferedWriter>();
	
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
			mThread = new GraphThread(holder, null,null); 
			mThread.start();
		}
		else
			mThread.start();
		mThread.setRunning(true);
	}
	public void surfaceChanged(SurfaceHolder holder, int format, int width, 
			int height) 
	{
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		boolean retry = true;
		mThread.setRunning(false);
		while (retry) 
		{
			try 
			{
				mThread.join();
				retry = false;
			} 
			catch (InterruptedException e) 
			{
			}
		}
	}

	class GraphThread extends Thread 
	{
		private SurfaceHolder mSurfaceHolder;
		private boolean mRun = false;
		
		public GraphThread(SurfaceHolder holder, Context context, 
				Handler handler) 
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
			while (mRun) 
			{
				c = null;
				try {
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) 
					{
						onDraw(c);
					}
				} 
				finally 
				{
					if (c != null)
						mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
	
	public void initializeValues()
	{
		textPaint.setColor(Color.BLACK);
		axisPaint.setColor(Color.BLACK);
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			pinPaints.add(new Paint());
			mMinValues[i] = 1023;
			mMaxValues[i] = 0;
		}
		pinPaints.get(0).setColor(Color.RED);
		//Orange
		pinPaints.get(1).setColor(Color.rgb(255, 165, 0));
		pinPaints.get(2).setColor(Color.YELLOW);
		pinPaints.get(3).setColor(Color.GREEN);
		pinPaints.get(4).setColor(Color.BLUE);
		//Indigo
		pinPaints.get(5).setColor(Color.rgb(111, 0, 255));
		//Violet
		pinPaints.get(HUMIDITY).setColor(Color.rgb(238, 130, 238));
		pinPaints.get(TEMPERATURE).setColor(Color.BLACK);
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			dataPoints.add(new ArrayList<Data>());
			averagedData.add(new ArrayList<Data>());
		}
	}
	
	public void createFiles(Calendar timeStarted)
	{
		try
		{
			int month = timeStarted.get(Calendar.MONTH);
			int day = timeStarted.get(Calendar.DAY_OF_MONTH);
			int hour = timeStarted.get(Calendar.HOUR_OF_DAY);
			int minute = timeStarted.get(Calendar.MINUTE);
			int millis = timeStarted.get(Calendar.MILLISECOND);
			File folder = new File(ROOT + "/ArduinoGUI/");
			folder.mkdirs();
			
			dataFiles = new ArrayList<File>();
			fileWriters = new ArrayList<FileWriter>();
			bufferedWriters = new ArrayList<BufferedWriter>();
			
			if(ROOT.canWrite())
			{
				for(int i = 0; i < ANALOG_PINS; ++i)
				{
					dataFiles.add(new File(ROOT + "/ArduinoGUI/", 
							month + "_" + day + "_" + hour + "_" + minute + "_" + 
							millis + "p" + i + ".txt"));
					fileWriters.add(new FileWriter(dataFiles.get(i) , true));
					bufferedWriters.add(new BufferedWriter(fileWriters.get(i)));
				}
				for(int i = 0; i < CHIP; ++i)
				{
					bufferedWriters.get(i).write("Time (min)\tResistance (kOhms)");
					bufferedWriters.get(i).newLine();
					bufferedWriters.get(i).flush();
				}
			}
		}
		catch(IOException e)
		{
			
		}
	}
	
	public void reset()
	{
		doReset = true;
	}

	public void deleteData()
	{
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			dataPoints.get(i).clear();
			averagedData.get(i).clear();
			mMaxValues[i] = 0;
			mMinValues[i] = 1023;
		}
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
		//Required so it doesn't delete an array while its being accessed
		if(doReset)
		{
			deleteData();
		}
	}
	
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
				setWindow(tempMin, tempMax);
				break;
			case HUMIDITY:
				setWindow(mMinValues[HUMIDITY], mMaxValues[HUMIDITY]);
				break;
			case TEMPERATURE:
				break;
			
		}
	}
	
	public void drawFPS(Canvas c)
	{
		int frames = (int) (1000 / (System.currentTimeMillis() - lastDraw));
		lastDraw = System.currentTimeMillis();
		c.drawText("FPS: " + frames, getWidth() - 50, 20, textPaint);
	}
	
	public void drawAxis(Canvas c)
	{
		c.drawLine(15, 5, 15, getHeight() - 5, axisPaint);
		c.drawLine(5, getHeight() - 15, getWidth() - 5, getHeight() - 15, 
				axisPaint);
		DecimalFormat df = new DecimalFormat("#.###");
		switch(mDisplayMode)
		{
			case CHIP:
				c.drawText("" + df.format(mWindowMax / 1023 * MAX_RESISTANCE / 1000) + "kOhms", 20, 10, axisPaint);
				c.drawText("" + df.format(mWindowMin / 1023 * MAX_RESISTANCE / 1000) + "kOhms", 20, getHeight() - 5, axisPaint);
				break;
			case HUMIDITY:
				c.drawText("" + mWindowMax, 20, 10, axisPaint);
				c.drawText("" + mWindowMin, 20, getHeight() - 5, axisPaint);
				break;
			case TEMPERATURE:
				c.drawText(df.format(TEMP_WINDOW_MAX * TEMP_SCALE + TEMP_CONSTANT - 273) + "C", 20, 10, axisPaint);
				c.drawText(df.format(TEMP_WINDOW_MIN * TEMP_SCALE + TEMP_CONSTANT - 273) + "C", 20, getHeight() - 5, axisPaint);
				break;
		}
		
		int lastIndex;
		
		if(AVERAGE_DATA)
		{
			lastIndex = averagedData.get(7).size() - 1;
			if(lastIndex < 0)
				return;
			c.drawText("" + df.format(averagedData.get(0).get(lastIndex).mTime / 2) + "m" , 
					(getWidth() - 50) / 2, getHeight() - 5, axisPaint);
			c.drawText("" + df.format(averagedData.get(0).get(lastIndex).mTime) + "m" , 
					(getWidth() - 50), getHeight() - 5, axisPaint);
		}
		else
		{
			lastIndex = dataPoints.get(7).size() - 1;
			if(lastIndex < 0)
				return;
			c.drawText("" + df.format(dataPoints.get(0).get(lastIndex).mTime / 2) + "m" , 
					(getWidth() - 50) / 2, getHeight() - 5, axisPaint);
			c.drawText("" + df.format(dataPoints.get(0).get(lastIndex).mTime) + "m" , 
					(getWidth() - 50), getHeight() - 5, axisPaint);
		}
	}
	
	/**
	 * Uses the provided polling rate to set the seconds for the x-axis label
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
		float width = getWidth();
		float startX = 0; 
		float stopX =  0;
		float startY = 0;
		float stopY = 0;
		float scaleConstant = (float) (mWindowScale / 1023 * (height - 5));
		int lastIndex = dataToDraw.get(7).size() - 1;
		if(lastIndex < 0)
			return;
		
		//Can't draw with less than 2 points. 
		//Use 7 because it would be the last one that gets added, making it the smallest
		if(dataToDraw.get(7).size() < 2)
		{
			return;
		}
		
		for(int i = 0; i < CHIP; ++i)
		{
			if(!mHiddenPins[i])
			{
				for(int j = 0; j < dataToDraw.get(i).size() - 1; ++j)
				{
					startX = 15 + width / (dataToDraw.get(i).size() - 1) * j;
					stopX = 15 + width / (dataToDraw.get(i).size() - 1)  * (j + 1);
					startY = (float) (height - (dataToDraw.get(i)
							.get(j).mBitResistance - mWindowMin) * scaleConstant);
					stopY = (float) (height - (dataToDraw.get(i)
							.get(j + 1).mBitResistance - mWindowMin) * scaleConstant);
					c.drawLine(startX, startY, stopX, stopY, pinPaints.get(i));
				}
			}
		}
		
		//Draws the resistance of the last value at the right end of the graph
		DecimalFormat df = new DecimalFormat("#.###");
		startX = getWidth() - 75;

		for(int i = 0; i < CHIP; ++i)
		{
			if(!mHiddenPins[i])
			{
				startY = (float) (height - (dataToDraw.get(i)
						.get(lastIndex).mBitResistance - mWindowMin) * scaleConstant);
				c.drawText(df.format(dataToDraw.get(i)
						.get(lastIndex).mResistance) + "kOhms", startX, startY, pinPaints.get(i));
			}	
		}
	}
	
	public void drawHumidityData(Canvas c, ArrayList<Data> dataToDraw, double tempCelcius)
	{
		float height = getHeight() - 15;
		float width = getWidth();
		float startX = 0; 
		float stopX =  0;
		float startY = 0;
		float stopY = 0;
		float scaleConstant = (float) (mWindowScale / 1023 * (height - 5));
		int lastIndex = dataToDraw.size() - 1;
		
		for(int i = 0; i < dataToDraw.size() - 2; ++i)
		{
			startX = 15 + width / dataToDraw.size() * i;
			stopX = 15 + width / dataToDraw.size() * (i + 1);
			startY = (float) (height - (dataToDraw
					.get(i).mVoltage - mWindowMin) * scaleConstant);
			stopY = (float) (height - (dataToDraw
					.get(i + 1).mVoltage - mWindowMin) * scaleConstant);
			c.drawLine(startX, startY, stopX, stopY, pinPaints.get(HUMIDITY));
		}
		
		//Draws the humidity percentage  at the right end of the graph
		DecimalFormat humidityFormat = new DecimalFormat("#.###");
		startX = getWidth() - 50;
		double voltage = REFERENCE_VOLTAGE * (dataToDraw
				.get(lastIndex).mVoltage / 1023.0);
		double humidityPercentage = (voltage / REFERENCE_VOLTAGE - 0.16) / 0.0062;
		double correctedPercentage = humidityPercentage / (1.0546 - 0.00216 * tempCelcius);
		
		if(correctedPercentage > 100) 
			correctedPercentage = 100;
		if(correctedPercentage < 0)
			correctedPercentage = 0;
		
		startY = (float) (height - (dataToDraw
				.get(lastIndex).mVoltage - mWindowMin) * scaleConstant);
		c.drawText(humidityFormat.format(correctedPercentage) + "%", startX, startY, pinPaints.get(HUMIDITY));
	}
	
	public void drawTempData(Canvas c, ArrayList<Data> dataToDraw, double temp)
	{
		float height = getHeight() - 15;
		float width = getWidth();
		float startX = 0; 
		float stopX =  0;
		float startY = 0;
		float stopY = 0;
		int lastIndex = dataToDraw.size() - 1;
		//Draws the graph for the temperature sensor data
		float tempScaleConstant = (float) (TEMP_GRAPH_SCALE / 1023.0 * (height - 5));
		for(int i = 0; i < dataToDraw.size() - 2; ++i)
		{
			startX = 15 + width / (lastIndex) * i;
			stopX = 15 + width / (lastIndex) * (i + 1);
			startY = (float) (height - (dataToDraw.get(i)
					.mVoltage - TEMP_WINDOW_MIN) * tempScaleConstant);
			stopY = (float) (height - (dataToDraw.get(i + 1)
					.mVoltage - TEMP_WINDOW_MIN) * tempScaleConstant);
			c.drawLine(startX, startY, stopX, stopY, pinPaints.get(TEMPERATURE));
		}
		
		DecimalFormat temperatureFormat = new DecimalFormat("#.###");
		startX = getWidth() - 50;
		startY = (float) (height - (dataToDraw.get(dataToDraw.size() - 1)
				.mVoltage - TEMP_WINDOW_MIN) * tempScaleConstant);
		c.drawText(temperatureFormat.format(temp) + "C", startX, startY, pinPaints.get(TEMPERATURE));
	}
	
	public void drawData(Canvas c)
	{
		int lastIndex = 0;
		double tempKelvins = 0;
		//Get temperature
		if(AVERAGE_DATA)
		{
			lastIndex = averagedData.get(TEMPERATURE).size() - 1;
			if(lastIndex < 0)
				return;
			tempKelvins = ((double) (averagedData.get(TEMPERATURE)
					.get(lastIndex).mVoltage)) * TEMP_SCALE + TEMP_CONSTANT;
		}
		else
		{
			lastIndex = dataPoints.get(TEMPERATURE).size() - 1;
			if(lastIndex < 0)
				return;
			tempKelvins = ((double) (dataPoints.get(TEMPERATURE)
					.get(lastIndex).mVoltage)) * TEMP_SCALE + TEMP_CONSTANT;
		}
		double tempCelcius = tempKelvins - 273;
		double tempFarenheit = (tempCelcius * 9 / 5) + 32;
		
		//Draws all the pin data for the non hidden pins
		switch(mDisplayMode)
		{
			case CHIP:
				//Draws the graph for the chip sensor data
				if(AVERAGE_DATA)
					drawChipData(c, averagedData);
				else
					drawChipData(c, dataPoints);
				break;
			case HUMIDITY:
				//Draws the graph for the humidity sensor data
				if(AVERAGE_DATA)
					drawHumidityData(c, averagedData.get(HUMIDITY), tempCelcius);
				else
					drawHumidityData(c, dataPoints.get(HUMIDITY), tempCelcius);
				break;
			case TEMPERATURE:
				
				if(AVERAGE_DATA)
					drawTempData(c, averagedData.get(TEMPERATURE), tempCelcius);
				else
					drawTempData(c, dataPoints.get(TEMPERATURE), tempCelcius);
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
				bufferedWriters.get(i).close();
			}
		}
		catch(IOException e)
		{
			
		}
	}
	
	/**
	 * Returns the file paths to pin data txt files
	 * @return
	 */
	public ArrayList<Uri> getFilePaths()
	{
		ArrayList<Uri> filePaths = new ArrayList<Uri>();
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			filePaths.add(Uri.fromFile(dataFiles.get(i)));
		}
		return filePaths;
	}
	
	
	public Data calculateAverage(int pin)
	{
		//TODO Smooth out the values, take the left and right values and average that with the mid
		int averageVoltage = 0;
		double averageResistance = 0;
		int correctedAverageVoltage = 0;
		ArrayList<Data> dataGroup = new ArrayList<Data>();
		for(int i = 1; i <= 5; ++i)
		{
			Data temp = dataPoints.get(pin).get(dataPoints.get(pin).size() - i);
			dataGroup.add(temp);
			averageVoltage += temp.mVoltage;
		}
		
		averageVoltage /= 5;
		averageResistance /= 5;
		
		int keepCount = 0;
		for(int i = 0; i < 5; ++i)
		{
			if(dataGroup.get(i).mVoltage < averageVoltage * 1 + ACCEPTANCE_RATIO && 
					dataGroup.get(i).mVoltage > averageVoltage * 1 - ACCEPTANCE_RATIO);
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
				bufferedWriters.get(i).write("" + 
						dataToWrite.get(i).get(lastIndex).mTime + "\t" + 
						dataToWrite.get(i).get(lastIndex).mResistance);
				bufferedWriters.get(i).newLine();
				bufferedWriters.get(i).flush();
			} 
			catch (IOException e) 
			{
				Log.e("ArduinoGUI", "Error writing Chip Data to File ");
				e.printStackTrace();
			}
		}
	}
	
	public void writeHumidityData(ArrayList<Data> dataToWrite)
	{
		//TODO Change to write humidity in % instead of bitVoltage
		int lastIndex =  dataToWrite.size() - 1;
		if(lastIndex < 0)
			return;
		try 
		{
			bufferedWriters.get(HUMIDITY).write("" + 
					dataToWrite.get(lastIndex).mTime + "\t" + 
					dataToWrite.get(lastIndex).mVoltage);
			bufferedWriters.get(HUMIDITY).newLine();
			bufferedWriters.get(HUMIDITY).flush();
		} 
		catch (IOException e) 
		{
			Log.e("ArduinoGUI", "Error writing Humidity Data to File ");
			e.printStackTrace();
		}
	}
	
	public void writeTempData(ArrayList<Data> dataToWrite)
	{
		//TODO Change to write temp in C instead of bitVoltage
		int lastIndex = dataToWrite.size() - 1;
		if(lastIndex < 0)
			return;
		try 
		{
			bufferedWriters.get(TEMPERATURE).write("" + 
					dataToWrite.get(lastIndex).mTime + "\t" + 
					dataToWrite.get(lastIndex).mVoltage);
			bufferedWriters.get(TEMPERATURE).newLine();
			bufferedWriters.get(TEMPERATURE).flush();
		} 
		catch (IOException e) 
		{
			Log.e("ArduinoGUI", "Error writing Temperature Data to File ");
			e.printStackTrace();
		}
	}

	/**
	 * Adds the data received from the Arduino to the graph and Data ArrayList
	 * @param values
	 * @param pollRate
	 * @param timeStarted
	 */
	public void addData(int[] values, int pollRate, Calendar timeStarted)
	{
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			//TODO Change to add time in minutes
			dataPoints.get(i).add(new Data(values[i], 
					dataPoints.get(i).size() * pollRate / MS_PER_MIN, i));
			if(AVERAGE_DATA && dataPoints.get(i).size() > POINTS_TO_AVERAGE)
					averagedData.get(i).add(calculateAverage(i));
		}
		
		if(AVERAGE_DATA && dataPoints.get(7).size() > POINTS_TO_AVERAGE)
		{
			writeChipData(averagedData);
			writeHumidityData(averagedData.get(HUMIDITY));
			writeTempData(averagedData.get(TEMPERATURE));
		}
		else
		{
			writeChipData(dataPoints);
			writeHumidityData(dataPoints.get(HUMIDITY));
			writeTempData(dataPoints.get(TEMPERATURE));
		}
		
		checkWindowLimits(values);
	}
	
	public void checkWindowLimits(int[] values)
	{
		int lastIndex = 0;
		//If the newly added value is greater than the max or less than the min
		//it sets the max and min accordingly for the autoscaling
		for(int i = 0; i < CHIP; ++i)
		{
			int bitResistance;
			if(AVERAGE_DATA)
			{
				lastIndex = averagedData.get(i).size() - 1;
				if(lastIndex < 0)
					return;
				bitResistance = averagedData.get(i).get(lastIndex).mBitResistance;
			}
			else
			{
				lastIndex = dataPoints.get(i).size() - 1;
				if(lastIndex < 0)
					return;
				bitResistance = dataPoints.get(i).get(lastIndex).mBitResistance;
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
		
		if(values[HUMIDITY] > mMaxValues[HUMIDITY])
			mMaxValues[HUMIDITY] = values[HUMIDITY];
		if(values[HUMIDITY] < mMinValues[HUMIDITY])
			mMinValues[HUMIDITY] = values[HUMIDITY];
	}
	
	public void setDisplayMode(int displayMode)
	{
		mDisplayMode = displayMode;
	}
	
	public int getDisplayMode()
	{
		return mDisplayMode;
	}
	
	public void setAutoScaling(boolean autoScaling)
	{
		mAutoScaling = autoScaling;
	}
}
