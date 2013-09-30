/**
 * This program acts as a multi-threaded server for sensor clients, connecting each one 
 * on a different port, and displays the data received.
 * 
 * It evenly spaces the clients in a GridLayout and a single sensor's view can be 
 * expanded by clicking on it, and it switches back to the multi-client view 
 * when clicked on again. 
 * 
 * Individual sensors of the nano sensor can also be viewed by right clicking on the 
 * client panel and unchecking the sensor. The colors are as follows for the sensors
 * Red - 1, Orange - 2, Yellow - 3, Green - 4
 * 
 * @author Albert Chen
 */

package edu.ucr.sensorservergui;

import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

public class SensorServerGUI extends WindowAdapter
{
	/** Starting port for the server */
	public static final int START_PORT = 8080;
	
	/** Number of chip pins on the physical sensor devices */
	protected static final int CHIP_PINS = 4;
	
	/** Screen width and height in pixels */
	public static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
	public static final int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;

	
	/** Number of panels currently attached/displayed */
	private static int mNumPanels = 0;
	
	/** Whether or not it is focused on one panel */
	private static boolean mFocused = false;
	
	/** JFrame for applet */
	private static JFrame mFrame = new JFrame("SensorServerGUI");
	private static JFrame mMapFrame = new JFrame("Map");
	
	private static MapRequest mMapRequest = new MapRequest();
	private static JPanel mMapPanel = new JPanel();
	private static JLabel mMapLabel = new JLabel();
	private static long mLastMapUpdate = System.currentTimeMillis();
	
	/** JLabel initially displayed until a connection is made */
	private static JLabel mConnectionLabel = new JLabel("Awaiting connections...", JLabel.CENTER);
	
	/** JPopupMenu for selecting which sensors to hide/draw */
	private static JPopupMenu mMenu = new JPopupMenu("Show/Hide Pins");
	
	/** ArrayList that contains the JPopupMenu selections */
	private static ArrayList<JCheckBoxMenuItem> mMenuItems = new ArrayList<JCheckBoxMenuItem>();
	
	/** ArrayList that contains all of the ClientPanels */
	private static ArrayList<ClientPanel> mClientPanels = new ArrayList<ClientPanel>();

	/** ClientPanel that allows the JCheckboxMenuItems know which one was clicked on */
	private static ClientPanel mActivePanel = null;
	
	/** 
	 * Initially called when the program starts, opens a new window and adds a label
	 * letting the user know it's waiting for connections 
	 */
	private static void initializeGUI()
	{
		/** JCheckBoxMenuItems for the right-click menu */
		JCheckBoxMenuItem menuItem1 = new JCheckBoxMenuItem("Sensor 1");
		menuItem1.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				setPin(0, mMenuItems.get(0).isSelected());
			}
		});
		JCheckBoxMenuItem menuItem2 = new JCheckBoxMenuItem("Sensor 2");
		menuItem2.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent event)
			{
				setPin(1, mMenuItems.get(1).isSelected());
			}
		});
		JCheckBoxMenuItem menuItem3 = new JCheckBoxMenuItem("Sensor 3");
		menuItem3.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent event)
			{
				setPin(2, mMenuItems.get(2).isSelected());
			}
		});
		JCheckBoxMenuItem menuItem4 = new JCheckBoxMenuItem("Sensor 4");
		menuItem4.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent event)
			{
				setPin(3, mMenuItems.get(3).isSelected());
			}
		});
		
		/** Adds menu items to an ArrayList so they can be accessed in a for loop */
		mMenuItems.add(menuItem1);
		mMenuItems.add(menuItem2);
		mMenuItems.add(menuItem3);
		mMenuItems.add(menuItem4);
		
		/** Adds the JCheckBoxMenuItems to the JMenu */
		mMenu.add(menuItem1);
		mMenu.add(menuItem2);
		mMenu.add(menuItem3);
		mMenu.add(menuItem4);
		
		mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mFrame.setLayout(new GridLayout(1, 1));
		
		mMapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mMapFrame.setLayout(new GridLayout(1, 1));
		mMapFrame.setVisible(true);
		
		/** Initializes the JFrame and launches it */
		mFrame.getContentPane().add(mConnectionLabel);
		mFrame.pack();
		mFrame.setVisible(true);
		mFrame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
		
		mMapPanel.add(mMapLabel);
		mMapFrame.getContentPane().add(mMapPanel);
		mMapFrame.pack();
		mMapFrame.setVisible(true);
		mMapFrame.setSize(500, 500);
		
		/** Adds the mouse listener to the JFrame to detect mouse inputs */
		mFrame.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseClicked(MouseEvent event){}
			@Override
			public void mouseEntered(MouseEvent event){}
			@Override
			public void mouseExited(MouseEvent event){}
			/**
			 *  Right-click menu handled in both mousePressed and mouseReleased since the
			 *  way PopupTrigger varies depending on the system.
			 */
			@Override
			public void mousePressed(MouseEvent event)
			{
				/** Checks if right-click was pressed and there are actually Panels being
				 * displayed 
				 */
				if(event.isPopupTrigger() && mNumPanels > 0)
				{
					boolean[] hiddenPins = new boolean[CHIP_PINS];
					/**
					 *  Finds the panel clicked on, gets the hidden pins from it, and
					 * sets the JCheckBoxMenuItems accordingly
					 */
					for(int i = 0; i < mClientPanels.size(); ++i)
					{
						ClientPanel tempPanel = mClientPanels.get(i);
						if(tempPanel.contains(event.getX(), event.getY()) && tempPanel.isAttached())
						{
							hiddenPins = tempPanel.getHiddenPins();
							mActivePanel = tempPanel;
							break;
						}
					}
					for(int i = 0; i < mMenuItems.size(); ++i)
					{
						mMenuItems.get(i).setSelected(!hiddenPins[i]);
					}
					mMenu.show(mFrame, event.getX(), event.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent event)
			{
				/** Same as above */
				if(event.isPopupTrigger() && mNumPanels > 0)
				{
					boolean[] hiddenPins = new boolean[CHIP_PINS];
					for(int i = 0; i < mClientPanels.size(); ++i)
					{
						ClientPanel tempPanel = mClientPanels.get(i);
						if(tempPanel.contains(event.getX(), event.getY()) && tempPanel.isAttached())
						{
							hiddenPins = tempPanel.getHiddenPins();
							mActivePanel = tempPanel;
							break;
						}
					}
					for(int i = 0; i < mMenuItems.size(); ++i)
					{
						mMenuItems.get(i).setSelected(!hiddenPins[i]);
					}
					mMenu.show(event.getComponent(), event.getX(), event.getY());
				}
				/** When it's not a right-click zoom in on the panel clicked on */
				else
				{
					/** Make sure there is more than one panel otherwise zooming does nothing */ 
					if(mNumPanels > 1 || mFocused)
					{
						/** Get the location of the click */
						int clickX = event.getX();
						int clickY = event.getY();
						
						/** Checks each panel to see which one was clicked on */
						for(int i = 0; i < mClientPanels.size(); ++i)
						{
							ClientPanel tempPanel = mClientPanels.get(i);
							if(tempPanel.contains(clickX, clickY))
							{
								setFocus(tempPanel);
								break;
							}
						}
					}
				}
			}
		});
	}
	
	/**
	 * Sets the pin for the sensor on the SensorGraphPanel to be hidden.
	 * 
	 * @param x
	 * @param y
	 * @param pin
	 * @param setHidden
	 */
	public static void setPin(int pin, boolean setHidden)
	{
		mActivePanel.getHiddenPins()[pin] = !setHidden;
		mActivePanel.updateUI();
		mActivePanel = null;
	}

	/**
	 * Adds a ClientPanel onto the main grid and readjusts it
	 * @param panel - ClientPanel to be added to the grid
	 */
	public synchronized static void addPanel(ClientPanel panel)
	{
		mNumPanels++;
		mFrame.getContentPane().add(panel);
		panel.setAttached(true);
		readjustGrid(mNumPanels);
	}
	
	/**
	 * Removes a ClientPanel from the main grid and readjusts it
	 * @param panel - ClientPanel to be removed from the grid
	 */
	public synchronized static void removePanel(ClientPanel panel)
	{
		mFrame.remove(panel);
		panel.setAttached(false);
		mNumPanels--;
		readjustGrid(mNumPanels);
	}
	
	
	/**
	 * Removes all other panels except the passed in and resizes the grid accordingly.
	 * If it was already focused it adds back all the ClientPanels that are connected
	 *   
	 * @param panel - ClientPanel to focus on
	 */
	public synchronized static void setFocus(ClientPanel panel)
	{
		if(!mFocused)
		{
			/** Remove all other ClientPanels */
			for(int i = 0; i < mClientPanels.size(); ++i)
			{
				ClientPanel tempPanel = mClientPanels.get(i);
				if(panel != tempPanel && tempPanel.isAttached())
				{
					removePanel(tempPanel);
				}
			}
		}
		else if(mFocused)
		{
			removePanel(panel);
			/** Add back all connected ClientPanels */
			for(int i = 0; i < mClientPanels.size(); ++i)
			{
				ClientPanel tempPanel = mClientPanels.get(i);
				if(tempPanel.isConnected() && !tempPanel.isAttached())
				{
					addPanel(tempPanel);
				}
			}
		}
		mFocused = !mFocused;
	}

	/**
	 * Readjusts the grid to best fit the passed in number of panels
	 * 
	 * @param numPanels - number of panels to fit in the grid
	 */
	public synchronized static void readjustGrid(int numPanels)
	{
		/** Calculate the number of rows and columns for the grid attempting to make it
		 * a square, if possible
		 */
		int rows = (int) Math.sqrt(numPanels);
		if(rows == 0)
		{
			rows = 1;
		}
		int cols = numPanels / rows;
		if(cols == 0)
		{
			cols = 1;
		}
		
		/** Get the current width and height of the window in pixels */
		int frameWidth = mFrame.getWidth();
		int frameHeight = mFrame.getHeight();
		
		/** Resize the grid and fit the contents */
		mFrame.setLayout(new GridLayout(rows, cols));
		mFrame.pack();
		
		/** Sets the window size to the same as the old one */ 
		mFrame.setSize(frameWidth, frameHeight);
	}

	/**
	 * Adds a new ClientPanel on the next port up
	 */
	public static void addClient()
	{
		ClientPanel tempPanel = new ClientPanel(START_PORT + mClientPanels.size());
		mClientPanels.add(tempPanel);
	}

	/**
	 * Checks if to make sure there is at least one non-connected ClientPanel and adds
	 * one if there isn't
	 */
	public static void manageConnections()
	{
		int connectionCount = 0;
		for(int i = 0; i < mClientPanels.size(); ++i)
		{
			ClientPanel tempPanel = mClientPanels.get(i);
			if(tempPanel.isConnected())
			{
				connectionCount++;
			}
		}
		if(connectionCount == mClientPanels.size())
		{
			addClient();
		}
	}

	/**
	 * Adds ClientPanels to the frame as they become connected and removes them when
	 * they're disconnected
	 */
	public synchronized static void manageClientPanels()
	{
		for(int i = 0; i < mClientPanels.size(); ++i)
		{
			ClientPanel tempPanel = mClientPanels.get(i);
			
			/** Attach the ClientPanel Object once it's connected */
			if(tempPanel.isConnected() && !tempPanel.isAttached() && !mFocused)
			{
				addPanel(tempPanel);
			}
			/** Detach the ClientPanel Object if it's disconnected */
			else if(!tempPanel.isConnected() && tempPanel.isAttached())
			{
				removePanel(tempPanel);
				tempPanel.clearData();
			}
		}
		
		/** Counts the number of connected panels */
		int connectionCount = 0;
		for(int i = 0; i < mClientPanels.size(); ++i)
		{
			ClientPanel tempPanel = mClientPanels.get(i);
			if(tempPanel.isConnected())
			{
				connectionCount++;
			}
		}
		
		/** Removes the "Awaiting connections..." once there's a connected client */
		if(connectionCount > 0)
		{
			mFrame.remove(mConnectionLabel);
		}
		else if(connectionCount == 0)
		{
			mFrame.getContentPane().add(mConnectionLabel);
		}
		
	}
	
	public static void updateMap()
	{
		System.out.println("MAP UPDATED");
		ArrayList<Coordinate> markerCoordinates = new ArrayList<Coordinate>();
		for(int i = 0; i < mClientPanels.size(); ++i)
		{
			ClientPanel tempPanel = mClientPanels.get(i);
			Coordinate tempCoordinate = tempPanel.getCoordinates();
			double latitude = tempCoordinate.getLatitude();
			double longitude = tempCoordinate.getLongitude();
			if(latitude != 0 && longitude != 0)
			{
				System.out.println(latitude + ",," + longitude);
				markerCoordinates.add(mClientPanels.get(i).getCoordinates());
			}
		}
		if(!markerCoordinates.isEmpty())
		{
			mMapRequest.addMarkers(markerCoordinates);
			mMapLabel = new JLabel(new ImageIcon(mMapRequest.getMapImage()));
			mMapPanel.removeAll();
			mMapPanel.add(mMapLabel);
			mMapFrame.pack();
			mMapFrame.setSize(500, 500);
		}
		mLastMapUpdate = System.currentTimeMillis();
	}

	public static void main(String[] args)
	{
		initializeGUI();
		Thread updateGUIThread = new Thread(new Runnable()
		{
			public void run()
			{
				while(true)
				{
					manageConnections();
					manageClientPanels();
					
					if(System.currentTimeMillis() - mLastMapUpdate >= 1000)
					{
						updateMap();
					}
				}
			}
		});
		updateGUIThread.start();
	}
}