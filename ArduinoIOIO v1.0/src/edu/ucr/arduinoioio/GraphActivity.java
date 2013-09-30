package edu.ucr.arduinoioio;

import edu.ucr.arduinoioiov1.R;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.util.Calendar;
import java.util.LinkedList;

import smarttools.ucr.edu.remotesensors.ClientConnect;
import smarttools.ucr.edu.remotesensors.common.Datagram;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;

public class GraphActivity extends IOIOActivity
{
	private static final int MISO = 9;
	private static final int SS = 10;
	private static final int MOSI = 11;
	private static final int CLK = 13;

	private static final int ADC0 = 33;

	private static final int ANALOG_PINS = 7;

	private static final int CHIP_PINS = 4;
	
	private static final int CHIP = 3;
	private static final int MQ2 = 4;
	private static final int HUMIDITY = 5;
	private static final int TEMPERATURE = 6;
	private static final int GPS = 7;

	private static GraphView mGraphView;
	private static Calendar mTimeStarted;
	private static boolean[] mPinsChecked = new boolean[ANALOG_PINS];
	private static boolean mIsPolling = false;
	private static int mPollingRate = 100;
	
	private static int[] mDividerResistances = new int[CHIP_PINS];
	
	private static boolean mFirstRun = true;

	private static ClientConnect mClient;
	
	private static LinkedList<Datagram> mNetworkData = new LinkedList<Datagram>();
	
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
					// TODO Auto-generated catch block
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
	
	public static void startPolling(boolean[] pins, int pollingRate, String email, float percentThreshold, final String ip, final int port)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				mClient = new ClientConnect(ip, port);
			}
		}).start();
		while(mClient == null);
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
		private SpiMaster mSPI;
		private AnalogInput[] mAnalogInputs = new AnalogInput[ANALOG_PINS];


		@Override
		protected synchronized void setup() throws ConnectionLostException, InterruptedException
		{
			mLED = ioio_.openDigitalOutput(0, true);
			initializeSPI();
			initializeA2D();
		}

		public void matchResistances() throws InterruptedException, ConnectionLostException
		{
			for(int i = 0; i < CHIP_PINS; ++i)
			{
				mDividerResistances[i] = matchResistance(i, 0, 255); 
			}
			Datagram d = new Datagram("init", mDividerResistances[0], mDividerResistances[1], mDividerResistances[2], mDividerResistances[3], mPollingRate, 0, 0, 0, 0);
			mNetworkData.add(d);
		}

		public void initializeSPI() throws ConnectionLostException
		{
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

		public void digitalPotWrite(int address, int value) throws ConnectionLostException, InterruptedException
		{
			byte[] byteAddress = new byte[1];
			byte[] byteValue = new byte[1];
			byteAddress[0] = (byte) (address & 0xFF);
			byteValue[0] = (byte) (value & 0xFF);
			mSPI.writeRead(byteAddress, byteAddress.length, byteAddress.length, null, 0);
			Thread.sleep(20);
			mSPI.writeRead(byteValue, byteValue.length, byteValue.length, null, 0);
			Thread.sleep(20);
		}

		public int matchResistance(int pin, int low, int high) throws InterruptedException, ConnectionLostException
		{
			int mid = (low + high) / 2;
			digitalPotWrite(pin, mid);
			int voltage = (int) (mAnalogInputs[pin].read() * 1023);
			
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
					final Datagram d = new Datagram("client" + mClient.getPort(), 
							bitVoltages[0], bitVoltages[1], bitVoltages[2], bitVoltages[3], bitVoltages[4], bitVoltages[5], bitVoltages[6], 
							mGraphView.getLatitude(), mGraphView.getLongitude());
					mNetworkData.add(d);
				}
			});
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
		
		@Override
		public synchronized void loop() throws ConnectionLostException, InterruptedException
		{
			if(mIsPolling)
			{
				if(mFirstRun)
				{
					matchResistances();
					setDividerResistances();
					mFirstRun = false;
				}
				int[] bitVoltages = new int[ANALOG_PINS];
				for(int i = 0; i < ANALOG_PINS; ++i)
				{
					bitVoltages[i] = (int) (mAnalogInputs[i].read() * 1023);
				}

				mGraphView.setHiddenPins(mPinsChecked);
				addData(bitVoltages);
			}
			Thread.sleep(mPollingRate);
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
	     if (keyCode == KeyEvent.KEYCODE_BACK) 
	     {
	    	 return true;
	     }
	     return super.onKeyDown(keyCode, event);    
	}
}