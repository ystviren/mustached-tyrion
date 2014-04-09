import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class JobTrackerHandlerThread extends Thread {

	Socket mySocket = null;
	
	public JobTrackerHandlerThread(Socket socket) {
		super("JobTrackerHandlerThread");
		this.mySocket = socket;
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
					// handle submission of a new job
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
