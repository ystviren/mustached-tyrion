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
	Socket JobServer = null;
	ObjectOutputStream out = null;
	ObjectInputStream in = null;
	Boolean active = true;
	JobTrackerPacket lastReq = null;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// zookeeper connection info
		String zooInfo = null;		
		if(args.length == 1 ) { //get config from command line
			zooInfo = args[0];
		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}
		
		// connect to zookeeper 
		ClientDriver client = new ClientDriver(zooInfo);
		// get the jobserver info
		String[] jobTrackerInfo = client.checkpath().split(":");
		
		// need to get jobserver connection info
		client.JobServer = new Socket(jobTrackerInfo[0], Integer.parseInt(jobTrackerInfo[1]));
		client.out = new ObjectOutputStream(client.JobServer.getOutputStream());
		client.in = new ObjectInputStream(client.JobServer.getInputStream());
			
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		System.out.print("Enter queries or x for exit:\n");
		System.out.print(">");
		// Here we assume that ANYTHING the user inputs that is not an x is a job request
		// When the jobTracker receives the request, it will determine if a duplicate exists,
		// in which case it treats the request as a querry and returns the state of the job
		// otherwise it goes ahead with processing a new job request
		while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("x") == -1) {
			if (client.active) { // naive error handling that simply ignores user input until a new connection is established
								 // ideally would just buffer requests and send batch when new connection occurs
				/* make a new request packet */
				JobTrackerPacket packetToServer = new JobTrackerPacket();
				packetToServer.type = JobTrackerPacket.JOB_REQUEST;
				packetToServer.hash = userInput;
				// save packet for failure handling
				client.lastReq = packetToServer;
				
				client.out.writeObject(packetToServer);
	
				/* server reply */
				JobTrackerPacket packetFromServer = (JobTrackerPacket) client.in.readObject();
	
				if (packetFromServer.type == JobTrackerPacket.REPLY_REQUEST) {
					if (packetFromServer.error_code == 0) {
						System.out.println("New job request submitted");
					} else {
						System.out.println("Invalid job request");
					}
				} else if (packetFromServer.type == JobTrackerPacket.REPLY_QUERRY) {
					if (packetFromServer.error_code == 0) {
						System.out.println("Job Querry");
						System.out.println(packetFromServer.status);
					} else {
						System.out.println("Invalid job querry");
					}
				}
				/* re-print console prompt */
				System.out.print(">");
			}
		}

		/* tell server that i'm quitting */
		JobTrackerPacket packetToServer = new JobTrackerPacket();
		packetToServer.type = JobTrackerPacket.CLIENT_BYE;
		client.out.writeObject(packetToServer);

		client.out.close();
		client.in.close();
		stdIn.close();
		client.JobServer.close();
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
    
	// looks for the /jobTrack node and gets its data
    private String checkpath() {
    	String jobServerInfo = null;
    	
    	// first check that the jobtrack node exists
        Stat stat = zkc.exists("/jobTrack", watcher);
        if (stat != null) {              // znode exist;         	
        	try {
        		// get the data
				jobServerInfo = new String(theZoo.getData("/jobTrack", watcher, stat));
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
            	System.out.println("/jobTrack" + " deleted!");
            	// close the connections
            	try {
					out.close();
					in.close();
	        		JobServer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		
            	active = false;
            	
            	// reset watch
            	zkc.exists("/jobTrack", watcher);
            	
            }
            if (type == EventType.NodeCreated) {
            	System.out.println("/jobTrack" + " created!");
            	// connect to the new jobtrack node
            	// get the jobserver
        		String[] jobServerInfo = checkpath().split(":");
        		String jobHost = jobServerInfo[0];
        		int jobPort = Integer.parseInt(jobServerInfo[1]);
        		
        		// need to get jobserver connection info
        		try {
					JobServer = new Socket(jobHost, jobPort);
	        		out = new ObjectOutputStream(JobServer.getOutputStream());
	        		in = new ObjectInputStream(JobServer.getInputStream());
	        		active = true;
	        		
	        		// reset the watch
	        		zkc.exists("/jobTrack", watcher);
	        		
	        		// resend the last packet? 
	        		
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
    }
}
