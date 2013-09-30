package chat;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class TFrmMenu extends List implements CommandListener{ 
    private Chat c;
    public TFrmMenu(final Chat c) {
        super("Chat@Bluetooth", IMPLICIT);
        this.c = c;
        try {
            append("Server", Image.createImage("/images/server.png"));
            append("Client", Image.createImage("/images/client.png"));
            append("Bantuan", Image.createImage("/images/bantuan.png"));
            append("Informasi", Image.createImage("/images/info.png"));
            append("Tutup", Image.createImage("/images/tutup.png"));
            
            addCommand(new Command("Pilih", Command.BACK, 0));
            setCommandListener(this);
        } catch (Exception e) {            
            System.out.println("Error:\nInisialisasi TFrmMenu\n"+e.getMessage());
        }
    }    
    public void show() {
        c.display.setCurrent(this);
    }

    public void commandAction(Command command, Displayable displayable) {
        if (getSelectedIndex() == 0) {
            c.frmServer.show();
        }
        if (getSelectedIndex() == 1) {
            c.frmClient.show();
        }
        if (getSelectedIndex() == 2) {
            c.frmBantuan.show();
        }
        if (getSelectedIndex() == 3) {
            c.frmInformasi.show();
        }
        if (getSelectedIndex() == 4) {
            c.frmTutup.show();  
        }
    }

}
