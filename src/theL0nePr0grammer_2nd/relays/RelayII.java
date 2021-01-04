package theL0nePr0grammer_2nd.relays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repast.simphony.random.RandomHelper;
import theL0nePr0grammer_2nd.perturbations.AddressedPerturbation;
import theL0nePr0grammer_2nd.perturbations.BroadcastPerturbation;
import theL0nePr0grammer_2nd.perturbations.Perturbation;
import theL0nePr0grammer_2nd.perturbations.TopicPerturbation;

public class RelayII extends Relay{
	private Map<Relay, Integer> frontier;
	private List<Perturbation> bag;
	
	public RelayII(int id, double xpos, double ypos, double perturbationProb, double latency, double jitter, double spawnProb, double leaveProb) {
		super(id, xpos, ypos, perturbationProb, true, latency, jitter, spawnProb, leaveProb, 0.0);
		
		this.frontier = new HashMap<Relay,Integer>();
		this.bag =  new ArrayList<Perturbation>();
	}
	
	private void sendMessage(Perturbation p) {	
		this.aether.sendWavefront(this, p);
		this.frontier.replace(this, nextRef(p));
	}
	
	@Override
	public void sendMessageBroadcast(Object value) {
		if (!this.frontier.containsKey(this))
			this.frontier.put(this, nextRef(null));
		
		Perturbation msg = new BroadcastPerturbation(this, this.frontier.get(this), value);
		this.sendMessage(msg);
	}
	
	@Override
	public void sendMessageAddressed(Object value, List<Relay> dest) {
		if (!this.frontier.containsKey(this))
			this.frontier.put(this, nextRef(null));
		
		Perturbation msg = new AddressedPerturbation(this, this.frontier.get(this), value, dest);
		this.sendMessage(msg);
	}
	
	@Override
	public void sendMessageTopic(Object value, String topic) {
		if (!this.frontier.containsKey(this))
			this.frontier.put(this, nextRef(null));
		
		Perturbation msg = new TopicPerturbation(this, this.frontier.get(this), value, topic);
		this.sendMessage(msg);
	}

	@Override
	public void onSense(Perturbation p) {
		if (!p.getSource().equals(this)) {
			if (!this.frontier.containsKey(p.getSource()))
				//this.frontier.put(p.getSource(), 1);
				this.frontier.put(p.getSource(), p.getRef());
				
			if (p.getRef() >= this.frontier.get(p.getSource()) && !bag.contains(p)) {
				bag.add(p);
				
				boolean changes = true;
				while (!bag.isEmpty() && changes) {
					changes = false;
					
					List<Perturbation> toRemove = new ArrayList<Perturbation>();
					for (Perturbation q : bag) {
						if (frontier.get(q.getSource()) == q.getRef()) {
							aether.sendWavefront(this, q);
							this.frontier.replace(q.getSource(), nextRef(q));
							toRemove.add(q);
							changes = true;
							
							if (p instanceof AddressedPerturbation) {
								if (((AddressedPerturbation) p).destinated(this))
									System.out.println("Recived a direct message from " + p.getSource());
							} else if (p instanceof TopicPerturbation) {
								if (this.topics.contains(((TopicPerturbation) p).getTopic())) {
									System.out.println("Recived a message on topic " + ((TopicPerturbation)p).getTopic() + " (from " + p.getSource() + ")");
								}
							}
						}
					}
					
					for (Perturbation r : toRemove) {
						bag.remove(r);
					}
				}
			}	
		}
	} 
	
	@Override
	public boolean compareStatus(Relay cmp) {
		RelayII conv = (RelayII) cmp;
		for (Map.Entry<Relay, Integer> entry : this.frontier.entrySet()) {
			if (conv.frontier.containsKey(entry.getKey()))
				if ((int) entry.getValue() != (int) conv.frontier.get(entry.getKey())) {
					return false;
				}
		}
		return true;
	}
	
	@Override
	public void printStatus() {
		System.out.println(this);
		frontier.forEach((r,m) -> {
			System.out.println(r + ": " + m);
		});
		System.out.println();
	}

	@Override
	public void join() {
		this.active = true;
		int recover = -1;
		if (this.frontier.containsKey(this))
			recover = this.frontier.get(this);
		this.frontier.clear();
		if (recover != -1)
			this.frontier.put(this, recover);
		this.bag.clear();
	}

	@Override
	public void leave() {
		this.active = false;
		this.incomingWavefronts.clear();
	}
}
