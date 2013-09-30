/**
 * This class handles the drawing of the nano sensor data. Data is added to an array by
 * the ClientPanel when the ServerThread receives new data. 
 * 
 * The SensorGraphPanel then calculates the resistance of the sensor based on the bit 
 * voltage and the initial divider values set by the ClientPanel. It then graphs the 
 * resistance values vs time
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

public class SensorGraphPanel extends JPanel
{
	private static final long serialVersionUID = 7726426928800051175L;

	private static final int CHIP_PINS = 4;

	/** Input voltage to the voltage divider setup on the device */
	private static final double INPUT_VOLTAGE = 3.3;
	
	/** Max resistance to draw on the graph */
//	private static final double MAX_RESISTANCE = 100;
	
	/** Max ppm to draw on th graph */
	private static final double MAX_PPM = 100;
	
	private long mPollingRate = 100; 
	
	/** Controls which pins are to be hidden when drawing the chip sensor graph*/
	private boolean[] mHiddenPins = new boolean[CHIP_PINS];

	/** Initial resistance of the chip to get dR/R */
	private double[] mInitialResistances = new double[CHIP_PINS];
	
	
	/** Digital potentiometer resistance values in kOhms */
	private double[] mDividerResistances = new double[CHIP_PINS];
	
	/** ArrayList to hold all the data points of sensor resistances in kOhms*/
	private ArrayList<ArrayList<Double>> mResistances = new ArrayList<ArrayList<Double>>();
	
	private ArrayList<ArrayList<Double>> mPPM = new ArrayList<ArrayList<Double>>();
	
	/** SensorGraphPanel is used to draw the data read from the nano sensor */
	public SensorGraphPanel()
	{
		for(int i = 0; i < CHIP_PINS; ++i)
		{
			mHiddenPins[i] = false;
			mInitialResistances[i] = -1;
			mDividerResistances[i] = -1;
			mResistances.add(new ArrayList<Double>());
			mPPM.add(new ArrayList<Double>());
		}
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
//		String maxString = df.format(MAX_RESISTANCE) + "kOhms";
//		String minString = df.format(0) + "kOhms";
		g.drawString(maxString, 5, 15);
		g.drawString(minString, 5, getHeight() - 20);
		
		/** Draws the x-axis labels */
		String minTime = df.format(0) + "min";
		double minutes = mPollingRate * (mPPM.get(3).size() - 1) / 1000.0 / 60.0;
//		double minutes = mPollingRate * (mResistances.get(3).size() - 1) / 1000.0 / 60.0; 
		String maxTime = df.format(minutes) + "min"; 
		g.drawString(minTime, 10, getHeight() - 5);
		g.drawString(maxTime, getWidth() - 50, getHeight() - 5);
	}
	
	/**
	 * Sets the color to draw with based on which pin of the sensor is being drawn
	 * 
	 * @param g - Graphics component used by the panel
	 * @param n - Pin number
	 */
	private void setColor(Graphics g, int n)
	{
		switch(n)
		{
			case 0:
				g.setColor(Color.RED);
				break;
			case 1:
				g.setColor(Color.GREEN);
				break;
			case 2:
				g.setColor(Color.BLUE);
				break;
			case 3:
				g.setColor(Color.BLACK);
				break;
		}
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
		
		for(int i = 0; i < CHIP_PINS; ++i)
		{
			if(!mHiddenPins[i])
			{
				int lastIndex = mPPM.get(i).size() - 1;
				
				if(lastIndex < 1)
				{
					return;
				}
				
				setColor(g, i);
				for(int j = 0; j < lastIndex - 1; ++j)
				{
					startX = 5 + (width / lastIndex) * j;
					stopX = 5 + (width / lastIndex) * (j + 1);
					startY = (height + 5) - (mPPM.get(i).get(j) / MAX_PPM) * height;
					stopY = (height + 5) - (mPPM.get(i).get(j + 1) / MAX_PPM) * height;
					g.drawLine((int) startX, (int) startY, (int) stopX, (int) stopY);
				}
				
				setColor(g, i);
				DecimalFormat df = new DecimalFormat("#.##");
				String ppmString = df.format(mPPM.get(i).get(lastIndex)) + "ppm";
				g.drawString(ppmString, (int) (startX * 0.85), (int) startY);
			}	
		}
		
		/**
		for(int i = 0; i < CHIP_PINS; ++i)
		{
			if(!mHiddenPins[i])
			{
				int lastIndex = mResistances.get(i).size() - 1;
				
				if(lastIndex < 1)
				{
					return;
				}
				
				setColor(g, i);
				for(int j = 0; j < lastIndex - 1; ++j)
				{
					startX = 5 + (width / lastIndex) * j;
					stopX = 5 + (width / lastIndex) * (j + 1);
					startY = (height + 5) - (mResistances.get(i).get(j) / MAX_RESISTANCE) * height;
					stopY = (height + 5) - (mResistances.get(i).get(j + 1) / MAX_RESISTANCE) * height;
					g.drawLine((int) startX, (int) startY, (int) stopX, (int) stopY);
				}
				
				setColor(g, i);
				DecimalFormat df = new DecimalFormat("#.##");
				String resistanceString = df.format(mResistances.get(i).get(lastIndex)) + "kOhms";
				g.drawString(resistanceString, (int) (startX * 0.85), (int) startY);
			}	
		}*/
		
		//TODO: dR/R graph
	}
	
	/** 
	 * Sets the divider resistances used for calculating the resistance of the sensor 
	 * 
	 * @param dividerResistances - Bit resistance of the divider
	 */
	public void setDividerResistances(int[] dividerResistances)
	{
		for(int i = 0; i < CHIP_PINS; ++i)
		{
			mDividerResistances[i] = (double) dividerResistances[i] * 100.0 / 255.0;
		}
	}
	
	/**
	 * Ges the array of hidden pins
	 * 
	 * @return - Boolean array indicating which pins are hidden
	 */
	public boolean[] getHiddenPins()
	{
		return mHiddenPins;
	}
	
	/**
	 * Sets the pins to be hidden
	 * 
	 * @param hiddenPins - Boolean array indicating which pins are hidden
	 */
	public void setHiddenPins(boolean[] hiddenPins)
	{
		mHiddenPins = hiddenPins; 
	}
	
	/**
	 * Calculates the resistance of the sensor based on the passed in bit voltage and the
	 * divider resistance
	 * 
	 * @param bitVoltage - Bit voltage reading from the device
	 * @param pin - Pin number on the sensor
	 * @return - Resistance of the sensor on the passed in pin
	 */
	public double calculateResistance(int bitVoltage, int pin)
	{
		double outputVoltage = (double) bitVoltage / 1023.0 * INPUT_VOLTAGE;
		double chipResistance = outputVoltage * mDividerResistances[pin] / (INPUT_VOLTAGE - outputVoltage);
		return chipResistance;
	}
	
	public double calculatePPM(int bitVoltage, int pin)
	{
		double chipResistance = calculateResistance(bitVoltage, pin);
		double percentDelta = (chipResistance - mInitialResistances[pin]) / mInitialResistances[pin];
		double ppm =  137960 * Math.pow(percentDelta, 4) - 8296.1 * Math.pow(percentDelta, 3)
				- 343.51 * Math.pow(percentDelta, 2) + 41.623 * percentDelta - 0.0692;
				//1.76 * Math.exp(1.7525 * percentDelta);
		if(ppm < 0)
		{
			ppm = 0;
		}
		return ppm;
	}
	
	/**
	 * Calculates and adds the data to the ArrayList of resistances
	 * 
	 * @param bitVoltages - Bit voltage reading from the device
	 */
	public void addData(int[] bitVoltages)
	{
		for(int i = 0; i < CHIP_PINS; ++i)
		{
			if(mInitialResistances[i] == -1)
			{
				mInitialResistances[i] = calculateResistance(bitVoltages[i], i);
			}
			else
			{
				mPPM.get(i).add(calculatePPM(bitVoltages[i], i));
			}
//			mResistances.get(i).add(calculateResistance(bitVoltages[i], i));
		}
	}
	
	/**
	 * 
	 * @param pollingRate - Rate the data samples are taken at in ms
	 */
	public void setPollingRate(long pollingRate)
	{
		mPollingRate = pollingRate;
	}
	
	public void clearData()
	{
		mPollingRate = 100;
		for(int i = 0; i < CHIP_PINS; ++i)
		{
			mPPM.get(i).clear();
			mResistances.clear();
			mInitialResistances[i] = -1;
			mDividerResistances[i] = 0;
		}
	}
}
