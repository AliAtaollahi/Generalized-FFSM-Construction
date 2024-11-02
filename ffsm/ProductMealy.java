package uk.le.ac.ffsm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.words.Alphabet;

public class ProductMealy<I,O> extends CompactMealy<I, O> implements IConfigurableFSM<I,O>{

	private static final long serialVersionUID = -5631683009867401872L;
	
	private IFeatureModel featureModel;
	private static final String TRUE_STRING = "TRUE";
	private List<Node> configuration;
	private Properties info;
	
	public ProductMealy(Alphabet<I> alphabet) {
		super(alphabet);
		this.configuration = new ArrayList<>();
		this.info = new Properties();
	}
	
	public ProductMealy(Alphabet<I> alphabet, IFeatureModel fm, Collection<Node> configuration) {
		this(alphabet);
		this.featureModel = fm;
		this.configuration.addAll(configuration);
		addTRUE_feature();
	}
	
	public Properties getInfo() {
		return info;
	}
	
	private void addTRUE_feature() {
		IFeature newChild = FMFactoryManager.getFactory(this.featureModel).createFeature(this.featureModel, TRUE_STRING);
		this.featureModel.addFeature(newChild);
		IFeature root = this.featureModel.getStructure().getRoot().getFeature();
		root.getStructure().addChild(newChild.getStructure());
		newChild.getStructure().setMandatory(true);
	}
	
	@Override
	public List<Node> getConfiguration() {
		return Collections.unmodifiableList(configuration);
	}
	
	public void setConfiguration(List<Node> configuration) {
		this.configuration = configuration;
	}

	@Override
	public Map<I,List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input, O output, Integer sj){
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		for (CompactMealyTransition<O> tr : super.getTransitions(si, input)) {
			if(tr.getOutput().equals(output) & sj.equals(tr.getSuccId())) {
				tr_match.putIfAbsent(input, new ArrayList<>());
				SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si, input, output, sj);
				simplyTr.setTransition(tr);
				tr_match.get(input).add(simplyTr);
			}
		}
		return tr_match;
	}
	
	@Override
	public Map<I,List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input, O output){
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		for (CompactMealyTransition<O> tr : super.getTransitions(si, input)) {
			if(tr.getOutput().equals(output)) {
				Integer sj = tr.getSuccId();
				tr_match.putIfAbsent(input, new ArrayList<>());
				SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si, input, output, sj);
				simplyTr.setTransition(tr);
				tr_match.get(input).add(simplyTr);
			}
		}
		return tr_match;
	}
	
	@Override
	public Map<I,List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si, I input){
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		for (CompactMealyTransition<O> tr : super.getTransitions(si, input)) {
			Integer sj = tr.getSuccId();
			O output = tr.getOutput();
			tr_match.putIfAbsent(input, new ArrayList<>());
			SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si, input, output, sj);
			simplyTr.setTransition(tr);
			tr_match.get(input).add(simplyTr);
		}
		return tr_match;
	}
	
	@Override
	public Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitions(Integer si) {
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		for (I input : getInputAlphabet()) {
			for (CompactMealyTransition<O> tr : getTransitions(si, input)) {
				tr_match.putIfAbsent(input, new ArrayList<>());
				SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si, input, tr.getOutput(), tr.getSuccId());
				simplyTr.setTransition(tr);
				tr_match.get(input).add(simplyTr);
			}
		}
		return tr_match;
	}
	
	@Override
	public Map<I, List<SimplifiedTransition<I, O>>> getSimplifiedTransitionsIn(Integer sj) {
		Map<I,List<SimplifiedTransition<I, O>>> tr_match = new LinkedHashMap<>() ;
		for (Integer si : getStates()) {
			for (I input : getInputAlphabet()) {
				for (CompactMealyTransition<O> tr : getTransitions(si, input)) {
					if(!sj.equals(tr.getSuccId())) continue;
					tr_match.putIfAbsent(input, new ArrayList<>());
					SimplifiedTransition<I, O> simplyTr = new SimplifiedTransition<I,O>(si, input, tr.getOutput(), tr.getSuccId());
					simplyTr.setTransition(tr);
					tr_match.get(input).add(simplyTr);
				}
			}
		}
		return tr_match;
	}
	
	@Override
	public Integer getInitialStateIndex() {
		return getStateId(getInitialState());
	}
	
	@Override
	public List<Integer> getStateIDs() {
		List<Integer> out = new ArrayList<>();
		for (Integer state : getStates()) {
			out.add(getStateId(state));
		}
		return out;
	}
}
