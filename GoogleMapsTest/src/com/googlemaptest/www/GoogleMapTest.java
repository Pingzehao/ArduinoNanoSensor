package com.googlemaptest.www;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.event.MouseInputAdapter;

import org.jdesktop.swingx.JXMapKit;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.jdesktop.swingx.mapviewer.WaypointPainter;
import org.jdesktop.swingx.mapviewer.WaypointRenderer;

public class GoogleMapTest
{
	public static JXMapKit jxMapKit;

	public static void main(String args[]) throws Exception
	{

		// Get us a map provider to get Google maps.


		// From here, set things up to get the main map viewer using this provider as a source.

		TileFactoryInfo tileProviderInfo = map.getTileProviderInfo();
		TileFactory tileFactory = new DefaultTileFactory(tileProviderInfo);
		jxMapKit = new JXMapKit();

		// Create a frame for the application and set up the window appropriately.

		JFrame frame = new JFrame("Google Maps Test");
		frame.getContentPane().add(jxMapKit);
		frame.setSize(800, 600);
		frame.setVisible(true);

		// Set up the map viewer, and give a location and zoom level for something familiar.

		jxMapKit.setTileFactory(tileFactory);
		jxMapKit.setCenterPosition(new GeoPosition(43.005, -81.275));
		jxMapKit.setZoom(3);

		// Add a sample waypoint.

		WaypointPainter waypointPainter = new WaypointPainter();
		waypointPainter.setRenderer(new WaypointRenderer()
		{
			public boolean paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint wp)
			{
				g.setColor(Color.RED);
				g.fillRect(-5, -5, 10, 10);
				g.setColor(Color.BLACK);
				g.drawRect(-5, -5, 10, 10);
				return true;
			}
		});

		waypointPainter.getWaypoints().add(new Waypoint(43.005, -81.275));
		jxMapKit.getMainMap().setOverlayPainter(waypointPainter);

		// Add a mouse listener to check for the waypoint being clicked.

		jxMapKit.getMainMap().addMouseListener(new MouseInputAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{

				// Get the screen point of mouse click.
				Point pt = e.getPoint();

				// Get the pixel coordinates of the waypoint in question from the map.

				JXMapViewer map = jxMapKit.getMainMap();
				Point2D point = map.getTileFactory().geoToPixel(new GeoPosition(43.005, -81.275), map.getZoom());

				// Adjust the pixel coordinates to their relative position on screen.

				Rectangle bounds = map.getViewportBounds();
				int x = (int) (point.getX() - bounds.getX());
				int y = (int) (point.getY() - bounds.getY());

				// Create a bounding rectangle around the waypoint, and see if the mouse click occured
				// within its boundaries.

				Rectangle rect = new Rectangle(x - 5, y - 5, 10, 10);
				if(rect.contains(pt))
				{
					JFrame newframe = new JFrame("Waypoint");
					JLabel newlabel = new JLabel("Waypoint at (43.005, -81.275)");
					newframe.getContentPane().add(newlabel);
					newframe.pack();
					newframe.setVisible(true);
				}

			}
		});

		// Add listener to kill the things if the window dies.

		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});

		// Wait around for a while here.

		while(frame.isVisible())
		{
			Thread.sleep(200);
		}
	}
}
