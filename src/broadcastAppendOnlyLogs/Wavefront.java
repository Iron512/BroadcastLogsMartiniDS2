package broadcastAppendOnlyLogs;

import java.util.Objects;

import repast.simphony.random.RandomHelper;

public class Wavefront {
	private Relay source; //The source of the Wavefront is different from the source of the Perturbation (as perturbation are usually forwarded)
	private Perturbation msg;
	
	//This 4 parameters have the task of simulating the expansion of the perturbation. They could have been implemented differently, but in the report 
	//I tried to explain my reasons. 
	private int msgLife; //How much the Wavefront have been traveled (in term of distance "units")
	private int minDistancePerTick; //Fixed parameter for the min distance travelled each tick
	private int maxDistancePerTick; //Fixed parameter for the max distance travelled each tick
	private Double msgDistance; //Represent the arrival of the Wavefront. It is calculated by the Manager, based on the euclidean distance of the Relays.

	//Standard method definition (constructor, equals/hashcode, toString)
	public Wavefront(Relay source, Perturbation msg, int msgLife, int minDistancePerTick, int maxDistancePerTick, Double msgDistance) {
		this.source = source;
		this.msg = msg;
		
		this.msgLife = msgLife;
		this.minDistancePerTick = minDistancePerTick;
		this.maxDistancePerTick = maxDistancePerTick;
		
		this.msgDistance = msgDistance;
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wavefront)) return false;
        Wavefront cmp = (Wavefront) o;
        return source.equals(cmp.source) && msg.equals(cmp.msg);
    }
	@Override
	public int hashCode() {
		return Objects.hash(source, msg);
	}
	@Override
	public String toString() {
		return "Wavefront (" + Integer.toString(msgLife) + "/" + Double.toString(msgDistance) + ") of " + msg.toString();
	}
	
	public Relay getSource() {
		return this.source;
	}
	
	public Perturbation getMsg() {
		return this.msg;
	}
	//Custom methods
	
	//This method is used by the relays to understand when the Wavefront (and the perturbation carried) reached them. The distance traveled is 7
	//Incremented randomly by a value in the min-max range, defined as a parameter (in the repast simulation gui)
	public Perturbation live() {
		this.msgLife += RandomHelper.nextIntFromTo(minDistancePerTick, maxDistancePerTick);
		if(this.msgLife >= this.msgDistance) {
			//message has to be delivered
			//(Here the perturbation should be destroyed)
			return msg;
		} else {
			//still traveling, let the perturbation continue
			return null;
		}
	}
	
	//This method indeed calculates how far the perturbation can go in the worst case. The manager aether track each wavefront, in order to 
	//forward them also to new Relays (in the case of the dynamic network topology) and keeps them tracked until no new Relay can sense them.
	public boolean track() {
		this.msgLife += minDistancePerTick;
		return this.msgLife >= this.msgDistance;
	}
}