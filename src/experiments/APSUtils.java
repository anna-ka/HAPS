package experiments;

import java.util.ArrayList;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.configuration.PropertiesConfiguration;


public class APSUtils {
	
	public APSUtils()
	{}
	
	

	public SegParameters ProcessArguments(String[] args) throws Exception
	{
		SegParameters params = new SegParameters();
		Options curOptions = this.ParseCommandLine(args);
		
		CommandLineParser parser = new BasicParser();
		CommandLine parsedCmd = parser.parse( curOptions, args);
		
		//see if we need to load things from a config file
		if (parsedCmd.hasOption("useConfig"))
		{
			String configFilePath = parsedCmd.getOptionValue("useConfig").toString();
			SegParameters configParams = ParseConfigFile(configFilePath);
			return configParams;
		}
		
		
		//at first we want to get in all options we need anyways
		
		if (parsedCmd.hasOption("inputFile"))
		{

			String inputFilePath = parsedCmd.getOptionValue("inputFile").toString();
			params.setInputFilePath(inputFilePath);
			params.ModifyCurParamsString("-inputFile: " + inputFilePath);
		}
		else if (parsedCmd.hasOption("inputDir"))
		{
			String inputDirPath = parsedCmd.getOptionValue("inputDir").toString();
			params.setInputDirPath(inputDirPath);
			params.ModifyCurParamsString("-inputDir: " + inputDirPath );
		}
		
		if (parsedCmd.hasOption("annotDir"))
		{
			String annotDirPath = parsedCmd.getOptionValue("annotDir").toString();
			params.setAnnotDirPath(annotDirPath);
			params.ModifyCurParamsString("-annotDir: " + annotDirPath + "\n");
		}
		
		if (parsedCmd.hasOption("dataSourceType"))
		{
			String dsClass = parsedCmd.getOptionValue("dataSourceType");
			params.setTypeOfDataSource(dsClass);
		}
		
		if (parsedCmd.hasOption("inputDataType"))
		{
			Integer type = Integer.valueOf( parsedCmd.getOptionValue("inputDataType") );
			params.setInputDataType(type);
		}
		
		String outputDirPath = parsedCmd.getOptionValue("outputDir").toString();
		params.setOutputDirPath(outputDirPath);
		params.ModifyCurParamsString("-outputDir: " + outputDirPath);
		
		String resultsFileName = parsedCmd.getOptionValue("resultsFile").toString();
		params.setResultsFileName(resultsFileName);
		params.ModifyCurParamsString("-resultsFile: " + resultsFileName );
		
		
		String[] corpusExt = parsedCmd.getOptionValues("corpusExtensions");
		for (String ext: corpusExt)
		{
			params.addCorpusExt(ext);
			params.ModifyCurParamsString("-corpusExtension: " + ext);
		}
		
		String[] inputExt = parsedCmd.getOptionValues("inputExtensions");
		for (String ext: inputExt)
		{
			params.addInputExt(ext);
			params.ModifyCurParamsString("-inputExtension: " + ext);
		}
		
		if (parsedCmd.hasOption("sparse"))
		{
			params.setUseSparseSegmenter(true);
			params.ModifyCurParamsString("-sparse");
		}
		
		if (parsedCmd.hasOption("smoothing"))
		{
			params.setUseSmoothing(true);
			params.ModifyCurParamsString("-smoothing");
			
			if (parsedCmd.hasOption("smoothingAlpha"))
			{
				Double parzenAlpha = Double.valueOf(parsedCmd.getOptionValue("smoothingAlpha"));
				params.setParzenAlpha(parzenAlpha);
				params.ModifyCurParamsString("\tparzenAlpha: " + parzenAlpha );
			}
			if (parsedCmd.hasOption("smoothingWindow"))
			{
				Integer parzenWindowSize = Integer.valueOf(parsedCmd.getOptionValue("smoothingWindow"));
				params.setParzenWindowSize(parzenWindowSize);
				params.ModifyCurParamsString("\tparzenWindowSize: " + parzenWindowSize);
			}
			
		}
		
		if (parsedCmd.hasOption("useSegmentDf"))
		{
			Boolean useSgmDf = Boolean.valueOf(parsedCmd.getOptionValue("useSegmentDf"));
			params.setUseSegmentDf(useSgmDf);
			params.ModifyCurParamsString("-useSegmentDf " + parsedCmd.getOptionValue("useSegmentDf"));
			if (parsedCmd.hasOption("numTFIDFsegments"))
			{
				Integer numSegments = Integer.valueOf(parsedCmd.getOptionValue("numTFIDFsegments"));
				params.setNumSegments(numSegments);
				params.ModifyCurParamsString("\tnumTFIDFsegments: " + numSegments );
			}
			if (parsedCmd.hasOption("allNumTFIDFSegments"))
			{
				String[] all = parsedCmd.getOptionValues("allNumTFIDFSegments");
				for (String cur: all)
				{
					Integer curNum = new Integer(cur);
					params.addNewNumSegm(curNum);
				}
			}
		}
		
		
		if (parsedCmd.hasOption("preference"))
		{
			Double curPref = Double.valueOf(parsedCmd.getOptionValue("preference"));
			params.setCurPref(curPref);
			params.ModifyCurParamsString("-preference: " + curPref );
		}
		
		if (parsedCmd.hasOption("damping"))
		{
			Double curDamp = Double.valueOf(parsedCmd.getOptionValue("damping"));
			params.setCurDamp(curDamp);
			params.ModifyCurParamsString("-damping: " + curDamp + "\n");
		}
		
		if (parsedCmd.hasOption("windowRatio"))
		{
			Double curWinRatio = Double.valueOf(parsedCmd.getOptionValue("windowRatio"));
			params.setCurWinRatio(curWinRatio);
			params.ModifyCurParamsString("-windowRatio: " + curWinRatio );
		}
		else if (parsedCmd.hasOption("windowSize"))
		{
			Integer curWinSize = Integer.valueOf(parsedCmd.getOptionValue("windowSize"));
			params.setCurWinSize(curWinSize);
			params.ModifyCurParamsString("-windowSize: " + curWinSize );
		}
		
		String verifyMsg = params.VerifyParameters();
		if (verifyMsg != null)
		{
			System.out.println("SegParameters.VerifyParameters failed!");
			System.out.println(verifyMsg);
			System.out.println( params.GetCurParamsString() );
		}
		
		System.out.println( params.GetCurParamsString() );
		return params;
		
	}
	
	public SegParameters ParseConfigFile(String configPath) throws Exception
	{
		SegParameters confParams = new SegParameters();
		
		PropertiesConfiguration config = new PropertiesConfiguration(configPath);
		
		if (config.containsKey("run"))
		{
			confParams.setDoRun(true);
			confParams.ModifyCurParamsString("run=true");
		}
		
		if (config.containsKey("inputFile"))
		{
			String inputFilePath = config.getString("inputFile");
			confParams.setInputFilePath(inputFilePath);
			confParams.ModifyCurParamsString("-inputFile: " + inputFilePath);
		}
		else if (config.containsKey("inputDir"))
		{
			String inputDirPath = config.getString("inputDir").toString();
			confParams.setInputDirPath(inputDirPath);
			confParams.ModifyCurParamsString("-inputDir: " + inputDirPath );
		}
		
		
		if (config.containsKey("annotDir"))
		{
			String annotDirPath = config.getString("annotDir").toString();
			confParams.setAnnotDirPath(annotDirPath);
			confParams.ModifyCurParamsString("-annotDir: " + annotDirPath + "\n");
		}
				
		if (config.containsKey("dataSourceType"))
		{
			String dsClass = config.getString("dataSourceType");
			confParams.setTypeOfDataSource(dsClass);
		}
				
		if (config.containsKey("inputDataType"))
		{
			Integer type = config.getInteger("inputDataType", 0 );
			confParams.setInputDataType(type);
		}
				
		String outputDirPath = config.getString("outputDir");	
		confParams.setOutputDirPath(outputDirPath);
		confParams.ModifyCurParamsString("-outputDir: " + outputDirPath);
				
		String resultsFileName = config.getString("resultsFile");
		confParams.setResultsFileName(resultsFileName);
		confParams.ModifyCurParamsString("-resultsFile: " + resultsFileName );
				
			
		ArrayList<Object> corpExt = new ArrayList<Object>();
		corpExt = (ArrayList<Object>) config.getList("corpusExtensions");
		
		for (Object obj: corpExt)
		{
			String ext = obj.toString();
			confParams.addCorpusExt(ext);
			confParams.ModifyCurParamsString("-corpusExtension: " + ext);
		}
		
		ArrayList<Object> inExt = new ArrayList<Object>();
		inExt = (ArrayList<Object>) config.getList("inputExtensions");
		
		for (Object obj : inExt)
		{
			String ext = obj.toString();
			confParams.addInputExt(ext);
			confParams.ModifyCurParamsString("-inputExtension: " + ext);
		}
		
		if (config.containsKey("sparse"))
		{
			confParams.setUseSparseSegmenter(true);
			confParams.ModifyCurParamsString("-sparse");
		}
		
		if (config.containsKey("smoothing"))
		{
			confParams.setUseSmoothing(true);
			confParams.ModifyCurParamsString("-smoothing");
			
			if (config.containsKey("smoothingAlpha"))
			{
				Double parzenAlpha = config.getDouble("smoothingAlpha");
				confParams.setParzenAlpha(parzenAlpha);
				confParams.ModifyCurParamsString("\tparzenAlpha: " + parzenAlpha );
			}
			if (config.containsKey("smoothingWindow"))
			{
				Integer parzenWindowSize = config.getInteger("smoothingWindow", 1);
				confParams.setParzenWindowSize(parzenWindowSize);
				confParams.ModifyCurParamsString("\tparzenWindowSize: " + parzenWindowSize);
			}
		}
				
				
		if (config.containsKey("useSegmentDf"))
		{
			Boolean useSgmDf = config.getBoolean("useSegmentDf", true);
			confParams.setUseSegmentDf(useSgmDf);
			confParams.ModifyCurParamsString("-useSegmentDf " + useSgmDf.toString());
			if (config.containsKey("numTFIDFsegments"))
			{
				Integer numSegments = config.getInteger("numTFIDFsegments", 0);
				confParams.setNumSegments(numSegments);
				confParams.ModifyCurParamsString("\tnumTFIDFsegments: " + numSegments );
			}
		}
				
		if (config.containsKey("preference"))	
		{
			Double curPref = config.getDouble("preference", 0);
			confParams.setCurPref(curPref);
			confParams.ModifyCurParamsString("-preference: " + curPref );
		}
				
		
		if (config.containsKey("damping"))
		{
			Double curDamp = config.getDouble("damping", 0);
			confParams.setCurDamp(curDamp);
			confParams.ModifyCurParamsString("-damping: " + curDamp + "\n");
		}
		
		if (config.containsKey("windowRatio"))
		{
			Double curWinRatio = config.getDouble("windowRatio", 0.3);
			confParams.setCurWinRatio(curWinRatio);
			confParams.ModifyCurParamsString("-windowRatio: " + curWinRatio );
		}
		else if (config.containsKey("windowSize"))
		{
			Integer curWinSize = config.getInteger("windowSize", 0);
			confParams.setCurWinSize(curWinSize);
			confParams.ModifyCurParamsString("-windowSize: " + curWinSize );
		}
				
				String verifyMsg = confParams.VerifyParameters();
				if (verifyMsg != null)
				{
					System.out.println("SegParameters.VerifyParameters failed!");
					System.out.println(verifyMsg);
					System.out.println( confParams.GetCurParamsString() );
				}
		
		
		return confParams;
	}
	
	public   Options ParseCommandLine(String[] args) throws Exception
	
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
			
			
			//rest of the options
			Option prefValue = OptionBuilder.withArgName( "preference" )
			        .hasArg()
			        .withDescription(  "uniform preference values" )
			        .withType(Double.class)
			        .create( "preference" );
			
			Option dampValue = OptionBuilder.withArgName( "damping" )
			        .hasArg()
			        .withDescription(  "damping coefficient for APS" )
			        .withType(Double.class)
			        .create( "damping" );
			
			OptionGroup winSizeGroup = new OptionGroup();
			
			
			Option windowRatio = OptionBuilder.withArgName( "windowRatio" )
			        .hasArg()
			        .withDescription(  "the size of the sliding window expressed as a coefficient. Must be <= 1 " )
			        .withType(Double.class)
			        .create( "windowRatio" );

			Option windowSize = OptionBuilder.withArgName( "windowSize" )
			        .hasArg()
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
			inputGroup.addOption(inputDir);
			inputGroup.addOption(inputFile);
			
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
			        .withDescription(  "value of the smoothing alpha" )
			         .withType(Double.class)
			        .create( "smoothingAlpha" );
			
			Option smoothingWindow = OptionBuilder.withArgName( "smoothingWindow" )
			        .hasArg()
			        .withDescription(  "size of the smoothing window" )
			         .withType(Integer.class)
			        .create( "smoothingWindow" );
			
			Option numTFIDFsegments = OptionBuilder.withArgName( "numTFIDFsegments" )
			        .hasArg()
			        .withDescription(  "number of segemnts for tf.idf computation" )
			         .withType(Integer.class)
			        .create( "numTFIDFsegments" );
			
			//options for tuning
			Option allNumSegm = OptionBuilder.withArgName( "allNumTFIDFSegments" )
			        .hasArgs()
			        .withDescription(  "possible number of tf.ifd segments to use" )
			        .withValueSeparator(',')
			        .withType(String.class)
			        .create( "allNumTFIDFSegments" );
			
			
			allOptions.addOptionGroup(modeGroup);
			allOptions.addOptionGroup(winSizeGroup);
			allOptions.addOptionGroup(inputGroup);
			
			allOptions.addOption(dataSourceType);
			allOptions.addOption(inputDataType);
			
			allOptions.addOption(sparse);
			allOptions.addOption(smoothing);
			allOptions.addOption(useSegmentDf);
			
			allOptions.addOption(prefValue);
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
			
		
			return allOptions;
		
		
		
	}
	
}
