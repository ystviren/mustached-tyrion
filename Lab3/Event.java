import java.io.Serializable;

public class Event implements Serializable{
	public int action;
	public int source;
	public int pID2;
	public Point location;
	public Direction orientation;
	public String sourceName;
	
	public Event(int source, int target, Point location, Direction oreintation, int action){
		this.action = action;
		this.source = source;
		this.pID2 = target;
		this.location = location;
		this.orientation = oreintation;
	}
	
}