import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.Code;

public class JobTrackerHandlerThread extends Thread {

	Socket mySocket = null;
	ZooKeeper theZoo = null;
	ZkConnector zkc = null;
	
	public JobTrackerHandlerThread(Socket socket, ZooKeeper zoo, ZkConnector zkc) {
		super("JobTrackerHandlerThread");
		this.theZoo = zoo;
		this.mySocket = socket;
		this.zkc = zkc;
		System.out.println("Created new Thread to handle client");
	}

	// Handle connections from client drivers
	public void run() {

		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(mySocket.getInputStream());
			JobTrackerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(mySocket.getOutputStream());
			
			while (( packetFromClient = (JobTrackerPacket) fromClient.readObject()) != null) {
				
				if (packetFromClient.type == JobTrackerPacket.JOB_REQUEST) {
					// do a lookup on zoo keeper.
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
					
					// check if the job request already exists:
					int i;
					for (i = 0; i < jobsList.size(); i++) {
						if (jobsList.get(i).contains(packetFromClient.hash)) {
							// turn the request into a querry
							String path = jobsList.get(i);
							// check the state of the job:
							JobTrackerPacket packetToClient = new JobTrackerPacket();
							packetToClient.type = JobTrackerPacket.REPLY_QUERRY;
							try {
								packetToClient.status = new String(theZoo.getData(path, null, theZoo.exists(path, null)));
							} catch (KeeperException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							toClient.writeObject(packetToClient);
							
							continue;
						}						
					}
					
					// if we get here its because this request is new \o/
					// create the node and its children
					Code ret = zkc.create(
	                        ("/jobs/"+packetFromClient.hash),         // Path of znode
	                        null,           // Data not needed.
	                        CreateMode.EPHEMERAL   // Znode type, set to EPHEMERAL.
	                        );
					if (ret == Code.OK) {
						System.out.println("created new job request");
						for (i = 0; i < 16; i++) {
							// create children
							ret = zkc.create( ("/jobs/"+packetFromClient.hash+"/"+String.valueOf(i)), null, CreateMode.EPHEMERAL);
						}
					}
					
					// return a packet acknowledging the submission of request
					JobTrackerPacket packetToClient = new JobTrackerPacket();
					packetToClient.type = JobTrackerPacket.REPLY_REQUEST;
					toClient.writeObject(packetToClient);
					
				} else if (packetFromClient.type == JobTrackerPacket.JOB_QUERRY) {
					// handle querry
				} 
				
			}
		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}
}
