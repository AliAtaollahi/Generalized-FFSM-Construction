package uk.le.ac.convertFfsmToProject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealy;
import uk.le.ac.ffsm.FeaturedMealyUtils;

public class DataManagerFactory {
	private static final String ALL_FEATURES = "All Features";
	private static final String ROOT_CONFIG = "MinePumpSys";

	private static final String HELP = "h";
	private static final String FM = "fm";
	private static final String FFSM = "ffsm";
	private static final String CONFIG = "config";
	private static final String OUT = "out";
	private static final String ALPHABET = "alphabet";
	private static final String NO_LOOP = "no_loop";
	private static final String CHECK_SCC = "check_scc";

	private CommandLineParser parser_;
	private Options options_;
	private HelpFormatter formatter_;
	private CommandLine line_;

	public DataManagerFactory() {
		parser_ = new BasicParser();
		options_ = create_and_get_options();
		formatter_ = new HelpFormatter();
	}

	public void set_arguments(String[] args) {
		try {
			line_ = parser_.parse(options_, args);
			check_necessary_options();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private static Options create_and_get_options() {
		Options options = new Options();
		options.addOption(HELP, false, "Help menu");
		options.addOption(FM, true, "Feature model");
		options.addOption(FFSM, true, "FFSM");
		options.addOption(CONFIG, true, "Product configuration");
		options.addOption(OUT, true, "Output directory");
		options.addOption(ALPHABET, true, "Alphabet of features");
		options.addOption(NO_LOOP, true, "Determine is there loops");
		options.addOption(CHECK_SCC, true, "Consider one scc that root is there");
		return options;
	}

	private void check_necessary_options() {
		if (line_.hasOption(HELP) || !line_.hasOption(FM) || !line_.hasOption(FFSM) || !line_.hasOption(CONFIG)
				|| !line_.hasOption(OUT) || !line_.hasOption(NO_LOOP) || !line_.hasOption(CHECK_SCC)) {
			formatter_.printHelp("Generalize FFSM", options_);
			System.exit(0);
		}
	}

	public IFeatureModel read_and_get_feature_model() {
		File fm_file = new File(line_.getOptionValue(FM));
		return FeatureModelManager.load(fm_file.toPath()).getObject();
	}

	public FeaturedMealy<String, Word<String>> read_and_get_ffsm(IFeatureModel fm) {
		File ffsm_file_1 = new File(line_.getOptionValue(FFSM));
		FeaturedMealy<String, Word<String>> ffsm = null;
		try {
			ffsm = FeaturedMealyUtils.getInstance().loadFeaturedMealy(ffsm_file_1, fm);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ffsm;
	}

	public Map<String, List<String>> read_and_get_features_alphabet() {
		List<String> lines = read_file_lines(line_.getOptionValue(ALPHABET));
		return create_and_get_alphabet(lines);
	}

	private static List<String> read_file_lines(String file_path) {
		List<String> lines = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
			String line_;
			while ((line_ = br.readLine()) != null) {
				lines.add(line_);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	private static Map<String, List<String>> create_and_get_alphabet(List<String> lines) {
		Map<String, List<String>> alphabet = new HashMap<>();
		for (String line_ : lines) {
			
			String[] splitted_line = line_.split(": ");
			String feature = splitted_line[0].trim();
			List<String> input_signals = Arrays.asList(splitted_line[1].split(", "));
			
			if(feature.equals(ALL_FEATURES))
				for(String inp : input_signals)
					alphabet.put(inp, null);
			
			alphabet.put(feature, input_signals);
		}
		
		return alphabet;
	}

	public Map<Object, Boolean> read_and_get_config(Set<String> features) {
		List<String> config_lines = read_file_lines(line_.getOptionValue(CONFIG));
		return create_and_get_config(config_lines, features);
	}

	private static Map<Object, Boolean> create_and_get_config(List<String> config_lines, Set<String> features) {
		Map<Object, Boolean> configs = new HashMap<>();
		add_feature_to_configs(config_lines, features, configs);
		add_root_config(configs);
		return configs;
	}

	private static void add_feature_to_configs(List<String> config_lines, Set<String> features,
			Map<Object, Boolean> configs) {
		for (String feature : features)
			configs.put(feature.toString(), config_lines.contains(feature));
	}

	private static void add_root_config(Map<Object, Boolean> configs) {
		configs.put(ROOT_CONFIG, true);
	}

	public void write_output(StringBuilder buffer, String file_extension) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(line_.getOptionValue(OUT) + file_extension))) {
			bw.append(buffer.toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean get_loop_existance_status() {
		return Boolean.parseBoolean(line_.getOptionValue(NO_LOOP));
	}

    public boolean get_should_one_scc_check_status() {
        return Boolean.parseBoolean(line_.getOptionValue(CHECK_SCC));
    }
}
