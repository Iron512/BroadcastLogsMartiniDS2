package broadcastAppendOnlyLogs;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.WrapAroundBorders;

public class BroadcastBuilder implements ContextBuilder<Object>{

	@Override
	public Context build(Context<Object> context) {
		int dstX = 50;
		int dstY = 50;
		
		context.setId("BroadcastAppendOnlyLogs");
		
		//The first step is the definition of the space. I will stick to the well-known continuous space from tutorials,
		//but I will drop the discrete grid. If i have the correct insight, this component wont be necessary.
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		//I will keep the standard dimension 50,50 for the space
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space",context,new RandomCartesianAdder<Object>(),new WrapAroundBorders(), dstX, dstY);
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		//defines the number of nodes acting in the scene
		int nodeCount = params.getInteger("nodeCount");
		//defines the distance traveled by any perturbation during a  tick.
		//This should allow me to simulate a realistic environment, since the conversion between ticks and second
		//is almost trivial. Also the mapping into the 50x50 space is easily done. 
		int dstPerTick = params.getInteger("dstPerTick");
		//defines the probability of each relay of generating a perturbation
		int pertGen = params.getInteger("pertGen");
	
		Double maxLife = 0.0;
		if (dstY > dstX) {
			maxLife = dstY + 1.0;
		} else {
			maxLife = dstX + 1.0;
		}
		
		PerturbationManager aether = new PerturbationManager(dstPerTick, maxLife);
		context.add(aether);
		
		for (int i = 0; i < nodeCount; i++) {
			Relay item = new Relay(space, aether, i, pertGen);
			
			//when relays are added, they must be added both in the context and in the relay manager
			context.add(item);
			aether.addRelay(item);
		}
		
		return context;
	}
}