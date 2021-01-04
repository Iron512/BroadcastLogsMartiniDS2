package theL0nePr0grammer_2nd.relays;

import java.util.List;
import java.util.Objects;

import org.geotools.filter.expression.ThisPropertyAccessorFactory;

import cern.jet.random.Normal;

import java.text.DecimalFormat;
import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import theL0nePr0grammer_2nd.Manager;
import theL0nePr0grammer_2nd.Wavefront;
import theL0nePr0grammer_2nd.perturbations.*;

public abstract class Relay {
	protected int id;
	protected double xpos;
	protected double ypos;
	protected double perturbationProb;
	
	protected boolean finalPhase;
	
	//The space and the manager are declared, but not initialized, as they exist when the Relay is generated as RelayI, II or III.
	protected ContinuousSpace<Object> space;
	protected Manager aether;
	
	protected List<Wavefront> incomingWavefronts;
	protected List<String> topics;
	
	protected boolean active;
	protected double latency;
	protected double jitter;
	
	protected double spawnProb;
	protected double leaveProb;
	protected double lossProb;
	
	protected Normal distribution;
	
	public Relay(int id, double xpos, double ypos, double perturbationProb, boolean active, double latency, double jitter, double spawnProb, double leaveProb, double lossProb) {
		this.id = id;
		this.xpos = xpos;
		this.ypos = ypos;
		this.perturbationProb = perturbationProb;
		this.finalPhase = false;
		
		this.incomingWavefronts = new ArrayList<Wavefront>();
		this.topics = new ArrayList<String>();
		
		this.active = active;
		this.latency = latency;
		this.jitter = jitter;
		
		this.spawnProb = spawnProb;
		this.leaveProb = leaveProb;
		this.lossProb = lossProb;
		
		this.distribution = RandomHelper.createNormal(latency,jitter);
	}
	
	//STANDARD METHOD OVERRIDE
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relay)) return false;
        Relay cmp = (Relay) o;
        return id == cmp.id;
    }
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
    @Override
    public String toString() { 
        return "(relay " + Integer.toString(this.id) + ")"; 
    }
    
    //GETTERS AND SETTERS
    //Note that not all attributes have a setter or a getter. 
    //This has been planned to avoid inconsistencies or wrongful information leaking.
    public int getId() {
    	return this.id;
    }
    
    public double getX() {
    	return this.xpos;
    }
    
    public double getY() {
    	return this.ypos;
    }
    
    public double getPerturbationProb() {
    	return this.perturbationProb;
    }
    
    public double getLatency() {
    	return this.latency;
    }
    
    public boolean getActive() {
    	return this.active;
    }
    
    public void setActive(boolean value) {
    	this.active = value;
    }
    
    public void end() {
    	this.finalPhase = true;
    }
    
	public double getMessageLife() {
		return this.distribution.nextDouble() / 2.0;
	}
	
    public String getLabel() {
    	DecimalFormat df2 = new DecimalFormat("#.##");
    	return this.toString() + "\n" + df2.format(this.getX()) + ", " + df2.format(this.getY());
    }
    
	public void setContext(Manager aether, ContinuousSpace<Object> space) {
		this.aether = aether;
		this.space = space;
	}

    public double getR() {
       	if (this.perturbationProb != 0 && this.active)
    		return 0.0;
    	return 255.0;
    }
    public double getG() {
    	if (!this.active)
    		return 0.0;
    	return 255.0;
    }
    public double getB() {
    	return 0.0;
    }
    
    public boolean addTopic(String topic) {
    	return this.topics.add(topic);
    }
    
    public boolean removeTopic(String topic) {
    	return this.topics.remove(topic);
    }

    
    //ACTUAL RELAY IMPLEMENTATION
    public void addWavefront(Wavefront w) {
    	this.incomingWavefronts.add(w);
    }
    
    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
    	if (!finalPhase) {
        	//Generate or not a message
        	if (RandomHelper.nextDoubleFromTo(0.0, 1.0) > (1.0-this.perturbationProb)) {
        		double type = RandomHelper.nextDoubleFromTo(0.0, 1.0);
        		
        		if (type <= 0.6) 
        			this.sendMessageBroadcast((Object) RandomHelper.nextIntFromTo(20, 1500));
        		else if (type <= 0.8) {
        			List<Relay> dst =  new ArrayList<Relay>();
        			
        			//Might insert duplicates, but doesn't matter, just test purpose
        			int count = RandomHelper.nextIntFromTo(0,4);
        			for (int i = 0; i < count; i++) {
        				dst.add(aether.getRandomRelay(this));
        			}
        		
        			this.sendMessageAddressed((Object) RandomHelper.nextIntFromTo(20, 1500), dst);
        		} else {
        			this.sendMessageTopic((Object) RandomHelper.nextIntFromTo(20, 1500), aether.getRandomTopic());
        		}
        	}
    		
        	if (RunEnvironment.getInstance().getCurrentSchedule().getTickCount() % 10 == 0)
	        	if (this.active) {
	        		//might leave the network
	            	if (RandomHelper.nextDoubleFromTo(0.0, 1.0) > (1.0-this.leaveProb)) {
	            		this.leave();      		
	            	}
	        	} else {
	        		//might join the network
	            	if (RandomHelper.nextDoubleFromTo(0.0, 1.0) > (1.0-this.spawnProb)) {
	            		this.join();
	            	}
	        	}
    	}
    		    	
    	List<Wavefront> remove = new ArrayList<Wavefront>();
    	incomingWavefronts.forEach((w -> {
    		Perturbation rcv = w.live();
    		
    		if (rcv != null) {
            	if (RandomHelper.nextDoubleFromTo(0.0, 1.0) > (this.lossProb)) {	
        			this.onSense(rcv);
            	}
            	remove.add(w);
    		}
    	}));
    	
    	remove.forEach((w) -> {
    		incomingWavefronts.remove(w);
    	});
    	
    	remove.clear();   	
    }
    
	
	public int nextRef(Perturbation p) {
		if (p == null)
			return 1;
		return p.getRef()+1;
	}
    
	public abstract void sendMessageBroadcast(Object value);
	public abstract void sendMessageAddressed(Object value, List<Relay> dest);
	public abstract void sendMessageTopic(Object value, String topic);
	
	public abstract void onSense(Perturbation p);
	
	public abstract boolean compareStatus(Relay cmp);
	public abstract void printStatus();
	
	public abstract void join();
	public abstract void leave();
}
