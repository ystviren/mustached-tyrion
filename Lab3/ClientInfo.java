import java.io.Serializable;

class ClientInfo implements Serializable {
	public String clientName; // name of the client that sent the packet
	public String clientHostname; 
	public int clientPort;
	public int clientID;
	public Point clientPos;
	public Direction clientOrientation;
	public int clientScore;
	
	public ClientInfo(){};
	
	public ClientInfo(String name, String hostname, int port, int ID) {
		this.clientName = name;
		this.clientHostname = hostname;
		this.clientPort = port;
		this.clientID = ID;
	}
	
	public ClientInfo(String name, String hostname, int port, int ID, Point pos, Direction dir, int score) {
		this.clientName = name;
		this.clientHostname = hostname;
		this.clientPort = port;
		this.clientID = ID;
		this.clientPos = pos;
		this.clientOrientation = dir;
		this.clientScore = score;
	}
}