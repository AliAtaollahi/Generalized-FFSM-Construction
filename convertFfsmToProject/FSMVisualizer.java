package uk.le.ac.convertFfsmToProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;

public class FSMVisualizer {
	private static final String KISS_FILE_EXTENSION = ".txt";
	private static final String GRAPH_VIEW_FILE_EXTENSION = ".dot";

	private FastNFA<String> fsm_;
	private Map<FastNFAState, Boolean> reachable_states_;
	private boolean should_one_scc_check_;
	private SCCFinder ssc_finder_;
	private Map<FastNFAState, Boolean> is_in_root_scc_;

	public FSMVisualizer() {
	}

	public void visualize(FastNFA<String> fsm, DataManagerFactory data_manager_factory) {
		set_fsm(fsm);
		set_reachable_states_by_bfs();
		set_scc_check(data_manager_factory);
		save_fsm_kiss(data_manager_factory);
		save_fsm_graph_view(data_manager_factory);
		announce_successful_message();
	}

	private void set_fsm(FastNFA<String> fsm) {
		fsm_ = fsm;
	}

	private void set_reachable_states_by_bfs() {
		FastNFAState root_state = find_root_state();
		initiate_states_reachablity_mapping(root_state);
		run_bfs_to_determine_reachable_states(root_state);
	}
	
	private void set_scc_check(DataManagerFactory data_manager_factory) {
		ssc_finder_ = new SCCFinder(fsm_);
		should_one_scc_check_ = data_manager_factory.get_should_one_scc_check_status();
		if(should_one_scc_check_ == true) 
			is_in_root_scc_ = ssc_finder_.get_is_in_root_scc();
	}

	private FastNFAState find_root_state() {
		return (new ArrayList<>(fsm_.getInitialStates())).get(0);
	}

	private void initiate_states_reachablity_mapping(FastNFAState root_state) {
		reachable_states_ = fsm_.getStates().stream().collect(Collectors.toMap(s -> s, s -> false));
		reachable_states_.put(root_state, true);
	}

	private void run_bfs_to_determine_reachable_states(FastNFAState root_state) {
		Queue<FastNFAState> queue = new LinkedList<>();
		queue.offer(root_state);

		while (!queue.isEmpty()) {
			FastNFAState current_vertex = queue.poll();
			for (String in : fsm_.getLocalInputs(current_vertex)) {
				Collection<FastNFAState> neighbors = fsm_.getTransitions(current_vertex, in);
				for (FastNFAState neighbors_it : neighbors) {
					if (!reachable_states_.get(neighbors_it)) {
						reachable_states_.put(neighbors_it, true);
						queue.offer(neighbors_it);
					}
				}
			}
		}
	}

	private void save_fsm_kiss(DataManagerFactory data_manager_factory) {
		StringBuilder buffer = new StringBuilder();

		for (FastNFAState state : fsm_.getStates()) {
			for (String in : fsm_.getLocalInputs(state)) {
				Collection<FastNFAState> neighbors = fsm_.getTransitions(state, in);
				for (FastNFAState neighbors_it : neighbors) {
					if (reachable_states_.get(neighbors_it) == false || reachable_states_.get(state) == false)
						continue;
					if (should_one_scc_check_ == true && (is_in_root_scc_.get(neighbors_it) == false || is_in_root_scc_.get(state) == false))
						continue;
					String si = state.toString();
					String sj = neighbors_it.toString();
					String input = in.split("/")[0], output = in.split("/")[1];
					buffer.append(String.format("%s -- %s / %s -> %s\n", si, input, output, sj));
				}
			}
		}
		data_manager_factory.write_output(buffer, KISS_FILE_EXTENSION);
	}

	private void save_fsm_graph_view(DataManagerFactory data_manager_factory) {
		StringBuilder buffer = new StringBuilder();
		add_header_to_buffer(buffer);
		add_states_to_buffer(buffer);
		add_transitions_to_buffer(buffer);
		add_bottom_complementary_lines_to_buffer(buffer);
		data_manager_factory.write_output(buffer, GRAPH_VIEW_FILE_EXTENSION);
	}

	private void add_header_to_buffer(StringBuilder buffer) {
		buffer.append("digraph g {\n\tedge [lblstyle=\"above, sloped\"];\n");
	}

	private void add_states_to_buffer(StringBuilder buffer) {
		for (FastNFAState state : fsm_.getStates()) {
			if (reachable_states_.get(state) == false)
				continue;
			if (should_one_scc_check_ == true && is_in_root_scc_.get(state) == false)
				continue;
			buffer.append(String.format("\t%s [shape=\"circle\" label=\"%d@[()]\"];\n", state.toString(),
					extract_number(state.toString())));
		}
	}

	private static int extract_number(String input) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(input);

		matcher.find();
		String number_str = matcher.group();
		return Integer.parseInt(number_str);
	}

	private void add_transitions_to_buffer(StringBuilder buffer) {
		for (FastNFAState state : fsm_.getStates()) {
			for (String in : fsm_.getLocalInputs(state)) {
				Collection<FastNFAState> neighbors = fsm_.getTransitions(state, in);
				for (FastNFAState neighbors_it : neighbors) {
					if (reachable_states_.get(neighbors_it) == false || reachable_states_.get(state) == false)
						continue;
					if (should_one_scc_check_ == true && (is_in_root_scc_.get(neighbors_it) == false || is_in_root_scc_.get(state) == false))
						continue;
					String si = state.toString();
					String sj = neighbors_it.toString();
					String input = in.split("/")[0], output = in.split("/")[1];
					buffer.append(String.format("\t%s -> %s [label=\"%s / %s\"];\n", si, sj, input, output));
				}
			}
		}
	}

	private void add_bottom_complementary_lines_to_buffer(StringBuilder buffer) {
		buffer.append("\t__start0 [label=\"\" shape=\"none\" width=\"0\" height=\"0\"];\n");
		buffer.append(String.format("\t__start0 -> %s;\n", fsm_.getInitialStates().toArray()[0].toString()));
		buffer.append("}");
	}

	private static void announce_successful_message() {
		System.out.println("Finished!");
	}
}
