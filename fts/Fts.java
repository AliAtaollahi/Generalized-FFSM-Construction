package uk.le.ac.fts;

import java.util.ArrayList;
import java.util.List;

public class Fts {
	String init;
	List<FtsTransition> transitions;
	
	public Fts(String init) {
		this.init = init;
		this.transitions = new ArrayList<>();
	}

	public String getInit() { return init; }
	public List<FtsTransition> getTransitions() { return transitions; }
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (FtsTransition tr : transitions) {
			sb.append(tr.getOrigin());
			sb.append(" -- ");
			
			sb.append(tr.getAction());
			sb.append("@[");
			sb.append(tr.getFexpression());
			sb.append("]");
			sb.append(" / 0");
			
			sb.append(" -> ");
			sb.append(tr.getTarget());
			sb.append("\n");
		}
		return sb.toString();
	}
	
}