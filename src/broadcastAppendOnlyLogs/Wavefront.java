package broadcastAppendOnlyLogs;

import java.util.Objects;

public class Wavefront {
	private Relay generator;
	private Perturbation msg;
	private int msgLife;
	private Double msgDistance;

	//Standard method definition (constructor, equals/hashcode, toString)
	public Wavefront(Relay generator, Perturbation msg, int msgLife, Double msgDistance) {
		this.generator = generator;
		this.msg = msg;
		this.msgLife = msgLife;
		this.msgDistance = msgDistance;
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wavefront)) return false;
        Wavefront cmp = (Wavefront) o;
        return generator.equals(cmp.generator) && msg.equals(cmp.msg);
    }
	@Override
	public int hashCode() {
		return Objects.hash(generator, msg);
	}
	@Override
	public String toString() {
		return "Wavefront (" + Integer.toString(msgLife) + "/" + Double.toString(msgDistance) + ") of " + msg.toString();
	}
	
	//Custom methods
	public Perturbation live() {
		this.msgLife++;
		if(this.msgLife >= this.msgDistance) {
			//message has to be delivered
			//(Here the perturbation should be destroyed)
			return msg;
		} else {
			//still traveling, let the perturbation continue
			return null;
		}
	}
}