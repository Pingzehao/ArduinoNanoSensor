package chat;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class TFrmSplash extends Canvas {
    private Chat c;
    private Image iSplash;
    public TFrmSplash(final Chat c) {
        this.c = c;
        try {
            iSplash = Image.createImage("/images/splash.gif");
        } catch (Exception e) {
            
        }
        setFullScreenMode(true);
    }
    
    public void show() {
        c.display.setCurrent(this);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        c.frmMenu.show();
    }
    
    public void paint(Graphics g) {
        g.setColor(0, 0, 0);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.drawImage(iSplash, (getWidth()-iSplash.getWidth())/2, (getHeight()-iSplash.getHeight())/2, 0);
    }
    
}
