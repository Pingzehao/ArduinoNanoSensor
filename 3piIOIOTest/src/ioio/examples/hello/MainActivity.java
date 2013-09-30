package ioio.examples.hello;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ToggleButton;
import edu.ucr.threepicontroller.R;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the on-board LED. This example shows a very simple usage of the IOIO, by using the {@link IOIOActivity} class. For a more
 * advanced use case, see the HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity implements SensorEventListener
{
	private SensorManager mSensorManager;
	
	private ToggleButton mAccelerometerButton;
	private ToggleButton mRelativeMode;
	private Button mForward;
	private Button mLeft;
	private Button mRight;
	private Button mBack;
	private Button mReset;
	
	private SeekBar mSpeedBar;
	private byte mSpeed = 0x10;
	
	private double mXAccel = 0;
	private double mYAccel = 0;
	private double mZAccel = 0;
	
	private Uart mUart;
	private InputStream mRX;
	private OutputStream mTX;
	/**
	 * Called when the activity is first created. Here we normally initialize our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mAccelerometerButton = (ToggleButton) findViewById(R.id.button);
		mRelativeMode = (ToggleButton) findViewById(R.id.relativeModeToggle);
		mForward = (Button) findViewById(R.id.forwardButton);
		mLeft = (Button) findViewById(R.id.leftButton);
		mRight = (Button) findViewById(R.id.rightButton);
		mBack = (Button) findViewById(R.id.backButton);
		mReset = (Button) findViewById(R.id.resetButton);
		mSpeedBar = (SeekBar) findViewById(R.id.speedBar);
		mSpeedBar.setMax(100);
		mSpeedBar.setProgress(mSpeed);
		mTX = null;
		mRX = null;
		
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
		
		mForward.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
					moveForward();
				else if(event.getAction() == MotionEvent.ACTION_UP)
					stopMovement();
				return false;
			}
			
		});
		
		mBack.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
					moveBack();
				else if(event.getAction() == MotionEvent.ACTION_UP)
					stopMovement();
				return false;
			}
			
		});
		
		mLeft.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
					turnLeft();
				else if(event.getAction() == MotionEvent.ACTION_UP)
					stopMovement();
				return false;
			}
			
		});
		
		mRight.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
					turnRight();
				else if(event.getAction() == MotionEvent.ACTION_UP)
					stopMovement();
				return false;
			}
			
		});
		
		mReset.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				stopMovement();
			}
		});
	
		mSpeedBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				mSpeed = (byte) (progress + 5);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
		});
	}

	public void moveForward()
	{
		if(mTX != null)
		{
			byte[] forwardBuffer = {(byte) 0xC1, mSpeed, (byte) 0xC5, mSpeed};
			try
			{
				mTX.write(forwardBuffer);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void moveBack()
	{
		if(mTX != null)
		{
			byte[] backBuffer = {(byte) 0xC2, mSpeed, (byte) 0xC6, mSpeed};
			try
			{
				mTX.write(backBuffer);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void turnLeft()
	{
		if(mTX != null)
		{
			byte[] leftBuffer = {(byte) 0xC2, mSpeed, (byte) 0xC5, mSpeed};
			try
			{
				mTX.write(leftBuffer);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void turnRight()
	{
		if(mTX != null)
		{
			byte[] rightBuffer = {(byte) 0xC1, mSpeed, (byte) 0xC6, mSpeed};
			try
			{
				mTX.write(rightBuffer);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void stopMovement()
	{
		if(mTX != null)
		{
			byte[] stopBuffer = {(byte) 0xC1, (byte) 0x00, (byte) 0xC5, (byte) 0x00};
			try
			{
				mTX.write(stopBuffer);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void setWheels(double leftWheel, double rightWheel)
	{
		if(mTX != null)
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
				mTX.write(moveBuffer);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Moves the robot directionally
	 * X-axis controls forward/back movment
	 * Y-axis controls turning
	 * @param deltaX
	 * @param deltaY
	 */
	public void moveDirectional(double deltaX, double deltaY)
	{
		if(Math.abs(deltaX) > 4)
		{
			if(deltaX < 0)
			{
				moveForward();
			}
			else if(deltaX > 0)
			{
				moveBack();
			}
		}
		else if(Math.abs(deltaY) > 4)
		{
			if(deltaY < 0)
			{
				turnLeft();
			}
			else if(deltaY > 0)
			{
				turnRight();
			}
		}
		else if(!mForward.isPressed() && !mBack.isPressed() && !mRight.isPressed() && !mLeft.isPressed())
		{
			stopMovement();
		}
	}
	
	/**
	 * Moves the rover relative to the accelerometer values. 
	 * X-axis change calculates the speed of movement. 
	 * Y-axis change calculates the difference in speed of the wheels
	 * @param deltaX
	 * @param deltaY
	 */
	public void moveRelative(double deltaX, double deltaY)
	{
		double turnRatio = deltaY / 9.8 / 1.25;
		if(turnRatio > 1)
			turnRatio = 1;
		else if(turnRatio < -1)
			turnRatio = -1;
	
		/** Split into sub if condition */
		if(Math.abs(deltaX) > 4)
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
		else if(!mForward.isPressed() && !mBack.isPressed() && !mRight.isPressed() && !mLeft.isPressed())
		{
			stopMovement();
		}
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run every time the application is resumed and aborted when it is paused. The method setup() will be called right after a
	 * connection with the IOIO has been established (which might happen several times!). Then, loop() will be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper
	{
		private DigitalOutput led_;

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
			mUart = ioio_.openUart(5, 6, 115200, Uart.Parity.NONE, Uart.StopBits.ONE);
			mRX = mUart.getInputStream();
			mTX = mUart.getOutputStream();
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
			led_.write(false);
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					byte[] buffer = {(byte) 0xB7, (byte) 0xB8, (byte) 0x04, (byte) 'T', (byte) 'E', (byte) 'S', (byte) 'T'};
					try
					{
						mTX.write(buffer);
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}		
				}
			});
			
			Thread.sleep(1000);
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			if(!mAccelerometerButton.isChecked())
			{
				mXAccel = event.values[0];
				mYAccel = event.values[1];
				mZAccel = event.values[2];
				if(!mForward.isPressed() && !mBack.isPressed() && !mRight.isPressed() && !mLeft.isPressed())
					stopMovement();
			}
			
			/**
			 * Set speed scale based on X
			 * Set speed difference in wheels based on Y
			 */
			else if(mAccelerometerButton.isChecked())
			{
				
				//x positive go back
				double deltaX = event.values[0] - mXAccel;
				double deltaY = event.values[1] - mYAccel;
				
				if(mRelativeMode.isChecked())
					moveRelative(deltaX, deltaY);
				else
					moveDirectional(deltaX, deltaY);
			}
			Log.d("ArduinoIOIO", "x: " + mXAccel + "\ny: " + mYAccel + "\nz: " + mZAccel);
		}
	}
	
	
}