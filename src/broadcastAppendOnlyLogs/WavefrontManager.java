package broadcastAppendOnlyLogs;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

import java.util.Set;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;

public class WavefrontManager {
	private int totalTick;
	private int minDistancePerTick;
	private int maxDistancePerTick;
	private Double maxWavelengthLife;
	private Set<Relay> activeRelays;
	
	//contains the newly generated perturbations, which have to be initialized;
	private Map<Relay, Perturbation> newestWavefront;
	//contains the currently alive perturbations, which have already been initialized and are still traveling.
	private Set<Wavefront> activeWavefront;
	
	private ISchedule scheduler;
	private Map<Relay, Map<Relay, Double>> distances;
	
	//Standard method definition (constructor)
	public WavefrontManager(int totalTick,int minDistancePerTick, int maxDistancePerTick, Double maxWavelengthLife) {
		this.totalTick = totalTick;
		
		this.minDistancePerTick = minDistancePerTick;
		this.maxDistancePerTick = maxDistancePerTick;
		
		this.maxWavelengthLife = maxWavelengthLife;
		
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

	public void generatePerturbation(Relay src, Perturbation msg) {
		newestWavefront.put(src, msg);
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.LAST_PRIORITY)
	public void propagatePerturbation() {
		newestWavefront.forEach((k,v) -> {
			
			Wavefront probe = new Wavefront(k, v, 0, minDistancePerTick, maxDistancePerTick, maxWavelengthLife);
			
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
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.FIRST_PRIORITY)
	public void checkExecution() {
		if (scheduler.getTickCount() == (totalTick-(maxWavelengthLife))) {
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