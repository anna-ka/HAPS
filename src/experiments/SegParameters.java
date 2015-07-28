package experiments;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import datasources.IDataSource;
import segmenter.SegFileFilter;




public class SegParameters {
	
	/**
	 * constants to specify the basic units of segmentation
	 * @see SegParamers.PAR_LEVEL
	 */
	public final static int SENT_LEVEL = 1;
	public final static int PAR_LEVEL = 2;
	
	/**
	 * A constant to specify a particular implementation of IGenericDataSource. Correpsonds to the regular one-annotation per document files
	 * (and SimpleFileDataSource class)
	 * @see multipleAnnotations
	 */
	public final static int singleAnnotation = 0;
	/**
	 * A constant to specify a particular implementation of IGenericDataSource. Corresponds to file
	 * for which multiple annotations are available, as in (Kazantseva and Szpakowicz 2012)
	 * (and SimpleFileMultipleAnnotsDataSource class)
	 * @see singleAnnotation
	 */
	public final static int multipleAnnotations = 1;
	
	private String inputFilePath = "unknown";
	private String inputDirPath = "unknown";
	private String annotDirPath = "unknown";
	private String outputDirPath = "unknown";
	private String resultsFileName = "unknown";
	private TreeSet<String> corpusExt = new TreeSet<String>();
	private TreeSet<String> inputExt = new TreeSet<String>();
	
	private boolean useSegmentDf = false; //if false, we use regular global tf.idf  
	private Integer numSegments = -1; //how many segments we split a document into if using segment df
	private boolean useSparseSegmenter = false; //should we use sparse or dense segmenter
	private boolean useSmoothing = false;
	private Double parzenAlpha = null; //alpha parameter for smoothing
	private Integer parzenWindowSize = null;

	private Double curPref = null; // preference
	private Double curDamp = null; // damping factor
	private Integer curWinSize = null; // window size
	private Double curWinRatio = null; // window ratio

	private ArrayList<Double> allPrefs = null;
	


	private   ArrayList<Double> allDamps = null;
	private   ArrayList<Double> allWinRatios = null;
	private   ArrayList<Double> allParzenAlphas = null;
	
	private   ArrayList<Integer> allWinSizes = null;
	private   ArrayList<Integer> allParzenWindows = null;
	private   ArrayList<Integer> allNumSegm = null;
	
	private String typeOfDataSource = "";
	private int inputDataType = -1;
	
	private String segmPattern = "[=]+";
	private int segmLevel = 1; //default is SENT
	//these are parameters specifying tokenization
	private String stopWordsPath = "./STOPWORD.list";
	private boolean lowerCase = true;
	private boolean useStemmer = true;
	private boolean removeStopWords = true;
	

	private boolean doRun;
	
	private StringBuilder curParams = new StringBuilder();
	
	public SegParameters()
	{
		this.allPrefs = new ArrayList<Double>();
		this.allDamps = new ArrayList<Double>();
		this.allWinRatios = new ArrayList<Double>();
		allParzenAlphas =  new ArrayList<Double>();
		
		this.allWinSizes = new ArrayList<Integer>();
		this.allNumSegm = new ArrayList<Integer>();
		
	}
	
	
	public String VerifyParameters()
	{
		//eventually check that the values are valid and return a sensible message
		String msg = null;
		
		//check the IO parameters
		if (this.inputDirPath != null )
		{
			try{
				File testFile = new File (this.inputDirPath);
			}
			catch (Exception e)
			{
				msg = msg + "inputDirPath is invalid:\t" + this.inputDirPath;
			}
		}
		else if (this.inputFilePath != null)
		{
			try{
				File testFile = new File (this.inputFilePath);
			}
			catch (Exception e)
			{
				msg = msg + "inputFilePath is invalid:\t" + this.inputFilePath;
			}
		}
		else
		{
			msg = msg + "No inputDirPath and no inputFilePath is specified";
		}
		
		//check the results file
		try{
			File testFile = new File (this.resultsFileName);
		}
		catch (Exception e)
		{
			msg = msg + "resultsFileName is invalid:\t" + this.resultsFileName;
		}
		
		//check type of DS
		if (this.typeOfDataSource.compareToIgnoreCase("SimpleFileDataSource") == 0)
			;
		else if (this.typeOfDataSource.compareToIgnoreCase("SimpleFileMultipleAnnotsDataSource") == 0)
		{
			//it needs to have a valid annotDir
			try{
				File testFile = new File (this.annotDirPath);
			}
			catch (Exception e)
			{
				msg = msg + "annotDirPath is invalid:\t" + this.annotDirPath;
			}
		}
		else
		{
			msg = msg + "\nInvalid typeOfDataSource:\t" + this.typeOfDataSource;
		}
		
		//check corpus extensions
		if (this.useSegmentDf == false)
		{
			
			try {
				File testFile = new File (this.inputDirPath);
				File[] dfFiles= testFile.listFiles(new SegFileFilter(this.getCorpusExtAsArray()));
				if (dfFiles.length <= 0)
				{
					msg = msg + "\nno corpus files found in " + this.inputDirPath + " for corpusExt:\n";
					for (String ext: this.getCorpusExtAsArray())
					{
						msg = msg + "\t" + ext + "\n";
					}
					
				}
			} catch (Exception e) 
			{
				msg = msg + "\nsomething is wrong with corpus extensions for input dir " + this.inputDirPath + " for corpusExt:\n";
				for (String ext: this.getCorpusExtAsArray())
				{
					msg = msg + "\t" + ext + "\n";
				}
			}
		}
		
		else if (this.useSegmentDf == true)
		{
			if (this.numSegments <= 0)
			{
				msg = msg + "\nuseSegmentDf=true and numSegments is <= 0:\t" + this.numSegments;
			}
		}
		
		if (this.segmLevel != IDataSource.PAR_LEVEL && this.segmLevel != IDataSource.SENT_LEVEL)
		{
			msg = msg + "\nInvalid segmLevel:\t" + this.segmLevel;
		}
		
		
		if (this.curDamp== null || this.curDamp >= 1)
		{
			msg = msg + "\nInvalid curDamp:\t" + this.curDamp;
		}
		
		if (this.curPref == null )
		{
			msg = msg + "\nInvalid curPref:\t" + this.curPref;
		}
		
		if (this.useSmoothing)
		{
			if (this.parzenAlpha == null)
				msg = msg + "\nuseSmoothing=true and parzenAlpha is not specified";
			if (this.parzenWindowSize == null)
				msg = msg + "\nuseSmoothing=true and parzenWindow is not specified";
		}
		
		if (this.useSparseSegmenter == true)
		{
			if  ( (this.curWinSize == null || this.curWinSize <= 0) &&
					(this.curWinRatio == null || this.curWinRatio <= 0 || this.curWinRatio >= 1) )
			{
				msg = msg + "\nuseSparseSegmenter=true and no both curWinSize and curWinRatio are invalid";
				msg = msg + "\n\tcurWinSize:\t" + this.curWinSize;
				msg = msg + "\n\tcurWinRatio:\t" + this.curWinRatio;
			}
		}
		
		
		if (this.removeStopWords == true)
		{
			try{
				File testFile = new File (this.stopWordsPath);
			}
			catch (Exception e)
			{
				msg = msg + "stopWordsPath is invalid:\t" + this.stopWordsPath;
			}
		}
		
		return msg;
	}
	
	public String GetCurParamsString()
	{
		return this.curParams.toString();
	}
	
	public void ModifyCurParamsString(String change)
	{
		curParams.append(change + "\n");
	}
	

	public String getInputFilePath() {
		return inputFilePath;
	}

	public void setInputFilePath(String inputFilePath) {
		this.inputFilePath = inputFilePath;
		this.ModifyCurParamsString("inputFilePath:\t" + this.inputFilePath);
	}

	public String getInputDirPath() {
		return inputDirPath;
	}

	public void setInputDirPath(String inputDirPath) {
		this.inputDirPath = inputDirPath;
		this.ModifyCurParamsString("inputDirPath:\t" + this.inputDirPath);
	}

	public String getAnnotDirPath() {
		return annotDirPath;
	}

	public void setAnnotDirPath(String annotDirPath) {
		this.annotDirPath = annotDirPath;
		this.ModifyCurParamsString("annotDirPath:\t" + this.annotDirPath);
	}

	public String getOutputDirPath() {
		return outputDirPath;
	}

	public void setOutputDirPath(String outputDirPath) {
		this.outputDirPath = outputDirPath;
		this.ModifyCurParamsString("outputDirPath:\t" + this.outputDirPath);
	}

	public String getResultsFileName() {
		return resultsFileName;
	}

	public void setResultsFileName(String resultsFileName) {
		this.resultsFileName = resultsFileName;
		this.ModifyCurParamsString("resultsFileName:\t" + this.resultsFileName);
	}

	public TreeSet<String> getCorpusExt() {
		return corpusExt;
	}
	
	public String[] getCorpusExtAsArray()
	{
		String[] corp = new String[this.corpusExt.size()];
		int i = 0;
		for (String ext: this.corpusExt)
		{
			corp[i] = ext;
			i++;
		}
		return corp;
	}

	public void addCorpusExt(String curCorpusExt) {
		this.corpusExt.add(curCorpusExt);
		this.ModifyCurParamsString("\tcorpus ext:\t" + curCorpusExt);
	}

	public TreeSet<String> getInputExt() {
		return inputExt;
	}

	public void addInputExt(String curInputExt) {
		this.inputExt.add(curInputExt);
		this.ModifyCurParamsString("\tinput ext:\t" + curInputExt);
	}

	public boolean getUseSegmentDf() {
		return useSegmentDf;
	}

	public void setUseSegmentDf(boolean useSegmentDf) {
		this.useSegmentDf = useSegmentDf;
		this.ModifyCurParamsString("useSegmentDf:\t" + this.useSegmentDf);
	}

	public Integer getNumSegments() {
		return numSegments;
	}

	public void setNumSegments(Integer numSegments) {
		this.numSegments = numSegments;
		this.ModifyCurParamsString("numSegments:\t" + this.getNumSegments());
	}

	public boolean isUseSparseSegmenter() {
		return useSparseSegmenter;
	}

	public void setUseSparseSegmenter(boolean useSparseSegmenter) {
		this.useSparseSegmenter = useSparseSegmenter;
		this.ModifyCurParamsString("useSparseSegmenter:\t" + this.useSparseSegmenter);
	}

	public boolean isUseSmoothing() {
		return useSmoothing;
	}

	public void setUseSmoothing(boolean useSmoothing) {
		this.useSmoothing = useSmoothing;
		this.ModifyCurParamsString("useSmoothing:\t" + this.useSmoothing);
	}

	public Double getParzenAlpha() {
		return parzenAlpha;
	}

	public void setParzenAlpha(Double parzenAlpha) {
		this.parzenAlpha = parzenAlpha;
		this.ModifyCurParamsString("parzenAlpha:\t" + this.getParzenAlpha());
	}

	public Integer getParzenWindowSize() {
		return parzenWindowSize;
	}

	public void setParzenWindowSize(Integer parzenWindowSize) {
		this.parzenWindowSize = parzenWindowSize;
		this.ModifyCurParamsString("parzenWindow:\t" + this.getParzenWindowSize());
	}

	public Double getCurPref() {
		return curPref;
	}

	public void setCurPref(Double curPref) {
		this.curPref = curPref;
		this.ModifyCurParamsString("curPref:\t" + this.curPref);
	}

	public Double getCurDamp() {
		return curDamp;
	}

	public void setCurDamp(Double curDamp) {
		this.curDamp = curDamp;
		this.ModifyCurParamsString("curDamp:\t" + this.curDamp);
	}

	public Integer getCurWinSize() {
		return curWinSize;
	}

	public void setCurWinSize(Integer curWinSize) {
		this.curWinSize = curWinSize;
		this.ModifyCurParamsString("curWinSize:\t" + this.curWinSize);
	}

	public Double getCurWinRatio() {
		return curWinRatio;
	}

	public void setCurWinRatio(Double curWinRatio) {
		this.curWinRatio = curWinRatio;
		this.ModifyCurParamsString("curWinRatio:\t" + this.curWinRatio);
	}

	public boolean isDoRun() {
		return doRun;
	}

	public void setDoRun(boolean doRun) {
		this.doRun = doRun;
		this.ModifyCurParamsString("doRun:\t" + this.doRun);
	}


	public String getSegmPattern() {
		return segmPattern;
	}


	public void setSegmPattern(String segmPattern) {
		this.segmPattern = segmPattern;
		this.ModifyCurParamsString("segmPattern:\t" + this.segmPattern);
	}


	public int getSegmLevel() {
		return segmLevel;
	}


	public void setSegmLevel(int segmLevel) {
		this.segmLevel = segmLevel;
		String level = "";
		if (this.segmLevel == IDataSource.SENT_LEVEL)
			level = "SENT";
		else if (this.segmLevel == IDataSource.PAR_LEVEL)
			level = "PAR";
		else
			level = "UNKNOWN";
		this.ModifyCurParamsString("segmLevel:\t" + level);
	}


	public String getStopWordsPath() {
		return stopWordsPath;
	}


	public void setStopWordsPath(String stopWordsPath) {
		this.stopWordsPath = stopWordsPath;
		this.ModifyCurParamsString("stopWordsPath:\t" + this.stopWordsPath);
	}


	public boolean getUseStemmer() {
		return useStemmer;
	}


	public void setUseStemmer(boolean useStemmer) {
		this.useStemmer = useStemmer;
		this.ModifyCurParamsString("useStemmer:\t" + this.useStemmer);
	}


	public boolean getLowerCase() {
		return lowerCase;
	}


	public void setLowerCase(boolean lowerCase) {
		this.lowerCase = lowerCase;
		this.ModifyCurParamsString("use lower-case tokenizer:\t" + this.lowerCase);
	}


	public boolean getRemoveStopWords() {
		return removeStopWords;
	}


	public void setRemoveStopWords(boolean removeStopWords) {
		this.removeStopWords = removeStopWords;
		this.ModifyCurParamsString("remove stopwords:\t" + this.removeStopWords);
	}


	public String getTypeOfDataSource() {
		return typeOfDataSource;
	}


	public void setTypeOfDataSource(String typeOfDataSource) {
		this.typeOfDataSource = typeOfDataSource;
		this.ModifyCurParamsString("typeOfDataSource:\t" + this.typeOfDataSource);
	}


	public int getInputDataType() {
		return inputDataType;
	}


	public void setInputDataType(int inputDataType) {
		this.inputDataType = inputDataType;
	}
	
	
	public ArrayList<Double> getAllPrefs() {
		return allPrefs;
	}


	public void setAllPrefs(ArrayList<Double> allPrefs) {
		this.allPrefs = allPrefs;
	}
	
	public void addPref (Double newPref)
	{
		this.allPrefs.add(newPref);
	}


	public ArrayList<Double> getAllDamps() {
		return allDamps;
	}


	public void setAllDamps(ArrayList<Double> allDamps) {
		this.allDamps = allDamps;
	}
	
	public void addDamp (Double newDamp)
	{
		this.allDamps.add(newDamp);
	}


	public ArrayList<Double> getAllWinRatios() {
		return allWinRatios;
	}


	public void setAllWinRatios(ArrayList<Double> allWinRatios) {
		this.allWinRatios = allWinRatios;
	}
	
	public void addWinRatio (Double newWinRatio)
	{
		this.allWinRatios.add(newWinRatio);
	}


	public ArrayList<Double> getAllParzenAlphas() {
		return allParzenAlphas;
	}


	public void setAllParzenAlphas(ArrayList<Double> allParzenAlphas) {
		this.allParzenAlphas = allParzenAlphas;
	}
	
	public void addParzenAlpha (Double newAlpha)
	{
		this.allParzenAlphas.add(newAlpha);
	}


	public ArrayList<Integer> getAllWinSizes() {
		return allWinSizes;
	}


	public void setAllWinSizes(ArrayList<Integer> allWinSizes) {
		this.allWinSizes = allWinSizes;
	}
	
	public void addNewWinSize(Integer winSize)
	{
		this.allWinSizes.add(winSize);
	}


	public ArrayList<Integer> getAllParzenWindows() {
		return allParzenWindows;
	}


	public void setAllParzenWindows(ArrayList<Integer> allParzenWindows) {
		this.allParzenWindows = allParzenWindows;
	}

	public void addParzenWindow(Integer newParzenWindow)
	{
		this.allParzenWindows.add(newParzenWindow);
	}

	public ArrayList<Integer> getAllNumSegm() {
		return allNumSegm;
	}


	public void setAllNumSegm(ArrayList<Integer> allNumSegm) {
		this.allNumSegm = allNumSegm;
	}
	
	public void addNewNumSegm(Integer newNumSegm)
	{
		this.allNumSegm.add(newNumSegm);
	}

}
