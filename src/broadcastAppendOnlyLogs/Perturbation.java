package broadcastAppendOnlyLogs;
import java.util.Set;
import java.util.Objects;

//This class implements the perturbation as stricly defined in the Paper
public class Perturbation {
	private Relay source; //The source that generated the perturbation
	private int ref; // The reference of the perturbation. Just an incremental logical clock, assigned by the source.
	
	private int dimension; //This substitutes the "value" field in the perturbation. I wanted to represent the payload
	//of the message as a number of byte. In this way, in a possibly future implementation of a throughput evaluation 
	//(narrowing for example the bandwidth), the simulator can already coherently adapt and execute finer real case scenarios.
	
	private Set<Relay> destination; //The set of the destination that the perturbation carries. It resolves both unicast and multicast
	private String topic; //The topic for which the message is intended, in the pub/sub architecture.
	
	//Standard method definition (constructor, equals/hashcode, toString)
	//Classic message
	public Perturbation (Relay source, int ref, int dimension) {
		this.source = source;
		this.ref = ref;
		this.dimension = dimension; 
		
		this.destination = null;
		this.topic = null;
	}
	
	//Unicast/Multicast message
	public Perturbation (Relay source, int ref, int dimension, Set<Relay> destination) {
		this.source = source;
		this.ref = ref;
		this.dimension = dimension; 
		
		this.destination = destination;
		this.topic = null;
	}
	
	//Pub/Sub message
	public Perturbation (Relay source, int ref, int dimension, String topic) {
		this.source = source;
		this.ref = ref;
		this.dimension = dimension; 
		
		this.destination = null;
		this.topic = topic;
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
	
	public String getTopic() {
		return this.topic;
	}
	
	//Custom methods
	//I avoided the implementation of getDestination, which is substituted by this method.
	//In this way no relay can know the destination of a message.
	public boolean destinatedTo(Relay r) {
		if (this.destination != null)
			return this.destination.contains(r);
		return false;
	}
}