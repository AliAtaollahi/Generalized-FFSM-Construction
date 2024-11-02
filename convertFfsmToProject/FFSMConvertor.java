package uk.le.ac.convertFfsmToProject;

import java.util.List;
import java.util.Map;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealy;

public class FFSMConvertor {
	public FFSMConvertor() {
		data_manager_factory_ = new DataManagerFactory();
		fsm_extractor_ = new FSMExractor();
		fsm_visualizer_ = new FSMVisualizer();
	}

	private DataManagerFactory data_manager_factory_;
	private FSMExractor fsm_extractor_;
	private FSMVisualizer fsm_visualizer_;

	public void start_loading_ffsm_and_convert_to_fsm(String[] args) {
		data_manager_factory_.set_arguments(args);

		IFeatureModel fm = data_manager_factory_.read_and_get_feature_model();
		FeaturedMealy<String, Word<String>> ffsm = data_manager_factory_.read_and_get_ffsm(fm);

		Map<String, List<String>> features_alphabet = data_manager_factory_.read_and_get_features_alphabet();
		Map<Object, Boolean> configs = data_manager_factory_.read_and_get_config(features_alphabet.keySet());
		boolean is_there_loop = data_manager_factory_.get_loop_existance_status();
		
		FastNFA<String> fsm = fsm_extractor_.exract_fsm_from_ffsm(ffsm, configs, is_there_loop);
		fsm_visualizer_.visualize(fsm, data_manager_factory_);
	}
}
