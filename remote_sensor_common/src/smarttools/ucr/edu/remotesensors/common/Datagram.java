/**
 * Simple class used to hold data passed from clients to the server.
 * Contains a parameter for each data element that will be transmitted.
 * The createMessage() function is used to create a simple comma delimited string
 * of all values in the datagram that will be sent to the server.
 *  
 * @author jhero
 *
 */

package smarttools.ucr.edu.remotesensors.common;

public class Datagram {

	/** Unique id of the client */
	private String id; 
	
	/** Data elements */
	private int d1, d2, d3, d4, d5, d6, d7;

	/** Longitude and latitude */
	private double mLatitude, mLongitude;
	
	/**
	 * Creates a new datagram object with all necessary data elements
	 * @param id - the unique id of the client
	 * @param d1 - some data value
	 * @param mLatitude - latitude of the device
	 * @param mLongitude - longitude of the device;
	 */
	public Datagram(String id, int d1, int d2, 
						int d3, int d4, int d5, int d6, int d7, 
						double latitude, double longitude){

		this.id = id;
		this.d1 = d1;
		this.d2 = d2;
		this.d3 = d3;
		this.d4 = d4;
		this.d5 = d5;
		this.d6 = d6;
		this.d7 = d7;
		mLatitude = latitude;
		mLongitude = longitude;
	}
	
	public String getId(){ return this.id; }
	public int getD1(){ return this.d1; }
	public int getD2(){ return this.d2; }
	public int getD3(){ return this.d3; }
	public int getD4(){ return this.d4; }
	public int getD5(){ return this.d5; }
	public int getD6(){ return this.d6; }
	public int getD7(){ return this.d7; }
	public double getLatitude(){ return mLatitude; }
	public double getLongitude(){ return mLongitude; }
	/**
	 * Returns a string message to be transmitted via socket to the server.
	 * The message is formatted as a comma delimited string of all required values.
	 */
	public String createMessage(){
		String message = "";
		
		message += id + ",";
		message += d1 + ",";
		message += d2 + ",";
		message += d3 + ",";
		message += d4 + ",";
		message += d5 + ",";
		message += d6 + ",";
		message += d7 + ",";
		message += mLatitude + ",";
		message += mLongitude;
		return message;
	}
	
	/**
	 * Creates a Datagram object from a string created with this class' createMessage() function.
	 * @param line
	 * @return
	 */
	public static Datagram processLine(String line){
		Datagram d;
		
		String[] split = line.split(",");
		
		try{
			String id = split[0].trim();
			int d1 = Integer.parseInt(split[1].trim());
			int d2 = Integer.parseInt(split[2].trim());
			int d3 = Integer.parseInt(split[3].trim());
			int d4 = Integer.parseInt(split[4].trim());
			int d5 = Integer.parseInt(split[5].trim());
			int d6 = Integer.parseInt(split[6].trim());
			int d7 = Integer.parseInt(split[7].trim());
			double latitude = Double.parseDouble(split[8].trim());
			double longitude = Double.parseDouble(split[9].trim());
			
			d = new Datagram(id, d1, d2, d3, d4, d5, d6, d7, latitude, longitude);
			
		} catch(Exception e){
			throw new IllegalArgumentException();
		}
		
		return d;
	}
}
