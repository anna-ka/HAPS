package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import datasources.SimpleFileDataSource;
import datasources.SimpleFileMultipleAnnotsDataSource;
import datasources.IGenericDataSource;


import segmenter.SegFileFilter;


import evaluation.AbstractEvaluator;
import evaluation.EvalResultSet;
import evaluation.SimpleWinDiffEvaluator;

public class RandomRun implements IRun {
	
	AbstractParameterDictionary curParams = null;
	
	protected File[] inputFiles = null;
	protected File outputDir = null;
	protected int inputDataType = -1;

	@Override
	public void Run() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void Init(AbstractParameterDictionary params) throws Exception {
		
		String inputDirPath= params.GetStringValue("inputDir");
		ArrayList<String> inExt = params.GetStringList("inputExtensions");
		String[]inputExt = new String[inExt.size()];
		inputExt = inExt.toArray(inputExt);
		
		File inputDir = new File(inputDirPath);
		File[] inFiles =  inputDir.listFiles(new SegFileFilter(inputExt));
		this.SetInputFiles( inFiles);
		
		File outDir = new File(params.GetStringValue("outputDir"));
		this.SetOutputDir(outDir);
		
		Double dataType = params.GetNumericValue("inputDataType");
		this.SetInputDataType(dataType.intValue());
		
		this.curParams = params;
		
	}

	public File[] GetInputFiles() {
		return inputFiles;
	}

	public void SetInputFiles(File[] inputFiles)  throws Exception {
		this.inputFiles = inputFiles;
	}

	public File GetOutputDir() {
		return outputDir;
	}

	public void SetOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	public int GetInputDataType() {
		return inputDataType;
	}

	public void SetInputDataType(int inputDataType) {
		this.inputDataType = inputDataType;
	}

	/**
	 * If the source document is annotated, then random segmenter generates the same number of randomly generated breaks (between 0 nd numChunks).
	 * If the source document is not annotated, then the segmenter randomnly generates between 0 and 10 breaks (but less then numChunks).
	 */
	public EvalResultSet Evaluate(AbstractParameterDictionary runParams) throws Exception {
		EvalResultSet results = new EvalResultSet();
		results.Init(this.inputFiles);
		
		//loop through files and run
		for (File curFile: inputFiles)
		{
			Double value = this.EvaluateDocument( curFile);
			results.MapResult(curFile, value);
		}
		return results;
	}

	public Double EvaluateDocument(File curFile) throws Exception
	{
		int curDataType = this.GetInputDataType();
		
		IGenericDataSource curDS = null;
		Double segmLevel = this.curParams.GetNumericValue("segmLevel");
		String segmPattern = this.curParams.GetStringValue("segmPattern");
		
		if (curDataType == 0)
		{
			curDS = new SimpleFileDataSource();
			curDS.Init(segmLevel.intValue(), segmPattern, curFile, null);
		}
		//SimpleFileMultipleDataSource
		else if (inputDataType == 1)
		{
			String annotDirPath = this.curParams.GetStringValue("annotDir");
			File annotationsFile = new File( annotDirPath, curFile.getName());
			curDS = new SimpleFileMultipleAnnotsDataSource();
			curDS.Init(segmLevel.intValue(), segmPattern, curFile, annotationsFile);
		}
		//unknown data type
		else 
		{
			Exception e2 = new Exception ( "Unknown inputDataType in RandomRun.EvaluateDocument: " + String.valueOf(curDataType) );
			throw (e2);
		}
		
		int numSegm = curDS.GetAveNumberOfSegments().intValue();
		
		if (numSegm <= 1)
		{
			//this file has not been annotated
			int max = 10;
			if (curDS.GetNumberOfChunks() <= 10 )
				max = curDS.GetNumberOfChunks() - 1;
			int min = 0;
			numSegm = GenerateRandomInt(min, max);
		}
		
		IGenericDataSource hypoSegmentation = null;
		hypoSegmentation = new SimpleFileDataSource();		
		hypoSegmentation.LightWeightInit(curDS.GetNumberOfChunks());
		
		//now generate numSegm breaks between 0 and numChunks
		
		HashSet<Integer> chosenPositions = new HashSet<Integer>(numSegm);
		
		for (int i = 0; i < numSegm; i++)
		{
			int curBreak = GenerateRandomInt(0, curDS.GetNumberOfChunks()-1);
			if (chosenPositions.contains(new Integer(curBreak)))
			{
				i--;
				continue;
			}
			chosenPositions.add(new Integer(curBreak));
		}
		
		ArrayList<Integer> hypoBreaks = new ArrayList<Integer>(chosenPositions);
		
		//we use annotID=1 here because there is only one hypo segmentation
		hypoSegmentation.SetReferenceBreaks(1, hypoBreaks);
		
		ArrayList<Integer> refBreaks = curDS.GetReferenceBreaks(0);
		ArrayList<Integer> randBreaks = hypoSegmentation.GetReferenceBreaks(1);
		
//		System.out.println("\nref breaks:");
//		for (Integer rb: refBreaks)
//		{
//			System.out.print(rb.toString() + ", ");
//		}
//		System.out.println("\n rand breaks:");
//		for (Integer randB: randBreaks)
//		{
//			System.out.print(randB.toString() + ", ");
//		}
		
		
		AbstractEvaluator evaluator;
		
		if (this.curParams.GetStringValue("evalMetric").compareTo("winDiff") == 0)
		{
			evaluator= new SimpleWinDiffEvaluator();
		}
		else 
		{
			Exception e2 = new Exception ("Unknown evaluation metric in RandomRun.EvaluateDocument: " + this.curParams.GetStringValue("evalMetric"));
			throw (e2);
		}
		
		evaluator.Init("winDiff");
		evaluator.SetRefDS(curDS);
		evaluator.SetHypoDS(hypoSegmentation);
		if (evaluator.VerifyCompatibility() != AbstractEvaluator.COMPATIBLE )
		{
			String msg = "Exception in RandomRun.EvaluateDocument. Reference and hypo DS are not compatible. evaluator.VerifyCompatibility() failed.";
			throw (new Exception(msg));
		}
		else if (evaluator.SpecificVerifyCompatibility() != AbstractEvaluator.COMPATIBLE )
		{
			String msg = "Exception in RandomRun.EvaluateDocument. Reference and hypo DS are not compatible. evaluator.SpecificVerifyCompatibility() failed.";
			throw (new Exception(msg));
		}
		
		Double wd = evaluator.ComputeValue();
		return wd;
		
	}

	
	public static int GenerateRandomInt(int min, int max)
	{
		int answer = min + (int)(Math.random() * ( max-min ));
		return answer;
	}

	}
