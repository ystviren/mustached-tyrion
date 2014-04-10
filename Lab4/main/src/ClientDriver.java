import java.io.*;
import java.net.*;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ClientDriver {
	ZkConnector zkc;
	Watcher watcher;
	ZooKeeper theZoo = null;
	static Socket JobServer = null;
	static ObjectOutputStream out = null;
	static ObjectInputStream in = null;
	
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		String zooInfo = null;
				
		if(args.length == 1 ) { //get config from command line
			zooInfo = args[0];
		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}
		
		// connect to zookeeper to 
		ClientDriver client = new ClientDriver(zooInfo);
		// get the jobserver
		String[] jobServerInfo = client.checkpath().split(":");
		String jobHost = jobServerInfo[0];
		int jobPort = Integer.parseInt(jobServerInfo[1]);
		
		// need to get jobserver connection info
		JobServer = new Socket(jobHost, jobPort);

		out = new ObjectOutputStream(JobServer.getOutputStream());
		in = new ObjectInputStream(JobServer.getInputStream());
			
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		System.out.print("Enter queries or x for exit:\n");
		System.out.print(">");
		while ((userInput = stdIn.readLine()) != null
				&& userInput.toLowerCase().indexOf("x") == -1) {
			/* make a new request packet */
			JobTrackerPacket packetToServer = new JobTrackerPacket();
			packetToServer.type = JobTrackerPacket.JOB_REQUEST;
			out.writeObject(packetToServer);

			/* print server reply */
			JobTrackerPacket packetFromServer;
			packetFromServer = (JobTrackerPacket) in.readObject();

			if (packetFromServer.type == JobTrackerPacket.REPLY_REQUEST) {
				System.out.println("replied to request");
			} else if (packetFromServer.type == JobTrackerPacket.REPLY_QUERRY) {
				System.out.println("replied to querry");
			}
			/* re-print console prompt */
			System.out.print(">");
		}

		/* tell server that i'm quitting */
		JobTrackerPacket packetToServer = new JobTrackerPacket();
		packetToServer.type = JobTrackerPacket.CLIENT_BYE;
		//packetToServer.message = "Bye!";
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		JobServer.close();
	}
	
	public ClientDriver(String hosts) {
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
    
    private String checkpath() {
    	String jobServerInfo = null;
    	// first check that the jobtrack node exists
        Stat stat = zkc.exists("/jobTrack", watcher);
        if (stat != null) {              // znode exist;         	
        	try {
        		// get the data
				jobServerInfo = new String(theZoo.getData("/jobTrack", false, stat));
			} catch (KeeperException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } 
        
        return jobServerInfo;
    }

    private void handleEvent(WatchedEvent event) {
        String path = event.getPath();
        EventType type = event.getType();
        if(path.equalsIgnoreCase("/jobTrack")) {
            if (type == EventType.NodeDeleted) {
                System.out.println("/jobTrack" + " deleted! Let's go!");       
                checkpath(); // try to become the boss
            }
            if (type == EventType.NodeCreated) {
                System.out.println("/jobTrack" + " created!");       
                try{ Thread.sleep(5000); } catch (Exception e) {}
                checkpath(); // re-enable the watch
            }
        }
    }
}
