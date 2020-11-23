package broadcastAppendOnlyLogs;
import java.util.Objects;

public class Perturbation {
	private Relay source; 
	private int ref;
	//This substitutes the value in the message. I wanted to represent the payload of the message as a number of byte.
	//In this way, in a future implementation of a throughput evaluation (narrowing for example the bandwidth), 
	//the simulator can coherently adapt and execute finer real case scenarios.
	private int dimension;
	
	//Standard method definition (constructor, equals/hashcode, toString)
	public Perturbation (Relay source, int ref, int dimension) {
		this.source = source;
		this.ref = ref;
		this.dimension = dimension; 
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Perturbation)) return false;
        Perturbation cmp = (Perturbation) o;
        return ref == cmp.ref && source == cmp.source;
    }
	@Override
	public int hashCode() {
		return Objects.hash(source, ref);
	}
	@Override
	public String toString() {
		return "message " + source.toString() + " - " + Integer.toString(ref) + "(dim:" + Integer.toString(dimension) + ")";
	}
	
	//Getter and Setter
	public Relay getSource() {
		return this.source;
	}
	
	public int getRef() {
		return this.ref;
	}
	
	public int getDimension() {
		return this.dimension;
	}
	
	//Custom methods
}