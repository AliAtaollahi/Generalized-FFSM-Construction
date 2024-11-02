package uk.le.ac.comparingFsmStructuralSimilarity;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FfsmDiffUtils;
import uk.le.ac.ffsm.IConfigurableFSM;
import uk.le.ac.ffsm.SimplifiedTransition;

public class FSMComparator {
	public FSMComparator() {
		data_manager_factory_ = new DataManagerFactory();
	}
	
	public static final Word<String> OMEGA_SYMBOL = Word.fromLetter("Ω");
	
	private DataManagerFactory data_manager_factory_;
	
	private IConfigurableFSM<String, Word<String>> nfsm_;
	private IConfigurableFSM<String, Word<String>> dfsm_;
	private File nfsm_prod_, dfsm_prod_;
	double K_;
	double T_;
	double R_;

	public void start_comparing(String[] args) {
		try {
			data_manager_factory_.set_arguments(args);
			
			IFeatureModel fm = data_manager_factory_.read_and_get_feature_model();

			boolean is_no_loop = data_manager_factory_.get_loop_condition();

			nfsm_prod_ = data_manager_factory_.read_and_get_nfsm_prod();
			nfsm_ = data_manager_factory_.loadFeaturedMealy(nfsm_prod_, fm, is_no_loop);
			
			dfsm_prod_ = data_manager_factory_.read_and_get_fsm_prod();
			dfsm_ = data_manager_factory_.loadProductMachine(dfsm_prod_, fm, is_no_loop);

			K_ = data_manager_factory_.get_k_value();
			T_ = data_manager_factory_.get_t_value();
			R_ = data_manager_factory_.get_r_value();

			compare_fsms();
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}

	private void compare_fsms() throws Exception {
		Set<List<Integer>> kPairs = FfsmDiffUtils.getInstance().ffsmDiff(nfsm_,dfsm_,K_,T_,R_);
		
		Set<SimplifiedTransition<String, Word<String>>> addedTr = new HashSet<>(FfsmDiffUtils.getInstance().getAddedTransitions(nfsm_,dfsm_,kPairs));
		Set<SimplifiedTransition<String, Word<String>>> removTr = new HashSet<>(FfsmDiffUtils.getInstance().getRemovTransitions(nfsm_,dfsm_,kPairs));
		
		Set<SimplifiedTransition<String, Word<String>>> deltaRef = FfsmDiffUtils.getInstance().mkTransitionsSet(nfsm_);
		
		float precision = FfsmDiffUtils.getInstance().calcPerformance(deltaRef,removTr,addedTr);
		float recall    = FfsmDiffUtils.getInstance().calcPerformance(deltaRef,removTr,removTr);
		float f_measure = (2*precision*recall)/(precision+recall);
		
		System.out.println(String.format("ModelRef|ModelUpdt|Precision|Recall|F-measure:%s|%s|%f|%f|%f", nfsm_prod_.getName(),dfsm_prod_.getName(),precision, recall, f_measure));
	}
}




		
//		for (Integer b1 : dfsm_.getStateIDs()) {
//			for (String _in : dfsm_.getInputAlphabet()) {
//				Map<String, List<SimplifiedTransition<String, Word<String>>>> trs_matching_in = dfsm_.getSimplifiedTransitions(b1, _in);
//				for (List<SimplifiedTransition<String, Word<String>>> _tr : trs_matching_in.values()) {
//					for (SimplifiedTransition<String, Word<String>> upd_tr : _tr) {
//						String si = upd_tr.getSi().toString();
//						String sj = upd_tr.getSj().toString();
//						String input = _in;
//						String output = upd_tr.getOut().toString();
//						System.out.print(String.format("%s -- %s / %s -> %s\n", si, input, output, sj));
//					}
//				}
//			}
//		}