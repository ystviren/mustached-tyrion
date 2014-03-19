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
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.
 * 
 * @author Geoffrey Washburn &lt;<a
 *         href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {
	List<ObjectOutputStream> out = null;
	int pID;
	
	/**
	 * Create a GUI controlled {@link LocalClient}.
	 */
	public GUIClient(String name, int pID, ObjectOutputStream out) {
		super(name);
		this.out = new ArrayList<ObjectOutputStream>();
		this.out.add(out);
		this.pID = pID;
		
	}

	public GUIClient(String name) {
		super(name);
	}
	
	public void addClient(ObjectOutputStream out){
		this.out.add(out);
	}
	
	public int getLamport(){
		return 0;
	}

	/**
	 * Handle a key press.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyPressed(KeyEvent e) {
		/* make a new request packet */

		MazewarPacket packetToMulticast = null;


		// If the user pressed Q, invoke the cleanup code and quit.
		if ((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
			// Mazewar.quit();
			packetToMulticast = new MazewarPacket();
			packetToMulticast.event = new Event(pID, getLamport(), MazewarPacket.CLIENT_BYE);
			packetToMulticast.type = MazewarPacket.NEW_EVENT;
			packetToMulticast.clientName = getName();
			packetToMulticast.clientPosition = getPoint();
			packetToMulticast.clientOrientation = getOrientation();
			
			// Up-arrow moves forward.
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			//forward();
			packetToMulticast = new MazewarPacket();
			packetToMulticast.event = new Event(pID, getLamport(), MazewarPacket.CLIENT_FORWARD);
			packetToMulticast.type = MazewarPacket.NEW_EVENT;
			packetToMulticast.clientName = getName();
			packetToMulticast.clientPosition = getPoint();
			packetToMulticast.clientOrientation = getOrientation();
			if (maze.checkMoveClientForward(this)) {
				packetToMulticast.clientPosition = packetToMulticast.clientPosition.move(getOrientation());
			}
			// Down-arrow moves backward.
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			//backup();
			packetToMulticast = new MazewarPacket();
			packetToMulticast.event = new Event(pID, getLamport(), MazewarPacket.CLIENT_REVERSE);
			packetToMulticast.type = MazewarPacket.NEW_EVENT;
			packetToMulticast.clientName = getName();
			packetToMulticast.clientPosition = getPoint();
			packetToMulticast.clientOrientation = getOrientation();
			if (maze.checkMoveClientBackward(this)) {
				packetToMulticast.clientPosition = packetToMulticast.clientPosition.move(getOrientation().invert());
			}
			// Left-arrow turns left.
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			//turnLeft();
			packetToMulticast = new MazewarPacket();
			packetToMulticast.event = new Event(pID, getLamport(), MazewarPacket.CLIENT_LEFT);
			packetToMulticast.type = MazewarPacket.NEW_EVENT;
			packetToMulticast.clientName = getName();
			packetToMulticast.clientPosition = getPoint();
			packetToMulticast.clientOrientation = getOrientation().turnLeft();
			
			// Right-arrow turns right.
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			//turnRight();
			packetToMulticast = new MazewarPacket();
			packetToMulticast.event = new Event(pID, getLamport(), MazewarPacket.CLIENT_RIGHT);
			packetToMulticast.type = MazewarPacket.NEW_EVENT;
			packetToMulticast.clientName = getName();
			packetToMulticast.clientPosition = getPoint();
			packetToMulticast.clientOrientation = getOrientation().turnRight();
			// Spacebar fires.
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			//fire();
			packetToMulticast = new MazewarPacket();
			packetToMulticast.event = new Event(pID, getLamport(), MazewarPacket.CLIENT_FIRE);
			packetToMulticast.type = MazewarPacket.NEW_EVENT;
			packetToMulticast.clientName = getName();
			packetToMulticast.clientPosition = getPoint();
			packetToMulticast.clientOrientation = getOrientation();
		}

		try {
			if (packetToMulticast != null) {
				for (ObjectOutputStream out : this.out){
					synchronized (out){
						out.writeObject(packetToMulticast);
					}
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
