import java.io.Serializable;
import java.util.List;
 /**
 * MazewarPacket
 * ============
 */

class ClientInfo implements Serializable {
	public String clientName; // name of the client that sent the packet
	public String clientHostname; 
	public int clientPort;
	public int clientID;
	
	public ClientInfo(String name, String hostname, int port, int ID) {
		this.clientName = name;
		this.clientHostname = hostname;
		this.clientPort = port;
		this.clientID = ID;
	}
}

class Event implements Serializable{
	public int action;
	public int initTime;
	public int timeDeliver;
	public boolean deliverable;
	public int pID;
	
	public Event(int pID, int timeSent, int action){
		this.action = action;
		this.pID = pID;
		this.deliverable = false;
		this.initTime = timeSent;
		this.timeDeliver = timeSent;
	}
	
	public void setDeliverable(int deliverTime){
		this.deliverable = true;
		this.timeDeliver = deliverTime;
	}
}

public class MazewarPacket implements Serializable {

	/* define constants */
	public static final int PACKET_NULL		= 0;
	
	// messages sent to server
	public static final int CLIENT_FORWARD	= 101;
	public static final int CLIENT_REVERSE	= 102;
	public static final int CLIENT_LEFT		= 103;
	public static final int CLIENT_RIGHT	= 104;
	public static final int CLIENT_FIRE		= 105;
	
	
	// first packet sent by client to the server. client expects
	// a reply from server, and then proceeds to enable user actions
	public static final int CLIENT_REGISTER		= 107;
	public static final int CLIENT_BYE 			= 108;
	public static final int CLIENT_GETCLIENTS 	= 109;
	
	// packets for updating and broadcasting score
	public static final int CLIENT_SCORE_UPDATE	= 110;
	public static final int CLIENT_SCORE_SEND	= 111;
	
	//Client test packet
	public static final int CLIENT_TEST 		= 999;
	public static final int CLIENT_UPDATEPOS	= 112;
	
	// Messages from server
	// tell clients to add a remote client object
	public static final int REMOTE_ADD    	= 201;
	public static final int CLIENT_REMOVE	= 202;
		
	// notifty a client that it was hit
	public static final int CLIENT_KILLED 	= 301;
	
	// Server Reply
	public static final int NAME_SERVER_REPLY = 501;
	public static final int NAME_SERVER_BYE = 502;
	public static final int CLIENT_LOOKUP_REGISTER = 503;
	
	/* error codes */
	//there shouldnt be any invalid packets unless some special cases occur
	
	/* message header */
	public int type = MazewarPacket.PACKET_NULL;
	
	public Point clientPosition;
	public Direction clientOrientation;
	public int clientScore;
	
	public String sourceName; // second client name for certain actions
	
	public int mapSeed;
	
<<<<<<< HEAD
	public List<ClientInfo> remoteList = null;
=======
	//  make client hold its own Info
	public ClientInfo myInfo = null;
	
	List<ClientInfo> remoteList = null;
>>>>>>> d4dfd7509958f9bc87f6d7f6613e71649d892f03
	
	
	// event packet
	public Event event;
	
	public static int NEW_EVENT = 601;
	public static int CONFIRM_EVENT = 602;
	public static int DELIVER_EVENT = 603;
	
}
