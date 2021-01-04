package theL0nePr0grammer_2nd.perturbations;

import java.util.Objects;

import theL0nePr0grammer_2nd.relays.Relay;

public abstract class Perturbation {
	protected Relay source;
	protected int ref;
	protected Object value;
	
	//CONSTRUCTOR
		public Perturbation(Relay source, int ref, Object value) {
			this.source = source;
			this.ref = ref;
			this.value = value;
		}
		
		//STANDARD METHODS
		@Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof Perturbation)) return false;
	        Perturbation cmp = (Perturbation) o;
	        return ref == cmp.ref && source == cmp.source;
	    }
		@Override
		public int hashCode() {
			return Objects.hash(source, ref);
		}
		@Override
		public String toString() {
			return "pertubation " + source.toString() + " - " + Integer.toString(ref) + "(val:" + value.toString() + ")";
		}
		
		//GETTERS (NO SETTERS)
		public Relay getSource() {
			return this.source;
		}
		
		public int getRef() {
			return this.ref;
		}
		
		public Object getValue() {
			return this.value;
		}
}
