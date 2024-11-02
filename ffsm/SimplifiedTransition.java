package uk.le.ac.ffsm;

import org.prop4j.Node;

public class SimplifiedTransition<I,O> {
	
	
	private static final String TRUE_STRING = "TRUE";
	
	Integer si;
	I in;
	O out;
	Node condition;
	Integer sj;
	
	Object transition;
	
	
	
	public SimplifiedTransition(Integer si, I in, O out, Node condition, Integer sj) {
		this.si = si;
		this.in = in;
		this.out = out;
		this.condition = condition;
		this.sj = sj;
	}
	
	public SimplifiedTransition(Integer si, I in, O out, Integer sj) {
		this(si, in, out, null, sj);
	}

	public Integer getSi() {
		return si;
	}
	
	public Integer getSj() {
		return sj;
	}
	
	public I getIn() {
		return in;
	}
	
	public O getOut() {
		return out;
	}
	
	public Node getCondition() {
		return condition;
	}
	
	public Object getTransition() {
		return transition;
	}
	
	public void setTransition(Object transition) {
		this.transition = transition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((in == null) ? 0 : in.hashCode());
		result = prime * result + ((out == null) ? 0 : out.hashCode());
		result = prime * result + ((si == null) ? 0 : si.hashCode());
		result = prime * result + ((sj == null) ? 0 : sj.hashCode());
//		result = prime * result + ((transition == null) ? 0 : transition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SimplifiedTransition))
			return false;
		SimplifiedTransition other = (SimplifiedTransition) obj;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (in == null) {
			if (other.in != null)
				return false;
		} else if (!in.equals(other.in))
			return false;
		if (out == null) {
			if (other.out != null)
				return false;
		} else if (!out.equals(other.out))
			return false;
		if (si == null) {
			if (other.si != null)
				return false;
		} else if (!si.equals(other.si))
			return false;
		if (sj == null) {
			if (other.sj != null)
				return false;
		} else if (!sj.equals(other.sj))
			return false;
//		if (transition == null) {
//			if (other.transition != null)
//				return false;
//		} else if (!transition.equals(other.transition))
//			return false;
		return true;
	}
	
	

}