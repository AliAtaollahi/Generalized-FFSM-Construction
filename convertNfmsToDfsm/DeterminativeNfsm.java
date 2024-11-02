package uk.le.ac.convertNfmsToDfsm;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.util.automata.fsa.NFAs;

public class DeterminativeNfsm {
	public DeterminativeNfsm() {
		data_manager_factory_ = new DataManagerFactory();
	}
	
	public static final Word<String> OMEGA_SYMBOL = Word.fromLetter("Ω");
	
	private DataManagerFactory data_manager_factory_;
	private CompactNFA<String> non_det_fsm_;
	private Map<Integer, Boolean> is_trap_;

	public void determinate_nfsm(String[] args) {
		try {
			data_manager_factory_.set_arguments(args);
			
			non_det_fsm_ = data_manager_factory_.read_and_get_non_det_fsm();
			delete_non_det_null_destinations();
			
			CompactDFA<String> dfa = get_extracted_dfa();

			data_manager_factory_.visualize(dfa);
						

		} catch (Exception e) {
			e.printStackTrace();
		}  
	}

	private CompactDFA<String> get_extracted_dfa() {
		CompactDFA<String> dfa = NFAs.determinize(non_det_fsm_);
		System.out.println(non_det_fsm_.getStates());
		get_traped_states(dfa);
		
		for (Integer state : dfa.getStates()) {
			for (String in : dfa.getLocalInputs(state)) {
				Collection<Integer> neighbors = dfa.getTransitions(state, in);
				for (Integer neighbors_it : neighbors) {
					if(is_trap_.get(neighbors_it) || is_trap_.get(state))
						dfa.removeTransition(state, in, neighbors_it);
				}
			}
		}
		
		return dfa;
	}

	private void delete_non_det_null_destinations() {
		for (Integer state : non_det_fsm_.getStates()) {
			for (String in : non_det_fsm_.getLocalInputs(state)) {
				Collection<Integer> neighbors = non_det_fsm_.getTransitions(state, in);
				for (Integer neighbors_it : neighbors) {
					if (neighbors_it != null)
						if (state == neighbors_it)
							non_det_fsm_.removeTransition(state, in, neighbors_it);
				}
			}
		}
	}
	
	private void get_traped_states(CompactDFA<String> dfa){
		is_trap_ = dfa.getStates().stream().collect(Collectors.toMap(i -> i, i -> true));
		for (Integer state : dfa.getStates()) {
			for (String in : dfa.getLocalInputs(state)) {
				Collection<Integer> neighbors = dfa.getTransitions(state, in);
				for (Integer neighbors_it : neighbors)
				if(state != neighbors_it)
				is_trap_.put(state, false);
			}
		}
	}
}