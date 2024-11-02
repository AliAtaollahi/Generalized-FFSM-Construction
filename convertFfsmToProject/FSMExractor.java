package uk.le.ac.convertFfsmToProject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.prop4j.Node;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import uk.le.ac.ffsm.ConditionalState;
import uk.le.ac.ffsm.ConditionalTransition;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.SimplifiedTransition;

public class FSMExractor {
	private Map<Integer, Boolean> accepted_conditions_states_;
	private FastNFA<String> fsm_;
	private FeaturedMealy<String, Word<String>> ffsm_;
	private Map<Object, Boolean> configs_;
	private Map<FastNFAState, Integer> nfa_to_model_;
	private Map<Integer, FastNFAState> model_to_nfa_;
	private boolean is_there_loop_;

	public FSMExractor() {
	}

	public FastNFA<String> exract_fsm_from_ffsm(FeaturedMealy<String, Word<String>> ffsm,
			Map<Object, Boolean> configs, boolean is_there_loop) {
		set_requirements(ffsm, configs, is_there_loop);
		create_accepted_conditions_states();
		build_fsm();
		return fsm_;
	}

	private void set_requirements(FeaturedMealy<String, Word<String>> ffsm, Map<Object, Boolean> configs,
			boolean is_there_loop) {
		ffsm_ = ffsm;
		configs_ = configs;
		nfa_to_model_ = new LinkedHashMap<>();
		model_to_nfa_ = new LinkedHashMap<>();
		is_there_loop_ = is_there_loop;
	}

	private void create_accepted_conditions_states() {
		accepted_conditions_states_ = new HashMap<Integer, Boolean>();
		for (ConditionalState<ConditionalTransition<String, Word<String>>> state : ffsm_.getStates())
			accepted_conditions_states_.put(state.getId(),
					state.getCondition() == null || state.getCondition().getValue(configs_));
	}

	private void build_fsm() {
		construct_fsm();
		add_states_to_fsm();
		add_transitions_to_fsm();
	}

	private void construct_fsm() {
		fsm_ = new FastNFA<String>(make_and_get_alphabet());
	}

	private void add_states_to_fsm() {
		for (ConditionalState<ConditionalTransition<String, Word<String>>> state : ffsm_.getStates()) {
			Integer current_state_id = ffsm_.getStateId(state);
			add_single_state(current_state_id);
		}
	}

	private void add_single_state(Integer current_state_id) {
		if (!model_to_nfa_.containsKey(current_state_id)) {
			model_to_nfa_.putIfAbsent(current_state_id, fsm_.addState());
			nfa_to_model_.putIfAbsent(model_to_nfa_.get(current_state_id), current_state_id);
		}
	}

	private void add_transitions_to_fsm() {
		Integer i = 0;
		for (ConditionalState<ConditionalTransition<String, Word<String>>> state : ffsm_.getStates()) {
			Integer current_state_id = ffsm_.getStateId(state);
			FastNFAState current_state = model_to_nfa_.get(current_state_id);
			check_and_set_initial_state(i, current_state);
			add_transtions_after_check_condition(i, current_state);
			i++;
		}
	}

	private void check_and_set_initial_state(Integer i, FastNFAState current_state) {
		if (i == 0)
			fsm_.setInitial(current_state, true);
	}

	private void add_transtions_after_check_condition(Integer i, FastNFAState current_state) {
		Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions = ffsm_.getSimplifiedTransitions(i);
		for (Entry<String, List<SimplifiedTransition<String, Word<String>>>> t : transitions.entrySet()) {
			for (SimplifiedTransition<String, Word<String>> transition : t.getValue()) {
				Integer destination_state_id = transition.getSj();
				FastNFAState destination_state = model_to_nfa_.get(destination_state_id);
				add_single_transition(current_state, transition, destination_state);
			}
		}
	}

	private void add_single_transition(FastNFAState current_state,
			SimplifiedTransition<String, Word<String>> transition, FastNFAState destination_state) {
		if (!does_state_accept_conditions(current_state) || !does_state_accept_conditions(destination_state))
			return;
		
		if(is_there_loop_ && current_state == destination_state)
			return;

		Node condition_of_transtion = get_condition_of_transtion(transition);
		if (condition_of_transtion == null || condition_of_transtion.getValue(configs_)) {
			String input_and_output = make_transition_input_output_by_protocol(transition);
			fsm_.addTransition(current_state, input_and_output, destination_state);
		}
	}

	private Boolean does_state_accept_conditions(FastNFAState destination_state) {
		return accepted_conditions_states_.get(destination_state.getId());
	}

	private Node get_condition_of_transtion(SimplifiedTransition<String, Word<String>> transition) {
		@SuppressWarnings("unchecked")
		ConditionalTransition<String, Word<String>> c_transition = (ConditionalTransition<String, Word<String>>) transition
				.getTransition();
		return c_transition.getCondition();
	}

	private String make_transition_input_output_by_protocol(SimplifiedTransition<String, Word<String>> transition) {
		return transition.getIn().toString() + "/" + transition.getOut().toString();
	}

	private Alphabet<String> make_and_get_alphabet() {
		Set<String> set_input_alphabet = new LinkedHashSet<>();
		Integer i = 0;
		for (ConditionalState<ConditionalTransition<String, Word<String>>> state : ffsm_.getStates()) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions = ffsm_
					.getSimplifiedTransitions(i);
			for (Entry<String, List<SimplifiedTransition<String, Word<String>>>> t : transitions.entrySet()) {
				for (SimplifiedTransition<String, Word<String>> transition : t.getValue()) {
					String input_and_output = transition.getIn().toString() + "/" + transition.getOut().toString();
					set_input_alphabet.add(input_and_output);
				}
			}
			i++;
		}
		return Alphabets.fromCollection(set_input_alphabet);
	}
}
