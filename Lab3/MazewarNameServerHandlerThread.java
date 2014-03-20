import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class MazewarNameServerHandlerThread extends Thread {
	private Socket socket = null;
	
	List<ClientInfo> connectedClients;
	private int pID;

	public MazewarNameServerHandlerThread(Socket socket) {
		super("BrokerServerHandlerThread");
		connectedClients = new ArrayList<ClientInfo>();
		this.socket = socket;
		pID = 1;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {

		boolean gotByePacket = false;
		try {
			
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			MazewarPacket packetFromClient;

			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			MazewarPacket packetToClient;

			while ((packetFromClient = (MazewarPacket) fromClient.readObject()) != null) {
				
				// Assert that the sender, sends it information
				assert(packetFromClient.myInfo != null);
				
				/* create a packet to send reply back to client */
				packetToClient = new MazewarPacket();
				packetToClient.type = MazewarPacket.NAME_SERVER_REPLY;
				
				if (packetFromClient.type == MazewarPacket.PACKET_NULL || packetFromClient.type == MazewarPacket.CLIENT_BYE) {
					gotByePacket = true;
					packetToClient.type = MazewarPacket.NAME_SERVER_BYE;
					toClient.writeObject(packetToClient);
					break;
				}	
				
				if (packetFromClient.type == MazewarPacket.CLIENT_LOOKUP_REGISTER) {
					packetToClient.remoteList = new ArrayList<ClientInfo>(connectedClients);
					packetToClient.myInfo = new ClientInfo(packetFromClient.myInfo.clientName, packetFromClient.myInfo.clientHostname, packetFromClient.myInfo.clientPort, pID);
					connectedClients.add(packetToClient.myInfo);
					pID++;
					toClient.writeObject(packetToClient);
					break;
				}
			}

			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();
		} catch (EOFException e){
			System.out.println("EOF reached connection closed");
		} catch (IOException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if (!gotByePacket)
				e.printStackTrace();
		}
	}
}