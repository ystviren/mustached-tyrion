import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClientManager implements MazeListener, Runnable{
	
	private ServerSocket mySocket = null;
	private List<RemoteClient> remoteClients = null;
	
	/**
	 * The {@link GUIClient} for the game.
	 */
	private GUIClient guiClient = null;
	
	private Maze maze = null;
	private Mazewar mazePanel = null;
	
	private static int player_number;
	private static String player_name;
	
	
	private final String lookupName;
	private final int lookupPort;
	
	private ObjectOutputStream nextClient = null;
	
	private final Thread thread;
	private boolean active = false;
	
	private ClientNetworkListener networkReceiver = null;
	
	private boolean haveToken = false;
	
	public void setVisible( Mazewar mazePanel){
		this.mazePanel = mazePanel;
	}
	
	public ClientManager(Maze maze, String LocalName, String local_hostname, int local_port, String lookup_hostname, int lookup_port) {
		/**
		 * Steps for registration:
		 * 1. connect to the lookup server and register with it
		 * 2. receive all existing remote clients from lookup server 
		 * 3. create the clients locally
		 * 3. insert yourself into the ring after the last client
		 * 4. Receive location and score information from each client
		 * 5. Add remote clients and set up listening comm
		 * 6. Once everything else is updated, add yourself to the board and then send this to other clients 
		 */
		
		this.maze = maze;
		ClientManager.player_name = LocalName;
		lookupName = lookup_hostname;
		lookupPort = lookup_port;
		
/** step 1: existing clients from lookup server **/
		
		// set up connection to the lookup server
		Socket lookupServer = null;
		ObjectInputStream lookupIn = null;
		ObjectOutputStream lookupOut = null;
		try {
			lookupServer = new Socket(lookupName, lookupPort);
			lookupOut = new ObjectOutputStream(lookupServer.getOutputStream());
			lookupIn = new ObjectInputStream(lookupServer.getInputStream());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		// register with the lookup server
		MazewarPacket outPacket = new MazewarPacket();
		outPacket.type = MazewarPacket.CLIENT_REGISTER;
		// TODO: change this to use a packet object
		outPacket.myInfo = new ClientInfo(ClientManager.player_name, local_hostname, local_port, 0);
		
		try {
			lookupOut.writeObject(outPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// get reply from lookup server
		MazewarPacket inPacket = null;
		ArrayList<ClientInfo> remoteInfo = new ArrayList<ClientInfo>();
		try {
			inPacket = (MazewarPacket)lookupIn.readObject();
			// look for our player number
			ClientManager.player_number = inPacket.myInfo.clientID;
			
			guiClient = new GUIClient(LocalName, ClientManager.player_number);
			
			remoteInfo.addAll(inPacket.remoteList);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// TODO: SEND BYE PACKET TO NAME SERVER
		outPacket = new MazewarPacket();
		outPacket.type = MazewarPacket.CLIENT_BYE;
		// TODO: change this to use a packet object
		outPacket.myInfo = new ClientInfo(ClientManager.player_name, local_hostname, local_port, 0);
		try {
			lookupOut.writeObject(outPacket);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// dont need to talk to the lookup server anymore
		try {
			lookupOut.close();
			lookupIn.close();
			lookupServer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// the lookup server should return a list of names corresponding to all the registered clients (including us)
		
		/** Step through the list of remote clients to create RemoteClient objects **/
		// iterate through the list the name server gave us and set up remote clients
		String tmpName = null;
		String tmpHostname = null;
		int tmpPort = 0;
		int tmpID = 0;
		remoteClients = new ArrayList<RemoteClient>();
		
		int i;
		for (i = 0; i < remoteInfo.size(); i++) {
			tmpName = remoteInfo.get(i).clientName;
			tmpHostname = remoteInfo.get(i).clientHostname;
			tmpPort = remoteInfo.get(i).clientPort;
			tmpID = remoteInfo.get(i).clientID;
			
			// No longer creates output socket, preventing a new client from opening connections
			// to all previously existing clients
			RemoteClient tmpClient = new RemoteClient(tmpName, tmpHostname, tmpPort, tmpID);
			
			//guiClient.addClient(tmpClient.getOutStream());
			
			remoteClients.add(tmpClient);
			
			//maze.addClient(tmpClient);
		}

// setup server socket for receiving incoming connections
		try {
			mySocket = new ServerSocket(local_port);
			networkReceiver = new ClientNetworkListener(mySocket, this);
		} catch (IOException e) {
			System.out.println("Failed to create Server Socket");
			e.printStackTrace();
		}		
		
/** step 2: Add yourself to the ring **/		
		
		outPacket = new MazewarPacket();
		outPacket.type = MazewarPacket.CLIENT_REGISTER;
		// TODO: change this to use a packet object
		outPacket.myInfo = new ClientInfo(ClientManager.player_name, local_hostname, local_port, ClientManager.player_number);
		
		if (remoteClients.size() > 0) {
			
			// we add ourselves to the "end" of the ring by notifying the last client to use us
			// as his successor
			remoteClients.get(remoteClients.size()-1).writeObject(outPacket);
			
			// we set our successor to be first client in the list:
			outPacket = new MazewarPacket();
			outPacket.type = MazewarPacket.CLIENT_TEST;
			// TODO: change this to use a packet object
			outPacket.myInfo = new ClientInfo(ClientManager.player_name, local_hostname, local_port, ClientManager.player_number);
			remoteClients.get(0).writeObject(outPacket);
			nextClient = remoteClients.get(0).getOutStream();
		}
		
		// start thread for broadcasting and processing command buffer
		//System.out.println("Do I get here?");
		active = true;
		thread = new Thread(this);
		thread.start();
	}

	public GUIClient getLocalClient() {
		// TODO Auto-generated method stub
		return this.guiClient;
	}
	
	@Override
	public void mazeUpdate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void clientKilled(Client source, Client target) {

	}

	@Override
	public void clientAdded(Client client) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clientFired(Client client) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clientRemoved(Client client) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		// this thread needs to do the following:
		// 1. get the queue and token that was received by one of the remote
		//    client listening threads.
		// 2. process the queue
		// 3. add local buffer contents to the queue
		// 4. send queue and token to the next guy
		
		// need to handle the first receive of queue here for joining properly
		
//		maze.addClient(guiClient);
//		// gui client listens for its own death
//		maze.addMazeListener(guiClient);
		
		// we need to send a packet to the new client with our new info

		System.out.println("I AM ALIVE");
		while (active) {

			try {
				if (nextClient == null){
					System.out.println("no next connection");
					
				}else {
					System.out.println("Sending Packet");
					MazewarPacket test = new MazewarPacket();
					Event event = new Event(player_number, 0, null, null, MazewarPacket.CLIENT_TEST);
					test.type = MazewarPacket.CLIENT_TEST;
					test.eventQueue = new ArrayList<Event>();
					test.eventQueue.add(event);
					test.eventQueue.add(event);
					test.eventQueue.add(event);
					test.clientName = guiClient.getName();
					nextClient.writeObject(test);
				}
			    Thread.sleep(5000);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			} catch(IOException ex){
				System.out.println("Packet Error");
				ex.printStackTrace();
			}
		}
	} // end of run function


	// Extra functions
//	public void updateScore(Client c, int score){
//		MazewarPacket scoreToServer = new MazewarPacket();
//		scoreToServer.clientName = c.getName();
//		scoreToServer.clientScore = score;
//		//scoreToServer.clientPosition = c.getPoint();
//		//scoreToServer.clientOrientation = c.getOrientation();
//		System.out.println("Sending score "+ scoreToServer.clientScore);
//		scoreToServer.type = MazewarPacket.CLIENT_SCORE_SEND;
//		synchronized (out){
//			try {
//				out.writeObject(scoreToServer);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}

	private Client getClient(int pID) {
		// TODO Auto-generated method stub
		Client target = null;
		
		int i;
		// search in remote clients first
		for (i = 0; i < remoteClients.size(); i++) {
			if (remoteClients.get(i).getID() == pID) {
				target = remoteClients.get(i);
				break;
			}
		}
		
		if (target == null) {
			// should be guiClient then
			if (guiClient.getID() == pID) {
				target = guiClient;
			}
		}
		
		return target;
	}

	public void addRemoteClient(Socket newSocket) {
		// TODO Auto-generated method stub
		
		// wait for the client to send us their info
		ObjectInputStream tmpIn = null;
		try {
			tmpIn = new ObjectInputStream(newSocket.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		MazewarPacket packetFromRemote = null;
		try {
			packetFromRemote = (MazewarPacket)tmpIn.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Set up the new remote client object
		String newName = packetFromRemote.myInfo.clientName;
		String newHostname = packetFromRemote.myInfo.clientHostname;
		int newPort = packetFromRemote.myInfo.clientPort;
		int newID = packetFromRemote.myInfo.clientID;
		RemoteClient newClient = remoteClientExists(newID);
		
		if (newClient == null) {
			// use a different constructor here because we don't need to open 
			// a connection to the new client
			newClient = new RemoteClient(newName, newHostname, newPort, newID);
			remoteClients.add(newClient);
		}
		
		if (packetFromRemote.type == MazewarPacket.CLIENT_REGISTER) {
			// the guy that sent me this is now my new next
			nextClient = newClient.getOutStream();
		}
		
		newClient.setInSocket(newSocket, tmpIn);
		// start listening for stuff to throw in the command buffer
		newClient.startThread();
	}

	private RemoteClient remoteClientExists(int newID) {
		// TODO Auto-generated method stub
		int i;
		// search in remote clients first
		for (i = 0; i < remoteClients.size(); i++) {
			if (remoteClients.get(i).getID() == newID) {
				return remoteClients.get(i);
			}
		}
		
		
		return null;
	}
	
}
