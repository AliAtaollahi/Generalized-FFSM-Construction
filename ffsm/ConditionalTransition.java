package uk.le.ac.ffsm;

import org.prop4j.Node;
import org.prop4j.NodeReader;

import net.automatalib.automata.transducers.impl.MealyTransition;

public class ConditionalTransition<I,O> extends MealyTransition<ConditionalState<ConditionalTransition<I,O>>, O> {

	NodeReader nodeReader = new NodeReader();
    private Node condition;
    private ConditionalState<ConditionalTransition<I,O>> predecessor;
	private I input;
	

    public ConditionalTransition(ConditionalState<ConditionalTransition<I,O>> origin, I in, ConditionalState<ConditionalTransition<I,O>> successor, O output, Node cond) {
        super(successor, output);
        this.predecessor = origin;
        this.input =  in;
        this.condition = cond;
    }
    
    public ConditionalTransition(ConditionalState<ConditionalTransition<I,O>> successor, O output) {
        super(successor, output);
		nodeReader.activateTextualSymbols();
		setCondition(nodeReader.stringToNode("(TRUE)"));
    }

    public Node getCondition() {
		return condition;
	}
    
    public void setCondition(Node condition) {
		this.condition = condition;
	}
    
    public ConditionalState<ConditionalTransition<I, O>> getPredecessor() {
		return predecessor;
	}
    
    public void setPredecessor(ConditionalState<ConditionalTransition<I, O>> predecessor) {
		this.predecessor = predecessor;
	}
    
    public I getInput() {
		return input;
	}
    
    public void setInput(I input) {
		this.input = input;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		result = prime * result + ((nodeReader == null) ? 0 : nodeReader.hashCode());
		result = prime * result + ((predecessor == null) ? 0 : predecessor.hashCode());
		return result;
	}

	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConditionalTransition<I,O> other = (ConditionalTransition<I,O>) obj;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		if (nodeReader == null) {
			if (other.nodeReader != null)
				return false;
		} else if (!nodeReader.equals(other.nodeReader))
			return false;
		if (predecessor == null) {
			if (other.predecessor != null)
				return false;
		} else if (!predecessor.equals(other.predecessor))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getPredecessor().toString()+" -- "+getInput().toString()+"@["+getCondition().toString()+"] / "+getOutput()+" -> "+getSuccessor().toString();
	}
}
