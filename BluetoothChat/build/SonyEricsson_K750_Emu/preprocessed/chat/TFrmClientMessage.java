package chat;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

public class TFrmClientMessage extends List  implements CommandListener {
    private Chat c;
    public TFrmClientMessage(final Chat c) {
        super("client@chat", IMPLICIT);
        this.c = c;        
        addCommand(new Command("Kirim", Command.OK, 0));
        addCommand(new Command("Tutup", Command.BACK, 1));
        setCommandListener(this);
    }
    public void show() {
        deleteAll();   
        c.display.setCurrent(this);
    }
    
    public void addMessage(String sender, String message) {
        if (size() > 20) {
            delete(0);
        }
        append("["+sender+"]\n"+message, null);
        setSelectedIndex(size()-1, true);
    }
    
    public void commandAction(Command command, Displayable displayable) {
        if (command.getLabel().equals("Kirim")) {
            c.frmClientMessageSend.show();
        } else {
            c.frmTutup.show('c');
        }
    }
}
