/**
 * This class handles the layout of a single connected client. It also handles the 
 * connection of a single client. The ServerThread receives data and parses it and the 
 * ClientPanel calculates and updates the JLabels. The graph data is added to the 
 * SensorGraphPanel for the nano sensor and the CommercialSensorGraphPanel for the 
 * commercial H2S sensor.
 * 
 * The class extends a JPanel using a BorderLayout. Within the BorderLayout, there are 
 * two more JPanels, one for the north and one for the center. The west, south, and east
 * are left empty and filled by the center. 
 * 
 * The north JPanel uses a 4x2 GridLayout and contains all the textual data such as
 * client IP, client port, date, time, latitude, longitude, temperature, and humidity.
 * 
 * The center JPanel uses a 1x2 GridLayout and contains a SensorGraphPanel and a
 * CommercialSensorGraphPanel which graphs the data receieved from the client. 
 * 
 * @author Albert Chen 
 */

package edu.ucr.sensorservergui16;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.PipedReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import smarttools.ucr.edu.remotesensors.common.Datagram;
import smarttools.ucr.edu.remotesensors.server.ServerThread;

public class ClientPanel extends JPanel
{
	private static final long serialVersionUID = -4601724424875398184L;
	private static final int CHIP_PINS = 16;
	private static final int TEMP_OFFSET = 55;
	private static final double INPUT_VOLTAGE = 3.3;
	
	private boolean mInitialized;
	
	/** Variable keeping track if this ClientPanel is attached to the JFrame */
	private boolean mAttached;
	
	/** JLabels used to display text data */
	private JLabel mClientIPLabel;
	private JLabel mClientPortLabel;
	private JLabel mDateLabel;
	private JLabel mTimeLabel;
	private JLabel mLatitudeLabel;
	private JLabel mLongitudeLabel;
	private JLabel mTemperatureLabel;
	private JLabel mHumidityLabel;
	
	/** JPanel used to contain the text data */
	private JPanel mNorthPanel;
	
	/** JPanel used to contain the two graphs */
	private JPanel mCenterPanel;
	
	/** Panel used to display the graph for nano sensor data */ 
	private SensorGraphPanel mSensorGraphPanel;
	
	/** Panel used to display the graph for commercial sensor data */
	private CommercialSensorGraphPanel mCommercialSensorGraphPanel; 
	
	/** ServerThread handles client connnections and receives the data */
	private ServerThread mServerThread;

	/** Pipe connected to the ServerThread object in order to receive data from it */
	private PipedReader mPipeIn;
	
	/** Thread that controls the ServerThread runnable */
	private Thread mConnectionThread;

	/**
	 * ClientPanel contains the necessary objects to connect a client device, to 
	 * read and to display all the data read from the device.
	 * 
	 * @param port - port to listen for the client on
	 */
	public ClientPanel(int port)
	{
		/** Initialize the main JPanel that will contain everything else */
		super(new BorderLayout());
		
		/** Borders used for the elements, bevelBorder for grouping, lineBorder for dividing */
		Border bevelBorder = BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.DARK_GRAY, Color.BLACK);
		Border lineBorder = BorderFactory.createLineBorder(Color.BLACK);
		setBorder(bevelBorder);
		
		/** Initialize the text data labels */
		Font labelFont = new Font("Arial", Font.PLAIN, 24);
		mClientIPLabel = new JLabel("IP: ");
		mClientPortLabel = new JLabel("Port: " + port);
		mDateLabel = new JLabel("Date: ");
		mTimeLabel = new JLabel("Time: ");
		mLatitudeLabel = new JLabel("Latitude: 0");
		mLongitudeLabel = new JLabel("Longitude: 0");
		mHumidityLabel = new JLabel("Humidity (RH%): ");
		mTemperatureLabel = new JLabel("Temp (C): ");
		
		mClientIPLabel.setFont(labelFont);
		mClientPortLabel.setFont(labelFont);
		mDateLabel.setFont(labelFont);
		mTimeLabel.setFont(labelFont);
		mLatitudeLabel.setFont(labelFont);
		mLongitudeLabel.setFont(labelFont);
		mHumidityLabel.setFont(labelFont);
		mTemperatureLabel.setFont(labelFont);
		
		mClientIPLabel.setBorder(lineBorder);
		mClientPortLabel.setBorder(lineBorder);
		mDateLabel.setBorder(lineBorder);
		mTimeLabel.setBorder(lineBorder);
		mLatitudeLabel.setBorder(lineBorder);
		mLongitudeLabel.setBorder(lineBorder);
		mHumidityLabel.setBorder(lineBorder);
		mTemperatureLabel.setBorder(lineBorder);
		
		/** Initialize the JPanel to contain the text data labels */
		mNorthPanel = new JPanel(new GridLayout(4, 2));
		mNorthPanel.setBackground(Color.LIGHT_GRAY);
		mNorthPanel.setBorder(bevelBorder);
		
		/** Add the labels to the JPanel */
		mNorthPanel.add(mClientIPLabel);
		mNorthPanel.add(mClientPortLabel);
		mNorthPanel.add(mDateLabel);
		mNorthPanel.add(mTimeLabel);
		mNorthPanel.add(mLatitudeLabel);
		mNorthPanel.add(mLongitudeLabel);
		mNorthPanel.add(mTemperatureLabel);
		mNorthPanel.add(mHumidityLabel);
		
		/** Add the JPanel containing the JLabels to the NORTH side of the main JPanel */
		add(mNorthPanel, BorderLayout.NORTH);
	
		/** Initialize the nano sensor and commercial sensor graph panels */
		mSensorGraphPanel = new SensorGraphPanel();
		mCommercialSensorGraphPanel = new CommercialSensorGraphPanel();
		mSensorGraphPanel.setBorder(bevelBorder);
		mCommercialSensorGraphPanel.setBorder(bevelBorder);
		
		/** Initialize the JPanel to contain both the graph panels */
		mCenterPanel = new JPanel(new GridLayout(1, 2));
		mCenterPanel.setBorder(bevelBorder);
		mCenterPanel.setBackground(Color.WHITE);
		mCenterPanel.add(mSensorGraphPanel);
		mCenterPanel.add(mCommercialSensorGraphPanel);
		
		/** Add the JPanel containing the graph panels to the main JPanel */
		add(mCenterPanel, BorderLayout.CENTER);
		
		/** Initialize the ServerThread, and start it on it's own Thread */
		mServerThread = new ServerThread(port, 5000);
		mConnectionThread = new Thread(mServerThread);
		mConnectionThread.start();
		
		/** Connects the other end of the pipe to the ServerThread object handling the transmissions */
		try
		{
			mPipeIn = new PipedReader(mServerThread.getPipedWriter());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		/** This thread reads the data from the ServerThread object and adds the parsed data */
		Thread graphThread = new Thread(new Runnable()
		{
			public void run()
			{
				while(true)
				{
					try
					{
						char[] buf = new char[16384];
						mPipeIn.read(buf);
						System.err.println("Buffer: " + new String(buf));
						String stringBuf = new String(buf);
						String bufferArray[] = stringBuf.split("\n");
						
						/** Sometimes multiple transmissions get clumped together so this splits them back up */
						for(int i = 0; i < bufferArray.length - 1; ++i)
						{
							String line = bufferArray[i];
							String[] split = line.split(",");
							while(split.length > 22)
							{
								int repeatIndex = line.indexOf("client", split[0].length());
								if(repeatIndex == -1)
								{
									repeatIndex = line.indexOf("init", split[0].length());
									line = line.substring(0, repeatIndex);
									addData(line);
									break;
								}
								addData(line.substring(0, repeatIndex));
								line = line.substring(0, repeatIndex);
								split = line.split(",");
							}
							if(line != null);
								addData(line);
							System.err.println("Pipe received data: " + line);
						}
						}
					catch(IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		});
		graphThread.start();
		
		mAttached = false;
		mInitialized = false;
	}
	
	/**
	 * Processes the raw textual data using the Datagram object and then calculates
	 * the actual values based on the raw voltages and updates the data display accordingly 
	 * 
	 * @param data - Serialized String of the Datagram
	 */
	public void addData(String data)
	{
//		System.out.println(data);
		Datagram d = Datagram.processLine(data);
		
		if(d == null)
		{
			return;
		}
		
		/** Gets the resistance values for the digital potentiometer and the polling rate 
		 * from the first transmission */
		if(d.getId().equals("init"))
		{
			mInitialized = true;
			int[] dividerResistances = new int[CHIP_PINS];
			dividerResistances[0] = d.getD1();
			dividerResistances[1] = d.getD2();
			dividerResistances[2] = d.getD3();
			dividerResistances[3] = d.getD4();
			dividerResistances[4] = d.getD5();
			dividerResistances[5] = d.getD6();
			dividerResistances[6] = d.getD7();
			dividerResistances[7] = d.getD8();
			dividerResistances[8] = d.getD9();
			dividerResistances[9] = d.getD10();
			dividerResistances[10] = d.getD11();
			dividerResistances[11] = d.getD12();
			dividerResistances[12] = d.getD13();
			dividerResistances[13] = d.getD14();
			dividerResistances[14] = d.getD15();
			dividerResistances[15] = d.getD16();
			long pollingRate = (long) d.getD17();
			mSensorGraphPanel.setPollingRate(pollingRate);
			mCommercialSensorGraphPanel.setPollingRate(pollingRate);
			mSensorGraphPanel.setDividerResistances(dividerResistances);
			String clientIPString = "IP: " + mServerThread.getIP().substring(1);
			mClientIPLabel.setText(clientIPString);
		}
		/** Calculates and updates the sensor readings accordingly */
		else if(mInitialized)
		{
			int[] nanoSensorData = new int[CHIP_PINS];
			nanoSensorData[0] = d.getD1();
			nanoSensorData[1] = d.getD2();
			nanoSensorData[2] = d.getD3();
			nanoSensorData[3] = d.getD4();
			nanoSensorData[4] = d.getD5();
			nanoSensorData[5] = d.getD6();
			nanoSensorData[6] = d.getD7();
			nanoSensorData[7] = d.getD8();
			nanoSensorData[8] = d.getD9();
			nanoSensorData[9] = d.getD10();
			nanoSensorData[10] = d.getD11();
			nanoSensorData[11] = d.getD12();
			nanoSensorData[12] = d.getD13();
			nanoSensorData[13] = d.getD14();
			nanoSensorData[14] = d.getD15();
			nanoSensorData[15] = d.getD16();
			addChipData(nanoSensorData);
			addMQ2Data(d.getD17());
			double tempCelcius = addTempData(d.getD19());
			addHumidityData(d.getD18(), tempCelcius);
			addGPSData(d.getLatitude(), d.getLongitude());
			addDate();
		}
	}

	/**
	 * Calculates and adds the commercial H2S sensor data and updates the display
	 * 
	 * @param bitVoltage - Bit voltage reading from the device
	 */
	public void addMQ2Data(int bitVoltage)
	{
		mCommercialSensorGraphPanel.addData(bitVoltage);
		mCommercialSensorGraphPanel.updateUI();
	}

	/**
	 * Gets the current date and updates the corresponding JLabels 
	 */
	public void addDate()
	{
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy,HH:mm:ss");
		String dateTimeString = df.format(date);
		String dateString = "Date: " + dateTimeString.substring(0, dateTimeString.indexOf(','));
		String timeString = "Time: " + dateTimeString.substring(dateTimeString.indexOf(',') + 1);
		mDateLabel.setText(dateString);
		mTimeLabel.setText(timeString);
	}

	/**
	 * Adds the nano sensor data and updates the display
	 * 
	 * @param nanoSensorData - Bit voltage readings from the device
	 */
	public void addChipData(int[] nanoSensorData)
	{
		mSensorGraphPanel.addData(nanoSensorData);
		mSensorGraphPanel.updateUI();
	}

	/**
	 * Calculates the temperature based on the bitVoltage
	 * 
	 * @param bitVoltage - Bit voltage reading from the device
	 * @return - Temperature based on the bit voltage in celcius
	 */
	public double calculateTemperature(int bitVoltage)
	{
		double outputVoltage = (double) bitVoltage / 1023 * INPUT_VOLTAGE;
		double tempCelcius = outputVoltage * 100 - TEMP_OFFSET;
		return tempCelcius;
	}

	/**
	 * Calculates the temperature based on the bit voltage readings from the device and 
	 * updates the JLabel correspondingly
	 * 
	 * @param bitVoltage - Bit voltage reading from the device
	 * @return - Temperature based on the bit voltage in celcius
	 */
	public double addTempData(int bitVoltage)
	{
		double tempCelcius = calculateTemperature(bitVoltage);
		DecimalFormat df = new DecimalFormat("#.##");
		String temperatureString = "Temp (C): " + df.format(tempCelcius) + "*C"; 
		mTemperatureLabel.setText(temperatureString);
		return tempCelcius;
	}

	/**
	 * Calculates the humidity based on the bit voltage read from the device and the
	 * calculated temperature readings
	 * 
	 * @param bitVoltage - Bit voltage reading from the device
	 * @param tempCelcius - Current temperature readings in celcius from the device
	 * @return - Relative humidity percentage based on the temperature and bit voltage
	 */
	public double calculateHumidity(int bitVoltage, double tempCelcius)
	{
		double outputVoltage = (double) bitVoltage / 1023 * INPUT_VOLTAGE;
		double humidityPercentage = (outputVoltage / INPUT_VOLTAGE - 0.16) / 0.0062;
		double correctedPercentage = humidityPercentage / (1.0546 - 0.00216 * tempCelcius);
		
		if(correctedPercentage > 100)
		{
			correctedPercentage = 100;
		}
		else if(correctedPercentage < 0)
		{
			correctedPercentage = 0;
		}
		
		return correctedPercentage;
	}

	/**
	 * Calculates the humidity based on the bit voltage readings and temperature read from
	 * the device and updates the JLabel correspondingly
	 * 
	 * @param bitVoltage - Bit voltage reading from the device
	 * @param tempCelcius - Temperature in celcius for relative humidity
	 */
	public void addHumidityData(int bitVoltage, double tempCelcius)
	{
		double relativeHumidity = calculateHumidity(bitVoltage, tempCelcius);
		DecimalFormat df = new DecimalFormat("#.##");
		String humidityString = "Humidity (RH%): " + df.format(relativeHumidity) + "%"; 
		mHumidityLabel.setText(humidityString);
	}

	/**
	 * Updates the JLabels latitude and longitude read from the device
	 * 
	 * @param latitude
	 * @param longitude
	 */
	public void addGPSData(double latitude, double longitude)
	{
		DecimalFormat df = new DecimalFormat("#.#####");
		String latitudeString = "Latitude: " + df.format(latitude);
		String longitudeString = "Longitude: " + df.format(longitude);
		mLatitudeLabel.setText(latitudeString);
		mLongitudeLabel.setText(longitudeString);
	}
	
	/**
	 * Checks whether or not a passed in coordinate is within the bounds of the panel
	 * 
	 * @param x - X coordinate of the click
	 * @param y - Y coordinate of the click
	 * @return
	 */
	public boolean contains(int clickX, int clickY)
	{
		int leftBound = getX();
		int lowerBound = getY();
		int rightBound = leftBound + getWidth();
		int upperBound = lowerBound + getHeight();
		if(clickX > leftBound && clickX < rightBound)
		{
			if(clickY > lowerBound && clickY < upperBound)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @return - Boolean array indicating which sensors should be hidden
	 */
	public boolean[] getHiddenPins()
	{
		return mSensorGraphPanel.getHiddenPins();
	}
	
	/**
	 * 
	 * @return - Connection status of the client
	 */
	public boolean isConnected()
	{
		return mServerThread.isConnected();
	}
	
	/** 
	 * 
	 * @return - Attachment status of the ClientPanel */
	public boolean isAttached()
	{
		return mAttached;
	}
	
	/** 
	 * Sets the attachment status of the ClientPanel 
	 * 
	 * @param attached
	 */
	public void setAttached(boolean attached)
	{
		mAttached = attached;
	}

	/**
	 * Sets the hidden pins for the SensorGraphPanel
	 * 
	 * @param hiddenPins - Boolean array representing the hidden pins
	 */
	public void setHiddenPins(boolean[] hiddenPins)
	{
		mSensorGraphPanel.setHiddenPins(hiddenPins);
	}
	
	public void clearData()
	{
		mClientIPLabel.setText("IP: ");
		mDateLabel.setText("Date: ");
		mTimeLabel.setText("Time: ");
		mLongitudeLabel.setText("Longitude: 0");
		mLatitudeLabel.setText("Latitude: 0");
		mTemperatureLabel.setText("Temp (C): ");
		mHumidityLabel.setText("Humidity (RH%): ");
		
		mSensorGraphPanel.clearData();
		mCommercialSensorGraphPanel.clearData();
		mInitialized = false;
	}
	
	public Coordinate getCoordinates()
	{
		String latitudeLabel = mLatitudeLabel.getText();
		latitudeLabel = latitudeLabel.substring(latitudeLabel.indexOf(" "));
		String longitudeLabel = mLongitudeLabel.getText();
		longitudeLabel = longitudeLabel.substring(longitudeLabel.indexOf(" "));
		double latitude = Double.parseDouble(latitudeLabel);
		double longitude = Double.parseDouble(longitudeLabel);
		return new Coordinate(latitude, longitude);
	}
}