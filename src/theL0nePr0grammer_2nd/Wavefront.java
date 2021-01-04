package theL0nePr0grammer_2nd;

import repast.simphony.engine.environment.RunEnvironment;
import theL0nePr0grammer_2nd.perturbations.Perturbation;
import theL0nePr0grammer_2nd.relays.Relay;

public class Wavefront {
	private Relay source; //The source of the Wavefront is different from the source of the Perturbation (as perturbation are usually forwarded)
	private Perturbation msg;
	
	private double msgBirth;
	private double msgLife;
	
	public Wavefront(Relay source, Perturbation msg, double msgBirth) {
		this.source = source;
		this.msg = msg;
		
		this.msgBirth = msgBirth;
		this.msgLife = source.getMessageLife();
	}
	
	public Relay getSource() {
		return this.source;
	}
	
	public Perturbation getMsg() {
		return this.msg;
	}
	
	public Perturbation live() {
		if (RunEnvironment.getInstance().getCurrentSchedule().getTickCount() > this.msgBirth+this.msgLife) {
			return this.msg;
		}
		return null;
	}
}
