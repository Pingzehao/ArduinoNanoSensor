package edu.ucr.arduinogui;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TextView;

public class TabContainer extends TabActivity
{
	private static TextView mTitle;
	private static PowerManager pm = null; 
	private static PowerManager.WakeLock wl = null;
	
	public void onCreate(Bundle savedInstanceState)
	{
	   	super.onCreate(savedInstanceState);
	   	requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	   	setContentView(R.layout.main);
	       
	   	//Keeps screen from dimming/going to sleep
	   	pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, 
        		"Stay Awake");
		wl.acquire();
	   	
	   	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, 
	   			R.layout.custom_title);
	   	setmTitle((TextView) findViewById(R.id.title_left_text));
	   	getmTitle().setText(R.string.app_name);
	   	setmTitle((TextView) findViewById(R.id.title_right_text));
	        
	   	Resources res = getResources(); // Resource object to get Drawables
	   	TabHost tabHost = getTabHost();  // The activity TabHost
	   	TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	   	Intent intent;  // Reusable Intent for each tab
	        
	   	// Create an Intent to launch an Activity for the tab (to be reused)
	   	intent = new Intent().setClass(this, GraphActivity.class);
	   	// Initialize a TabSpec for each tab and add it to the TabHost
	   	spec = tabHost.newTabSpec("Graph").setIndicator("",
	   			res.getDrawable(R.drawable.ic_menu_graph))
	   			.setContent(intent);
	   	tabHost.addTab(spec);
	        
	    intent = new Intent().setClass(this, OptionsActivity.class);
	    spec = tabHost.newTabSpec("Polling Options").setIndicator("",
	    		res.getDrawable(R.drawable.ic_menu_preferences))
	        	.setContent(intent);
	    tabHost.addTab(spec);
	        
	    intent = new Intent().setClass(this, EmailActivity.class);
	    spec = tabHost.newTabSpec("Email Data").setIndicator("",
	    		res.getDrawable(R.drawable.ic_menu_send_email))
	        	.setContent(intent);
	    tabHost.addTab(spec);	    

	    tabHost.setCurrentTab(0);
	}

	 public static void setmTitle(TextView mTitle) 
	 {
		 TabContainer.mTitle = mTitle;
	 }

	 public static TextView getmTitle() 
	 {
		return mTitle;
	 }
	
     //Prevents backbutton from killing the app
	 public boolean onKeyDown(int keyCode, KeyEvent event) 
	 {
		 if(keyCode == KeyEvent.KEYCODE_BACK) 
	     {
	    	 return true;
	     }
		 if(keyCode == KeyEvent.KEYCODE_HOME)
		 {
			 wl.release();
			 Log.v("ArduinoGUI", "Wake Lock released");
			 return super.onKeyDown(keyCode, event);
		 }
	     return super.onKeyDown(keyCode, event);    
	 }
}
