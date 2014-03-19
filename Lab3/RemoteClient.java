import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
	
	
    /**
     * Create a remotely controlled {@link Client}.
     * @param name Name of this {@link RemoteClient}.
     */
    public RemoteClient(String name, String hostname, int port, int ID) {
            super(name, ID);
            
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
            
    		thread = new Thread(this);	
    }


	public void writeObject(MazewarPacket outPacket) {
		// TODO Auto-generated method stub
		try {
			myOut.writeObject(outPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setInSocket(Socket socket) {
		// TODO Auto-generated method stub
		inSocket = socket;
		try {
			myIn = new ObjectInputStream(inSocket.getInputStream());
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
			
			while ((remotePacket = (MazewarPacket)remoteIn.readObject()) != null) {
				//System.out.println("Recieved Packet " + packetFromClient.type);
				
				/** process message **/		
				
				// take whatever packet we received and throw it into the buffer. Need to keep it sorted by timestamps
				synchronized (Client.command_buffer) {
					int i = 0;
					int currentSize = Client.command_buffer.size();
					// since we want to keep the buffer always sorted by timestamps, we need to insert to maintain sort
					for (i = 0; i < currentSize; i++) {
						if (Client.command_buffer.get(i).lamportClock > remotePacket.lamportClock) {
							Client.command_buffer.add(i, remotePacket);
						}
					}
					
					// handle case where we have to insert at the end
					if (currentSize == Client.command_buffer.size()) {
						Client.command_buffer.add(remotePacket);
					}
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
}
