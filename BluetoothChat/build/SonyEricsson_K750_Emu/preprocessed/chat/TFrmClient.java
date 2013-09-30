package chat;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.StringItem;

public class TFrmClient extends Form  implements CommandListener {
    private Chat c;
    private Gauge gauge;
    private boolean batal;
    private Timer timer;
    private TimerTask animate;
    private boolean left;
    private int index;    
    public TFrmClient(final Chat c) {
        super("Client Chat");
        this.c = c;        
        timer = new Timer();
        gauge = new Gauge("Menunggu server", false, 10, 0);
        append(gauge);
        animate = new TimerTask() {
            public void run() {
                if (!batal && isShown()) {                    
                    if (getLeft()) {
                        setIndex(getIndex()-1);                        
                        if (getIndex() == 0) {
                            setLeft(false);
                        }
                    } else {
                        setIndex(getIndex()+1);                        
                        if (getIndex() == gauge.getMaxValue()) {
                            setLeft(true);
                        }
                    }
                    gauge.setValue(getIndex());
                }
            }
        };
        timer.schedule(animate, 0, 200);
        addCommand(new Command("Batal", Command.BACK, 1));
        setCommandListener(this);
    }
    private void setLeft(boolean l) {
        left = l;
    }
    private boolean getLeft() {
        return left;
    }
    private void setIndex(int x) {
        index = x;
    }
    private int getIndex() {
        return index;
    }
    public void show() {
        c.client = new BluetoothClient();
        c.client.setListener(new BluetoothAdapter() {
            public void onConnect() {
                c.frmClientMessage.show();
            }
            public void onRecieve(String data) {        
                int index = data.indexOf(":");
                c.frmClientMessage.addMessage(data.substring(0, index), data.substring(index+1));
            }
        });
        new Thread(c.client).start();
        batal = false; 
        left = false;
        setIndex(0);
        gauge.setValue(index);        
        c.display.setCurrent(this);
    }
    
    public void commandAction(Command command, Displayable displayable) {
        batal = true;
        c.frmMenu.show();
    }
}
