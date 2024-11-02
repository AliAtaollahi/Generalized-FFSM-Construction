package uk.le.ac.ffsm;

import org.prop4j.Node;

public interface MutableFeatured<T> {

	void setTransitionConstraint(T transition, Node constraint);
	
}
