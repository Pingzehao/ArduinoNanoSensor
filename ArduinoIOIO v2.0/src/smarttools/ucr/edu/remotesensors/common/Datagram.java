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
	private int d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19;

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
						int d3, int d4, int d5, int d6, int d7, int d8, int d9, int d10,
						int d11, int d12, int d13, int d14, int d15, int d16, int d17, 
						int d18, int d19, double latitude, double longitude){

		this.id = id;
		this.d1 = d1;
		this.d2 = d2;
		this.d3 = d3;
		this.d4 = d4;
		this.d5 = d5;
		this.d6 = d6;
		this.d7 = d7;
		this.d8 = d8;
		this.d9 = d9;
		this.d10 = d10;
		this.d11 = d11;
		this.d12 = d12;
		this.d13 = d13;
		this.d14 = d14;
		this.d15 = d15;
		this.d16 = d16;
		this.d17 = d17;
		this.d18 = d18;
		this.d19 = d19;
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
	public int getD8(){ return this.d8; }
	public int getD9(){ return this.d9; }
	public int getD10(){ return this.d10; }
	public int getD11(){ return this.d11; }
	public int getD12(){ return this.d12; }
	public int getD13(){ return this.d13; }
	public int getD14(){ return this.d14; }
	public int getD15(){ return this.d15; }
	public int getD16(){ return this.d16; }
	public int getD17(){ return this.d17; }
	public int getD18(){ return this.d18; }
	public int getD19(){ return this.d19; }
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
		message += d8 + ",";
		message += d9 + ",";
		message += d10 + ",";
		message += d11 + ",";
		message += d12 + ",";
		message += d13 + ",";
		message += d14 + ",";
		message += d15 + ",";
		message += d16 + ",";
		message += d17 + ",";
		message += d18 + ",";
		message += d19 + ",";
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
			int d8 = Integer.parseInt(split[8].trim());
			int d9 = Integer.parseInt(split[9].trim());
			int d10 = Integer.parseInt(split[10].trim());
			int d11 = Integer.parseInt(split[11].trim());
			int d12 = Integer.parseInt(split[12].trim());
			int d13 = Integer.parseInt(split[13].trim());
			int d14 = Integer.parseInt(split[14].trim());
			int d15 = Integer.parseInt(split[15].trim());
			int d16 = Integer.parseInt(split[16].trim());
			int d17 = Integer.parseInt(split[17].trim());
			int d18 = Integer.parseInt(split[18].trim());
			int d19 = Integer.parseInt(split[19].trim());
			double latitude = Double.parseDouble(split[20].trim());
			double longitude = Double.parseDouble(split[21].trim());
			
			d = new Datagram(id, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, latitude, longitude);
			
		} catch(Exception e){
			throw new IllegalArgumentException();
		}
		
		return d;
	}
}
