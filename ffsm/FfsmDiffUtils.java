package uk.le.ac.ffsm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.prop4j.And;
import org.prop4j.Node;
import org.prop4j.Not;
import org.prop4j.Or;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

public class FfsmDiffUtils {

	private static FfsmDiffUtils instance;

	private FfsmDiffUtils() { }

	public static FfsmDiffUtils getInstance() {
		if(instance == null){
			FfsmDiffUtils.instance = new FfsmDiffUtils();
		}
		return instance;
	}
	
	public void removeConflicts(Set<List<Integer>> nPairs, Map<Integer, Set<Integer>> checked) {
		Set<List<Integer>> toRemove = new LinkedHashSet<>();
		for (List<Integer> pair : nPairs) {
			if (checked.get(0).contains(pair.get(0)) || checked.get(1).contains(pair.get(1))) {
				toRemove.add(pair);
			}
		}
		nPairs.removeAll(toRemove);
		
	}
	
	public void removeConflictsByOrder(List<ScorePair<Integer>> scorePairs) {
		Map<Integer,Set<Integer>> checked = new HashMap<>();
		checked.put(0, new LinkedHashSet<>());
		checked.put(1, new LinkedHashSet<>());
		List<ScorePair<Integer>> toRemove = new ArrayList<>();
		for (ScorePair<Integer> pair : scorePairs) {
			if (checked.get(0).contains(pair.getStatei()) || checked.get(1).contains(pair.getStatej())) {
				toRemove.add(pair);
			}else {
				checked.get(0).add(pair.getStatei());
				checked.get(1).add(pair.getStatej());
			}
						
		}
		scorePairs.removeAll(toRemove);
		
	}
	
	public Set<List<Integer>> identifyLandmaks(
			RealVector pairsToScore, 
			IConfigurableFSM<String, Word<String>> fsm1, 
			IConfigurableFSM<String, Word<String>> fsm2,
			double threshold,
			double ration
			) {
		
		// Comments below relate to Section 4.3.1, Page 13:14
		// in the paper https://doi.org/10.1145/2430545.2430549 
		
		Set<List<Integer>> outPairs = new LinkedHashSet<>();
		
		// add the initial states pair to outPairs
		List<Integer> kPairs = new ArrayList<>();
		kPairs = new ArrayList<>();
		kPairs.add(fsm1.getInitialStateIndex());
		kPairs.add(fsm2.getInitialStateIndex());
		outPairs.add(kPairs);
		
		// sort ScorePairs
		List<ScorePair<Integer>> scorePairs = new ArrayList<>();
		for (int i = 0; i < pairsToScore.getDimension(); i++) {
			int x = i / fsm2.getStateIDs().size();
			int y = i % fsm2.getStateIDs().size();
			
			// Check if 'score fall above t'  
			if(pairsToScore.getEntry(i) > threshold 
					&& x!=fsm1.getInitialStateIndex() 
					&& y!=fsm2.getInitialStateIndex()
					) {
				scorePairs.add(new ScorePair<Integer>(pairsToScore.getEntry(i), x, y));
			}
			
		}
		Collections.sort(scorePairs);
		
		// only pairs where the best match is
		// at least r times as good as any other match
		while(scorePairs.size()>1 && scorePairs.get(scorePairs.size()-1).getScore()*ration < scorePairs.get(0).getScore()) {
			scorePairs.remove(scorePairs.size()-1);
		}
		//System.out.println(scorePairs);
		
		// remove conflicting pairs by order
		removeConflictsByOrder(scorePairs);
		//System.out.println(scorePairs);
		
		// then add remainder in scorePairs to outPairs
		for (ScorePair<Integer> scorePair : scorePairs) {
			kPairs = new ArrayList<>();
			kPairs.add(scorePair.getStatei());
			kPairs.add(scorePair.getStatej());
			outPairs.add(kPairs);
		}
				
		return outPairs;
	}

	public Set<List<Integer>> surr(Set<List<Integer>> kPairs, IConfigurableFSM<String, Word<String>> fsm0, IConfigurableFSM<String, Word<String>> fsm1) {
		Set<List<Integer>> outPairs = new LinkedHashSet<>();
		Set<List<Integer>> outPairsIn = new LinkedHashSet<>();
		Set<List<Integer>> outPairsOut = new LinkedHashSet<>();
		
		for (List<Integer> pair : kPairs) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> trs0 = fsm0.getSimplifiedTransitions(pair.get(0));
			Map<String, List<SimplifiedTransition<String, Word<String>>>> trs1 = fsm1.getSimplifiedTransitions(pair.get(1));
			Set<String> commonInputs = new LinkedHashSet<>(trs0.keySet());
			commonInputs.retainAll(trs1.keySet());
			for (String input : commonInputs) {
				for (SimplifiedTransition<String, Word<String>> s0 : trs0.get(input)) {
					for (SimplifiedTransition<String, Word<String>> s1 : trs1.get(input)) {
						if(!s0.getOut().equals(s1.getOut())) continue;
						List<Integer> nPairs = new ArrayList<>();
						nPairs.add(s0.getSj());
						nPairs.add(s1.getSj());
						outPairsOut.add(nPairs);
					}	
				}
			}
		}
		for (List<Integer> pair : kPairs) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> trs0 = fsm0.getSimplifiedTransitionsIn(pair.get(0));
			Map<String, List<SimplifiedTransition<String, Word<String>>>> trs1 = fsm1.getSimplifiedTransitionsIn(pair.get(1));
			Set<String> commonInputs = new LinkedHashSet<>(trs0.keySet());
			commonInputs.retainAll(trs1.keySet());
			for (String input : commonInputs) {
				for (SimplifiedTransition<String, Word<String>> s0 : trs0.get(input)) {
					for (SimplifiedTransition<String, Word<String>> s1 : trs1.get(input)) {
						if(!s0.getOut().equals(s1.getOut())) continue;
						List<Integer> nPairs = new ArrayList<>();
						nPairs.add(s0.getSi());
						nPairs.add(s1.getSi());
						outPairsIn.add(nPairs);
					}	
				}
			}
		}

		outPairs.addAll(outPairsIn);
		outPairs.addAll(outPairsOut);
		outPairs.removeAll(kPairs);
		return outPairs;
	}

	public RealVector computeScores(IConfigurableFSM<String, Word<String>> fsm1, IConfigurableFSM<String, Word<String>> fsm2, double K) {
		
		List<Integer> lst_nfa1 = new ArrayList<>(fsm1.getStateIDs());
		List<Integer> lst_nfa2 = new ArrayList<>(fsm2.getStateIDs());
		
		
		//////////////////////////////////////////////////////
		// Solving linear equation for outgoing transitions //
		//////////////////////////////////////////////////////
		RealMatrix coeffOut = new Array2DRowRealMatrix(lst_nfa1.size()*lst_nfa2.size(),lst_nfa1.size()*lst_nfa2.size());
		RealVector constOut = new ArrayRealVector(lst_nfa1.size()*lst_nfa2.size());
		for (int i1 = 0; i1 < lst_nfa1.size(); i1++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> trsIn1 = fsm1.getSimplifiedTransitions(lst_nfa1.get(i1));
			Map<String,List<SimplifiedTransition<String, Word<String>>>> sigma_i1 = new LinkedHashMap<>();
			trsIn1.values().forEach(a_lst -> a_lst.forEach(sympTr -> sigma_i1.putIfAbsent(sympTr.getIn()+"\t/\t"+sympTr.getOut(),new ArrayList<>())));
			trsIn1.values().forEach(a_lst -> a_lst.forEach(sympTr -> sigma_i1.get(sympTr.getIn()+"\t/\t"+sympTr.getOut()).add(sympTr)));
			
			for (int i2 = 0; i2 < lst_nfa2.size(); i2++) {
				int rowIdx = lst_nfa1.get(i1)*lst_nfa2.size()+lst_nfa2.get(i2);
				
				Map<String,List<SimplifiedTransition<String, Word<String>>>> sigma_i2 = new LinkedHashMap<>();
				Map<String, List<SimplifiedTransition<String, Word<String>>>> trsIn2 = fsm2.getSimplifiedTransitions(lst_nfa2.get(i2));
				trsIn2.values().forEach(a_lst -> a_lst.forEach(sympTr -> sigma_i2.putIfAbsent(sympTr.getIn()+"\t/\t"+sympTr.getOut(),new ArrayList<>())));
				trsIn2.values().forEach(a_lst -> a_lst.forEach(sympTr -> sigma_i2.get(sympTr.getIn()+"\t/\t"+sympTr.getOut()).add(sympTr)));
				
				Set<String> sigma_i1_min_i2 = new LinkedHashSet<>(sigma_i1.keySet()); sigma_i1_min_i2.removeAll(sigma_i2.keySet());
				Set<String> sigma_i2_min_i1 = new LinkedHashSet<>(sigma_i2.keySet()); sigma_i2_min_i1.removeAll(sigma_i1.keySet());
				Set<String> sigma_intersec = new LinkedHashSet<>(sigma_i1.keySet()); sigma_intersec.retainAll(sigma_i2.keySet());
				
				int succ_i1_i2 = 0;
				for (String inputSymbol : sigma_intersec) {
					for (SimplifiedTransition<String, Word<String>> dState1 : sigma_i1.get(inputSymbol)) {
						for (SimplifiedTransition<String, Word<String>> dState2 : sigma_i2.get(inputSymbol)) {
							int colIdx = lst_nfa1.indexOf(dState1.getSj())*lst_nfa2.size()+lst_nfa2.indexOf(dState2.getSj());
							coeffOut.setEntry(rowIdx, colIdx, coeffOut.getEntry(rowIdx, colIdx)-K);
							constOut.setEntry(rowIdx, constOut.getEntry(rowIdx)+1);
						}
					}
					
					succ_i1_i2+=sigma_i1.get(inputSymbol).size()*sigma_i2.get(inputSymbol).size();
				}
				
				double sG = 2*(sigma_i1_min_i2.size()+sigma_i2_min_i1.size()+succ_i1_i2);
				sG = (sG==0)?1:sG;
				int colIdx = lst_nfa1.get(i1)*lst_nfa2.size()+lst_nfa2.get(i2);
				coeffOut.setEntry(rowIdx, colIdx, coeffOut.getEntry(rowIdx, colIdx)+sG);
				
			}
		}
		
		DecompositionSolver solverOut = new LUDecomposition(coeffOut).getSolver();
		RealVector solutionOut = solverOut.solve(constOut);

		//////////////////////////////////////////////////////
		// Solving linear equation for incoming transitions //
		//////////////////////////////////////////////////////
		RealMatrix coeffIn = new Array2DRowRealMatrix(lst_nfa1.size()*lst_nfa2.size(),lst_nfa1.size()*lst_nfa2.size());
		RealVector constIn = new ArrayRealVector(lst_nfa1.size()*lst_nfa2.size());
		for (int i1 = 0; i1 < lst_nfa1.size(); i1++) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> trsIn1 = fsm1.getSimplifiedTransitionsIn(lst_nfa1.get(i1));
			Map<String,List<SimplifiedTransition<String, Word<String>>>> sigma_i1 = new LinkedHashMap<>();
			trsIn1.values().forEach(a_lst -> a_lst.forEach(sympTr -> sigma_i1.putIfAbsent(sympTr.getIn()+"\t/\t"+sympTr.getOut(),new ArrayList<>())));
			trsIn1.values().forEach(a_lst -> a_lst.forEach(sympTr -> sigma_i1.get(sympTr.getIn()+"\t/\t"+sympTr.getOut()).add(sympTr)));
			for (int i2 = 0; i2 < lst_nfa2.size(); i2++) {
				int rowIdx = lst_nfa1.get(i1)*lst_nfa2.size()+lst_nfa2.get(i2);
				
				Map<String,List<SimplifiedTransition<String, Word<String>>>> sigma_i2 = new LinkedHashMap<>();
				Map<String, List<SimplifiedTransition<String, Word<String>>>> trsIn2 = fsm2.getSimplifiedTransitionsIn(lst_nfa2.get(i2));
				trsIn2.values().forEach(a_lst -> a_lst.forEach(sympTr -> sigma_i2.putIfAbsent(sympTr.getIn()+"\t/\t"+sympTr.getOut(),new ArrayList<>())));
				trsIn2.values().forEach(a_lst -> a_lst.forEach(sympTr -> sigma_i2.get(sympTr.getIn()+"\t/\t"+sympTr.getOut()).add(sympTr)));
				
				Set<String> sigma_i1_min_i2 = new LinkedHashSet<>(sigma_i1.keySet()); sigma_i1_min_i2.removeAll(sigma_i2.keySet());
				Set<String> sigma_i2_min_i1 = new LinkedHashSet<>(sigma_i2.keySet()); sigma_i2_min_i1.removeAll(sigma_i1.keySet());
				Set<String> sigma_intersec = new LinkedHashSet<>(sigma_i1.keySet()); sigma_intersec.retainAll(sigma_i2.keySet());
				
				int succ_i1_i2 = 0;
				for (String inputSymbol : sigma_intersec) {
					for (SimplifiedTransition<String, Word<String>> dState1 : sigma_i1.get(inputSymbol)) {
						for (SimplifiedTransition<String, Word<String>> dState2 : sigma_i2.get(inputSymbol)) {
							int colIdx = lst_nfa1.indexOf(dState1.getSj())*lst_nfa2.size()+lst_nfa2.indexOf(dState2.getSj());
							coeffIn.setEntry(rowIdx, colIdx, coeffIn.getEntry(rowIdx, colIdx)-K);
							constIn.setEntry(rowIdx, constIn.getEntry(rowIdx)+1);
						}
					}
					
					succ_i1_i2+=sigma_i1.get(inputSymbol).size()*sigma_i2.get(inputSymbol).size();
				}
				
				double sG = 2*(sigma_i1_min_i2.size()+sigma_i2_min_i1.size()+succ_i1_i2);
				sG = (sG==0)?1:sG;
				int colIdx = lst_nfa1.get(i1)*lst_nfa2.size()+lst_nfa2.get(i2);
				coeffIn.setEntry(rowIdx, colIdx, coeffIn.getEntry(rowIdx, colIdx)+sG);
				
			}
		}
		
		DecompositionSolver solverIn = new LUDecomposition(coeffIn).getSolver();
		RealVector solutionIn = solverIn.solve(constIn);
		
		RealVector solutionJoined = solutionOut.copy().add(solutionIn);
		solutionJoined.mapDivideToSelf(2);
		return solutionJoined;
	}
	
	public List<Integer> pickHighest(Set<List<Integer>> nPairs, RealVector solution, IConfigurableFSM<String, Word<String>> fsm1, IConfigurableFSM<String, Word<String>> fsm2) {
		List<Integer> lst_B = new ArrayList<>(fsm2.getStateIDs());
		
		List<Integer> max = new ArrayList<>();
		max.add(null); max.add(null);
		double maxSim = Double.MIN_VALUE;
		for (List<Integer> pair : nPairs) {
			Integer A = pair.get(0);
			Integer B = pair.get(1);
			int coordIdx = (A) * lst_B.size() + (B);
			// higher priority to fix point pairs, i.e., (x,x)
			if(maxSim == solution.getEntry(coordIdx) && A == B) {
				max.set(0, A);
				max.set(1, B);
			}else if(maxSim<solution.getEntry(coordIdx)) {
				max.set(0, A);
				max.set(1, B);
				maxSim = solution.getEntry(coordIdx);
			}
			
		}
		return max;
		
	}
	
	public <I> FeaturedMealy<String, Word<String>> makeFFSM(ProductMealy<String, Word<String>> fsm1, ProductMealy<String, Word<String>> fsm2, Set<List<Integer>> kPairs, IFeatureModel fm) {
		Map<Integer,Integer> kPairs_nfa0to1 = new HashMap<>();
		
		for (List<Integer> pair : kPairs) {
			kPairs_nfa0to1.put(pair.get(0), pair.get(1));
		}
		
		Map<Integer,ConditionalState<ConditionalTransition<String, Word<String>>>> nfa0ToFFSM = new HashMap<>();
		Map<Integer,ConditionalState<ConditionalTransition<String, Word<String>>>> nfa1ToFFSM = new HashMap<>();
		
		Alphabet<String> alphabet = Alphabets.fromCollection(createAlphabet(fsm1,fsm2));
		FeaturedMealy<String, Word<String>> ffsm = new FeaturedMealy<>(alphabet,fm);
		ConditionalState<ConditionalTransition<String, Word<String>>> s0 = ffsm.addInitialState();

		nfa0ToFFSM.put(fsm1.getInitialState(), s0);
		nfa1ToFFSM.put(fsm2.getInitialState(), s0);
		
		Set<Node> feat0 = new LinkedHashSet<>(fsm1.getConfiguration());
		Set<Node> feat1 = new LinkedHashSet<>(fsm2.getConfiguration());
		
		feat0.removeAll(fsm2.getConfiguration());
		feat1.removeAll(fsm1.getConfiguration());
		
		Set<Node> feat_common = new LinkedHashSet<>(fsm1.getConfiguration());
		feat_common.retainAll(fsm2.getConfiguration());
		
		s0.setCondition(new Or( 
				removeCoreFeatures(fm, new And(fsm1.getConfiguration())),
				removeCoreFeatures(fm, new And(fsm2.getConfiguration()))
				));
		//s0.setCondition(removeCoreFeatures(fm, FeaturedMealyUtils.getInstance().makeConditionAsOr(s0.getCondition())));
		
		
		for (Integer si : fsm1.getStates()) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions = fsm1.getSimplifiedTransitions(si);
			if(!nfa0ToFFSM.containsKey(si)){
				ConditionalState<ConditionalTransition<String, Word<String>>> newState = ffsm.addState();
				newState.setCondition(new Or(removeCoreFeatures(fm, new And(fsm1.getConfiguration()))));
				nfa0ToFFSM.put(si, newState);
				if(kPairs_nfa0to1.containsKey(si)) {
					nfa1ToFFSM.put(kPairs_nfa0to1.get(si), newState);
				}
			}
			ConditionalState<ConditionalTransition<String, Word<String>>> ffsm_si = nfa0ToFFSM.get(si);
			for (String input : transitions.keySet()) {
				for (SimplifiedTransition<String, Word<String>> simpleTr : transitions.get(input)) {
					Integer sj = simpleTr.getSj();
					if(!nfa0ToFFSM.containsKey(sj)){
						ConditionalState<ConditionalTransition<String, Word<String>>> newState = ffsm.addState();
						newState.setCondition(new Or(removeCoreFeatures(fm, new And(fsm1.getConfiguration()))));
						nfa0ToFFSM.put(sj, newState);
						if(kPairs_nfa0to1.containsKey(sj)) {
							nfa1ToFFSM.put(kPairs_nfa0to1.get(sj), newState);
						}
					}
					String ffsm_in  = simpleTr.getIn();
					Word<String> ffsm_out = simpleTr.getOut();
					ConditionalState<ConditionalTransition<String, Word<String>>> ffsm_sj = nfa0ToFFSM.get(sj);
					
					Node cond = new Or(removeCoreFeatures(fm, new And(fsm1.getConfiguration())));
					ConditionalTransition<String, Word<String>> newTr = ffsm.addTransition(ffsm_si, ffsm_in, ffsm_sj, ffsm_out, cond);
					//ffsm.addTransition(ffsm_si, ffsm_in, ffsm_sj, ffsm_out, new And("A"));
					
				}
			}
		}
		
		for (Integer si : fsm2.getStates()) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions = fsm2.getSimplifiedTransitions(si);
			if(!nfa1ToFFSM.containsKey(si)){
				ConditionalState<ConditionalTransition<String, Word<String>>> newState = ffsm.addState();
				newState.setCondition(new Or(removeCoreFeatures(fm, new And(fsm2.getConfiguration()))));
				nfa1ToFFSM.put(si, newState);
			}
			ConditionalState<ConditionalTransition<String, Word<String>>> ffsm_si = nfa1ToFFSM.get(si);
			
			Node a_cons = removeCoreFeatures(fm, new And(fsm2.getConfiguration()));
			Set<Node> the_set = new LinkedHashSet<>();
			for (Node node : ffsm_si.getCondition().getChildren())  the_set.add(node);
			if(!the_set.contains(a_cons)) {
				the_set.add(a_cons);
				ffsm_si.setCondition(new Or(the_set));
			}
			
			
			for (String input : transitions.keySet()) {
				for (SimplifiedTransition<String, Word<String>> simpleTr : transitions.get(input)) {
					Integer sj = simpleTr.getSj();
					if(!nfa1ToFFSM.containsKey(sj)){
						ConditionalState<ConditionalTransition<String, Word<String>>> newState = ffsm.addState();
						newState.setCondition(removeCoreFeatures(fm, new Or(new And(fsm2.getConfiguration()))));
						nfa1ToFFSM.put(sj, newState);
					}
					String ffsm_in  = simpleTr.getIn();
					Word<String> ffsm_out = simpleTr.getOut();
					ConditionalState<ConditionalTransition<String, Word<String>>> ffsm_sj = nfa1ToFFSM.get(sj);
					
					a_cons = removeCoreFeatures(fm, new And(fsm2.getConfiguration()));
					the_set = new LinkedHashSet<>();
					for (Node node : ffsm_sj.getCondition().getChildren()) the_set.add(node);
					if(!the_set.contains(a_cons)) {
						the_set.add(a_cons);
						ffsm_sj.setCondition(new Or(the_set));
					}
					
					Map<String, List<SimplifiedTransition<String, Word<String>>>> trs_matching = ffsm.getSimplifiedTransitions(ffsm_si.getId(), ffsm_in, ffsm_out, ffsm_sj.getId());
					ConditionalTransition<String, Word<String>> a_tr = null;
					if(trs_matching.isEmpty()) {
						Node cond = new Or(removeCoreFeatures(fm, new And(fsm2.getConfiguration())));
						a_tr = ffsm.addTransition(ffsm_si, ffsm_in, ffsm_sj, ffsm_out, cond);
					}else {
						SimplifiedTransition<String, Word<String>> tr = new ArrayList<>(trs_matching.values()).get(0).get(0);
						a_tr = (ConditionalTransition<String, Word<String>>) tr.getTransition();
						a_cons = removeCoreFeatures(fm, new And(fsm2.getConfiguration()));
						the_set = new LinkedHashSet<>();
						for (Node node : a_tr.getCondition().getChildren()) the_set.add(node);
						if(!the_set.contains(a_cons)) {
							the_set.add(a_cons);
							a_tr.setCondition(new Or(the_set));
						}
						
					}
					
				}
			}
		}
		
		return ffsm;
	}
	


	public <I> FeaturedMealy<String, Word<String>> makeFFSM(FeaturedMealy<String, Word<String>> pffsm, ProductMealy<String, Word<String>> fsm, Set<List<Integer>> kPairs, IFeatureModel fm) {
		Map<Integer,Integer> kPairs_nfa0to1 = new HashMap<>();
		
		for (List<Integer> pair : kPairs) {
			kPairs_nfa0to1.put(pair.get(0), pair.get(1));
		}
		
		Map<Integer,ConditionalState<ConditionalTransition<String, Word<String>>>> nfa0ToFFSM = new HashMap<>();
		Map<Integer,ConditionalState<ConditionalTransition<String, Word<String>>>> nfa1ToFFSM = new HashMap<>();
		
		Set<String> abcSet = new LinkedHashSet<>();
		
		abcSet.addAll(pffsm.getInputAlphabet());
		abcSet.addAll(fsm.getInputAlphabet());
		
		List<String> abc = new ArrayList<>(abcSet);
		Collections.sort(abc);
		
		Alphabet<String> alphabet = Alphabets.fromCollection(abc);
		FeaturedMealy<String, Word<String>> ffsm = new FeaturedMealy<>(alphabet,fm);
		ConditionalState<ConditionalTransition<String, Word<String>>> s0 = ffsm.addInitialState();

		nfa0ToFFSM.put(pffsm.getInitialStateIndex(), s0);
		nfa1ToFFSM.put(fsm.getInitialState(), s0);

		Node newConfig = removeCoreFeatures(fm, new And(fsm.getConfiguration()));
		
		Set<Node> ands = new LinkedHashSet<>(FeaturedMealyUtils.getInstance().getAllAnds(pffsm.getInitialState().getCondition()));
		
		if(!ands.contains(newConfig)) {
			ands.add(newConfig);
		}
		s0.setCondition(removeCoreFeatures(fm, (Or) new Or(ands)));
		
		for (ConditionalState<ConditionalTransition<String, Word<String>>> si : pffsm.getStates()) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions = pffsm.getSimplifiedTransitions(si.getId());
			if(!nfa0ToFFSM.containsKey(si.getId())){
				ConditionalState<ConditionalTransition<String, Word<String>>> newState = ffsm.addState();
				newState.setCondition(removeCoreFeatures(fm, new Or(FeaturedMealyUtils.getInstance().getAllAnds(si.getCondition()))));
				nfa0ToFFSM.put(si.getId(), newState);
				if(kPairs_nfa0to1.containsKey(si.getId())) {
					nfa1ToFFSM.put(kPairs_nfa0to1.get(si.getId()), newState);
				}
			}
			ConditionalState<ConditionalTransition<String, Word<String>>> ffsm_si = nfa0ToFFSM.get(si.getId());
			for (String input : transitions.keySet()) {
				for (SimplifiedTransition<String, Word<String>> simpleTr : transitions.get(input)) {
					Integer sj = simpleTr.getSj();
					if(!nfa0ToFFSM.containsKey(sj)){
						ConditionalState<ConditionalTransition<String, Word<String>>> newState = ffsm.addState();
						newState.setCondition(removeCoreFeatures(fm, new Or(FeaturedMealyUtils.getInstance().getAllAnds(pffsm.getState(sj).getCondition()))));
						nfa0ToFFSM.put(sj, newState);
						if(kPairs_nfa0to1.containsKey(sj)) {
							nfa1ToFFSM.put(kPairs_nfa0to1.get(sj), newState);
						}
					}
					String ffsm_in  = simpleTr.getIn();
					Word<String> ffsm_out = simpleTr.getOut();
					ConditionalState<ConditionalTransition<String, Word<String>>> ffsm_sj = nfa0ToFFSM.get(sj);

					ConditionalTransition<String, Word<String>> a_tr = (ConditionalTransition<String, Word<String>>)simpleTr.getTransition();
					Node cond = removeCoreFeatures(fm, new Or(FeaturedMealyUtils.getInstance().getAllAnds(a_tr.getCondition())));
					ConditionalTransition<String, Word<String>> newTr = ffsm.addTransition(ffsm_si, ffsm_in, ffsm_sj, ffsm_out, cond);
					//ffsm.addTransition(ffsm_si, ffsm_in, ffsm_sj, ffsm_out, new And("A"));
					
				}
			}
		}
		
		for (Integer si : fsm.getStates()) {
			Map<String, List<SimplifiedTransition<String, Word<String>>>> transitions = fsm.getSimplifiedTransitions(si);
			if(!nfa1ToFFSM.containsKey(si)){
				ConditionalState<ConditionalTransition<String, Word<String>>> newState = ffsm.addState();
				newState.setCondition(new Or(removeCoreFeatures(fm, new And(fsm.getConfiguration()))));
				nfa1ToFFSM.put(si, newState);
			}
			ConditionalState<ConditionalTransition<String, Word<String>>> ffsm_si = nfa1ToFFSM.get(si);
			
			Node a_cons = removeCoreFeatures(fm, new And(fsm.getConfiguration()));
			Set<Node> the_set = new LinkedHashSet<>(FeaturedMealyUtils.getInstance().getAllAnds(ffsm_si.getCondition()));
			if(!the_set.contains(a_cons)) {
				the_set.add(a_cons);
				ffsm_si.setCondition(new Or(the_set));
			}
			
			for (String input : transitions.keySet()) {
				for (SimplifiedTransition<String, Word<String>> simpleTr : transitions.get(input)) {
					Integer sj = simpleTr.getSj();
					if(!nfa1ToFFSM.containsKey(sj)){
						ConditionalState<ConditionalTransition<String, Word<String>>> newState = ffsm.addState();
						newState.setCondition(new Or(removeCoreFeatures(fm, new And(fsm.getConfiguration()))));
						nfa1ToFFSM.put(sj, newState);
					}
					String ffsm_in  = simpleTr.getIn();
					Word<String> ffsm_out = simpleTr.getOut();
					ConditionalState<ConditionalTransition<String, Word<String>>> ffsm_sj = nfa1ToFFSM.get(sj);
					
					a_cons = removeCoreFeatures(fm, new And(fsm.getConfiguration()));
					the_set = new LinkedHashSet<>(FeaturedMealyUtils.getInstance().getAllAnds(ffsm_sj.getCondition()));
					if(!the_set.contains(a_cons)) {
						the_set.add(a_cons);
						ffsm_sj.setCondition(new Or(the_set));
					}
					
					Map<String, List<SimplifiedTransition<String, Word<String>>>> trs_matching = ffsm.getSimplifiedTransitions(ffsm_si.getId(), ffsm_in, ffsm_out, ffsm_sj.getId());
					ConditionalTransition<String, Word<String>> a_tr = null;
					if(trs_matching.isEmpty()) {
						Node cond = new Or(removeCoreFeatures(fm, new And(fsm.getConfiguration())));
						a_tr = ffsm.addTransition(ffsm_si, ffsm_in, ffsm_sj, ffsm_out, cond);
					}else {
						SimplifiedTransition<String, Word<String>> tr = new ArrayList<>(trs_matching.values()).get(0).get(0);
						a_tr = (ConditionalTransition<String, Word<String>>) tr.getTransition();
						a_cons = removeCoreFeatures(fm, new And(fsm.getConfiguration()));
						the_set = new LinkedHashSet<>(FeaturedMealyUtils.getInstance().getAllAnds(a_tr.getCondition()));
						if(!the_set.contains(a_cons)) {
							the_set.add(a_cons);
							a_tr.setCondition(new Or(the_set));
						}
						
					}
					
				}
			}
		}
		
		return ffsm;
	}

	public Node removeCoreFeatures(IFeatureModel fm, Or condition) {
		List<IFeature> coreFeatures = new ArrayList<>(fm.getAnalyser().getCoreFeatures(300000));
		for (IFeature iFeat: coreFeatures) {
			if(iFeat.getName().equals("TRUE")) {
				coreFeatures.remove(iFeat);
				break;
			}
			
		}
		Set<String> featNames = new LinkedHashSet<>();
		coreFeatures.forEach(aFeat -> featNames.add(aFeat.getName()));
		
		Set<Node> or_set = new LinkedHashSet<>();
		for (Node and_child : condition.getChildren()) {
			if(and_child instanceof And) {
				Set<Node> and_set = removeFeaturesByName(featNames,(And)and_child); 
				or_set.add(new And(and_set));
			}
		}
		return new Or(or_set);
	}
	
	public  Node removeCoreFeatures(IFeatureModel fm, And condition) {
		List<IFeature> coreFeatures = new ArrayList<>(fm.getAnalyser().getCoreFeatures(300000));
		for (IFeature iFeat: coreFeatures) {
			if(iFeat.getName().equals("TRUE")) {
				coreFeatures.remove(iFeat);
				break;
			}	
		}
		Set<String> featNames = new LinkedHashSet<>();
		coreFeatures.forEach(aFeat -> featNames.add(aFeat.getName()));
		Set<Node> and_set = removeFeaturesByName(featNames,condition);
		return new And(and_set);
	}
	
	public Set<Node> removeFeaturesByName(Set<String> featNames, And and_child) {
		Set<Node> and_set =  new LinkedHashSet<>();
		for (Node node : and_child.getChildren()) {
			Node aLiteral;
			if (node instanceof Not) {
				aLiteral = node.getChildren()[0];
				if(!featNames.contains(aLiteral.toString())) {
					and_set.add(new Not(aLiteral));
				}
			}else{
				aLiteral  = node;
				if(!featNames.contains(aLiteral.toString())) {	
					and_set.add(aLiteral);
				}
			} 
		}
		return and_set;
	}
	
	public void updateCondition(ConditionalTransition<String, String> tr,
			ProductMealy<String, Word<String>> fsmRef, ProductMealy<String, Word<String>> fsmUpdt) {
		Set<Node> featRefOnly = new LinkedHashSet<>(fsmRef.getConfiguration());
		featRefOnly.removeAll(fsmUpdt.getConfiguration());
		
		Set<Node> featUpdtOnly = new LinkedHashSet<>(fsmUpdt.getConfiguration());
		featUpdtOnly.removeAll(fsmRef.getConfiguration());
		
		if(tr.getCondition()==null) {
			tr.setCondition(new And(featRefOnly));
		}else {
			tr.setCondition(new Or(tr.getCondition(),new And(featRefOnly)));
		}
	}

	public void updateCondition(ConditionalState<ConditionalTransition<String, String>> newState,
			ProductMealy<String, Word<String>> fsmRef, ProductMealy<String, Word<String>> fsmUpdt) {
		Set<Node> featRefOnly = new LinkedHashSet<>(fsmRef.getConfiguration());
		featRefOnly.removeAll(fsmUpdt.getConfiguration());
		
		Set<Node> featUpdtOnly = new LinkedHashSet<>(fsmUpdt.getConfiguration());
		featUpdtOnly.removeAll(fsmRef.getConfiguration());
		
		if(newState.getCondition()==null) {
			newState.setCondition(new And(featRefOnly));
		}else {
			newState.setCondition(new Or(newState.getCondition(),new And(featRefOnly)));
		}
	}
	
	public Collection<String> createAlphabet(
			ProductMealy<String, Word<String>> fsm1, 
			ProductMealy<String, Word<String>> fsm2) {
		Set<String> abcSet = new LinkedHashSet<>();
		
		abcSet.addAll(fsm1.getInputAlphabet());
		abcSet.addAll(fsm2.getInputAlphabet());
		
		List<String> abc = new ArrayList<>(abcSet);
		Collections.sort(abc);
		return abc;
	}
	
	public float calcPerformance(Set<SimplifiedTransition<String, Word<String>>> deltaRef,
			Set<SimplifiedTransition<String, Word<String>>> removTr,
			Set<SimplifiedTransition<String, Word<String>>> xTr) {
		
		Set<SimplifiedTransition<String, Word<String>>> tp = new LinkedHashSet<>(deltaRef);
		tp.removeAll(removTr);
		
		Set<SimplifiedTransition<String, Word<String>>> tp_u_x = new LinkedHashSet<>(tp);
		tp_u_x.addAll(xTr);
		
		float performance = ((float) tp.size())/tp_u_x.size(); 
		return performance;
	}

	public Set<SimplifiedTransition<String, Word<String>>> mkTransitionsSet(
			IConfigurableFSM<String, Word<String>> ref) {
		Set<SimplifiedTransition<String, Word<String>>> deltaRef = new LinkedHashSet<>();
		for (Integer si : ref.getStateIDs()) {
			for (List<SimplifiedTransition<String, Word<String>>> trs : ref.getSimplifiedTransitions(si).values()) {
				deltaRef.addAll(trs);	
			}
		}
		return deltaRef;
	}

	public List<SimplifiedTransition<String, Word<String>>> getAddedTransitions(
			IConfigurableFSM<String, Word<String>> ref,
			IConfigurableFSM<String, Word<String>> updt, 
			Set<List<Integer>> kPairs
			) {
		List<SimplifiedTransition<String, Word<String>>> added_trs = new ArrayList<>();
		
		Map<Integer,Integer> map_ba = new HashMap<>();
		for (List<Integer> _pair : kPairs) {
			Integer _a = _pair.get(0);
			Integer _b = _pair.get(1);
			map_ba.put(_b, _a);
		}
		
		for (Integer b1 : updt.getStateIDs()) {
			for (String _in : updt.getInputAlphabet()) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> trs_matching_in = updt.getSimplifiedTransitions(b1, _in);
				for (List<SimplifiedTransition<String, Word<String>>> _tr : trs_matching_in.values()) {
					for (SimplifiedTransition<String, Word<String>> upd_tr : _tr) {

						// for each a1 - \alpha -> a2 \in \Delta_X, where \alpha = (_in, _out)
						Integer b2 = upd_tr.getSj();
						Word<String> _out = upd_tr.getOut();
						
						// (a1,b1) \not\in Kpairs
						Integer  a1 = map_ba.get(b1);
						if(a1 == null) { 
							added_trs.add(upd_tr);
							continue;
						}
						
						// (a2,b2) \not\in Kpairs
						Integer a2 = map_ba.get(b2);
						if(a2 == null) { 
							added_trs.add(upd_tr);
							continue;
						}
						
						// b1 - \alpha -> b2 \not\in \Delta_Y
						if(!ref.getInputAlphabet().contains(_in)) {
							added_trs.add(upd_tr);
							continue;
						}
						
						// b1 - \alpha -> b2 \not\in \Delta_Y
						Map<String, List<SimplifiedTransition<String, Word<String>>>> y_trs = new HashMap<>(ref.getSimplifiedTransitions(a1, _in, _out, a2));
						if(y_trs.isEmpty()) {
							added_trs.add(upd_tr);
							continue;
						} 
					}
				}
			}
		}
		return added_trs;
	}

	public List<SimplifiedTransition<String, Word<String>>> getRemovTransitions(
			IConfigurableFSM<String, Word<String>> ref, 
			IConfigurableFSM<String, Word<String>> updt, 
			Set<List<Integer>> kPairs
			) {
		List<SimplifiedTransition<String, Word<String>>> removed_trs = new ArrayList<>();
		
		Map<Integer,Integer> map_ab = new HashMap<>();
		for (List<Integer> _pair : kPairs) {
			Integer _a = _pair.get(0);
			Integer _b = _pair.get(1);
			map_ab.put(_a, _b);
		}
		
		for (Integer a1 : ref.getStateIDs()) {
			for (String _in : ref.getInputAlphabet()) {
				Map<String, List<SimplifiedTransition<String, Word<String>>>> trs_matching_in = ref.getSimplifiedTransitions(a1, _in);
				for (List<SimplifiedTransition<String, Word<String>>> _tr : trs_matching_in.values()) {
					for (SimplifiedTransition<String, Word<String>> ref_tr : _tr) {

						// for each a1 - \alpha -> a2 \in \Delta_X, where \alpha = (_in, _out)
						Integer a2 = ref_tr.getSj();
						Word<String> _out = ref_tr.getOut();
						
						// (a1,b1) \not\in Kpairs
						Integer  b1 = map_ab.get(a1);
						if(b1 == null) { 
							removed_trs.add(ref_tr);
							continue;
						}
						
						// (a2,b2) \not\in Kpairs
						Integer b2 = map_ab.get(a2);
						if(b2 == null) { 
							removed_trs.add(ref_tr);
							continue;
						}
						
						// b1 - \alpha -> b2 \not\in \Delta_Y
						if(!updt.getInputAlphabet().contains(_in)) {
							removed_trs.add(ref_tr);
							continue;
						}
						
						// b1 - \alpha -> b2 \not\in \Delta_Y
						Map<String, List<SimplifiedTransition<String, Word<String>>>> y_trs = new HashMap<>(updt.getSimplifiedTransitions(b1, _in, _out, b2));
						if(y_trs.isEmpty()) {
							removed_trs.add(ref_tr);
							continue;
						} 
					}
				}
			}
		}
		return removed_trs;
	}

	public Set<List<Integer>> ffsmDiff(
			IConfigurableFSM<String, Word<String>> ref, 
			IConfigurableFSM<String, Word<String>> updt,
			double K, 
			double T, 
			double R
			) {
		// See https://doi.org/10.1145/2430545.2430549 (Algorithm 1)

		// Line 1 @ Algorithm 1 
		RealVector pairsToScore = FfsmDiffUtils.getInstance().computeScores(ref,updt,K);
		System.out.print("States pair scores:\t");
		System.out.println(pairsToScore);

		// Line 2-5 @ Algorithm 1
		Set<List<Integer>> kPairs = FfsmDiffUtils.getInstance().identifyLandmaks(pairsToScore,ref,updt, T, R);
		System.out.print("Landmarks found:\t");
		kPairs.forEach(pair ->System.out.print("\t"+pair.get(0)+","+pair.get(1)));
		System.out.println();

		// Line 6 @ Algorithm 1
		Set<List<Integer>> nPairs = FfsmDiffUtils.getInstance().surr(kPairs, ref,updt);
		Map<Integer,Set<Integer>> checked = new HashMap<>();
		checked.put(0, new LinkedHashSet<>()); checked.put(1, new LinkedHashSet<>());
		for (List<Integer> list : kPairs) {
			checked.get(0).add(list.get(0));
			checked.get(1).add(list.get(1));
		}
		FfsmDiffUtils.getInstance().removeConflicts(nPairs,checked);

		// Line 7-14 @ Algorithm 1
		while (!nPairs.isEmpty()) {
			while (!nPairs.isEmpty()) {
				// Line 9 @ Algorithm 1
				List<Integer> A_B = FfsmDiffUtils.getInstance().pickHighest(nPairs,pairsToScore,ref,updt);
				System.out.print("Highest state pair found:");
				System.out.println("\t"+A_B.get(0)+","+A_B.get(1));

				// Line 10 @ Algorithm 1
				kPairs.add(A_B);
				checked.get(0).add(A_B.get(0));
				checked.get(1).add(A_B.get(1));

				// Line 11 @ Algorithm 1
				FfsmDiffUtils.getInstance().removeConflicts(nPairs,checked);
			}
			// Line 13 @ Algorithm 1
			nPairs = FfsmDiffUtils.getInstance().surr(kPairs,ref,updt);
			FfsmDiffUtils.getInstance().removeConflicts(nPairs,checked);
		}
		return kPairs;
	}
}
