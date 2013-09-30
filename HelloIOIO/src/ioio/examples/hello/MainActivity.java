package ioio.examples.hello;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.util.ArrayList;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the on-board LED. This example shows a very simple usage of the IOIO, by using the {@link IOIOActivity} class. For a more
 * advanced use case, see the HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity
{
	private static final int MISO = 9;
	private static final int MOSI = 11;
	private static final int CLK = 13;
	private static final int SS = 10;

	private static final int ADC0 = 33;

	private static final int ANALOG_PINS = 8;

	private static final int CHIP = 4;
	private static final int MQ2 = 5;
	private static final int HUMIDITY = 6;
	private static final int TEMPERATURE = 7;

	private static final float REFERENCE_VOLTAGE = 3.3f;

	private ToggleButton button_;

	private TextView mDividerResistance;
	private TextView mOutputVoltage;
	private TextView mChipResistance;

	/**
	 * Called when the activity is first created. Here we normally initialize our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		button_ = (ToggleButton) findViewById(R.id.button);
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run every time the application is resumed and aborted when it is paused. The method setup() will be called right after a
	 * connection with the IOIO has been established (which might happen several times!). Then, loop() will be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper
	{
		private DigitalOutput led_;
		private DigitalOutput mChipSelect;
		private DigitalOutput mClock;
		private DigitalOutput mData;
		private AnalogInput[] mAnalogInputs = new AnalogInput[ANALOG_PINS];
		private SpiMaster mSPI;

		private int[] mDividerResistances = new int[5];

		/**
		 * Called every time a connection with IOIO has been established. Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException, InterruptedException
		{
			led_ = ioio_.openDigitalOutput(0, true);
			initializeSPI();
			initializeA2D();
			matchResistances();
		}

		public void matchResistances() throws InterruptedException, ConnectionLostException
		{
			for(int i = 0; i < CHIP; ++i)
			{
				Thread.sleep(15);
				int bitResistance = matchResistance(i, 0, 255);
				mDividerResistances[i] = bitResistance * 100000 / 255;
			}
		}

		public void initializeSPI() throws ConnectionLostException
		{
			/* Bit banging
			mChipSelect = ioio_.openDigitalOutput(SS, true);
			mClock = ioio_.openDigitalOutput(CLK, true);
			mData = ioio_.openDigitalOutput(MOSI, true);
			*/
			
			int[] spiSlaves = new int[1];
			spiSlaves[0] = SS;
			mSPI = ioio_.openSpiMaster(MISO, MOSI, CLK, spiSlaves, SpiMaster.Rate.RATE_125K);
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

		public void digitalPotWrite(byte address, byte value) throws ConnectionLostException, InterruptedException
		{
			/* Bit banging method
			mChipSelect.write(true);
			byte toWrite = address;
			for(int i = 0; i < 3; ++i)
			{
				if((toWrite & 0x04) == 0x04)
				{
					mData.write(true);
				}
				else
				{
					mData.write(false);
				}
				toWrite = (byte) (toWrite << 1);
				mClock.write(true);
				Thread.sleep(0, 1000);
				mClock.write(false);
			}
			
			toWrite = value;
			for(int i = 0; i < 8; ++i)
			{
				if((toWrite & 0x80) == 0x80)
				{
					mData.write(true);
				}
				else
				{
					mData.write(false);
				}
				mClock.write(true);
				Thread.sleep(0, 1000);
				mClock.write(false);
				toWrite = (byte) (toWrite << 1);
			}
			mChipSelect.write(false);
			*/

			byte[] byteAddress = new byte[1];
			byte[] byteValue = new byte[1];
			byteAddress[0] = address;
			byteValue[0] = value;
			mSPI.writeRead(byteAddress, byteAddress.length, byteAddress.length, null, 0);
			mSPI.writeRead(byteValue, byteValue.length, byteValue.length, null, 0);
		}

		public int matchResistance(int pin, int low, int high) throws InterruptedException, ConnectionLostException
		{
			int mid = (low + high) / 2;
			digitalPotWrite((byte) pin, (byte) mid);
			Thread.sleep(15);
			int voltage = (int) (mAnalogInputs[pin].read() * 1023);
			Thread.sleep(15);

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

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException
		{
			led_.write(!button_.isChecked());

			float scaledVoltage = mAnalogInputs[2].read() * 3.3f;
			float chipResistance = scaledVoltage * mDividerResistances[2] / (3.3f - scaledVoltage);
			/*
			 * for(int i = 0; i < CHIP; ++i) { float outputVoltage = mAnalogInputs[i].getVoltage(); float inputVoltage = REFERENCE_VOLTAGE; float chipResistance = mDividerResistances[i] *
			 * outputVoltage / (inputVoltage - outputVoltage); resistancesText += Float.toString(chipResistance) + "\n"; }
			 */

			final String dividerResistanceText = "Divider Resistance: \n" + Float.toString(mDividerResistances[2]);
			final String outputVoltageText = "Voltage: \n" + Float.toString(scaledVoltage);
			final String chipResistanceText = "Chip Resistance: \n" + Float.toString(chipResistance);
			
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					mDividerResistance.setText(dividerResistanceText);
					mOutputVoltage.setText(outputVoltageText);
					mChipResistance.setText(chipResistanceText);
					
				}
			});
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper()
	{
		return new Looper();
	}
}
