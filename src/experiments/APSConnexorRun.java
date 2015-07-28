package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.aliasi.tokenizer.TokenizerFactory;

import datasources.ConnexorXMLDataSource;
import datasources.ConnexorXMLHandler;
import datasources.ConnexorXMLMultipleAnnotsDS;
import datasources.ConnexorXMLSimpleDS;
import datasources.IGenericDataSource;
import datasources.SentenceTree;
import datasources.SimpleFileDataSource;
import datasources.SimpleFileMultipleAnnotsDataSource;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import evaluation.AbstractEvaluator;
import evaluation.FournierBoundarySimEvaluator;
import evaluation.MultWinDiffEvaluator;
import evaluation.SimpleWinDiffEvaluator;

import segmenter.AbstractAPSegmenterDP;
import segmenter.IMatrix;
import similarity.CorefSimComputer;
import similarity.DfDictionary;
import similarity.GenericCosineSimComputer;
import similarity.GenericDocument;
import similarity.IGenericSimComputer;
import similarity.TokenDictionary;



/**
 * This is the class to run the APS segmenter using input files parsed by Connexor  and using the syntactically motivated similarity metric.
 * @author anna
 *
 */
public class APSConnexorRun extends AbstractAPSRun {
	
	/**
	 * this matrix holds referential similarity between chunks. AbstractAPSRun.simMatrix holds lexical similarity
	 */
	protected IMatrix refSimMatrix = null;
	
	/**
	 * 
	 */
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
	public Double EvaluateDocument(File curInputFile, int dataType,
			AbstractParameterDictionary runParams) throws Exception {
		AbstractParameterDictionary  paramsInUse = null;
		if ( runParams == null)
			paramsInUse = this.curParams;
		else
			paramsInUse = runParams;
		
		Double inputDataType = paramsInUse.GetNumericValue("inputDataType");
		boolean useCorefSim = false; 
		try{
			useCorefSim = paramsInUse.GetBooleanValue("useCorefSim").booleanValue()
					;
		}
		catch (Exception e)
		{}
		
		
		GenericDocument curDoc = this.freqMap.get(curInputFile);
		IGenericDataSource curDS = curDoc.getDataSource();
		
		IGenericDataSource hypoSegmentation = null;
		
		if (inputDataType == IGenericDataSource.CONNEXOR_SIMPLE_DS)
		{
			hypoSegmentation = new ConnexorXMLSimpleDS();		
			hypoSegmentation.LightWeightInit(curDS.GetNumberOfChunks());
		}
		else if (inputDataType == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS)
		{
			hypoSegmentation = new ConnexorXMLMultipleAnnotsDS();
			hypoSegmentation.LightWeightInit(curDS.GetNumberOfChunks());	
		}
		else
		{
			throw (new Exception ("Unknown inputDataType in APSConnexorRun.EvaluateDocument: " + inputDataType));
		}
		
		
		//build the matrix of similarities 
//		
		IGenericSimComputer simComp = BuildSimMatrix(curInputFile, dataType, paramsInUse);
		
		//now build the matrix of referential similarities, using current parameters
		
		Double curPref = paramsInUse.GetNumericValue("preference");
		Double curDamp = paramsInUse.GetNumericValue("damping");
		Boolean sparse = paramsInUse.GetBooleanValue("sparse");
		
		AbstractAPSegmenterDP segmenter = this.CreateAffinityPropagationSegmenter(simComp, 
				curPref,
				curDamp,
				sparse );
		
		if (useCorefSim == true )
		{
			//build the matix of co-referential similarities
			System.out.println("*** STARTING coref sim");
			CorefSimComputer corefSim = this.BuildCorefSimMatrix(paramsInUse, curDoc, simComp);
			segmenter.ModifySimMatrix(corefSim);	
			
		}
		
		segmenter.Run();
		TreeMap<Integer, TreeSet<Integer>> assigns;
		assigns = segmenter.GetNonConflictingAssignments();
		System.out.println(curDS.GetName());
		System.out.println("ref breaks: " + curDS.GetReferenceBreaks(0));
		segmenter.PrintAssignments();
		
		Integer[] hypo = segmenter.GetHypoBreaks(assigns);
		
		ArrayList<Integer> hypoBreaks = new ArrayList<Integer>();
		for (Integer el: hypo)
			hypoBreaks.add(el);
		
		//we use annotID=1 here because there is only one hypo segmentation
		hypoSegmentation.SetReferenceBreaks(1, hypoBreaks);
		
		AbstractEvaluator evaluator;
		
		String evalMetric = paramsInUse.GetStringValue("evalMetric");
		if (evalMetric.compareTo("winDiff") == 0)
		{
			evaluator= new SimpleWinDiffEvaluator();
		}
		else if (paramsInUse.GetStringValue("evalMetric").compareTo("multWDUnnorm") == 0)
		{
			evaluator= new MultWinDiffEvaluator();	
			
		}
		else if (evalMetric.compareTo("multWDNorm") == 0)
		{
			evaluator= new MultWinDiffEvaluator();	
			MultWinDiffEvaluator ev = (MultWinDiffEvaluator)evaluator;
			ev.SetMetricType(MultWinDiffEvaluator.MULTWD_TYPE.MULTWD_NORM);
			evaluator = ev;
			
		}
		else if (evalMetric.compareTo("wdMajority") == 0)
		{
			evaluator= new MultWinDiffEvaluator();	
			MultWinDiffEvaluator ev = (MultWinDiffEvaluator)evaluator;
			ev.SetMetricType(MultWinDiffEvaluator.MULTWD_TYPE.MAJORITY_OPINION);
			evaluator = ev;
			
		}
		else if (evalMetric.compareTo("boundarySim") == 0)
		{
			evaluator= new FournierBoundarySimEvaluator();
			evaluator.Init("boundarySim");
			
		}
		else 
		{
			Exception e2 = new Exception ("Unknown evaluation metric in APSConnexorRun.EvaluateDocument: " + paramsInUse.GetStringValue("evalMetric"));
			throw (e2);
		}
		
		evaluator.SetMetricName(evalMetric);
		
		evaluator.Init(evalMetric);
		evaluator.SetRefDS(curDS);
		evaluator.SetHypoDS(hypoSegmentation);
		if (evaluator.VerifyCompatibility() != AbstractEvaluator.COMPATIBLE )
		{
			String msg = "Exception in APSConnexorRun.EvaluateDocument. Reference and hypo DS are not compatible. evaluator.VerifyCompatibility() failed.";
			throw (new Exception(msg));
		}
		else if (evaluator.SpecificVerifyCompatibility() != AbstractEvaluator.COMPATIBLE )
		{
			String msg = "Exception inAPSConnexorRun.EvaluateDocument. Reference and hypo DS are not compatible. evaluator.SpecificVerifyCompatibility() failed.";
			throw (new Exception(msg));
		}
		
		Double wd = evaluator.ComputeValue();
		System.out.println("Eval result: " +
				"" + wd);
		return wd;
	}
	
	@Override
	public void ComputePreliminaryFrequencies(AbstractParameterDictionary runParams) throws Exception
	{
		AbstractParameterDictionary paramsInUse = null;
		if (runParams != null)
			paramsInUse = runParams;
		else
			paramsInUse = this.curParams;
		
		Double decayFactor = null;
		
		int parThreshold = 1;
		int decayWinSize = 1;
		System.out.println("***APSConnexor.ComputePreliminaryFrequencies");
		
		try{
			for (File curFile: this.inputFiles)
			{
				try{
				ConnexorXMLDataSource curDataSource = null;
				Double segmLevel = this.curParams.GetNumericValue("segmLevel");
				Double inputDataType = paramsInUse.GetNumericValue("inputDataType");
				File inputDir = new File (paramsInUse.GetStringValue("inputDir"));
				String out = paramsInUse.GetStringValue("outputDir");
				File outputDir = new File (out);
				String boundaryMarker = paramsInUse.GetStringValue("segmPattern");
					
				
				if (inputDataType != IGenericDataSource.CONNEXOR_SIMPLE_DS &&
						inputDataType != IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS)
				{
					String inputDT = "null";
					if (inputDataType != null)
					{	inputDT = inputDataType.toString();}
					System.out.println(curFile.getAbsolutePath());
					String msg = "Unknown inputDataType in APSConnexorRun.ComputePreliminaryFrequencies: " + inputDT + " file " + curFile.getAbsolutePath();
					Exception e2 = new Exception ( msg);
					throw (e2);
				}
					
				
				
				String name = curFile.getName();
				
				File outFile = new File(outputDir, name + "_connexor_handler_output.txt");
				File logFile = new File(outputDir, name + "_connexor_handler_log.log");
				File annotDir = null;
				
				Double parThreshDouble = paramsInUse.GetNumericValue("parThreshold");
				Double decayWinDouble = paramsInUse.GetNumericValue("decayWindowSize");
				decayFactor = paramsInUse.GetNumericValue("decayFactor");
				

				parThreshold = parThreshDouble.intValue();
				decayWinSize = decayWinDouble.intValue();
				
				
				XMLReader reader = XMLReaderFactory.createXMLReader();
				reader.setFeature("http://xml.org/sax/features/validation", false);
				reader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
				reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
				
				
				InputSource parsedSource = new InputSource(curFile.getAbsolutePath());
				
				ConnexorXMLHandler assist = new ConnexorXMLHandler();
				
				int lastSentIndex = -1;
				
				if (inputDataType == IGenericDataSource.CONNEXOR_SIMPLE_DS)
				{
					assist.Init( curFile, null, outFile, logFile, segmLevel.intValue(), boundaryMarker, inputDataType.intValue()) ;
					curDataSource = (ConnexorXMLSimpleDS) assist.GetDocumentRepresentation();
					
				}
				else //inputDataType == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS
				{
					annotDir = new File (paramsInUse.GetStringValue("annotDir"));
					File annotFile = ConnexorXMLMultipleAnnotsDS.GetCSVPath(name, annotDir);
					assist.Init( curFile, annotFile, outFile, logFile, segmLevel.intValue(), boundaryMarker, inputDataType.intValue()) ;
					curDataSource = (ConnexorXMLMultipleAnnotsDS) assist.GetDocumentRepresentation();
					
				}
				
				
				
				reader.setContentHandler(assist);
				//System.out.println("registered handler.  for file " + curFile.getName());
				reader.parse(parsedSource);
				//System.out.println("loaded the parse tree complete ");
				
				lastSentIndex  = curDataSource.GetLastSentenceIndex();
				if (this.GetStanfordPipeline() != null)
				{
					for (int i = 0; i <= lastSentIndex; i++)
					{
						SentenceTree curSent = curDataSource.GetSentence(i);
						curSent.FindNamedEntities(stanfordPipeline);
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
				
				this.freqMap.put(curFile, curDoc);
				}
				catch(Exception e)
				{
					System.out.println("Could not precompute frequencies for file " + curFile.getAbsolutePath());
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}//end looping over files

		}
		catch (Exception e)
		{
			String msg = "APSConnexorRun: Could not precompute frequencies  " ;
			System.out.println(msg);
			System.out.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	
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
	

}
