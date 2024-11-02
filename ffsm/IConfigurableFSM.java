package uk.le.ac.ffsm;

import java.util.List;
import java.util.Map;

import org.prop4j.Node;

import net.automatalib.words.Alphabet;

public interface IConfigurableFSM<I,O> {
	
	public List<Node> getConfiguration();
	public void 	  setConfiguration(List<Node> configuration);
	
	public Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input, O output, Integer sj);
	public Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input, O output);
	public Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input);
	public Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si);
	Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitionsIn(Integer sj);
	
	public Alphabet<I> getInputAlphabet(Integer state);
	public List<Integer> getStateIDs();
	public Integer getInitialStateIndex();
	

}
