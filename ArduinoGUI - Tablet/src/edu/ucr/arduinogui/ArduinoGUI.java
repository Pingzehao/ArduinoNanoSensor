package edu.ucr.arduinogui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ArduinoGUI extends Activity {
	private static final String LOG_TAG = "ArduinoGUI";
	
	/*TODO Create a thread for sampling the analog read and passing it to the GraphView
	Also add a value for the sampling rate (every x ms) maybe the resolution (number of samples to draw)*/
	//Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    //Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int REPEATED_POLLING = 6;
    
    //Number of Analog Input Pin on the Arduino 
    public static final int ANALOG_PINS = 6;
    public static final int DIGITAL_PINS = 14;
    
    public String ANALOG_MODE;
    public String ANALOG_PULL;
    public String ANALOG_READ;
    public String ANALOG_WRITE;
    public String DIGITAL_MODE;
    public String DIGITAL_PULL;
    public String DIGITAL_READ;
    public String DIGITAL_WRITE;
    
    //Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private String mConnectedDeviceName = null;
    
    //Used to show connected to at the top
    private static TextView mTitle;
    private EditText awValue;
    private EditText prValue;
    private EditText windowMin;
    private EditText windowMax;
    private EditText email;
    private Thread repeatedPollingThread;
    private GraphView graphView;
    private Button sendCommand;
    private Button setWindow;
    private Button sendEmail;
    private ToggleButton dmToggle;
    private ToggleButton dpToggle;
    private ToggleButton dwToggle;
    private RadioButton digitalMode;
    private RadioButton digitalPull;
    private RadioButton digitalRead;
    private RadioButton digitalWrite;
    private RadioButton analogRead;
    private RadioButton analogWrite;
    private RadioGroup commandSelection;
    private CheckBox autoScaling;
    private CheckBox repeatedPolling;
    private CheckBox pin0;
    private CheckBox pin1;
    private CheckBox pin2;
    private CheckBox pin3;
    private CheckBox pin4;
    private CheckBox pin5;
    private CheckBox pin6;
    private CheckBox pin7;
    private CheckBox pin8;
    private CheckBox pin9;
    private CheckBox pin10;
    private CheckBox pin11;
    private CheckBox pin12;
    private CheckBox pin13;
    private boolean[] pinsChecked = new boolean[DIGITAL_PINS];
    private boolean dModeOn = false;
    private boolean dPullOn = false;
    private boolean autoScale = false;;
    private int dwValue = 0;
    private int mProcessedCharCount = 0;
    private int pollingRate = 100;
    private Calendar timeStarted;
    //private String outputCommand = "";
    private String radioSelected;
    PowerManager pm = null; 
	PowerManager.WakeLock wl = null;
    
    //Used to change the menu icon to disconnect when connected
    private MenuItem mMenuItemConnect;
    public static BluetoothAdapter mBluetoothAdapter = null;
	public static BluetoothSerialService mSerialService = null;
	private ByteQueue mByteQueue = null;
	private byte[] mReceiveBuffer;
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, 
        		R.layout.custom_title);
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mSerialService = new BluetoothSerialService(this, mHandlerBT);
		if (mBluetoothAdapter == null) 
		{
            finishDialogNoBluetooth();
			return;
		}
		mReceiveBuffer = new byte[4 * 1024];
		mByteQueue = new ByteQueue(4 * 1024);
		
		//Initialize the graphical interface elements
		graphView = (GraphView) findViewById(R.id.graphView);
		repeatedPolling = (CheckBox) findViewById(R.id.repeatedPolling);
		commandSelection = (RadioGroup) findViewById(R.id.commandSelection);
		digitalMode = (RadioButton) findViewById(R.id.digitalModeButton);
		digitalPull = (RadioButton) findViewById(R.id.digitalPullButton);
		digitalRead = (RadioButton) findViewById(R.id.digitalReadButton);
		digitalWrite = (RadioButton) findViewById(R.id.digitalWriteButton);
		analogRead = (RadioButton) findViewById(R.id.analogReadButton);
		analogWrite = (RadioButton) findViewById(R.id.analogWriteButton);
		sendCommand = (Button) findViewById(R.id.sendCommandButton);
		setWindow = (Button) findViewById(R.id.setWindowButton);
		sendEmail = (Button) findViewById(R.id.sendEmail);
		dwToggle = (ToggleButton) findViewById(R.id.dwToggle);
		dwToggle.setTextOn("1");
		dwToggle.setTextOff("0");
		dwToggle.setChecked(false);
	    dmToggle = (ToggleButton) findViewById(R.id.dmToggle);
	    dmToggle.setTextOn("IN");
	    dmToggle.setTextOff("OUT");
	    dmToggle.setChecked(false);
	    dpToggle = (ToggleButton) findViewById(R.id.dpToggle);
	    dpToggle.setTextOn("HIGH");
	    dpToggle.setTextOff("LOW");
	    dpToggle.setChecked(false);
	    pin0 = (CheckBox) findViewById(R.id.pin0);
		pin1 = (CheckBox) findViewById(R.id.pin1);
		pin2 = (CheckBox) findViewById(R.id.pin2);
		pin3 = (CheckBox) findViewById(R.id.pin3);
		pin4 = (CheckBox) findViewById(R.id.pin4);
		pin5 = (CheckBox) findViewById(R.id.pin5);
		pin6 = (CheckBox) findViewById(R.id.pin6);
		pin7 = (CheckBox) findViewById(R.id.pin7);
		pin8 = (CheckBox) findViewById(R.id.pin8);
		pin9 = (CheckBox) findViewById(R.id.pin9);
		pin10 = (CheckBox) findViewById(R.id.pin10);
		pin11 = (CheckBox) findViewById(R.id.pin11);
		pin12 = (CheckBox) findViewById(R.id.pin12);
		pin13 = (CheckBox) findViewById(R.id.pin13);
		autoScaling = (CheckBox) findViewById(R.id.autoScaling);
		windowMin = (EditText) findViewById(R.id.windowMin);
		windowMax = (EditText) findViewById(R.id.windowMax);
		awValue = (EditText) findViewById(R.id.awValue);
		prValue = (EditText) findViewById(R.id.pollingRate);
		email = (EditText) findViewById(R.id.email);
		
		ANALOG_READ = (String) analogRead.getText();
		ANALOG_WRITE = (String) analogWrite.getText();
		DIGITAL_MODE = (String) digitalMode.getText();
		DIGITAL_PULL = (String) digitalPull.getText();
		DIGITAL_READ = (String) digitalRead.getText();
		DIGITAL_WRITE = (String) digitalWrite.getText();
		
		repeatedPollingThread = new Thread()
		{
		      @Override
		      public void run() 
		      {
		          while(true) 
		          {
					  if(mSerialService.isPolling())
					  {
						  int[] dataToAdd = mSerialService.getData();
						  if(dataToAdd != null && dataToAdd[0] != -1)
						  {
							  graphView.addData(dataToAdd, pollingRate, 
									  timeStarted);
							  dataToAdd[0] = -1;
							  if(autoScale)
							  {
								  graphView.setWindow(graphView.getMin(), 
										  graphView.getMax());
							  }
						  }
						  for(int i = 0; i < ANALOG_PINS; ++i)
						  {
							  if(!pinsChecked[i])
							  {
								  graphView.hidePin(i);
							  }
							  else
							  {
								  graphView.unhidePin(i);
							  }
						  }
					  }
				  }
		      }
		};
		repeatedPollingThread.start();
		
		autoScaling.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(autoScaling.isChecked())
					autoScale = true;
				else
					autoScale = false;
			}
		});
		
		windowMin.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				if(arg2.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				{
					Log.v("ArduinoGUI", "ENTER PRESSED");
					InputMethodManager imm = (InputMethodManager) 
						getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(
							windowMin.getApplicationWindowToken(), 
							InputMethodManager.HIDE_NOT_ALWAYS);
				}
				return false;
			}
		});
		
		windowMax.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				if(arg2.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				{
					InputMethodManager imm = (InputMethodManager) 
						getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(windowMax.getWindowToken(), 0);
				}
				return false;
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
				}
				graphView.setWindow(min, max);
			}
		});
		
		repeatedPolling.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(repeatedPolling.isChecked())
				{
					pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, 
			        		"Stay Awake");
					wl.acquire();
					graphView.reset();
					timeStarted = Calendar.getInstance();
					graphView.setupOutput(timeStarted);
					String output = "R";
					for(int i = 0; i < pinsChecked.length; ++i)
					{
							if(pinsChecked[i])
							{
								output += (char) (i + 97);
							}
					}
					output += ".";
					if(prValue.getText().length() > 2)
						output += prValue.getText();
					else
						output += "100";
					output += ".";
					Log.v(LOG_TAG, output);
					mSerialService.startPolling();
					send(output.getBytes());
					try
					{
						String tempPollRate = "" + prValue.getText();
						pollingRate = Integer.parseInt(tempPollRate);
						graphView.setPollingRate(pollingRate);
					}
					catch(NumberFormatException e)
					{
					}
				}
				else
				{
					wl.release();
					graphView.closeFiles();
					
					String output = ".";
					mSerialService.stopPolling();
					send(output.getBytes());
				}
			}
		});
		
		sendEmail.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					ArrayList<Uri> filePaths = graphView.getFilePaths();
					Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
					sendIntent.setType("message/rfc822");
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, 
							"ArduinoGUI Data");
					String temp = email.getText().toString();
					if(temp.length() > 4)
					{
						String[] emailAddress = {temp};
						sendIntent.putExtra(Intent.EXTRA_EMAIL, emailAddress);
					}
					sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, 
							filePaths);
					startActivity(Intent.createChooser(sendIntent, 
							"Send E-Mail Using..."));
				}
				catch(NullPointerException e)
				{
					Toast.makeText(getApplicationContext(), "No Files to Send" , 
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		
	    dmToggle.setOnClickListener(new OnClickListener()
	    {
	    	public void onClick(View v)
	    	{
	    		if(dmToggle.isChecked())
	    			dModeOn = true;
	    		else
	    			dModeOn = false;
	    	}
	    });
	    dpToggle.setOnClickListener(new OnClickListener()
	    {
	    	public void onClick(View v)
	    	{
	    		if(dpToggle.isChecked())
	    			dPullOn = true;
	    		else
	    			dPullOn = false;
	    	}
	    });
	    dwToggle.setOnClickListener(new OnClickListener()
	    {
	    	public void onClick(View v)
	    	{
	    		if(dwToggle.isChecked())
	    			dwValue = 1;
	    		else
	    			dwValue = 0;
	    	}
	    });
	    pin0.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[0] = true;
				else
					pinsChecked[0] = false;
			}
		});
		pin1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[1] = true;
				else
					pinsChecked[1] = false;
			}
		});
		pin2.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[2] = true;
				else
					pinsChecked[2] = false;
			}
		});
		pin3.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[3] = true;
				else
					pinsChecked[3] = false;
			}
		});
		
		pin4.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[4] = true;
				else
					pinsChecked[4] = false;
			}
		});
		pin5.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[5] = true;
				else
					pinsChecked[5] = false;
			}
		});
		pin6.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[6] = true;
				else
					pinsChecked[6] = false;
			}
		});
		pin7.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[7] = true;
				else
					pinsChecked[7] = false;
			}
		});
		pin8.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[8] = true;
				else
					pinsChecked[8] = false;
			}
		});
		pin9.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[9] = true;
				else
					pinsChecked[9] = false;
			}
		});
		pin10.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[10] = true;
				else
					pinsChecked[10] = false;
			}
		});
		pin11.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[11] = true;
				else
					pinsChecked[11] = false;
			}
		});
		pin12.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
					pinsChecked[12] = true;
				else
					pinsChecked[12] = false;
			}
		});
		pin13.setOnCheckedChangeListener(new OnCheckedChangeListener() {
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
				    if(radioSelected == ANALOG_WRITE)
				    {
				    	pin0.setClickable(false);
				    	pin0.setChecked(false);
				    	pin0.setTextColor(Color.rgb(40, 40, 40));
				    	pin1.setClickable(false);
				    	pin1.setChecked(false);
				    	pin1.setTextColor(Color.rgb(40, 40, 40));
				    	pinsChecked[0] = false;
				    	pin2.setClickable(false);
				    	pin2.setChecked(false);
				    	pin2.setTextColor(Color.rgb(40, 40, 40));
				    	pinsChecked[1] = false;
				    	pin4.setClickable(false);
				    	pin4.setChecked(false);
				    	pin4.setTextColor(Color.rgb(40, 40, 40));
				    	pinsChecked[3] = false;
				    	pin7.setClickable(false);
				    	pin7.setChecked(false);
				    	pin7.setTextColor(Color.rgb(40, 40, 40));
				    	pinsChecked[6] = false;
				    	pin8.setClickable(false);
				    	pin8.setChecked(false);
				    	pin8.setTextColor(Color.rgb(40, 40, 40));
				    	pinsChecked[7] = false;
				    	pin12.setClickable(false);
				    	pin12.setChecked(false);
				    	pin12.setTextColor(Color.rgb(40, 40, 40));
				    	pinsChecked[11] = false;
				    	pin13.setClickable(false);
				    	pin13.setChecked(false);
				    	pin13.setTextColor(Color.rgb(40, 40, 40));
				    	pinsChecked[12] = false;
				    }
				    else
				    {
				    	pin0.setClickable(true);
				    	pin1.setTextColor(Color.rgb(255, 255, 255));
				    	pin1.setClickable(true);
				    	pin1.setTextColor(Color.rgb(255, 255, 255));
				    	pin2.setClickable(true);
				    	pin2.setTextColor(Color.rgb(255, 255, 255));
				    	pin4.setClickable(true);
				    	pin4.setTextColor(Color.rgb(255, 255, 255));
				    	pin7.setClickable(true);
				    	pin7.setTextColor(Color.rgb(255, 255, 255));
				    	pin8.setClickable(true);
				    	pin8.setTextColor(Color.rgb(255, 255, 255));
				    	pin12.setClickable(true);
				    	pin12.setTextColor(Color.rgb(255, 255, 255));
				    	pin13.setClickable(true);
				    	pin13.setTextColor(Color.rgb(255, 255, 255));
				    }
			   }   
		});
		
		sendCommand.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				if(mSerialService.isPolling())
				{
					Toast.makeText(getApplicationContext(), "Cannot Send " +
							"Command While Polling" , 
							Toast.LENGTH_SHORT).show();
					return;
				}
				String outputCommand = "";
				TextView t = (TextView) findViewById(R.id.outputDisplay);
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
			    for(; i < pinsChecked.length; ++i)
			    {
			    	if(pinsChecked[i])
			    		break;
			    }
			    
			    outputCommand += i;
			    outputCommand += ".";
			    
			    if(radioSelected == ANALOG_WRITE)
			    	outputCommand = outputCommand + awValue.getText() + ".";
			    else if(radioSelected == DIGITAL_WRITE)
			    	outputCommand = outputCommand + dwValue + ".";
			    else if(radioSelected == DIGITAL_MODE)
		    		outputCommand = outputCommand + boolToInt(dModeOn) + ".";
			    else if(radioSelected == DIGITAL_PULL)
		    		outputCommand = outputCommand + boolToInt(dPullOn) + ".";
			    
			    Log.d(LOG_TAG, outputCommand);
			    send(outputCommand.getBytes());
			    
			}

			private int boolToInt(boolean aModeOn) {
				if(aModeOn) return 1;
				return 0;
			}	
		});
    }
    
    /*private final Handler mHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		if(msg.what == 1)
    		{
    			update();
    		}
    	}
    };*/
    
    public void update()
    {
    		int bytesAvailable = mByteQueue.getBytesAvailable();
    		int bytestoRead = Math.min(bytesAvailable, mReceiveBuffer.length);
    		try
    		{
    			int bytesRead = mByteQueue.read(mReceiveBuffer, 0, bytestoRead);
    			
    			append(mReceiveBuffer, 0, bytesRead);
    			Log.v(LOG_TAG, new String(mReceiveBuffer));
    		}
    		catch (InterruptedException e)
    		{
    		}
    }
    
    public void append(byte[] buffer, int base, int length)
    {
    	for(int i = 0; i < length; i++)
    	{
    		byte b = buffer[base + i];
    		try
    		{
    			//if(BlueTerm.LOG_CHARACTERS_FLAG)
    			char printableB = (char) b;
    			Log.v(LOG_TAG, Character.toString(printableB));
    			if(b < 32 || b > 126)
    			{
    				printableB = ' ';
    			}
    			Log.v(LOG_TAG, "'" + Character.toString(printableB) + "' (" + 
    					Integer.toString(b) + ")");
    			mProcessedCharCount++;
    		}
    		catch(Exception e)
    		{
    			Log.e(LOG_TAG, "Exception while processing character "
    					+ Integer.toString(mProcessedCharCount) + " code "
    					+ Integer.toString(b), e);
    		}
    	}
    }
    	
    
	public synchronized void onResume(Bundle savedInstanceState) 
	{
		//super.onSuspend(savedInstanceState);
		if ((mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled())) 
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.alert_dialog_turn_on_bt)
            	.setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.alert_dialog_warning_title)
                .setCancelable( false )
                .setPositiveButton(R.string.alert_dialog_yes, 
                		new DialogInterface.OnClickListener() 
                {
	                public void onClick(DialogInterface dialog, int id) 
	                {
	                	Intent enableIntent = new Intent(
	                			BluetoothAdapter.ACTION_REQUEST_ENABLE);
	                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
	                }
                }).setNegativeButton(R.string.alert_dialog_no, 
                		new DialogInterface.OnClickListener() 
                {
                	public void onClick(DialogInterface dialog, int id) 
                	{
                		finishDialogNoBluetooth();            	
	                }
                });
            AlertDialog alert = builder.create();
            alert.show();
		}
		
		if (mSerialService != null) 
		{
			// Only if the state is STATE_NONE, do we know that we haven't 
			//started already
			if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) 
			{
				// Start the Bluetooth chat services
				mSerialService.start();
			}
		}
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        mMenuItemConnect = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        if(item.getItemId() == R.id.connect)
        {
        	if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) 
        	{
        		// Launch the DeviceListActivity to see devices and do scan
        		Intent serverIntent = new Intent(this, 
        				DeviceListActivity.class);
        		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        	}
        	else if (mSerialService.getState() == 
        		BluetoothSerialService.STATE_CONNECTED) 
            {
            	mSerialService.stop();
		    	mSerialService.start();
            }
        	return true;
        }
		return false;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        switch(requestCode) 
        {
        	case REQUEST_CONNECT_DEVICE:
	            // When DeviceListActivity returns with a device to connect
	            if (resultCode == Activity.RESULT_OK) 
	            {
	                // Get the device MAC address
	                String address = data.getExtras().getString(
	                		DeviceListActivity.EXTRA_DEVICE_ADDRESS);
	                // Get the BLuetoothDevice object
	                BluetoothDevice device = 
	                	mBluetoothAdapter.getRemoteDevice(address);
	                // Attempt to connect to the device
	                mSerialService.connect(device);                
	            }
	            break;
        	case REQUEST_ENABLE_BT:
	            // When the request to enable Bluetooth returns
	            if (resultCode == Activity.RESULT_OK) 
	            {
	                Log.d(LOG_TAG, "BT not enabled");             
	            }
	        }
    }
    
    //Shows alert when Bluetooth is disabled
    public void finishDialogNoBluetooth() 
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.app_name)
        .setCancelable( false )
        .setPositiveButton(R.string.alert_dialog_ok, 
        		new DialogInterface.OnClickListener() 
        {
        	public void onClick(DialogInterface dialog, int id) 
        	{
        		finish();            	
        	}
        });
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    //Bluetooth Handler, reports the state and connectivity status using Toast
	private final Handler mHandlerBT = new Handler() 
	{
		@Override
	    public void handleMessage(Message msg) 
		{        	
			switch(msg.what) 
			{
				case MESSAGE_STATE_CHANGE:
					switch(msg.arg1) 
					{
						case BluetoothSerialService.STATE_CONNECTED:
							if(mMenuItemConnect != null) 
							{
								mMenuItemConnect.setIcon(android.R.drawable.
										ic_menu_close_clear_cancel);
								mMenuItemConnect.setTitle(R.string.disconnect);
							}
							mTitle.setText(R.string.title_connected_to);
							mTitle.append(mConnectedDeviceName);
							break;
						case BluetoothSerialService.STATE_CONNECTING:
							mTitle.setText(R.string.title_connecting);
							break;
							
						case BluetoothSerialService.STATE_LISTEN:
						case BluetoothSerialService.STATE_NONE:
	                	if(mMenuItemConnect != null) 
	                	{
	                		mMenuItemConnect.setIcon(android.R.drawable.
	                				ic_menu_search);
	                		mMenuItemConnect.setTitle(R.string.connect);
	                	}
	                	
	                	mTitle.setText(R.string.title_not_connected);
	                    break;
					}
					break;
	            /*case MESSAGE_WRITE:
	            	if (mLocalEcho) {
	            		byte[] writeBuf = (byte[]) msg.obj;
	            		mEmulatorView.write(writeBuf, msg.arg1);
	            	}
	                
	                break;
	                */
	            case MESSAGE_READ:
	                byte[] readBuf = (byte[]) msg.obj;
	                TextView t = (TextView) findViewById(R.id.outputDisplay);
	                String s = new String(readBuf);
	                Log.v(LOG_TAG, s);
                	//t.setText(s);
	                break;
	                
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(getApplicationContext(), "Connected to "
							+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	                break;
	            case MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), 
	                		msg.getData().getString(TOAST),
	                		Toast.LENGTH_SHORT).show();
	                break;
	            case REPEATED_POLLING:
	            	break;
			}
		}
	};
	
	public void send(byte[] out) {
    	mSerialService.write( out );
    }
}