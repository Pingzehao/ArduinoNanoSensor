package edu.ucr.arduinoioio;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.LinkedList;

import smarttools.ucr.edu.remotesensors.ClientConnect;
import smarttools.ucr.edu.remotesensors.common.Datagram;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;
import edu.ucr.arduinorover.R;

public class GraphActivity extends IOIOActivity implements SensorEventListener
{
	public static final int ROVER_RX_PIN = 5;
	public static final int ROVER_TX_PIN = 6;
	public static final int GPS_RX_PIN = 27;
	public static final int GPS_TX_PIN = 28;
	
	/**
	 * Place movements of robot on a stack so it can reverse it's movements and go back easily
	 * Tracking down point source... perhaps using gps to mark its location, but it wouldn't be accurate enough
	 * Could use GPS for outside tracking down a general area... can also use the gps heading..
	 * mapping a room out in an x,y grid marking inaccessible x,y coordinates? but double check them
	 * 
	 * 
	 */
	public static final int DISTANCE_SENSORS = 3;
	public static final int LEFT_SENSOR_PIN = 7;
	public static final int FRONT_SENSOR_PIN = 12;
	public static final int RIGHT_SENSOR_PIN = 14;
	public static final int LEFT = 0;
	public static final int FRONT = 1;
	public static final int RIGHT = 2;
	private SensorManager mSensorManager;
	
	private byte mSpeed = 0x20;
	
	private static double[] mLastAccel = new double[3];
	private static double[] mNeutralAccel = new double[3];
	private static double[] mDeltaAccel = new double[3];
	
	private double mXAccel = 0;
	private double mYAccel = 0;
	private double mZAccel = 0;
	
	private Uart mRoverUart;
	private Uart mGPSUart;
	private InputStream mRoverRX;
	private InputStream mGPSRX;
	private OutputStream mRoverTX;
	private OutputStream mGPSTX;
	
	private static boolean mAccelControl;
	private static boolean mWallFollower;
	
	private static final int MISO = 9;
	private static final int SS = 10;
	private static final int MOSI = 11;
	private static final int CLK = 13;

	private static final int ADC0 = 31;

	private static final int MUX0 = 1;
	private static final int MUX_PINS = 4;
	private static final int ANALOG_PINS = 4;

	private static final int CHIP_PINS = 16;

	/** Number of inputs to be read (number of sensors) */
	private static final int ANALOG_INPUTS = 19;

	private static final int CHIP = 15;
	private static final int MQ2 = 16;
	private static final int HUMIDITY = 17;
	private static final int TEMPERATURE = 18;
	private static final int GPS = 19;

	private static GraphView mGraphView;
	private static Calendar mTimeStarted;
	private static boolean[] mPinsChecked = new boolean[ANALOG_INPUTS];
	private static boolean mIsPolling = false;
	private static int mPollingRate = 500;
	
	private static long mLastPoll = 0;

	private static int[] mDividerResistances = new int[CHIP_PINS];

	private static boolean mFirstRun = true;

	private static ClientConnect mClient;

	private LinkedList<Datagram> mNetworkData = new LinkedList<Datagram>();

	private Thread mNetworkThread = new Thread(new Runnable()
	{
		@Override
		public void run()
		{
			while(true)
			{
				if(mClient != null)
				{
					while(!mNetworkData.isEmpty())
					{
						mClient.send(mNetworkData.removeFirst());
					}
				}

				try
				{
					Thread.sleep(100);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	});
	
	

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph_tab);

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
		
		mGraphView = (GraphView) findViewById(R.id.graphView);
		mNetworkThread.start();
	}

	public boolean onTouchEvent(MotionEvent event)
	{
		if(event.getAction() == MotionEvent.ACTION_UP)
		{
			switch(mGraphView.getDisplayMode())
			{
				case CHIP:
					mGraphView.setDisplayMode(MQ2);
					Toast.makeText(getApplicationContext(), "MQ2 Sensor Data", Toast.LENGTH_SHORT).show();
					break;
				case MQ2:
					mGraphView.setDisplayMode(HUMIDITY);
					Toast.makeText(getApplicationContext(), "Humidity Sensor Data", Toast.LENGTH_SHORT).show();
					break;
				case HUMIDITY:
					mGraphView.setDisplayMode(TEMPERATURE);
					Toast.makeText(getApplicationContext(), "Temperature Sensor Data", Toast.LENGTH_SHORT).show();
					break;
				case TEMPERATURE:
					mGraphView.setDisplayMode(GPS);
					Toast.makeText(getApplicationContext(), "GPS Data", Toast.LENGTH_SHORT).show();
					break;
				case GPS:
					mGraphView.setDisplayMode(CHIP);
					Toast.makeText(getApplicationContext(), "Chip Sensor Data", Toast.LENGTH_SHORT).show();
					break;
			}
			return true;
		}
		return true;
	}

	public static boolean isPolling()
	{
		return mIsPolling;
	}

	public static void startPolling(boolean[] pins, int pollingRate, String ip, int port)
	{
		mClient = new ClientConnect(ip, port);
		mIsPolling = true;
		mGraphView.reset();
		mTimeStarted = Calendar.getInstance();
		mGraphView.createFiles(mTimeStarted);
		mGraphView.setPollingRate(pollingRate);
		mGraphView.startGPS();
		mPollingRate = pollingRate;
		mFirstRun = true;
	}

	public static void stopPolling()
	{
		mIsPolling = false;
		mFirstRun = true;
		mGraphView.closeFiles();
		if(mClient != null)
			mClient.close();
	}

	public static void setPinsChecked(boolean[] pins)
	{
		mPinsChecked = pins;
	}

	public static GraphView getGraphView()
	{
		return mGraphView;
	}

	class Looper extends BaseIOIOLooper
	{
		private DigitalOutput mLED;
		private DigitalOutput[] mMUXPins = new DigitalOutput[MUX_PINS];
		private DigitalInput[] mDistanceSensors = new DigitalInput[DISTANCE_SENSORS];

		private SpiMaster mSPI;
		private AnalogInput[] mAnalogInputs = new AnalogInput[ANALOG_PINS];
		
		/**public Thread mWallFollowerThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while(true)
				{
					if(mWallFollower && mIsPolling)
					{
						boolean isBlocked[] = new boolean[DISTANCE_SENSORS];
						for(int i = 0; i < DISTANCE_SENSORS; ++i)
						{
							try
							{
								isBlocked[i] = !mDistanceSensors[i].read();
							}
							catch(InterruptedException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							catch(ConnectionLostException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						//Backup every 15 seconds?
						
						if(isBlocked[FRONT])
						{
							setWheels(25, -25);
						}
						else
						{
							setWheels(0, 0);
						}
					}	
				}
			}
		});	**/
		
		@Override
		protected synchronized void setup() throws ConnectionLostException, InterruptedException
		{
			mLED = ioio_.openDigitalOutput(0, true);
			initializeUART();
			initializeSPI();
			initializeA2D();
			initializeMUX();
			initializeDistanceSensors();
		}
		
		public void initializeUART()
		{
			try
			{
				mRoverUart = ioio_.openUart(ROVER_RX_PIN, ROVER_TX_PIN, 115200, Uart.Parity.NONE, Uart.StopBits.ONE);
				mRoverRX = mRoverUart.getInputStream();
				mRoverTX = mRoverUart.getOutputStream();
				
				//TODO: Implement GPS
				/**
				mGPSUart = ioio_.openUart(GPS_RX_PIN, GPS_TX_PIN, 115200, Uart.Parity.NONE, Uart.StopBits.ONE);
				mGPSRX = mGPSUart.getInputStream();
				mGPSTX = mGPSUart.getOutputStream();
				**/
			}
			catch(ConnectionLostException e)
			{
				e.printStackTrace();
			}
		}

		public void initializeMUX()
		{
			for(int i = 0; i < MUX_PINS; ++i)
			{
				try
				{
					mMUXPins[i] = ioio_.openDigitalOutput(MUX0 + i);
					mMUXPins[i].write(false);
				}
				catch(ConnectionLostException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		public void initializeDistanceSensors() throws ConnectionLostException
		{
			mDistanceSensors[LEFT] = ioio_.openDigitalInput(LEFT_SENSOR_PIN);
			mDistanceSensors[FRONT] = ioio_.openDigitalInput(FRONT_SENSOR_PIN);
			mDistanceSensors[RIGHT] = ioio_.openDigitalInput(RIGHT_SENSOR_PIN);
//			mWallFollowerThread.start();
		}

		public void matchResistances() throws InterruptedException, ConnectionLostException
		{
			for(int i = 0; i < CHIP_PINS; ++i)
			{
				mDividerResistances[i] = matchResistance(i, 0, 255);
				wait(10);
			}
			sendDividerResistances(mDividerResistances);
		}

		public void initializeSPI() throws ConnectionLostException
		{
			//TODO: Try lowering the rate to fix the delay. Send dummy signal for 10ms delay or batch them. Make it match during the polling rate delay
			mSPI = ioio_.openSpiMaster(MISO, MOSI, CLK, SS, SpiMaster.Rate.RATE_125K);
		}
		
		public byte[] intToByteArray(int value)
		{
			byte[] byteAddress = new byte[4];
			for(int i = 0; i < 4; ++i)
			{
				int offset = (byteAddress.length - 1 - i) * 8;
				byteAddress[i] = (byte) ((value >>> offset) & 0xFF);
			}
			return byteAddress;
		}

		public synchronized void digitalPotWrite(int address, int value) throws ConnectionLostException, InterruptedException
		{
			byte[] byteAddress = new byte[1];
			byte[] byteValue = new byte[1];
			byteAddress[0] = (byte) (address & 0xFF);
			byteValue[0] = (byte) (value & 0xFF);
			mSPI.writeRead(byteAddress, byteAddress.length, byteAddress.length, null, 0);
			wait(20);
			mSPI.writeRead(byteValue, byteValue.length, byteValue.length, null, 0);
			wait(20);
		}

		public int matchResistance(int pin, int low, int high) throws InterruptedException, ConnectionLostException
		{
			setMUX(pin);
			int mid = (low + high) / 2;
			digitalPotWrite(0, mid);
			int voltage = (int) (mAnalogInputs[0].read() * 1023);

			if(low > high)
			{
				return mid;
			}

			if(voltage < 512)
			{
				return matchResistance(pin, low, mid - 1);
			}
			else if(voltage > 512)
			{
				return matchResistance(pin, mid + 1, high);
			}
			else
				return mid;
		}

		public void initializeA2D() throws ConnectionLostException
		{
			for(int i = 0; i < ANALOG_PINS; ++i)
			{
				mAnalogInputs[i] = ioio_.openAnalogInput(ADC0 + i);
			}
		}

		public void addData(final int[] bitVoltages)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					mGraphView.addData(bitVoltages, mTimeStarted);
					Datagram d = new Datagram("client" + mClient.getPort(), bitVoltages[0], bitVoltages[1], bitVoltages[2], bitVoltages[3], bitVoltages[4], bitVoltages[5], bitVoltages[6],
							bitVoltages[7], bitVoltages[8], bitVoltages[9], bitVoltages[10], bitVoltages[11], bitVoltages[12], bitVoltages[13], bitVoltages[14], bitVoltages[15], bitVoltages[16],
							bitVoltages[17], bitVoltages[18], mGraphView.getLatitude(), mGraphView.getLongitude());
					mNetworkData.add(d);
				}
			});
		}

		public void sendDividerResistances(int[] dividerResistances)
		{
			Datagram d = new Datagram("init", dividerResistances[0], dividerResistances[1], dividerResistances[2], dividerResistances[3], dividerResistances[4], dividerResistances[5],
					dividerResistances[6], dividerResistances[7], dividerResistances[8], dividerResistances[9], dividerResistances[10], dividerResistances[11], dividerResistances[12],
					dividerResistances[13], dividerResistances[14], dividerResistances[15], mPollingRate, 0, 0, 0, 0);
			mClient.send(d);
		}

		public void setDividerResistances()
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					mGraphView.setDividerResistances(mDividerResistances);
				}
			});
		}

		public synchronized void setMUX(int pin)
		{
			final int finalPin = pin;
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					try
					{
						if((finalPin & 0x0001) == 1)
							mMUXPins[0].write(true);
						else
							mMUXPins[0].write(false);
						if((finalPin & 0x0002) == 2)
							mMUXPins[1].write(true);
						else
							mMUXPins[1].write(false);
						if((finalPin & 0x0004) == 4)
							mMUXPins[2].write(true);
						else
							mMUXPins[2].write(false);
						if((finalPin & 0x0008) == 8)
							mMUXPins[3].write(true);
						else
							mMUXPins[3].write(false);
					}
					catch(ConnectionLostException e)
					{
						e.printStackTrace();
					}
				}
			});
		}

		@Override
		public synchronized void loop() throws ConnectionLostException, InterruptedException
		{
			if(mIsPolling && System.currentTimeMillis() - mLastPoll > mPollingRate)
			{
				mLastPoll = System.currentTimeMillis();
				if(mFirstRun)
				{
					matchResistances();
					setDividerResistances();
					mFirstRun = false;
				}
				int[] bitVoltages = new int[ANALOG_INPUTS];

				/** Read nano sensor values */
				for(int i = 0; i < CHIP_PINS; ++i)
				{
					setMUX(i);
					digitalPotWrite(0, mDividerResistances[i]);
					bitVoltages[i] = (int) (mAnalogInputs[0].read() * 1023);
				}

				/** Read other sensor values */
				for(int i = 1; i < ANALOG_PINS; ++i)
				{
					bitVoltages[i + CHIP_PINS - 1] = (int) (mAnalogInputs[i].read() * 1023);
				}
				
				//TODO: Read GPS NMEA string and pass to graph view (ends on nl/cr?)
				

				mGraphView.setHiddenPins(mPinsChecked);
				addData(bitVoltages);
			}

			runOnUiThread(new Runnable()
			{
				public void run()
				{
					if(mWallFollower && mIsPolling)
					{
						boolean isBlocked[] = new boolean[DISTANCE_SENSORS];
						for(int i = 0; i < DISTANCE_SENSORS; ++i)
						{
							try
							{
								isBlocked[i] = !mDistanceSensors[i].read();
							}
							catch(InterruptedException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							catch(ConnectionLostException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						//Backup every 15 seconds?
						
						if(isBlocked[FRONT])
						{
							setWheels(25, -25);
						}
						else
						{
							setWheels(0, 0);
						}
					}	
				}
			});	
			
			mLED.write(false);
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper()
	{
		return new Looper();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && mIsPolling)
		{
			Log.v("Accel", event.values[0] + "," + event.values[1] + "," + event.values[2]);
			for(int i = 0; i < 3; ++i)
			{
				mLastAccel[i] = event.values[i];
				mDeltaAccel[i] = mLastAccel[i] - mNeutralAccel[i];
			}
			if(mAccelControl)
			{
				moveRelative(mDeltaAccel[0], mDeltaAccel[1]);
			}
		}
	}
	
	public void stopMovement()
	{
		if(mRoverTX != null)
		{
			byte[] stopBuffer = {(byte) 0xC1, (byte) 0x00, (byte) 0xC5, (byte) 0x00};
			try
			{
				mRoverTX.write(stopBuffer);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void setWheels(double leftWheel, double rightWheel)
	{
		if(mRoverTX != null)
		{
			byte leftCommand = (byte) 0xC1;
			byte rightCommand = (byte) 0xC5;
			
			if(leftWheel < 0)
				leftCommand = (byte) 0xC2;
			if(rightWheel < 0)
				rightCommand = (byte) 0xC6;
			
			byte[] moveBuffer = {(byte) leftCommand, (byte) Math.abs(leftWheel), (byte) rightCommand, (byte) Math.abs(rightWheel)};
			try
			{
				mRoverTX.write(moveBuffer);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void moveRelative(double deltaX, double deltaY)
	{
		double turnRatio = deltaY / 9.8 / 1.25;
		if(turnRatio > 1)
			turnRatio = 1;
		else if(turnRatio < -1)
			turnRatio = -1;
	
		/** Split into sub if condition */
		if(Math.abs(deltaX) > 3)
		{
			double leftWheel = mSpeed;
			double rightWheel = mSpeed;
			if(Math.abs(deltaY) > 1)
			{
				if(turnRatio < 0)
				{
					leftWheel = mSpeed * (2 * turnRatio + 1);
				}
				else if(turnRatio > 0)
				{
					rightWheel = mSpeed * (-2 * turnRatio + 1);
				}
			}
			if(deltaX > 0)
			{
				leftWheel = -leftWheel;
				rightWheel = -rightWheel;
			}
			setWheels(leftWheel, rightWheel);
		}
		else
		{
			stopMovement();
		}
	}

	public static void setAccelerometer(boolean on)
	{
		mAccelControl = on;
		calibrateAccelerometer();
	}
	
	public static void setWallFollower(boolean on)
	{
		mWallFollower = on;
	}

	public static void calibrateAccelerometer()
	{
		for(int i = 0; i < 3; ++i)
			mNeutralAccel[i] = mLastAccel[i];
	}
}