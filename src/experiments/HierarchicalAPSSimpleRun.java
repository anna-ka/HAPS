package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.aliasi.tokenizer.TokenizerFactory;

import datasources.ConnexorXMLDataSource;
import datasources.ConnexorXMLHandler;
import datasources.ConnexorXMLMultipleAnnotsDS;
import datasources.ConnexorXMLSimpleDS;
import datasources.HierarchicalMultipleAnnotsDataSource;
import datasources.IGenericDataSource;
import datasources.SentenceTree;
import datasources.SimpleFileDataSource;
import datasources.TextFileIO;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import evaluation.HierEvalHDSEvaluator;
import evaluation.HierMultAnnotEvaluator;

import segmenter.HierAffinityPropagationSegmenter;
import similarity.CorefSimComputer;
import similarity.DfDictionary;
import similarity.GenericDocument;
import similarity.IGenericSimComputer;
import similarity.TokenDictionary;

/**
 * a class to handle hierarchical runs of SimpleFileDataSource
 * @author anna
 *
 */
public class HierarchicalAPSSimpleRun extends AbstractAPSRun {
	
	
	StanfordCoreNLP stanfordPipeline = null;
	
	
	public StanfordCoreNLP GetStanfordPipeline()
	{
		return this.stanfordPipeline;
	}
	
	
	public void SetStanfordPipeline(StanfordCoreNLP pipeline)
	{
		this.stanfordPipeline = pipeline;
	}
	

	@Override
	public Double EvaluateDocument(File inputFile, int dataType,
			AbstractParameterDictionary runParams) throws Exception {
		
		AbstractParameterDictionary  paramsInUse = null;
		if ( runParams == null)
			paramsInUse = this.curParams;
		else
			paramsInUse = runParams;
		
		
		int numLayers = 3;
		double dampFact = 0.9;
		int maxIterations = 1000;
		int maxConvergence = 100;
		
		boolean useCorefSim = false;
		try{
			useCorefSim = paramsInUse.GetBooleanValue("useCorefSim").booleanValue();
		}
		catch (Exception e)
		{
			
		}
		
		Double inputDataType = paramsInUse.GetNumericValue("inputDataType");
		//File outputDir = new File (paramsInUse.GetStringValue("outputDir"));
		
		Double pref0 = paramsInUse.GetNumericValue("preference_level0");
		Double pref1 = paramsInUse.GetNumericValue("preference_level1");
		Double pref2 = paramsInUse.GetNumericValue("preference_level2");
		
//		Boolean useWDPerLevel = new Boolean(false);
//		try{
//			useWDPerLevel = paramsInUse.GetBooleanValue("useWDPerLevel");
//		}
//		catch (Exception e)
//		{}
		
		GenericDocument curDoc = this.GetFreqMap().get(inputFile);
		IGenericDataSource refDS = curDoc.getDataSource();
		
		int numChunks = refDS.GetNumberOfChunks();
		IGenericSimComputer simComp;
		
//		if (inputDataType.intValue() == IGenericDataSource.CONNEXOR_SIMPLE_DS || inputDataType.intValue() == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS)
//			simComp = this.BuildConexorMatrix(inputFile, inputDataType.intValue(), runParams);
//		else
		simComp = this.BuildSimMatrix(inputFile, inputDataType.intValue(), paramsInUse);
		
		
		HierAffinityPropagationSegmenter haps = new HierAffinityPropagationSegmenter();
		haps.Init(numChunks, numLayers, dampFact);
		haps.SetMaxIterations(maxIterations);
		haps.SetMaxConvCount( maxConvergence);
		
		//set similarities
		haps.SetUniformSims(simComp);
		
		//set preferences
//		haps.SetUniformPrefsForLayer(0, -0.5);
//		haps.SetUniformPrefsForLayer(1, -2);
//		haps.SetUniformPrefsForLayer(2, -4);
		
		haps.SetUniformPrefsForLayer(0, pref0);
		haps.SetUniformPrefsForLayer(1, pref1);
		haps.SetUniformPrefsForLayer(2, pref2);
		
		if (useCorefSim == true)
		{
			String connexorDir = paramsInUse.GetStringValue("connexorDir");
			File connexorInputFile = new File(connexorDir, refDS.GetName() + ".xml");
			curDoc =this.GetConnexorDoc(connexorInputFile,  paramsInUse);
			IGenericSimComputer corefComp = this.BuildCorefSimMatrix(paramsInUse, curDoc, simComp);
			haps.ModifyMatrix(corefComp);
		}
		
		//run
		haps.Run();
//		haps.RunGivoniSchedule();
		TreeMap<Integer, ArrayList<Integer>> results = haps.GetAllHypoBreaksWithConflictsResolved();
		
		//evaluate
		
		
		
		SimpleFileDataSource hypoDS = new SimpleFileDataSource();
		hypoDS.SetIfHierarchical(true);
		hypoDS.LightWeightInit(refDS.GetNumberOfChunks());
		TreeMap <Integer, ArrayList<Integer>> formattedResults = hypoDS.HAPSBreaksToCarrolStyle(results);
		
		hypoDS.SetReferenceBreaksHierarchical(formattedResults);
		
		
		
		Double res ;
		
		if (inputDataType.intValue() == IGenericDataSource.SIMPLE_DS)
			res = this.EvaluateSimpleDS(refDS, hypoDS, paramsInUse);
		else if (inputDataType.intValue() == IGenericDataSource.HIER_MULTIPLE_ANNOTS_DS)
			res = this.EvaluateHierarchicalMultipleAnnotsDataSource(refDS, hypoDS, paramsInUse);
		else
		{
			throw (new Exception("INvalid inputDataType: " + inputDataType + " for " + refDS.GetName()));
		}
		
		return (res);
		
		
	}
	
	
	public GenericDocument GetConnexorDoc(  File inputFile,  AbstractParameterDictionary runParams) throws Exception
	{
		AbstractParameterDictionary paramsInUse = null;
		if (runParams != null)
			paramsInUse = runParams;
		else
			paramsInUse = this.curParams;
		
		Double decayFactor = null;
		
		int parThreshold = 1;
		int decayWinSize = 1;
		
		Double inputDataType = paramsInUse.GetNumericValue("inputDataType");
		File inputDir = new File (paramsInUse.GetStringValue("inputDir"));
		String out = paramsInUse.GetStringValue("outputDir");
		File outputDir = new File (out);
		String boundaryMarker = paramsInUse.GetStringValue("segmPattern");
		Double segmLevel = this.curParams.GetNumericValue("segmLevel");
		
		String connexorDir = runParams.GetStringValue("connexorDir");
		ConnexorXMLDataSource curDataSource = null;
		
		File outFile = new File(outputDir, inputFile.getName() + "_connexor_handler_output.txt");
		File logFile = new File(outputDir, inputFile.getName() + "_connexor_handler_log.log");
		
		Double parThreshDouble = paramsInUse.GetNumericValue("parThreshold");
		Double decayWinDouble = paramsInUse.GetNumericValue("decayWindowSize");
		decayFactor = paramsInUse.GetNumericValue("decayFactor");
		

		parThreshold = parThreshDouble.intValue();
		decayWinSize = decayWinDouble.intValue();
		
		//parThreshold, decayWinSize,  decayFactor
		
		String name = inputFile.getName();
		
		File annotDir = null;
		
		
		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setFeature("http://xml.org/sax/features/validation", false);
		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		
		InputSource parsedSource = new InputSource(inputFile.getAbsolutePath());
		
		ConnexorXMLHandler assist = new ConnexorXMLHandler();
		assist.Init( inputFile, null, outFile, logFile, segmLevel.intValue(), boundaryMarker, inputDataType.intValue()) ;
		curDataSource = (ConnexorXMLSimpleDS) assist.GetDocumentRepresentation();
		
//		if (inputDataType == IGenericDataSource.CONNEXOR_SIMPLE_DS)
//		{
//			
//		}
//		else //inputDataType == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS
//		{
//			annotDir = new File (paramsInUse.GetStringValue("annotDir"));
//			File annotFile = ConnexorXMLMultipleAnnotsDS.GetCSVPath(name, annotDir);
//			assist.Init( inputFile, annotFile, outFile, logFile, segmLevel.intValue(), boundaryMarker, inputDataType.intValue()) ;
//			curDataSource = (ConnexorXMLMultipleAnnotsDS) assist.GetDocumentRepresentation();
//		}
		
		
		
		reader.setContentHandler(assist);
		//System.out.println("registered handler.  for file " + curFile.getName());
		
		reader.parse(parsedSource);
		//System.out.println("loaded the parse tree complete ");
		
		int lastSentIndex  = curDataSource.GetLastSentenceIndex();
		if (this.GetStanfordPipeline() != null)
		{
			for (int i = 0; i <= lastSentIndex; i++)
			{
				SentenceTree curSent = curDataSource.GetSentence(i);
//				curSent.FindNamedEntities(stanfordPipeline);
			}
		}
		
		TokenizerFactory tokenizerFactory = this.CreateTokenizerFactory(paramsInUse);
		TokenDictionary tokenDict;
		int numDocs; // number of documents in the corpus
		int numSegm; // number of segments to ue in segment tf.idf
		GenericDocument curDoc;
		
		//
		if (paramsInUse.GetBooleanValue("useSegmentDf").booleanValue() == false)
		{
			tokenDict = this.GetDfDictionary().GetTokenDictionary();
			numDocs = this.GetDfDictionary().GetNumDocuments();
			numSegm = 1;
			curDoc = new GenericDocument(tokenDict, numDocs, curDataSource, numSegm);
			curDoc.setDocFreqs(this.GetDfDictionary());
		}
		else
		{
			numSegm = paramsInUse.GetNumericValue("numTFIDFsegments").intValue();
			numDocs = 1;
			tokenDict = new TokenDictionary(curDataSource, tokenizerFactory);
			tokenDict.ProcessText();
			IGenericDataSource[] corp  = {curDataSource};
			DfDictionary segmentDf = new DfDictionary (corp, tokenizerFactory, numSegm );
			segmentDf.ProcessCorpus();
			
			curDoc = new GenericDocument(tokenDict, numDocs, curDataSource, numSegm);
			curDoc.setDocFreqs(segmentDf);
			
		}
		
		if ( paramsInUse.GetBooleanValue("useWeightedTf").booleanValue() == true)
		{
			curDoc.SetUseWeightedTfIdf(true);
		}
		
		curDoc.SetParametersForSyntacticVectors(parThreshold, decayWinSize,  decayFactor);
		
		
		curDoc.Init();
		curDoc.ComputeTfIdf();
		
		
		
		return curDoc;
	}
	
	
	
//	public GenericDocument GetConnexorDoc(File inputFile, int dataType, AbstractParameterDictionary runParams)
//	{
//		
//	}
	
	public CorefSimComputer BuildCorefSimMatrix(AbstractParameterDictionary runParams, 
			GenericDocument curDoc,
			IGenericSimComputer lexSimComputer) throws Exception
	{
		AbstractParameterDictionary  paramsInUse = null;
		if ( runParams == null)
			paramsInUse = this.curParams;
		else
			paramsInUse = runParams;
		
		//needed params: Double decayFactor, int decayWinSize,
		Double decayFactor = paramsInUse.GetNumericValue("decayFactor");
		Double decayWinSize = paramsInUse.GetNumericValue("decayWindowSize");
		
		Double lexWinRatio = paramsInUse.GetNumericValue("windowRatio");
		int lexWindowSize = super.ComputeSlidingWindow(curDoc.getDataSource(), lexWinRatio);
		
		if (decayWinSize > lexWindowSize)
		{
			System.out.println("APSCOnnexorRun.BuildCorefSimMatrix: decayWInSize " + decayWinSize.toString() + "is too large");
			System.out.println("\tlexWIndowSize is " + lexWindowSize);
			decayWinSize = new Double (lexWindowSize);
		}
		
		CorefSimComputer corefComputer = new CorefSimComputer();
		corefComputer.Init(curDoc.getDataSource());
		corefComputer.SetUp(curDoc, decayFactor, decayWinSize.intValue(), lexSimComputer);
		corefComputer.ComputeSimilarities();
		
		System.out.println("mean coref sim " + corefComputer.GetMeanSim());
		System.out.println("max coref sim " + corefComputer.GetMaxSim());
		
		return corefComputer;
	}
	
	public Double EvaluateSimpleDS(IGenericDataSource refDS, IGenericDataSource hypoDS, AbstractParameterDictionary paramsInUse) throws Exception
	{
		System.out.println("Ref breaks:");
		System.out.println ( ((SimpleFileDataSource )refDS).PrintRefBreaks() );
		System.out.println("Hypo breaks:");
		System.out.println( ((SimpleFileDataSource )hypoDS).PrintRefBreaks() );
		
		File outputDir = new File (paramsInUse.GetStringValue("outputDir"));
		Boolean useWDPerLevel = new Boolean(false);
		try{
			useWDPerLevel = paramsInUse.GetBooleanValue("useWDPerLevel");
		}
		catch (Exception e)
		{}
		
		HierEvalHDSEvaluator eval = new HierEvalHDSEvaluator();
		eval.Init("evalHDS");
		
		eval.SetRefDS(refDS);
		eval.SetHypoDS(hypoDS);
		eval.SetUp(new File (outputDir, "tmp"), "HAPS");
		
		if (useWDPerLevel != null && useWDPerLevel.booleanValue() == true)
		{
			eval.SetUseWD(true);
		}
		
		
		Double result = eval.ComputeValue();
		System.out.println("EVALHDS for " + refDS.GetName() + " is: " + result);
		
		//"wdPerLevelPath"
		String wdPath = null;
		try{
			wdPath = paramsInUse.GetStringValue("wdPerLevelPath");
		}
		catch(Exception e)
		{
			
		}
		
		
		if (wdPath != null)
		{
			TreeMap<Integer, Double> map = eval.GetWinDiffPerLayer();
			String out = refDS.GetName() + "," + map.get(2) + "," + map.get(3) + "," + map.get(4) + "\n";
			File wdFile = new File(wdPath);
			
//			if (wdFile.exists())
//				wdFile.delete();
			
			if (wdFile.exists() == false)
				wdFile.createNewFile();
			TextFileIO.AppendToFile(wdFile, out);
		}
		
		return result;
	}
	
	public Double EvaluateHierarchicalMultipleAnnotsDataSource(IGenericDataSource ref, IGenericDataSource hypo, AbstractParameterDictionary paramsInUse) throws Exception
	{
		System.out.println("Ref breaks:");
		HierarchicalMultipleAnnotsDataSource r = (HierarchicalMultipleAnnotsDataSource)ref;
		for (Integer a: r.GetAnnotatorIds())
		{
			System.out.println(r.PrintRefBreaks(a));
		}
		
		
		System.out.println("Hypo breaks:");
		System.out.println( ((SimpleFileDataSource )hypo).GetReferenceBreaksHierarchical() );	
		
		
		File outputDir = new File (paramsInUse.GetStringValue("outputDir"));
		Boolean useWDPerLevel = new Boolean(false);
		try{
			useWDPerLevel = paramsInUse.GetBooleanValue("useWDPerLevel");
		}
		catch (Exception e)
		{}
		
		
		
		HierMultAnnotEvaluator eval = new HierMultAnnotEvaluator();
		if (useWDPerLevel != null && useWDPerLevel.booleanValue() == true)
		{
			eval.SetUseWD(true);
		}
		eval.Init("aveEvalHDS");
		eval.SetRefDS(ref);
		eval.SetHypoDS(hypo);

		File tmpDir = new File(outputDir, "tmp");
		eval.SetUp(tmpDir, "HAPS_mult");
		
		Double val = eval.ComputeValue();
		System.out.println("ave value: " + val);
		
		//"wdPerLevelPath"
		String wdPath = null;
		try{
			wdPath = paramsInUse.GetStringValue("wdPerLevelPath");
		}
		catch(Exception e)
		{
			
		}
		
		
		if (wdPath != null)
		{
			TreeMap<Integer, Double> map = eval.GetAverageWDPerLevel();
			String out = ref.GetName() + "," + map.get(2) + "," + map.get(3) + "," + map.get(4) + "\n";
			File wdFile = new File(wdPath);
//			if (wdFile.exists())
//				wdFile.delete();
			
			if (wdFile.exists() == false)
				wdFile.createNewFile();
			TextFileIO.AppendToFile(wdFile, out);
		}
		
		return val;
	}

}
