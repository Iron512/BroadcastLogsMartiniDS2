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

//This class is the whole wrapper developed as the Repast Symphony requires.
//This and all the other classes are commented to aid the understanding of my coding process,
//but the complete explanation for each of them is contained in the report.
public class BroadcastBuilder implements ContextBuilder<Object>{
	
	@Override
	public Context<Object> build(Context<Object> context) {
		int dstX = 50;
		int dstY = 50;
		context.setId("BroadcastAppendOnlyLogs");
		
		//The first step is the definition of the space. I will stick to the well-known continuous space from tutorials,
		//but I will drop the discrete grid. If i have the correct insight, this component wont be necessary.
		//EDIT: The insight I got, for how i developed the whole simulator, was right.
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		//I will keep the standard dimension 50,50 for the space
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace(
				"space",context,new RandomCartesianAdder<Object>(),new WrapAroundBorders(), dstX, dstY);
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		
		int totalTick = params.getInteger("totalTick"); //The time that the simulation should run
		int nodeCount = params.getInteger("nodeCount");	//defines the number of nodes acting in the scene
		int minDistancePerTick = params.getInteger("minDistancePerTick");
		int maxDistancePerTick = params.getInteger("maxDistancePerTick");
		double pertGen = params.getInteger("pertGen"); //defines the probability of each relay of generating a perturbation
	
		
		//It briefly calculates the furthest distance that a Wavelength can travel (the maximum life that can be scored)
		//Even in the worst case (with two Relays in the opposite angles of the square) no distance will be furthest than:
		Double maxLife = Math.sqrt((dstY*dstY) + (dstX*dstX)) + 1.0;
		
		WavefrontManager aether = new WavefrontManager(totalTick, minDistancePerTick, maxDistancePerTick, maxLife);
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