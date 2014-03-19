import java.net.*;
import java.io.*;

public class MazewarNameServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
        
        try {
        	if(args.length == 1) {
        		serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
        
        File checker = new File("lookup");
        if (checker.exists()){
        	checker.delete();
        }
        
        if (!checker.createNewFile()){
        	System.err.println("ERROR: Could not create register file");
        	System.exit(-1);
        }
        
        while (listening) {
        	new MazewarNameServerHandlerThread(serverSocket.accept()).start();
        }

        serverSocket.close();
    }
    
}
