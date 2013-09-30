package edu.ucr.arduinoioio;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import edu.ucr.arduinoioiov2.R;

public class EmailActivity extends Activity {
	//TODO Save the file names to an array in the preferences so that you can choose
	//which one to mail. Will need a try/catch for the thing if they deleted it with a file
	//browser, and if it doesn't exist, delete it.
	//TODO Have it take a screenshot of the graph and send it too
	
	private static Button sendEmail;
    private static EditText email;
    
    public void onCreate(Bundle savedInstanceState)
	    {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.email_tab);
	        
	        sendEmail = (Button) findViewById(R.id.sendEmailButton);
	        email = (EditText) findViewById(R.id.email);
	        
	        sendEmail.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					try
					{
						ArrayList<Uri> filePaths = GraphActivity.getGraphView().getFilePaths();
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
