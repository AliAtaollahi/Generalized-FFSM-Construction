package uk.le.ac.compare;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealyUtils;
import uk.le.ac.ffsm.FfsmDiffUtils;
import uk.le.ac.ffsm.IConfigurableFSM;
import uk.le.ac.ffsm.SimplifiedTransition;

public class CompareStructure {
	private static final String FM = "fm";
	private static final String HELP = "h";
	private static final String K_VALUE = "k";
	private static final String T_VALUE = "t";
	private static final String IS_FSM = "fsm";
	private static final String IS_BOTH = "both";
	private static final String R_VALUE = "r";
	
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

			if(line.hasOption(HELP) || !line.hasOption(FM) || line.getArgList().size()!=2){
				formatter.printHelp( "CompareStructure", options);
				System.exit(0);
			}
			
			// load the feature model
			File f_fm = new File(line.getOptionValue(FM));
			IFeatureModel fm = FeatureModelManager.load(f_fm.toPath()).getObject();
			
			
			
			File a_prod = new File(line.getArgList().get(0).toString());
			File b_prod = new File(line.getArgList().get(1).toString());
			
			IConfigurableFSM<String, Word<String>> a_fMealy = FeaturedMealyUtils.getInstance().loadProductMachine(a_prod,fm);
			IConfigurableFSM<String, Word<String>> b_fMealy = FeaturedMealyUtils.getInstance().loadProductMachine(b_prod,fm);
			
			if(line.hasOption(IS_FSM)) {
				a_fMealy = FeaturedMealyUtils.getInstance().loadProductMachine(a_prod,fm);
				b_fMealy = FeaturedMealyUtils.getInstance().loadProductMachine(b_prod,fm);
			}else if(line.hasOption(IS_BOTH)) {
				a_fMealy = FeaturedMealyUtils.getInstance().loadFeaturedMealy(a_prod,fm);
				b_fMealy = FeaturedMealyUtils.getInstance().loadProductMachine(b_prod,fm);
			}else {
				a_fMealy = FeaturedMealyUtils.getInstance().loadFeaturedMealy(a_prod,fm);
				b_fMealy = FeaturedMealyUtils.getInstance().loadFeaturedMealy(b_prod,fm);
			}
			
			double K = Double.valueOf(line.getOptionValue(K_VALUE,"0.50"));
			double T = Double.valueOf(line.getOptionValue(T_VALUE,"0.50"));
			double R = Double.valueOf(line.getOptionValue(R_VALUE,"1.40"));
			
			Set<List<Integer>> kPairs = FfsmDiffUtils.getInstance().ffsmDiff(a_fMealy,b_fMealy,K,T,R);
			
			Set<SimplifiedTransition<String, Word<String>>> addedTr = new HashSet<>(FfsmDiffUtils.getInstance().getAddedTransitions(a_fMealy,b_fMealy,kPairs));
			Set<SimplifiedTransition<String, Word<String>>> removTr = new HashSet<>(FfsmDiffUtils.getInstance().getRemovTransitions(a_fMealy,b_fMealy,kPairs));
			
			Set<SimplifiedTransition<String, Word<String>>> deltaRef = FfsmDiffUtils.getInstance().mkTransitionsSet(a_fMealy);
			
			float precision = FfsmDiffUtils.getInstance().calcPerformance(deltaRef,removTr,addedTr);
			float recall    = FfsmDiffUtils.getInstance().calcPerformance(deltaRef,removTr,removTr);
			float f_measure = (2*precision*recall)/(precision+recall);
			
			System.out.println(String.format("ModelRef|ModelUpdt|Precision|Recall|F-measure:%s|%s|%f|%f|%f", a_prod.getName(),b_prod.getName(),precision, recall, f_measure));
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
	}
		
	private static Options createOptions() {
		Options options = new Options();
		options.addOption( FM,    true, "Feature model" );
		options.addOption( K_VALUE, true, "Attenuation (i.e., surrounding states)" );
		options.addOption( T_VALUE, true, "Threshold (i.e., only above)" );
		options.addOption( IS_FSM, false, "Read FSM" );
		options.addOption( IS_BOTH, false, "Read FFSM and FSM" );
		options.addOption( R_VALUE, true, "Ratio (i.e., r times better only)" );
		options.addOption( HELP,  false, "Help menu" );
		return options;
	}	

}
