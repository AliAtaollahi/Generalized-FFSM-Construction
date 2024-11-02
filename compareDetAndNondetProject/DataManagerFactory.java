package uk.le.ac.compareDetAndNondetProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;
import uk.le.ac.fsm.MealyUtils;

public class DataManagerFactory {
	private static final String HELP = "h";
	private static final String FSM = "fsm";
	private static final String NFSM = "nfsm";

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
		options.addOption(FSM, true, "deterministic fsm");
		options.addOption(NFSM, true, "non deterministic fsm");
		return options;
	}

	private void check_necessary_options() {
		if (line_.hasOption(HELP) || !line_.hasOption(FSM) || !line_.hasOption(NFSM)) {
			formatter_.printHelp("Generalize FFSM", options_);
			System.exit(0);
		}
	}

	public CompactMealy<String, Word<String>> read_and_get_det_fsm() {
		File fsm_file = new File(line_.getOptionValue(FSM));
		CompactMealy<String, Word<String>> det_fsm = null;
		try {
			det_fsm = MealyUtils.getInstance().loadMealyMachine(fsm_file);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return det_fsm;
	}

    public FastNFA<String> read_and_get_non_det_fsm() {
		File nfsm_file = new File(line_.getOptionValue(NFSM));
		FastNFA<String> non_det_fsm = null;
		try {
			non_det_fsm = loadNonDetMealyMachine(nfsm_file);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return non_det_fsm;
    }

	private FastNFA<String> loadNonDetMealyMachine(File f) throws Exception {

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
		FastNFA<String> mealym = new FastNFA<String>(alphabet);
 
		Map<String,Integer> states = new HashMap<String,Integer>();
		Integer si=null,sf=null;

		Map<String,Word<String>> words = new HashMap<String,Word<String>>();		

		WordBuilder<String> aux = new WordBuilder<>();

		aux.clear();
		aux.append(OMEGA_SYMBOL);
		words.put(OMEGA_SYMBOL.toString(), aux.toWord());

		Integer s0 = null;

		for (String[] tr : trs) {
			if(!states.containsKey(tr[0])) states.put(tr[0], extract_number(mealym.addState().toString()));
			if(!states.containsKey(tr[3])) states.put(tr[3], extract_number(mealym.addState().toString()));

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

}
