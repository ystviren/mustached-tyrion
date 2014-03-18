import java.io.Serializable;
 /**
 * BrokerPacket
 * ============
 * 
 * Packet format of the packets exchanged between the Broker and the Client
 * 
 */




public class MazewarPacket implements Serializable {

	/* define constants */
	public static final int PACKET_NULL    = 0;
	
	// messages sent to server
	public static final int CLIENT_FORWARD = 101;
	public static final int CLIENT_REVERSE   = 102;
	public static final int CLIENT_LEFT   = 103;
	public static final int CLIENT_RIGHT = 104;
	public static final int CLIENT_FIRE     = 105;
	
	
	// first packet sent by client to the server. client expects
	// a reply from server, and then proceeds to enable user actions
	public static final int CLIENT_REGISTER     = 107;
	public static final int CLIENT_BYE     = 108;
	public static final int CLIENT_GETCLIENTS     = 109;
	
	// packets for updating and broadcasting score
	public static final int CLIENT_SCORE_UPDATE = 110;
	public static final int CLIENT_SCORE_SEND = 111;
	
	//Client test packet
	public static final int CLIENT_TEST = 999;
	public static final int CLIENT_UPDATEPOS = 112;
	
	// Messages from server
	// tell clients to add a remote client object
	public static final int REMOTE_ADD    = 201;
	public static final int CLIENT_REMOVE = 202;
		
	// notifty a client that it was hit
	public static final int CLIENT_KILLED = 301;
		
	/* error codes */
	//there shouldnt be any invalid packets unless some special cases occur
	
	/* message header */
	public int type = MazewarPacket.PACKET_NULL;
	
	public String clientName;
	public String sourceName;
	
	public int mapSeed;
	
	public Point clientPosition;
	
	public Direction clientOrientation;
	
	public int clientScore;
		
}
