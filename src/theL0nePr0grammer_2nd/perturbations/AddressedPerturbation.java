package theL0nePr0grammer_2nd.perturbations;

import java.util.List;

import theL0nePr0grammer_2nd.relays.Relay;

public class AddressedPerturbation extends Perturbation {
	List<Relay> dest;
	
	public AddressedPerturbation(Relay source, int ref, Object msg, List<Relay> dest) {
		super(source, ref, 0);
		this.value = msg;
		this.dest = dest;
	}

	public boolean destinated(Relay target) {
		return this.dest.contains(target);
	}
}
