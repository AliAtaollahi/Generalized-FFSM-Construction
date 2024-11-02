package uk.le.ac.fts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import be.vibes.fexpression.configuration.SimpleConfiguration;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.SimpleProjection;
import be.vibes.ts.TransitionSystem;
import be.vibes.ts.io.dot.FeaturedTransitionSystemDotPrinter;
import be.vibes.ts.io.dot.TransitionSystemDotPrinter;
import be.vibes.ts.io.xml.XmlLoaders;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.util.automata.Automata;
import net.automatalib.util.automata.minimizer.hopcroft.HopcroftMinimization;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealyUtils;

public class FsmFromFTS {
	
//	private static final String FM = "fm";
	private static final String FTS = "fts";
	private static final String HELP = "h";
	private static final String CONF = "conf";
	
	
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
			
			if(line.hasOption(HELP)
					|| !line.hasOption(FTS)
					|| !line.hasOption(CONF)){
				formatter.printHelp( "fts2fsm", options );
				System.exit(0);
			}
			
			String s_fts  = 
					//"Benchmark_SPL/minepump/fts/minepump.fts" ;
					line.getOptionValue(FTS);
			String s_conf = 
					//"Benchmark_SPL/minepump/products/00001.config";
					line.getOptionValue(CONF);
			
			// load the fts
			File f_fts = new File(s_fts);
			FeaturedTransitionSystem fts = XmlLoaders.loadFeaturedTransitionSystem(f_fts);
			
			// generate projection using configuration
			SimpleConfiguration product = FtsUtils.getInstance().loadConfiguration(s_conf);
			TransitionSystem lts = SimpleProjection.getInstance().project(fts, product);
			CompactMealy<String, Word<String>> mealy = FtsUtils.getInstance().lts2fsm(lts);
			mealy = HopcroftMinimization.minimizeMealy(mealy);
			
			// save fts.dot
			new FeaturedTransitionSystemDotPrinter(fts,new PrintStream(new File(s_fts.replaceFirst(".fts$", "_fts.dot")))).printDot();
			
			// save lts.dot
			String s_lts  = s_conf.replaceFirst(".config$", "_lts.dot");
			new TransitionSystemDotPrinter(lts, new PrintStream(new File(s_lts))).printDot();
			
			// save fsm.dot
			String s_fsm  = s_conf.replaceFirst(".config$", "_fsm.dot");
			BufferedWriter bw = new BufferedWriter(new FileWriter(s_fsm)); 
			GraphDOT.write(mealy, bw); 
			bw.close();
			
			// save kiss.txt
			String s_kiss = s_conf.replaceFirst(".config$", "_kiss.txt");
			String header = FtsUtils.getInstance().simpleConfigurationToString(product);
			FeaturedMealyUtils.getInstance().saveFSM_kiss(mealy, new File(s_kiss), header);
			
			// check the FSM is initially connected
			List<Word<String>> q_set = Automata.stateCover(mealy, mealy.getInputAlphabet());
			if(q_set.size() != mealy.getStates().size()) {
				throw new Exception(
						String.format(
								"Unreachable states at %s", 
								s_conf
								)
						);
			}
			//System.out.println(String.format("The FTS %s has been projected using %s",s_fts, s_conf));
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption( FTS,    true, "Featured transition system" );
		options.addOption( CONF,    true, "Product-line configuration" );
		options.addOption( HELP,  false, "Help menu" );
		return options;
	}
}
