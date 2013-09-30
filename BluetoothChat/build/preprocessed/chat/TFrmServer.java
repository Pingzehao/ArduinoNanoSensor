package chat;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.StringItem;

public class TFrmServer extends Form  implements CommandListener {
    private Chat c;
    private Gauge gauge;
    private boolean batal;
    private Timer timer;
    private TimerTask animate;
    private boolean left;
    private int index;    
    public TFrmServer(final Chat c) {
        super("Server Chat");
        this.c = c;        
        timer = new Timer();
        gauge = new Gauge("Mencari client", false, 10, 0);
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
        c.server = new BluetoothServer();
        c.server.setListener(new BluetoothAdapter() {
            public void onConnect() {
                c.frmServerMessage.show();
            }
            public void onRecieve(String data) {        
                int index = data.indexOf(";");
                String sender = data.substring(0, index);
                int idx_nama = sender.indexOf(":");
                c.frmServerMessage.addMessage(sender.substring(idx_nama+1), data.substring(index+1));
                // teruskan ke yang lain
                c.server.send(data);
            }
        });
        new Thread(c.server).start();
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
