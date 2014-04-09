// Defines communication between client driver and job tracker
public class JobTrackerPacket {
	/* define constants */
	public static final int PACKET_NULL    = 0;
	public static final int JOB_REQUEST = 101;
	public static final int JOB_QUERRY   = 102;
		
	public static final int REPLY_REQUEST    = 201;
	public static final int REPLY_QUERRY = 202;
	
	public static final int CLIENT_BYE  = 301;
	
	/* error codes */
	public static final int ERROR_INVALID   = -101;
	
	/* message header */
	public int type = JobTrackerPacket.PACKET_NULL;
	
	public int error_code;
	
	public String hash;
}
