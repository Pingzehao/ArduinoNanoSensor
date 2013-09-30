package edu.ucr.arduinogui;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;
import edu.ucr.arduinoguiv1.R;

public class GraphActivity extends Activity
{
	// TODO Make it not break when changing orientation, onCreate/onDestroy
	private static final String LOG_TAG = "ArduinoGUI";

	// Intent request codes
	static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Message types sent from the BluetoothReadService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int REPEATED_POLLING = 6;

	public static final int CHIP = 5;
	public static final int HUMIDITY = 6;
	public static final int TEMPERATURE = 7;

	// Number of Analog and Digital Pins on the Arduino
	public static final int ANALOG_PINS = 8;
	public static final int DIGITAL_PINS = 14;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	private String mConnectedDeviceName = null;
	private int mProcessedCharCount = 0;

	// Thread processes the data received from the Bluetooth thread
	private Thread repeatedPollingThread;

	// Graph that the data is actually drawn to
	private static GraphView graphView;

	// Calendar with time started for the text file naming
	private static Calendar timeStarted;

	// Boolean array that keeps track of which pins are checked, used to choose
	// which pins to poll and which pins to show after polling has started
	private static boolean[] pinsChecked = new boolean[ANALOG_PINS];

	// Autoscale fits all of the data from min to max in the window
	public static boolean autoScale = false;

	// Used to change the menu icon to disconnect when connected
	public static MenuItem mMenuItemConnect;
	public static BluetoothAdapter mBluetoothAdapter = null;
	public static BluetoothSerialService mSerialService = null;
	private ByteQueue mByteQueue = null;
	private byte[] mReceiveBuffer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graph_tab);

		TabContainer.getmTitle().setText(R.string.title_not_connected);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mSerialService = new BluetoothSerialService(this, mHandlerBT);
		if(mBluetoothAdapter == null)
		{
			finishDialogNoBluetooth();
			return;
		}
		else if(!mBluetoothAdapter.isEnabled())
		{
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
		}
		mReceiveBuffer = new byte[4 * 1024];
		mByteQueue = new ByteQueue(4 * 1024);

		// Initialize the graphical interface elements
		graphView = (GraphView) findViewById(R.id.graphView);
		// if(savedInstanceState != null && !savedInstanceState.isEmpty())
		// {
		// graphView = (GraphView) savedInstanceState.getSerializable("graphView");
		// pinsChecked = savedInstanceState.getBooleanArray("pinsChecked");
		// Log.d(LOG_TAG, "graphView gotten from savedInstanceState");
		// }

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
						if(dataToAdd != null && dataToAdd[0] != -1 && dataToAdd[7] != -1)
						{
							graphView.addData(dataToAdd, timeStarted);
							// Resets all the values to -1 so the same value doens't get added twice
							for(int i = 0; i < ANALOG_PINS; ++i)
								dataToAdd[i] = -1;
							mSerialService.clearBuffer();
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
		};
		repeatedPollingThread.start();
	}

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
		catch(InterruptedException e)
		{
		}
	}

	public void append(byte[] buffer, int base, int length)
	{
		for(int i = 0; i < length; ++i)
		{
			byte b = buffer[base + i];
			try
			{
				char printableB = (char) b;
				Log.v(LOG_TAG, Character.toString(printableB));
				if(b < 32 || b > 126)
				{
					printableB = ' ';
				}
				Log.v(LOG_TAG, "'" + Character.toString(printableB) + "' (" + Integer.toString(b) + ")");
				mProcessedCharCount++;
			}
			catch(Exception e)
			{
				Log.e(LOG_TAG, "Exception while processing character " + Integer.toString(mProcessedCharCount) + " code " + Integer.toString(b), e);
			}
		}
	}

	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.graph_tab);
	}

	public void onDestroy(Bundle b)
	{
		b.putSerializable("graphView", graphView);
		b.putBooleanArray("pinsChecked", pinsChecked);
	}

	public boolean onTouchEvent(MotionEvent event)
	{
		if(event.getAction() == MotionEvent.ACTION_UP)
		{
			switch(graphView.getDisplayMode())
			{
				case CHIP:
					graphView.setDisplayMode(HUMIDITY);
					Toast.makeText(getApplicationContext(), "Humidity Sensor Data", Toast.LENGTH_SHORT).show();
					break;
				case HUMIDITY:
					graphView.setDisplayMode(TEMPERATURE);
					Toast.makeText(getApplicationContext(), "Temperature Sensor Data", Toast.LENGTH_SHORT).show();
					break;
				case TEMPERATURE:
					graphView.setDisplayMode(CHIP);
					Toast.makeText(getApplicationContext(), "Chip Sensor Data", Toast.LENGTH_SHORT).show();
					break;
			}
			return true;
		}
		return true;
	}

	public synchronized void onResume(Bundle savedInstanceState)
	{
		/*
		 * //Bluetooth is off if ((mBluetoothAdapter != null) && (!mBluetoothAdapter.isEnabled())) { AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 * builder.setMessage(R.string.alert_dialog_turn_on_bt) .setIcon(android.R.drawable.ic_dialog_alert) .setTitle(R.string.alert_dialog_warning_title) .setCancelable( false )
		 * .setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { Intent enableIntent = new Intent(
		 * BluetoothAdapter.ACTION_REQUEST_ENABLE); startActivityForResult(enableIntent, REQUEST_ENABLE_BT); } }).setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
		 * public void onClick(DialogInterface dialog, int id) { finishDialogNoBluetooth(); } }); AlertDialog alert = builder.create(); alert.show(); }
		 * 
		 * if (mSerialService != null) { // Only if the state is STATE_NONE, do we know that we haven't //started already if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) { // Start
		 * the Bluetooth chat services mSerialService.start(); } }
		 */
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
			if(mSerialService.getState() == BluetoothSerialService.STATE_NONE)
			{
				// Launch the DeviceListActivity to see devices and do scan
				if(!mBluetoothAdapter.isEnabled())
				{
					startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
				}

				if(mBluetoothAdapter.isEnabled())
				{
					Intent serverIntent = new Intent(this, DeviceListActivity.class);
					startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				}
			}
			else if(mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED)
			{
				if(mSerialService.isPolling())
				{
					byte[] temp = { (byte) '.' };
					send(temp);
					OptionsActivity.stopPolling();
				}
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
				if(resultCode == Activity.RESULT_OK)
				{
					// Get the device MAC address
					String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					// Get the BLuetoothDevice object
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
					// Attempt to connect to the device
					mSerialService.connect(device);
				}
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if(resultCode == Activity.RESULT_OK)
				{
					Log.d(LOG_TAG, "BT not enabled");
				}
				else if(resultCode == Activity.RESULT_CANCELED)
				{
					Toast.makeText(getApplicationContext(), "Bluetooth required for device connection", Toast.LENGTH_LONG).show();
				}
		}
	}

	// Shows alert when Bluetooth is disabled
	public void finishDialogNoBluetooth()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.alert_dialog_no_bt).setIcon(android.R.drawable.ic_dialog_info).setTitle(R.string.app_name).setCancelable(false).setPositiveButton(R.string.alert_dialog_ok,
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

	// Bluetooth Handler, reports the state and connectivity status using Toast
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
								mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
								mMenuItemConnect.setTitle(R.string.disconnect);
							}
							TabContainer.getmTitle().setText(R.string.title_connected_to);
							TabContainer.getmTitle().append(mConnectedDeviceName);
							break;
						case BluetoothSerialService.STATE_CONNECTING:
							TabContainer.getmTitle().setText(R.string.title_connecting);
							break;

						case BluetoothSerialService.STATE_LISTEN:
						case BluetoothSerialService.STATE_NONE:
							if(mMenuItemConnect != null)
							{
								mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
								mMenuItemConnect.setTitle(R.string.connect);
							}

							TabContainer.getmTitle().setText(R.string.title_not_connected);
							break;
					}
					break;
				/*
				 * case MESSAGE_WRITE: if (mLocalEcho) { byte[] writeBuf = (byte[]) msg.obj; mEmulatorView.write(writeBuf, msg.arg1); }
				 * 
				 * break;
				 */
				case MESSAGE_READ:
					byte[] readBuf = (byte[]) msg.obj;
					// TextView t = (TextView) findViewById(R.id.outputDisplay);
					String s = new String(readBuf);
					Log.v(LOG_TAG, s);
					// t.setText(s);
					break;

				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
					break;
				case REPEATED_POLLING:
					break;
			}
		}
	};

	public static void send(byte[] out)
	{
		mSerialService.write(out);
	}

	public static void startPolling(boolean[] pinsChecked, String output, int pollingRate)
	{
		// TODO Sometimes crashing, mSerialSerivce is null
		mSerialService.stopPolling();
		mSerialService.clearBuffer();
		graphView.reset();
		timeStarted = Calendar.getInstance();
		graphView.createFiles(timeStarted);
		graphView.setPollingRate(pollingRate);
		send(output.getBytes());
		mSerialService.startPolling();
	}

	public static void stopPolling()
	{
		// wl.release();
		String output = ".";
		send(output.getBytes());
		mSerialService.stopPolling();
		graphView.closeFiles();
	}

	public static void setWindow(int min, int max)
	{
		graphView.setWindow(min, max);
	}

	public static void setScaling(boolean status)
	{
		autoScale = status;
	}

	public static void updatePinsChecked(boolean[] pins)
	{
		pinsChecked = pins;
	}

	public static ArrayList<Uri> getFilePaths()
	{
		return graphView.getFilePaths();
	}

	// Prevents backbutton from killing the app
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public static void setGraphView(GraphView graphView)
	{
		GraphActivity.graphView = graphView;
	}

	public static GraphView getGraphView()
	{
		return graphView;
	}
}
