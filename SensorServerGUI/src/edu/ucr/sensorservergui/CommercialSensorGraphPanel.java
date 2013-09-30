/**
 * This class handles the drawing of the commercial H2S sensor data. Data is added to an
 * array by the ClientPanel when the ServerThread receives new data.
 * 
 * The CommercialSensorGraphPanel then calculates the ppm based on the bit voltage 
 * readings and the initial resistance of the H2S sensor. It then graphs the ppm vs time.
 * 
 * @author Albert Chen 
 */

package edu.ucr.sensorservergui; 

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JPanel;

public class CommercialSensorGraphPanel extends JPanel
{
	private static final long serialVersionUID = 4353078509413513657L;

	private static final double MAX_PPM = 100.0;

	/** Reference voltage on the device for the ADC */
	private static final double REFERENCE_VOLTAGE = 3.3;

	/** Divider resistance on the sensor in kOhms*/
	private static final double MQ2_DIVIDER_RESISTANCE = 10.0;

	/** Input voltage for the MQ2 sensor */
	private static final double MQ2_VOLTAGE = 7.4;

	/** ArrayList used to store the data for the sensor, in ppm */
	private ArrayList<Double> mPPM = new ArrayList<Double>();

	/** Polling rate that each data point is taken at */ 
	private long mPollingRate = 100;
	
	/** Initial resistance of the MQ2 sensor */
	private double mMQ2InitialResistance = -1;

	/**
	 * CommercialSensorGraphPanel is used to draw the sensor data from the commercial
	 * H2S sensor.
	 */
	public CommercialSensorGraphPanel()
	{
		setBackground(Color.WHITE);
	}

	/**
	 * Called when updateUI() or repaint() is called
	 * 
	 * @param g - Graphics component used by the Panel
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		g.clearRect(0, 0, getWidth(), getHeight());
		drawData(g);
		drawAxis(g);
	}
	
	/**
	 * Draws the axis for the graph
	 * 
	 * @param g - Graphics component used by the Panel
	 */
	private void drawAxis(Graphics g)
	{
		/** Draws the axis lines */
		g.setColor(Color.BLACK);
		g.drawLine(5, 5, 5, getHeight());
		g.drawLine(0, getHeight() - 15, getWidth() - 5, getHeight() - 15);
		
		/** Draws the y-axis labels */
		DecimalFormat df = new DecimalFormat("#.##");
		String maxString = df.format(MAX_PPM) + "ppm";
		String minString = df.format(0) + "ppm";
		g.drawString(maxString, 5, 15);
		g.drawString(minString, 5, getHeight() - 20);
		
		/** Draws the x-axis labels */
		String minTime = df.format(0) + "min";
		double minutes = mPollingRate * (mPPM.size() - 1) / 1000.0 / 60.0; 
		String maxTime = df.format(minutes) + "min"; 
		g.drawString(minTime, 10, getHeight() - 5);
		g.drawString(maxTime, getWidth() - 50, getHeight() - 5);
	}
	
	/**
	 * Draws the data for the graph
	 * 
	 * @param g - Graphics object used by the Panel
	 */
	private void drawData(Graphics g2)
	{
		Graphics2D g = (Graphics2D) g2;
		g.setStroke(new BasicStroke(1.5f));
		
		double height = getHeight() - 20;
		double width = getWidth() - 5;
		double startX = 0;
		double startY = 0;
		double stopY = 0;
		double stopX = 0;
		
		int lastIndex = mPPM.size() - 1;
		
		if(lastIndex < 1)
		{
			return;
		}
		
		for(int i = 0; i < lastIndex - 1; ++i)
		{
			startX = 5 + (width / lastIndex) * i;
			stopX = 5 + (width / lastIndex) * (i + 1);
			startY = (height + 5) - (mPPM.get(i) / MAX_PPM) * height;
			stopY = (height + 5) - (mPPM.get(i + 1) / MAX_PPM) * height;
			g.drawLine((int) startX, (int) startY, (int) stopX, (int) stopY);
		}
		
		DecimalFormat df = new DecimalFormat("#.##");
		String ppmString = df.format(mPPM.get(mPPM.size() - 1)) + "ppm";
		g.drawString(ppmString, (int) (startX * 0.85), (int) startY);
	}
	
	/**
	 * Calculates and adds the data into an ArrayList
	 * 
	 * @param bitVoltage - bit voltage reading from the device
	 */
	public void addData(int bitVoltage)
	{
		double ppm = calculateMQ2Ppm(bitVoltage);
		mPPM.add(ppm);
	}
	
	/**
	 * Calculates the PPM from the H2S sensor based on the bit voltage reading passed in
	 * and the inital resistance of the sensor. It then fits the dR/R to a ppm value based
	 * on the equation below.
	 * 
	 * @param bitVoltage - bit voltage reading from the device 
	 * @return - the calculated ppm based on the voltage reading and initial resistance
	 */
	public double calculateMQ2Ppm(int bitVoltage)
	{
		double outputVoltage = (double) bitVoltage / 1023 * REFERENCE_VOLTAGE * 2;
		double MQ2Resistance = (MQ2_VOLTAGE / outputVoltage - 1) * MQ2_DIVIDER_RESISTANCE;
		if(mMQ2InitialResistance == -1)
		{
			mMQ2InitialResistance = MQ2Resistance / 4.5;
		}
		double deltaResistance = MQ2Resistance / mMQ2InitialResistance;
		double ppm = -63.99 * Math.log(deltaResistance) + 60.802; 
		
		if(ppm < 0)
			ppm = 0;
		
		return ppm;
	}

	/**
	 * Sets the polling rate value used to calculate time
	 *  
	 * @param pollingRate - rate the data samples are taken at in ms
	 */
	public void setPollingRate(long pollingRate)
	{
		mPollingRate = pollingRate;
	}
	
	public void clearData()
	{
		mMQ2InitialResistance = -1;
		mPPM.clear();
	}
}
