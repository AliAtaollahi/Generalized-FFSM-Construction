package uk.le.ac.prioritize;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

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
import uk.le.ac.ffsm.ScorePair;

public class PrtzProducts {
	
	private static final String FM = "fm";
	private static final String HELP = "h";
	private static final String SIMILARITY = "similar";
	private static final String SHUFFLE = "shuffle";
	private static final String GMDP = "gmdp";
	
	
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
				formatter.printHelp( "PrtzProducts", options );
				System.exit(0);
			}
			
			// load the feature model
			File f_fm = new File(line.getOptionValue(FM));
			IFeatureModel fm = FeatureModelManager.load(f_fm.toPath()).getObject();
			
			List<ScorePair<ProductMealy<String, Word<String>>>> prodPair_lst = new ArrayList<>();
			List<ProductMealy<String, Word<String>>> prod_lst = new ArrayList<>();
			
			Scanner scanner = new Scanner(System.in);
			while(scanner.hasNextLine()) {
				//System.out.println("-->"+scanner.nextLine());
				File a_prod = new File(scanner.nextLine());
				ProductMealy<String, Word<String>> fMealy = FeaturedMealyUtils.getInstance().loadProductMachine(a_prod,fm);
				fMealy.getInfo().setProperty("abspath", a_prod.getAbsolutePath());
				fMealy.getInfo().setProperty("filename", a_prod.getName());
				prod_lst.add(fMealy);
			}
			scanner.close();
			
			List<String> allFeatures = new ArrayList<>();
			for (IFeature node : fm.getFeatures()) {
				if(node.getName().equals("TRUE")) continue;
				allFeatures.add(node.getName());
			}
			
			for (int i = 0; i < prod_lst.size()-1; i++) {
				ProductMealy<String, Word<String>> fMealy_i = prod_lst.get(i);
				List<String> conf_i = new ArrayList<>();
				for (Node n: fMealy_i.getConfiguration()) {
					if(n instanceof Literal) conf_i.add(n.toString());
				}
				
				for (int j = i+1; j < prod_lst.size(); j++) {
					ProductMealy<String, Word<String>> fMealy_j = prod_lst.get(j);
					List<String> conf_j = new ArrayList<>();
					for (Node n: fMealy_j.getConfiguration()) {
						if(n instanceof Literal) conf_j.add(n.toString());
					}
					
					float sc = 1 - clacDistance(conf_i, conf_j, allFeatures);
					prodPair_lst.add(new ScorePair<ProductMealy<String,Word<String>>>(sc, fMealy_i, fMealy_j));
					System.err.println(fMealy_i.getInfo().getProperty("filename"));
					System.err.println(fMealy_j.getInfo().getProperty("filename"));
					System.err.println(String.format("Score: %f", sc));
				}				
			}
			
			if(line.hasOption(SHUFFLE)) Collections.shuffle(prodPair_lst);
			boolean by_similarity = line.hasOption(SIMILARITY);
			
			if(line.hasOption(GMDP)) {
				prod_lst = prioritizeGlobalMaxDistance(prod_lst,prodPair_lst,by_similarity);
			}else {
				prod_lst = prioritizeLocalMaxDistance (prod_lst,prodPair_lst,by_similarity);
			}
			
			for (ProductMealy<String, Word<String>> productMealy : prod_lst) {
				System.out.println(productMealy.getInfo().get("abspath"));
			}
			
			

			
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
	}
	
	private static List<ProductMealy<String, Word<String>>> prioritizeLocalMaxDistance(
			List<ProductMealy<String, Word<String>>> prod_lst,
			List<ScorePair<ProductMealy<String, Word<String>>>> prodPair_lst, 
			boolean by_similarity) {
		
		int pairTot = prod_lst.size();
		List<ScorePair<ProductMealy<String, Word<String>>>> prodPair_copy = new ArrayList<>(prodPair_lst);
		
		Collections.sort(prodPair_copy);
		if(!by_similarity) Collections.reverse(prodPair_copy);
		
		Iterator<ScorePair<ProductMealy<String, Word<String>>>> iter = prodPair_copy.iterator();
		List<ProductMealy<String, Word<String>>> result_lst = new ArrayList<>();
		Set<ProductMealy<String, Word<String>>> pairSet = new HashSet<>();
		
		while (pairTot>0){
			ScorePair<ProductMealy<String, Word<String>>> pair = iter.next();
			
			ProductMealy<String, Word<String>> ci = pair.getStatei();
			ProductMealy<String, Word<String>> cj = pair.getStatej();
			
			if(!pairSet.contains(ci) && !pairSet.contains(cj)){
				result_lst.add(ci); pairSet.add(ci); --pairTot;
				result_lst.add(cj); pairSet.add(cj); --pairTot;
//				System.out.println(pair.getScore());
			}
			if(pairTot==1){
				for (ProductMealy<String, Word<String>> a_conf : prod_lst) {
					if(!pairSet.contains(a_conf)) {
						result_lst.add(a_conf); pairSet.add(a_conf); --pairTot;
					}
				}
			}
		}
		
		
		return result_lst;
	}

	private static List<ProductMealy<String, Word<String>>> prioritizeGlobalMaxDistance(
			List<ProductMealy<String, Word<String>>> prod_lst,
			List<ScorePair<ProductMealy<String, Word<String>>>> prodPair_lst, 
			boolean by_similarity) {

		List<ProductMealy<String, Word<String>>> to_check = new ArrayList<>(prod_lst);
		Map<ProductMealy<String, Word<String>>,Map<ProductMealy<String, Word<String>>,Double>> pairSim = new HashMap<>();
		
		ScorePair<ProductMealy<String, Word<String>>>  best_pair = null;
		double best_ds;
		if(by_similarity) best_ds = Double.MAX_VALUE;	
		else best_ds = Double.MIN_VALUE;
		boolean is_best = true;
		
		for (ScorePair<ProductMealy<String, Word<String>>> scorePair : prodPair_lst) {
			if(!pairSim.containsKey(scorePair.getStatei())) pairSim.put(scorePair.getStatei(),new HashMap<>());
			if(!pairSim.containsKey(scorePair.getStatej())) pairSim.put(scorePair.getStatej(),new HashMap<>());
			
			pairSim.get(scorePair.getStatei()).put(scorePair.getStatej(), scorePair.getScore());
			pairSim.get(scorePair.getStatej()).put(scorePair.getStatei(), scorePair.getScore());
			
			
			if(by_similarity) is_best = scorePair.getScore() < best_ds;	
			else is_best = scorePair.getScore() > best_ds;
			
			if(is_best) {
				best_ds = scorePair.getScore();
				best_pair = scorePair;
			}
		}
//		System.out.println(best_ds);
		List<ProductMealy<String, Word<String>>> result_lst = new ArrayList<>();
		
		result_lst.add(best_pair.getStatei());
		result_lst.add(best_pair.getStatej());
		
		Map<ProductMealy<String, Word<String>>,Double> ds_sum = new HashMap<>();
		to_check.forEach(conf -> ds_sum.put(conf, 0.0));
		update_ds_sum(prod_lst,pairSim.get(best_pair.getStatei()),best_pair.getStatei(),ds_sum);
		update_ds_sum(prod_lst,pairSim.get(best_pair.getStatej()),best_pair.getStatej(),ds_sum);
		//
		to_check.remove(best_pair.getStatei());
		to_check.remove(best_pair.getStatej());
		
		ProductMealy<String, Word<String>> best_prod = null;
		while (to_check.size()>0){
			
			if(by_similarity) best_ds = Double.MAX_VALUE;
			else best_ds = Double.MIN_VALUE;
			for (ProductMealy<String, Word<String>> productMealy : to_check) {
				double tmp_ds = ds_sum.get(productMealy);
				
				if(by_similarity) is_best = tmp_ds < best_ds;
				else is_best = tmp_ds > best_ds;
				if(is_best) {
					best_ds = tmp_ds;
					best_prod = productMealy;
				}
			}
//			System.out.println(best_ds);
			result_lst.add(best_prod);
			update_ds_sum(prod_lst,pairSim.get(best_prod),best_prod,ds_sum);
			to_check.remove(best_prod);
		}
		
		return result_lst;
	}

	private static void update_ds_sum(
			List<ProductMealy<String, Word<String>>> result_lst,
			Map<ProductMealy<String, Word<String>>, Double> map,
			ProductMealy<String, Word<String>> state, 
			Map<ProductMealy<String, Word<String>>,Double> ds_sum
			) {
		map.put(state, null);
		for (ProductMealy<String, Word<String>> t : result_lst) {
			if(map.get(t) != null) {
				ds_sum.put(t,ds_sum.get(t) + map.get(t));
			}
		}
		
		
		
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
		options.addOption( SHUFFLE,    false, "Shuffle" );
		options.addOption( GMDP,    false, "Use GMDP to sort products (default: LMDP)" );
		options.addOption( SIMILARITY,  false, "Sort by similarity (default: Dissimilarity)" );		
		options.addOption( HELP,  false, "Help menu" );
		return options;
	}	

}
