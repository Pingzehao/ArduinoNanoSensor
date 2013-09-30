package chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class BluetoothClient implements Runnable {    
    private static String eof = "EOF\r\n";
    public LocalDevice local_device;
    private DiscoveryAgent disc_agent;
    private String service_UUID;
    private String player_name;
    private String url;
    private StreamConnectionNotifier notifier;
    private StreamConnection conn;
   
    /**
     *  Delay pemrosesan default 500 = 0.5 detik
     */
    private static int DELAY = 100;
    /**
     *  Variabel untuk status loging
     */
    private int log_level;
    public boolean active;
    private BluetoothAdapter adapter;
    
    private OutputStream os = null;
    private InputStream is = null;
    private boolean connected = false;
    private String data = " ";
    public BluetoothClient() {
        try {
            local_device = LocalDevice.getLocalDevice();
            disc_agent = local_device.getDiscoveryAgent();
            local_device.setDiscoverable( DiscoveryAgent.LIAC );
            service_UUID = "00000000000010008000006057028C19";
            player_name = local_device.getFriendlyName();
            url = "btspp://localhost:" + service_UUID + ";name="+player_name;    
            active = true;
            adapter = null;
        } catch (Exception e) {            
        }
    }    
    
    public void run() {
        try {                                   
            System.out.println("Waiting...");
            notifier = (StreamConnectionNotifier) Connector.open( url );
            while (active) {
                try {  
                    if (!connected) {
                        openConnection();
                    }
                    proses(conn);
                    Thread.sleep(DELAY);
                } catch (Exception e) {
                    // try reconnect
                    openConnection();
                    continue;                            
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void openConnection() {
        try {
            connected = false;
            System.out.println("[Client] --> Waiting...");
            conn = (StreamConnection) notifier.acceptAndOpen();     
            os = conn.openOutputStream();
            is = conn.openInputStream();
            System.out.println("[Client] --> Connected");
            connected = true;
            if (adapter != null) {
                adapter.onConnect();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void send(String data) {
        this.data = data;
    }
    
    private void write(String data) {
        try {
            if (connected) {
                System.out.println("[Client] --> Sending...");
                System.out.println("[Data] --> "+data);
                os.write(data.getBytes());
                os.write(eof.getBytes());
                os.flush();
            }
            System.out.println("[Client] --> Sending finish");
        } catch (Exception e) {
            connected = false;
            e.printStackTrace();
        }
    }
    
    private String read() {
        StringBuffer buffer = new StringBuffer("");
        try {
            if (connected) {
                System.out.println("[Client] --> Reading....");
                int x = 0;
                while (x != -1) {
                    x = is.read();
                    if (x != -1) {
                        buffer.append((char) x);
                    }
                    if (buffer.toString().endsWith(eof)) {
                        buffer.delete(buffer.length()-eof.length(), buffer.length());
                        break;
                    }
                }
                System.out.println("[Data] --> "+buffer.toString());
                System.out.println("[Client] --> Reading finish");
            }
        } catch (Exception e) {
            connected = false;
            e.printStackTrace();
        }
        return buffer.toString();
    }
    
    private void proses(StreamConnection conn) {     
        write(data);
        String respon = read().trim();   
        if (adapter != null && !respon.trim().equals("")) {
            adapter.onRecieve(respon);            
        }
        data = " ";
    }
    public void setListener(BluetoothAdapter adapter) {
        this.adapter = adapter;
    }
    
    public String getName() {
        return local_device.getFriendlyName();
    }
}
