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

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.
 * 
 * @author Geoffrey Washburn &lt;<a
 *         href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {
	ObjectOutputStream out = null;
	/**
	 * Create a GUI controlled {@link LocalClient}.
	 */
	public GUIClient(String name, ObjectOutputStream out) {
		super(name);
		this.out = out;
	}

	public GUIClient(String name) {
		super(name);
	}

	/**
	 * Handle a key press.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyPressed(KeyEvent e) {
		/* make a new request packet */
		MazewarPacket packetToServer = null;

		// out.writeObject(packetToServer);

		// If the user pressed Q, invoke the cleanup code and quit.
		if ((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
			// Mazewar.quit();
			packetToServer = new MazewarPacket();
			packetToServer.type = MazewarPacket.CLIENT_BYE;
			packetToServer.clientName = getName();
			packetToServer.clientPosition = getPoint();
			packetToServer.clientOrientation = getOrientation();
			
			// Up-arrow moves forward.
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			//forward();
			packetToServer = new MazewarPacket();
			packetToServer.type = MazewarPacket.CLIENT_FORWARD;
			packetToServer.clientName = getName();
			packetToServer.clientPosition = getPoint();
			packetToServer.clientOrientation = getOrientation();
			if (maze.checkMoveClientForward(this)) {
				packetToServer.clientPosition = packetToServer.clientPosition.move(getOrientation());
			}
			// Down-arrow moves backward.
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			//backup();
			packetToServer = new MazewarPacket();
			packetToServer.type = MazewarPacket.CLIENT_REVERSE;
			packetToServer.clientName = getName();
			packetToServer.clientPosition = getPoint();
			packetToServer.clientOrientation = getOrientation();
			if (maze.checkMoveClientBackward(this)) {
				packetToServer.clientPosition = packetToServer.clientPosition.move(getOrientation().invert());
			}
			// Left-arrow turns left.
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			//turnLeft();
			packetToServer = new MazewarPacket();
			packetToServer.type = MazewarPacket.CLIENT_LEFT;
			packetToServer.clientName = getName();
			packetToServer.clientPosition = getPoint();
			packetToServer.clientOrientation = getOrientation().turnLeft();
			
			// Right-arrow turns right.
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			//turnRight();
			packetToServer = new MazewarPacket();
			packetToServer.type = MazewarPacket.CLIENT_RIGHT;
			packetToServer.clientName = getName();
			packetToServer.clientPosition = getPoint();
			packetToServer.clientOrientation = getOrientation().turnRight();
			// Spacebar fires.
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			//fire();
			packetToServer = new MazewarPacket();
			packetToServer.type = MazewarPacket.CLIENT_FIRE;
			packetToServer.clientName = getName();
			packetToServer.clientPosition = getPoint();
			packetToServer.clientOrientation = getOrientation();
		}

		try {
			if (packetToServer != null) {
				
				synchronized (out){
					out.writeObject(packetToServer);
					System.out.println("Sent Packet " + packetToServer.type);
					//out.close();
				}
			}

		} catch (IOException e1) {
			System.out.println("Error Sending Packet to server");
			e1.printStackTrace();
		}

	}

	/**
	 * Handle a key release. Not needed by {@link GUIClient}.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyReleased(KeyEvent e) {
	}

	/**
	 * Handle a key being typed. Not needed by {@link GUIClient}.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyTyped(KeyEvent e) {
	}

	
	
}
