import java.io.Serializable;
 /**
 * BrokerPacket
 * ============
 * 
 * Packet format of the packets exchanged between the Broker and the Client
 * 
 */


/* inline class to describe host/port combo */
class ServerLocation implements Serializable {
	public String  server_host;
	public Integer server_port;
	
	/* constructor */
	public ServerLocation(String host, Integer port) {
		this.server_host = host;
		this.server_port = port;
	}
	
	/* printable output */
	public String toString() {
		return " HOST: " + server_host + " PORT: " + server_port; 
	}
	
}

public class MazewarPacket implements Serializable {

	/* define constants */
	public static final int PACKET_NULL    = 0;
	
	// messages sent to server
	public static final int CLIENT_FORWARD = 101;
	public static final int CLIENT_REVERSE   = 102;
	public static final int CLIENT_LEFT   = 103;
	public static final int CLIENT_RIGHT = 104;
	public static final int CLIENT_FIRE     = 105;
	// client notifies server that its projectile hit something
	public static final int CLIENT_SCOREHIT     = 106;
	// first packet sent by client to the server. client expects
	// a reply from server, and then proceeds to enable user actions
	public static final int CLIENT_REGISTER     = 107;
	public static final int CLIENT_BYE     = 108;
	public static final int CLIENT_GETCLIENTS     = 109;
	
	public static final int CLIENT_SCORE_UPDATE = 110;
	public static final int CLIENT_SCORE_SEND = 111;
	
	//Test packet
	public static final int CLIENT_TEST = 999;
	
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
