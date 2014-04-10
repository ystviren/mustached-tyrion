import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class FileServerHandlerThread extends Thread {

	Socket mySocket = null;
	 ArrayList<ArrayList<String>> wordList = null;
	
	public FileServerHandlerThread(Socket socket,  ArrayList<ArrayList<String>> wordList) {
		super("FileServerHandlerThread");
		this.mySocket = socket;
		System.out.println("Created new Thread to handle client");
		this.wordList = new ArrayList<ArrayList<String>>(wordList);
	}

	// Handle connections from client drivers
	public void run() {

		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(mySocket.getInputStream());
			FileServerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(mySocket.getOutputStream());
			
			while (( packetFromClient = (FileServerPacket) fromClient.readObject()) != null) {
				System.out.println("Received packet");
				if (packetFromClient.type == FileServerPacket.FILE_REQUEST) {
					FileServerPacket packetToClient = new FileServerPacket();
					packetToClient.type = FileServerPacket.REPLY_REQUEST;
					packetToClient.words = new ArrayList<String>(wordList.get(packetFromClient.partition));
					toClient.writeObject(packetToClient);
					break;
				}
			}
			
			fromClient.close();
			toClient.close();
			mySocket.close();
			
		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}
}
