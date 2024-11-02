package uk.le.ac.ffsm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
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

import org.prop4j.And;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.NodeReader;
import org.prop4j.NodeWriter;
import org.prop4j.NodeWriter.Notation;

import org.prop4j.Not;
import org.prop4j.Or;

import de.learnlib.api.query.DefaultQuery;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.base.impl.FeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;

public class FeaturedMealyUtils {
	
	public static final Word<String> OMEGA_SYMBOL = Word.fromLetter("Ω");
	
	private static final long MAX_CONFIGURATIONS = 100000;
	private static final long MIN_CONFIGURATIONS = 10;
	public static IFeatureModelFactory fact = FMFactoryManager.getDefaultFactory();
	
	private static FeaturedMealyUtils instance;
	
	private FeaturedMealyUtils() { }
	
	public static FeaturedMealyUtils getInstance() {
		if(instance == null){
			FeaturedMealyUtils.instance = new FeaturedMealyUtils();
		}
		return instance;
	}
	
	
	public FeaturedMealy<String, Word<String>> loadFeaturedMealy(File f_ffsm, IFeatureModel fm) throws IOException{
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
				
				ConditionalTransition newTr = ffsm.addTransition(statesMap.get(si), in, statesMap.get(sj), out, (in_c));
			}
			ffsm.setInitialState(s0);
	
			return ffsm;
	}

	
	public void saveFFSM(FeaturedMealy<String, Word<String>> ffsm, File f) throws Exception {
		saveFFSM(ffsm, f,true);
		
	}
	public void saveFFSM(FeaturedMealy<String, Word<String>> ffsm, File f, boolean plotLoop) throws Exception {

		BufferedWriter bw = new BufferedWriter(new FileWriter(f));	
		bw.write("digraph g {\n");
		bw.write("	edge [lblstyle=\"above, sloped\"];\n");
		for (ConditionalState<ConditionalTransition<String, Word<String>>> si : ffsm.getStates()) {
			bw.write(String.format("	s%d [shape=\"circle\" label=\"%s@[%s]\"];\n", si.getId(), si.getId(), nodeWriter(si.getCondition())));
		}
		for (ConditionalState<ConditionalTransition<String, Word<String>>> si : ffsm.getStates()) {
			for (String in : ffsm.getInputAlphabet()) {
				for (ConditionalTransition<String, Word<String>> tr : ffsm.getTransitions(si,in)) {
					if(!plotLoop && tr.getPredecessor().equals(tr.getSuccessor())) continue;
					bw.write(String.format("	s%d -> s%d [label=\"%s / %s [%s]\"];\n", tr.getPredecessor().getId(), tr.getSuccessor().getId(), tr.getInput().toString(),tr.getOutput().toString(),nodeWriter(tr.getCondition())));
				}

			}

		}

		bw.write("	__start0 [label=\"\" shape=\"none\" width=\"0\" height=\"0\"];\n");
		bw.write("	__start0 -> s"+ffsm.getInitialState().getId()+";\n");
		bw.write("}");
		bw.close();
	}

	public void saveFFSM_kiss(FeaturedMealy<String, Word<String>> ffsm, File f) throws Exception {
		saveFFSM_kiss(ffsm, f, true);
	}
	
	public void saveFFSM_kiss(FeaturedMealy<String, Word<String>> ffsm, File f, boolean plotLoop) throws Exception {

		BufferedWriter bw = new BufferedWriter(new FileWriter(f));	
		List<ConditionalState<ConditionalTransition<String, Word<String>>>> states = new ArrayList<>(ffsm.getStates());
		states.remove(ffsm.getInitialState());
		states.add(0,ffsm.getInitialState());
		
		for (ConditionalState<ConditionalTransition<String, Word<String>>> si : states) {
			for (String in : ffsm.getInputAlphabet()) {
				for (ConditionalTransition<String, Word<String>> tr : ffsm.getTransitions(si,in)) {
					if(!plotLoop && tr.getPredecessor().equals(tr.getSuccessor())) continue;
					bw.write(String.format("s%s@[%s] -- %s@[%s]/%s -> s%s@[%s]\n", 
							tr.getPredecessor().getId(), 
							nodeWriter(tr.getPredecessor().getCondition()),
							tr.getInput().toString(),
							nodeWriter(tr.getCondition()),
							tr.getOutput().toString(),
							tr.getSuccessor().getId(),
							nodeWriter(tr.getSuccessor().getCondition())
							));
				}

			}

		}
		bw.close();
	}
	
	public void saveFSM_kiss(CompactMealy<String, Word<String>> fsm, File f) throws Exception {
		saveFSM_kiss(fsm, f, true, null);
	}
	
	public void saveFSM_kiss(CompactMealy<String, Word<String>> fsm, File f, boolean plotLoop) throws Exception {
		saveFSM_kiss(fsm, f, plotLoop, null);
	}
	
	public void saveFSM_kiss(CompactMealy<String, Word<String>> fsm, File f, String header) throws Exception {
		saveFSM_kiss(fsm, f, true, header);
	}
	public void saveFSM_kiss(CompactMealy<String, Word<String>> fsm, File f, boolean plotLoop, String header) throws Exception {

		BufferedWriter bw = new BufferedWriter(new FileWriter(f));	
		List<Integer> states = new ArrayList<>(fsm.getStates());
		states.remove(fsm.getInitialState());
		states.add(0,fsm.getInitialState());
		
		if(header != null) {
			bw.append(header);
			bw.append("\n");
		}
		
		for (Integer si : states) {
			for (String in : fsm.getInputAlphabet()) {
				for (CompactMealyTransition<Word<String>> tr : fsm.getTransitions(si,in)) {
					if(!plotLoop && tr.getSuccId() == si) continue;
					bw.write(String.format("s%d -- %s/%s -> s%d\n", 
							si, 
							in,
							tr.getOutput().toString(),
							tr.getSuccId()
							));
				}

			}

		}
		bw.close();
	}
	
	public void printFFSM_kiss(FeaturedMealy<String, String> ffsm) throws Exception {
		printFFSM_kiss(ffsm,true);
	}
	
	public void printFFSM_kiss(FeaturedMealy<String, String> ffsm, boolean plotLoop) throws Exception {

		List<ConditionalState<ConditionalTransition<String, String>>> states = new ArrayList<>(ffsm.getStates());
		states.remove(ffsm.getInitialState());
		states.add(0,ffsm.getInitialState());
		
		for (ConditionalState<ConditionalTransition<String, String>> si : states) {
			for (String in : ffsm.getInputAlphabet()) {
				for (ConditionalTransition<String, String> tr : ffsm.getTransitions(si,in)) {
					if(!plotLoop && tr.getPredecessor().equals(tr.getSuccessor())) continue;
					System.out.println(String.format("s%s@[%s] -- %s@[%s]/%s -> s%s@[%s]", 
							tr.getPredecessor().getId(), 
							nodeWriter(tr.getPredecessor().getCondition()),
							tr.getInput().toString(),
							nodeWriter(tr.getCondition()),
							tr.getOutput().toString(),
							tr.getSuccessor().getId(),
							nodeWriter(tr.getSuccessor().getCondition())
							));
				}

			}

		}
	}

	public ProductMealy<String, Word<String>> loadProductMachine(File f, IFeatureModel fm) throws Exception {

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
			mealym.addTransition(si, words.get(tr[1]).toString(), sf, words.get(tr[2]));
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

	// https://github.com/vhfragal/ConFTGen-tool/blob/450dd0a0e408be6b42e223d41154eab2269427f3/work_neon_ubu/br.icmc.ffsm.ui.base/src/br/usp/icmc/feature/logic/FFSMProperties.java#L2384
	public  <I, O> boolean isDeterministic(FeaturedMealy<I,O> ffsm){
		FeatureModel fm = (FeatureModel) ffsm.getFeatureModel().clone();

		Alphabet<I> alphabet = ffsm.getInputAlphabet();

		for (ConditionalState<ConditionalTransition<I,O>> cState : ffsm.getStates()) {
			for (I inputIdx : alphabet) {
				Collection<ConditionalTransition<I,O>> outTrs = ffsm.getTransitions(cState,inputIdx);
				for (ConditionalTransition<I,O> tr1 : outTrs) {
					List<Node> ands_l = new ArrayList<>();
					for (ConditionalTransition<I,O> tr2 : outTrs) {
						if(tr1.equals(tr2)) continue;
						ands_l.clear();
						ands_l.add(new And(
								new And(tr1.getPredecessor().getCondition(),tr1.getCondition(),tr1.getSuccessor().getCondition()),
								new And(tr2.getPredecessor().getCondition(),tr2.getCondition(),tr2.getSuccessor().getCondition())
								));
						IConstraint ands = fact.createConstraint(fm, new And(ands_l));
						fm.addConstraint(ands);
						Configuration conf = new Configuration(fm);
						if(conf.number(MIN_CONFIGURATIONS)!=0) {
							fm.reset();
							return false;
						}
						fm.removeConstraint(ands);
					}
				}
				
			}
		}
		fm.reset();
		return true;
	}

	public  <I, O> boolean isComplete(FeaturedMealy<I,O> ffsm){
		FeatureModel fm = (FeatureModel) ffsm.getFeatureModel().clone();

		Alphabet<I> alphabet = ffsm.getInputAlphabet();

		for (ConditionalState<ConditionalTransition<I,O>> cState : ffsm.getStates()) {

			IConstraint stateAnd = fact.createConstraint(fm, cState.getCondition());
			fm.addConstraint(stateAnd); // check origin condition
			List<Node> notAnds = new ArrayList<>(); 
			for (I inputIdx : alphabet) {
				Collection<ConditionalTransition<I,O>> outTrs = ffsm.getTransitions(cState,inputIdx);
				if(outTrs.isEmpty()) {
					return false;
				}
				notAnds.add(ffsm.getConditionalInputs().get(inputIdx));
				for (ConditionalTransition<I,O> tr : outTrs) {
					notAnds.add(new Not(new And(tr.getSuccessor().getCondition()))); // input and destination conditions
				}				
			}
			IConstraint and_notAnds = fact.createConstraint(fm, new And(notAnds));
			fm.addConstraint(and_notAnds); // check origin condition
			Configuration conf = new Configuration(fm);
			if(conf.number(MIN_CONFIGURATIONS)!=0) {
				fm.reset();
				return false;
			}
			fm.removeConstraint(and_notAnds);
			fm.removeConstraint(stateAnd);
		}
		fm.reset();
		return true;

	}
	
	public <I, O> List<DefaultQuery<I, Word<O>>> getValidTraces(FeaturedMealy<I,O> ffsm){
		List<DefaultQuery<I, Word<O>>> valid_q = new ArrayList<>();
		
		Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> allValid = getAllValidPaths(ffsm);
		
//		for(ConditionalState<ConditionalTransition<I, O>> cstate : ffsm.getStates()){
//			if(!ffsm.getInitialStates().contains(cstate)){
//				if(allValid.get(cstate) == null || allValid.get(cstate).size() <= 0){
//					return valid_q; //there is no path for this state 
//				} 
//			}
//		}
//		
//		//remove invalid paths
//		boolean epath = check_valid_paths(ffsm,allValid);
		
		for (ConditionalState<ConditionalTransition<I, O>> a_state : allValid.keySet()) {
			List<List<ConditionalTransition<I, O>>> a_path = allValid.get(a_state);
			for (List<ConditionalTransition<I, O>> ios : a_path) {
				WordBuilder<I> wbIn = new WordBuilder<>();
		        WordBuilder<O> wbOut = new WordBuilder<>();
				for (ConditionalTransition<I, O> an_io : ios) {
					wbIn.append(an_io.getInput());
					wbOut.append(an_io.getOutput());
				}
				DefaultQuery<I, Word<O>> a_query = new DefaultQuery<>(wbIn.toWord());
				a_query.answer(wbOut.toWord());
				valid_q.add(a_query);
			}
		}
		return valid_q;
	}
	
	public  <I,O> boolean isInitiallyConnected(FeaturedMealy<I,O> ffsm){
		Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> allValid = null;
		allValid = getAllValidPaths(ffsm);
		
		for(ConditionalState<ConditionalTransition<I, O>> cstate : ffsm.getStates()){
			if(!ffsm.getInitialStates().contains(cstate)){
				if(allValid.get(cstate) == null || allValid.get(cstate).size() <= 0){
					return false; //there is no path for this state 
				} 
			}
		}
		
		//remove invalid paths
		boolean epath = check_valid_paths(ffsm,allValid);			
		// if a state has no path that reach it
		if(!epath){
			return false;
		}
		
		//check reachability of products
		Set<ConditionalState<ConditionalTransition<I, O>>> uncovStates = null;
		uncovStates = check_product_coverage(ffsm.getFeatureModel(),allValid);
		if(uncovStates.size()!=0){				
			return false;
		}
		
		//reduce redundant paths
		// TODO check why reduce_redundant_paths is buggy?
		//reduce_redundant_paths(ffsm,allValid); 
		
		//reduce set of paths
		for(ConditionalState<ConditionalTransition<I, O>> s: allValid.keySet()){
			if(!ffsm.getInitialState().equals(s)){
				reduce_state_cover(s,allValid);
			}
		}	
		
		//check reachability of products 2
		uncovStates = check_product_coverage(ffsm.getFeatureModel(),allValid);
		if(uncovStates.size()!=0){				
			return false;
		}
					
		//generate table with conditional inputs
		Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> condQSet =  null; 
		condQSet = create_state_cover_set(ffsm,allValid);
		
		return true;
	}
	
	private  <I,O> void 
			reduce_state_cover(
				ConditionalState<ConditionalTransition<I, O>> s,
				Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> allValid) {
		// TODO Auto-generated method stub
		
	}

	private  <I,O> Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> 
		create_state_cover_set(
			FeaturedMealy<I, O> ffsm,
			Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> allValid) {
		// TODO Auto-generated method stub
		return null;
	}

	private  <I,O> void 
		reduce_redundant_paths(
				FeaturedMealy<I, O> ffsm,
				Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> allValid) {
		
		IFeatureModel fm = ffsm.getFeatureModel().clone();
		
		new_state:for(ConditionalState<ConditionalTransition<I, O>> s: allValid.keySet()){
			if(!ffsm.getInitialState().equals(s)){
				List<List<ConditionalTransition<I, O>>> checked_paths = new ArrayList<>();
				List<List<ConditionalTransition<I, O>>> valid_paths = new ArrayList<>();
				List<List<ConditionalTransition<I, O>>> original_paths = new ArrayList<>(allValid.get(s));
				List<Node> checked_cond = new ArrayList<>();
				
				new_path:for(List<ConditionalTransition<I, O>> path : original_paths){
					List<Node> path_nodes = new ArrayList<>();
					for(ConditionalTransition<I, O>ft: path){
//						path_cond.add(new And(ft.getPredecessor().getCondition(),ft.getCondition(),ft.getSuccessor()));
						path_nodes.add(ffsm.getConditionalInputs().get(ft.getInput()));
						path_nodes.add(ft.getPredecessor().getCondition());
						path_nodes.add(ft.getCondition());
						path_nodes.add(ft.getSuccessor().getCondition());
					}
					Node path_cond = new And(path_nodes);
					//check if this path has a cond prefix of another path
					int i=0;
					for(Node ccond : checked_cond){
						if(check_cond_prefix(fm,path_cond, ccond)){
							allValid.get(s).remove(path);
							continue new_path;
						}
						if(check_cond_prefix(fm,ccond, path_cond)){
							allValid.get(s).remove(checked_paths.get(i));
							valid_paths.remove(checked_paths.get(i));
						}						
						i++;
					}					
					checked_cond.add(path_cond);
					checked_paths.add(path);
					valid_paths.add(path);
					if(check_path_coverage(fm,s, valid_paths)){
						// remove the rest
						allValid.get(s).clear();
						allValid.get(s).addAll(valid_paths);						
						continue new_state;
					}
				}			
			}
		}
	}

	private  boolean check_cond_prefix(IFeatureModel featModel, Node cond_prefix, Node cond_seq) {
		IFeatureModel fm = featModel.clone();
		Configuration conf =  null;
		
		IConstraint andC1C2 = fact.createConstraint(fm, new And(cond_prefix,cond_seq));
		fm.addConstraint(andC1C2);
		
		conf = new Configuration(fm);
		boolean sat_andC1C2 = (conf.number(MIN_CONFIGURATIONS)!=0);
		
		IConstraint andC1notC2 = fact.createConstraint(fm, new And(cond_prefix, new Not(cond_seq)));
		fm.addConstraint(andC1notC2);
		
		conf = new Configuration(fm);
		boolean sat_andC1notC2 = (conf.number(MIN_CONFIGURATIONS)!=0);
		
		if(sat_andC1C2 && !sat_andC1notC2){
			return true;
		}
		return false;	
	}

	private  <I,O> Map<ConditionalState<ConditionalTransition<I,O>>, List<List<ConditionalTransition<I,O>>>> 
			getAllValidPaths(FeaturedMealy<I,O> ffsm){
		Map<ConditionalState<ConditionalTransition<I,O>>, List<List<ConditionalTransition<I,O>>>> allPaths = new HashMap<ConditionalState<ConditionalTransition<I,O>>,List<List<ConditionalTransition<I,O>>>>();

		ConditionalState<ConditionalTransition<I,O>> s0 = ffsm.getInitialState();
		
		Set<ConditionalTransition<I,O>> no_loop_tr = new LinkedHashSet<>();
		for (ConditionalState<ConditionalTransition<I,O>> state : ffsm.getStates()) {
			for (I input : ffsm.getInputAlphabet()) {
				for (ConditionalTransition<I,O> tr : ffsm.getTransitions(state, input)) {
					if(!tr.getPredecessor().equals(tr.getSuccessor())) no_loop_tr.add(tr);
				}
			}			
		}
		
		Set<ConditionalState<ConditionalTransition<I,O>>> found_fc = new LinkedHashSet<>();
		Set<ConditionalState<ConditionalTransition<I,O>>> nfound_fc = new LinkedHashSet<>(ffsm.getStates());
		if((ffsm.getInitialStates().size() == 1)) {
			nfound_fc.remove(s0);
			found_fc.add(s0);			
		} else {
			return allPaths;
		}

		nfound_fc.forEach(cState -> allPaths.put(cState, new ArrayList<>()));
		
		for (I input : ffsm.getInputAlphabet()) {
			Collection<ConditionalTransition<I,O>> trs = ffsm.getTransitions(s0 , input);
			for (ConditionalTransition<I,O> tr : trs) {
				if(no_loop_tr.contains(tr)) {
					if(!found_fc.contains(tr.getSuccessor())){
						nfound_fc.remove(tr.getSuccessor());
						found_fc.add(tr.getSuccessor());
					}
					ArrayList<ConditionalTransition<I,O>> currPath = new ArrayList<>();
					currPath.add(tr);
					allPaths.get(tr.getSuccessor()).add(currPath);
				}
			}
		}

		ArrayList<ConditionalState<ConditionalTransition<I,O>>> covered_fc = new ArrayList<>();
		for(ConditionalState<ConditionalTransition<I,O>> cs : found_fc){
			if(!cs.equals(s0)){
				rec_find_paths(ffsm,cs,covered_fc,no_loop_tr,allPaths);
			}				
		}
		return allPaths;
	}
	

	private  <I,O> void rec_find_paths(
			FeaturedMealy<I, O> ffsm, 
			ConditionalState<ConditionalTransition<I,O>> current,
			List<ConditionalState<ConditionalTransition<I,O>>> covered_fc, 
			Set<ConditionalTransition<I,O>> no_loop_tr,
			Map<ConditionalState<ConditionalTransition<I,O>>, List<List<ConditionalTransition<I,O>>>> allPaths) {

		ArrayList<ConditionalTransition<I,O>> listOfTrs = new ArrayList<>();
		for (I input : ffsm.getInputAlphabet())  listOfTrs.addAll(ffsm.getTransitions(current, input));
		
		for(ConditionalTransition<I,O> currTr : listOfTrs){
			if(no_loop_tr.contains(currTr) && !ffsm.getInitialStates().contains(currTr.getSuccessor())){				
				List<List<ConditionalTransition<I,O>>> c_paths = new ArrayList<>(allPaths.get(currTr.getSuccessor()));
				List<List<ConditionalTransition<I,O>>> lc_paths = allPaths.get(current);
				if(check_path_coverage(ffsm.getFeatureModel(),currTr.getSuccessor(), c_paths)){
					if(!covered_fc.contains(currTr.getSuccessor())){
						covered_fc.add(currTr.getSuccessor());
					}
					continue;
				}
				boolean contribute = false;
				prepath: for(List<ConditionalTransition<I,O>> inc_path : lc_paths){					
					//if this c. state was found before in the previous cycle (avoid loops)
					for(ConditionalTransition<I,O> c : inc_path){
						if(c.getSuccessor().equals(currTr.getSuccessor())){
							continue prepath;
						}
					}
					ConditionalState<ConditionalTransition<I,O>> last = inc_path.get(inc_path.size()-1).getPredecessor();
					List<ConditionalTransition<I,O>> new_path = new ArrayList<>();
					if(!last.equals(currTr.getSuccessor()) && c_paths != null){						
						new_path.addAll(inc_path);
						new_path.add(currTr);
						if(!c_paths.contains(new_path) && check_valid_path(ffsm, new_path)){
							c_paths.add(new_path);
							contribute = true;
						}						
					}
				}
				//update paths
				allPaths.put(currTr.getSuccessor(), c_paths);
				if(contribute){
					rec_find_paths(ffsm, currTr.getSuccessor(), covered_fc, no_loop_tr, allPaths);
				}
			}			
		}
	}


	private  <I,O> boolean check_valid_paths(
			FeaturedMealy<I, O> ffsm,
//			IFeatureModel fm,
			Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> allValid) {
		
		for(ConditionalState<ConditionalTransition<I, O>> state: allValid.keySet()){
			List<List<ConditionalTransition<I, O>>> aux_paths = new ArrayList<>(allValid.get(state));
			for(List<ConditionalTransition<I, O>> path : aux_paths){					
				if(!check_valid_path(ffsm, path)) {
					allValid.get(state).remove(path);
				}
			}				
		}
		
		for(ConditionalState<ConditionalTransition<I, O>> state: allValid.keySet()){
			if(allValid.get(state).size() < 1){
				return false;
			}
		}
		return true;
	}

	private  <I, O> boolean check_valid_path(
			FeaturedMealy<I, O> ffsm,
//			IFeatureModel featModel,
			List<ConditionalTransition<I,O>> new_path) {
		
//		IFeatureModel fm = featModel.clone();
		IFeatureModel fm = ffsm.getFeatureModel().clone();
		ArrayList<Node> pathClause = new ArrayList<>();

		IConstraint stateConstr = fact.createConstraint(fm, new_path.get(new_path.size()-1).getSuccessor().getCondition());
		for (ConditionalTransition<I,O> path : new_path){
			pathClause.add(path.getPredecessor().getCondition());
			pathClause.add(path.getPredecessor().getCondition());
			pathClause.add(path.getCondition());
		}
		IConstraint andPathConstrs = fact.createConstraint(fm, new And(pathClause));
		fm.addConstraint(stateConstr);
		fm.addConstraint(andPathConstrs); 
		Configuration conf = new Configuration(fm);
		if(conf.number(MIN_CONFIGURATIONS)==0) {
			return false;
		}
		return true;
	}

	private  <I,O> boolean check_path_coverage(
			IFeatureModel fm_par,
			ConditionalState<ConditionalTransition<I,O>> state, 
			List<List<ConditionalTransition<I,O>>> list) {
		
		FeatureModel fm = (FeatureModel) fm_par.clone();
		List<Node> pathClause = new ArrayList<>();
		IConstraint stateConstr = fact.createConstraint(fm, state.getCondition());
		for (List<ConditionalTransition<I,O>> path : list){
			List<Node> notAndClauses = new ArrayList<>();
			for (ConditionalTransition<I,O> condTr : path) {
				notAndClauses.add(condTr.getPredecessor().getCondition());
				notAndClauses.add(condTr.getCondition());
			}
			pathClause.add(new Not(new And(notAndClauses)));
		}
		IConstraint pathConstr = fact.createConstraint(fm, new And(pathClause));
		fm.addConstraint(stateConstr); // check origin condition;
		fm.addConstraint(pathConstr); // check origin condition
		Configuration conf = new Configuration(fm);
		if(conf.number(MIN_CONFIGURATIONS)!=0) {
			return false;
		}
		return true;
	}

	private  <I,O> Set<ConditionalState<ConditionalTransition<I, O>>> 
			check_product_coverage(
					//FeaturedMealy<I, O> ffsm,
					IFeatureModel featModel,
					Map<ConditionalState<ConditionalTransition<I, O>>, List<List<ConditionalTransition<I, O>>>> allValid) {
		// TODO Auto-generated method stub
		Set<ConditionalState<ConditionalTransition<I, O>>> uncovStates = new LinkedHashSet<>() ;
		
		//check_path_coverage
		for(ConditionalState<ConditionalTransition<I, O>> state: allValid.keySet()){
			if(allValid.get(state) != null){
				if(!check_path_coverage(featModel, state, allValid.get(state))) {
					uncovStates.add(state);
				}
			}			
		}
		return uncovStates;
	}

	public  <I, O> boolean isMinimal(FeaturedMealy<I,O> ffsm){
		return false;

	}

	public  List<String> getAlphabetFromFeature(IFeature feat) {
		List<String> abc = new ArrayList<>();
		String descr = feat.getProperty().getDescription();
		descr=descr.replaceFirst("^Inputs=\\{", "").replaceFirst("\\}$", "");
		if(descr.length()==0) return abc;
		String[] inputs = descr.split(";");
		for (String in : inputs) {
			abc.add(in);
		}
		return abc;
	}
	
	public  Map<String, Node> mapConditionalInputs(IFeatureModel featModel) {
		Map<String,Node> conditionalInputs = new HashMap<>();
		Map<String,Set<Node>> inputCondSet = new HashMap<>();
		List<String> inputs = getAlphabetFromFeature(featModel.getStructure().getRoot().getFeature());
		for (String key : inputs) {
			String[] in_cond = key.split("@");
			in_cond[1] = in_cond[1].replaceAll("^\\[", "").replaceAll("\\]$", "");
			inputCondSet.putIfAbsent(in_cond[0], new LinkedHashSet<>());
			
			inputCondSet.get(in_cond[0]).add(nodeReader(in_cond[1]));
		}
		for (String key : inputCondSet.keySet()) {
			conditionalInputs.put(key, new Or(inputCondSet.get(key)));			
		}
		
		return conditionalInputs;
	}
	public  String nodeWriter(Node n) {
		NodeWriter nw = new NodeWriter(n);
		nw.setNotation(Notation.INFIX);
//		nw.setNotation(Notation.PREFIX);
		nw.setSymbols(NodeWriter.textualSymbols);
		nw.setEnforceBrackets(true);
		return nw.nodeToString();
	}
	
	public  Node nodeReader(String constraint) {
		NodeReader nr = new NodeReader();
		nr.activateTextualSymbols();
		return nr.stringToNode(constraint);
	}
	
	public Or makeConditionAsOr(Node condition) {
		if(condition instanceof And) {
			return new Or(condition);
		}else if(condition instanceof Or) {
			return (Or)condition;
		}
		return null;
	}

	public Set<Node> getAllAnds(Node condition) {
		HashSet<Node> andNodes = new LinkedHashSet<>();
		if(condition instanceof And) {
			andNodes.add(condition);
			return andNodes;
		}else if(condition instanceof Or){
			for (Node node : condition.getChildren()) {
				if(node instanceof And) {
					andNodes.add(node);
				}else {
					andNodes.addAll(getAllAnds(node));
				}
			}
		}		
		return andNodes;
	}

	public void cleanFeaturedMealy(FeaturedMealy<String, Word<String>> ref,
			IFeatureModel fm) {
		Set<Node> allConds = new LinkedHashSet<Node>();
		List<Node> listConds = new ArrayList<Node>();
		
		for (ConditionalState<ConditionalTransition<String, Word<String>>> cState : ref.getStates()) {
			List<Node> simpleCond = new ArrayList<Node>();
			for (Node node : getAllAnds(cState.getCondition())) {
				if(allConds.add(node)) {
					listConds.add(node);
				}
				simpleCond.add(new Literal("c"+(listConds.indexOf(node)+1)));
			}
			cState.setCondition(new Or(simpleCond));
		}

		for (ConditionalState<ConditionalTransition<String, Word<String>>> cState : ref.getStates()) {
			for (String input : ref.getInputAlphabet()) {
				for (ConditionalTransition<String, Word<String>> tr : ref.getTransitions(cState, input)) {
					List<Node> simpleCond = new ArrayList<Node>();
					for (Node node : getAllAnds(tr.getCondition())) {
						if(allConds.add(node)) {
							listConds.add(node);
						}
						simpleCond.add(new Literal("c"+(listConds.indexOf(node)+1)));
					}
					tr.setCondition(new Or(simpleCond));
				}
			}
		}
		ref.getInitialState().setCondition(new Literal("TRUE"));
	}

	public List<SimplifiedTransition<String, Word<String>>> getTransitions(IConfigurableFSM<String, Word<String>> model) {
		List<SimplifiedTransition<String, Word<String>>> list_out = new ArrayList<>();
		for (Integer si : model.getStateIDs()) {
			model.getSimplifiedTransitions(si).values().forEach(tr -> list_out.addAll(tr));
		}
		return list_out;
	}
}
