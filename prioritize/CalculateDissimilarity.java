package uk.le.ac.prioritize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.prop4j.Literal;
import org.prop4j.Node;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealyUtils;
import uk.le.ac.ffsm.ProductMealy;

public class CalculateDissimilarity {
	
	private static final String FM = "fm";
	private static final String HELP = "h";
	private static final String PROD_ORDER = "order";
	
	
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
				formatter.printHelp( "CalculateDissimilarity", options );
				System.exit(0);
			}
			
			// load the feature model
			File f_fm = new File(line.getOptionValue(FM));
			IFeatureModel fm = FeatureModelManager.load(f_fm.toPath()).getObject();
			List<String> allFeatures = new ArrayList<>();
			for (IFeature node : fm.getFeatures()) {
				if(node.getName().equals("TRUE")) continue;
				allFeatures.add(node.getName());
			}
			
			if(!line.hasOption(PROD_ORDER)) {
				File a_prod = new File(line.getArgList().get(0).toString());
				ProductMealy<String, Word<String>> aMealy = FeaturedMealyUtils.getInstance().loadProductMachine(a_prod,fm);
				List<String> a_conf = getFeatures(aMealy); 
				
				File b_prod = new File(line.getArgList().get(1).toString());
				ProductMealy<String, Word<String>> bMealy = FeaturedMealyUtils.getInstance().loadProductMachine(b_prod,fm);
				List<String> b_conf = getFeatures(bMealy);
				
				float sc = 1 - clacDistance(a_conf, b_conf, allFeatures);
				
				System.out.println(String.format("Pair dissimilarity:%s\t%s\t%s", a_prod.getName(),b_prod.getName(),Float.toString(sc)));
			}else {
				File prods_file = new File(line.getOptionValue(PROD_ORDER));
				BufferedReader br = new BufferedReader(new FileReader(prods_file));
				List<List<String>> prod_lst = new ArrayList<>();
				List<Double> cum_dissimilarity = new ArrayList<>();
				
				while(br.ready()) {
					String a_line = br.readLine(); 
					File a_prod = new File(a_line);
					ProductMealy<String, Word<String>> aMealy = FeaturedMealyUtils.getInstance().loadProductMachine(a_prod,fm);
					List<String> a_conf = getFeatures(aMealy);
					prod_lst.add(a_conf);
					cum_dissimilarity.add(0.0);
				}
				br.close();
				
				for (int ii = 1; ii < prod_lst.size(); ii++) {
					for (int i = 0; i < ii; i++) {
						//System.out.println(String.format("%d,%d", ii, i));
						double dist_ii_i = 1 - clacDistance(prod_lst.get(ii), prod_lst.get(i), allFeatures);
						cum_dissimilarity.set(ii,cum_dissimilarity.get(ii)+dist_ii_i);
					}
				}
				System.out.println(String.format("Cumulative dissimilarity (%s)", prods_file.getName()));
				cum_dissimilarity.forEach(sim -> System.out.println(sim));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
	}
	
	private static List<String> getFeatures(ProductMealy<String, Word<String>> aMealy) {
		List<String> conf_i = new ArrayList<>();
		for (Node n: aMealy.getConfiguration()) {
			if(n instanceof Literal) conf_i.add(n.toString());
		}
		return conf_i;
	}

	private static float clacDistance(List<String> x, List<String> y, List<String> allFeatures) {
		final Collection<String> similar = new HashSet<String>(x);
		final Collection<String> different = new HashSet<String>();

		different.addAll(x);
		different.addAll(y);
		similar.retainAll(y);

		different.removeAll(similar);

		final float s = similar.size();
		final float d = different.size();
		final float t = allFeatures.size();

		return (s + (t - (s + d))) / t;
	}
	
	private static Options createOptions() {
		Options options = new Options();
		options.addOption( FM,    true, "Feature model" );
		options.addOption( PROD_ORDER,    true, "Calculate similarity for sorted products from file (default: product pair)");
		options.addOption( HELP,  false, "Help menu" );
		return options;
	}	

}
