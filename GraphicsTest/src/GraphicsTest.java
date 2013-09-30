import java.applet.Applet;
import java.awt.Graphics;


public class GraphicsTest extends Applet 
{
	public void init()
	{
		resize(1280, 800);
	}
	
	public void paint(Graphics g)
	{
		g.drawString("Derp", 0, 25);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		
	}

}
