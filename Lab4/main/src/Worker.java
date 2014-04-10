import java.io.*;
import java.net.*;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

public class Worker {
	
	String myPath = "/worker";
    ZkConnector zkc;
    Watcher watcher;
    Watcher watchParent = null;
    String myInfo = null;   
	
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
        
        Worker t = new Worker(args[1], myInfo);   
        
        t.checkparent();
        t.checkpath(args[2]);       

        while (listening) {
        	new JobTrackerHandlerThread(mySocket.accept()).start();
        }

        mySocket.close();
    }
	
	
	public Worker(String hosts, String myInfo) {
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
	private void checkparent() {
        Stat stat = zkc.exists(myPath, watchParent);
        if (stat == null) {              // znode doesn't exist; let's try creating it        	
        	
            System.out.println("Creating " + myPath);
            Code ret = zkc.create(
                        myPath,         // Path of znode
                        null,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK){};

        } 
    }
    private void checkpath(String id) {
        Stat stat = zkc.exists(myPath +"/"+id, watcher);
        if (stat == null) {              // znode doesn't exist; let's try creating it        	
        	
            System.out.println("Creating " + myPath);
            Code ret = zkc.create(
            			myPath +"/"+id,         // Path of znode
                        myInfo,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK){};

        } 
    }

    private void handleEvent(WatchedEvent event) {
        String path = event.getPath();
        EventType type = event.getType();
        if(path.equalsIgnoreCase(myPath)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(myPath + " deleted! Let's go!");       
                //checkpath(); // try to become the boss
            }
            if (type == EventType.NodeCreated) {
                System.out.println(myPath + " created!");       
                try{ Thread.sleep(5000); } catch (Exception e) {}
                //checkpath(); // re-enable the watch
            }
        }
    }
	
}
