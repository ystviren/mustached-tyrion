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

public class GUIClient extends LocalClient implements KeyListener, MazeListener {
	List<ObjectOutputStream> out = null;
	int pID;

	/**
	 * Create a GUI controlled {@link LocalClient}.
	 */
	public GUIClient(String name, int pID) {
		super(name, pID);
		this.out = new ArrayList<ObjectOutputStream>();
		this.pID = pID;

	}

	public void addClient(ObjectOutputStream out) {
		this.out.add(out);
	}

	/**
	 * Handle a key press.
	 * 
	 * @param e
	 *            The {@link KeyEvent} that occurred.
	 */
	public void keyPressed(KeyEvent e) {
		/* make a new request packet */

		Event event = null;

		// If the user pressed Q, invoke the cleanup code and quit.
		if ((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
			event = new Event(pID, 0, getPoint(), getOrientation(), MazewarPacket.CLIENT_BYE);
			// Up-arrow moves forward.
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			// forward();
			event = new Event(pID, 0, getPoint(), getOrientation(), MazewarPacket.CLIENT_FORWARD);
			// Down-arrow moves backward.
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			// backup();
			event = new Event(pID, 0, getPoint(), getOrientation(), MazewarPacket.CLIENT_REVERSE);
			// Left-arrow turns left.
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			// turnLeft();
			event = new Event(pID, 0, getPoint(), getOrientation(), MazewarPacket.CLIENT_LEFT);
			// Right-arrow turns right.
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			// turnRight();
			event = new Event(pID, 0, getPoint(), getOrientation(), MazewarPacket.CLIENT_RIGHT);
			// Spacebar fires.
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			// fire();
			event = new Event(pID, 0, getPoint(), getOrientation(), MazewarPacket.CLIENT_FIRE);
		}


		if (event != null){
			synchronized(Client.localQueue){
				Client.localQueue.add(event);

			}
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

	@Override
	public void mazeUpdate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void clientKilled(Client source, Client target) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub

		// ObjectOutputStream out = null;
//		if (target.getName().equals(getName())) {
//			try {
//				System.out.println("I died");
//
//				MazewarPacket packetToMulticast = new MazewarPacket();
//				packetToMulticast.type = MazewarPacket.CLIENT_KILLED;
//				packetToMulticast.clientName = target.getName();
//				packetToMulticast.sourceName = source.getName();
//				packetToMulticast.clientPosition = getPoint();
//				packetToMulticast.clientOrientation = getOrientation();
//				System.out.println("reposition: "
//						+ packetToMulticast.clientOrientation.toString());
//
//				for (ObjectOutputStream out : this.out) {
//					synchronized (out) {
//						out.writeObject(packetToMulticast);
//					}
//				}
//				synchronized (queue) {
//					addSorted(packetToMulticast.event);
//				}
//				// updateScore(source, source.getClientScore(source));
//				// updateScore(target, target.getClientScore(target));
//
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}

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
