package uk.le.ac.ffsm;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import net.automatalib.automata.MutableAutomaton;
import net.automatalib.automata.base.fast.AbstractFastMutableNondet;
import net.automatalib.automata.concepts.MutableTransitionOutput;
import net.automatalib.words.Alphabet;

public class FeaturedMealy<I,O> 
				extends 	AbstractFastMutableNondet<ConditionalState<ConditionalTransition<I,O>>, I, ConditionalTransition<I,O>, Node, O>
				implements 	MutableTransitionOutput<ConditionalTransition<I,O>, O>,
                			MutableAutomaton<ConditionalState<ConditionalTransition<I,O>>, I, ConditionalTransition<I,O>, Node, O> ,
                			IConfigurableFSM<I,O>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String TRUE_STRING = "TRUE";
	private IFeatureModel featureModel;
	private ConditionalState<ConditionalTransition<I,O>> initialState;
	private Map<I, Node> conditionalInputs;
	private List<Node> configuration;

	public FeaturedMealy(Alphabet<I> inputAlphabet, IFeatureModel fm, Map<I, Node> condInp) {
		this(inputAlphabet,fm);
		setConditionalInputs(condInp);
	}
	
	public FeaturedMealy(Alphabet<I> inputAlphabet, IFeatureModel fm) {
		this(inputAlphabet);
		this.featureModel = fm;
		addTRUE_feature();
	}
	
	private void addTRUE_feature() {
		IFeature newChild = FMFactoryManager.getFactory(this.featureModel).createFeature(this.featureModel, TRUE_STRING);
		this.featureModel.addFeature(newChild);
		IFeature root = this.featureModel.getStructure().getRoot().getFeature();
		root.getStructure().addChild(newChild.getStructure());
		newChild.getStructure().setMandatory(true);
	}

	public FeaturedMealy(Alphabet<I> inputAlphabet) {
		super(inputAlphabet);
	}

	public IFeatureModel getFeatureModel() {
		return featureModel;
	}
	
	public Map<I, Node> getConditionalInputs() {
		return conditionalInputs;
	}
	
	public void setConditionalInputs(Map<I, Node> conditionalInputs) {
		this.conditionalInputs = new HashMap<>(conditionalInputs);
	}
	
	
	public ConditionalState<ConditionalTransition<I, O>> getInitialState() {
		return this.initialState;
	}
	
	public void setInitialState(ConditionalState<ConditionalTransition<I, O>> initialState) {
		getInitialStates().clear();
		super.setInitial(initialState,true);
		this.initialState = initialState;
	}
	
    @Override
    public void setInitial(ConditionalState<ConditionalTransition<I, O>> state, boolean initial) {
    	setInitialState(state);
    }
	
    @Override
    public ConditionalState<ConditionalTransition<I,O>> getSuccessor(ConditionalTransition<I,O> transition) {
        return transition.getSuccessor();
    }

    @Override
    public O getTransitionOutput(ConditionalTransition<I,O> transition) {
        return transition.getOutput();
    }

    @Override
    public Node getStateProperty(ConditionalState<ConditionalTransition<I,O>> state) {
        return state.getCondition();
    }

    @Override
	public void setStateProperty(ConditionalState<ConditionalTransition<I,O>> state, Node cond) {
		state.setCondition(cond);
	}

    @Override
    public O getTransitionProperty(ConditionalTransition<I,O> transition) {
    	return transition.getOutput();
    }
    
    @Override
    public void setTransitionProperty(ConditionalTransition<I,O> transition, O output) {
    	transition.setOutput(output);
    }

    
    @Override
    public void setTransitionOutput(ConditionalTransition<I,O> transition, O output) {
    	setTransitionProperty(transition, output);
    }
    
    @Override
    protected ConditionalState<ConditionalTransition<I,O>> createState(Node constr) {
        return new ConditionalState<ConditionalTransition<I,O>>(inputAlphabet.size()*2, constr);
    }

    public ConditionalTransition<I,O> addTransition(
    		ConditionalState<ConditionalTransition<I,O>> state, I input,
    		ConditionalState<ConditionalTransition<I,O>> successor, O output, Node cond) {
    	ConditionalTransition<I,O> tr = new ConditionalTransition<>(state,input,successor, output, cond);
    	addTransition(state, input, tr);
    	return tr;
    }
    
    @Override
    public ConditionalTransition<I,O> addTransition(
    		ConditionalState<ConditionalTransition<I,O>> state, I input,
    		ConditionalState<ConditionalTransition<I,O>> successor, O output) {
    	ConditionalTransition<I,O> tr = new ConditionalTransition<I,O>(successor, output);
    	addTransition(state, input, tr);
    	return tr;
    }
    
    @Override
    public void addTransition(ConditionalState<ConditionalTransition<I,O>> state, 
    		I input, ConditionalTransition<I,O> tr) {
    	ArrayList<ConditionalTransition<I,O>> tmp_transitions = new ArrayList<>(getTransitions(state, input));
    	tmp_transitions.add(tr);
    	addTransitions(state, input, tmp_transitions);
    }
    
    @Override
    public void addTransitions(ConditionalState<ConditionalTransition<I,O>> state, I input,
    		Collection<? extends ConditionalTransition<I,O>> transitions) {
    	setTransitions(state, input, transitions);
    }

	@Override
	public ConditionalTransition<I,O> createTransition(ConditionalState<ConditionalTransition<I,O>> successor,
			O output) {
		return new ConditionalTransition<I,O>(successor, output);
	}
	
	@Override
	public List<Node> getConfiguration() {
		return Collections.unmodifiableList(configuration);
	}
	
	public void setConfiguration(List<Node> configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public Map<I,List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input, O output, Integer sj) {
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		ConditionalState<ConditionalTransition<I, O>> statei = getState(si);
		ConditionalState<ConditionalTransition<I, O>> statej = getState(sj);
		for (ConditionalTransition<I, O> tr : super.getTransitions(statei, input)) {
			if(tr.getOutput().equals(output) & statej.equals(tr.getSuccessor())) {
				tr_match.putIfAbsent(input, new ArrayList<>());
				SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si, input, output, sj);
				simplyTr.setTransition(tr);
				tr_match.get(input).add(simplyTr);
			}
		}
		return tr_match;
	}
	
	@Override
	public Map<I,List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input, O output) {
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		ConditionalState<ConditionalTransition<I, O>> statei = getState(si);
		for (ConditionalTransition<I, O> tr : super.getTransitions(statei, input)) {
			Integer sj = tr.getSuccessor().getId();
			tr_match.putIfAbsent(input, new ArrayList<>());
			SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si, input, output, sj);
			simplyTr.setTransition(tr);
			tr_match.get(input).add(simplyTr);
		}
		return tr_match;
	}
	
	@Override
	public Map<I,List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input) {
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		ConditionalState<ConditionalTransition<I, O>> statei = getState(si);
		for (ConditionalTransition<I, O> tr : super.getTransitions(statei, input)) {
			Integer sj = tr.getSuccessor().getId();
			O output = tr.getOutput();
			tr_match.putIfAbsent(input, new ArrayList<>());
			SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si, input, output, sj);
			simplyTr.setTransition(tr);
			tr_match.get(input).add(simplyTr);
		}
		return tr_match;
	}
	
	@Override
	public Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si) {
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		ConditionalState<ConditionalTransition<I, O>> statei = getState(si);
		for (I input : getInputAlphabet()) {
			for (ConditionalTransition<I, O> tr : getTransitions(statei, input)) {
				tr_match.putIfAbsent(input, new ArrayList<>());
				SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(tr.getPredecessor().getId(), tr.getInput(), tr.getOutput(), tr.getSuccessor().getId());
				simplyTr.setTransition(tr);
				tr_match.get(input).add(simplyTr);
			}
		}
		return tr_match;
	}
	
	@Override
	public Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitionsIn(Integer sj) {
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		for (ConditionalState<ConditionalTransition<I, O>> si : getStates()) {
			for (I input : getInputAlphabet()) {
				for (ConditionalTransition<I, O> tr : getTransitions(si, input)) {
					if(!getState(sj).equals(tr.getSuccessor())) continue;
					tr_match.putIfAbsent(input, new ArrayList<>());
					SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si.getId(), input, tr.getOutput(), tr.getSuccessor().getId());
					simplyTr.setTransition(tr);
					tr_match.get(input).add(simplyTr);
				}
			}
		}
		return tr_match;
	}
	
	@Override
	public Integer getInitialStateIndex() {
		return getStateId(getInitialState());
	}

	@Override
	public List<Integer> getStateIDs() {
		List<Integer> out = new ArrayList<>();
		for (ConditionalState<ConditionalTransition<I, O>> state : getStates()) {
			out.add(state.getId());
		}
		return out;
	}
}
