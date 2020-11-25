package broadcastAppendOnlyLogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;

//This class is an artifice developed to simulate the "medium" where perturbation are transfered.
//It acts as a network for the other Relays, which doesn't have any clue on the topology nor the members.
//For this reason, is charged to end the run of the simulation when needed.
public class WavefrontManager {
	private int totalTick;
	private int minDistancePerTick;
	private int maxDistancePerTick;
	private Double maxWavefrontLife; 
	private Set<Relay> activeRelays;
	
	
	private Map<Relay, Perturbation> newestWavefront; //contains the newly generated (during the current tick) perturbations,
	//which have still to be initialized
	private Set<Wavefront> activeWavefront; //contains the currently alive perturbations,
	//which have already been initialized and are still traveling. Useful in the presence of a dynamic network
	
	private ISchedule scheduler;
	private Map<Relay, Map<Relay, Double>> distances; //Its a full symmetrical matrix containing the distances
	//between each member of the network
	
	//Standard method definition (constructor)
	public WavefrontManager(int totalTick,int minDistancePerTick, int maxDistancePerTick, Double maxWavefrontLife) {
		this.totalTick = totalTick;
		
		this.minDistancePerTick = minDistancePerTick;
		this.maxDistancePerTick = maxDistancePerTick;
		
		this.maxWavefrontLife = maxWavefrontLife;
		
		this.activeRelays = new HashSet<Relay>();
		
		this.newestWavefront = new HashMap<Relay, Perturbation>();
		this.activeWavefront = new HashSet<Wavefront>();
		
		this.distances = new HashMap<Relay, Map<Relay, Double>>();
		this.scheduler = RunEnvironment.getInstance().getCurrentSchedule();
	}
	
	//Custom methods
	public boolean addRelay(Relay item) {
		//Relays MUST also be added in the context
		boolean result = activeRelays.add(item);

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
		if (scheduler.getTickCount() == (int)(totalTick-(maxWavefrontLife/minDistancePerTick))) {
			//Stop the generation of messages, to allow the wavefront to syncronize
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