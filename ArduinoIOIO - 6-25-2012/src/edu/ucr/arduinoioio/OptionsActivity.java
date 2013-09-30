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

public class OptionsActivity extends Activity
{
	public static final String LOG = "ArduinoIOIO";
	
	public static final int ANALOG_PINS = 8;

	private static Button mStartPolling;
	private Button mSetWindow;
	private ArrayList<CheckBox> mPins = new ArrayList<CheckBox>();
	private CheckBox mAutoScaling;
	private EditText mPollingRateValue;
	private EditText mWindowMin;
	private EditText mWindowMax;
	private EditText mAutoEmail;
	private EditText mThresholdPercentage;

	private static boolean mIsPolling = false;
	private boolean[] mPinsChecked = new boolean[ANALOG_PINS];
	private int mPollingRate = 100;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.options_tab);
		mStartPolling = (Button) findViewById(R.id.startPollingButton);
		mSetWindow = (Button) findViewById(R.id.setWindowButton);
		mPins.add((CheckBox) findViewById(R.id.pin0));
		mPins.add((CheckBox) findViewById(R.id.pin1));
		mPins.add((CheckBox) findViewById(R.id.pin2));
		mPins.add((CheckBox) findViewById(R.id.pin3));
		mPins.add((CheckBox) findViewById(R.id.pin4));
		mPins.add((CheckBox) findViewById(R.id.pin5));
		// Humidity Sensor
		mPins.add((CheckBox) findViewById(R.id.pin6));
		// Temperature Sensor
		mPins.add((CheckBox) findViewById(R.id.pin7));
		mAutoScaling = (CheckBox) findViewById(R.id.autoScaling);
		mPollingRateValue = (EditText) findViewById(R.id.pollingRate);
		mWindowMin = (EditText) findViewById(R.id.windowMin);
		mWindowMax = (EditText) findViewById(R.id.windowMax);
		mAutoEmail = (EditText) findViewById(R.id.autoEmail);
		mThresholdPercentage = (EditText) findViewById(R.id.thresholdPercentage);

		mAutoScaling.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				GraphActivity.getGraphView().setAutoScaling(mAutoScaling.isChecked());
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

		mStartPolling.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!GraphActivity.isPolling())
				{
					try
					{
						mPollingRate = Integer.parseInt(mPollingRateValue.getText().toString());
					}
					catch(NumberFormatException n)
					{
						Log.e(LOG, "OptionsActivity::onClick(View v) - Error parsing polling rate");
					}
						
					mStartPolling.setText("Stop Polling");
					
					String email = mAutoEmail.getText().toString();
					float percentThreshold = 1;
					try
					{
						percentThreshold = Float.parseFloat(mThresholdPercentage.getText().toString());
					}
					catch(NumberFormatException n)
					{
						Log.e(LOG, "OptionsActivity::onClick(View v) - Error parsing threshold value");
					}
					
					GraphActivity.startPolling(mPinsChecked, mPollingRate, email, percentThreshold);
				}
				else
				{
					mStartPolling.setText("Start Polling");
					mIsPolling = false;
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
