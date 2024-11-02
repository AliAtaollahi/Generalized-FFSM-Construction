package uk.le.ac.fsm;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import uk.le.ac.ffsm.ConditionalState;
import uk.le.ac.ffsm.ConditionalTransition;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.IConfigurableFSM;
import uk.le.ac.ffsm.ProductMealy;

public class ModelAsNfa<I, O> {

	private IConfigurableFSM<I, O> model;
	private FastNFA<String> nfa;
	private Map<FastNFAState, Integer>  nfa2model;
	private Map<Integer, FastNFAState>  model2nfa;

	private Map<FastNFAState,Map<String,Set<FastNFAState>>> transitionsOut;
	private Map<FastNFAState,Map<String,Set<FastNFAState>>> transitionsIn;

	public ModelAsNfa() {
		model2nfa = new LinkedHashMap<>();
		nfa2model = new LinkedHashMap<>();
		transitionsOut = new LinkedHashMap<>();
		transitionsIn = new LinkedHashMap<>();
	}
	
	public ModelAsNfa(ProductMealy<I, O> the_fsm) {
		this();
		MealyToNFA(the_fsm);
		this.model = the_fsm;
	}
	
	public ModelAsNfa(FeaturedMealy<I, O> the_ffsm) {
		this();
		FfsmToNFA(the_ffsm);
		this.model = the_ffsm;
	}

	private FastNFA<String> FfsmToNFA(FeaturedMealy<I, O> the_ffsm) {
		nfa = new FastNFA<String>(makeAlphabetIO(the_ffsm));
		
		for (ConditionalState<ConditionalTransition<I, O>> state : the_ffsm.getStates()) {
			Integer stateId = the_ffsm.getStateId(state);
			if(!model2nfa.containsKey(stateId))  {
				model2nfa.putIfAbsent(stateId, nfa.addState());
				nfa2model.putIfAbsent(model2nfa.get(stateId),stateId);
			}
			FastNFAState s1 = model2nfa.get(stateId);
			for (I inputSymbol : the_ffsm.getInputAlphabet()) {
				FastNFAState s2 = nfa.addState();
				nfa.addTransition(s1, inputSymbol.toString(), s2);
				
				transitionsOut.putIfAbsent(s1, new LinkedHashMap<>());
				transitionsOut.get(s1).putIfAbsent(inputSymbol.toString(), new LinkedHashSet<>());
				transitionsOut.get(s1).get(inputSymbol.toString()).add(s2);
				
				transitionsIn .putIfAbsent(s2, new LinkedHashMap<>());
				transitionsIn .get(s2).putIfAbsent(inputSymbol.toString(), new LinkedHashSet<>());
				transitionsIn .get(s2).get(inputSymbol.toString()).add(s1);
				
				for (ConditionalTransition<I, O> tr : the_ffsm.getTransitions(state, inputSymbol)) {
					if(!model2nfa.containsKey(the_ffsm.getStateId(tr.getSuccessor())))  {
						model2nfa.putIfAbsent(the_ffsm.getStateId(tr.getSuccessor()), nfa.addState());
						nfa2model.putIfAbsent(model2nfa.get(the_ffsm.getStateId(tr.getSuccessor())),the_ffsm.getStateId(tr.getSuccessor()));
					}
					FastNFAState s3 = model2nfa.get(the_ffsm.getStateId(tr.getSuccessor()));
					O outputSymbol = tr.getOutput();
					nfa.addTransition(s2, outputSymbol.toString(), s3);
					
					transitionsOut.putIfAbsent(s2, new LinkedHashMap<>());
					transitionsOut.get(s2).putIfAbsent(outputSymbol.toString(), new LinkedHashSet<>());
					transitionsOut.get(s2).get(outputSymbol.toString()).add(s3);
					
					transitionsIn .putIfAbsent(s3, new LinkedHashMap<>());
					transitionsIn .get(s3).putIfAbsent(outputSymbol.toString(), new LinkedHashSet<>());
					transitionsIn .get(s3).get(outputSymbol.toString()).add(s2);
				}
				
			}
		}
		return nfa;
	}

	private Alphabet<String> makeAlphabetIO(FeaturedMealy<I, O> the_ffsm) {
		Set<String> set_inputAlphabet = new LinkedHashSet<>();
		for (I in : the_ffsm.getInputAlphabet()) set_inputAlphabet.add(in.toString());
		for (ConditionalState<ConditionalTransition<I, O>> stateId : the_ffsm.getStates()) {
			for (I inputIdx : the_ffsm.getInputAlphabet()) {
				for (ConditionalTransition<I, O> tr : the_ffsm.getTransitions(stateId, inputIdx)) {
					set_inputAlphabet.add(tr.getOutput().toString());	
				}
			}
		}
		Alphabet<String> inpAlphabet = Alphabets.fromCollection(set_inputAlphabet);
		return inpAlphabet;
	}

	private FastNFA<String> MealyToNFA(ProductMealy<I, O> the_fsm) {
		nfa = new FastNFA<String>(makeAlphabetIO(the_fsm));
		
		for (Integer stateId : the_fsm.getStates()) {
			if(!model2nfa.containsKey(stateId))  {
				model2nfa.putIfAbsent(stateId, nfa.addState());
				nfa2model.putIfAbsent(model2nfa.get(stateId),stateId);
			}
			FastNFAState s1 = model2nfa.get(stateId);
			for (I inputSymbol : the_fsm.getInputAlphabet()) {
				FastNFAState s2 = nfa.addState();
				nfa.addTransition(s1, inputSymbol.toString(), s2);
				
				transitionsOut.putIfAbsent(s1, new LinkedHashMap<>());
				transitionsOut.get(s1).putIfAbsent(inputSymbol.toString(), new LinkedHashSet<>());
				transitionsOut.get(s1).get(inputSymbol).add(s2);
				
				transitionsIn .putIfAbsent(s2, new LinkedHashMap<>());
				transitionsIn .get(s2).putIfAbsent(inputSymbol.toString(), new LinkedHashSet<>());
				transitionsIn .get(s2).get(inputSymbol).add(s1);
				
				CompactMealyTransition<O> tr = the_fsm.getTransition(stateId, inputSymbol);
				if(!model2nfa.containsKey(tr.getSuccId()))  {
					model2nfa.putIfAbsent(tr.getSuccId(), nfa.addState());
					nfa2model.putIfAbsent(model2nfa.get(tr.getSuccId()),tr.getSuccId());
				}
				FastNFAState s3 = model2nfa.get(tr.getSuccId());
				String outputSymbol = tr.getOutput().toString();
				nfa.addTransition(s2, outputSymbol, s3);
				
				transitionsOut.putIfAbsent(s2, new LinkedHashMap<>());
				transitionsOut.get(s2).putIfAbsent(outputSymbol, new LinkedHashSet<>());
				transitionsOut.get(s2).get(outputSymbol).add(s3);
				
				transitionsIn .putIfAbsent(s3, new LinkedHashMap<>());
				transitionsIn .get(s3).putIfAbsent(outputSymbol, new LinkedHashSet<>());
				transitionsIn .get(s3).get(outputSymbol).add(s2);
				
			}
		}
		return nfa;
	}

	private Alphabet<String> makeAlphabetIO(ProductMealy<I, O> model) {
		Set<String> set_inputAlphabet = new LinkedHashSet<>();
		for (I in : model.getInputAlphabet()) set_inputAlphabet.add(in.toString());
		for (Integer stateId : model.getStates()) {
			for (I inputIdx : model.getInputAlphabet()) {
				set_inputAlphabet.add(model.getTransition(stateId, inputIdx).getOutput().toString());
			}
		}
		Alphabet<String> inpAlphabet = Alphabets.fromCollection(set_inputAlphabet);
		return inpAlphabet;
	}
	
	public Map<Integer, FastNFAState> getModel2nfa() {
		return model2nfa;
	}
	
	public Map<FastNFAState, Integer> getNfa2model() {
		return nfa2model;
	}
	
	public FastNFA<String> getNfa() {
		return nfa;
	}
	
	
	public IConfigurableFSM<I, O> getModel() {
		return model;
	}
	
	public Map<FastNFAState, Map<String, Set<FastNFAState>>> getTransitionsIn() {
		return transitionsIn;
	}
	
	public Map<FastNFAState, Map<String, Set<FastNFAState>>> getTransitionsOut() {
		return transitionsOut;
	}
	
}
