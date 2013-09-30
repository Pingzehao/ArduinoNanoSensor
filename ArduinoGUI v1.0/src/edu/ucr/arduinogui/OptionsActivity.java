package edu.ucr.arduinogui;

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
import edu.ucr.arduinoguiv1.R;

public class OptionsActivity extends Activity {
	public static final int ANALOG_PINS = 8;
	
	private static Button startPolling;
	private Button setWindow;
    private ArrayList<CheckBox> pins = new ArrayList<CheckBox>();
    private CheckBox autoScaling;
    private EditText prValue;
    private EditText windowMin;
    private EditText windowMax;
    
    private static boolean startedPolling = false;
    private boolean[] pinsChecked = new boolean[ANALOG_PINS];
    private int pollingRate = 100;
    
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
	    setContentView(R.layout.options_tab);
	    startPolling = (Button) findViewById(R.id.startPollingButton);
	    setWindow = (Button) findViewById(R.id.setWindowButton);
	    pins.add((CheckBox) findViewById(R.id.pin0));
	    pins.add((CheckBox) findViewById(R.id.pin1));
	    pins.add((CheckBox) findViewById(R.id.pin2));
	    pins.add((CheckBox) findViewById(R.id.pin3));
	    pins.add((CheckBox) findViewById(R.id.pin4));
	    pins.add((CheckBox) findViewById(R.id.pin5));
	    //Humidity Sensor
	    pins.add((CheckBox) findViewById(R.id.pin6));
	    //Temperature Sensor
	    pins.add((CheckBox) findViewById(R.id.pin7));
		autoScaling = (CheckBox) findViewById(R.id.autoScaling);
		prValue = (EditText) findViewById(R.id.pollingRate);
		windowMin = (EditText) findViewById(R.id.windowMin);
		windowMax = (EditText) findViewById(R.id.windowMax);
		
		autoScaling.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(autoScaling.isChecked())
					GraphActivity.getGraphView().setAutoScaling(true);
				else
					GraphActivity.getGraphView().setAutoScaling(false);
			}
		});
			
		setWindow.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				int min = 0;
				int max = 1023;
				try
				{
					min = Integer.parseInt(windowMin.getText() + "");
				}
				catch(NumberFormatException e)
				{
					Toast.makeText(getApplicationContext(), "Invalid Window " +
							"Min", Toast.LENGTH_SHORT).show();
				}
				try
				{
					max = Integer.parseInt(windowMax.getText() + "" );
				}
				catch(NumberFormatException e)
				{
					Toast.makeText(getApplicationContext(), "Invalid Window " +
							"Max" , Toast.LENGTH_SHORT).show();
					return;
				}
				autoScaling.setChecked(false);
				GraphActivity.getGraphView().setAutoScaling(false);
				GraphActivity.setWindow(min, max);
			}
		});
		
		pins.get(0).setOnCheckedChangeListener(new OnCheckedChangeListener() 
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				pinsChecked[0] = checked;
				GraphActivity.updatePinsChecked(pinsChecked);
			}
		});
		pins.get(1).setOnCheckedChangeListener(new OnCheckedChangeListener() 
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				pinsChecked[1] = checked;
				GraphActivity.updatePinsChecked(pinsChecked);
			}
		});
		pins.get(2).setOnCheckedChangeListener(new OnCheckedChangeListener() 
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				pinsChecked[2] = checked;
				GraphActivity.updatePinsChecked(pinsChecked);
			}
		});
		pins.get(3).setOnCheckedChangeListener(new OnCheckedChangeListener() 
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				pinsChecked[3] = checked;
				GraphActivity.updatePinsChecked(pinsChecked);
			}
		});
		pins.get(4).setOnCheckedChangeListener(new OnCheckedChangeListener() 
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				pinsChecked[4] = checked;
				GraphActivity.updatePinsChecked(pinsChecked);
			}
		});
		pins.get(5).setOnCheckedChangeListener(new OnCheckedChangeListener() 
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				pinsChecked[5] = checked;
				GraphActivity.updatePinsChecked(pinsChecked);
			}
		});
		pins.get(6).setOnCheckedChangeListener(new OnCheckedChangeListener() 
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				pinsChecked[6] = checked;
				GraphActivity.updatePinsChecked(pinsChecked);
			}
		});
		pins.get(7).setOnCheckedChangeListener(new OnCheckedChangeListener() 
		{
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				pinsChecked[7] = checked;
				GraphActivity.updatePinsChecked(pinsChecked);
			}
		});
		
		startPolling.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(GraphActivity.mSerialService.getState() == 
					BluetoothSerialService.STATE_NONE)
				{
					Toast.makeText(getApplicationContext(), 
							"Device not Connected", Toast.LENGTH_SHORT).show();
					return;
				}
				if(!startedPolling)
				{
					String output = "R";
					for(int i = 0; i < ANALOG_PINS; ++i)
					{
							if(pinsChecked[i])
							{
								output += (char) (i + 97);
							}
					}
					if(output == "R")
					{
						Toast.makeText(getApplicationContext(), 
								"No Pins Checked", Toast.LENGTH_SHORT).show();
						return;
					}
					output += ".";
					if(prValue.getText().length() > 2)
						output += prValue.getText();
					else
						output += "100";
					output += ".";
					Log.d("ArduinoGUI", output);
					startPolling.setText("Stop Polling");
					startedPolling = true;
					try
					{
						pollingRate = Integer.parseInt(prValue.getText() + "");
						Log.v("ArduinoGUI", "op" + pollingRate);
					}
					catch(NumberFormatException n)
					{
						Log.e("ArduinoGUI", "ERROR PARSING POLLING RATE");
					}
					GraphActivity.startPolling(pinsChecked, output, pollingRate);
					
				}
				else
				{
					startPolling.setText("Start Polling");
					GraphActivity.stopPolling();
					startedPolling = false;
				}
			}
		});
    }    
    //Prevents back button from killing the app
    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
	     if (keyCode == KeyEvent.KEYCODE_BACK) 
	     {
	    	 return true;
	     }
	     return super.onKeyDown(keyCode, event);    
	}
    
    public static void stopPolling()
    {
    	startedPolling = false;
    	startPolling.setText("Start Polling");
    }
}
