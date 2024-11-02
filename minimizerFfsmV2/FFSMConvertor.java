package uk.le.ac.minimizerFfsmV2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import guidsl.prim;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.util.automata.minimizer.hopcroft.HopcroftMinimization;
import net.automatalib.util.automata.minimizer.hopcroft.HopcroftMinimization.PruningMode;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import uk.le.ac.ffsm.ConditionalState;
import uk.le.ac.ffsm.ConditionalTransition;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.SimplifiedTransition;

public class FFSMConvertor {
	public FFSMConvertor() {
		data_manager_factory_ = new DataManagerFactory();
		fsmextractor_ = new FSMExractor();
	}
	private static final String KISS_FILE_EXTENSION = ".txt";
	private static final String GRAPH_VIEW_FILE_EXTENSION = ".dot";
	private DataManagerFactory data_manager_factory_;
	private FSMExractor fsmextractor_;
	private Map<Integer, Boolean> is_trap_;
	private Map<Integer, Boolean> is_trap__;


	public void start_loading_ffsm_and_convert_to_fsm(String[] args) {
		data_manager_factory_.set_arguments(args);

		IFeatureModel fm = data_manager_factory_.read_and_get_feature_model();
		FeaturedMealy<String, Word<String>> ffsm = data_manager_factory_.read_and_get_ffsm(fm);
		
		CompactNFA<String> nfsm = fsmextractor_.exract_fsm_from_ffsm(ffsm);

//		 for (Integer state : nfsm.getStates()) {
//		 	for (String in : nfsm.getLocalInputs(state)) {
//		 		Collection<Integer> neighbors = nfsm.getTransitions(state, in);
//		 		for (Integer neighbors_it : neighbors) {
//		 			String si = state.toString();
//		 			String sj = neighbors_it.toString();
//		 			String input = in;
//		 			System.out.print(String.format("s%s -- %s -> s%s\n", si, input, sj));
//		 		}
//		 	}
//		 }

		CompactDFA<String> dfsm = get_extracted_dfa(nfsm);
		Integer initiate_state = 0;
		for (Integer state : dfsm.getStates()) {
			if (dfsm.isAccepting(state) == false) {
				initiate_state = state;
			}
		}
		// FastNFA<String> fsm = new FastNFA<String>(make_and_get_alphabet(ffsm));
		// Map<FastNFAState, Integer> nfa_to_model_ = new LinkedHashMap<>();
		// Map<Integer, FastNFAState> model_to_nfa_ = new LinkedHashMap<>();

		// Integer init_state = dfsm.getStateId(initiate_state);
		// model_to_nfa_.putIfAbsent(init_state, fsm.addState());
		// nfa_to_model_.putIfAbsent(model_to_nfa_.get(init_state), init_state);
		// FastNFAState init_o = model_to_nfa_.get(init_state);
		// fsm.setInitial(init_o, true);

		// for (Integer state : dfsm.getStates()) {
		// 	if(state != initiate_state) {
		// 		Integer current_state_id = dfsm.getStateId(state);
		// 		model_to_nfa_.putIfAbsent(current_state_id, fsm.addState());
		// 		nfa_to_model_.putIfAbsent(model_to_nfa_.get(current_state_id), current_state_id);
		// 	}
		// }

		// Map<Integer, Boolean> reachable_states = dfsm.getStates().stream().collect(Collectors.toMap(s -> s, s -> false));
		// reachable_states.put(initiate_state, true);
		// Queue<Integer> queue = new LinkedList<>();
		// queue.offer(initiate_state);

		// System.out.println(dfsm.getLocalInputs(1));

		// while (!queue.isEmpty()) {
		// 	Integer current_vertex = queue.poll();
		// 	for (String in : dfsm.getLocalInputs(current_vertex)) {
		// 		System.out.print(in);
		// 		Collection<Integer> neighbors = dfsm.getTransitions(current_vertex, in);
		// 		for (Integer neighbors_it : neighbors) {
		// 			if (!reachable_states.get(neighbors_it)) {
		// 				reachable_states.put(neighbors_it, true);
		// 				queue.offer(neighbors_it);
		// 			}
		// 			Integer current_state_id = dfsm.getStateId(current_vertex);
		// 			FastNFAState current_state = model_to_nfa_.get(current_state_id);
		// 			Integer next_state_id = dfsm.getStateId(neighbors_it);
		// 			FastNFAState next_state = model_to_nfa_.get(next_state_id);
		// 			System.out.print(current_state_id);
		// 			System.out.print(in);
		// 			System.out.println(next_state_id);
		// 			String input = in.split("/")[0], output = in.split("/")[1];
		// 			fsm.addTransition(current_state, input + " / " + output, next_state);
		// 		}
		// 	}
		// }
		// if (initiate_state != 0){
		// 	for (String in : dfsm.getLocalInputs(initiate_state)) {
		// 		Collection<Integer> neighbors = dfsm.getTransitions(initiate_state, in);
		// 		for (Integer neighbors_it : neighbors) {
		// 			String si = "0";
		// 			String sj = neighbors_it.toString();
		// 			String input = in.split("/")[0], output = in.split("/")[1];
		// 			// System.out.print(String.format("\ts%s -- %s/%s -> s%s;\n", si, input, output, sj));
		// 			System.out.print(String.format("\ts%s -> s%s [label=\"%s/%s\"];\n", si, sj, input, output));

		// 		}
		// 	}

		// 	for (String in : dfsm.getLocalInputs(0)) {
		// 		Collection<Integer> neighbors = dfsm.getTransitions(0, in);
		// 		for (Integer neighbors_it : neighbors) {
		// 			String si = "s" + initiate_state.toString();
		// 			String sj = neighbors_it.toString();
		// 			String input = in.split("/")[0], output = in.split("/")[1];
		// 			// System.out.print(String.format("\ts%s -- %s/%s -> s%s;\n", si, input, output, sj));
		// 			System.out.print(String.format("\ts%s -> s%s [label=\"%s/%s\"];\n", si, sj, input, output));

		// 		}
		// 	}
		// }

		Map<Integer, Integer> states_for_init = new LinkedHashMap<>();
		for (Integer state : dfsm.getStates()) {
			if(initiate_state != 0 && (state == 0 || state == initiate_state)){
				if(state == 0)
					states_for_init.put(0, initiate_state);
				else if(state == initiate_state)
					states_for_init.put(initiate_state, 0);
			}
			else
				states_for_init.put(state, state);
		}

        make_graph_view(dfsm, states_for_init);

		make_txt_output(dfsm, states_for_init);

		// for (FastNFAState state : fsm.getStates()) {
		// 	for (String in : fsm.getLocalInputs(state)) {
		// 		Collection<FastNFAState> neighbors = fsm.getTransitions(state, in);
		// 		for (FastNFAState neighbors_it : neighbors) {
		// 			String si = state.toString();
		// 			String sj = neighbors_it.toString();
		// 			String input = in.split("/")[0], output = in.split("/")[1];
		// 			System.out.print(String.format("\ts%s -> s%s [label=\"%s/%s\"];\n", si, sj, input, output));
		// 		}
		// 	}
		// }

		// CompactMealy<String, Word<String>> dffsm = data_manager_factory_.clear_and_sort_dfa(dfsm);
		// dffsm = delete_null_from_dffsm(dffsm);
		// dffsm = HopcroftMinimization.minimizeMealy(dffsm);
		// dfsm = HopcroftMinimization.minimizeDFA(dfsm, dfsm.getInputAlphabet(), this.pruningMode);

//		for (Integer state : dffsm.getStates()) {
//			for (String in : dffsm.getLocalInputs(state)) {
//				Collection<CompactMealyTransition<Word<String>>> neighbors = dffsm.getTransitions(state, in);
//				for (CompactMealyTransition<Word<String>> neighbors_it : neighbors) {
//					String si = state.toString();
//					int sj = neighbors_it.getSuccId();
//					String input = in;
//					Word<String> output = neighbors_it.getOutput();
//					System.out.print(String.format("s%s -- %s/%s -> s%d\n", si, input, output, sj));
//				}
//			}
//		}
	}

	private void make_txt_output(CompactDFA<String> dfsm, Map<Integer, Integer> states_for_init) {
		StringBuilder buffer = new StringBuilder();
		for (Integer state : dfsm.getStates()) {
		 	for (String in : dfsm.getLocalInputs(state)) {
		 		Collection<Integer> neighbors = dfsm.getTransitions(state, in);
		 		for (Integer neighbors_it : neighbors) {
		 			String si = states_for_init.get(state).toString();
		 			String sj = states_for_init.get(neighbors_it).toString();
		 			String input = in.split("/")[0], output = in.split("/")[1];
		 	        String prefix = output.substring(0, output.indexOf(')') + 1);
		 	        String condition = output.substring(output.indexOf('[') + 1, output.indexOf(']'));
					buffer.append(String.format("s%s@[()] -- %s@[(%s)]/%s -> s%s@[()]\n", si, input, condition, prefix, sj));
				}
			}
		}
		data_manager_factory_.write_output(buffer, KISS_FILE_EXTENSION, true);
	}

	private void make_graph_view(CompactDFA<String> dfsm, Map<Integer, Integer> states_for_init) {
		Set<String> uniqueStates = new HashSet<>();
        StringBuilder stateBuffer = new StringBuilder();
        StringBuilder edgeBuffer = new StringBuilder();

        for (Integer state : dfsm.getStates()) {
            for (String in : dfsm.getLocalInputs(state)) {
                Collection<Integer> neighbors = dfsm.getTransitions(state, in);
                for (Integer neighbors_it : neighbors) {
                    String si = states_for_init.get(state).toString();
                    String sj = states_for_init.get(neighbors_it).toString();
                    String input = in.split("/")[0];
                    String output = in.split("/")[1];

                    // Add states to the set and buffer if they are unique
                    if (uniqueStates.add(si)) {
                        stateBuffer.append(String.format("\ts%s [shape=\"circle\" label=\"%s@[%s]\"];\n", si, si, "()"));
                    }
                    if (uniqueStates.add(sj)) {
                        stateBuffer.append(String.format("\ts%s [shape=\"circle\" label=\"%s@[%s]\"];\n", sj, sj, "()"));
                    }

                    // Add edges to the buffer
                    edgeBuffer.append(String.format("\ts%s -> s%s [label=\"%s / %s\"];\n", si, sj, input, output));
                }
            }
        }

        // Combine state and edge buffers into one StringBuilder
        StringBuilder finalOutput = new StringBuilder();
        finalOutput.append("digraph g {\n	edge [lblstyle=\"above, sloped\"];\n");
        finalOutput.append(stateBuffer);
        finalOutput.append(edgeBuffer);
        finalOutput.append("	__start0 [label=\"\" shape=\"none\" width=\"0\" height=\"0\"];\r"
        		+ "	__start0 -> s0;\r"
        		+ "}");
        
        data_manager_factory_.write_output(finalOutput, GRAPH_VIEW_FILE_EXTENSION, false);
	}

    private PruningMode pruningMode;

    @Factory(dataProvider = "pruningModes")
    public void FFSMConvertor(PruningMode pruningMode) {
        this.pruningMode = pruningMode;
    }

	private CompactDFA<String> get_extracted_dfa(CompactNFA<String> nfsm) {
		// for finding init_state
		Integer init_state = 0;
		nfsm.setAccepting(init_state, false);

		CompactDFA<String> dfa = NFAs.determinize(nfsm);
		dfa = HopcroftMinimization.minimizeDFA(dfa, dfa.getInputAlphabet(), this.pruningMode);
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

	private CompactMealy<String, Word<String>> delete_null_from_dffsm(CompactMealy<String, Word<String>> dffsm) {
		get_traped_statess(dffsm);
		
		for (Integer state : dffsm.getStates()) {
			for (String in : dffsm.getLocalInputs(state)) {
				Collection<CompactMealyTransition<Word<String>>> neighbors = dffsm.getTransitions(state, in);
				for (CompactMealyTransition<Word<String>> neighbors_it : neighbors) {
					if(state==neighbors_it.getSuccId()) ////////////////////////////////////
						dffsm.removeTransition(state, in, neighbors_it);
					if((is_trap__.get(neighbors_it.getSuccId()) || is_trap__.get(state)))
						dffsm.removeTransition(state, in, neighbors_it);
				}
			}
		}
		return dffsm;
	}

	private void get_traped_statess(CompactMealy<String, Word<String>> dffsm){
		is_trap__ = dffsm.getStates().stream().collect(Collectors.toMap(i -> i, i -> true));
		for (Integer state : dffsm.getStates()) {
			for (String in : dffsm.getLocalInputs(state)) {
				Collection<CompactMealyTransition<Word<String>>> neighbors = dffsm.getTransitions(state, in);
				for (CompactMealyTransition<Word<String>> neighbors_it : neighbors)
					if(state != neighbors_it.getSuccId())
						is_trap__.put(state, false);
			}
		}
	}
	private Alphabet<String> make_and_get_alphabet(FeaturedMealy<String, Word<String>> ffsm) {
		Set<String> set_input_alphabet = new LinkedHashSet<>();
		Integer i = 0;
		for (ConditionalState<ConditionalTransition<String, Word<String>>> state : ffsm.getStates()) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions = ffsm
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
