package theL0nePr0grammer_2nd.perturbations;

import theL0nePr0grammer_2nd.relays.Relay;

public class ARQPerturbation extends Perturbation{
	public ARQPerturbation(Relay source, Relay reqSource, int reqRel) {
		super(source, reqRel, reqSource);		
	}
}
