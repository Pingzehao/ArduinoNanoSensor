package chat;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

public class TFrmInformasi extends Form  implements CommandListener {
    private Chat c;
    public TFrmInformasi(final Chat c) {
        super("Tentang Program");
        this.c = c;
        append(new StringItem("", 
                "Chat@Bluetooth\n" +
                "Oleh: Didin"));
        addCommand(new Command("OK", Command.BACK, 1));
        setCommandListener(this);
    }
    
    public void show() {
        c.display.setCurrent(this);
    }
    
    public void commandAction(Command command, Displayable displayable) {
        c.frmMenu.show();
    }
}
