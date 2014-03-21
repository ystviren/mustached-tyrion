import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class MazewarNameServerHandlerThread extends Thread {
	private Socket socket = null;
	
	public static ArrayList<ClientInfo> connectedClients = new ArrayList<ClientInfo>();
	private static int pID = 1;

	public MazewarNameServerHandlerThread(Socket socket) {
		super("MazewarNameServerHandlerThread");
		this.socket = socket;
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
				

				
				/* create a packet to send reply back to client */
				packetToClient = new MazewarPacket();
				packetToClient.type = MazewarPacket.NAME_SERVER_REPLY;
				
				if (packetFromClient.type == MazewarPacket.CLIENT_BYE) {
					gotByePacket = true;
					break;
				}	
				
				if (packetFromClient.type == MazewarPacket.CLIENT_REGISTER) {
					synchronized (MazewarNameServerHandlerThread.connectedClients){
						// Assert that the sender, sends it information
						assert(packetFromClient.myInfo != null);
						packetToClient.remoteList = new ArrayList<ClientInfo>();
						packetToClient.remoteList.addAll(MazewarNameServerHandlerThread.connectedClients);
						packetToClient.myInfo = new ClientInfo(packetFromClient.myInfo.clientName, packetFromClient.myInfo.clientHostname, packetFromClient.myInfo.clientPort, pID);
						MazewarNameServerHandlerThread.connectedClients.add(packetToClient.myInfo);
						System.out.println("Sent Packet, size of list is " + MazewarNameServerHandlerThread.connectedClients.size());
						System.out.println("Size of sent packet is " + packetToClient.remoteList.size());
						pID++;
						toClient.writeObject(packetToClient);
					}
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