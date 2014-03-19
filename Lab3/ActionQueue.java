import java.util.ArrayList;
import java.util.List;


public class ActionQueue{
	List<Event> queue;
	
	ActionQueue(){
		queue = new ArrayList<Event>();
	}
	
	public void addSorted(Event event){
		if (queue.size() == 0){
			queue.add(event);
		}
		
		int sortbyTime = event.timeDeliver;
		int i;
		for (i = queue.size() -1; i >-1; i--){
			if (queue.get(i).timeDeliver < sortbyTime){
				queue.add(i+1, event);
				return;
			}
		}
		//if we get here, event should be at top of queue
		queue.add(0, event);
		
	}
	
	public void updateAndSort(Event event){
		for (Event queueEvent: queue){
			if (queueEvent.initTime == event.initTime){
				queue.remove(queueEvent);
				addSorted(event);
				return;
			}
		}
		System.out.println("Error, could not find event program exiting");
		System.exit(-1);
	}
	
}