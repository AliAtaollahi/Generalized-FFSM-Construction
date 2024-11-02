package uk.le.ac;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.FeaturedMealyUtils;
import uk.le.ac.ffsm.FfsmDiffUtils;
import uk.le.ac.ffsm.IConfigurableFSM;
import uk.le.ac.ffsm.ProductMealy;
import uk.le.ac.ffsm.SimplifiedTransition;

public class LearnFFSM {

	private static final String FM = "fm";
	private static final String UPDT = "updt";
	private static final String FREF = "fref";
	private static final String MREF = "mref";
	private static final String HELP = "h";
	private static final String K_VALUE = "k";
	private static final String T_VALUE = "t";
	private static final String R_VALUE = "r";
	private static final String OUT = "out";
	private static final String CLEAN = "clean";

	public static void main(String[] args) {
		try {
			
			// create the command line parser
			CommandLineParser parser = new BasicParser();
			// create the Options
			Options options = createOptions();
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			
			// parse the command line arguments
			CommandLine line = parser.parse( options, args);

			if(line.hasOption(HELP) || !line.hasOption(FM)){
				formatter.printHelp( "LearnFFSM", options );
				System.exit(0);
			}
			
			File f_fm = new File(line.getOptionValue(FM));
			
			IFeatureModel fm = FeatureModelManager.load(f_fm.toPath()).getObject();
			
			IConfigurableFSM<String, Word<String>> ref  = null;
			IConfigurableFSM<String, Word<String>> updt = null;
			FeaturedMealy<String, Word<String>> ffsm = null;
			File f_ref = null, f_upd = null;
			
			if(line.hasOption(CLEAN)) {
				f_ref  = new File(line.getOptionValue(CLEAN));
				ffsm  = FeaturedMealyUtils.getInstance().loadFeaturedMealy(f_ref,fm);
				FeaturedMealyUtils.getInstance().cleanFeaturedMealy(ffsm,fm);
				
				FeaturedMealyUtils.getInstance().saveFFSM_kiss(ffsm, new File(f_ref.getParent(),f_ref.getName()+".clean"),false);
				FeaturedMealyUtils.getInstance().saveFFSM(ffsm, new File(f_ref.getParent(),f_ref.getName()+".clean"+".dot"),false);
				System.exit(0);
			}
			
			if(line.hasOption(FREF)){
				f_ref = new File(line.getOptionValue(FREF));
				f_upd = new File(line.getOptionValue(UPDT));
				ref  = FeaturedMealyUtils.getInstance().loadFeaturedMealy (f_ref,fm);
				updt = FeaturedMealyUtils.getInstance().loadProductMachine(f_upd,fm);
			}

			if(line.hasOption(MREF)){
				f_ref  = new File(line.getOptionValue(MREF));
				f_upd = new File(line.getOptionValue(UPDT));
				ref  = FeaturedMealyUtils.getInstance().loadProductMachine(f_ref,fm);
				updt = FeaturedMealyUtils.getInstance().loadProductMachine(f_upd,fm);
			}
			
			double K = Double.valueOf(line.getOptionValue(K_VALUE,"0.50"));
			double T = Double.valueOf(line.getOptionValue(T_VALUE,"0.50"));
			double R = Double.valueOf(line.getOptionValue(R_VALUE,"1.40"));
			
			Set<List<Integer>> kPairs = FfsmDiffUtils.getInstance().ffsmDiff(ref,updt,K,T,R);
			
 			System.out.print("Common states found:");
			kPairs.forEach(pair ->System.out.print("\t"+pair.get(0)+","+pair.get(1)));
			System.out.println();
			
			Set<SimplifiedTransition<String, Word<String>>> addedTr = new HashSet<>(FfsmDiffUtils.getInstance().getAddedTransitions(ref,updt,kPairs));
			Set<SimplifiedTransition<String, Word<String>>> removTr = new HashSet<>(FfsmDiffUtils.getInstance().getRemovTransitions(ref,updt,kPairs));
			
			Set<SimplifiedTransition<String, Word<String>>> deltaRef = FfsmDiffUtils.getInstance().mkTransitionsSet(ref);
			
			float precision = FfsmDiffUtils.getInstance().calcPerformance(deltaRef,removTr,addedTr);
			float recall    = FfsmDiffUtils.getInstance().calcPerformance(deltaRef,removTr,removTr);
			float f_measure = (2*precision*recall)/(precision+recall);
			
			System.out.println(String.format("Precision|Recall|F-measure:%f|%f|%f", precision, recall, f_measure));
			
			if(line.hasOption(FREF)){
				ffsm = FfsmDiffUtils.getInstance().makeFFSM((FeaturedMealy<String, Word<String>>)ref,(ProductMealy<String, Word<String>>)updt,kPairs,fm);
			}else {
				ffsm = FfsmDiffUtils.getInstance().makeFFSM((ProductMealy<String, Word<String>>)ref,(ProductMealy<String, Word<String>>)updt,kPairs,fm);
			}
			
			FeaturedMealyUtils.getInstance().saveFFSM_kiss(ffsm, new File(line.getOptionValue(OUT)));
			FeaturedMealyUtils.getInstance().saveFFSM(ffsm, new File(line.getOptionValue(OUT)+".dot"));
			
			List<SimplifiedTransition<String, Word<String>>> refTrs = FeaturedMealyUtils.getInstance().getTransitions(ref);
			List<SimplifiedTransition<String, Word<String>>> updtTrs = FeaturedMealyUtils.getInstance().getTransitions(updt);
			List<SimplifiedTransition<String, Word<String>>> ffsmTrs = FeaturedMealyUtils.getInstance().getTransitions(ffsm);
			
			if(line.hasOption(MREF)){
				Set<String> refFeatures = new HashSet<>();
				Set<String> updtFeatures = new HashSet<>();
				
				for (Node conf : ref.getConfiguration()) {
					refFeatures.add(conf.toString());
				}
				
				for (Node conf : updt.getConfiguration()) {
					updtFeatures.add(conf.toString());
				}
				
				Set<String> commonFeatures = new HashSet<>(refFeatures);
				commonFeatures.retainAll(updtFeatures);
				
				Set<String> allFeatures = new HashSet<>();
				allFeatures.addAll(refFeatures);
				allFeatures.addAll(updtFeatures);
				
				System.out.println(
						"Reference/Updated"
						+ "/TotalStatesRef"+ "/TotalStatesUpdt"
						+ "/TotalTransitionsRef"+ "/TotalTransitionsUpdt"
						+ "/TotalFeaturesRef"+ "/TotalFeaturesUpdt"
						+ "/CommonFeatures"
						+ "/RatioFeatures"
						+ "/RatioStates"
						+ "/RatioTransitions"
						+ "/StatesFFSM"
						+ "/TransitionsFFSM"
						+ ":"
						+f_ref.getName()+"/"+f_upd.getName()
						+"/"+ref.getStateIDs().size()+"/"+updt.getStateIDs().size()
						+"/"+refTrs.size()+"/"+updtTrs.size()
						+"/"+refFeatures.size()+"/"+updtFeatures.size()
						+"/"+commonFeatures.size()
						+"/"+(((double)commonFeatures.size())    /(allFeatures.size()))
						+"/"+(((double)ffsm.getStateIDs().size())/(ref.getStateIDs().size()+updt.getStateIDs().size()))
						+"/"+((double)ffsmTrs.size())/(refTrs.size()+updtTrs.size())
						+"/"+ffsm.getStateIDs().size()
						+"/"+ffsmTrs.size()						
						);
			}
			
			if(line.hasOption(FREF)){
				System.out.println(
						"Reference/Updated"
						+ "/TotalStatesRef"+ "/TotalStatesUpdt"
						+ "/TotalTransitionsRef"+ "/TotalTransitionsUpdt"
						+ "/TotalFeaturesRef"+ "/TotalFeaturesUpdt"
						+ "/CommonFeatures"
						+ "/RatioFeatures"
						+ "/RatioStates"
						+ "/RatioTransitions"
						+ "/StatesFFSM"
						+ "/TransitionsFFSM"
						+ ":"
						+f_ref.getName()+"/"+f_upd.getName()
						+"/"+ref.getStateIDs().size()+"/"+updt.getStateIDs().size()
						+"/"+refTrs.size()+"/"+updtTrs.size()
						+"/-1"+"/-1"
						+"/-1"
						+"/-1"
						+"/"+(((double)ffsm.getStateIDs().size())/(ref.getStateIDs().size()+updt.getStateIDs().size()))
						+"/"+((double)ffsmTrs.size())/(refTrs.size()+updtTrs.size())
						+"/"+ffsm.getStateIDs().size()
						+"/"+ffsmTrs.size()
						);
				
			}

			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	private static Options createOptions() {
		Options options = new Options();
		options.addOption( CLEAN,  true, "Simplify FFSM labels" );
		options.addOption( MREF,  true, "Mealy reference" );
		options.addOption( FREF,  true, "FFSM reference" );
		options.addOption( UPDT,  true, "Mealy update" );
		options.addOption( FM,    true, "Feature model" );
		options.addOption( K_VALUE, true, "Attenuation (i.e., surrounding states)" );
		options.addOption( T_VALUE, true, "Threshold (i.e., only above)" );
		options.addOption( R_VALUE, true, "Ratio (i.e., r times better only)" );
		options.addOption( OUT,    true, "Output file" );
		options.addOption( HELP,  false, "Help menu" );
		return options;
	}
}
