package edu.ucr.arduinogui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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



public class GraphView extends SurfaceView implements SurfaceHolder.Callback 
{
	public class Data
	{
		public int voltage;
		public int time;
		Data(int v, int t)
		{
			voltage = v;
			time = t;
		}
	}
	
	//Number of Analog Input Pins on the Arduino board
	private static final int ANALOG_PINS = 6;
	
	//Reference voltage supplied to the chip
	private static final double REFERENCE_VOLTAGE = 5.0;
	
	//Main thread
	private GraphThread mThread = null;
	
	//Flag indicating the graphView be reset
	private boolean doReset = false;
	
	//Boolean array that allows for the drawing of just certain pins
	private boolean[] hiddenPins = new boolean[ANALOG_PINS];
	
	//Window scaling values
	private int minValue = 1023;
	private int maxValue = 0;
	private int pollingRate = 100;
	private double windowScale = 1;
	private double windowMin = 0;
	private double windowMax = 1023;
	
	//ArrayLists that store the data for each pin
	private ArrayList<Data> p0Data = new ArrayList<Data>();
	private ArrayList<Data> p1Data = new ArrayList<Data>();
	private ArrayList<Data> p2Data = new ArrayList<Data>();
	private ArrayList<Data> p3Data = new ArrayList<Data>();
	private ArrayList<Data> p4Data = new ArrayList<Data>();
	private ArrayList<Data> p5Data = new ArrayList<Data>();
	
	//Paint objects for different data colors
	private Paint p0Paint = new Paint();
	private Paint p1Paint = new Paint();
	private Paint p2Paint = new Paint();
	private Paint p3Paint = new Paint();
	private Paint p4Paint = new Paint();
	private Paint p5Paint = new Paint();
	
	//File objects for writing to txt files
	File p0File = null;
	FileWriter p0FileWriter = null;
	BufferedWriter p0Out = null;
	FileOutputStream p0FileOutputStream = null;
	File p1File = null;
	FileWriter p1FileWriter = null;
	BufferedWriter p1Out = null;
	FileOutputStream p1FileOutputStream = null;
	File p2File = null;
	FileWriter p2FileWriter = null;
	BufferedWriter p2Out = null;
	FileOutputStream p2FileOutputStream = null;
	File p3File = null;
	FileWriter p3FileWriter = null;
	BufferedWriter p3Out = null;
	FileOutputStream p3FileOutputStream = null;
	File p4File = null;
	FileWriter p4FileWriter = null;
	BufferedWriter p4Out = null;
	FileOutputStream p4FileOutputStream = null;
	File p5File = null;
	FileWriter p5FileWriter = null;
	BufferedWriter p5Out = null;
	FileOutputStream p5FileOutputStream = null;
	File root = Environment.getExternalStorageDirectory();
	
	public GraphView(Context context) {
		super(context);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		mThread = new GraphThread(holder, context, new Handler());
		setFocusable(true); // need to get the key events
	}
	
	public GraphView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		mThread = new GraphThread(holder, context, new Handler());
		setFocusable(true);
	}
	
	public GraphView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		mThread = new GraphThread(holder, context, new Handler());
		setFocusable(true);
	}

	public GraphThread getThread() {
		return mThread;
	}
	
	public void deleteFiles()
	{
		new File("/sdcard/ArduinoGUI/p0.txt").delete();
		new File("/sdcard/ArduinoGUI/p1.txt").delete();
		new File("/sdcard/ArduinoGUI/p2.txt").delete();
		new File("/sdcard/ArduinoGUI/p3.txt").delete();
		new File("/sdcard/ArduinoGUI/p4.txt").delete();
		new File("/sdcard/ArduinoGUI/p5.txt").delete();
	}
	
	public void hidePin(int n)
	{
		hiddenPins[n] = true;
	}
	
	public void unhidePin(int n)
	{
		hiddenPins[n] = false;
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
		while (retry) {
			try {
				mThread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	class GraphThread extends Thread {
		private SurfaceHolder mSurfaceHolder;
		private boolean mRun = false;
		
		public GraphThread(SurfaceHolder holder, Context context, 
				Handler handler) 
		{
			mSurfaceHolder = holder;
		}
		public void setRunning(boolean b) {
			mRun = b;
		}

		public void run() {
			Canvas c;
			while (mRun) {
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
	
	public void setupOutput(Calendar timeStarted)
	{
		p0Paint.setColor(Color.GRAY);
		p1Paint.setColor(Color.BLUE);
		p2Paint.setColor(Color.GREEN);
		p3Paint.setColor(Color.RED);
		p4Paint.setColor(Color.YELLOW);
		p5Paint.setColor(Color.CYAN);
		try
		{
			int month = timeStarted.get(Calendar.MONTH);
			int day = timeStarted.get(Calendar.DAY_OF_MONTH);
			int hour = timeStarted.get(Calendar.HOUR_OF_DAY);
			int minute = timeStarted.get(Calendar.MINUTE);
			int millis = timeStarted.get(Calendar.MILLISECOND);
			File folder = new File(root + "/ArduinoGUI/");
			folder.mkdirs();
			
			if(root.canWrite())
			{
				p0File = new File(root + "/ArduinoGUI/", 
						month + "_" + day + "_" + hour + "_" + minute + "_" + 
						millis + "p0.txt");
				p0FileWriter = new FileWriter(p0File , true);
				p0Out = new BufferedWriter(p0FileWriter);
				p1File = new File(root + "/ArduinoGUI/", 
						month + "_" + day + "_" + hour + "_" + minute + "_" + 
						millis + "p1.txt");
				p1FileWriter = new FileWriter(p1File , true);
				p1Out = new BufferedWriter(p1FileWriter);
				p2File = new File(root + "/ArduinoGUI/", 
						month + "_" + day + "_" + hour + "_" + minute + "_" + 
						millis + "p2.txt");
				p2FileWriter = new FileWriter(p2File , true);
				p2Out = new BufferedWriter(p2FileWriter);
				p3File = new File(root + "/ArduinoGUI/", 
						month + "_" + day + "_" + hour + "_" + minute + "_" + 
						millis + "p3.txt");
				p3FileWriter = new FileWriter(p3File , true);
				p3Out = new BufferedWriter(p3FileWriter);
				p4File = new File(root + "/ArduinoGUI/", 
						month + "_" + day + "_" + hour + "_" + minute + "_" + 
						millis + "p4.txt");
				p4FileWriter = new FileWriter(p4File , true);
				p4Out = new BufferedWriter(p4FileWriter);
				p5File = new File(root + "/ArduinoGUI/", 
						month + "_" + day + "_" + hour + "_" + minute + "_" + 
						millis + "p5.txt");
				p5FileWriter = new FileWriter(p5File , true);
				p5Out = new BufferedWriter(p5FileWriter);
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
		p0Data.clear();
		p1Data.clear();
		p2Data.clear();
		p3Data.clear();
		p4Data.clear();
		p5Data.clear();
		maxValue = 0;
		minValue = 1024;
		doReset = false;
	}
	protected void onDraw(Canvas c) 
	{
		c.drawColor(Color.WHITE);
		drawData(c);
		drawAxis(c);
		if(doReset)
		{
			deleteData();
		}
	}
	
	public void drawAxis(Canvas c)
	{
		Paint axisPaint = new Paint();
		axisPaint.setColor(Color.BLACK);
		c.drawLine(15, 5, 15, 345, axisPaint);
		c.drawLine(5, getHeight() - 15, getWidth() - 5, getHeight() - 15, 
				axisPaint);
		c.drawText("" + windowMax, 20, 10, axisPaint);
		c.drawText("" + windowMin, 20, getHeight() - 5, axisPaint);
		c.drawText("" + pollingRate * p0Data.size() / 2 / 1000.0 + "s" , 
				(getWidth() - 30) / 2, getHeight() - 5, axisPaint);
		c.drawText("" + pollingRate * p0Data.size() / 1000.0 + "s", 
				getWidth() - 30, getHeight() - 5, axisPaint);
	}
	
	public void setPollingRate(int rate)
	{
		pollingRate = rate;
	}
	
	public void setWindow(int min, int max)
	{
		if(min >= max)
		{
			return;
		}
		else if(max > 1024 || max < 0)
		{
			return;
		}
		else if(min > 1024 || min < 0)
		{
			return;
		}
		windowMin = min;
		windowMax = max;
		windowScale = 1023 / (windowMax - windowMin);
	}
	
	public int getMin()
	{
		return minValue;
	}
	
	public int getMax()
	{
		return maxValue;
	}
	
	public void drawData(Canvas c)
	{
		float height = getHeight() - 15;
		float width = getWidth();
		float startX = 0; 
		float stopX =  0;
		float startY = 0;
		float stopY = 0;
		float scaleConstant = (float) (windowScale / 1023 * (height - 5));
		
		if(p0Data.size() < 2)
		{
			return;
		}
		
		//Draws all the pin data for the non hidden pins
		for(int i = 0; i < p0Data.size() - 2; i++)
		{
			startX = 15 + width / (p0Data.size() - 1) * i;
			stopX = 15 + width / (p0Data.size() - 1) * (i + 1);
			if(!hiddenPins[0])
			{
				startY = (float) (height - ((p0Data.get(i).voltage - windowMin) 
						* scaleConstant));
				stopY = (float) (height - (p0Data.get(i + 1).voltage - 
						windowMin) * scaleConstant);
				c.drawLine(startX, startY, stopX, stopY, p0Paint);
			}
			if(!hiddenPins[1])
			{
				startY = (float) (height - ((p1Data.get(i).voltage - windowMin) 
						* scaleConstant));
				stopY = (float) (height - (p1Data.get(i + 1).voltage - 
						windowMin) * scaleConstant);
				c.drawLine(startX, startY, stopX, stopY, p1Paint);
			}
			if(!hiddenPins[2])
			{
				startY = (float) (height - ((p2Data.get(i).voltage - windowMin) 
						* scaleConstant));
				stopY = (float) (height - (p2Data.get(i + 1).voltage - 
						windowMin) * scaleConstant);
				c.drawLine(startX, startY, stopX, stopY, p2Paint);
			}
			if(!hiddenPins[3])
			{
				startY = (float) (height - ((p3Data.get(i).voltage - windowMin) 
						* scaleConstant));
				stopY = (float) (height - (p3Data.get(i + 1).voltage - 
						windowMin) * scaleConstant);
				c.drawLine(startX, startY, stopX, stopY, p3Paint);
			}
			if(!hiddenPins[4])
			{
				startY = (float) (height - ((p4Data.get(i).voltage - windowMin) 
						* scaleConstant));
				stopY = (float) (height - (p4Data.get(i + 1).voltage - 
						windowMin) * scaleConstant);
				c.drawLine(startX, startY, stopX, stopY, p4Paint);
			}
			if(!hiddenPins[5])
			{
				startY = (float) (height - ((p5Data.get(i).voltage - windowMin) 
						* scaleConstant));
				stopY = (float) (height - (p5Data.get(i + 1).voltage - 
						windowMin) * scaleConstant);
				c.drawLine(startX, startY, stopX, stopY, p5Paint);
			}
		}
		
		//Gets the last pin value and formats it properly as a voltage
		DecimalFormat df = new DecimalFormat("#.###");
		double v0 = REFERENCE_VOLTAGE * (p0Data.get(p0Data.size() - 1).voltage / 
				1023.0);
		double v1 = REFERENCE_VOLTAGE * (p1Data.get(p1Data.size() - 1).voltage / 
				1023.0);
		double v2 = REFERENCE_VOLTAGE * (p2Data.get(p2Data.size() - 1).voltage / 
				1023.0);
		double v3 = REFERENCE_VOLTAGE * (p3Data.get(p3Data.size() - 1).voltage / 
				1023.0);
		double v4 = REFERENCE_VOLTAGE * (p4Data.get(p4Data.size() - 1).voltage / 
				1023.0);
		double v5 = REFERENCE_VOLTAGE * (p5Data.get(p5Data.size() - 1).voltage / 
				1023.0);
		
		//Draws the last pin value to the graph
		startX = getWidth() - 50;
		if(!hiddenPins[0])
		{
			startY = (float) (height - (p0Data.get(p0Data.size() - 1).voltage - 
					windowMin) * scaleConstant);
			c.drawText(df.format(v0) + "v", startX, startY, p0Paint);
		}
		if(!hiddenPins[1])
		{
			startY = (float) (height - (p1Data.get(p1Data.size() - 1).voltage - 
					windowMin) * scaleConstant);
			c.drawText(df.format(v1) + "v", startX, startY, p1Paint);
		}
		if(!hiddenPins[2])
		{
			startY = (float) (height - (p2Data.get(p2Data.size() - 1).voltage - 
					windowMin) * scaleConstant);
			c.drawText(df.format(v2) + "v", startX, startY, p2Paint);
		}
		if(!hiddenPins[3])
		{
			startY = (float) (height - (p3Data.get(p3Data.size() - 1).voltage - 
					windowMin) * scaleConstant);
			c.drawText(df.format(v3) + "v", startX, startY, p3Paint);
		}
		if(!hiddenPins[4])
		{
			startY = (float) (height - (p4Data.get(p4Data.size() - 1).voltage - 
					windowMin) * scaleConstant);
			c.drawText(df.format(v4) + "v", startX, startY, p4Paint);
		}
		if(!hiddenPins[5])
		{
			startY = (float) (height - (p4Data.get(p4Data.size() - 1).voltage - 
					windowMin) * scaleConstant);
			c.drawText(df.format(v5) + "v", startX, startY, p5Paint);
		}
	}
	
	/**
	 * Closes the files, called when done writing to the files
	 */
	public void closeFiles()
	{
		try
		{
			p0Out.close();
			p1Out.close();
			p2Out.close();
			p3Out.close();
			p4Out.close();
			p5Out.close();
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
		filePaths.add(Uri.fromFile(p0File));
		filePaths.add(Uri.fromFile(p1File));
		filePaths.add(Uri.fromFile(p2File));
		filePaths.add(Uri.fromFile(p3File));
		filePaths.add(Uri.fromFile(p4File));
		filePaths.add(Uri.fromFile(p5File));
		return filePaths;
	}
	
	/**
	 * Adds the data received from the Arduino to the graph and Data ArrayList
	 * @param values
	 * @param pollRate
	 * @param timeStarted
	 */
	public void addData(int[] values, int pollRate, Calendar timeStarted)
	{
		p0Data.add(new Data(values[0], p0Data.size() * pollRate));
		p1Data.add(new Data(values[1], p1Data.size() * pollRate));
		p2Data.add(new Data(values[2], p2Data.size() * pollRate));
		p3Data.add(new Data(values[3], p3Data.size() * pollRate));
		p4Data.add(new Data(values[4], p4Data.size() * pollRate));
		p5Data.add(new Data(values[5], p5Data.size() * pollRate));
		
		//Writes the time, and the value to the files
		try
		{
				p0Out.write("" + p0Data.size() * pollRate + "\t" + values[0]);
				p0Out.newLine();
				p0Out.flush();
				p1Out.write("" + p1Data.size() * pollRate + "\t" + values[1]);
				p1Out.newLine();
				p1Out.flush();
				p2Out.write("" + p2Data.size() * pollRate + "\t" + values[2]);
				p2Out.newLine();
				p2Out.flush();
				p3Out.write("" + p3Data.size() * pollRate + "\t" + values[3]);
				p3Out.newLine();
				p3Out.flush();
				p4Out.write("" + p4Data.size() * pollRate + "\t" + values[4]);
				p4Out.newLine();
				p4Out.flush();
				p5Out.write("" + p5Data.size() * pollRate + "\t" + values[5]);
				p5Out.newLine();
				p5Out.flush();
		}
		catch(IOException e)
		{
			Log.e("ArduinoGUI", "Error writing to file");
		}
		
		//If the newly added value is greater than the max or less than the min
		//it sets the max and min accordingly for the autoscaling
		for(int i = 0; i < ANALOG_PINS; ++i)
		{
			if(values[i] > maxValue)
			{
				maxValue = values[i];
			}
			if(values[i] < minValue)
			{
				minValue = values[i];
			}
		}
	}
}
