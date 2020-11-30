package broadcastAppendOnlyLogs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.ContextUtils;

//This class is the implementation of the Relay class. There is not a strict definition on the paper and this
//is how i figured it out. Every relay acts also as perturbation generator. 
//Having a relay just as an observer is only a specific case of this class (Where the probabilty of generating perturbation = 0) 
public class Relay {
	private ContinuousSpace<Object> space;
	private WavefrontManager aether; //The manager that is charged to handle the Wavefronts (which carry the perurbations).
	private List<Wavefront> incomingWavefronts; //The list of all the incoming Wavefront. This is an abstraction of the real life, where
	//the perturbations are obviously sensed and the observer is not aware of the perturbation that it's going to sense 
	
	private Map<Relay, Integer> frontier; //The frontier, basically as described in the paper. I chose an HashMap to keep the
	//abstraction level as high as possible (with no visible matching between "src" and a position into a sort of Array)
	private List<Perturbation> bag; //The straightforward implementation of the bag, as described in Relay II
	
	//The topics list, containing all the subscribed ones.
	private Set<String> topics;
	
	private int id; 
	private double pertGen; //A parameters (expressed as % between 0 and 1) that considers at a certain tick
	//whether or not the relay should generate any perturbation
	private boolean stopGen; //A boolean that inhibts the generation of the messages. Stopping the simulation at any random point 
	//might lead to inconsistencies, due to the propagation of the perturbation. Some ticks before the end of the simulation the Relays
	//stop generating perturbations, in order let all the existing ones to reach their destination. This clearly doesn't break
	//the 2nd property, which requirest that the all the perturbations EVENTUALLY reach all the observers.
	
	//Evaluation Metrics. Here some variables have been defined to evaluate the protocol.
	private int byteProcessed;
	private int byteReceived;
	
	//Standard method definition (constructor, equals/hashcode, toString)
	public Relay(ContinuousSpace<Object> space, WavefrontManager aether, int id, double pertGen) {
		this.space = space;
		this.aether = aether;
		this.id = id;
		this.pertGen = pertGen;
		this.stopGen = false;
		
		this.incomingWavefronts = new ArrayList<Wavefront>();
		this.frontier = new HashMap<Relay,Integer>();
		this.frontier.put(this, 1);
		this.bag = new ArrayList<Perturbation>();
		this.topics = new HashSet<String>();
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
		return Objects.hash(id);
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
    public boolean addTopic(String topic) {
    	return topics.add(topic);
    }
    
    public void addPerturbation(Wavefront p) {
    	//When a perturbation(and therefore a wavefront) is generated the Manager(aether) will enqueue them
    	//Into the incoming Wavefront List, having the Relays to handle them.
    	incomingWavefronts.add(p);
    }
    
	@ScheduledMethod(start = 1, interval = 1)
	//This is the main process of the Relay. It handles the reception of incoming wavefronts and the random generation of some of them
	//It covers all the necessary operation that are not described on the paper.
	public void step() {
		this.byteProcessed = 0;
		this.byteReceived = 0;
		
		//The list toRemove has the task of keeping track of all the wavefronts received during this current tick.ù
		//Removing them while looping might lead to inconsistencies.
		List<Wavefront> toRemove = new ArrayList<Wavefront>();
		for (Wavefront w : incomingWavefronts) {
			//The live method of the wavefront returns the message which is carring if has reached the observer, null otherwise.
			//When a message is returned the Relay processes it, and stops handling it (since it has already been delivered)
			Perturbation msg = w.live();
					
			if (msg != null) {
				onSenseMessageRelayII(msg);
				toRemove.add(w);
			}
		}
		
		for (Wavefront w : toRemove) {
			incomingWavefronts.remove(w);
		}

		//Perturbation generation process. If the Relay decides to generate one, it informs the Manager to create and handle it.
		//Here with probabilities of 50%, 25% and 25% the message is generated as broadcast, uni/multicast or for the pub/sub arch.
		//I decided to fix arbitrarily these probabilities, as they don't really change the evaluation of the process.
		if (!this.stopGen && RandomHelper.nextDoubleFromTo(0.0, 1.0) > (1.0-pertGen)) {
			Perturbation tmp;
			
			if (RandomHelper.nextDoubleFromTo(0.0, 1.0) > 0.5) {
				if (RandomHelper.nextDoubleFromTo(0.0, 1.0) > 0.5) {
					//Uni/Multicast, random number of dest picked randomly from the Manager, which is the only one who knows the active relays
					Set<Relay> dest = new HashSet<Relay>();
					int count = RandomHelper.nextIntFromTo(1, 3);
					while (count != dest.size()) 
						dest.add(aether.getRandomRelay());
					
					tmp =  new Perturbation(this, frontier.get(this), RandomHelper.nextIntFromTo(0, 1500), dest);
				} else {
					//Pub/Sub message, on a random topic picked from the Manager
					tmp =  new Perturbation(this, frontier.get(this), RandomHelper.nextIntFromTo(0, 1500), aether.getRandomTopic());
				}
			} else {
				//Classi broadcast message
				tmp =  new Perturbation(this, frontier.get(this), RandomHelper.nextIntFromTo(0, 1500));
			}
			
			aether.generatePerturbation(this,tmp);
			frontier.replace(this, nextRef(tmp));
		}
	}
	
	public void onSenseMessageRelayII(Perturbation p) {
		Relay tmpR = p.getSource();
		if (!tmpR.equals(this)) { //No relays cares about perturbations sent from himself (they have been forwarded of course)
			if (frontier.get(tmpR) == null) {
				//frontier.put(tmpR, 1); //Once i used this line to initialize the correspondent expected value for each relay.
				
				//However, when a new Relay joins, it can not be aware of the previous perturbations (by definition, they
				//don't leave disturbance behind them) therefore, the first message got from a source becames the head of the frontier.
				frontier.put(tmpR, p.getRef());
			}
			
			if (p.getRef() >= frontier.get(tmpR) && !bag.contains(p)) {
				bag.add(p);
				
				//Here the code is translated a little bit differently, since there is no way (at least that i know, right now i can 
				//not think of any suitable) to translate the pseudocode of "While exists Q in bag with frontier[Q.src] == Q.ref".
				//So my solution is to check each element in the bag, if we have a match we execute another iteration, if not, both the 
				//bag and the frontier have not changed their status, so its time to exit (no "changes" detected)
				boolean changes = true;
				while (!bag.isEmpty() && changes) {
					changes = false;
					//I need to act as above. Removing a node while looping might lead to inconsitencies
					List<Perturbation> toRemove = new ArrayList<Perturbation>();
					
					for (Perturbation q : bag) {
						if (frontier.get(q.getSource()) == q.getRef()) {
							//the message here gets delivered
							aether.generatePerturbation(this, q);
							
							if (q.getTopic() != null) {
								//this message has been sent by a publisher, check the topic to see if i am subscripted.
								if (this.topics.contains(q.getTopic())) {
									//I am subscribed
									consume_upcall(q); 
								}
							}
							
							if (q.destinatedTo(this)) {
								//this message is destinated to me (and possibily some other Relays)
								recv_upcall(q);
							}
							
							//update the frontier
							Network<Object> net = (Network<Object>) ContextUtils.getContext(this).getProjection("messagesNetwork");
								
							for (RepastEdge e : net.getEdges(this)) {
								if (e.getSource().equals(this))
									net.removeEdge(e);
							}
							
							if (aether.active((q.getSource())))
								net.addEdge(q.getSource(), this);
							
							
							frontier.replace(tmpR, nextRef(q));
							toRemove.add(q);
							changes = true;
							
							byteProcessed += p.getDimension();
							byteReceived += p.getDimension();
						}
					}
					
					for (Perturbation r : toRemove) {
						bag.remove(r);
					}
				}
			} else {
				byteReceived += p.getDimension();
			}
		}
	}
	
	//Here the code for getting a message to a subscripted topic
	public void consume_upcall(Perturbation p) {
		System.out.println(this + " recived a message from " + p.getSource() + " with the topic " + p.getTopic());
	}
	
	//Here the code for getting a private uni/multicast
	public void recv_upcall(Perturbation p) {
		System.out.println(this + " recived a private message from " + p.getSource());
	}
	
	public int nextRef(Perturbation p) {
		if (p == null)
			return 1;
		else 
			return p.getRef()+1;
	}
	
	//Auxiliary functions
	public void stopPerturbations() {
		this.stopGen = true;
	} 
	
	public void printFrontier() {
		System.out.println(this.toString());
		frontier.forEach((k,v) -> {
			System.out.println("    " + k.toString() + ": " + v);
		});
	}
	
	public boolean checkFrontiers(Relay cmp) {
		//It is not really trivial (as it may look) to consider if two frontiers are the same.
		//With a static network and no packet loss the task is easy, but in dynamic network this is not the same
		//as a node might have joined the network after the all the packets from another one were sent.
		//for this reason i decided to consider 2 frontiers identical, iff each element is either identical in both or eventually null

		for (Map.Entry<Relay, Integer> entry : this.frontier.entrySet()) {
			if ((cmp.frontier.get(entry.getKey()) != null && (int) entry.getValue() != cmp.frontier.get(entry.getKey()))) {
				return false;
			}
		}

		return true;
	}
	
	//Evaluation Metrics Methods
	public double getBytesReceived() {
		return this.byteReceived/1024.0;
	}
	
	public double getBytesProcessed() {
		return this.byteProcessed/1024.0;
	}
	
	public int getBagSize() {
		return this.bag.size();
	}
	
	public int getMessagesCount() {
		int count = 0;

		for (Map.Entry<Relay, Integer> entry : this.frontier.entrySet()) {
			if (!entry.getKey().equals(this))
				count += entry.getValue();
		}
		return count;
	}
}