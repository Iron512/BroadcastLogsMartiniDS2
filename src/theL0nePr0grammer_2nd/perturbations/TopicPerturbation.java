package theL0nePr0grammer_2nd.perturbations;

import theL0nePr0grammer_2nd.relays.Relay;

public class TopicPerturbation extends Perturbation {
	String topic;
		
	public TopicPerturbation(Relay source, int ref, Object msg, String topic) {
		super(source, ref, 0);
		this.value = msg;
		this.topic = topic;
	}
	
	public String getTopic() {
		return this.topic;
	}
}

