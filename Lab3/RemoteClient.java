import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
  
/**
 * A skeleton for those {@link Client}s that correspond to clients on other computers.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: RemoteClient.java 342 2004-01-23 21:35:52Z geoffw $
 */

public class RemoteClient extends Client implements Runnable{
	
	private Thread thread = null;
	private Socket outSocket = null;
	private ObjectOutputStream myOut = null;
	
	private Socket inSocket = null;
	private ObjectInputStream myIn = null;
	
	private String hostname;
	private int port;
	
	private static ClientManager manager;
	
    /**
     * Create a remotely controlled {@link Client}.
     * @param name Name of this {@link RemoteClient}.
     */
    public RemoteClient(String name, String hostname, int port, int ID, ClientManager manager) {
            super(name, ID);
            this.manager = manager;
            this.hostname = hostname;
			this.port = port;
			            
    		thread = new Thread(this);	
    }

    public RemoteClient(String name, int ID, ClientManager manager) {
        super(name, ID);
        this.manager = manager;
		thread = new Thread(this);	
}

    // this function assumes hostname and port are already filled in
	public void writeObject(MazewarPacket outPacket) {
		// TODO Auto-generated method stub
		if (outSocket == null) {
			try {
				outSocket = new Socket(hostname, port);
				myOut = new ObjectOutputStream(outSocket.getOutputStream());		
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		try {
			myOut.writeObject(outPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setInSocket(Socket socket, ObjectInputStream in) {
		// TODO Auto-generated method stub
		inSocket = socket;
		try {
			if (in == null) {
				myIn = new ObjectInputStream(inSocket.getInputStream());
			} else {
				myIn = in;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		boolean gotByePacket = false;

		try {

			MazewarPacket remotePacket = null;
			
			while ((remotePacket = (MazewarPacket)myIn.readObject()) != null) {
				//System.out.println("Recieved Packet " + packetFromClient.type);
				
				/** process message **/
				MazewarPacket newPacket = new MazewarPacket();
				
				if (remotePacket.type == MazewarPacket.RING_TOKEN){
					//System.out.println("Got token");
					synchronized (Client.actionQueue){
						// need to clear everything in the action queue
						Client.actionQueue = new ArrayList<Event>(remotePacket.eventQueue);
						manager.setToken();
					}
				}
				else if (remotePacket.type == MazewarPacket.CLIENT_REGISTER){
					//TODO player join logic
				}
				else if (remotePacket.type == MazewarPacket.CLIENT_BYE){
					//TODO change connection to maintain ring
				}
				else if(remotePacket.type == MazewarPacket.CLIENT_TEST){
					System.out.println("Recieved packeted from " + remotePacket.myInfo.clientName);
				}
				else if (remotePacket.type == MazewarPacket.JOIN_CONFIRM){
					System.out.println("Recieved join confirm from " + remotePacket.clientName);
				}
				else if (remotePacket.type == MazewarPacket.REQUEST_STATE){
					// need to construct a packet containing all the locations, orientations and scores of everyone
					
					newPacket.remoteList = new ArrayList<ClientInfo>(manager.getAllClientInfo(this.getID()));
					newPacket.type = MazewarPacket.REPLY_STATE;
					
					// send the list	
					writeObject(newPacket);
				
				}
				else if (remotePacket.type == MazewarPacket.REPLY_STATE){
					// pass list to manager and let it update stuff
					manager.syncrhonizeClients(remotePacket.remoteList);
				}
				else {
					System.out.println("Unknown packet type" + remotePacket.type);
				}
				
						
				/* quit case */
				if (remotePacket.type == MazewarPacket.PACKET_NULL || remotePacket.type == MazewarPacket.CLIENT_BYE) {
					gotByePacket = true;
					break;
				}
			}
			

			/* cleanup when client exits */
			myIn.close();
			inSocket.close();
		
		} catch (IOException e) {
			if (!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if (!gotByePacket)
				e.printStackTrace();
		}
	
	}


	public void startThread() {
		// TODO Auto-generated method stub
		thread.start();
	}


	public MazewarPacket readObject() {
		// TODO Auto-generated method stub
		MazewarPacket ret = null;
		try {
			ret = (MazewarPacket)myIn.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
	}


	public ObjectOutputStream getOutStream() {
		// TODO Auto-generated method stub
		if (outSocket == null) {
			try {
				outSocket = new Socket(hostname, port);
				myOut = new ObjectOutputStream(outSocket.getOutputStream());		
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return myOut;
	}
	
	public void setOutSocket(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
		if (outSocket == null) { 
			try {
				outSocket = new Socket(hostname, port);
				myOut = new ObjectOutputStream(outSocket.getOutputStream());		
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
