import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class FileServerHandlerThread extends Thread {

	Socket mySocket = null;
	
	public FileServerHandlerThread(Socket socket) {
		super("FileServerHandlerThread");
		this.mySocket = socket;
		System.out.println("Created new Thread to handle client");
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
				
				if (packetFromClient.type == FileServerPacket.FILE_REQUEST) {
					// handle submission of a new job
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
