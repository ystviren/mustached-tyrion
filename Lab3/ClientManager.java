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
	
	private final static int player_number;
	private final static String player_name;
	
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
		lookupServer = new Socket(lookupName, lookupPort);
		ObjectInputStream lookupIn = new ObjectInputStream(lookupServer.getInputStream());
		ObjectOutputStream lookupOut = new ObjectOutputStream(lookupServer.getOutputStream());
		
		// register with the lookup server
		MazewarPacket outPacket = new MazewarPacket();
		outPacket.type = MazewarPacket.CLIENT_REGISTER;
		// TODO: change this to use a packet object
		outPacket.clientName = ClientManager.player_name;
		outPacket.clientHostname = local_hostname;
		outPacket.clientPort = local_port;
		
		lookupOut.writeObject(outPacket);
		
		/** step 2 **/
		// get reply from lookup server
		MazewarPacket inPacket = (MazewarPacket)lookupIn.readObject();
		
		// TODO: SEND BYE PACKET TO NAME SERVER
		
		// dont need to talk to the lookup server anymore
		lookupOut.close();
		lookupIn.close();
		lookupServer.close();
		
		// the lookup server should return a list of names corresponding to all the registered clients (including us)
		
		/** Use the list to generate required connections **/
		
		// look for our player number
		player_number = inPacket.clientID;
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
			Socket tmpSocket = mySocket.accept();
			
			// configures input socket, also completes registration by
			// waiting for incoming register packet with client position
			tmpClient.setInSocket(tmpSocket);
			
			MazewarPacket packetFromRemote = (MazewarPacket)tmpIn.readObject();
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
		this.addKeyListener(guiClient);	
		
		// we need to send our position information to everyone
		outPacket = new MazewarPacket();
		outPacket.type = MazewarPacket.CLIENT_REGISTER;
		outPacket.clientName = ClientManager.player_name;
		outPacket.clientPosition = guiClient.getPoint();
		outPacket.clientOrientation = guiClient.getOrientation();
		
		// multicast
		multicastMessage(outPacket);
		
		// lamport clock starts at player number to avoid decimals, gets incremented by 100
		ClientManager.lamportClock = ClientManager.player_number;
		
		
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
		// TODO Auto-generated method stub
		
		//ObjectOutputStream out = null;
		if (target.getName().equals(guiClient.getName()) ) {
			try {
				System.out.println("I died");
	
				MazewarPacket packetToServer = new MazewarPacket();
				packetToServer.type = MazewarPacket.CLIENT_KILLED;
				packetToServer.clientName = target.getName();
				packetToServer.sourceName = source.getName();
				packetToServer.clientPosition = guiClient.getPoint();
				packetToServer.clientOrientation = guiClient.getOrientation();
				System.out.println("reposition: " + packetToServer.clientOrientation.toString());
				
				synchronized(out){
				out.writeObject(packetToServer);
				}
				//updateScore(source, source.getClientScore(source));
				//updateScore(target, target.getClientScore(target));
				
	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
		// 1. get a deliverable commands from the command buffer
		// 2. multicast local actions
		// 3. 
		
		
		
		// int current_players = 0;
		
		while (active) {
			
			
		/** Handle a command from the buffer **/	
			// broadcast a command as long as the buffer is not empty.
			// this assumes that the clients are connected
			synchronized (Client.command_buffer) {

				if (Client.command_buffer.size() > 0) {
					System.out.println(Client.command_buffer.size());
					// get the next command from the buffer
					MazewarPacket broadcast_command = Client.command_buffer.remove(0);

					if ( broadcast_command.type == MazewarPacket.CLIENT_GETCLIENTS ){
						int lastIndex = Client.list_clients.size()-1;
						System.out.println("lastindex: " + lastIndex);
						int j;
						MazewarPacket tmp = null;
						for (j=0;j<lastIndex;j++) {
							tmp = new MazewarPacket();
							tmp.type = MazewarPacket.CLIENT_GETCLIENTS;
							tmp.clientName = Client.clientNames.get(j);
							tmp.clientPosition = Client.clientPositions.get(j);
							tmp.clientOrientation = Client.clientOrientations.get(j);
			
							tmp.clientScore = Client.clientScore.get(j);
							//System.out.println(MazewarServerHandlerThread.clientNames.get(j) + " has positions " + tmp.clientPosition);
							Client.outStreams.get(lastIndex).writeObject(tmp);
						}
						tmp = new MazewarPacket();
						tmp.type = MazewarPacket.PACKET_NULL;
						Client.outStreams.get(lastIndex).writeObject(tmp);
						continue;	
					}else if (broadcast_command.type == MazewarPacket.CLIENT_SCORE_SEND){
						int i;
						for (i = 0; i < Client.clientNames.size(); i++) {
							if (broadcast_command.clientName.equals(Client.clientNames.get(i))) {
								System.out.println("Updated score to " + broadcast_command.clientScore);
								Client.clientScore.set(i, broadcast_command.clientScore);
								break;
							}
						}
						continue;
					}
					
					// broadcast to clients
					int i = 0;
					for (i = 0; i < Client.outStreams.size(); i++) {
						Client.outStreams.get(i).writeObject(broadcast_command);
						System.out.println("Sent Packet " + broadcast_command.type);
					}


				} // end of buffer send
			}
		

		
		/** Handle multicast of locally queued commands **/
		
			try {	
				/* check here that server received? */
							
				
	
					/* print server reply */
					System.out.println("before in read");
					packetFromServer = (MazewarPacket) in.readObject();
					
					System.out.println("after in read" + " " + packetFromServer.type);
					
					Client target = null;
					
					// identify the client targeted for action
					if (packetFromServer.type != MazewarPacket.PACKET_NULL) {
						int i;
						for (i = 0; i < clients.size(); i++) {
							System.out.println(clients.get(i).getName());
							System.out.println(packetFromServer.clientName);
							
							if (clients.get(i).getName().equals(packetFromServer.clientName)) {
								System.out.println("got target");
								target = clients.get(i);
								break;
							}
						}
	
					}
					
					assert(target != null);
					
					// add new clients
					
					else if (packetFromServer.type == MazewarPacket.CLIENT_REMOVE) {
						//targetClient.forward();
						maze.removeClient(target);
						clients.remove(target);
					}
					
					
					if (packetFromServer.type == MazewarPacket.CLIENT_FORWARD) {
						System.out.println("forward");
						target.forward();
					} 
					else if (packetFromServer.type == MazewarPacket.CLIENT_REVERSE) {
						System.out.println("back");
						target.backup();
					}
					else if (packetFromServer.type == MazewarPacket.CLIENT_LEFT) {
						System.out.println("left");
						target.turnLeft();
					}
					else if (packetFromServer.type == MazewarPacket.CLIENT_RIGHT) {
						System.out.println("right");
						target.turnRight();
					}
					
					
					if (packetFromServer.type == MazewarPacket.CLIENT_FIRE) {
						target.fire();
					}
					else if (packetFromServer.type == MazewarPacket.CLIENT_KILLED) {
						System.out.println("processing client death");
						Client source = null;
						//search for source client
						int i;
						for (i = 0; i < clients.size(); i++) {
							if (clients.get(i).getName().equals(packetFromServer.sourceName)) {
								source = clients.get(i);
							}
						}
						
						assert(source != null);
		                assert(target != null);
		                
		                System.out.println(source.getName() + " killed " + target.getName());
		                
		                
		                //if this is being received by someone other than the client that died, need to update position of client that died
		                if (!target.getName().equals(guiClient.getName())) {
		                	Mazewar.consolePrintLn(source.getName() + " just vaporized " + target.getName());
		                	System.out.println("reposition: " + packetFromServer.clientOrientation.toString());
		                	maze.repositionClient(target, packetFromServer.clientPosition, packetFromServer.clientOrientation);
		                	// notify everybody that the kill happened
			                maze.notifyKill(source, target);
		                }else{
		                	updateScore(guiClient, guiClient.getClientScore(guiClient));
		                	updateScore(source, source.getClientScore(source));
		                }	                
		                
					}
					else if (packetFromServer.type == MazewarPacket.CLIENT_SCORE_UPDATE){
						maze.notifyClientFiredPublic(target);
						updateScore(target, target.getClientScore(target));
						System.out.println(target.getClientScore(target));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ERROR: Couldn't get I/O for the connection.");
				System.exit(1);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
	}
}

	// Extra functions
	public void updateScore(Client c, int score){
		MazewarPacket scoreToServer = new MazewarPacket();
		scoreToServer.clientName = c.getName();
		scoreToServer.clientScore = score;
		//scoreToServer.clientPosition = c.getPoint();
		//scoreToServer.clientOrientation = c.getOrientation();
		System.out.println("Sending score "+ scoreToServer.clientScore);
		scoreToServer.type = MazewarPacket.CLIENT_SCORE_SEND;
		synchronized (out){
			try {
				out.writeObject(scoreToServer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void addRemoteClient(String tmpName, String tmpHostname, int tmpPort, int tmpID) {
		// TODO Auto-generated method stub
		
		RemoteClient tmpClient = new RemoteClient(tmpName, tmpHostname, tmpPort, tmpID); 
		remoteClients.add(tmpClient);
		maze.addClient(tmpClient);
		
		// create output sockets for each client and send them a register packet
		tmpClient.writeObject(outPacket);
		
		// wait for client to reply
		Socket tmpSocket = mySocket.accept();
		ObjectInputStream tmpIn = new ObjectInputStream(tmpSocket.getInputStream());
		// configures input socket, also completes registration by
		// waiting for incoming register packet with client position
		tmpClient.setInSocket(tmpSocket);
		
		//wait for client to send us its position and score info
		MazewarPacket packetFromRemote = (MazewarPacket)tmpIn.readObject();
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
	
}