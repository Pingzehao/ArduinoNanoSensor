package chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class BluetoothServer implements Runnable {
    private static String eof = "EOF\r\n";
    /**
     *  Delay pemrosesan default 500 = 0.5 detik
     */
    private static int DELAY = 100;
    /**
     *  Variable untuk local device
     */
    public LocalDevice local_device;
    /**
     *  Variabel untuk discovery agent
     */
    private DiscoveryAgent disc_agent;
    /**
     *  Variabel untuk inquiry
     */
    private InquiryListener inq_listener;
    /**
     *  Variabel untuk alamat URL untuk koneksi bluetooth
     */
    private String[] urls;
    /**
     *  Variabel untuk menampung UUID client yang terkoneksi
     */
    private UUID[] u;
    /**
     *  Variabel untuk perulangan / enumerasi
     */
    private Enumeration devices;
    /**
     *  Variabel untuk koneksi dengan client yang ditemukan
     */
    private StreamConnection[] conns;
    private OutputStream[] oss;
    private InputStream[] iss;
    private boolean[] connecteds;
    /**
     *  Variabel untuk menampung jumlah client yang ditemukan
     */
    private int device_count;
    /**
     *  Variabel untuk status server
     */
    public boolean active;
    /**
     *  Pesan yang akan di transfer
     */
    private Vector pesans = new Vector();
    private String pesan = "";
    private BluetoothAdapter adapter;
    private int log_level;
    /**
     *  Constructor untuk server
     */    
    public BluetoothServer() {        
        active = true;        
    }
    /**
     *  Thread server bluetooth
     */
    public void run() {
        try {
            //System.out.println("[Server] --> Searching...");
            local_device = LocalDevice.getLocalDevice();
            disc_agent = local_device.getDiscoveryAgent();
            local_device.setDiscoverable( DiscoveryAgent.NOT_DISCOVERABLE );
            inq_listener = new InquiryListener();
            synchronized(inq_listener) {
                disc_agent.startInquiry(DiscoveryAgent.LIAC, inq_listener);
                inq_listener.wait();
            }
            u = new UUID[1];
            u[0] = new UUID( "00000000000010008000006057028C19", false );
            int[] attrbs = {0x0100};
            devices = inq_listener.cached_devices.elements();
            ServiceListener serv_listener = new ServiceListener();
            while(devices.hasMoreElements()) {
                synchronized( serv_listener ) {
                    disc_agent.searchServices( attrbs, u, (RemoteDevice) devices.nextElement(), serv_listener );
                    serv_listener.wait();
                }
            }
            device_count = serv_listener.FoundServiceRecords.size();
            
            conns = new StreamConnection[device_count];
            urls = new String[device_count];
            oss = new OutputStream[device_count];
            iss = new InputStream[device_count];
            connecteds = new boolean[device_count];
            for( int i = 0; i < device_count; i++ ) {
                urls[i] = ((ServiceRecord) serv_listener.FoundServiceRecords.elementAt( i )).getConnectionURL( 0, false );  
                connecteds[i] = false;
            } 
            //System.out.println("[Server] --> Searching finish found "+device_count+" device");
            if (adapter != null) {
                adapter.onCompleteSearch();
            }
            while (active) {
                try {                    
                    pesan = " ";
                    if (pesans.size() > 0) {
                        pesan = (String) pesans.elementAt(0);
                    }                    
                    for( int i = 0; i < device_count; i++ ) {
                        if (!connecteds[i]) {
                            connect(i);
                        }
                        proses(i, pesan);                            
                    }
                    if (pesans.size() > 0) {
                        pesans.removeElementAt(0);               
                    }
                    Thread.sleep(DELAY);                                        
                } catch (Exception e) {                    
                    continue;
                }
            }            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void connect(int index) {
        try {
            connecteds[index] = false;
            //System.out.println("[Server] --> Connecting...");
            conns[index] = (StreamConnection) Connector.open(urls[index]);  
            oss[index] = conns[index].openOutputStream();
            iss[index] = conns[index].openInputStream();
            //System.out.println("[Server] --> Connected");
            if (adapter != null) {
                adapter.onConnect();
            }
            connecteds[index] = true;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void write(int index, String data) {
        try {
            if (connecteds[index]) {
                //System.out.println("[Server] --> Sending...");
                //System.out.println("[Data] --> "+data);
                oss[index].write(data.getBytes());
                oss[index].write(eof.getBytes());
                oss[index].flush();  
                //System.out.println("[Server] --> Sending finish");
            }
        } catch (Exception e) {
            connecteds[index] = false;
            e.printStackTrace();
        }
    }
    
    public String read(int index) {
        StringBuffer buffer = new StringBuffer("");
        try {
            if (connecteds[index]) {
                //System.out.println("[Server] --> Reading...");
                int x = 0;
                while (x != -1) {
                    x = iss[index].read();
                    if (x != -1) {
                        buffer.append((char) x);
                    }
                    if (buffer.toString().endsWith(eof)) {
                        buffer.delete(buffer.length()-eof.length(), buffer.length());
                        break;
                    }
                }            
                //System.out.println("[Data] --> "+buffer.toString());
                //System.out.println("[Server] --> Reading finish...");
            }
        } catch (Exception e) {
            connecteds[index] = false;
            e.printStackTrace();
        }        
        return buffer.toString();
    }
    
    public void proses(int index, String pesan) {
        write(index, pesan);
        String respon = read(index).trim();
        if (adapter != null && !respon.trim().equals("")) {
            adapter.onRecieve(respon);
        }
    }
        
    public void send(String pesan) {
        pesans.addElement(pesan);
    }
    
    public void setListener(BluetoothAdapter adapter) {
        this.adapter = adapter;
    }
    
    public String getName() {
        return local_device.getFriendlyName();
    }
    public String getId() {
        return local_device.getBluetoothAddress();
    }
    private class InquiryListener implements DiscoveryListener {
        public Vector cached_devices;
        public InquiryListener() {
            cached_devices = new Vector();
        }
        public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
            int major = cod.getMajorDeviceClass();
            if( major == 0x0200 ) {
                if(!cached_devices.contains(btDevice)) {
                    cached_devices.addElement( btDevice );
                }
            }
        }
        public void inquiryCompleted(int discType) {
            synchronized(this) {
                this.notify();
            }
        }
        public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        
        }
        public void serviceSearchCompleted(int transID, int respCode) {
        }
    }
    private class ServiceListener implements DiscoveryListener {
        private Vector FoundServiceRecords;
        public ServiceListener() {
            FoundServiceRecords = new Vector();
        }
        public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
            DataElement sn;
            FoundServiceRecords.addElement( servRecord[0] );
        }
        public void serviceSearchCompleted(int transID, int respCode) {
            synchronized(this) {
                this.notify();
            }
        }
        
        public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        }
        public void inquiryCompleted(int discType) {
        }
    }
}
