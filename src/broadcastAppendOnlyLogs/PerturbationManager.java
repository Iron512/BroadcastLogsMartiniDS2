package broadcastAppendOnlyLogs;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

import java.util.Set;

import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;

public class PerturbationManager {
	private int dstPerTick;
	private Double pertMaxLife;
	private Set<Relay> activeRelays;
	
	//contains the newly generated perturbations, which have to be initialized;
	private Map<Relay, Message> newestPerturbation;
	//contains the currently alive perturbations, which have already been initialized and are still traveling.
	private Set<Perturbation> activePerturbation;
	
	private Map<Relay, Map<Relay, Double>> distances;
	
	//Standard method definition (constructor)
	public PerturbationManager(int dstPerTick, Double pertMaxLife) {
		this.dstPerTick = dstPerTick;
		this.pertMaxLife = pertMaxLife;
		
		this.activeRelays = new HashSet<Relay>();
		
		this.newestPerturbation = new HashMap<Relay, Message>();
		this.activePerturbation = new HashSet<Perturbation>();
		
		this.distances = new HashMap<Relay, Map<Relay, Double>>();
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

	public void generatePerturbation(Relay src, Message msg) {
		newestPerturbation.put(src, msg);
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.LAST_PRIORITY)
	public void propagatePerturbation() {
		newestPerturbation.forEach((k,v) -> {
			
			Perturbation probe = new Perturbation(k, v, 0, pertMaxLife);
			
			if (!activePerturbation.contains(probe)) {
				
				for(Relay relay : activeRelays) {
					if (!relay.equals(k)) {
						relay.addPerturbation(new Perturbation(k, v, 0, distances.get(k).get(relay)/dstPerTick));
					}	
				}
			}
		});
		
		newestPerturbation.clear();
	}
}