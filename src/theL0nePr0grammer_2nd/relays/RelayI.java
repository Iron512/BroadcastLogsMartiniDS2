package theL0nePr0grammer_2nd.relays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.filter.expression.ThisPropertyAccessorFactory;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import theL0nePr0grammer_2nd.perturbations.*;

public class RelayI extends Relay{
	private Map<Relay, Integer> frontier;
	
	public RelayI(int id, double xpos, double ypos, double perturbationProb, double latency) {
		super(id, xpos, ypos, perturbationProb, true, latency, 0.0, 0.0, 0.0, 0.0);
		
		this.frontier = new HashMap<Relay,Integer>();
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
				this.frontier.put(p.getSource(), 1);
				
			if (this.frontier.get(p.getSource()) == p.getRef()) {
				aether.sendWavefront(this, p);
				this.frontier.replace(p.getSource(), nextRef(p));
				
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
	} 
	
	@Override
	public boolean compareStatus(Relay cmp) {
		RelayI conv = (RelayI) cmp;
		for (Map.Entry<Relay, Integer> entry : this.frontier.entrySet()) {
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
		//Do nothing. RelayI live in static networks
	}

	@Override
	public void leave() {
		//Do nothing. RelayI live in static networks
	}
}
