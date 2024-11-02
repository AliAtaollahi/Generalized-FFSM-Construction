package uk.le.ac.comparingFsmStructuralSimilarity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.NodeReader;
import org.prop4j.Not;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import guidsl.and;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;
import uk.le.ac.ffsm.ConditionalState;
import uk.le.ac.ffsm.ConditionalTransition;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.FeaturedMealyUtils;
import uk.le.ac.ffsm.ProductMealy;

public class DataManagerFactory {
	private static final String HELP = "h";
	private static final String FSM = "fsm";
	private static final String NFSM = "nfsm";
	private static final String FM = "fm";
	private static final String NO_LOOP = "no_loop";

	private static final String K_VALUE = "k";
	private static final String T_VALUE = "t";
	private static final String R_VALUE = "r";

	private String K_DEFAULT_VALUE = "0.50";
	private String T_DEFAULT_VALUE = "0.50";
	private String R_DEFAULT_VALUE = "1.40";


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
	
	public IFeatureModel read_and_get_feature_model() {
		File fm_file = new File(line_.getOptionValue(FM));
		return FeatureModelManager.load(fm_file.toPath()).getObject();
	}

	private static Options create_and_get_options() {
		Options options = new Options();
		options.addOption(HELP, false, "Help menu");
		options.addOption(FSM, true, "deterministic fsm");
		options.addOption(NFSM, true, "non deterministic fsm");
		options.addOption(FM, true, "Feature model");
		options.addOption(NO_LOOP, true, "Delete self-loops");
		options.addOption(K_VALUE, true, "Attenuation (i.e., surrounding states)");
		options.addOption(T_VALUE, true, "Threshold (i.e., only above)");
		options.addOption(R_VALUE, true, "Ratio (i.e., r times better only)");
		return options;
	}

	private void check_necessary_options() {
		if (line_.hasOption(HELP) || !line_.hasOption(FSM) || !line_.hasOption(NFSM) 
			|| !line_.hasOption(FM) || !line_.hasOption(NO_LOOP)) {
			formatter_.printHelp("Generalize FFSM", options_);
			System.exit(0);
		}
	}

	public File read_and_get_nfsm_prod() {
		File nfsm_file = new File(line_.getOptionValue(NFSM));
		nfsm_file = resemble_to_ffsm(nfsm_file);
		return nfsm_file;
	}

	private File resemble_to_ffsm(File nfsm_file) {
		File tempFile;
		BufferedReader reader = null;
		BufferedWriter writer = null;

		try {
			tempFile = File.createTempFile("tempfile", ".txt");

			reader = new BufferedReader(new FileReader(nfsm_file));
			writer = new BufferedWriter(new FileWriter(tempFile));

			String line;
			while ((line = reader.readLine()) != null) {
				line = line.replaceAll(" / ", "@[()]/");
				line = line.replaceAll("s(\\d+)", "s$1@[()]");
				writer.write(line);
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (reader != null) reader.close();
				if (writer != null) writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return tempFile;
    }

    public FeaturedMealy<String, Word<String>> loadFeaturedMealy(File f_ffsm, IFeatureModel fm, boolean is_no_loop) throws IOException{
			Pattern kissLine = Pattern.compile(
					"\\s*"
					+ "(\\S+)" + "@" + "\\[([^\\]]+)\\]"
					+ "\\s+--\\s+"+
					"\\s*"
					+ "(\\S+)" + "@" + "\\[([^\\]]+)\\]"
					+ "\\s*/\\s*"
					+ "(\\S+)"
					+ "\\s+->\\s+"
					+ "(\\S+)" + "@" + "\\[([^\\]]+)\\]"
					);
	
			BufferedReader br = new BufferedReader(new FileReader(f_ffsm));
			
			Set<String> abc = new LinkedHashSet<>();
			List<String[]> linesTruncated = new ArrayList<>();
			if(br.ready()){
				String line = null;
				while(br.ready()){
					line = br.readLine();
					Matcher m = kissLine.matcher(line);
					if(m.matches()){
						String[] tr = new String[7];
						IntStream.range(1, tr.length+1).forEach(idx-> tr[idx-1] = m.group(idx));
						abc.add(tr[2]);
						linesTruncated.add(tr);
					}
				}
				
			}
			
			br.close();
			
			List<String> abcList = new ArrayList<>(abc);
			Collections.sort(abcList);
			Alphabet<String> alphabet = Alphabets.fromCollection(abcList);
			FeaturedMealy<String, Word<String>> ffsm = new FeaturedMealy<>(alphabet,fm);
			Map<String, Node> conditionalInputs = FeaturedMealyUtils.getInstance().mapConditionalInputs(fm);
			ffsm.setConditionalInputs(conditionalInputs);

			
			ConditionalState<ConditionalTransition<String,Word<String>>> s0 = null;
			Map<Integer,ConditionalState<ConditionalTransition<String,Word<String>>>> statesMap = new HashMap<>();
			Map<String,Integer> statesId = new HashMap<>();
			int stateId = 0;
			for (String[] tr : linesTruncated) {
				/* Conditional state origin */
				if(!statesId.containsKey(tr[0])) statesId.put(tr[0],stateId++);
				
				Integer si = statesId.get(tr[0]); 
				Node si_c = nodeReader(tr[1]);
				if(!statesMap.containsKey(si)) {
					statesMap.put(si,ffsm.addState((si_c)));
					if(s0==null) {
						s0 = statesMap.get(si);
					}
				}
				
				/* Conditional Input */
				String in = tr[2];
				Node in_c = nodeReader(tr[3]);
				
				/* Output */
				Word out = Word.epsilon();
				out = out.append(tr[4]);
				
				/* Conditional state destination */
				if(!statesId.containsKey(tr[5])) statesId.put(tr[5],stateId++);
				Integer sj = statesId.get(tr[5]);
				Node sj_c = nodeReader(tr[6]);
				if(!statesMap.containsKey(sj)) {
					statesMap.put(sj,ffsm.addState((sj_c)));
				}

				ConditionalTransition newTr;
				// if
				if (is_no_loop && si != sj)
					newTr = ffsm.addTransition(statesMap.get(si), in, statesMap.get(sj), out, (in_c));
			}
			ffsm.setInitialState(s0);
	
			return ffsm;
	}

	public File read_and_get_fsm_prod() {
		File fsm_file = new File(line_.getOptionValue(FSM));
		return fsm_file;
	}

	public ProductMealy<String, Word<String>> loadProductMachine(File f, IFeatureModel fm, boolean is_no_loop) throws Exception {

		Pattern kissLine = Pattern.compile("\\s*(\\S+)\\s+--\\s+(\\S+)\\s*/\\s*(\\S+)\\s+->\\s+(\\S+)\\s*");

		BufferedReader br = new BufferedReader(new FileReader(f));

		List<String[]> trs = new ArrayList<String[]>();

		HashSet<String> abcSet = new LinkedHashSet<>();
		List<String> abc = new ArrayList<>();

		//		int count = 0;
		String configuration = br.readLine();
		String[] configurations_split = configuration.split("\t");

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
				if(!abcSet.contains(tr[1])){
					abcSet.add(tr[1]);
					abc.add(tr[1]);					
				}
				tr[2] = m.group(3);
				tr[3] = m.group(4);
				trs.add(tr);
			}
			//			count++;
		}

		br.close();
		
		List<Node> configuration_list = new ArrayList<>();
		Set<String> configuration_names = new HashSet<>();
		
		for (String string : configurations_split) {
			if(string.length()==0) continue;
			Node newNode = nodeReader(string);
			configuration_list.add(newNode);
			if(newNode instanceof Literal) configuration_names.add(((Literal)newNode).toString());
			if(newNode instanceof Not) configuration_names.add(((Not)newNode).getChildren()[0].toString());
		}
		for (IFeature node : fm.getFeatures()) {
			if(node.getName().equals("TRUE")) continue;
			if(!configuration_names.contains(node.getName())) {
				configuration_list.add(new Not(nodeReader(node.getName())));
			}
		}
		
		Collections.sort(abc);
		Alphabet<String> alphabet = Alphabets.fromCollection(abc);
		ProductMealy<String, Word<String>> mealym = new ProductMealy<String, Word<String>>(alphabet,fm,configuration_list);
 
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
			// if
			if(is_no_loop && si != sf)
				mealym.addTransition(si, words.get(tr[1]).toString(), sf, words.get(tr[2]));
		}

		for (Integer st : mealym.getStates()) {
			for (String in : alphabet) {
				//				System.out.println(mealym.getTransition(st, in));
				if(mealym.getTransition(st, in)==null){
					// if
					if(is_no_loop && st != st) 
						mealym.addTransition(st, in, st, OMEGA_SYMBOL);
				}
			}
		}


		mealym.setInitialState(s0);

		return mealym;
	}

	public  Node nodeReader(String constraint) {
		NodeReader nr = new NodeReader();
		nr.activateTextualSymbols();
		return nr.stringToNode(constraint);
	}

    public double get_k_value() {
        return Double.valueOf(line_.getOptionValue(K_VALUE, K_DEFAULT_VALUE));
    }

	public double get_t_value() {
		return Double.valueOf(line_.getOptionValue(T_VALUE, T_DEFAULT_VALUE));
	}

    public double get_r_value() {
        return Double.valueOf(line_.getOptionValue(R_VALUE, R_DEFAULT_VALUE));
    }

    public boolean get_loop_condition() {
		return Boolean.parseBoolean(line_.getOptionValue(NO_LOOP));
    }
}
