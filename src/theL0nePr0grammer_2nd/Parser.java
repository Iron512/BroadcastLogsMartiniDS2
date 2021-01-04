package theL0nePr0grammer_2nd;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import theL0nePr0grammer_2nd.relays.*;

public class Parser {
	private JSONObject json;
	
	public Parser(String filepath) {
		try {
			json = (JSONObject) new JSONParser().parse(new FileReader(filepath));
		} catch (Exception e) {
			System.out.println("Error opening file");
		} 
	}
	
	class Dimensions {
		private int xdim;
		private int ydim;
		
		public Dimensions(int x, int y) {
			this.xdim = x;
			this.ydim = y;
		}
		
		public int x() {
			return this.xdim;
		}
		
		public int y() {
			return this.ydim;
		}
	}
	
	public Dimensions getDimensions() {
		return new Dimensions(
			(int) (long) ((JSONObject) this.json.get("Dimensions")).get("X"),
			(int) (long) ((JSONObject) this.json.get("Dimensions")).get("Y"));
	}
	
	public int getDuration() {
		return (int) (long) this.json.get("Duration");
	}
	
	public String getNetwork() {
		return (String) this.json.get("Network");
	}
	
	public List<Relay> getRelaysList() {
		ArrayList<Relay> toReturn = new ArrayList<Relay>();
		
		if (json == null)
			return null;
		
		JSONArray relays = (JSONArray) json.get("FixedRelays");
		if (relays == null)
			return null;
		
		for (Object item : relays) {
			JSONObject conv =  (JSONObject) item;
			Relay generated = null;
			
			if (((String) this.json.get("Network")).equals("RelayI")) {
				generated = new RelayI(
					(int) (long) conv.get("id"), 
					(double) conv.get("X"), 
					(double) conv.get("Y"), 
					(double) conv.get("PerturbationProb"), 
					(double) conv.get("Latency"));
			} else if (((String) this.json.get("Network")).equals("RelayII")) {
				generated = new RelayII(
					(int) (long) conv.get("id"), 
					(double) conv.get("X"), 
					(double) conv.get("Y"), 
					(double) conv.get("PerturbationProb"), 
					(double) conv.get("Latency"),
					(double) conv.get("Jitter"),
					(double) conv.get("SpawnProb"),
					(double) conv.get("LeaveProb"));
			} else if (((String) this.json.get("Network")).equals("RelayIII")) {
				generated = new RelayIII(
						(int) (long) conv.get("id"), 
						(double) conv.get("X"), 
						(double) conv.get("Y"), 
						(double) conv.get("PerturbationProb"), 
						(double) conv.get("Latency"),
						(double) conv.get("Jitter"),
						(double) conv.get("SpawnProb"),
						(double) conv.get("LeaveProb"),
						(double) conv.get("LossProb"));
			} else {
				//Something wrong, this field should be either "RelayI, II or III"
				return null;
			}
			
			if (! toReturn.contains(generated))
				toReturn.add(generated);		
		}
		return toReturn;
	}
	
}
