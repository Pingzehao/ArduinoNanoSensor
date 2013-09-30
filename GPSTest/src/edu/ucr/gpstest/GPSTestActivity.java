package edu.ucr.gpstest;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class GPSTestActivity extends Activity {
    /** Called when the activity is first created. */
	
	private TextView coordinates;
	private LocationManager locManager;
	private LocationListener locListener;
	private double longitude;
	private double latitude;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        coordinates = (TextView) findViewById(R.id.Coordinates);
        
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        locListener = new LocationListener()
        {

			@Override
			public void onLocationChanged(Location location)
			{
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				coordinates.setText("Longitude: " + longitude + "\nLatitude: " + latitude);
			}

			@Override
			public void onProviderDisabled(String provider)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderEnabled(String provider)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras)
			{
				// TODO Auto-generated method stub
				
			}
        	
        };
        
        locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListener);
        
        coordinates.setOnClickListener(new OnClickListener()
        {

			@Override
			public void onClick(View v)
			{
				coordinates.setText("Longitude: " + longitude + "\nLatitude: " + latitude);
				// TODO Auto-generated method stub
				
			}
        	
        });
        
        

    }
}