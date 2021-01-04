package theL0nePr0grammer_2nd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.WrapAroundBorders;
import repast.simphony.util.SimUtilities;
import theL0nePr0grammer_2nd.perturbations.Perturbation;
import theL0nePr0grammer_2nd.relays.*;

public class Manager implements ContextBuilder<Object>{
	private ISchedule scheduler;
	
	private int x;
	private int y;
	private int totalTick;
	
	private List<Relay> relays;
	private List<String> topics;
	private List<Wavefront> newestWavefronts;
	
	@Override
	public Context build(Context<Object> context) {
		context.setId("TheL0nePr0grammer_2nd");
		Parser config = new Parser("src/config/config_3.json");
		
		this.x = config.getDimensions().x();
		this.y = config.getDimensions().y();
		this.totalTick = config.getDuration();
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", context, new RandomCartesianAdder<Object>(), new WrapAroundBorders(), this.x, this.y);
		
		this.topics = new ArrayList<String>();
		this.topics.add("first");
		this.topics.add("second");
		this.topics.add("third");
		
		this.topics.add("fourth");
		this.topics.add("fifth");
		this.topics.add("sixth");
		
		this.topics.add("seventh");
		this.topics.add("eigtht");
		this.topics.add("nineth");
		
		this.relays = config.getRelaysList();
		this.newestWavefronts = new ArrayList<Wavefront>();
		
		for (Relay item : relays) {
			context.add(item);
			item.setContext(this, space);
			space.moveTo(item, item.getX(), item.getY());
		
			SimUtilities.shuffle(topics,RandomHelper.getUniform());
			int count = RandomHelper.nextIntFromTo(0,3);
			
			for (int i = 0; i < count; i++) {
				item.addTopic(topics.get(i));
			}
		}
		
		NetworkBuilder<Object> broadcast = new NetworkBuilder<Object>("broadcastNet", context, true);
		broadcast.buildNetwork();
		NetworkBuilder<Object> direct = new NetworkBuilder<Object>("directNet", context, true);
		direct.buildNetwork();
		NetworkBuilder<Object> pubsub = new NetworkBuilder<Object>("pubNet", context, true);
		pubsub.buildNetwork();
		
		NetworkBuilder<Object> ARQms = new NetworkBuilder<Object>("arqNetwork", context, true);
		ARQms.buildNetwork();
		
		this.scheduler = RunEnvironment.getInstance().getCurrentSchedule();
		context.add(this);
		
		return context;
	}

	public void sendWavefront(Relay source, Perturbation msg) {
		this.newestWavefronts.add(new Wavefront (source, msg, 0));
	}

	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.LAST_PRIORITY)
	public void propagateWavefronts() {
		this.newestWavefronts.forEach((w) -> {
			for (Relay d : this.relays) {
				if (d.getActive() && !d.equals(w.getSource())) {
					d.addWavefront(new Wavefront(w.getSource(), w.getMsg(), this.scheduler.getTickCount()));
				}
			}
		});
		
		this.newestWavefronts.clear();
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = ScheduleParameters.FIRST_PRIORITY)
	public void run() {
		if (scheduler.getTickCount() >= totalTick) {	
			List<Relay> filtered = new ArrayList<Relay>();
			
			for (Relay r : relays) {
				if (r.getActive())
					filtered.add(r);
			}
			
			boolean result = true;
			
			for (Relay r : filtered) {
				r.end();
				for (Relay s : filtered) 
					result &= r.compareStatus(s);
			}
			
			if (result || scheduler.getTickCount() >= totalTick*5) {
				for (Relay r : filtered) {
					r.printStatus();
				}
				RunEnvironment.getInstance().endRun();
			}
		}
	}
	
	public Relay getRandomRelay(Relay r) {
		SimUtilities.shuffle(relays,RandomHelper.getUniform());
		return relays.get(0);
	} 
	
	public String getRandomTopic() {
		SimUtilities.shuffle(topics,RandomHelper.getUniform());
		return topics.get(0);
	}
}
