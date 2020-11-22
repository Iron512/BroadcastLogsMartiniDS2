package broadcastAppendOnlyLogs;

import java.util.Objects;

public class Message {
	private Relay source; 
	private int id;
	//This substitutes the value in the message. I wanted to represent the payload of the message as a number of byte.
	//In this way, in a future implementation of a throughput evaluation (narrowing for example the bandwidth), 
	//the simulator can coherently adapt and execute finer real case scenarios.
	private int dimension;
	
	//Standard method definition (constructor, equals/hashcode, toString)
	public Message (Relay source, int id, int dimension) {
		this.source = source;
		this.id = id;
		this.dimension = dimension; 
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message cmp = (Message) o;
        return id == cmp.id && source == cmp.source;
    }
	@Override
	public int hashCode() {
		return Objects.hash(source, id);
	}
	@Override
	public String toString() {
		return "message " + source.toString() + " - " + Integer.toString(id) + "(dim:" + Integer.toString(dimension) + ")";
	}
	
	//Custom methods
}