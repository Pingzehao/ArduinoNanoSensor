package chat;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

public class TFrmBantuan extends Form  implements CommandListener {
    private Chat c;
    public TFrmBantuan(final Chat c) {
        super("Bantuan");
        this.c = c;
        append(new StringItem("Cara Penggunaan\n", 
                "Aplikasi chat via bluetooth ini dapat digunakan untuk berkomunikasi " +
                "data (teks) antara dua ponsel yang mendudung teknologi bluetooth"));
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
