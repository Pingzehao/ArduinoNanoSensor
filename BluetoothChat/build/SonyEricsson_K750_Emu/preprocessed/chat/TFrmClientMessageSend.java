package chat;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class TFrmClientMessageSend extends TextBox  implements CommandListener {
    private Chat c;
    public TFrmClientMessageSend(final Chat c) {
        super("client@chat", "", 255, TextField.ANY);
        this.c = c;        
        addCommand(new Command("Kirim", Command.OK, 0));
        addCommand(new Command("Batal", Command.BACK, 1));
        setCommandListener(this);
    }
    public void show() {
        setString("");
        c.display.setCurrent(this);
    }
    
    
    public void commandAction(Command command, Displayable displayable) {
        if (command.getLabel().equals("Kirim")) {
            if (getString().equals("")) {
                c.display.setCurrent(new Alert("Masukkan pesan"), this);
                return;
            }
            c.client.send(c.client.getName()+":"+getString());
            c.frmClientMessage.addMessage(c.client.getName(), getString());
            c.display.setCurrent(c.frmClientMessage);
        } else {
            c.display.setCurrent(c.frmClientMessage);
        }
    }
}
