package broadcastAppendOnlyLogs;

import java.util.Objects;

public class Perturbation {
	private Relay generator;
	private Message msg;
	private int msgLife;
	private Double msgDistance;

	//Standard method definition (constructor, equals/hashcode, toString)
	public Perturbation(Relay generator, Message msg, int msgLife, Double msgDistance) {
		this.generator = generator;
		this.msg = msg;
		this.msgLife = msgLife;
		this.msgDistance = msgDistance;
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Perturbation)) return false;
        Perturbation cmp = (Perturbation) o;
        return generator.equals(cmp.generator) && msg.equals(cmp.msg);
    }
	@Override
	public int hashCode() {
		return Objects.hash(generator, msg);
	}
	@Override
	public String toString() {
		return "perturbation (" + Integer.toString(msgLife) + "/" + Double.toString(msgDistance) + ") of " + msg.toString();
	}
	
	//Custom methods
	public Message live() {
		this.msgLife++;
		if(this.msgLife >= this.msgDistance) {
			//message has to be delivered
			//Here the perturbation should be destroyed
			return msg;
		} else {
			//still traveling, let the perturbation continue
			return null;
		}
	}
}