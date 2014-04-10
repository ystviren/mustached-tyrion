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
	
	String myParent = "/worker";
	String myPath = null;
    ZkConnector zkc;
    ZooKeeper zookeeper;
    Watcher watcher;
    Watcher watchParent = null;
	
	public static void main(String[] args) throws IOException {
        String zooInfo = null;
        

        zooInfo = args[0];
        
        //Connect to zookeeper
        Worker t = new Worker(args[0], args[1]);   
        
        t.checkparent();
        t.checkpath(args[2]);       

    }
	
	
	public Worker(String hosts, String id) {
        zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
        myPath = myParent +"/"+id;
        watchParent = new Watcher() { // Anonymous Watcher
            @Override
            public void process(WatchedEvent event) {
                handleEvent(event);
        
            } };
 
        watcher = new Watcher() { // Anonymous Watcher
                            @Override
                            public void process(WatchedEvent event) {
                                handleEvent(event);
                        
                            } };
       zookeeper = zkc.getZooKeeper();
    }
	private void checkparent() {
        Stat stat = zkc.exists(myParent, watchParent);
        if (stat == null) {              // znode doesn't exist; let's try creating it        	
        	
            System.out.println("Creating " + myParent);
            Code ret = zkc.create(
                        myParent,         // Path of znode
                        null,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK){};

        } 
    }
    private void checkpath(String id) {
        Stat stat = zkc.exists(myPath, watcher);
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
