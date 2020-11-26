package broadcastAppendOnlyLogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bsh.This;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
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
import repast.simphony.util.ContextUtils;

//This class is an artifice developed to simulate the "medium" where perturbation are transfered.
//It acts as a network for the other Relays, which doesn't have any clue on the topology nor the members.
//For this reason, is charged to end the run of the simulation when needed.

//Since going further with the project required this manager to check more and more function i decided to 
//using it also for generating the Context. This involves adding complexity to this code, but from the programming logic point of view
//I believe this one is the most consistent. I hope that the comments i am writing are enough.
public class WavefrontManager implements ContextBuilder<Object>{
	private Context<Object> context;
	ContinuousSpace<Object> space; 
	
	private int totalTick;
	private int minDistancePerTick;
	private int maxDistancePerTick;
	private double maxWavefrontLife;
	
	private int nodeIndex;
	private int nodeMinCount;
	private int nodeMaxCount;
	
	private boolean dynamicity;
	private double spawnProb;
	private double leaveProb;
	private double pertGen;
	
	private Set<Relay> activeRelays;
	private Map<Relay, Perturbation> newestWavefront; //contains the newly generated (during the current tick) perturbations,
	//which have still to be initialized
	private Set<Wavefront> activeWavefront; //contains the currently alive perturbations,
	//which have already been initialized and are still traveling. Useful in the presence of a dynamic network
	
	private ISchedule scheduler;
	private Map<Relay, Map<Relay, Double>> distances; //Its a full symmetrical matrix containing the distances
	//between each member of the network. Gets updated each time a new node enters in the game
	
	
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
		
		this.totalTick = params.getInteger("totalTick"); //The time that the simulation should run
		this.minDistancePerTick = params.getInteger("minDistancePerTick"); //defines how many distance a perturbation travels during a tick.
		this.maxDistancePerTick = params.getInteger("maxDistancePerTick");
		this.maxWavefrontLife = Math.sqrt((dstY*dstY) + (dstX*dstX)) + 1.0; //It briefly calculates the furthest distance that a Wavelength
		//can travel (the maximum life that can be scored). Even in the worst case (with two Relays in the opposite angles of the square)
		//no distance will be furthest than this.
		
		this.nodeIndex = 0;
		this.nodeMinCount = params.getInteger("nodeMinCount");	//defines the number of nodes acting in the scene
		this.nodeMaxCount = params.getInteger("nodeMaxCount");	
		
		this.dynamicity =  true;
		this.spawnProb = params.getDouble("spawnProb"); //probability of joining or leaving the network for each node/new node
		this.leaveProb = params.getDouble("leftProb");
		this.pertGen = params.getDouble("pertGen"); //defines the probability of each relay of generating a perturbation
	
		//All the required data structures are initialized
		this.activeRelays = new HashSet<Relay>();
		
		this.newestWavefront = new HashMap<Relay, Perturbation>();
		this.activeWavefront = new HashSet<Wavefront>();
		
		this.scheduler = RunEnvironment.getInstance().getCurrentSchedule();
		this.distances = new HashMap<Relay, Map<Relay, Double>>();
		


		this.context = context;
		
		//Initialize nodes
		int activeNodes = RandomHelper.nextIntFromTo(nodeMinCount, nodeMaxCount);
		for (nodeIndex = 0; nodeIndex < activeNodes; nodeIndex++) {
			this.addRelay(new Relay(space, this, nodeIndex, pertGen));
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
		this.context.remove(item);
		if (result) {
			distances.forEach((k,v) -> {
				if (k != item) {
					v.remove(item);
				}
			});
			distances.remove(item);
		}
		return result;
	}

	//The generatePerturbation simply puts "a reminder". The perturbation will be processed by the next method, with least priority
	//on the Scheduler. This wrapper is possibly called by any Relay while some have already been executed and some still have to 
	//(Sequential execution). Having a wrapper simulates somehow a parallel implementation, where all the Relays are aware
	//of a perturbation in the same moment (still sequential).
	public void generatePerturbation(Relay src, Perturbation msg) {
		newestWavefront.put(src, msg);
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.LAST_PRIORITY)
	public void propagatePerturbation() {
		//The new perturbations are encapsulated in the wavefront
		newestWavefront.forEach((k,v) -> {
			Wavefront probe = new Wavefront(k, v, 0, minDistancePerTick, maxDistancePerTick, maxWavefrontLife);
			if (!activeWavefront.contains(probe)) {
				for(Relay relay : activeRelays) {
					if (!relay.equals(k)) {
						relay.addPerturbation(
							new Wavefront(k, v, 0, minDistancePerTick, maxDistancePerTick, distances.get(k).get(relay)));
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
	
	//This method ensures the correct ending procedure to be activated before stoping the run.
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.FIRST_PRIORITY)
	public void checkExecution() {
		if (this.dynamicity && RandomHelper.nextDoubleFromTo(0.0, 1.0) > (1.0-this.spawnProb) && this.activeRelays.size() < this.nodeMaxCount) {
			this.addRelay(new Relay(this.space, this, nodeIndex++, pertGen));
			System.out.println("Added");
		}
		
		if (this.dynamicity && RandomHelper.nextDoubleFromTo(0.0, 1.0) > (1.0-this.leaveProb) && this.activeRelays.size() > this.nodeMinCount) {
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
		
		if (scheduler.getTickCount() == (int)(totalTick-(maxWavefrontLife/minDistancePerTick))) {
			//Stop the generation of messages, to allow the wavefront to syncronize
			this.dynamicity = false;
			for(Relay relay : activeRelays) {
				relay.stopPerturbations();
			}
		}
		
		if (scheduler.getTickCount() >= totalTick) {
			//take one random relay as the ground truth for our frontier.
			Relay ground = activeRelays.iterator().next();
			boolean result = true;
			
			for(Relay relay : activeRelays) {
				relay.printFrontier();
				//If just any relay's frontier is different from the ground truth the frontier are disaligned,
				//therefore the algorithm is faulty.
				result &= ground.checkFrontiers(relay);
			}
			System.out.println("Result: " + result);
			RunEnvironment.getInstance().endRun();
		}
	}
}