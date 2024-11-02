package uk.le.ac.compareDetAndNondetProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.words.Word;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;

public class FSMComparator {
	public FSMComparator() {
		data_manager_factory_ = new DataManagerFactory();
	}
	
	public static final Word<String> OMEGA_SYMBOL = Word.fromLetter("Ω");
	
	private DataManagerFactory data_manager_factory_;
	private CompactMealy<String, Word<String>> det_fsm_;
	private FastNFA<String> non_det_fsm_;
	Map<Integer, FastNFAState> correspondence_states_;

	public void start_comparing(String[] args) {
		try {
			data_manager_factory_.set_arguments(args);

			det_fsm_ = data_manager_factory_.read_and_get_det_fsm();
			delete_null_destinations();
			
			non_det_fsm_ = data_manager_factory_.read_and_get_non_det_fsm();
			delete_non_det_null_destinations();

			compare_fsms();
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}

	private void delete_null_destinations() {
		List<Integer> states_ = new ArrayList<>();
		states_.addAll(det_fsm_.getStates()); 
		states_.removeAll(det_fsm_.getInitialStates());
		states_.addAll(0, det_fsm_.getInitialStates());
		for (Integer si : states_) {
			for (String in : det_fsm_.getInputAlphabet()) {
				Collection<CompactMealyTransition<Word<String>>> tr = det_fsm_.getTransitions(si,in);
				for(CompactMealyTransition<Word<String>> tr_it : tr)
					if (tr_it != null)
						if(si == tr_it.getSuccId())
							det_fsm_.removeTransition(si, in, tr_it);
			}
		}
	}

	private void delete_non_det_null_destinations() {
		for (FastNFAState state : non_det_fsm_.getStates()) {
			for (String in : non_det_fsm_.getLocalInputs(state)) {
				Collection<FastNFAState> neighbors = non_det_fsm_.getTransitions(state, in);
				for (FastNFAState neighbors_it : neighbors) {
					if (neighbors_it != null)
						if (state.getId() == neighbors_it.getId())
							non_det_fsm_.removeTransition(state, in, neighbors_it);
				}
			}
		}
	}

	private void compare_fsms() throws Exception {
		initiate_correspondence_states();
		iterate_states_and_compare_det_and_non_det_fsm();
		announce_equal();
	}

	private void initiate_correspondence_states() {
		correspondence_states_ = new HashMap<>();
		correspondence_states_.put(det_fsm_.getIntInitialState(), (new ArrayList<>(non_det_fsm_.getInitialStates())).get(0));
	}

	private void iterate_states_and_compare_det_and_non_det_fsm() throws Exception {
		List<Integer> states = get_det_fsm_states();
		for (Integer si : states) {
			check_number_of_edges_equivalence_on_correspond_state(si);
			investigate_all_edges_with_common_source(si);
		}
	}

	private List<Integer> get_det_fsm_states() {
		List<Integer> states = new ArrayList<>(det_fsm_.getStates());
		states.remove(det_fsm_.getInitialState());
		states.add(0, det_fsm_.getInitialState());
		return states;
	}

	private void check_number_of_edges_equivalence_on_correspond_state(Integer si) throws Exception {
		if(count_number_of_transitions_on_det_state(si) != count_number_of_transitions_on_non_det_state(correspondence_states_.get(si)))
			announce_not_equal();
	}

	private void announce_not_equal() throws Exception {
		System.out.println("Not equivalent");
		throw new Exception("error message");
	}	

	private void investigate_all_edges_with_common_source(Integer si) throws Exception {
		for (String in : det_fsm_.getInputAlphabet()) {
			for (CompactMealyTransition<Word<String>> tr : det_fsm_.getTransitions(si,in)) {
				String input_and_output = concat_input_and_output_by_protocol(in, tr);
				check_is_state_in_correspondence_states_and_add(si, tr, input_and_output);
				check_states_equivalence_by_destination_of_edges(si, tr, input_and_output);
			}

		}
	}

	private String concat_input_and_output_by_protocol(String in, CompactMealyTransition<Word<String>> tr) {
		return in + " / " + tr.getOutput().toString();
	}

	private void check_is_state_in_correspondence_states_and_add(Integer si, CompactMealyTransition<Word<String>> tr,
			String input_and_output) throws Exception {
		if(!correspondence_states_.containsKey((int)tr.getSuccId())) {
			FastNFAState corresponding_state = find_corresponding_state(correspondence_states_.get(si), input_and_output);
			correspondence_states_.put((int)tr.getSuccId(), corresponding_state);
		}
	}

	private void check_states_equivalence_by_destination_of_edges(Integer si,
			CompactMealyTransition<Word<String>> tr, String input_and_output) throws Exception {
		FastNFAState expected_state = correspondence_states_.get((int)tr.getSuccId());
		FastNFAState practical_state = find_corresponding_state(correspondence_states_.get(si), input_and_output);
		if(expected_state != practical_state)
			announce_not_equal();
	}

	private void announce_equal() {
		System.out.println("equal");
	}
	
	private int count_number_of_transitions_on_det_state(Integer si) {
		int counter = 0;
		for (String in : det_fsm_.getInputAlphabet()) 
			for (CompactMealyTransition<Word<String>> tr : det_fsm_.getTransitions(si,in))
				counter++;
		return counter;
	}
	
	private int count_number_of_transitions_on_non_det_state(FastNFAState state) {
		int counter = 0;
		for (String in : non_det_fsm_.getLocalInputs(state)) {
			Collection<FastNFAState> neighbors = non_det_fsm_.getTransitions(state, in);
			System.out.println(in);
			counter += neighbors.size();
		}
		return counter;
	}
	
	private FastNFAState find_corresponding_state(FastNFAState state, String det_in) throws Exception {
		for (String in : non_det_fsm_.getLocalInputs(state)) {
			Collection<FastNFAState> neighbors = non_det_fsm_.getTransitions(state, in);
			for (FastNFAState neighbors_it : neighbors)
				if(det_in.equals(in))
					return neighbors_it;
		}
		announce_not_equal();
		return state;
	}

}