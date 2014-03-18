import java.net.*;
import java.io.*;


// This cla
public class ClientNetworkListener implements Runnable {
	
	private ServerSocket mySocket = null;
    private boolean listening = false;
    
    private final Thread thread;
	
	
	public ClientNetworkListener(ServerSocket mySocket){
		// step 1: try to connect to the first port. If we fail, then we are player1
		// and we setup a server socket there
		// TODO: currently the ports are hardcoded, need to make this dynamic, also add
		// dynamic hostnames
		
		this.mySocket = mySocket;
		
		listening = true;
		// Create our thread
        thread = new Thread(this);	
	}

	@Override
	public void run() {
		// this thread simply listens for new client connections and creates threads for handling them

//		if ((packetFromServer.type == MazewarPacket.CLIENT_REGISTER) && !(packetFromServer.clientName.equals(guiClient.getName())) ) {
//			//System.out.println(packetFromServer.clientName + " " + guiClient.getName());
//			RemoteClient newClient = new RemoteClient(packetFromServer.clientName);
//			maze.addRemoteClient(newClient, packetFromServer.clientPosition, packetFromServer.clientOrientation);
//			clients.add(newClient);
//		}
		
	    while (listening) {
	    	try {
	    		ClientHandlerThread tmp = new ClientHandlerThread(mySocket.accept());
	    		tmp.
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	
	    try {
			mySocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}