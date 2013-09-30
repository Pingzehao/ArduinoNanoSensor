package edu.ucr.arduinoioio; 
import ioio.lib.api.AnalogInput;
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
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Toast;
import edu.ucr.arduinoioiov2.R;

public class GraphActivity extends IOIOActivity
{
	private Uart mRoverUart;
	private Uart mGPSUart;
	private InputStream mRXRover;
	private InputStream mRXGPS;
	private OutputStream mTXRover;
	private OutputStream mTXGPS;
	
	
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

	private static int SPI_DELAY = 40;
	static int MIN_POLLING_RATE = SPI_DELAY * CHIP_PINS * 2;
	
	private static GraphView mGraphView;
	private static Calendar mTimeStarted;
	private static boolean[] mPinsChecked = new boolean[ANALOG_INPUTS];
	private static boolean mIsPolling = false;
	
	private static int mPollingRate = MIN_POLLING_RATE + 100;

	private static int[] mDividerResistances = new int[CHIP_PINS];

	private static boolean mFirstRun = true;

	private static ClientConnect mClient;

	private LinkedList<Datagram> mNetworkData = new LinkedList<Datagram>();

	private static ProgressDialog mInitProgress;
	
	private Thread mNetworkThread = new Thread(new Runnable()
	{
		@Override
		public void run()
		{
			while(true)
			{
				if(mClient != null)
				{
					if(!mNetworkData.isEmpty())
					{
						mClient.send(mNetworkData.removeFirst());
						Log.d("Network", "Sent");
					}
				}

				try
				{
					Thread.sleep(10);
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

		mGraphView = (GraphView) findViewById(R.id.graphView);
		mNetworkThread.start();
		mInitProgress = new ProgressDialog(GraphActivity.this);
		mInitProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mInitProgress.setTitle("Initializing Sensors");
		mInitProgress.setMessage("Initializing...");
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
		//TODO: Make dialog for connecting 
		mClient = new ClientConnect(ip, port);
		mIsPolling = true;
		mGraphView.reset();
		mTimeStarted = Calendar.getInstance();
		mGraphView.createFiles(mTimeStarted);
		mGraphView.setPollingRate(pollingRate);
		mGraphView.startGPS();
		mPollingRate = pollingRate;
		mFirstRun = true;
		mInitProgress.show();
	}
	
	public static void stopPolling()
	{
		mIsPolling = false;
		mFirstRun = true;
		mGraphView.closeFiles();
		if(mClient != null)
			mClient.close();
		mInitProgress.setProgress(0);
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

		private SpiMaster mSPI;
		private AnalogInput[] mAnalogInputs = new AnalogInput[ANALOG_PINS];

		@Override
		protected synchronized void setup() throws ConnectionLostException, InterruptedException
		{
			mLED = ioio_.openDigitalOutput(0, true);
			initializeSPI();
			initializeA2D();
			initializeMUX();
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

		public void matchResistances() throws InterruptedException, ConnectionLostException
		{
			for(int i = 0; i < CHIP_PINS; ++i)
			{
				try
				{
					mDividerResistances[i] = matchResistance(i, 0, 255);
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
				mInitProgress.incrementProgressBy(100 / 16);
			}
			mInitProgress.setProgress(100);
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					mInitProgress.hide();
				}
			});
			sendDividerResistances(mDividerResistances);
			//TODO: Set dialog for initializing
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

		public synchronized void digitalPotWrite(int address, int value) throws ConnectionLostException, InterruptedException
		{
			byte[] byteAddress = new byte[1];
			byte[] byteValue = new byte[1];
			byteAddress[0] = (byte) (address & 0xFF);
			byteValue[0] = (byte) (value & 0xFF);
			mSPI.writeRead(byteAddress, byteAddress.length, byteAddress.length, null, 0);
			//TODO: Find out if theres a way to check if it's using a Bluetooth connection with the IOIO
			//		then set the wait duration correspondingly (about 10ms) seems to work for wired. 20 works ~100%
			//		Need about 30 for Bluetooth w/ 20 ms it seems to work but with some slight bugs
			wait(SPI_DELAY);
			mSPI.writeRead(byteValue, byteValue.length, byteValue.length, null, 0);
			wait(SPI_DELAY);
			//Is the problem with sending and the two spi commands are too close together? Or is it not a long enough delay between setting and reading
		}

		public synchronized int matchResistance(int pin, int low, int high) throws InterruptedException, ConnectionLostException
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
//					mClient.send(d);
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
			if(mIsPolling)
			{
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

				mGraphView.setHiddenPins(mPinsChecked);
				addData(bitVoltages);
			}
			wait(mPollingRate - MIN_POLLING_RATE);
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
}