package uk.le.ac.convertNfmsToDfsm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import guidsl.prim;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;
import uk.le.ac.fsm.MealyUtils;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;

public class DataManagerFactory {
	private static final String HELP = "h";
	private static final String NFSM = "nfsm";
	private static final String OUT = "out";
	private static final String GRAPH_VIEW_FILE_EXTENSION = ".dot";
	private static final String KISS_FILE_EXTENSION = ".txt";

	private Map<Integer, Integer> corresponds_states_;

	private CompactMealy<String, Word<String>> fsm_;
	private CompactMealy<String, Word<String>> dfsm_;
	public static final Word<String> OMEGA_SYMBOL = Word.fromLetter("Ω");

	private CommandLineParser parser_;
	private Options options_;
	private HelpFormatter formatter_;
	private CommandLine line_;

	public DataManagerFactory() {
		parser_ = new BasicParser();
		options_ = create_and_get_options();
		formatter_ = new HelpFormatter();
	}

	public void set_arguments(String[] args) {
		try {
			line_ = parser_.parse(options_, args);
			check_necessary_options();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private static Options create_and_get_options() {
		Options options = new Options();
		options.addOption(HELP, false, "Help menu");
		options.addOption(NFSM, true, "non deterministic fsm");
		options.addOption(OUT, true, "output path");
		return options;
	}

	private void check_necessary_options() {
		if (line_.hasOption(HELP) || !line_.hasOption(NFSM) || !line_.hasOption(OUT)) {
			formatter_.printHelp("Generalize FFSM", options_);
			System.exit(0);
		}
	}

    public CompactNFA<String> read_and_get_non_det_fsm() {
		File nfsm_file = new File(line_.getOptionValue(NFSM));
		CompactNFA<String> non_det_fsm = null;
		try {
			non_det_fsm = loadNonDetMealyMachine(nfsm_file);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return non_det_fsm;
    }

	private CompactNFA<String> loadNonDetMealyMachine(File f) throws Exception {

		Pattern kissLine = Pattern.compile("\\s*(\\S+)\\s+--\\s+(\\S+)\\s*/\\s*(\\S+)\\s+->\\s+(\\S+)\\s*");

		BufferedReader br = new BufferedReader(new FileReader(f));

		List<String[]> trs = new ArrayList<String[]>();

		HashSet<String> abcSet = new HashSet<>();
		List<String> abc = new ArrayList<>();

		//		int count = 0;

		while(br.ready()){
			String line = br.readLine();
			Matcher m = kissLine.matcher(line);
			if(m.matches()){
				//				System.out.println(m.group(0));
				//				System.out.println(m.group(1));
				//				System.out.println(m.group(2));
				//				System.out.println(m.group(3));
				//				System.out.println(m.group(4));

				String[] tr = new String[4];
				tr[0] = m.group(1);
				tr[1] = m.group(2); 
				tr[2] = m.group(3);
				tr[3] = m.group(4);
				if(!abcSet.contains(tr[1] + " / " + tr[2])){
					abcSet.add(tr[1] + " / " + tr[2]);
					abc.add(tr[1] + " / " + tr[2]);					
				}
				trs.add(tr);
			}
			//			count++;
		}

		br.close();

		Collections.sort(abc);
		Alphabet<String> alphabet = Alphabets.fromCollection(abc);
		CompactNFA<String> mealym = new CompactNFA<String>(alphabet);
 
		Map<String,Integer> states = new HashMap<String,Integer>();
		Integer si=null,sf=null;

		Map<String,Word<String>> words = new HashMap<String,Word<String>>();		

		WordBuilder<String> aux = new WordBuilder<>();

		aux.clear();
		aux.append(OMEGA_SYMBOL);
		words.put(OMEGA_SYMBOL.toString(), aux.toWord());

		Integer s0 = null;

		for (String[] tr : trs) {
			if(!states.containsKey(tr[0])) states.put(tr[0], extract_number(mealym.addState(true).toString()));
			if(!states.containsKey(tr[3])) states.put(tr[3], extract_number(mealym.addState(true).toString()));

			si = states.get(tr[0]);
			if(s0==null) s0 = si;
			sf = states.get(tr[3]);

			if(!words.containsKey(tr[1])){
				aux.clear();
				aux.add(tr[1]);
				words.put(tr[1], aux.toWord());
			}
			if(!words.containsKey(tr[2])){
				aux.clear();
				aux.add(tr[2]);
				words.put(tr[2], aux.toWord());
			}

			mealym.addTransition(mealym.getState(si), words.get(tr[1]).toString() + " / " + words.get(tr[2]), mealym.getState(sf));
		}

		mealym.setInitial(mealym.getState(s0), true);
		return mealym;
	}
	
	private static Integer extract_number(String input) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(input);

		matcher.find();
		String number_str = matcher.group();
		return Integer.parseInt(number_str);
	}

	public void visualize(CompactDFA<String> dfa) throws IOException {
		get_corresponds_states(dfa);
		save_dfa_kiss_file(dfa);
		sort_dfa_output();
		read_and_get_det_fsm();
		delete_null_destinations();
		save_fsm_kiss_file();
		save_fsm_graph_view();
	}

	private void sort_dfa_output() throws FileNotFoundException, IOException {
		File inputFile = new File(line_.getOptionValue(OUT) + KISS_FILE_EXTENSION);

		// Read the file content
		Scanner scanner = new Scanner(inputFile);
		List<String> lines = new ArrayList<>();
		while (scanner.hasNextLine()) {
			lines.add(scanner.nextLine());
		}
		scanner.close();

		// Sort the lines
		Collections.sort(lines);

		// Write sorted content back to the file
		FileWriter writer = new FileWriter(inputFile);
		for (String line : lines) {
			writer.write(line + "\n");
		}
		writer.close();
	}

	private void get_corresponds_states(CompactDFA<String> dfa) {
		Integer counter = 0;
		corresponds_states_ = dfa.getStates().stream().collect(Collectors.toMap(i -> i, i -> -1));
        Stack<Integer> stack = new Stack<>();

        stack.push(dfa.getInitialState());

        while (!stack.isEmpty()) {
            int currentState = stack.pop();
			corresponds_states_.put(currentState, counter++);

			for (String in : dfa.getLocalInputs(currentState)) {
				Collection<Integer> neighbors = dfa.getTransitions(currentState, in);
				for (Integer neighbors_it : neighbors) {
					if(corresponds_states_.get(neighbors_it) == -1)
						stack.push(neighbors_it);
				}
			}
        }

		System.out.println(corresponds_states_);
	}

	private void read_and_get_det_fsm() {
		File fsm_file = new File(line_.getOptionValue(OUT) + KISS_FILE_EXTENSION);
		CompactMealy<String, Word<String>> det_fsm = null;
		try {
			det_fsm = MealyUtils.getInstance().loadMealyMachine(fsm_file);
		} catch (Exception e) {
			e.printStackTrace();
		}

		dfsm_ = det_fsm;
	}

	void save_fsm_graph_view() {
		fsm_ = dfsm_;
		StringBuilder buffer = new StringBuilder();
		add_header_to_buffer(buffer);
		add_states_to_buffer(buffer);
		add_transitions_to_buffer(buffer);
		add_bottom_complementary_lines_to_buffer(buffer);
		write_output(buffer, GRAPH_VIEW_FILE_EXTENSION);
	}

	private void add_header_to_buffer(StringBuilder buffer) {
		buffer.append("digraph g {\n\tedge [lblstyle=\"above, sloped\"];\n");
	}

	private void add_states_to_buffer(StringBuilder buffer) {
		for (Integer state : fsm_.getStates())
			buffer.append(String.format("\ts%s [shape=\"circle\" label=\"%d@[()]\"];\n", state,
				extract_number(state.toString())));
	}

	private void add_transitions_to_buffer(StringBuilder buffer) {
		for (Integer state : fsm_.getStates()) {
			for (String in : fsm_.getLocalInputs(state)) {
				Collection<CompactMealyTransition<Word<String>>> neighbors = fsm_.getTransitions(state, in);
				for (CompactMealyTransition<Word<String>> neighbors_it : neighbors) {
					String si = state.toString();
					int sj = neighbors_it.getSuccId();
					String input = in;
					Word<String> output = neighbors_it.getOutput();
					buffer.append(String.format("\ts%s -> s%d [label=\"%s/%s\"];\n", si, sj, input, output));
				}
			}
		}
	}

	private void add_bottom_complementary_lines_to_buffer(StringBuilder buffer) {
		buffer.append("\t__start0 [label=\"\" shape=\"none\" width=\"0\" height=\"0\"];\n");
		buffer.append(String.format("\t__start0 -> s%s;\n", fsm_.getInitialStates().toArray()[0].toString()));
		buffer.append("}");
	}

	public void write_output(StringBuilder buffer, String file_extension) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(line_.getOptionValue(OUT) + file_extension))) {
			bw.append(buffer.toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void save_dfa_kiss_file(CompactDFA<String> dfa) {
		StringBuilder buffer = new StringBuilder();

		for (Integer state : dfa.getStates()) {
			for (String in : dfa.getLocalInputs(state)) {
				Collection<Integer> neighbors = dfa.getTransitions(state, in);
				for (Integer neighbors_it : neighbors) {
					String si = corresponds_states_.get(state).toString();
					String sj = corresponds_states_.get(neighbors_it).toString();
					String input = in.split("/")[0], output = in.split("/")[1];
					input = input.substring(0, input.length() - 1);
					output = output.substring(1, output.length());
					buffer.append(String.format("s%s -- %s/%s -> s%s\n", si, input, output, sj));
				}
			}
		}
		write_output(buffer, KISS_FILE_EXTENSION);
	}

	private void delete_null_destinations() {
		List<Integer> states_ = new ArrayList<>();
		states_.addAll(dfsm_.getStates()); 
		states_.removeAll(dfsm_.getInitialStates());
		states_.addAll(0, dfsm_.getInitialStates());
		for (Integer si : states_) {
			for (String in : dfsm_.getInputAlphabet()) {
				Collection<CompactMealyTransition<Word<String>>> tr = dfsm_.getTransitions(si,in);
				for(CompactMealyTransition<Word<String>> tr_it : tr)
					if (tr_it != null)
						if(si == tr_it.getSuccId())
							dfsm_.removeTransition(si, in, tr_it);
			}
		}
	}

	public void save_fsm_kiss_file() {
		StringBuilder buffer = new StringBuilder();

		for (Integer state : dfsm_.getStates()) {
			for (String in : dfsm_.getLocalInputs(state)) {
				Collection<CompactMealyTransition<Word<String>>> neighbors = dfsm_.getTransitions(state, in);
				for (CompactMealyTransition<Word<String>> neighbors_it : neighbors) {
					String si = state.toString();
					int sj = neighbors_it.getSuccId();
					String input = in;
					Word<String> output = neighbors_it.getOutput();
					buffer.append(String.format("s%s -- %s/%s -> s%d\n", si, input, output, sj));
				}
			}
		}
		write_output(buffer, KISS_FILE_EXTENSION);
	}

}
