package uk.le.ac.minimizerFfsmV2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Comparator;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.FeaturedMealyUtils;
import uk.le.ac.fsm.MealyUtils;

public class DataManagerFactory {
	private static final String ALL_FEATURES = "All Features";
	private static final String GRAPH_VIEW_FILE_EXTENSION = ".dot";
	private static final String KISS_FILE_EXTENSION = ".txt";
	private static final String TEMP_EXTENSION = ".temp";
	private CompactMealy<String, Word<String>> fsm_;
	private CompactMealy<String, Word<String>> dfsm_;

	private Map<Integer, Integer> corresponds_states_;

	private static final String HELP = "h";
	private static final String FM = "fm";
	private static final String FFSM = "ffsm";
	private static final String OUT = "out";
	private static final String ALPHABET = "alphabet";
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
		options.addOption(FM, true, "Feature model");
		options.addOption(FFSM, true, "FFSM");
		options.addOption(OUT, true, "Output directory");
		options.addOption(ALPHABET, true, "Alphabet of features");
		return options;
	}

	private void check_necessary_options() {
		if (line_.hasOption(HELP) || !line_.hasOption(FM) || !line_.hasOption(FFSM)
				|| !line_.hasOption(OUT)) {
			formatter_.printHelp("Generalize FFSM", options_);
			System.exit(0);
		}
	}

	public IFeatureModel read_and_get_feature_model() {
		File fm_file = new File(line_.getOptionValue(FM));
		return FeatureModelManager.load(fm_file.toPath()).getObject();
	}

	public FeaturedMealy<String, Word<String>> read_and_get_ffsm(IFeatureModel fm) {
		File ffsm_file_1 = new File(line_.getOptionValue(FFSM));
		FeaturedMealy<String, Word<String>> ffsm = null;
		try {
			ffsm = FeaturedMealyUtils.getInstance().loadFeaturedMealy(ffsm_file_1, fm);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ffsm;
	}

	public Map<String, List<String>> read_and_get_features_alphabet() {
		List<String> lines = read_file_lines(line_.getOptionValue(ALPHABET));
		return create_and_get_alphabet(lines);
	}

	private static List<String> read_file_lines(String file_path) {
		List<String> lines = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			String line_;
			while ((line_ = br.readLine()) != null) {
				lines.add(line_);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	private static Map<String, List<String>> create_and_get_alphabet(List<String> lines) {
		Map<String, List<String>> alphabet = new HashMap<>();
		for (String line_ : lines) {
			
			String[] splitted_line = line_.split(": ");
			String feature = splitted_line[0].trim();
			List<String> input_signals = Arrays.asList(splitted_line[1].split(", "));
			
			if(feature.equals(ALL_FEATURES))
				for(String inp : input_signals)
					alphabet.put(inp, null);
			
			alphabet.put(feature, input_signals);
		}
		
		return alphabet;
	}

	private static Integer extract_number(String input) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(input);

		matcher.find();
		String number_str = matcher.group();
		return Integer.parseInt(number_str);
	}

    public CompactMealy<String, Word<String>> clear_and_sort_dfa(CompactDFA<String> dfa) {
		get_corresponds_states(dfa);
		save_dfa_kiss_file(dfa);
		try {
			sort_dfa_output();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return read_and_get_det_fsm();
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

	private CompactMealy<String, Word<String>> read_and_get_det_fsm() {
		File fsm_file = new File(line_.getOptionValue(OUT) + KISS_FILE_EXTENSION);
		CompactMealy<String, Word<String>> det_fsm = null;
		try {
			det_fsm = loadMealyMachine(fsm_file);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return det_fsm;
	}

	void save_fsm_graph_view() {
		fsm_ = dfsm_;
		StringBuilder buffer = new StringBuilder();
		add_header_to_buffer(buffer);
		add_states_to_buffer(buffer);
		add_transitions_to_buffer(buffer);
		add_bottom_complementary_lines_to_buffer(buffer);
		write_output(buffer, GRAPH_VIEW_FILE_EXTENSION, false);
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

	public void write_output(StringBuilder buffer, String file_extension, boolean with_sort) {
        String fileNameWithExtension = line_.getOptionValue(FFSM).substring(line_.getOptionValue(FFSM).lastIndexOf("\\") + 1);
        String inputFileName = fileNameWithExtension.substring(0, fileNameWithExtension.lastIndexOf("."));
	    try (BufferedWriter bw = new BufferedWriter(new FileWriter(line_.getOptionValue(OUT) + "\\" + inputFileName + "_minimized" + file_extension))) {
	        List<String> lines = new ArrayList<>();
	        String[] bufferLines = buffer.toString().split("\n");

	        // Add lines from buffer to the list
	        Collections.addAll(lines, bufferLines);

	        // Sort the list if with_sort is true
	        if (with_sort) {
	            Collections.sort(lines, new Comparator<String>() {
	                @Override
	                public int compare(String line1, String line2) {
	                    int state1 = Integer.parseInt(line1.split("@")[0].substring(1));
						int state2 = Integer.parseInt(line2.split("@")[0].substring(1));
						return Integer.compare(state1, state2);
	                }
	            });
	        }

	        // Write the lines to the output file
	        for (String line : lines) {
	            bw.write(line);
	            bw.newLine();
	        }

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
					buffer.append(String.format("s%s -- %s / %s -> s%s\n", si, input, output, sj));
				}
			}
		}
		write_output(buffer, KISS_FILE_EXTENSION, false);
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
		write_output(buffer, KISS_FILE_EXTENSION, false);
	}

    public CompactNFA<String> read_and_get_non_det_fsm() {
		File nfsm_file = new File(line_.getOptionValue(OUT) + TEMP_EXTENSION);
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

	public void write_nfms(CompactNFA<String> nfsm) {
		StringBuilder buffer = new StringBuilder();

		for (Integer state : nfsm.getStates()) {
			for (String in : nfsm.getLocalInputs(state)) {
				Collection<Integer> neighbors = nfsm.getTransitions(state, in);
				for (Integer neighbors_it : neighbors) {
					String si = state.toString();
					String sj = neighbors_it.toString();
					String input = in.split("/")[0], output = in.split("/")[1];
					buffer.append(String.format("s%s -- %s / %s -> s%s\n", si, input, output, sj));
				}
			}
		}
		write_output(buffer, TEMP_EXTENSION, false);
	}

	public CompactMealy<String, Word<String>> loadMealyMachine(File f) throws Exception {

		Pattern kissLine = Pattern.compile("\\s*(\\S+)\\s+--\\s+(\\S+)\\s*/\\s*(\\S+)" 
										   + "\\[([^\\]]+)\\]"
										   + "\\s+->\\s+(\\S+)\\s*");

		BufferedReader br = new BufferedReader(new FileReader(f));

		List<String[]> trs = new ArrayList<String[]>();

		HashSet<String> abcSet = new HashSet<>();
		List<String> abc = new ArrayList<>();

		//		int count = 0;

		while(br.ready()){
			String line = br.readLine();
			Matcher m = kissLine.matcher(line);
			if(m.matches()){
								// System.out.println(m.group(0));
								// System.out.println(m.group(1));
								// System.out.println(m.group(2));
								// System.out.println(m.group(3));
								// System.out.println(m.group(4));
								// System.out.println(m.group(5));


				String[] tr = new String[5];
				tr[0] = m.group(1);
				tr[1] = m.group(2); 
				tr[4] = m.group(4);
				if(!abcSet.contains(tr[1] + "[" + tr[4] + "]")){
					abcSet.add(tr[1] + "[" + tr[4] + "]");
					abc.add(tr[1] + "[" + tr[4] + "]");					
				}
				tr[2] = m.group(3);
				tr[3] = m.group(5);
				trs.add(tr);
			}
			//			count++;
		}

		br.close();

		Collections.sort(abc);
		Alphabet<String> alphabet = Alphabets.fromCollection(abc);
		CompactMealy<String, Word<String>> mealym = new CompactMealy<String, Word<String>>(alphabet);
 
		Map<String,Integer> states = new HashMap<String,Integer>();
		Integer si=null,sf=null;

		Map<String,Word<String>> words = new HashMap<String,Word<String>>();		


		WordBuilder<String> aux = new WordBuilder<>();

		aux.clear();
		aux.append(OMEGA_SYMBOL);
		words.put(OMEGA_SYMBOL.toString(), aux.toWord());

		Integer s0 = null;

		for (String[] tr : trs) {
			if(!states.containsKey(tr[0])) states.put(tr[0], mealym.addState());
			if(!states.containsKey(tr[3])) states.put(tr[3], mealym.addState());

			si = states.get(tr[0]);
			if(s0==null) s0 = si;
			sf = states.get(tr[3]);

			if(!words.containsKey(tr[1] + "[" + tr[4] + "]")){
				aux.clear();
				aux.add(tr[1] + "[" + tr[4] + "]");
				words.put(tr[1] + "[" + tr[4] + "]", aux.toWord());
			}
			if(!words.containsKey(tr[2])){
				aux.clear();
				aux.add(tr[2]);
				words.put(tr[2], aux.toWord());
			}
			mealym.addTransition(si, words.get(tr[1] + "[" + tr[4] + "]").toString(), sf, words.get(tr[2]));
		}

		for (Integer st : mealym.getStates()) {
			for (String in : alphabet) {
				//				System.out.println(mealym.getTransition(st, in));
				if(mealym.getTransition(st, in)==null){
					mealym.addTransition(st, in, st, OMEGA_SYMBOL);
				}
			}
		}


		mealym.setInitialState(s0);

		return mealym;
	}
}
