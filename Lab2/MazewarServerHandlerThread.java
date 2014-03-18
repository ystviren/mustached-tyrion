import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class MazewarServerHandlerThread extends Thread {
	// socket for individual client objects
	private Socket socket = null;

	private boolean is_broadcaster = false;

	private static int maze_seed = 0;

	private static List<Socket> list_clients = null;
	private static List<MazewarPacket> command_buffer = null;
	private static List<ObjectOutputStream> outStreams = null;
	private static List<Point> clientPositions = null;
	private static List<Direction> clientOrientations = null;
	private static List<String> clientNames = null;
	private static List<Integer> clientScore = null;

	public MazewarServerHandlerThread(Socket socket, int seed) throws IOException {
		super("MazewarServerHandlerThread");

		this.socket = socket;
		MazewarServerHandlerThread.maze_seed = seed;

		System.out.println("Created new Thread to handle client");

		if (MazewarServerHandlerThread.list_clients == null) {
			MazewarServerHandlerThread.list_clients = new ArrayList<Socket>();
			MazewarServerHandlerThread.clientPositions = new ArrayList<Point>();
			MazewarServerHandlerThread.clientOrientations = new ArrayList<Direction>();
			MazewarServerHandlerThread.clientNames = new ArrayList<String>();
			MazewarServerHandlerThread.clientScore = new ArrayList<Integer>();
		}

		if (MazewarServerHandlerThread.command_buffer == null) {
			MazewarServerHandlerThread.command_buffer = new ArrayList<MazewarPacket>();
		}

		if (MazewarServerHandlerThread.outStreams == null) {
			MazewarServerHandlerThread.outStreams = new ArrayList<ObjectOutputStream>();
		}

		MazewarServerHandlerThread.list_clients.add(socket);

		MazewarServerHandlerThread.outStreams.add(new ObjectOutputStream(socket.getOutputStream()));
		MazewarServerHandlerThread.clientScore.add(0);
	}

	public MazewarServerHandlerThread(int seed) {
		super("MazewarServerHandlerThread");

		this.is_broadcaster = true;

		System.out.println("Created broadcast thread.");

		MazewarServerHandlerThread.maze_seed = seed;

		if (MazewarServerHandlerThread.list_clients == null) {
			MazewarServerHandlerThread.list_clients = new ArrayList<Socket>();
			MazewarServerHandlerThread.clientPositions = new ArrayList<Point>();
			MazewarServerHandlerThread.clientOrientations = new ArrayList<Direction>();
			MazewarServerHandlerThread.clientNames = new ArrayList<String>();
			MazewarServerHandlerThread.clientScore = new ArrayList<Integer>();
		}

		if (MazewarServerHandlerThread.command_buffer == null) {
			MazewarServerHandlerThread.command_buffer = new ArrayList<MazewarPacket>();
		}

		if (MazewarServerHandlerThread.outStreams == null) {
			MazewarServerHandlerThread.outStreams = new ArrayList<ObjectOutputStream>();
		}

	}

	public void run() {

		boolean gotByePacket = false;

		try {

			if (is_broadcaster) {
				while (true) {

					// broadcast a command as long as the buffer is not empty.
					// this assumes that the clients are connected
					synchronized (MazewarServerHandlerThread.command_buffer) {

						if (MazewarServerHandlerThread.command_buffer.size() > 0) {
							// get the next command from the buffer
							MazewarPacket broadcast_command = MazewarServerHandlerThread.command_buffer.remove(0);

							if (broadcast_command.type == MazewarPacket.CLIENT_GETCLIENTS) {
								int lastIndex = MazewarServerHandlerThread.list_clients.size() - 1;
								int j;
								MazewarPacket tmp = null;
								for (j = 0; j < lastIndex; j++) {
									tmp = new MazewarPacket();
									tmp.type = MazewarPacket.CLIENT_GETCLIENTS;
									tmp.clientName = MazewarServerHandlerThread.clientNames.get(j);
									tmp.clientPosition = MazewarServerHandlerThread.clientPositions.get(j);
									tmp.clientOrientation = MazewarServerHandlerThread.clientOrientations.get(j);
									tmp.mapSeed = MazewarServerHandlerThread.maze_seed;
									tmp.clientScore = MazewarServerHandlerThread.clientScore.get(j);
									MazewarServerHandlerThread.outStreams.get(lastIndex).writeObject(tmp);
								}
								tmp = new MazewarPacket();
								tmp.type = MazewarPacket.PACKET_NULL;
								tmp.mapSeed = MazewarServerHandlerThread.maze_seed;
								MazewarServerHandlerThread.outStreams.get(lastIndex).writeObject(tmp);
								continue;
							} else if (broadcast_command.type == MazewarPacket.CLIENT_SCORE_SEND) {
								int i;
								for (i = 0; i < MazewarServerHandlerThread.clientNames.size(); i++) {
									if (broadcast_command.clientName.equals(MazewarServerHandlerThread.clientNames.get(i))) {
										MazewarServerHandlerThread.clientScore.set(i, broadcast_command.clientScore);
										break;
									}
								}
								continue;
							}

							// broadcast to clients
							int saveIndex = 0;
							int i = 0;
							for (i = 0; i < MazewarServerHandlerThread.clientNames.size(); i++) {
								if (MazewarServerHandlerThread.clientNames.get(i).equals(broadcast_command.clientName)) {
									saveIndex = i;
								}
								MazewarServerHandlerThread.outStreams.get(i).writeObject(broadcast_command);
							}

							if (broadcast_command.type == MazewarPacket.CLIENT_BYE) {
								// remove all the objects associated with the client
								MazewarServerHandlerThread.outStreams.get(saveIndex).close();
								MazewarServerHandlerThread.list_clients.remove(saveIndex);
								MazewarServerHandlerThread.outStreams.remove(saveIndex);
								MazewarServerHandlerThread.clientPositions.remove(saveIndex);
								MazewarServerHandlerThread.clientOrientations.remove(saveIndex);
								MazewarServerHandlerThread.clientNames.remove(saveIndex);
								MazewarServerHandlerThread.clientScore.remove(saveIndex);
							}

						} // end of buffer send
					}
				} // end of loop

			} // end of broadcaster code
				// client threads run this
			else {
				/* stream to read from client */
				ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
				MazewarPacket packetFromClient = null;

				while ((packetFromClient = (MazewarPacket) fromClient.readObject()) != null) {
					/* process message */
					if (packetFromClient.type == MazewarPacket.CLIENT_GETCLIENTS) {
						MazewarServerHandlerThread.clientNames.add(packetFromClient.clientName);
					}

					else if (packetFromClient.type == MazewarPacket.CLIENT_REGISTER) {
						MazewarServerHandlerThread.clientPositions.add(packetFromClient.clientPosition);
						MazewarServerHandlerThread.clientOrientations.add(packetFromClient.clientOrientation);
					}		

					else if ((packetFromClient.type != MazewarPacket.CLIENT_SCORE_SEND) && (packetFromClient.type != MazewarPacket.CLIENT_SCORE_UPDATE)) {
						// update your position to the server
						// need to find yourself in the list
						int i;
						for (i = 0; i < MazewarServerHandlerThread.list_clients.size(); i++) {
							if (MazewarServerHandlerThread.list_clients.get(i).equals(socket)) {

								MazewarServerHandlerThread.clientPositions.set(i, packetFromClient.clientPosition);
								MazewarServerHandlerThread.clientOrientations.set(i, packetFromClient.clientOrientation);
								break;
								
							}
						}

					}
					synchronized (MazewarServerHandlerThread.command_buffer) {
						MazewarServerHandlerThread.command_buffer.add(packetFromClient);
					}
					/* quit case */
					if (packetFromClient.type == MazewarPacket.PACKET_NULL || packetFromClient.type == MazewarPacket.CLIENT_BYE) {
						gotByePacket = true;
					}
				}

				/* cleanup when client exits */
				fromClient.close();
				socket.close();
			} // end of client code

		} catch (IOException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if (!gotByePacket)
				e.printStackTrace();
		}
	}
}
