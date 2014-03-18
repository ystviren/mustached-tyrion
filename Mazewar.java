/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
 */

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JOptionPane;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.BorderFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The entry point and glue code for the game. It also contains some helpful
 * global utility methods.
 * 
 * @author Geoffrey Washburn &lt;<a
 *         href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame implements Runnable, MazeListener {

	/**
	 * The default width of the {@link Maze}.
	 */
	private final int mazeWidth = 20;

	/**
	 * The default height of the {@link Maze}.
	 */
	private final int mazeHeight = 10;

	/**
	 * The default random seed for the {@link Maze}. All implementations of the
	 * same protocol must use the same seed value, or your mazes will be
	 * different.
	 */
	private final int mazeSeed = 42;

	private final Thread thread;

	private Socket serverConnection = null;
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private List<Client> clients = null;

	/**
	 * The {@link Maze} that the game uses.
	 */
	private Maze maze = null;

	private boolean active = false;
	/**
	 * The {@link GUIClient} for the game.
	 */
	private GUIClient guiClient = null;

	private String LocalName = null;

	/**
	 * The panel that displays the {@link Maze}.
	 */
	private OverheadMazePanel overheadPanel = null;

	/**
	 * The table the displays the scores.
	 */
	private JTable scoreTable = null;

	/**
	 * Create the textpane statically so that we can write to it globally using
	 * the static consolePrint methods
	 */
	private static final JTextPane console = new JTextPane();

	/**
	 * Write a message to the console followed by a newline.
	 * 
	 * @param msg
	 *            The {@link String} to print.
	 */
	public static synchronized void consolePrintLn(String msg) {
		console.setText(console.getText() + msg + "\n");
	}

	/**
	 * Write a message to the console.
	 * 
	 * @param msg
	 *            The {@link String} to print.
	 */
	public static synchronized void consolePrint(String msg) {
		console.setText(console.getText() + msg);
	}

	/**
	 * Clear the console.
	 */
	public static synchronized void clearConsole() {
		console.setText("");
	}

	/**
	 * Static method for performing cleanup before exiting the game.
	 */
	public static void quit() {
		// Put any network clean-up code you might have here.
		// (inform other implementations on the network that you have
		// left, etc.)
		// active = false;

		System.exit(0);
	}

	/**
	 * The place where all the pieces are put together.
	 */
	public Mazewar() {
		super("ECE419 Mazewar");
		consolePrintLn("ECE419 Mazewar started!");

		// Throw up a dialog to get the GUIClient name.
		LocalName = JOptionPane.showInputDialog("Enter your name");
		if ((LocalName == null) || (LocalName.length() == 0)) {
			Mazewar.quit();
		}

		String hostname = JOptionPane.showInputDialog("Enter server location");
		if ((hostname == null) || (hostname.length() == 0)) {
			Mazewar.quit();
		}

		String port = JOptionPane.showInputDialog("Enter port number");
		if ((port == null) || (port.length() == 0)) {
			Mazewar.quit();
		}

		// You may want to put your network initialization code somewhere in
		// here.

		// connect to server.
		try {
			/* variables for hostname/port */
			// default values
			serverConnection = new Socket(hostname, Integer.parseInt(port));
			out = new ObjectOutputStream(serverConnection.getOutputStream());
			in = new ObjectInputStream(serverConnection.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		clients = new ArrayList<Client>();
		guiClient = new GUIClient(LocalName, out);
		clients.add(guiClient);

		MazewarPacket packetFromServer = null;
		ScoreTableModel scoreModel = null;

		try {

			// first task is to register to the server
			MazewarPacket packetToServer = new MazewarPacket();
			packetToServer.type = MazewarPacket.CLIENT_GETCLIENTS;
			packetToServer.clientName = guiClient.getName();

			out.writeObject(packetToServer);

			packetFromServer = (MazewarPacket) in.readObject();
			// Create the maze
			maze = new MazeImpl(new Point(mazeWidth, mazeHeight), packetFromServer.mapSeed);
			assert (maze != null);

			// Have the ScoreTableModel listen to the maze to find
			// out how to adjust scores.
			scoreModel = new ScoreTableModel();
			assert (scoreModel != null);
			maze.addMazeListener(scoreModel);

			// expect to receive existing clients one by one until null
			// Method used to receive position, direction, and score of also
			// existing clients
			while (packetFromServer.type == MazewarPacket.CLIENT_GETCLIENTS) {
				/* print server reply */
				RemoteClient newClient = new RemoteClient(packetFromServer.clientName);
				maze.addRemoteClient(newClient, packetFromServer.clientPosition, packetFromServer.clientOrientation);
				clients.add(newClient);
				newClient.clientSetScore(newClient, packetFromServer.clientScore);
				packetFromServer = (MazewarPacket) in.readObject();
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

		// cant start properly without having this initialized here
		maze.addClient(guiClient);
		this.addKeyListener(guiClient);

		// Create the panel that will display the maze.
		overheadPanel = new OverheadMazePanel(maze, guiClient);
		assert (overheadPanel != null);
		maze.addMazeListener(overheadPanel);

		// Don't allow editing the console from the GUI
		console.setEditable(false);
		console.setFocusable(false);
		console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

		// Allow the console to scroll by putting it in a scrollpane
		JScrollPane consoleScrollPane = new JScrollPane(console);
		assert (consoleScrollPane != null);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));

		// Create the score table
		scoreTable = new JTable(scoreModel);
		assert (scoreTable != null);
		scoreTable.setFocusable(false);
		scoreTable.setRowSelectionAllowed(false);

		// Allow the score table to scroll too.
		JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
		assert (scoreScrollPane != null);
		scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));

		// Create the layout manager
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		getContentPane().setLayout(layout);

		// Define the constraints on the components.
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 3.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(overheadPanel, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 2.0;
		c.weighty = 1.0;
		layout.setConstraints(consoleScrollPane, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		layout.setConstraints(scoreScrollPane, c);

		// Add the components
		getContentPane().add(overheadPanel);
		getContentPane().add(consoleScrollPane);
		getContentPane().add(scoreScrollPane);

		// Pack everything neatly.
		pack();

		// Let the magic begin.
		setVisible(true);
		overheadPanel.repaint();
		this.requestFocusInWindow();

		// listen for changes in mazelistener
		maze.addMazeListener(this);

		// start client manager thread
		active = true;
		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Entry point for the game.
	 * 
	 * @param args
	 *            Command-line arguments.
	 */
	public static void main(String args[]) {
		/* Create the GUI */
		new Mazewar();
	}

	public void run() {
		consolePrintLn("client manager is running");

		// create input stream

		try {

			MazewarPacket packetFromServer = null;

			// first task is to register to the server
			MazewarPacket packetToServer = new MazewarPacket();
			packetToServer.type = MazewarPacket.CLIENT_REGISTER;
			packetToServer.clientName = guiClient.getName();
			packetToServer.clientPosition = guiClient.getPoint();
			packetToServer.clientOrientation = guiClient.getOrientation();

			out.writeObject(packetToServer);

			while (true) {
				packetFromServer = (MazewarPacket) in.readObject();

				Client target = null;

				// identify the client targeted for action
				if (packetFromServer.type != MazewarPacket.PACKET_NULL) {
					int i;
					for (i = 0; i < clients.size(); i++) {

						if (clients.get(i).getName().equals(packetFromServer.clientName)) {
							target = clients.get(i);
							break;
						}
					}

				}

				assert (target != null);

				// add new clients as they register and join
				if ((packetFromServer.type == MazewarPacket.CLIENT_REGISTER) && !(packetFromServer.clientName.equals(guiClient.getName()))) {
					RemoteClient newClient = new RemoteClient(packetFromServer.clientName);
					maze.addRemoteClient(newClient, packetFromServer.clientPosition, packetFromServer.clientOrientation);
					clients.add(newClient);
				} else if (packetFromServer.type == MazewarPacket.CLIENT_BYE) {
					if (packetFromServer.clientName.equals(guiClient.getName())) {
						active = false;
						out.close();
						in.close();
						serverConnection.close();
						System.out.println("Client is exiting.");
						quit();
					} else {
						maze.removeClient(target);
						clients.remove(target);
					}
				}

				if (packetFromServer.type == MazewarPacket.CLIENT_FORWARD) {
					target.forward();
				} else if (packetFromServer.type == MazewarPacket.CLIENT_REVERSE) {
					target.backup();
				} else if (packetFromServer.type == MazewarPacket.CLIENT_LEFT) {
					target.turnLeft();
					if (!target.getOrientation().toString().equals(packetFromServer.clientOrientation.toString())) {
						maze.repositionClient(target, target.getPoint(), packetFromServer.clientOrientation);
					}
				} else if (packetFromServer.type == MazewarPacket.CLIENT_RIGHT) {
					target.turnRight();

					if (!target.getOrientation().toString().equals(packetFromServer.clientOrientation.toString())) {
						maze.repositionClient(target, target.getPoint(), packetFromServer.clientOrientation);
					}
				}

				if (packetFromServer.type == MazewarPacket.CLIENT_FIRE) {
					target.fire();
				} else if (packetFromServer.type == MazewarPacket.CLIENT_KILLED) {
					Client source = null;
					// search for source client
					int i;
					for (i = 0; i < clients.size(); i++) {
						if (clients.get(i).getName().equals(packetFromServer.sourceName)) {
							source = clients.get(i);
						}
					}

					assert (source != null);
					assert (target != null);

					// if this is being received by someone other than the
					// client that died, need to update position of client that
					// died
					if (!target.getName().equals(guiClient.getName())) {
						
						Mazewar.consolePrintLn(source.getName() + " just vaporized " + target.getName());
						boolean needtoupdate = maze.repositionClient(target, packetFromServer.clientPosition, packetFromServer.clientOrientation);
						// notify everybody that the kill happened
						if (needtoupdate) {
							packetToServer = new MazewarPacket();
							packetToServer.type = MazewarPacket.CLIENT_UPDATEPOS;
							packetToServer.clientName = target.getName();
							packetToServer.clientPosition = target.getPoint();
							packetToServer.clientOrientation = target.getOrientation();
							out.writeObject(packetToServer);
						}
						maze.notifyKill(source, target);
					} else {
						updateScore(guiClient, guiClient.getClientScore(guiClient));
						updateScore(source, source.getClientScore(source));
					}

				} else if (packetFromServer.type == MazewarPacket.CLIENT_SCORE_UPDATE) {
					maze.notifyClientFiredPublic(target);
					updateScore(target, target.getClientScore(target));
				} else if (packetFromServer.type == MazewarPacket.CLIENT_UPDATEPOS) {
					boolean needtoupdate = maze.repositionClient(target, packetFromServer.clientPosition, packetFromServer.clientOrientation);
					// notify everybody that the kill happened
					if (needtoupdate) {
						packetToServer = new MazewarPacket();
						packetToServer.type = MazewarPacket.CLIENT_UPDATEPOS;
						packetToServer.clientName = target.getName();
						packetToServer.clientPosition = target.getPoint();
						packetToServer.clientOrientation = target.getOrientation();
						out.writeObject(packetToServer);
					}
				}
					
			}
		} catch (IOException e) {
			System.err.println("ERROR: Lost connection to server.");
			System.exit(1);

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

	}

	public void updateScore(Client c, int score) {
		MazewarPacket scoreToServer = new MazewarPacket();
		scoreToServer.clientName = c.getName();
		scoreToServer.clientScore = score;
		scoreToServer.type = MazewarPacket.CLIENT_SCORE_SEND;
		synchronized (out) {
			try {
				out.writeObject(scoreToServer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void clientKilled(Client source, Client target) {
		if (target.getName().equals(guiClient.getName())) {
			try {
				MazewarPacket packetToServer = new MazewarPacket();
				packetToServer.type = MazewarPacket.CLIENT_KILLED;
				packetToServer.clientName = target.getName();
				packetToServer.sourceName = source.getName();
				packetToServer.clientPosition = guiClient.getPoint();
				packetToServer.clientOrientation = guiClient.getOrientation();
				maze.repositionClient(target, packetToServer.clientPosition, packetToServer.clientOrientation);

				synchronized (out) {
					out.writeObject(packetToServer);
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public void mazeUpdate() {
		// TODO Auto-generated method stub

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
}
