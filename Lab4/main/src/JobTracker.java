import java.io.*;
import java.net.*;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

public class JobTracker {
	
	String myPath = "/jobTrack";
    ZkConnector zkc;
    Watcher watcher;
    String myInfo = null;   
    ZooKeeper theZoo = null;
    boolean listening = false;
    
	public static void main(String[] args) throws IOException {
		
        ServerSocket mySocket = null;
        String zooInfo = null;

        try {
        	if(args.length == 2) {
        		// set up our server socket
        		mySocket = new ServerSocket(Integer.parseInt(args[0]));
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
        
        // format: localhost:port        
        zooInfo = args[1];
        // format: localhost:port
        String myInfo = InetAddress.getLocalHost().getHostName() + ":" + args[0];
        
        // connect to zookeeper
        JobTracker tracker = new JobTracker(zooInfo, myInfo);   
        // set up znode
        tracker.checkpath();       

        while (true) {
        	if (tracker.listening) { // we are the primary
        		new JobTrackerHandlerThread(mySocket.accept(), tracker.theZoo, tracker.zkc).start();
        	} else { // we are the secondary
        		try{ Thread.sleep(5000); } catch (Exception e) {}
        	}
        }

        //mySocket.close();
    }
	
	
	public JobTracker(String hosts, String myInfo) {
		this.myInfo = myInfo;
        zkc = new ZkConnector();
        try {
            zkc.connect(hosts);
        } catch(Exception e) {
            System.out.println("Zookeeper connect "+ e.getMessage());
        }
 
        theZoo = zkc.getZooKeeper();
        
        watcher = new Watcher() { // Anonymous Watcher
                            @Override
                            public void process(WatchedEvent event) {
                                handleEvent(event);
                        
                            } };
    }
    
	// try to create znode and become primary. if we fail, become secondary
    private boolean checkpath() {
    	boolean result = false;
        Stat stat = zkc.exists(myPath, watcher);
        if (stat == null) {              // znode doesn't exist; let's try creating it        	
   
            System.out.println("Creating " + myPath);
            Code ret = zkc.create(
                        myPath,         // Path of znode
                        myInfo,           // Data not needed.
                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
                        );
            if (ret == Code.OK) {
            	System.out.println("the boss!");
            	listening = true;
            	result = true;
            }
        } 
        return result;
    }

    private void handleEvent(WatchedEvent event) {
        String path = event.getPath();
        EventType type = event.getType();
        if(path.equalsIgnoreCase(myPath)) {
            if (type == EventType.NodeDeleted) {
                System.out.println(myPath + " deleted! Let's go!");       
                if (checkpath()) { // try to become the boss
                	// now we need to go through all the nodes under /jobs to check for consistency
                	if (zkc.exists("/jobs", null) == null) {
                		// no jobs have been submitted, so theres really nothing to recover from
                		return;
                	}
                	
                	
                	List<String> jobsList = null;
					try {
						jobsList = theZoo.getChildren("/jobs", null);
					} catch (KeeperException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					// check if the state of job tree:
					int i;
					for (i = 0; i < jobsList.size(); i++) {
						// for each child, check that it has 16 children
						List<String> temp = null;
						try {
							temp = theZoo.getChildren(jobsList.get(i), null);
						} catch (KeeperException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (temp.size() < 16) { // need to fill in remaining children
							int j;
							for (j = temp.size(); j < 16; j++) {
								zkc.create( ("/jobs/"+jobsList.get(i)+"/"+String.valueOf(j)), null, CreateMode.PERSISTENT);
							}
						}
					}
                }
                
            }
            if (type == EventType.NodeCreated) {
                System.out.println(myPath + " created!");       
                try{ Thread.sleep(5000); } catch (Exception e) {}
                checkpath(); // re-enable the watch
            }
        }
    }
	
}
