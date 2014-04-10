import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Dictionary;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

public class FileServer {
	
	String myPath = "/fileSrv";
    ZkConnector zkc;
    Watcher watcher;
    String myInfo = null;   
    static final String fp = "lowercase.rand";
    static ArrayList<ArrayList<String>> wordList = new ArrayList<ArrayList<String>>();
	
	public static void main(String[] args) throws IOException {
        ServerSocket mySocket = null;
        String zooInfo = null;
        
        boolean listening = true;

        try {
        	if(args.length == 3) {
        		mySocket = new ServerSocket(Integer.parseInt(args[0]));
        		zooInfo = args[1];
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
        
        String myInfo = InetAddress.getLocalHost().getHostName() + ":" + args[0];
        
        FileServer t = new FileServer(args[1], myInfo);   
        
        t.checkpath();       

        File file = new File(fp);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        ArrayList<String> tmp = new ArrayList<String>();
        int count = 0;
        int limit = 16608;
        while ((line = br.readLine()) != null) {
           tmp.add(line);
           count++;
           if (count == limit){
        	   count = 0;
        	   wordList.add(tmp);
           }
        }
        br.close();
        
        while (listening) {
        	new FileServerHandlerThread(mySocket.accept(), wordList).start();
        }

        mySocket.close();
    }
	
	
	public FileServer(String hosts, String myInfo) {
		this.myInfo = myInfo;
        zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
 
        watcher = new Watcher() { // Anonymous Watcher
                            @Override
                            public void process(WatchedEvent event) {
                                handleEvent(event);
                        
                            } };
    }
    
    private void checkpath() {
        Stat stat = zkc.exists(myPath, watcher);
        if (stat == null) {              // znode doesn't exist; let's try creating it        	
        	
            System.out.println("Creating " + myPath);
            Code ret = zkc.create(
                        myPath,         // Path of znode
                        myInfo,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK) System.out.println("the boss!");
        } 
    }

    private void handleEvent(WatchedEvent event) {
        String path = event.getPath();
        EventType type = event.getType();
        if(path.equalsIgnoreCase(myPath)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(myPath + " deleted! Let's go!");       
                checkpath(); // try to become the boss
            }
            if (type == EventType.NodeCreated) {
                System.out.println(myPath + " created!");       
                try{ Thread.sleep(5000); } catch (Exception e) {}
                checkpath(); // re-enable the watch
            }
        }
    }
	
}
