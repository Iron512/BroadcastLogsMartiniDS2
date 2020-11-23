package broadcastAppendOnlyLogs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import bsh.This;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;

public class Relay {
	private ContinuousSpace<Object> space;
	private WavefrontManager aether;
	private List<Wavefront> incomingPert;
	
	private Map<Relay, Integer> frontier;
	
	private int logicalClock = 1;
	private int id;
	private double pertGen;
	private boolean stopGen;
	
	//Standard method definition (constructor, equals/hashcode, toString)
	public Relay(ContinuousSpace<Object> space, WavefrontManager aether, int id, double pertGen) {
		this.space = space;
		this.aether = aether;
		this.id = id;
		this.pertGen = pertGen;
		this.stopGen = false;
		
		this.incomingPert = new ArrayList<Wavefront>();
		this.frontier = new HashMap<Relay,Integer>();
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relay)) return false;
        Relay cmp = (Relay) o;
        return id == cmp.id;
    }
	@Override
	public int hashCode() {
		return Objects.hash(id, pertGen);
	}
    @Override
    public String toString() { 
        return "(relay " + Integer.toString(this.id) + ")"; 
    } 

    //Getters and Setters
	public int getId() {		
		return this.id;
	}
	
	public double getX() {
		return space.getLocation(this).getX();
	}
	
	public double getY() {
		return space.getLocation(this).getY();
	}
	
	
    public String getLabel() {
    	DecimalFormat df2 = new DecimalFormat("#.##");
    	return this.toString() + "\n" + df2.format(this.getX()) + ", " + df2.format(this.getY());
    }
    
	//Custom methods
    public void addPerturbation(Wavefront p) {
    	incomingPert.add(p);
    }
    
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		ArrayList<Wavefront> toRemove = new ArrayList<Wavefront>();
		for (Wavefront p : incomingPert) {
			Perturbation msg = p.live();
					
			if (msg != null) {
				senseMessage(msg);
				toRemove.add(p);
			}
		}
		for (Wavefront p : toRemove) {
			incomingPert.remove(p);
		}

		if (!this.stopGen && RandomHelper.nextDoubleFromTo(0.0, 100.0) > (100.0-pertGen)) {
			System.out.println(this.toString() + "is generating a new perturbation");
			aether.generatePerturbation(this, new Perturbation(this, logicalClock++, 30));
		}
	}
	
	public void senseMessage(Perturbation msg) {
		//System.out.println(
		//		this.toString() + " received (" + logicalClock++ + ") " + msg.toString() + " --- " + frontier.get(msg.getSource()));
	
		Relay tmpR = msg.getSource();
		if (!tmpR.equals(this)) { //No relays cares about message sent from himself
			if (frontier.get(tmpR) == null) {
				frontier.put(tmpR, 1);	
			}
			
			if (frontier.get(tmpR) == msg.getRef()) {
				aether.generatePerturbation(this, msg);
				frontier.replace(tmpR, nextRef(msg.getRef()));
			}
		}
	}
	
	public int nextRef(int ref) {
		return ref+1;
	}
	
	public void stopPerturbations() {
		this.stopGen = true;
	} 
	
	public void printFrontier() {
		System.out.println(this.toString() + ": " + this.logicalClock);
		frontier.forEach((k,v) -> {
			System.out.println("    " + k.toString() + ": " + v);
		});
	}
	
	public boolean checkFrontiers(Relay cmp) {
		boolean toRtn = true;
		
		//It is not really trivial (as it may look) to consider if two frontiers are the same.
		//With a static network and no packet loss the task is easy, but in dynamic network this is not the same
		
		
		return toRtn;
	}
}