package broadcastAppendOnlyLogs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bsh.This;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;

public class Relay {
	private ContinuousSpace<Object> space;
	private PerturbationManager aether;
	private List<Perturbation> incomingPert;
	
	private int i = 0;
	private boolean onlyone = true;
	private int id;
	private int pertGen;
	
	//Standard method definition (constructor, equals/hashcode, toString)
	public Relay(ContinuousSpace<Object> space, PerturbationManager aether, int id, int pertGen) {
		this.space = space;
		this.aether = aether;
		this.id = id;
		this.pertGen = pertGen;
		
		this.incomingPert = new ArrayList<Perturbation>();
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
    public void addPerturbation(Perturbation p) {
    	incomingPert.add(p);
    }
    
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		ArrayList<Perturbation> toRemove = new ArrayList<Perturbation>();
		for (Perturbation p : incomingPert) {
			Message msg = p.live();
					
			if (msg != null) {
				senseMessage(msg);
				toRemove.add(p);
			}
		}
		for (Perturbation p : toRemove) {
			incomingPert.remove(p);
		}

		if (RandomHelper.nextIntFromTo(0, 100) > 100-pertGen && onlyone) {
			onlyone = false;
			System.out.println(this.toString() + "is generating a new perturbation");
			aether.generatePerturbation(this, new Message(this, 1, 30));
		}
	}
	
	public void senseMessage(Message msg) {
		System.out.println(this.toString() + " received (" + i + ") " + msg.toString());
	}
}