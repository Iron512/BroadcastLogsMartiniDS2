package broadcastAppendOnlyLogs;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.WrapAroundBorders;

//This class is the whole wrapper developed as the Repast Symphony requires.
//This and all the other classes are commented to aid the understanding of my coding process,
//but the complete explanation for each of them is contained in the report.
public class BroadcastBuilder implements ContextBuilder<Object>{
	
	@Override
	public Context<Object> build(Context<Object> context) {
		return null;
	}
}