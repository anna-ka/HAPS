package experiments;

import java.util.ArrayList;
import java.util.Locale;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;

/**
 * an implementation of AbstractParameterDictionary to process parameters for the APS segmenter
 * @author anna
 *
 */
public class APSParameterDictionary extends AbstractParameterDictionary {
	
	public APSParameterDictionary()
	{
		
		super();
		
		//put in some defaults
		ArrayList<Object> segmPattern = new ArrayList<Object>();
		segmPattern.add("[=]+");
		ArrayList<Object> segmLevel = new ArrayList<Object>();
		segmLevel.add(new Double(1));
		ArrayList<Object> useLowerCase = new ArrayList<Object>();
		useLowerCase.add(new Boolean(true));
		ArrayList<Object> useStemmer = new ArrayList<Object>();
		useStemmer.add(new Boolean(true));
		ArrayList<Object> removeStopWords = new ArrayList<Object>();
		removeStopWords.add(new Boolean(true));
		ArrayList<Object> stopWordsFile = new ArrayList<Object>();
		stopWordsFile.add("./STOPWORD.list");
		ArrayList<Object> useSegmentDf = new ArrayList<Object>();
		useSegmentDf.add(new Boolean(false));
		ArrayList<Object> sparse = new ArrayList<Object>();
		sparse.add(new Boolean(false));
		ArrayList<Object> smoothing = new ArrayList<Object>();
		smoothing.add(new Boolean(false));
		ArrayList<Object> trainResultsFile = new ArrayList<Object>();
		trainResultsFile.add("train_results.txt");
		ArrayList<Object> evalMetric = new ArrayList<Object>();
		evalMetric.add("winDiff");
		ArrayList<Object> useWeightedTf = new ArrayList<Object>();
		useWeightedTf.add(new Boolean(false));
		ArrayList<Object> parThreshold = new ArrayList<Object>();
		parThreshold.add(new Double(3));
		ArrayList<Object> useWordNetSim = new ArrayList<Object>();
		useWordNetSim.add(new Boolean(false));
		ArrayList<Object> useSTSSim = new ArrayList<Object>();
		useSTSSim.add(new Boolean(false));
		ArrayList<Object> outputSTSSim = new ArrayList<Object>();
		outputSTSSim.add(new Boolean(false));
		ArrayList<Object> loadSTSSim = new ArrayList<Object>();
		loadSTSSim.add(new Boolean(false));
		ArrayList<Object> useReadyMatrix = new ArrayList<Object>();
		useReadyMatrix.add(new Boolean(false));
		ArrayList<Object> useHier = new ArrayList<Object>();
		useHier.add(new Boolean(false));

		
		this.paramValueDictionary.put("segmPattern", segmPattern);
		this.paramValueDictionary.put("segmLevel", segmLevel);
		this.paramValueDictionary.put("useLowerCase", useLowerCase);
		this.paramValueDictionary.put("useStemmer", useStemmer);
		this.paramValueDictionary.put("removeStopWords", removeStopWords);
		this.paramValueDictionary.put("stopWordsFile", stopWordsFile);
		this.paramValueDictionary.put("parThreshold", parThreshold);

		
		this.paramValueDictionary.put("useSegmentDf", useSegmentDf);
		this.paramValueDictionary.put("useWeightedTf", useWeightedTf);
		
		this.paramValueDictionary.put("smoothing", smoothing);
		this.paramValueDictionary.put("sparse", sparse);
		this.paramValueDictionary.put("trainResultsFile", trainResultsFile);
		this.paramValueDictionary.put("evalMetric", evalMetric);
		this.paramValueDictionary.put("useWordNetSim", useWordNetSim);
		this.paramValueDictionary.put("useSTSSim", useSTSSim);
		this.paramValueDictionary.put("outputSTSSim", outputSTSSim);
		this.paramValueDictionary.put("loadSTSSim", loadSTSSim);
		
		this.paramValueDictionary.put("useReadyMatrix", useReadyMatrix);
		this.paramValueDictionary.put("useHier", useHier);
	}

	/**
	 * This method defines how APS command line options must be parsed
	 */
	public void ParseCommandLine(String[] args) throws Exception {
		Options curOptions = this.ProcessCommandLine(args);
		
		CommandLineParser parser = new BasicParser();
		CommandLine parsedCmd = parser.parse( curOptions, args);
		
		//see if we need to load things from a config file
		if (parsedCmd.hasOption("useConfig"))
		{
			String configFilePath = parsedCmd.getOptionValue("useConfig").toString();
			this.ParseConfigFile(configFilePath);
			return;
		}
		
		//otherwise keep parsing the command line
		//at first we want to get in all options we need anyways
		
		//by default we are in run mode
		this.SetValue("run", new Boolean(true));
		this.SetValue("tune", new Boolean(false));
		this.SetValue("useCorefSim", new Boolean(false));
		this.SetValue("useWordNetSim", new Boolean(false));
		
		if (parsedCmd.hasOption("tune"))
		{
			this.SetValue("run", new Boolean(false));
			this.SetValue("tune", new Boolean(true));
		}
		if (parsedCmd.hasOption("useHier"))
		{
			this.SetValue("useHier", new Boolean(true));
		}
		if (parsedCmd.hasOption("useCorefSim"))
		{
			this.SetValue("useCorefSim", new Boolean(true));
		}
		if (parsedCmd.hasOption("useWordNetSim"))
		{
			this.SetValue("useWordNetSim", new Boolean(true));
		}
		if (parsedCmd.hasOption("useSTSSim"))
		{
			this.SetValue("useSTSSim", new Boolean(true));
			if (parsedCmd.hasOption("loadSTSSim"))
			{
				this.SetValue("loadSTSSim", new Boolean(true));
			}
			else if (parsedCmd.hasOption("outputSTSSim"))
			{
				this.SetValue("outputSTSSim", new Boolean(true));
			}
			if (parsedCmd.hasOption("stsSerDir"))
			{

				String stsDirPath = parsedCmd.getOptionValue("stsSerDir").toString();
				this.SetValue("stsSerDir", stsDirPath);
			}
		}
		
		
		if (parsedCmd.hasOption("segmPattern"))
		{

			String segmPattern = parsedCmd.getOptionValue("segmPattern").toString();
			this.SetValue("segmPattern", segmPattern);
		}
		if (parsedCmd.hasOption("inputSimpleTextDir"))
		{

			String inputTextDir = parsedCmd.getOptionValue("inputSimpleTextDir").toString();
			this.SetValue("inputSimpleTextDir", inputTextDir);
		}
		if (parsedCmd.hasOption("inputFile"))
		{

			String inputFilePath = parsedCmd.getOptionValue("inputFile").toString();
			this.SetValue("inputFile", inputFilePath);
		}
		else if (parsedCmd.hasOption("inputDir"))
		{
			String inputDirPath = parsedCmd.getOptionValue("inputDir").toString();
			this.SetValue("inputDir", inputDirPath);
		}
		
		if (parsedCmd.hasOption("annotDir"))
		{
			String annotDirPath = parsedCmd.getOptionValue("annotDir").toString();
			this.SetValue("annotDir", annotDirPath);
		}
		
		if (parsedCmd.hasOption("dataSourceType"))
		{
			String dsClass = parsedCmd.getOptionValue("dataSourceType");
			this.SetValue("dataSourceType", dsClass);
		}
		
		if (parsedCmd.hasOption("inputDataType"))
		{
			Double type = Double.valueOf( parsedCmd.getOptionValue("inputDataType") );
			this.SetValue("inputDataType", type);
		}
		
		String outputDirPath = parsedCmd.getOptionValue("outputDir").toString();
		this.SetValue("outputDir", outputDirPath);
		
		String resultsFileName = parsedCmd.getOptionValue("resultsFile").toString();
		this.SetValue("resultsFile", resultsFileName);
		
		
		String[] corpusExt = parsedCmd.getOptionValues("corpusExtensions");
		this.InitializeList("corpusExtensions");
		
		for (String ext: corpusExt)
		{
			this.AddValueToList("corpusExtensions", ext);
		}
		
		String[] inputExt = parsedCmd.getOptionValues("inputExtensions");
		this.InitializeList("inputExtensions");
		for (String ext: inputExt)
		{
			this.AddValueToList("inputExtensions", ext);
		}
		
		if (parsedCmd.hasOption("sparse"))
		{
			this.InitializeList("sparse");
			String[] sparseValues = parsedCmd.getOptionValues("sparse");
			
			for (String sp: sparseValues)
			{
				this.AddValueToList("sparse", new Boolean(sp));
			}
		}
		else
		{
			this.SetValue("sparse", new Boolean(false));
		}
		
		if (parsedCmd.hasOption("smoothing"))
		{
			this.InitializeList("smoothing");
			String[] smoothValues = parsedCmd.getOptionValues("smoothing");
			
			for (String sm: smoothValues)
			{
				this.AddValueToList("smooth", new Boolean(sm));
			}
			
			if (parsedCmd.hasOption("smoothingAlpha"))
			{
				this.InitializeList("smoothingAlpha");
				String[] smAlphas = parsedCmd.getOptionValues("smoothingAlpha");
				for (String alpha: smAlphas)
				{
					Double value = new Double(alpha);
					this.AddValueToList("smoothingAlpha", value);
				}
			}
			if (parsedCmd.hasOption("smoothingWindow"))
			{
				this.InitializeList("smoothingWindow");
				String[] wins = parsedCmd.getOptionValues("smoothingWindow");
				for (String curWin: wins)
				{
					Double value = new Double(curWin);
					this.AddValueToList("smoothingWindow", value);
				}
			}
			
		}
		
		if (parsedCmd.hasOption("useSegmentDf"))
		{
			this.InitializeList("useSegmentDf");
			String[] dfValues = parsedCmd.getOptionValues("useSegmentDf");
			
			for (String df: dfValues)
			{
				this.AddValueToList("useSegmentDf", new Boolean(df));
			}
			
			if (parsedCmd.hasOption("numTFIDFsegments"))
			{
				this.InitializeList("numTFIDFsegments");
				String[] nums = parsedCmd.getOptionValues("numTFIDFsegments");
				for (String curNum: nums)
				{
					Double value = new Double(curNum);
					this.AddValueToList("numTFIDFsegments", value);
				}
			}
		}
		else
		{
			this.SetValue("useSegmentDf", new Boolean(false));
		}
		
		if (parsedCmd.hasOption("useWeightedTf"))
		{
			this.InitializeList("useWeightedTf");
			String[] wTfValues = parsedCmd.getOptionValues("useWeightedTf");
			
			for (String wTf: wTfValues)
			{
				this.AddValueToList("useWeightedTf", new Boolean(wTf));
			}
		}
		
		
		if (parsedCmd.hasOption("preference"))
		{
			this.InitializeList("preference");
			String[] prefs = parsedCmd.getOptionValues("preference");
			for (String curPref: prefs)
			{
				Double value = new Double(curPref);
				this.AddValueToList("preference", value);
			}
		}
		
		if (parsedCmd.hasOption("preference_level0"))
		{
			this.InitializeList("preference_level0");
			String[] prefs = parsedCmd.getOptionValues("preference_level0");
			for (String curPref: prefs)
			{
				Double value = new Double(curPref);
				this.AddValueToList("preference_level0", value);
			}
		}
		if (parsedCmd.hasOption("preference_level1"))
		{
			this.InitializeList("preference_level1");
			String[] prefs = parsedCmd.getOptionValues("preference_level1");
			for (String curPref: prefs)
			{
				Double value = new Double(curPref);
				this.AddValueToList("preference_level1", value);
			}
		}
		
		if (parsedCmd.hasOption("preference_level2"))
		{
			this.InitializeList("preference_level2");
			String[] prefs = parsedCmd.getOptionValues("preference_level2");
			for (String curPref: prefs)
			{
				Double value = new Double(curPref);
				this.AddValueToList("preference_level2", value);
			}
		}
		
		if (parsedCmd.hasOption("damping"))
		{
			this.InitializeList("damping");
			String[] damps = parsedCmd.getOptionValues("damping");
			for (String curDamp: damps)
			{
				Double value = new Double(curDamp);
				this.AddValueToList("damping", value);
			}
		}
		
		if (parsedCmd.hasOption("windowRatio"))
		{
			this.InitializeList("windowRatio");
			String[] ratios = parsedCmd.getOptionValues("windowRatio");
			for (String curRatio: ratios)
			{
				Double value = new Double(curRatio);
				this.AddValueToList("windowRatio", value);
			}
		}
		else if (parsedCmd.hasOption("windowSize"))
		{
			this.InitializeList("windowSize");
			String[] sizes = parsedCmd.getOptionValues("windowSize");
			for (String curSize: sizes)
			{
				Double value = new Double(curSize);
				this.AddValueToList("windowSize", value);
			}
		}
		
		if (parsedCmd.hasOption("segmLevel"))
		{
			String valueStr = parsedCmd.getOptionValue("segmLevel");
			Double value = new Double (valueStr);
			ArrayList<Object> segmLevel = new ArrayList<Object>();
			segmLevel.add(value);
			this.SetList("segmLevel", segmLevel);
		}
		if (parsedCmd.hasOption("evalMetric"))
		{
			String valueStr = parsedCmd.getOptionValue("evalMetric");
			ArrayList<Object> evalMetric = new ArrayList<Object>();
			evalMetric.add(valueStr);
			this.SetList("evalMetric", evalMetric);
		}
		
		if (parsedCmd.hasOption("decayFactor"))
		{
			this.InitializeList("decayFactor");
			String[] factors = parsedCmd.getOptionValues("decayFactor");
			for (String curDecayFactor: factors)
			{
				Double value = new Double(curDecayFactor);
				this.AddValueToList("decayFactor", value);
			}
		}
		if (parsedCmd.hasOption("decayWindowSize"))
		{
			this.InitializeList("decayWindowSize");
			String[] sizes = parsedCmd.getOptionValues("decayWindowSize");
			for (String curSize: sizes)
			{
				Double value = new Double(curSize);
				this.AddValueToList("decayWindowSize", value);
			}
		}
		
		if (parsedCmd.hasOption("parThreshold"))
		{
			Double v = Double.valueOf( parsedCmd.getOptionValue("parThreshold") );
			this.SetValue("parThreshold", v);
		}
		
		if (parsedCmd.hasOption("corefWeights"))
		{
			this.InitializeList("corefWeights");
			String[] weights = parsedCmd.getOptionValues("corefWeights");
			for (String curWeight: weights)
			{
				Double value = new Double(curWeight);
				this.AddValueToList("corefWeights", value);
			}
		}	
	}
	
	/**
	 * This method defines APS command-line options to be used by Apache CLI parser
	 * @param args
	 * @return
	 */
	protected Options ProcessCommandLine(String[] args)
	{
		
		Options allOptions = new Options();
		
		
		//there is an option to get arguments from a config file
		Option useConfig = OptionBuilder.withArgName( "useConfig" )
		        .hasArg()
		        .withDescription(  "get all options from this config file" )
		        .create( "useConfig" );
		Options prelimOptions = new Options();
		prelimOptions.addOption(useConfig);
		
		//if useConfig is set then we need to work with the configFile instead
		CommandLineParser prelimParser = new BasicParser();
		try{
			CommandLine cmdLine = prelimParser.parse(prelimOptions, args);
			if ( cmdLine.hasOption( "useConfig" ) )
			{
				//call the command line parser and parse them instead
				return prelimOptions;
			}
		}
		catch(ParseException e)
		{
			
		}
	
		
		//boolean options
		Option run = new Option( "run", "Run the segmenter" );			
		Option tune = new Option ("tune", "Find the best parameters on using a development set of data");
		Option evaluate = new Option("evaluate", "Run the segmenetr on a test set and output the results ");
		OptionGroup modeGroup = new OptionGroup();
		modeGroup.addOption(run);
		modeGroup.addOption(tune);
		modeGroup.addOption(evaluate);
		
		Option sparse = new Option("sparse", "Use sparse APS. Suitable for sparse input matrices of similarities");
		Option smoothing = new Option("smoothing", "Smooth the tf.idf matrix");
		Option useSegmentDf = new Option ("useSegmentDf", "Do you want to segment the input file for computing tf.idf instead of using a corpus?");
		Option useWeightedTf = new Option ("useWeightedTf", "Do you want to use a weighted version of tf.idf?");
		Option useCorefSim = new Option ("useCorefSim", "Do you want to use coreference similarity?");
		Option useWordNetSim = new Option ("useWordNetSim", "Do you want to use WordNet to compute similarity?");
		Option useSTSSim = new Option ("useSTSSim", "Do you want to use DKPRoSimilarity, Semeval Text Sim. baseline 2013 ?");
		Option loadSTSSim = new Option("loadSTSSim", "Do you want to deserialize already precomputed STS similarities?");
		Option outputSTSSim = new Option("outputSTSSim", "Do you want to serializeSTS similarities?");
		Option useReadyMatrix = new Option("useReadyMatrix", "Do you want to supply MinCutSeg with a matrix computed by APS?");
		Option useHier = new Option("useHier", "Do you want to perfrom hierarchical segmentation?");
		
		
		//rest of the options
		Option parThreshold = OptionBuilder.withArgName( "parThreshold" )
		        .hasArg()
		        .withDescription(  "number of opening sentences to consider in each paragraph when computing coref sim" )
		        .withValueSeparator(',')
		        .withType(Double.class)
		        .create( "parThreshold" );
		
		Option prefValue = OptionBuilder.withArgName( "preference" )
		        .hasArg()
		        .withDescription(  "uniform preference values" )
		        .withValueSeparator(',')
		        .withType(Double.class)
		        .create( "preference" );
		
		Option preference_level0 = OptionBuilder.withArgName( "preference_level0" )
		        .hasArg()
		        .withDescription(  "uniform preference values for level 0 (bottom of the tree)" )
		        .withValueSeparator(',')
		        .withType(Double.class)
		        .create( "preference_level0" );
		
		Option preference_level1 = OptionBuilder.withArgName( "preference_level1" )
		        .hasArg()
		        .withDescription(  "uniform preference values for level 1 " )
		        .withValueSeparator(',')
		        .withType(Double.class)
		        .create( "preference_level1" );
		
		Option preference_level2 = OptionBuilder.withArgName( "preference_level2" )
		        .hasArg()
		        .withDescription(  "uniform preference values for level 2 (top of the tree)" )
		        .withValueSeparator(',')
		        .withType(Double.class)
		        .create( "preference_level2" );
		
		Option dampValue = OptionBuilder.withArgName( "damping" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "damping coefficient for APS" )
		        .withType(Double.class)
		        .create( "damping" );
		
		OptionGroup winSizeGroup = new OptionGroup();
		
		
		Option windowRatio = OptionBuilder.withArgName( "windowRatio" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "the size of the sliding window expressed as a coefficient. Must be <= 1 " )
		        .withType(Double.class)
		        .create( "windowRatio" );

		Option windowSize = OptionBuilder.withArgName( "windowSize" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "the size of the sliding window expressed in absolute terms. Must be less or equa to the document length" )
		        .withType(Integer.class)
		        .create( "windowSize" );
		
		winSizeGroup.addOption(windowSize);
		winSizeGroup.addOption(windowRatio);
		
		Option annotDir = OptionBuilder.withArgName( "annotDir" )
		        .hasArg()
		        .withDescription(  "directory where reference annotations are stored" )
		        .withType(String.class)
		        .create( "annotDir" );
		
		Option outputDir = OptionBuilder.withArgName( "outputDir" )
		        .hasArg()
		        .withDescription(  "directory where results will be output" )
		        .withType(String.class)
		        .create( "outputDir" );
		Option stsSerDir = OptionBuilder.withArgName( "stsSerDir" )
		        .hasArg()
		        .withDescription(  "Directory for serializtion/deserialization of STS similarities" )
		        .withType(String.class)
		        .create( "stsSerDir" );
		
		
		OptionGroup inputGroup = new OptionGroup();
		
		Option inputDir = OptionBuilder.withArgName( "inputDir" )
		        .hasArg()
		        .withDescription(  "Directory with the input files" )
		        .withType(String.class)
		        .create( "inputDir" );
		
		Option inputFile = OptionBuilder.withArgName( "inputFile" )
		        .hasArg()
		        .withDescription(  "Path to the input file" )
		        .withType(String.class)
		        .create( "inputFile" );
		Option inputSimpleTextDir = OptionBuilder.withArgName( "inputSimpleTextDir" )
		        .hasArg()
		        .withDescription(  "Path to input files in text format to be used by MinCutSeg.jar" )
		        .withType(String.class)
		        .create( "inputSimpleTextDir" );
		
		inputGroup.addOption(inputDir);
		inputGroup.addOption(inputFile);
		inputGroup.addOption(inputSimpleTextDir);
		
		Option dataSourceType = OptionBuilder.withArgName( "dataSourceType" )
		        .hasArg()
		        .withDescription(  "name of the class implementing IDataSource" )
		        .withType(String.class)
		        .create( "dataSourceType" );
		
		Option inputDataType = OptionBuilder.withArgName( "inputDataType" )
		        .hasArg()
		        .withDescription(  "name of the class implementing IGenericDataSource" )
		        .withType(Integer.class)
		        .create( "inputDataType" );
		
		
		Option inputExtensions = OptionBuilder.withArgName( "inputExtensions" )
		        .hasArgs()
		        .withDescription(  "extensions of the files that need to be segmented" )
		        .withValueSeparator(',')
		        .withType(String.class)
		        .create( "inputExtensions" );
		
		Option corpusExtensions = OptionBuilder.withArgName( "corpusExtensions" )
		        .hasArgs()
		        .withDescription(  "extensions of the files that will be used to compute tf.idf" )
		        .withValueSeparator(',')
		        .withType(String.class)
		        .create( "corpusExtensions" );
		
		Option resultsFile = OptionBuilder.withArgName( "resultsFile" )
		        .hasArg()
		        .withDescription(  "the name of the results file that will be output in the outputDir" )
		         .withType(String.class)
		        .create( "resultsFile" );
		
		Option smoothingAlpha = OptionBuilder.withArgName( "smoothingAlpha" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "value of the smoothing alpha" )
		         .withType(Double.class)
		        .create( "smoothingAlpha" );
		
		Option smoothingWindow = OptionBuilder.withArgName( "smoothingWindow" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "size of the smoothing window" )
		        .withType(Integer.class)
		        .create( "smoothingWindow" );
		
		Option numTFIDFsegments = OptionBuilder.withArgName( "numTFIDFsegments" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "number of segemnts for tf.idf computation" )
		         .withType(Integer.class)
		        .create( "numTFIDFsegments" );
		
		Option segmPattern = OptionBuilder.withArgName("segmPattern").hasArg()
				.withValueSeparator(',')
		        .withDescription(  "pattern to recognize segmentation markers in documents" )
		        .withType(String.class)
		        .create( "segmPattern" );
		
		Option segmLevel = OptionBuilder.withArgName("segmLevel").hasArg()
				.withValueSeparator(',')
		        .withDescription(  "a constant from IGenericData source to specify if docs are segmented as senence or paragraph level" )
		        .withType(Integer.class)
		        .create( "segmLevel" );
		Option evalMetric = OptionBuilder.withArgName("evalMetric").hasArg()
				.withValueSeparator(',')
		        .withDescription(  "the name of the evaluation metric to use" )
		        .withType(String.class)
		        .create( "evalMetric" );
		
		//now options for coref similarity
		Option decayFactor = OptionBuilder.withArgName( "decayFactor" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "decay factor for coreference similarity" )
		         .withType(Double.class)
		        .create( "decayFactor" );
		
		Option decayWindowSize = OptionBuilder.withArgName( "decayWindowSize" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "size of the window for computing decay in coref similarity" )
		        .withType(Integer.class)
		        .create( "decayWindowSize" );
		
		Option corefWeights = OptionBuilder.withArgName( "corefWeights" )
		        .hasArg()
		        .withValueSeparator(',')
		        .withDescription(  "weights for 9 types of referential expressions when computing coref similarity" )
		        .withType(Double.class)
		        .create( "corefWeights" );

		allOptions.addOptionGroup(modeGroup);
		allOptions.addOptionGroup(winSizeGroup);
		allOptions.addOptionGroup(inputGroup);
		
		allOptions.addOption(dataSourceType);
		allOptions.addOption(inputDataType);
		
		allOptions.addOption(sparse);
		allOptions.addOption(smoothing);
		allOptions.addOption(useSegmentDf);
		allOptions.addOption(useWeightedTf);
		allOptions.addOption(useCorefSim);
		allOptions.addOption(useWordNetSim);
		
		allOptions.addOption(parThreshold);
		allOptions.addOption(prefValue);
		allOptions.addOption(preference_level0);
		allOptions.addOption(preference_level1);
		allOptions.addOption(preference_level2);
		
		
		allOptions.addOption(dampValue);
		allOptions.addOption(annotDir);
		allOptions.addOption(outputDir);
		allOptions.addOption(inputExtensions);
		allOptions.addOption(corpusExtensions);
		allOptions.addOption(resultsFile);
		allOptions.addOption(smoothingAlpha);
		allOptions.addOption(smoothingWindow);
		allOptions.addOption(useSegmentDf);
		allOptions.addOption(numTFIDFsegments);
		
		allOptions.addOption(segmLevel);
		allOptions.addOption(segmPattern);
		allOptions.addOption(evalMetric);
		allOptions.addOption(decayFactor);
		allOptions.addOption(decayWindowSize);
		allOptions.addOption(corefWeights);
		
		allOptions.addOption(useSTSSim);
		allOptions.addOption(outputSTSSim);
		allOptions.addOption(loadSTSSim);
		allOptions.addOption(stsSerDir);
		allOptions.addOption(useReadyMatrix);
		allOptions.addOption(useHier);
		
		
	
		return allOptions;
	
	
	}
	

	/**
	 * This method defines how APS config files must be parsed
	 */
	public void ParseConfigFile(String configPath) throws Exception {

		
		PropertiesConfiguration config = new PropertiesConfiguration(configPath);
		
		//mode: running or tuning parameters?
		//by default we are in run mode
		this.SetValue("run", new Boolean (true));
		this.SetValue("tune", new Boolean (false));
		this.SetValue("useCorefSim", new Boolean (false));
		
		if (config.containsKey("tune"))
		{
			this.SetValue("tune", new Boolean (true));
			this.SetValue("run", new Boolean (false));
		}
		if (config.containsKey("useHier"))
		{
			this.SetValue("useHier", new Boolean (true));
		}
		if (config.containsKey("useCorefSim"))
		{
			this.SetValue("useCorefSim", new Boolean (true));
		}
		if (config.containsKey("useWordNetSim"))
		{
			this.SetValue("useWordNetSim", new Boolean (true));
		}
		if (config.containsKey("useSTSSim"))
		{
			this.SetValue("useSTSSim", new Boolean(true));
			if (config.containsKey("loadSTSSim"))
			{
				this.SetValue("loadSTSSim", new Boolean(true));
			}
			else if (config.containsKey("outputSTSSim"))
			{
				this.SetValue("outputSTSSim", new Boolean(true));
			}
			if (config.containsKey("stsSerDir"))
			{

				String stsDirPath = config.getString("stsSerDir");
				this.SetValue("stsSerDir", stsDirPath);
			}
		}
		
		if (config.containsKey("useReadyMatrix"))
		{
			this.SetValue("useReadyMatrix", new Boolean (true));
		}
		
		
		if (config.containsKey("segmPattern"))
		{
			String pattern = config.getString("segmPattern");
			this.SetValue("segmPattern", pattern);
		}
		
		if (config.containsKey("inputSimpleTextDir"))
		{

//			String inputTextDir = parsedCmd.getOptionValue("inputSimpleTextDir").toString();
			String inputTextDir = config.getString("inputSimpleTextDir");
			this.SetValue("inputSimpleTextDir", inputTextDir);
		}
		
		if (config.containsKey("inputFile"))
		{
			String inputFilePath = config.getString("inputFile");
			this.SetValue("inputFile", inputFilePath);
		}
		else if (config.containsKey("inputDir"))
		{
			String inputDirPath = config.getString("inputDir").toString();
			this.SetValue("inputDir", inputDirPath);
		}
		
		
		if (config.containsKey("annotDir"))
		{
			String annotDirPath = config.getString("annotDir").toString();
			this.SetValue("annotDir", annotDirPath);
		}
				
		if (config.containsKey("dataSourceType"))
		{
			String dsClass = config.getString("dataSourceType");
			this.SetValue("dataSourceType", dsClass);
		}
				
		if (config.containsKey("inputDataType"))
		{
			Double type = config.getDouble("inputDataType", 0 );
			this.SetValue("inputDataType", type);
		}
				
		String outputDirPath = config.getString("outputDir");	
		this.SetValue("outputDir", outputDirPath);
				
		String resultsFileName = config.getString("resultsFile");
		this.SetValue("resultsFile", resultsFileName);
		
		
		ArrayList<Object> corpExt = new ArrayList<Object>();
		
		corpExt = (ArrayList<Object>) config.getList("corpusExtensions");
		this.InitializeList("corpusExtensions");
		
		for (Object obj: corpExt)
		{
			String ext = obj.toString();
			this.AddValueToList("corpusExtensions", ext);
		}
		
		ArrayList<Object> inExt = new ArrayList<Object>();
		inExt = (ArrayList<Object>) config.getList("inputExtensions");
		this.InitializeList("inputExtensions");
		
		for (Object obj : inExt)
		{
			String ext = obj.toString();
			this.AddValueToList("inputExtensions", ext);
		}
		
		if (config.containsKey("sparse"))
		{
			this.InitializeList("sparse");
			ArrayList<Object> sparseValues = (ArrayList<Object>) config.getList("sparse");
			for (Object spValue: sparseValues)
			{
				Boolean curVal = new Boolean ( spValue.toString());
				this.AddValueToList("sparse", curVal);
			}

		}
		else
		{
			this.SetValue("sparse", new Boolean(false));
		}
		
		if (config.containsKey("smoothing"))
		{
			
			this.InitializeList("smoothing");
			ArrayList<Object> smoothValues = (ArrayList<Object>) config.getList("smoothing");
			for (Object smValue: smoothValues)
			{
				Boolean curVal = new Boolean ( smValue.toString());
				this.AddValueToList("smoothing", curVal);
			}
			
			ArrayList<Object> allSmoothingAlphas = (ArrayList<Object>) config.getList("smoothingAlpha");
			this.InitializeList("smoothingAlpha");
			
			for (Object obj : allSmoothingAlphas)
			{
				Double alpha = new Double ( obj.toString() );
				this.AddValueToList("smoothingAlpha", alpha);
			}	
			
			
			ArrayList<Object> smoothingWindows = (ArrayList<Object>) config.getList("smoothingWindow");
			this.InitializeList("smoothingWindow");
			
			for (Object obj : smoothingWindows)
			{
				Double window = new Double ( obj.toString() );
				this.AddValueToList("smoothingWindow", window);
			}
		}
		else
		{
			this.SetValue("smoothing", new Boolean(false));
		}
				
				
		if (config.containsKey("useSegmentDf"))
		{
			this.InitializeList("useSegmentDf");
			ArrayList<Object> dfValues = (ArrayList<Object>) config.getList("useSegmentDf");
			for (Object dValue: dfValues)
			{
				Boolean curVal = new Boolean ( dValue.toString());
				this.AddValueToList("useSegmentDf", curVal);
			}
			
			ArrayList<Object> allNums = (ArrayList<Object>)config.getList("numTFIDFsegments");
			this.InitializeList("numTFIDFsegments");
			
			for (Object obj : allNums)
			{
				Double curNum = new Double ( obj.toString() );
				this.AddValueToList("numTFIDFsegments", curNum);
			}
		}
		else{
			
			this.SetValue("useSegmentDf", new Boolean(false));
		}
		
		if (config.containsKey("useWeightedTf"))
		{
			this.InitializeList("useWeightedTf");
			ArrayList<Object> wTfValues = (ArrayList<Object>) config.getList("useWeightedTf");
			for (Object wTf: wTfValues)
			{
				Boolean curVal = new Boolean ( wTf.toString());
				this.AddValueToList("useWeightedTf", curVal);
			}
		}
				
		if (config.containsKey("preference"))	
		{
			ArrayList<Object> prefs = (ArrayList<Object>) config.getList("preference");
			this.InitializeList("preference");
			
			for (Object obj : prefs)
			{
				Double curPref = new Double ( obj.toString() );
				this.AddValueToList("preference", curPref);
			}
		}
		
		if (config.containsKey("preference_level0"))	
		{
			ArrayList<Object> prefs = (ArrayList<Object>) config.getList("preference_level0");
			this.InitializeList("preference_level0");
			
			for (Object obj : prefs)
			{
				Double curPref = new Double ( obj.toString() );
				this.AddValueToList("preference_level0", curPref);
			}
		}
		
		if (config.containsKey("preference_level1"))	
		{
			ArrayList<Object> prefs = (ArrayList<Object>) config.getList("preference_level1");
			this.InitializeList("preference_level1");
			
			for (Object obj : prefs)
			{
				Double curPref = new Double ( obj.toString() );
				this.AddValueToList("preference_level1", curPref);
			}
		}
		
		if (config.containsKey("preference_level2"))	
		{
			ArrayList<Object> prefs = (ArrayList<Object>) config.getList("preference_level2");
			this.InitializeList("preference_level2");
			
			for (Object obj : prefs)
			{
				Double curPref = new Double ( obj.toString() );
				this.AddValueToList("preference_level2", curPref);
			}
		}
				
		
		if (config.containsKey("damping"))
		{
			ArrayList<Object> damps = (ArrayList<Object>) config.getList("damping");
			this.InitializeList("damping");
			
			for (Object obj : damps)
			{
				Double curDamp = new Double ( obj.toString() );
				this.AddValueToList("damping", curDamp);
			}
		}
		
		if (config.containsKey("windowRatio"))
		{
			ArrayList<Object> windows = (ArrayList<Object>) config.getList("windowRatio");
			this.InitializeList("windowRatio");
			
			for (Object obj : windows)
			{
				Double curWindow = new Double ( obj.toString() );
				this.AddValueToList("windowRatio", curWindow);
			}
		}
		else if (config.containsKey("windowSize"))
		{
			ArrayList<Object> winSizes = (ArrayList<Object>) config.getList("windowSize");
			this.InitializeList("windowSize");
			
			for (Object obj : winSizes)
			{
				Double curWindow = new Double ( obj.toString() );
				this.AddValueToList("windowSize", curWindow);
			}
		}
		
		if (config.containsKey("segmLevel"))
		{
			
			String valueStr = config.getString("segmLevel");
			Double value = new Double (valueStr);
			ArrayList<Object> segmLevel = new ArrayList<Object>();
			segmLevel.add(value);
			this.SetList("segmLevel", segmLevel);
			
		}
		
		if (config.containsKey("evalMetric"))
		{
			String valueStr = config.getString("evalMetric");
			ArrayList<Object> evalMetric = new ArrayList<Object>();
			evalMetric.add(valueStr);
			this.SetList("evalMetric", evalMetric);
		}
//		allOptions.addOption(decayFactor);
//		allOptions.addOption(decayWindowSize);
//		allOptions.addOption(corefWeights);
		
		if (config.containsKey("decayFactor"))
		{
			ArrayList<Object> factors = (ArrayList<Object>) config.getList("decayFactor");
			this.InitializeList("decayFactor");
			
			for (Object obj : factors)
			{
				Double curDecayFactor = new Double ( obj.toString() );
				this.AddValueToList("decayFactor", curDecayFactor);
			}
		}
		if (config.containsKey("decayWindowSize"))
		{
			ArrayList<Object> winSizes = (ArrayList<Object>) config.getList("decayWindowSize");
			this.InitializeList("decayWindowSize");
			
			for (Object obj : winSizes)
			{
				Double curWindow = new Double ( obj.toString() );
				this.AddValueToList("decayWindowSize", curWindow);
			}
		}
		if (config.containsKey("parThreshold"))
		{
			Double v = config.getDouble("parThreshold", 0 );
			this.SetValue("parThreshold", v);
		}
		if (config.containsKey("corefWeights"))
		{
			ArrayList<Object> weights = (ArrayList<Object>) config.getList("corefWeights");
			this.InitializeList("corefWeights");
			
			for (Object obj : weights)
			{
				Double curWeight = new Double ( obj.toString() );
				this.AddValueToList("corefWeights", curWeight);
			}
		}
	}

}
