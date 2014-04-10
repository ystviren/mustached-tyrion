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
    Watcher FSwatcher;
    Socket FSSocket;
    ObjectOutputStream out = null;
	ObjectInputStream in = null;
	// takes in zookeeperinfo, worker id
	public static void main(String[] args) throws IOException {
        String zooInfo = null;
        

        zooInfo = args[0];
        
        //Connect to zookeeper
        Worker t = new Worker(args[0], args[1]);   
        
        t.checkparent();
        t.checkpath();
        String filServInfo = null;
        Stat stat = t.zkc.exists("/fileSrv", t.FSwatcher);
        try {
        	filServInfo = t.zookeeper.getData("/fileSrv", false, stat).toString();
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
        
        String[] fileServerInfo = filServInfo.split(":");
		String fsHost = fileServerInfo[0];
		int fsPort = Integer.parseInt(fileServerInfo[1]);
		
		t.FSSocket = new Socket(fsHost, fsPort);
		
		t.out = new ObjectOutputStream(t.FSSocket.getOutputStream());
		t.in = new ObjectInputStream(t.FSSocket.getInputStream());
		
        while(true){
        	//While there is work do work else spin
        }
    }
	
	
	public Worker(String hosts, String id) {
        zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
        myPath = myParent +"/"+id;
        
        FSwatcher = new Watcher() { // Anonymous Watcher
	            @Override
	            public void process(WatchedEvent event) {
	                handleEvent(event);
	            }};
        
        zookeeper = zkc.getZooKeeper();
    }
	private void checkparent() {
        Stat stat = zkc.exists(myParent, null);
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
    private void checkpath() {
        Stat stat = zkc.exists(myPath, null);
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
		//FS is dead need to switch connection and enable watcher. 
		
	}
	
}
