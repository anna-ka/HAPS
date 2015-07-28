package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.RegExFilteredTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import evaluation.EvalResultSet;


import segmenter.AbstractAPSegmenterDP;
import segmenter.AffinityPropagationSegmenterDense;
import segmenter.AffinityPropagationSegmenterSparse;
import segmenter.IMatrix;
import segmenter.SegFileFilter;

import similarity.CosineSimComputer;
import similarity.DfDictionary;
import similarity.Document;
import similarity.GenericCosineSimComputer;
import similarity.GenericDocument;
import similarity.IGenericSimComputer;
import similarity.ISimComputer;
//import similarity.STSSimComputer;
import similarity.TokenDictionary;
//import similarity.WordNetSimComputer;

import datasources.IGenericDataSource;

import datasources.ConnexorXMLDataSource;
import datasources.ConnexorXMLHandler;
import datasources.ConnexorXMLMultipleAnnotsDS;
import datasources.ConnexorXMLSimpleDS;
import datasources.HierarchicalMultipleAnnotsDataSource;
import datasources.IDataSource;
import datasources.SimpleFileDataSource;
import datasources.SimpleFileMultipleAnnotsDataSource;
import datasources.TextFileIO;

//import dkpro.similarity.algorithms.lsr.LexSemResourceComparator;

/**
 * A class to hold parameters and results of a single run of APS segmenter on a set of files
 * @author anna
 *
 */
public abstract class AbstractAPSRun implements IRun {
	
	public final static int SimpleDS = 0;
	public final static int multAnnotDS = 1;
	
	
	/**
	 * matrix of lexical similarity between chunks
	 */
	protected IMatrix simMatrix = null;
	
	
	/**
	 * matrix of coreferential similarities between chunks
	 */
	protected IMatrix corefMatrix = null;
	
	protected DfDictionary dfDict = null;
	protected File[] inputFiles = null;
	protected File[] dfFiles = null;
	protected AbstractParameterDictionary curParams;
	/**
	 * a map to keep a pre-computed GenericDocument representations for each of the input files
	 */
	protected TreeMap<File, GenericDocument> freqMap = new TreeMap<File, GenericDocument>();
	
	/**
	 * an initialized instance of LexSemResourceComparator from DKSimPro
	 */
	//protected LexSemResourceComparator lexSemResourceComparator = null;
	
	/**
	 * set this variable to false if this run does not require a premade table of File->GenericDocument representations for all input file
	 */
	protected boolean ifPrecomputeFreqInInit = true;
	
	
	
	/**
	 * A method that initialzies the run with necessary parameters
	 */
	public void Init(AbstractParameterDictionary params) throws Exception
	{
		this.curParams = params;
		this.SetInputFiles(null);
		
		//set up inverse document frequences dictionary
		DfDictionary dfDictionary = null;
		if (this.dfDict == null)
		{
			if ( this.curParams.GetBooleanValue("useSegmentDf").booleanValue() == false  )
				//&& this.dfDict == null)
			{
				
				this.dfFiles = this.GetDfFilesFromParameters(null);
				//process corpus for Df - inverse document frequencies
				dfDictionary = this.ProcessDfCorpus(this.dfFiles, null);
				this.dfDict = dfDictionary;
			}
			// else we use segment df which is computed on per document basis
			
		}
		
		
		if (this.ifPrecomputeFreqInInit == true)
		{
			//compute preliminary doc frequencies
			this.ComputePreliminaryFrequencies(null);
		}
		
	}
	
	
//	/**
//	 * return the copy of DKProSim comparator that will be used
//	 * @return 
//	 */
//	public LexSemResourceComparator GetLSRComparator()
//	{
//		return this.lexSemResourceComparator;
//	}
//	
//	/**
//	 * sets lexSemResourceComparator to an instance that will be used in computations of similarity
//	 * @param comparator an initialized instance of LexSemResourceComparator, e.g. ResnikComparator
//	 * 
//	 */
//	public void SetLSRComparator(LexSemResourceComparator comparator)
//	{
//		this.lexSemResourceComparator = comparator;
//	}
//	
	
	
	/**
	 * This method allows to specify dfDictionary externally, but only when the corpus consists of a single file. 
	 * It may be useful for optimizing the training time.
	 * @param dict dictionary of inverse document frequecies for the computation of tf.idf
	 */
	public void SetDfDictionary(DfDictionary dict) throws Exception
	{
		if (this.inputFiles.length != 1)
		{
			String msg = "Exception in AbstractAPSRun.SetDfDictionary(). Cannot set DfDict since the number of input files != 1.";
			for (File f: this.inputFiles)
			{
				msg = msg + "\n\tfile: " + f.getName();
			}
			throw (new Exception(msg));
		}
		this.dfDict = dict;	
	}
	
	public DfDictionary GetDfDictionary()
	{
		return this.dfDict;
	}
	
	/**
	 * This method should only be used in special cases when we are optimizing parameters of the actual segmenter on a  single file
	 * @param matrix the actual similarities matrix to be used AbstractAPSSegmenterDP
	 */
	public void SetSimMatrix(IMatrix matrix) throws Exception
	{
		if (this.inputFiles.length != 1)
		{
			String msg = "Exception in AbstractAPSRun.SetSimMatrix(). Cannot set sim matrix since the number of input files != 1.";
			for (File f: this.inputFiles)
			{
				msg = msg + "\n\tfile: " + f.getName();
			}
			throw (new Exception(msg));
		}
		this.simMatrix = matrix;
	}
	
	/**
	 *  This method should only be used in special cases when we are optimizing parameters of the actual segmenter on a  single file
	 * @return
	 */
	public IMatrix GetSimMatrix() throws Exception
	{
		if (this.inputFiles.length != 1)
		{
			String msg = "Exception in AbstractAPSRun.GetSimMatrix(). Cannot get sim matrix since the number of input files != 1.";
			for (File f: this.inputFiles)
			{
				msg = msg + "\n\tfile: " + f.getName();
			}
			throw (new Exception(msg));
		}
		
		return this.simMatrix;
	}
	
	
	public File[] GetInputFiles()
	{
		return this.inputFiles;
	}
	
	
	/**
	 * @param inputFile a set of files on which the segmenter should be run
	 */
	public void SetInputFiles(File[] inputFiles) throws Exception
	{
		if (inputFiles != null)
		{
			this.inputFiles = inputFiles;
			return;
		}
		//otherwise set input files from the specified parameters
		String inputDirPath = null;
		try{
			inputDirPath = this.curParams.GetStringValue("inputDir");
		}
		catch (Exception e)
		{}
		
		File[] tmpFiles;
		if (inputDirPath != null)
		{
			File inputDirFile = new File(inputDirPath);
			String[] inputExt = new String[this.curParams.GetStringList("inputExtensions").size()];
			inputExt = this.curParams.GetStringList("inputExtensions").toArray(inputExt);
			tmpFiles= inputDirFile.listFiles(new SegFileFilter(inputExt));
		}
		else //try inputFile parameter
		{
			File inFile = new File (this.curParams.GetStringValue("inputFile"));
			tmpFiles = new File[1];
			tmpFiles[0] = inFile;
		}
		this.inputFiles = tmpFiles;
	}
	
	/**
	 * @param inputFile a set of files on which the segmenter should be run
	 */
	public void SetDfFiles(File[] dfFiles)
	{
		this.dfFiles = dfFiles;
	}
	
	public File[] GetDfFilesFromParameters(AbstractParameterDictionary runParams) throws Exception
	{
		AbstractParameterDictionary paramsInUse = null;
		if (runParams != null)
			paramsInUse = runParams;
		else
			paramsInUse = this.curParams;
		
		String path = paramsInUse.GetStringValue("inputDir");
		File inDir = new File (path);
		
		ArrayList<String> corExt = paramsInUse.GetStringList("corpusExtensions");
		String[] corpusExtensions = new String[corExt.size()];
		corpusExtensions = corExt.toArray(corpusExtensions);
		
		File[] dfFiles= inDir.listFiles(new SegFileFilter(corpusExtensions));
		return dfFiles;
	}
	
	/**
	 * A method to create a dictionary of inverse document frequencies to be use for computation of tf.idf
	 * @param dfFiles a set of documents that will be used to compute tf.idf
	 * @return DfDictionary of inverse document frequencies
	 * @throws Exception
	 */
	private DfDictionary ProcessDfCorpus(File[] dfFiles, AbstractParameterDictionary runParams) throws Exception
	{
		
		AbstractParameterDictionary paramsInUse = null;
		if (runParams != null)
			paramsInUse = runParams;
		else
			paramsInUse = this.curParams;
		
		TokenizerFactory tokenizerFactory = this.CreateTokenizerFactory(paramsInUse);
		
		Double inputDataType = paramsInUse.GetNumericValue("inputDataType");
		Double segmLevel = paramsInUse.GetNumericValue("segmLevel");
		String segmPattern = paramsInUse.GetStringValue("segmPattern");
		File inputDir = new File (paramsInUse.GetStringValue("inputDir"));
		File outputDir = new File (paramsInUse.GetStringValue("outputDir"));
		
		IGenericDataSource[] corpus = new IGenericDataSource[dfFiles.length];
		
		//SimpleFileDataSource[] corpus = new SimpleFileDataSource[dfFiles.length];
		for (int i = 0; i < dfFiles.length; i++)
		{
			File cFile = dfFiles[i];
			try
			{
				//SimpleFileDataSource or SimpleFileMultipleAnnotsDS
				if (inputDataType == IGenericDataSource.SIMPLE_DS || inputDataType == IGenericDataSource.SIMPLE_MULTIPLE_ANNOTS_DS ||
						inputDataType == IGenericDataSource.CONNEXOR_SIMPLE_DS || inputDataType == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS ||
						inputDataType == IGenericDataSource.HIER_MULTIPLE_ANNOTS_DS)
				{
					corpus[i] =  new SimpleFileDataSource();
					if (paramsInUse.GetBooleanValue("useHier").booleanValue() == true)
						corpus[i].SetIfHierarchical(true);
					corpus[i].Init(segmLevel.intValue(), segmPattern, cFile, null);
				}
				else if (inputDataType == IGenericDataSource.CONNEXOR_SIMPLE_DS || inputDataType == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS)
				{
					File outFile = new File (outputDir, "out" + cFile.getName());
					File logFile = new File (outputDir, "log" + cFile.getName());
					XMLReader reader = ConnexorXMLDataSource.CreateXMLReader();
					InputSource parsedSource = new InputSource(cFile.getAbsolutePath());
					
					ConnexorXMLHandler assist = new ConnexorXMLHandler();
					assist.Init( cFile, null, outFile, logFile, segmLevel.intValue(), segmPattern, IGenericDataSource.CONNEXOR_SIMPLE_DS) ;
					
					reader.setContentHandler(assist);
					reader.parse(parsedSource);
					ConnexorXMLSimpleDS curDS  = (ConnexorXMLSimpleDS) assist.GetDocumentRepresentation();
					corpus[i] = curDS;
				}
				else
				{
					throw (new Exception("Invalid InputDataType: " + inputDataType));
				}
				
//				corpus[i] =  new SimpleFileDataSource();
//				String segmPattern = paramsInUse.GetStringValue("segmPattern");
//				Double segmLevel = paramsInUse.GetNumericValue("segmLevel");
//				corpus[i].Initialize(cFile, segmPattern);
//				corpus[i].Init(segmLevel.intValue());
				
			}
			catch(Exception e)
			{
				System.out.println("Exception in AbstractAPSRun.ProcessDfCorpus:\n");
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		
		//create DF dictionary
		DfDictionary dfDict = new DfDictionary(corpus, tokenizerFactory, corpus.length);
		int numDocs = corpus.length;
		try 
		{
			dfDict.ProcessCorpus();
		} 
		catch (Exception e1) {
			// TODO Auto-generated catch block
			System.out.println("Exception in AbstractAPSRun.ProcessDfCorpus when processing processing the corpus");
			e1.printStackTrace();
		}
		
		return dfDict;
		
	}
	
	/**
	 * This method precomputes and stores raw tf.idf vectors for each of the input files
	 */
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
		
		boolean useHierarchicalVersion = false;
		
		try{
			useHierarchicalVersion = paramsInUse.GetBooleanValue("useHier").booleanValue();
		}
		catch (Exception e)
		{}
		
		for (File curFile: this.inputFiles)
		{
			IGenericDataSource curDataSource = null;
			
			Double segmLevel = this.curParams.GetNumericValue("segmLevel");
			
			
			try{
				
				Double inputDataType = paramsInUse.GetNumericValue("inputDataType");
				File inputDir = new File (paramsInUse.GetStringValue("inputDir"));
				String out = paramsInUse.GetStringValue("outputDir");
				File outputDir = new File (out);
				String boundaryMarker = paramsInUse.GetStringValue("segmPattern");
				
				
				//SimpleFileDataSOurce
				if (inputDataType == IGenericDataSource.SIMPLE_DS)
				{
					curDataSource = new SimpleFileDataSource();
					
					if (useHierarchicalVersion == true)
						curDataSource.SetIfHierarchical(useHierarchicalVersion);
					
					
					curDataSource.Init(segmLevel.intValue(), paramsInUse.GetStringValue("segmPattern"), curFile, null);
					
					if (useHierarchicalVersion == true)
					{
						SimpleFileDataSource ds = (SimpleFileDataSource) curDataSource;
						ds.TransformCarrolsBreaks();
					}
				}
				//SimpleFileMultipleDataSource
				else if (inputDataType == IGenericDataSource.SIMPLE_MULTIPLE_ANNOTS_DS)
				{
					
					String textFileName = curFile.getName();
					String annotName = "";
					Pattern annotExtensionPattern = Pattern.compile("\\.[a-z\\.]+");
					Matcher m = annotExtensionPattern.matcher(textFileName);
					if (m != null)
					{
						annotName = m.replaceAll(".csv");
					}
					else
					{
						System.out.println("WARNING in AbstractAPSRUN.ComputePreliminaryFrequencies: failed to generate and annotFile name for text " + textFileName);
					}
					File annotationsFile = new File( paramsInUse.GetStringValue("annotDir"), annotName);
					
					curDataSource = new SimpleFileMultipleAnnotsDataSource();
					curDataSource.Init(segmLevel.intValue(), paramsInUse.GetStringValue("segmPattern"), curFile, annotationsFile);
				}
				//hierarchical data source with multiple annotations
				else if (inputDataType == IGenericDataSource.HIER_MULTIPLE_ANNOTS_DS)
				{
					File annotDir = new File(paramsInUse.GetStringValue("annotDir"));
					String annotName = this.GetShortName(curFile.getName());
					File curAnnotFile = new File(annotDir, annotName);
					
					//HierarchicalMultipleAnnotsDataSource hierDS 
					curDataSource = new HierarchicalMultipleAnnotsDataSource();
					curDataSource.Init(segmLevel.intValue(), "", curFile, curAnnotFile );
				}
				else if (inputDataType == IGenericDataSource.CONNEXOR_SIMPLE_DS ||
						inputDataType == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS)
				{
					
					File outFile = new File(outputDir, curFile.getName() + "_connexor_handler_output.txt");
					File logFile = new File(outputDir, curFile.getName() + "_connexor_handler_log.log");
					
					Double parThreshDouble = paramsInUse.GetNumericValue("parThreshold");
					Double decayWinDouble = paramsInUse.GetNumericValue("decayWindowSize");
					decayFactor = paramsInUse.GetNumericValue("decayFactor");
					

					parThreshold = parThreshDouble.intValue();
					decayWinSize = decayWinDouble.intValue();
					
					//parThreshold, decayWinSize,  decayFactor
					
					String name = curFile.getName();
					
					File annotDir = null;
					
					
					XMLReader reader = XMLReaderFactory.createXMLReader();
					reader.setFeature("http://xml.org/sax/features/validation", false);
					reader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
					reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
					
					
					InputSource parsedSource = new InputSource(curFile.getAbsolutePath());
					
					ConnexorXMLHandler assist = new ConnexorXMLHandler();
					
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
					
					
				}
				
				//unknown data type
				else 
				{
					String inputDT = "null";
					if (inputDataType != null)
						inputDT = inputDataType.toString();
					Exception e2 = new Exception ("Unknown inputDataType in AbstractAPSRun.ComputePreliminaryFrequencies: " + inputDT);
					throw (e2);
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
				
				if (inputDataType == IGenericDataSource.CONNEXOR_SIMPLE_DS || inputDataType == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS)
				{
					curDoc.SetParametersForSyntacticVectors(parThreshold, decayWinSize,  decayFactor);
				}
				
				curDoc.Init();
				curDoc.ComputeTfIdf();
				
				this.freqMap.put(curFile, curDoc);
				
			}
			catch (Exception e)
			{
				String msg = "Could not precompute frequencies for File " + curFile.getAbsolutePath();
				System.out.println(msg);
				System.out.println(e.getLocalizedMessage());
				e.printStackTrace();
			}
			
		}
		
	}
	
	/**
	 * Do not use this method for now, it had a serious bug
	 * @param curDataSource
	 * @return
	 */
	private DfDictionary ProcessSegmentDf(IGenericDataSource curDataSource)
	{
		return null;
	}
	
	public TokenizerFactory CreateTokenizerFactory( AbstractParameterDictionary runParams) throws Exception
	{
		AbstractParameterDictionary paramsInUse = null;
		if (runParams != null)
			paramsInUse = runParams;
		else
			paramsInUse = this.curParams;
		
		//initialize the tokenizer and create the TokenDictionary
		TokenizerFactory tokenizerFactory;
		HashSet<String> stopWords = new HashSet<String>();
		
		//read in stop words
		File stopWordsFile = new File(paramsInUse.GetStringValue("stopWordsFile"));
		String str = TextFileIO.ReadTextFile(stopWordsFile);
		String[] sWords = TextFileIO.LinesToArray(str);
		for (int j = 0; j < sWords.length; j++)
		{
			String word = sWords[j].trim();
			if (word.isEmpty())
				continue;
			stopWords.add(word);
		}
		

		//intialize the appropriate tokenizer
		String regex = "[a-zA-Z]{2,}|[0-9]+|\\S{2,}";
		
		//pattern to remove punctuation
		Pattern punctRegex = Pattern.compile("[\\_]+|[^\\W]+");
		
	    tokenizerFactory = new RegExTokenizerFactory(regex);
	    if (paramsInUse.GetBooleanValue("useLowerCase").booleanValue() == true)
	    {
	    	tokenizerFactory = new LowerCaseTokenizerFactory(tokenizerFactory, new Locale("en"));
	    }
	    if (paramsInUse.GetBooleanValue("useStemmer").booleanValue()   == true)
	    {
	    	tokenizerFactory = new PorterStemmerTokenizerFactory(tokenizerFactory);
	    }
	    if (paramsInUse.GetBooleanValue("removeStopWords").booleanValue() == true)
	    {
	    	tokenizerFactory = new StopTokenizerFactory(tokenizerFactory , stopWords) ;
	    }
	    
	    //filter out punctuation
	    tokenizerFactory = new RegExFilteredTokenizerFactory(tokenizerFactory, punctRegex);
	    return tokenizerFactory;
	}

	@Override
	public void Run() {
		// TODO Auto-generated method stub

	}
	
	

	/**
	 * This method runs the segmenter with specified parameters on the input files
	 * @return EvalResultSet file-Double mapping of results per file
	 */
	public EvalResultSet Evaluate( AbstractParameterDictionary runParams) throws Exception{
		
		AbstractParameterDictionary paramsInUse = null;
		if (runParams != null)
			paramsInUse = runParams;
		else
			paramsInUse = this.curParams;
		
		EvalResultSet results = new EvalResultSet();
		
		results.Init(this.inputFiles);
		
		//loop through files and run
		for (File curFile: inputFiles)
		{
			try{
				Double inputDataType = paramsInUse.GetNumericValue("inputDataType");
				Double value = this.EvaluateDocument( curFile, inputDataType.intValue(), runParams);
				
				//this was a mistake, most likely because SegEval failed
				if (value < 0)
				{
					System.out.println("Warning, failed to compute the result for file" + curFile.getAbsolutePath());
				}
				else
				{
					results.MapResult(curFile, value);
				}
			}
			catch(Exception e)
			{
				System.out.println("AbstractAPSRun.Evaluate: Problem evaluating file " + curFile.getAbsolutePath());
				System.out.println(e.getMessage());
				e.printStackTrace();
				continue;
			}
			
		}
		
		return results;
		
	}
	
	//public GenericCosineSimComputer BuildSimMatrix(  File inputFile, int dataType, AbstractParameterDictionary runParams) throws Exception
	public IGenericSimComputer BuildSimMatrix(  File inputFile, int dataType, AbstractParameterDictionary runParams) throws Exception
	{
		AbstractParameterDictionary paramsInUse = null;
		if (runParams != null)
			paramsInUse = runParams;
		else
			paramsInUse = this.curParams;
		
		
		GenericDocument curDoc = (GenericDocument) this.freqMap.get(inputFile).clone();
		
		if (paramsInUse.GetBooleanValue("smoothing") == true)
		{
			Double parzenWindow = paramsInUse.GetNumericValue("smoothingWindow");
			Double parzenAlpha = paramsInUse.GetNumericValue("smoothingAlpha");
			curDoc.SmoothSentCounts(parzenWindow.intValue(), parzenAlpha.doubleValue());
		}
		
		curDoc.ApplyTfIdfWeighting();
		
		//now compute similarities
		File outDir = new File (paramsInUse.GetStringValue("outputDir"));
		IGenericSimComputer simComp;
		
		boolean useWNSim = false;
		boolean useSTSSim = false;
		try{
			useWNSim = paramsInUse.GetBooleanValue("useWordNetSim").booleanValue();
			//System.out.println("useWNSim is set");
		}
		catch (Exception e)
		{
			//maybe this option was not set
		}
		
		try{
			useSTSSim = paramsInUse.GetBooleanValue("useSTSSim").booleanValue();
			//System.out.println("useWNSim is set");
		}
		catch (Exception e)
		{
			//maybe this option was not set
		}
		
		Double winRatio = paramsInUse.GetNumericValue("windowRatio");
		int slidingWindow = this.ComputeSlidingWindow(curDoc.getDataSource(), winRatio);
		
		
		GenericCosineSimComputer sComp = new GenericCosineSimComputer();
		sComp = new GenericCosineSimComputer();
		sComp.Init(curDoc.getDataSource());
		
		
		
		sComp.SetUp(curDoc.getTokenDict(), slidingWindow, paramsInUse.GetBooleanValue("sparse").booleanValue(), outDir );
		sComp.SetSentenceVectors(curDoc.getSentVectors());
		sComp.ComputeSimilarities();
		simComp = (IGenericSimComputer)sComp;
		
		sComp = null;
		//simComp.OutputSimilarities(outDir);
		
		
		
		
		
		//free memory
		curDoc = null;
		//simComp.ForgetSentVectors();
		return simComp;
		

	}
	
	public int ComputeSlidingWindow(IGenericDataSource curDataSource, Double winRatio) throws Exception
	{
		//Double winRatio = this.curParams.GetNumericValue("windowRatio");
		if (winRatio == null || winRatio <= 0 || winRatio > 1)
		{
			System.out.println("SimpleAPSFold.Train: no valid curWinSize or curWinRation specified  for file " + curDataSource.GetName());
			Exception e = new Exception ("SimpleAPSFold.Train: no valid curWinSize or curWinRation specified  for file " + curDataSource.GetName());
		}
		Integer numChunks = curDataSource.GetNumberOfChunks();
		Double interm =  numChunks * winRatio ;		
		Integer slidingWindow =   new Integer(interm.intValue()) ;
		return slidingWindow.intValue();
	}
	
	
	AbstractAPSegmenterDP CreateAffinityPropagationSegmenter(IGenericSimComputer sims, double pref, double damp, boolean useSparse) throws Exception
	{
		int maxIterations = 1000;
		AbstractAPSegmenterDP segmenter;
		if (useSparse == false)
			segmenter = new AffinityPropagationSegmenterDense();
		else
			segmenter = new AffinityPropagationSegmenterSparse();
		
		segmenter.Init(sims);
		
		segmenter.setDampFactor(damp);
		segmenter.setMaxIterations(maxIterations);
		segmenter.SetPreferences(pref);
		//System.out.println("set prefs");
		return segmenter;
	}
	
	public TreeMap<File, GenericDocument> GetFreqMap() {
		return freqMap;
	}

	public void SetFreqMap(TreeMap<File, GenericDocument> freqMap) {
		this.freqMap = freqMap;
	}

	public AbstractParameterDictionary GetCurrentParameters()
	{
		return this.curParams;
	}
	
	public void SetIfPrecomputeFreqInInit(boolean newVal)
	{
		this.ifPrecomputeFreqInInit = newVal;
	}
	
	public boolean GetIfPrecomputeFreqInInit()
	{
		return this.ifPrecomputeFreqInInit;
	}
	
	/**
	 * Implementations of this method should run the APS segmenter on one files
	 * @params runParams parameters for the specific run, may be different from original parameters
	 * @return
	 */
	public abstract Double EvaluateDocument(  File inputFile, int dataType, AbstractParameterDictionary runParams) throws Exception;

	/**
	 * returns the name of the file without any extension
	 * @param longName
	 * @return
	 */
	public String GetShortName(String longName) 
	{
		Pattern namePattern = Pattern.compile("[^\\.]+"); // a pattern to identify only the name of the file name, not the extensions
		String name = "";
		
		Matcher m1 = namePattern.matcher(longName);
		if (m1.find())
		{
			name = m1.group(0);
			//System.out.println("new name " + newName);
		}
		return name;
	}

}
