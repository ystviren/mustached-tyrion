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
	
	private static int player_number;
	private static String player_name;
	
	private Socket lookupServer = null;
	private final String lookupName;
	private final int lookupPort;
	
	private ObjectOutputStream nextClient = null;
	
	private final Thread thread;
	private boolean active = false;
	
	private ClientNetworkListener networkReceiver = null;
	
	private boolean haveToken = false;
	
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
		
		/** step 1 **/
		// set up connection to the lookup server
		
		ObjectInputStream lookupIn = null;
		ObjectOutputStream lookupOut = null;
		
		try {
			lookupServer = new Socket(lookupName, lookupPort);
			lookupIn = new ObjectInputStream(lookupServer.getInputStream());
			lookupOut = new ObjectOutputStream(lookupServer.getOutputStream());
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
		
		/** step 2 **/
		// get reply from lookup server
		MazewarPacket inPacket = null;
		try {
			inPacket = (MazewarPacket)lookupIn.readObject();
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
		
		/** Use the list to generate required connections **/
		
		// look for our player number
		ClientManager.player_number = inPacket.myInfo.clientID;
		// TODO: check the contents of the packet
		guiClient = new GUIClient(LocalName, ClientManager.player_number);
		
		/** step 3 **/
		// iterate through the list the name server gave us and set up remote clients
		String tmpName = null;
		String tmpHostname = null;
		int tmpPort = 0;
		int tmpID = 0;
		remoteClients = new ArrayList<RemoteClient>();
		
		outPacket = new MazewarPacket();
		outPacket.type = MazewarPacket.CLIENT_REGISTER;
		// TODO: change this to use a packet object
		outPacket.myInfo = new ClientInfo(ClientManager.player_name, local_hostname, local_port, 0);
		
		int i;
		for (i = 0; i < inPacket.remoteList.size(); i++) {
			tmpName = inPacket.remoteList.get(i).clientName;
			tmpHostname = inPacket.remoteList.get(i).clientHostname;
			tmpPort = inPacket.remoteList.get(i).clientPort;
			tmpID = inPacket.remoteList.get(i).clientID;
			
			// creates new client object with connected output socket/stream
			RemoteClient tmpClient = new RemoteClient(tmpName, tmpHostname, tmpPort, tmpID);
			
			//guiClient.addClient(tmpClient.getOutStream());
			
			remoteClients.add(tmpClient);
			
			//maze.addClient(tmpClient);
		}
		
		if (remoteClients.size() > 0) {
			
			// tell the guy before us to start sending us his stuff
			remoteClients.get(i-1).writeObject(outPacket);
			nextClient = remoteClients.get(0).getOutStream();
		}
		
		// setup server socket for receiving incoming connections
		networkReceiver = new ClientNetworkListener(mySocket, this);
		
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
		MazewarPacket outPacket = new MazewarPacket();
		outPacket.myInfo = new ClientInfo(ClientManager.player_name, null, 0, ClientManager.player_number);
		outPacket.clientPosition = guiClient.getPoint();
		outPacket.clientOrientation = guiClient.getOrientation();
		outPacket.clientScore = guiClient.getClientScore(guiClient);
		newClient.writeObject(outPacket);
		
		while (active) {
			// only do work if we have the token
			if (haveToken == true){
				Event newEvent = null;
				
				// process the stuff in the queue
				int i;
				for(i = 0; i < Client.queue.size();i++){

					// commit actions that are deliverable
					Event newEvent = null;
					synchronized(Client.localQueue){
					if (Client.localQueue.size() > 0 ) {
						newEvent = Client.localQueue.get(0);
					} else {
						continue;
					}
					
					if (true) {
						// now we can remove it from the queue
						Client.localQueue.remove(0);
						
						// parse the contents and commit them
						/** deliver a command **/		
		//				public int action;
		//				public int initTime;
		//				public int timeDeliver;
		//				public boolean deliverable;
		//				public int pID;
		//				public int count;
							
						//identify the client targeted for action
						Client target = getClient(newEvent.source);
						
						assert(target != null);
						
						if (newEvent.action == MazewarPacket.CLIENT_REMOVE) {
		//					maze.removeClient(target);
		//					clients.remove(target);
						}
									
						if (newEvent.action == MazewarPacket.CLIENT_FORWARD) {
							//System.out.println("forward");
							target.forward();
						} 
						else if (newEvent.action == MazewarPacket.CLIENT_REVERSE) {
							//System.out.println("back");
							target.backup();
						}
						else if (newEvent.action == MazewarPacket.CLIENT_LEFT) {
							//System.out.println("left");
							target.turnLeft();
						}
						else if (newEvent.action == MazewarPacket.CLIENT_RIGHT) {
							//System.out.println("right");
							target.turnRight();
						}
						
							newEvent = Client.queue.get(i);
							// process queue			
											
							// parse the contents and commit them
							/** deliver a command **/		
			//				public int action;
			//				public int initTime;
			//				public int timeDeliver;
			//				public boolean deliverable;
			//				public int pID;
			//				public int count;
								
							//identify the client targeted for action
							Client target = getClient(newEvent.pID);
							
							assert(target != null);
							
							if (newEvent.action == MazewarPacket.CLIENT_REMOVE) {
			//					maze.removeClient(target);
			//					clients.remove(target);
							}
										
							if (newEvent.action == MazewarPacket.CLIENT_FORWARD) {
								//System.out.println("forward");
								target.forward();
							} 
							else if (newEvent.action == MazewarPacket.CLIENT_REVERSE) {
								//System.out.println("back");
								target.backup();
							}
							else if (newEvent.action == MazewarPacket.CLIENT_LEFT) {
								//System.out.println("left");
								target.turnLeft();
							}
							else if (newEvent.action == MazewarPacket.CLIENT_RIGHT) {
								//System.out.println("right");
								target.turnRight();
							}
							
							if (newEvent.action == MazewarPacket.CLIENT_FIRE) {
								//target.fire();
							}
							else if (newEvent.action == MazewarPacket.CLIENT_KILLED) {
								//System.out.println("processing client death");
								Client source = getClient(newEvent.pID2);
								
								assert(source != null);
			
				                //if this is being received by someone other than the client that died, need to update position of client that died
				                if (!target.getName().equals(guiClient.getName())) {
				                	Mazewar.consolePrintLn(source.getName() + " just vaporized " + target.getName());
				                	//System.out.println("reposition: " + packetFromServer.clientOrientation.toString());
				                	maze.repositionClient(target, newEvent.location, newEvent.orientation);
				                	// notify everybody that the kill happened
					                maze.notifyKill(source, target);
				                }
			//	                else{
			//	                	updateScore(guiClient, guiClient.getClientScore(guiClient));
			//	                	updateScore(source, source.getClientScore(source));
			//	                }	                
				                
							}
			//				else if (newEvent.action == MazewarPacket.CLIENT_SCORE_UPDATE){
			//					maze.notifyClientFiredPublic(target);
			//					updateScore(target, target.getClientScore(target));
			//					System.out.println(target.getClientScore(target));
			//				}
			
						}
					} //if haveToken
				}
			} 
		}// end of while loop
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
		RemoteClient newClient = null;
		
		if (!remoteClientExists(newID)) {
			// use a different constructor here because we don't need to open 
			// a connection to the new client
			newClient = new RemoteClient(newName, newID);
			newClient.setInSocket(newSocket, tmpIn);
			remoteClients.add(newClient);
		}
		else {
			newClient = (RemoteClient)getClient(newID);
			newClient.setInSocket(newSocket, tmpIn);
		}
		
		if (packet type == register) {
			// the guy that sent me this is now my new next
			nextClient = newClient.getOutStream();
		}
		
		// start listening for stuff to throw in the command buffer
		newClient.startThread();
	}

	private boolean remoteClientExists(int newID) {
		// TODO Auto-generated method stub
		int i;
		// search in remote clients first
		for (i = 0; i < remoteClients.size(); i++) {
			if (remoteClients.get(i).getID() == newID) {
				return true;
			}
		}
		
		
		return false;
	}
	
}
