import java.io.Serializable;
import java.util.ArrayList;


public class FileServerPacket implements Serializable{
	/* define constants */
	public static final int PACKET_NULL    = 0;
	public static final int FILE_REQUEST = 101;
		
	public static final int REPLY_REQUEST    = 201;
	
	public static final int CLIENT_BYE  = 301;
	
	/* error codes */
	public static final int ERROR_INVALID   = -101;
	
	/* message header */
	public int type = FileServerPacket.PACKET_NULL;
	
	public int partition;
	
	public ArrayList<String> words = null;
	
	public int error_code;
	
	public String hash;
}
