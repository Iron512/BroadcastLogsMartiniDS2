package broadcastAppendOnlyLogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.WrapAroundBorders;
import repast.simphony.space.graph.Network;

//This class is an artifice developed to simulate the "medium" where perturbation are transfered.
//It acts as a network for the other Relays, which doesn't have any clue on the topology nor the members.
//For this reason, is charged to end the run of the simulation when needed.

//Since going further with the project required this manager to check more and more function i decided to 
//using it also for generating the Context. This involves adding complexity to this code, but from the programming logic point of view
//I believe this one is the most consistent. I hope that the comments i am writing are enough.
public class WavefrontManager implements ContextBuilder<Object>{
	private Context<Object> context;
	ContinuousSpace<Object> space; 
	
	private int totalTick; //The total tick of the simulation. During this period the Relays will procede generating messages.
	//After this time the simulation will run until all the messages has been delivered, to guarantee the consistency of the algoithm 
	//and of the entire simulated system.
	private int minDistancePerTick; //the min distance that a perturbation travels each tick
	private int maxDistancePerTick; //the min distance that a perturbation travels each tick
	private double maxWavefrontLife; //the furthest that a perturbation can travel
	
	private int nodeIndex; //The node index, each time a node is created, it grows.
	private int nodeMinCount; //The upper bound of the network
	private int nodeMaxCount; //The lowr bound of the network
	
	private double spawnProb; //The probability for having a new node to join the network	
	private double leaveProb; //The probability for having an existing node to leave the network 
	private double pertGen; //The probability for each node to generate a message
	
	private Set<Relay> activeRelays;
	private Set<Wavefront> newestWavefront; //contains the newly generated (during the current tick) perturbations,
	//which have still to be initialized (Already in the form of wavefronts)
	private Set<Wavefront> activeWavefront; //contains the currently alive perturbations,
	//which have already been initialized and are still traveling. Useful in the presence of a dynamic network
	
	private ISchedule scheduler;
	private Map<Relay, Map<Relay, Double>> distances; //Its a full symmetrical matrix containing the distances
	//between each member of the network. Gets updated each time a new node enters in the game
	private List<String> topics;
	
	//Evaluation Metrics
	private long kbyteReceived;
	private long kbyteProcessed;
	
	//Standard method definition (constructor, no need for equals and hashcode)
	public Context<Object> build(Context<Object> context) {
		int dstX = 50;
		int dstY = 50;
		
		context.setId("BroadcastAppendOnlyLogs");
		context.add(this); //add also this class, in order to let the scheduled method proceed.
		
		//The first step is the definition of the space. I will stick to the well-known continuous space from tutorials,
		//but I will drop the discrete grid. If i have the correct insight, this component wont be necessary.
		//EDIT: The insight I got, for how i developed the whole simulator, was right.
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		//I will keep the standard dimension 50,50 for the space
		this.space = spaceFactory.createContinuousSpace(
				"space",context,new RandomCartesianAdder<Object>(),new WrapAroundBorders(), dstX, dstY);
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		this.totalTick = params.getInteger("totalTick"); 
		this.minDistancePerTick = params.getInteger("minDistancePerTick");
		this.maxDistancePerTick = params.getInteger("maxDistancePerTick");
		this.maxWavefrontLife = Math.sqrt((dstY*dstY) + (dstX*dstX)) + 1.0; //It briefly calculates the furthest distance that a Wavelength
		//can travel (the maximum life that can be scored). Even in the worst case (with two Relays in the opposite angles of the square)
		//no distance will be furthest than this.
		
		this.nodeIndex = 0;
		this.nodeMinCount = params.getInteger("nodeMinCount");	
		this.nodeMaxCount = params.getInteger("nodeMaxCount");	

		this.spawnProb = params.getDouble("spawnProb"); 
		this.leaveProb = params.getDouble("leftProb");
		this.pertGen = params.getDouble("pertGen"); 
	
		//All the required data structures are initialized
		this.activeRelays = new HashSet<Relay>();
		
		this.newestWavefront = new HashSet<Wavefront>();
		this.activeWavefront = new HashSet<Wavefront>();
		
		this.scheduler = RunEnvironment.getInstance().getCurrentSchedule();
		this.distances = new HashMap<Relay, Map<Relay, Double>>();

		this.topics = new ArrayList<String>();
		this.topics.add("first");
		this.topics.add("second");
		this.topics.add("third");
		
		this.topics.add("fourth");
		this.topics.add("fifth");
		this.topics.add("sixth");
		
		this.topics.add("seventh");
		this.topics.add("eighth");
		this.topics.add("nineth");

		NetworkBuilder<Object> netBuilder = new NetworkBuilder<Object>("messagesNetwork", context, true);
		netBuilder.buildNetwork();
		
		this.context = context;

		this.kbyteProcessed = 0;
		this.kbyteReceived = 0;
		
		//Initialize nodes
		int activeNodes = RandomHelper.nextIntFromTo(nodeMinCount, nodeMaxCount);
		for (nodeIndex = 0; nodeIndex < activeNodes; nodeIndex++) {
			Relay relay = new Relay(space, this, nodeIndex, pertGen);
			this.addRelay(relay);
			
			//Add also some random topics from the pool of existing ones.
			int subscriptions = RandomHelper.nextIntFromTo(0, 3);
			while (subscriptions > 0) {
				int position = RandomHelper.nextIntFromTo(0, this.topics.size()-1);
				
				if (relay.addTopic(this.topics.get(position)))
					subscriptions--;
			}
		}
		return context;
	}
	
	//Custom methods
	public boolean addRelay(Relay item) {
		//Relays MUST also be added in the context
		boolean result = activeRelays.add(item);
		this.context.add(item);
		if (result) {
			distances.put(item, new HashMap<Relay, Double>());
			
			for (Relay rel : activeRelays) {
				if (!rel.equals(item)) {
					//easy euclidean distance
					double distance = Math.sqrt(Math.pow(rel.getX() - item.getX(), 2) + Math.pow(rel.getY() - item.getY(), 2));
					distances.get(rel).put(item, distance);
					distances.get(item).put(rel, distance);
				}
			}
		}
		return result;
	}
	
	public boolean removeRelay(Relay item) {
		//Relays should be also removed from the context!
		boolean result =  activeRelays.remove(item);
		
		if (result) {
			Network<Object> net = (Network<Object>) context.getProjection("messagesNetwork");
			net.getEdges(item).forEach((e) -> {
				net.removeEdge(e);
			});
			
			distances.forEach((k,v) -> {
				if (k != item) {
					v.remove(item);
				}
			});
			distances.remove(item);
			
			this.context.remove(item);
		}
		
		
		return result;
	}
	
	public boolean active(Relay r) {
		return this.activeRelays.contains(r);
	}

	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.FIRST_PRIORITY)
	public void run() {
		
		//Generate a new Relay
		if (RandomHelper.nextDoubleFromTo(0.0, 1.0) > (1.0-this.spawnProb) && this.activeRelays.size() < this.nodeMaxCount) {
			this.addRelay(new Relay(this.space, this, nodeIndex++, pertGen));
			System.out.println("Added");
		}
		
		//Destroy a Relay
		if (RandomHelper.nextDoubleFromTo(0.0, 1.0) > (1.0-this.leaveProb) && this.activeRelays.size() > this.nodeMinCount) {
			int element = RandomHelper.nextIntFromTo(0, this.activeRelays.size());
			Relay item = this.activeRelays.iterator().next();
			
			for (Relay r : this.activeRelays) {
				if (element == 0)
					item = r;
				element--;
			}
			
			this.removeRelay(item);
			System.out.println("Removed");
		}
		
		//Check if we reached the limit of the execution
		if (scheduler.getTickCount() == totalTick) {
			//Stop the generation of messages, to allow the wavefront to syncronize
			for(Relay relay : activeRelays) {
				relay.stopPerturbations();
			}
		}
		
		//Check if we eventually synchronized everything
		if (scheduler.getTickCount() >= totalTick ) {
			//take one random relay as the ground truth for our frontier.

			Relay ground = activeRelays.iterator().next();
			boolean result = true;
			
			for(Relay relay : activeRelays) {
				//If just any relay's frontier is different from the ground truth the frontier are disaligned,
				//therefore the algorithm is faulty.
				result &= ground.checkFrontiers(relay);
			}
			
			if (result || scheduler.getTickCount() >= totalTick*20) {
				for(Relay relay : activeRelays) {
					relay.printFrontier();
				}
				System.out.println("Frontiers consistency: " + result );
				RunEnvironment.getInstance().endRun();
			}
		}
		
		//Collect all the data
		for (Relay r: activeRelays) {
			this.kbyteProcessed += r.getBytesProcessed();
			this.kbyteReceived += r.getBytesReceived();
		}
	}
	
	//The generatePerturbation simply puts "a reminder". The Wavefront will be processed by the next method, with least priority
	//on the Scheduler. This wrapper is possibly called by any Relay while some have already been executed and some still have to 
	//(Sequential execution). Having a wrapper simulates somehow a parallel implementation, where all the Relays are aware
	//of a perturbation in the same moment (even if still sequential).
	public void generatePerturbation(Relay src, Perturbation msg) {
		Wavefront probe = new Wavefront(src, msg, 0, minDistancePerTick, maxDistancePerTick, maxWavefrontLife);
		newestWavefront.add(probe);
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.LAST_PRIORITY)
	public void propagatePerturbation() {
		newestWavefront.forEach((k) -> {
			if (!activeWavefront.contains(k)) {
				activeWavefront.add(k);
				for(Relay relay : activeRelays) {
					if (!relay.equals(k.getSource())) {
						relay.addPerturbation(
							new Wavefront(k.getSource(), k.getMsg(), 0, minDistancePerTick, maxDistancePerTick, distances.get(k.getSource()).get(relay)));
						
					}	
				}
			}
		});
		newestWavefront.clear();
		
		
		//While the oldest wavefronts are removed, since they have been sensed by every Relay
		//(=have already traveled the furthest distance possibile) 
		List<Wavefront> toRemove = new ArrayList<Wavefront>();
		activeWavefront.forEach((w) -> {
			if (w.track()) {
				toRemove.add(w);
			}
		});
		
		for (Wavefront w : toRemove) {
			activeWavefront.remove(w);
		}
	
	}
	
	public String getRandomTopic() {
		return this.topics.get(RandomHelper.nextIntFromTo(0, this.topics.size()-1));
	}
	
	public Relay getRandomRelay() {
		int position = RandomHelper.nextIntFromTo(0, this.activeRelays.size()-1);
		Iterator<Relay> iter = this.activeRelays.iterator();
		Relay toRtn = iter.next();
		
		while (position > 0) {
			toRtn = iter.next();
			position--;
		}
		
		return toRtn;
	}
	
	public double getBytesReceived() {
		return this.kbyteReceived;
	}
	
	public double getBytesProcessed() {
		return this.kbyteProcessed;
	}
	
	public double getBytesUseless() {
		return this.kbyteReceived - this.kbyteProcessed;
	}
}