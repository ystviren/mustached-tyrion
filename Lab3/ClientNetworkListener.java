import java.net.*;
import java.io.*;


// This cla
public class ClientNetworkListener implements Runnable {
	
	private ServerSocket mySocket = null;
    private boolean listening = false;
    private ClientManager manager = null;
    private final Thread thread;
	
	
	public ClientNetworkListener(ServerSocket mySocket, ClientManager manager){
		// step 1: try to connect to the first port. If we fail, then we are player1
		// and we setup a server socket there
		// TODO: currently the ports are hardcoded, need to make this dynamic, also add
		// dynamic hostnames
		
		this.mySocket = mySocket;
		this.manager = manager;
		listening = true;
		// Create our thread
        thread = new Thread(this);	
        thread.start();
	}

	@Override
	public void run() {
		// this thread simply listens for new client connections and creates threads for handling them
	    while (listening) {
	    	try {
	    		manager.addRemoteClient(mySocket.accept());
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