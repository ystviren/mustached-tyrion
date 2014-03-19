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
	//private List<Socket> clientSockets = null;
	private List<RemoteClient> remoteClients = null;
	//private int supported_ports[] = {4444,4445,4446,4447};

	private static List<ObjectInputStream> in_streams = null;
	private static List<ObjectOutputStream> out_streams = null;
	
	/**
	 * The {@link GUIClient} for the game.
	 */
	private GUIClient guiClient = null;
	
	private List<Client> list_clients = null;
	
	private Maze maze = null;
	
	private static int player_number;
	private static String player_name;
	
	private Socket lookupServer = null;
	private final String lookupName;
	private final int lookupPort;
	
	private final Thread thread;
	private boolean active = false;
	
	private static List<MazewarPacket> command_buffer = null;
	private static int lamportClock = 0;
	
	private ClientNetworkListener networkReceiver = null;
	
	public ClientManager(Maze maze, String LocalName, String local_hostname, int local_port, String lookup_hostname, int lookup_port) {
		/**
		 * Steps for registration:
		 * 1. connect to the lookup server and register with it
		 * 2. receive all existing clients from lookup server (including yourself)
		 * 3. Open communication with each client and send them a register packet
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
		player_number = inPacket.myInfo.clientID;
		// TODO: check the contents of the packet
		
		/** step 3 **/
		// iterate through the list the name server gave us and set up remote clients
		String tmpName = null;
		String tmpHostname = null;
		int tmpPort = 0;
		int tmpID = 0;
		
		int i;
		for (i = 0; i < inPacket.remoteList.size(); i++) {
			tmpName = inPacket.remoteList.get(i).clientName;
			tmpHostname = inPacket.remoteList.get(i).clientHostname;
			tmpPort = inPacket.remoteList.get(i).clientPort;
			tmpID = inPacket.remoteList.get(i).clientID;
			
			RemoteClient tmpClient = new RemoteClient(tmpName, tmpHostname, tmpPort, tmpID); 
			remoteClients.add(tmpClient);
			maze.addClient(tmpClient);
			
			// create output sockets for each client and send them a register packet
			tmpClient.writeObject(outPacket);
			
			// wait for client to reply
			Socket tmpSocket = null;
			try {
				tmpSocket = mySocket.accept();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// configures input socket, also completes registration by
			// waiting for incoming register packet with client position
			tmpClient.setInSocket(tmpSocket, null);
			
			MazewarPacket packetFromRemote = tmpClient.readObject();
			// extract position, direction, score
			Point tmpPos = packetFromRemote.clientPosition;
			Direction tmpDir = packetFromRemote.clientOrientation;
			int tmpScore = packetFromRemote.clientScore;
			
			maze.repositionClient(tmpClient, tmpPos, tmpDir);
			// update the score
			tmpClient.clientSetScore(tmpClient, tmpScore);
			
			// start listening for stuff to throw in the command buffer
			tmpClient.startThread();
		}
		
		// at this point we should have all the remote clients placed and scores up to date
		// now we can place ourselves on the map
		
		guiClient = new GUIClient(LocalName);
		maze.addClient(guiClient);
		// gui client listens for its own death
		maze.addMazeListener(guiClient);
		
		// we need to send our position information to everyone
		outPacket = new MazewarPacket();
		outPacket.type = MazewarPacket.CLIENT_REGISTER;
		outPacket.myInfo = new ClientInfo(ClientManager.player_name, null, 0, ClientManager.player_number);
		outPacket.clientPosition = guiClient.getPoint();
		outPacket.clientOrientation = guiClient.getOrientation();
		
		// multicast
		multicastMessage(outPacket);
		
		// setup server socket for receiving incoming connections
		networkReceiver = new ClientNetworkListener(mySocket, this);		
		
		// start thread for broadcasting and processing command buffer
		active = true;
		thread = new Thread(this);
		thread.start();
	}
	
	private void multicastMessage(MazewarPacket outPacket) {
		// iterate throught the list of remote clients and send packet to all of them
		int i;
		for (i = 0; i < remoteClients.size(); i++) {
			remoteClients.get(i).writeObject(outPacket);
		}
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
		// get a deliverable commands from the command buffer and commit them 
		
		while (active) {
			// commit actions that are deliverable
			Event newEvent = null;
			if (Client.queue.size() > 0 ) {
				newEvent = Client.queue.get(0);
			} else {
				continue;
			}
			
			if (newEvent.deliverable) {
				// now we can remove it from the queue
				Client.queue.remove(0);
				
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
		} // end of while loop
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
		ObjectInputStream tmpIn = null;;
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
		
		RemoteClient newClient = new RemoteClient(newName, newHostname, newPort, newID);
		newClient.setInSocket(newSocket, tmpIn);
		remoteClients.add(newClient);
		maze.addClient(newClient);
		
		// we need to send a packet to the new client with our new info
		MazewarPacket outPacket = new MazewarPacket();
		outPacket.myInfo = new ClientInfo(ClientManager.player_name, null, 0, ClientManager.player_number);
		outPacket.clientPosition = guiClient.getPoint();
		outPacket.clientOrientation = guiClient.getOrientation();
		outPacket.clientScore = guiClient.getClientScore(guiClient);
		newClient.writeObject(outPacket);
		
		//wait for client to send us its position and score info
		try {
			packetFromRemote = (MazewarPacket)tmpIn.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// extract position, direction, score
		Point tmpPos = packetFromRemote.clientPosition;
		Direction tmpDir = packetFromRemote.clientOrientation;
		int tmpScore = packetFromRemote.clientScore;
		
		maze.repositionClient(newClient, tmpPos, tmpDir);
		// update the score
		newClient.clientSetScore(newClient, tmpScore);
		
		// start listening for stuff to throw in the command buffer
		newClient.startThread();
	}
	
}
	
	/** Handle a command from the buffer **/	
//	// broadcast a command as long as the buffer is not empty.
//	// this assumes that the clients are connected
//	synchronized (Client.command_buffer) {
//
//		if (Client.command_buffer.size() > 0) {
//			System.out.println(Client.command_buffer.size());
//			// get the next command from the buffer
//			MazewarPacket broadcast_command = Client.command_buffer.remove(0);
//
//			if ( broadcast_command.type == MazewarPacket.CLIENT_GETCLIENTS ){
//				int lastIndex = Client.list_clients.size()-1;
//				System.out.println("lastindex: " + lastIndex);
//				int j;
//				MazewarPacket tmp = null;
//				for (j=0;j<lastIndex;j++) {
//					tmp = new MazewarPacket();
//					tmp.type = MazewarPacket.CLIENT_GETCLIENTS;
//					tmp.clientName = Client.clientNames.get(j);
//					tmp.clientPosition = Client.clientPositions.get(j);
//					tmp.clientOrientation = Client.clientOrientations.get(j);
//	
//					tmp.clientScore = Client.clientScore.get(j);
//					//System.out.println(MazewarServerHandlerThread.clientNames.get(j) + " has positions " + tmp.clientPosition);
//					Client.outStreams.get(lastIndex).writeObject(tmp);
//				}
//				tmp = new MazewarPacket();
//				tmp.type = MazewarPacket.PACKET_NULL;
//				Client.outStreams.get(lastIndex).writeObject(tmp);
//				continue;	
//			}else if (broadcast_command.type == MazewarPacket.CLIENT_SCORE_SEND){
//				int i;
//				for (i = 0; i < Client.clientNames.size(); i++) {
//					if (broadcast_command.clientName.equals(Client.clientNames.get(i))) {
//						System.out.println("Updated score to " + broadcast_command.clientScore);
//						Client.clientScore.set(i, broadcast_command.clientScore);
//						break;
//					}
//				}
//				continue;
//			}
//			
//			// broadcast to clients
//			int i = 0;
//			for (i = 0; i < Client.outStreams.size(); i++) {
//				Client.outStreams.get(i).writeObject(broadcast_command);
//				System.out.println("Sent Packet " + broadcast_command.type);
//			}
//
//
//		} // end of buffer send
//	}
//
//