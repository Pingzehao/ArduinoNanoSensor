package edu.ucr.arduinogui;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ManualActivity extends Activity {
	//TODO Make it so it displays the console response either using toast or another
	//tab that has the console. Also, for detecting the end of a console command, add 
	//a symbol like @ or something because the stream gets broken up sometimes
	
    private static final int DIGITAL_PINS = 14;
	private static final int IN = 1;
	private static final int OUT = 0;
	private static final int HIGH = 1;
	private static final int LOW = 0;
	
    public static String ANALOG_MODE;
    public static String ANALOG_PULL;
    public static String ANALOG_READ;
    public static String ANALOG_WRITE;
    public static String DIGITAL_MODE;
    public static String DIGITAL_PULL;
    public static String DIGITAL_READ;
    public static String DIGITAL_WRITE;
	
	private EditText awValue;
    private Button sendCommand;
    private ToggleButton dmToggle;
    private ToggleButton dpToggle;
    private RadioButton digitalMode;
    private RadioButton digitalPull;
    private RadioButton digitalRead;
    private RadioButton analogRead;
    private RadioButton analogWrite;
    private RadioGroup commandSelection;
    private ArrayList<CheckBox> pins = new ArrayList<CheckBox>();
    private boolean[] pinsChecked = new boolean[DIGITAL_PINS];
    private int dMode = 0;
    private int dPull = 0;
    private int dwValue = 0;
    private String radioSelected;
	
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
	    setContentView(R.layout.manual_tab);
	        
	    commandSelection = (RadioGroup) findViewById(R.id.commandSelection);
		digitalMode = (RadioButton) findViewById(R.id.digitalModeButton);
		digitalPull = (RadioButton) findViewById(R.id.digitalPullButton);
		digitalRead = (RadioButton) findViewById(R.id.digitalReadButton);
		analogRead = (RadioButton) findViewById(R.id.analogReadButton);
		analogWrite = (RadioButton) findViewById(R.id.analogWriteButton);
		sendCommand = (Button) findViewById(R.id.sendCommandButton);
		dmToggle = (ToggleButton) findViewById(R.id.dmToggle);
		dmToggle.setTextOn("IN");
		dmToggle.setTextOff("OUT");
		dmToggle.setChecked(false);
		dpToggle = (ToggleButton) findViewById(R.id.dpToggle);
		dpToggle.setTextOn("HIGH");
		dpToggle.setTextOff("LOW");
		dpToggle.setChecked(false);
		pins.add((CheckBox) findViewById(R.id.pin0));
		pins.add((CheckBox) findViewById(R.id.pin1));
		pins.add((CheckBox) findViewById(R.id.pin2));
		pins.add((CheckBox) findViewById(R.id.pin3));
		pins.add((CheckBox) findViewById(R.id.pin4));
		pins.add((CheckBox) findViewById(R.id.pin5));
		pins.add((CheckBox) findViewById(R.id.pin6));
		pins.add((CheckBox) findViewById(R.id.pin7));
		pins.add((CheckBox) findViewById(R.id.pin8));
		pins.add((CheckBox) findViewById(R.id.pin9));
		pins.add((CheckBox) findViewById(R.id.pin10));
		pins.add((CheckBox) findViewById(R.id.pin11));
		pins.add((CheckBox) findViewById(R.id.pin12));
		pins.add((CheckBox) findViewById(R.id.pin13));
		awValue = (EditText) findViewById(R.id.awValue);
		
		ANALOG_READ = (String) analogRead.getText();
		ANALOG_WRITE = (String) analogWrite.getText();
		DIGITAL_MODE = (String) digitalMode.getText();
		DIGITAL_PULL = (String) digitalPull.getText();
		DIGITAL_READ = (String) digitalRead.getText();
	     
		dmToggle.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(dmToggle.isChecked())
					dMode = IN;
		    	else
		    		dMode = OUT;
		    }
		});
		dpToggle.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
		    {
		    	if(dpToggle.isChecked())
		    		dPull = HIGH;
		    	else
		    		dPull = LOW;
		    }
	    });
	    pins.get(0).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[0] = true;
				else
					pinsChecked[0] = false;
			}
		});
	    pins.get(1).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[1] = true;
				else
					pinsChecked[1] = false;
			}
		});
	    pins.get(2).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[2] = true;
				else
					pinsChecked[2] = false;
			}
		});
	    pins.get(3).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[3] = true;
				else
					pinsChecked[3] = false;
			}
		});
		
	    pins.get(4).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[4] = true;
				else
					pinsChecked[4] = false;
			}
		});
	    pins.get(5).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[5] = true;
				else
					pinsChecked[5] = false;
			}
		});
	    pins.get(6).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[6] = true;
				else
					pinsChecked[6] = false;
			}
		});
	    pins.get(7).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[7] = true;
				else
					pinsChecked[7] = false;
			}
		});
	    pins.get(8).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[8] = true;
				else
					pinsChecked[8] = false;
			}
		});
	    pins.get(9).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[9] = true;
				else
					pinsChecked[9] = false;
			}
		});
	    pins.get(10).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[10] = true;
				else
					pinsChecked[10] = false;
			}
		});
	    pins.get(11).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[11] = true;
				else
					pinsChecked[11] = false;
			}
		});
	    pins.get(12).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[12] = true;
				else
					pinsChecked[12] = false;
			}
		});
	    pins.get(13).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[13] = true;
				else
					pinsChecked[13] = false;
			}
		});
		commandSelection.setOnCheckedChangeListener(new android.widget.
				RadioGroup.OnCheckedChangeListener()
		{
			   public void onCheckedChanged(RadioGroup arg0, int arg1) 
			   {
				   //TextView t = (TextView) findViewById(R.id.outputDisplay);
				    RadioButton rb = (RadioButton) findViewById(arg1);
				    radioSelected = (String) rb.getText();
				    
				    for(int i = 0; i < DIGITAL_PINS; ++i)
			    		enablePin(i);
				    
				    if(radioSelected == ANALOG_WRITE)
				    {
				    	disablePin(0);
				    	disablePin(1);
				    	disablePin(2);
				    	disablePin(4);
				    	disablePin(7);
				    	disablePin(8);
				    	disablePin(12);
				    	disablePin(13);
				    }
				    else if(radioSelected == ANALOG_READ)
				    	for(int i = 6; i < DIGITAL_PINS; ++i)
				    		disablePin(i);
				    else if(radioSelected == DIGITAL_MODE || 
				    		radioSelected == DIGITAL_PULL ||
				    		radioSelected == DIGITAL_READ)
				    {
				    	disablePin(0);
				    	disablePin(1);
				    }
			   }   
		});
		sendCommand.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				if(GraphActivity.mSerialService.isPolling())
				{
					Toast.makeText(getApplicationContext(), "Cannot Send " +
							"Command While Polling" , 
							Toast.LENGTH_SHORT).show();
					return;
				}
				if(GraphActivity.mSerialService.getState() == 
					BluetoothSerialService.STATE_NONE)
				{
					Toast.makeText(getApplicationContext(), 
							"Device not Connected", Toast.LENGTH_SHORT).show();
					return;
				}
				String outputCommand = "";
			    if(radioSelected == ANALOG_READ)
			    	outputCommand += "AR";
			    else if(radioSelected == ANALOG_WRITE)
			    	outputCommand += "AW";
			    else if(radioSelected == DIGITAL_MODE)
			    	outputCommand += "DM";
			    else if(radioSelected == DIGITAL_PULL)
			    	outputCommand += "DP";
			    else if(radioSelected == DIGITAL_READ)
			    	outputCommand += "DR";
			    else if(radioSelected == DIGITAL_WRITE)
			    	outputCommand += "DW";
			    
			    int i = 0;
			    for(; i < DIGITAL_PINS ; ++i)
			    {
			    	if(pinsChecked[i])
			    	{
			    		outputCommand += i;
			    		outputCommand += ".";
			    		break;
			    	}
			    }
			    
			    if(i == DIGITAL_PINS) //No pins checked
		    	{
		    		Toast.makeText(getApplicationContext(), 
		    				"No Pins Checked", Toast.LENGTH_SHORT).show();
		    		return;
		    	}
			    
			    if(radioSelected == ANALOG_WRITE)
			    	outputCommand = outputCommand + awValue.getText() + ".";
			    else if(radioSelected == DIGITAL_WRITE)
			    	outputCommand = outputCommand + dwValue + ".";
			    else if(radioSelected == DIGITAL_MODE)
		    		outputCommand = outputCommand + dMode + ".";
			    else if(radioSelected == DIGITAL_PULL)
		    		outputCommand = outputCommand + dPull + ".";
			    
			    Toast.makeText(getApplicationContext(), 
                		outputCommand, Toast.LENGTH_SHORT).show();
			    GraphActivity.send(outputCommand.getBytes());
			}
		});
    }
   
    public void disablePin(int pinNumber)
    {
    	pins.get(pinNumber).setClickable(false);
    	pins.get(pinNumber).setChecked(false);
    	pins.get(pinNumber).setTextColor(Color.rgb(40, 40, 40));
    	pinsChecked[pinNumber] = false;
    }
    
    public void enablePin(int pinNumber)
    {
    	pins.get(pinNumber).setClickable(true);
    	pins.get(pinNumber).setTextColor(Color.rgb(255, 255, 255));
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
