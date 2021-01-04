package theL0nePr0grammer_2nd.relays;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.ContextUtils;
import theL0nePr0grammer_2nd.perturbations.ARQPerturbation;
import theL0nePr0grammer_2nd.perturbations.AddressedPerturbation;
import theL0nePr0grammer_2nd.perturbations.BroadcastPerturbation;
import theL0nePr0grammer_2nd.perturbations.Perturbation;
import theL0nePr0grammer_2nd.perturbations.TopicPerturbation;

public class RelayIII extends Relay{
	private Map<Relay, List<Perturbation>> log;
	
	public RelayIII(int id, double xpos, double ypos, double perturbationProb, double latency, double jitter, double spawnProb, double leaveProb, double lossProb) {
		super(id, xpos, ypos, perturbationProb, true, latency, jitter, spawnProb, leaveProb, lossProb);
		
		log =  new HashMap<Relay, List<Perturbation>>();
	}

	private void sendMessage(Perturbation p) {		
		this.aether.sendWavefront(this, p);
		this.log.get(this).add(p);
	}
	
	@Override
	public void sendMessageBroadcast(Object value) {
		if (!this.log.containsKey(this)) {
			this.log.put(this, new ArrayList<Perturbation>());
			this.log.get(this).add(null);
		}
		
		Perturbation msg = new BroadcastPerturbation(this, nextRef(this.log.get(this).get(this.log.get(this).size()-1)), value);
		this.sendMessage(msg);
	}
	
	@Override
	public void sendMessageAddressed(Object value, List<Relay> dest) {
		if (!this.log.containsKey(this)) {
			this.log.put(this, new ArrayList<Perturbation>());
			this.log.get(this).add(null);
		}
		
		Perturbation msg = new AddressedPerturbation(this, nextRef(this.log.get(this).get(this.log.get(this).size()-1)), value, dest);
		this.sendMessage(msg);
	}
	@Override
	public void sendMessageTopic(Object value, String topic) {
		if (!this.log.containsKey(this)) {
			this.log.put(this, new ArrayList<Perturbation>());
			this.log.get(this).add(null);
		}
		
		Perturbation msg = new TopicPerturbation(this, nextRef(this.log.get(this).get(this.log.get(this).size()-1)), value, topic);
		this.sendMessage(msg);
	}
	

	@Override
	public void onSense(Perturbation p) {
		if (p instanceof ARQPerturbation)
			onSenseARQ(p);
		else 
			onSenseStandard(p);
	}
	
    public String getLabel() {
    	DecimalFormat df2 = new DecimalFormat("#.##");
    	
    	String latest;
    	if (this.log.containsKey(this))
    		latest = "(" + Integer.toString(this.log.get(this).get(this.log.get(this).size()-1).getRef()) + ")";
    	else
    		latest = "";
    		
    	return this.toString() + "\n" + df2.format(this.getX()) + ", " + df2.format(this.getY()) + latest ;
    }
	
	public void onSenseARQ(Perturbation p) {
		Relay req = (Relay) p.getValue();
		int rref = p.getRef();
		
		if (this.log.containsKey(req)) {
			for (Perturbation elem : this.log.get(req)) {
				if (elem != null && elem.getRef() == rref) 
					this.aether.sendWavefront(this, elem);
			}
		}
	}
	
	public void onSenseStandard(Perturbation p) {
		if (!this.log.containsKey(p.getSource())) {
			this.log.put(p.getSource(), new ArrayList<Perturbation>());
			this.log.get(p.getSource()).add(null);
		}
		
		if (nextRef(this.log.get(p.getSource()).get(this.log.get(p.getSource()).size()-1)) == p.getRef()) {
			this.aether.sendWavefront(this, p);
			this.log.get(p.getSource()).add(p);
		
			

			if (p instanceof AddressedPerturbation) {
				if (((AddressedPerturbation) p).destinated(this)) {
					System.out.println("Recived a direct message from " + p.getSource());
					
					Network<Object> net = (Network<Object>) ContextUtils.getContext(this).getProjection("directNet");
					for (RepastEdge e : net.getEdges(this)) {
						if (e.getSource().equals(this))
							net.removeEdge(e);
					}
					net.addEdge(p.getSource(), this);
				}
			} else if (p instanceof TopicPerturbation) {
				if (this.topics.contains(((TopicPerturbation) p).getTopic())) {
					System.out.println("Recived a message on topic " + ((TopicPerturbation)p).getTopic() + " (from " + p.getSource() + ")");
					
					Network<Object> net = (Network<Object>) ContextUtils.getContext(this).getProjection("pubNet");
					for (RepastEdge e : net.getEdges(this)) {
						if (e.getSource().equals(this))
							net.removeEdge(e);
					}
					net.addEdge(p.getSource(), this);
				}
			} else {
				Network<Object> net = (Network<Object>) ContextUtils.getContext(this).getProjection("broadcastNet");
				for (RepastEdge e : net.getEdges(this)) {
					if (e.getSource().equals(this))
						net.removeEdge(e);
				}
				net.addEdge(p.getSource(), this);
			}
			
			
		}
	}

	@Override
	public boolean compareStatus(Relay cmp) {
		RelayIII conv = (RelayIII) cmp;
		for (Map.Entry<Relay, List<Perturbation>> entry : this.log.entrySet()) {
			if ((int) entry.getValue().get(entry.getValue().size()-1).getRef() != (int) conv.log.get(entry.getKey()).get(conv.log.get(entry.getKey()).size()-1).getRef()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void printStatus() {
		// TODO Auto-generated method stub		
		System.out.println(this);
		this.log.forEach((r,m) -> {
			System.out.println(r + ": " + m.get(m.size()-1).getRef());
		});
		System.out.println();
	}

	@Override
	public void join() {
		this.active = true;
	}

	@Override
	public void leave() {
		this.active = false;
		this.incomingWavefronts.clear();
	}

	@ScheduledMethod(start = 100, interval = 100)
	public void ARQ() {
		this.log.forEach((r, l) -> {
			Perturbation arqm = new ARQPerturbation((Relay) this, r, nextRef(l.get(l.size()-1)));
			this.aether.sendWavefront(this, arqm);
		});
	}
	
}
