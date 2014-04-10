import java.io.*;
import java.net.*;
import java.util.ArrayList;

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

		// Connect to zookeeper
		Worker t = new Worker(args[0], args[1]);

		t.checkparent();
		t.checkpath();
		String filServInfo = null;
		Stat stat = t.zkc.exists("/fileSrv", t.FSwatcher);
		while (stat == null) {
			stat = t.zkc.exists("/fileSrv", t.FSwatcher); // poll until a file
															// server is alive
		}
		try {
			filServInfo = new String(t.zookeeper.getData("/fileSrv", false, stat));
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
		try {
			System.out.println("Starting jobs");
			while (true) {
				Stat tmp = t.zkc.exists("/jobs", null);
				Stat tmp2 = new Stat();
				if (tmp != null) {
					ArrayList<String> list = new ArrayList<String>(t.zookeeper.getChildren("/jobs", false));
					for (int i = 0; i < list.size(); i++){
						//check if the job is done, if it is not do a part
						//if all jobs are done, just loop around
						
						//TODO: grab some kind of lock???
						String tmpString = new String (t.zookeeper.getData("/jobs/"+list.get(i), false, tmp2));
						if (tmpString.equals("pending")){
							System.out.println("Doing job " + list.get(i));
							ArrayList<String> listJobs = new ArrayList<String>(t.zookeeper.getChildren("/jobs/"+list.get(i), false));
							for (int j = 0; j < listJobs.size(); j++){
								if (t.zookeeper.getData("/jobs/"+list.get(i)+"/"+listJobs.get(j), false, null) == null){
									System.out.println("Looking at " + list.get(i) + "with parition " + j);
									t.hashMatch(list.get(i), j, t);

								}
							}
						}
						//send a packet to FileServer requesting dictionary section
						// for each word, compute the hash and string cmp.
						// if match set job as found, else mark sub part as done
					}
				}
			}
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void hashMatch(String hash, int partition, Worker worker){
		FileServerPacket toFs = new FileServerPacket();
		FileServerPacket fromFs = null;
		
		toFs.type = FileServerPacket.FILE_REQUEST;
		toFs.partition = partition;
		
		try {
			worker.out.writeObject(toFs);
			fromFs = (FileServerPacket) worker.in.readObject();
			for (int i = 0; i < fromFs.words.size(); i++){
				//get hash for each function
				if(MD5Test.getHash(fromFs.words.get(i)).equals(hash)){
					worker.zookeeper.setData("/jobs/"+hash+"/"+partition, "Found".getBytes(), -1);
					worker.zookeeper.setData("/jobs/"+hash, "Found".getBytes(), -1);
					return;
				}
			}
			
			worker.zookeeper.setData("/jobs/"+hash+"/"+partition, "NotFound".getBytes(), -1);
			ArrayList<String> listJobs = new ArrayList<String>(zookeeper.getChildren("/jobs/"+hash, false));
			for (int j = 0; j < listJobs.size(); j++){
				if (zookeeper.getData("/jobs/"+hash+"/"+listJobs.get(j), false, null) == null){
					return;
				}
			}
			worker.zookeeper.setData("/jobs/"+hash, "NotFound".getBytes(), -1);
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	public Worker(String hosts, String id) {
		zkc = new ZkConnector();
		try {
			zkc.connect(hosts);
		} catch (Exception e) {
			System.out.println("Zookeeper connect " + e.getMessage());
		}
		myPath = myParent + "/" + id;

		FSwatcher = new Watcher() { // Anonymous Watcher
			@Override
			public void process(WatchedEvent event) {
				handleEvent(event);
			}
		};

		zookeeper = zkc.getZooKeeper();
	}

	private void checkparent() {
		Stat stat = zkc.exists(myParent, null);
		if (stat == null) { // znode doesn't exist; let's try creating it

			System.out.println("Creating " + myParent);
			Code ret = zkc.create(myParent, // Path of znode
					null, // Data not needed.
					CreateMode.PERSISTENT // Znode type, set to EPHEMERAL.
					);
			if (ret == Code.OK) {
				System.out.println("Created " + myParent);
			}
			;

		}
	}

	private void checkpath() {
		Stat stat = zkc.exists(myPath, null);
		if (stat == null) { // znode doesn't exist; let's try creating it

			System.out.println("Creating " + myPath);
			Code ret = zkc.create(myPath, // Path of znode
					null, // Data not needed.
					CreateMode.EPHEMERAL // Znode type, set to EPHEMERAL.
					);
			if (ret == Code.OK) {
				System.out.println("Created" + myPath);
			};

		}
	}

	private void handleEvent(WatchedEvent event) {
		// FS is dead need to switch connection and enable watcher.

	}

}
