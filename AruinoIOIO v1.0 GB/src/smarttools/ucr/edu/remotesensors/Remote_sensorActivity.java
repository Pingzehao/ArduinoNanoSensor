/** 
 * This is a simple example showing how ClientConnect may be used. This activity creates a single
 * connection to the server and then generates a random Datagram and sends it every second. 
 * 
 * It is worth noting that a permission was manually added to this projects manifest file to
 * allow this activity to have access to the network.
 * 
 * @author jhero
 *
 */

package smarttools.ucr.edu.remotesensors;

import java.util.Random;

import smarttools.ucr.edu.remotesensors.common.Datagram;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;


public class Remote_sensorActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TextView tv = new TextView(this);
		tv.setText("Remote Sensor Client");
		setContentView(tv);

		/** Create a client connection to the server */
		while(true){
			ClientConnect ct = new ClientConnect("127.0.0.1", 8080);

			boolean connected = true;

			Random  r = new Random(System.currentTimeMillis());
			while(connected){
				/** Create a pay load with dummy id and location and random data payload */
				Datagram d = new Datagram("unique_id", r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt(), r.nextDouble(), r.nextDouble());

				/** Send the data in this thread */
				int err = ct.send(d);
				if(err == -1) connected = false;

				/** Wait some seconds to avoid DoS */
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}