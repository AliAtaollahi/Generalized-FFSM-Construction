package uk.le.ac.fts;

import org.w3c.dom.Node;

public class FtsTransition{
	
	String origin, target;
	String fexpression;
	String action;

	public FtsTransition(String origin, String target, String fexpression, String action) {
		this.origin = origin;
		this.target = target;
		this.fexpression = fexpression;
		this.action = action;
	}
	
	public FtsTransition(Node origin, Node fexpression, Node action, Node target) {
		this.origin = origin.getTextContent();
		this.target = target.getTextContent();
		this.fexpression = fexpression != null? fexpression.getTextContent() : "(True)";
		this.action = action.getTextContent();
	}
	
	public String getOrigin() { return origin; }
	public String getTarget() { return target; }
	
	public String getFexpression() { return fexpression; }
	public String getAction() { return action; }

}