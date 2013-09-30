package edu.ucr.arduinoioio;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import edu.ucr.arduinorover.R;

public class OptionsActivity extends Activity
{
	public static final String LOG = "ArduinoIOIO";
	
	public static final int ANALOG_INPUTS = 19;

	private static Button mStartPolling;
	private Button mSetWindow;
	private Button mCheckAll;
	private ArrayList<CheckBox> mPins = new ArrayList<CheckBox>();
	private CheckBox mAutoScaling;
	private EditText mPollingRateValue;
	private EditText mWindowMin;
	private EditText mWindowMax;
	private EditText mServerIP;
	private CheckBox mAccelerometer;
	private CheckBox mWallFollower;
	private Button mCalibrate;
	
	
	private boolean[] mPinsChecked = new boolean[ANALOG_INPUTS];
	private int mPollingRate = 500;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.options_tab);
		mStartPolling = (Button) findViewById(R.id.startPollingButton);
		mSetWindow = (Button) findViewById(R.id.setWindowButton);
		mCheckAll = (Button) findViewById(R.id.checkAllButton);
	
		/** Nano sensor */
		mPins.add((CheckBox) findViewById(R.id.pin0));
		mPins.add((CheckBox) findViewById(R.id.pin1));
		mPins.add((CheckBox) findViewById(R.id.pin2));
		mPins.add((CheckBox) findViewById(R.id.pin3));
		mPins.add((CheckBox) findViewById(R.id.pin4));
		mPins.add((CheckBox) findViewById(R.id.pin5));
		mPins.add((CheckBox) findViewById(R.id.pin6));
		mPins.add((CheckBox) findViewById(R.id.pin7));
		mPins.add((CheckBox) findViewById(R.id.pin8));
		mPins.add((CheckBox) findViewById(R.id.pin9));
		mPins.add((CheckBox) findViewById(R.id.pin10));
		mPins.add((CheckBox) findViewById(R.id.pin11));
		mPins.add((CheckBox) findViewById(R.id.pin12));
		mPins.add((CheckBox) findViewById(R.id.pin13));
		mPins.add((CheckBox) findViewById(R.id.pin14));
		mPins.add((CheckBox) findViewById(R.id.pin15));
		
		/** Commercial Sensor */
		mPins.add((CheckBox) findViewById(R.id.pin16));
		
		/** Humidity Sensor */
		mPins.add((CheckBox) findViewById(R.id.pin17));
		
		/** Temperature Sensor */
		mPins.add((CheckBox) findViewById(R.id.pin18));
		
		for(int i = 0; i < mPinsChecked.length; ++i)
		{
			mPinsChecked[i] = false;
		}
		
		mAutoScaling = (CheckBox) findViewById(R.id.autoScaling);
		mPollingRateValue = (EditText) findViewById(R.id.pollingRate);
		mWindowMin = (EditText) findViewById(R.id.windowMin);
		mWindowMax = (EditText) findViewById(R.id.windowMax);
		mServerIP = (EditText) findViewById(R.id.serverIP);
		
		mAccelerometer = (CheckBox) findViewById(R.id.accelerometerCheckBox);
		mWallFollower = (CheckBox) findViewById(R.id.wallFollowerCheckBox);
		mCalibrate = (Button) findViewById(R.id.calibrateButton);

		mCheckAll.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				for(int i = 0; i < mPins.size(); ++i)
				{
					CheckBox tempCheckBox = mPins.get(i);
					tempCheckBox.setChecked(!tempCheckBox.isChecked());
				}
			}
		});
		
		mAutoScaling.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				GraphActivity.getGraphView().setAutoScaling(mAutoScaling.isChecked());
			}
		});
		
		mAccelerometer.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				mWallFollower.setChecked(false);
				GraphActivity.setWallFollower(mWallFollower.isChecked());
				GraphActivity.setAccelerometer(mAccelerometer.isChecked());
			}
		});
		
		mWallFollower.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				mAccelerometer.setChecked(false);
				GraphActivity.setAccelerometer(mAccelerometer.isChecked());
				GraphActivity.setWallFollower(mWallFollower.isChecked());
			}
		});
		
		mCalibrate.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				GraphActivity.calibrateAccelerometer();
			}
		});

		mSetWindow.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				int min = 0;
				int max = 1023;
				
				try
				{
					min = Integer.parseInt(mWindowMin.getText().toString());
				}
				catch (NumberFormatException e)
				{
					Toast.makeText(getApplicationContext(), "Invalid Window Min", Toast.LENGTH_SHORT).show();
				}
				
				try
				{
					max = Integer.parseInt(mWindowMax.getText().toString());
				}
				catch (NumberFormatException e)
				{
					Toast.makeText(getApplicationContext(), "Invalid Window Max", Toast.LENGTH_SHORT).show();
					return;
				}
				
				mAutoScaling.setChecked(false);
				
				if(min > max)
				{
					Toast.makeText(getApplicationContext(), "Invalid Window Min: Min must be less than Max", Toast.LENGTH_SHORT).show();
				}
				else if(min < 0)
				{
					Toast.makeText(getApplicationContext(), "Invalid Window Min: Min must be greater than 0", Toast.LENGTH_SHORT).show();
				}
				else if(max > 1023)
				{
					Toast.makeText(getApplicationContext(), "Invalid Window Max: Max must be less than 1024", Toast.LENGTH_SHORT).show();

				}
				
				GraphActivity.getGraphView().setAutoScaling(false);
				GraphActivity.getGraphView().setWindow(min, max);
			}
		});

		mPins.get(0).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[0] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(1).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[1] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(2).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[2] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(3).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[3] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(4).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[4] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(5).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[5] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(6).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[6] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(7).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[7] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(8).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[8] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(9).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[9] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(10).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[10] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(11).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[11] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(12).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[12] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(13).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[13] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(14).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[14] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(15).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[15] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(16).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[16] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(17).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[17] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		mPins.get(18).setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked)
			{
				mPinsChecked[18] = checked;
				GraphActivity.setPinsChecked(mPinsChecked);
			}
		});
		
		mStartPolling.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				int pinsChecked = 0;
				for(int i = 0; i < ANALOG_INPUTS; ++i)
				{
					if(mPinsChecked[i])
					{
						pinsChecked++;
					}
				}
				
				if(pinsChecked == 0)
				{
					Toast.makeText(getApplicationContext(), "No pins checked", Toast.LENGTH_SHORT).show();
					return;
				}
				
				if (!GraphActivity.isPolling())
				{
					/** Get and parse the polling rate from the EditText */
					try
					{
						mPollingRate = Integer.parseInt(mPollingRateValue.getText().toString());
					}
					catch(NumberFormatException n)
					{
						Log.e(LOG, "edu.ucr.arduinoioio::OptionsActivity.java::onClick(View v) - Error parsing polling rate");
					}
						
					/** Get and parse the server IP and server port from the EditTexts */
					String ip = mServerIP.getText().toString();
					String serverIP = "127.0.0.1";
					int serverPort = 8080;
					String[] serverInfo = ip.split(":");
					if(serverInfo.length == 2)
					{
						serverIP = serverInfo[0];
						try
						{
							serverPort = Integer.parseInt(serverInfo[1]);
						}
						catch(NumberFormatException n)
						{
							Log.e(LOG, "edu.ucr.arduinoioio::OptionsActivity.java::onClick(View v) - Error parsing server port");
						}
					
						/** Dont start if it's an invalid port or ip */
						if(serverPort > 65535)
						{
							Toast.makeText(getApplicationContext(), "Invalid server port", Toast.LENGTH_SHORT).show();
							return;
						}
						
						if(serverIP.length() > 15)
						{
							Toast.makeText(getApplicationContext(), "Invalid server ip", Toast.LENGTH_SHORT).show();
							return;
						}
					}
				
					Toast.makeText(getApplicationContext(), "Server IP: " + serverIP + "\tServer Port: " + serverPort, Toast.LENGTH_SHORT).show();
					mStartPolling.setText("Stop Polling");
					GraphActivity.startPolling(mPinsChecked, mPollingRate, serverIP, serverPort);
				}
				else
				{
					mStartPolling.setText("Start Polling");
					GraphActivity.stopPolling();
				}
			}
		});
	}

	// Prevents back button from killing the app
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
