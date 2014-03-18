import java.net.*;
import java.io.*;

public class MazewarServer {
    public static void main(String[] args) throws IOException {
        
    	ServerSocket serverSocket = null;
        boolean listening = true;
        int maze_seed = 0;
        
        try {
        	if(args.length == 2) {
        		
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        		
        		// initialize the server here.
        		// store seed to pass on to clients that connect
        		maze_seed = Integer.parseInt(args[1]);
        		      		        		
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        	
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }      
        
        // start broadcast thread:
        new MazewarServerHandlerThread(maze_seed).start();
        
        while (listening) {
        	new MazewarServerHandlerThread(serverSocket.accept(), maze_seed).start();
        }

        serverSocket.close();
    }
}
