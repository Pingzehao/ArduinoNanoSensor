package edu.ucr.arduinoioio;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.util.Calendar;

import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Toast;

public class GraphActivity extends IOIOActivity
{
	private static final int MISO = 12;
	private static final int MOSI = 11;
	private static final int CLK = 13;
	private static final int SS = 10;

	private static final int ADC0 = 33;

	private static final int ANALOG_PINS = 8;

	private static final int CHIP = 4;
	private static final int MQ2 = 5;
	private static final int HUMIDITY = 6;
	private static final int TEMPERATURE = 7;
	private static final int GPS = 8;

	private static final float REFERENCE_VOLTAGE = 3.3f;

	private static GraphView mGraphView;
	private static Calendar mTimeStarted;
	private static boolean[] mPinsChecked = new boolean[ANALOG_PINS];
	private static boolean mIsPolling = false;
	private static int mPollingRate = 100;
	
	private static int[] mDividerResistances = new int[5];
	
	private static boolean mFirstRun = true;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph_tab);

		mGraphView = (GraphView) findViewById(R.id.graphView);
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

	public static void startPolling(boolean[] pins, int pollingRate, String email, float percentThreshold)
	{
		mIsPolling = true;
		mGraphView.reset();
		mTimeStarted = Calendar.getInstance();
		mGraphView.setAutoEmail(email);
		mGraphView.createFiles(mTimeStarted);
		mGraphView.setThreshold(percentThreshold);
		mGraphView.setPollingRate(pollingRate);
		mGraphView.startGPS();
		mPollingRate = pollingRate;
		mFirstRun = true;
	}

	public static void stopPolling()
	{
		mIsPolling = false;
		mGraphView.closeFiles();
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
		protected void setup() throws ConnectionLostException, InterruptedException
		{
			mLED = ioio_.openDigitalOutput(0, true);
			initializeSPI();
			initializeA2D();
		}

		public void matchResistances() throws InterruptedException, ConnectionLostException
		{
			for(int i = 0; i < CHIP; ++i)
			{
				mDividerResistances[i] = matchResistance(i, 0, 255);
			}
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
			byte[] byteAddress = intToByteArray(address);
			byte[] byteValue = intToByteArray(value);
			mSPI.writeRead(byteAddress, byteAddress.length, byteAddress.length, null, 0);
			mSPI.writeRead(byteValue, byteValue.length, byteValue.length, null, 0);
		}

		public int matchResistance(int pin, int low, int high) throws InterruptedException, ConnectionLostException
		{
			int mid = (low + high) / 2;
			digitalPotWrite(pin, mid);
			float voltage = mAnalogInputs[pin].read();

			if(low > high)
			{
				return mid;
			}

			if(voltage < 0.5)
			{
				return matchResistance(pin, low, mid - 1);
			}
			else if(voltage > 0.5)
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
					mGraphView.addData(bitVoltages, mPollingRate, mTimeStarted);
				}
			});

		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException
		{
			if(mIsPolling)
			{
				if(mFirstRun)
				{
					matchResistances();
					mGraphView.setDividerResistances(mDividerResistances);
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
}