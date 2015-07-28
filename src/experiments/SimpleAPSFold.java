package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import com.aliasi.tokenizer.TokenizerFactory;

import datasources.IGenericDataSource;
import datasources.SimpleFileDataSource;
import datasources.TextFileIO;

import evaluation.EvalResultSet;
import evaluation.EvalResultSetComparator;

import segmenter.AbstractAPSegmenterDP;

import similarity.DfDictionary;
import similarity.Document;
import similarity.GenericCosineSimComputer;
import similarity.GenericDocument;
import similarity.TokenDictionary;


public class SimpleAPSFold extends AbstractFold {
	
	

	/**
	 * The method searches over the specified parameter space to find the best combination of parameters on the dev set of this fold
	 */
	public void Train() throws Exception
	{
		ArrayList<Double> winRatios = this.inputParameters.GetNumericList("windowRatio");
		ArrayList<Boolean> useSegmDfs = this.inputParameters.GetBooleanList("useSegmentDf");
		ArrayList<Boolean> useWeightedTfs = this.inputParameters.GetBooleanList("useWeightedTf");
		ArrayList<Double> numSegms = this.inputParameters.GetNumericList("numTFIDFsegments");
		ArrayList<Boolean> smoothValues = this.inputParameters.GetBooleanList("smoothing");
		ArrayList<Double> smoothingWindows = this.inputParameters.GetNumericList("smoothingWindow");
		ArrayList<Double> smoothingAlphas = this.inputParameters.GetNumericList("smoothingAlpha");
		ArrayList<Double> prefs = this.inputParameters.GetNumericList("preference");
		ArrayList<Double> damps = this.inputParameters.GetNumericList("damping");
		
//		File inputDir = new File (this.inputParameters.GetStringValue("inputDir") );
//		File[] inputFiles = inputDir.listFiles();
		
		//indicates if we have already done a run of conventional, non-segment-based tf.idf
		boolean triedSimpleTf = false;
		
		for (Boolean useDf: useSegmDfs)
		{
		if (useDf.booleanValue() == false)
			{
				if ( triedSimpleTf == false )
				{
					triedSimpleTf = true;
				}
				else //we've already done one run of conventional tf
				{
					continue;
				}
			}
			
			for (Double curNumSegm: numSegms)
			{
				APSSimpleRun simpleRun = new APSSimpleRun();
				simpleRun.Init(this.inputParameters);
				//DfDictionary dfDict = simpleRun.GetDfDictionary();
				
				simpleRun.SetInputFiles(this.devFiles);
				simpleRun.ComputePreliminaryFrequencies(null);
				
				for (Boolean useWeightedTf: useWeightedTfs)
				{
					for (Double windowRatio: winRatios)
					{
							Boolean unsmoothedRun = false;
							for (Boolean curSmooth: smoothValues)
							{
								for (Double curPref: prefs)
									{
										for (Double curDamp: damps)
										{	
											for (Double parzenAlpha: smoothingAlphas)
											{
												for (Double parzenWindow: smoothingWindows)
												{
//													//is this the first attempt to do an unsmoothed run?
//													if (curSmooth.booleanValue() == false )
//													{
//														if (unsmoothedRun == false)
//														{
//															unsmoothedRun = true;
//														}
//														else
//															continue; //we do not want to loop over smoothing paramters for unsmoothed run
//													}
													
													APSParameterDictionary curParams = (APSParameterDictionary) this.inputParameters.clone();
													
													//alter parameters
													curParams.SetValue("smoothing", curSmooth);
													curParams.SetValue("useSegmentDf", useDf);
													curParams.SetValue("windowRatio", windowRatio);
													curParams.SetValue("preference", curPref);
													curParams.SetValue("damping", curDamp);
													curParams.SetValue("smoothingAlpha", parzenAlpha);
													curParams.SetValue("smoothingWindow", parzenWindow);
													curParams.SetValue("numTFIDFsegments", curNumSegm);
													curParams.SetValue("useWeightedTf", useWeightedTf);
													
													System.out.println("***Parameters on this iteration:");
													curParams.PrintParametersInUse();
													
													EvalResultSet curResult = simpleRun.Evaluate(curParams);
//													System.gc(); //free memory
//													EvalResultSet curResult = this.GenerateRandomValues(this.GetDevFiles());
													this.trainResultsMap.AddEntry(curResult, curParams);
													
		//											
												}
											} // end looping over parzenAlpha
										}//end looping over damps
									}//end looping over prefs	
							}//end of for (Boolean curSmooth: smoothValues)	
					}//end of for (Double windowRation: winRatios)
				}
			}
		}
	}
	
	public int GetBestTrainRun() throws Exception
	{
		if (this.trainResultsMap == null || this.trainResultsMap.GetNumEntries() <= 0)
		{
			throw (new Exception("trainResultsMap is empty"));
		}
		
		return this.trainResultsMap.GetBestId();
	}

	/**
	 * This method retrieves the best parameters found on the train set in this fold and then runs the system with those parameters on the test data
	 * and stores the result
	 */
	@Override
	public void Test() throws Exception {
		
		int bestId = this.GetBestTrainRun();
		EvalResultSet bestResult = this.trainResultsMap.GetBestResult();
		AbstractParameterDictionary bestParams = this.trainResultsMap.GetParams(bestId);
		
		APSSimpleRun testRun = new APSSimpleRun();
		testRun.Init(bestParams);
		//DfDictionary dfDict = simpleRun.GetDfDictionary();
		
		testRun.SetInputFiles(this.GetTestFiles());
		testRun.ComputePreliminaryFrequencies(null);
		
		EvalResultSet curResult = testRun.Evaluate(bestParams);
//		EvalResultSet curResult = this.GenerateRandomValues(this.GetTestFiles());
		this.testResultsMap.AddEntry(curResult, bestParams);
		
		

	}

//	private GenericCosineSimComputer USelessComputerIntermediateSimilarities(GenericDocument baseDoc, Boolean useSmoothing, Double parzenAlpha,
//			Double parzenWindow, Integer slidingWindow, Boolean sparse) throws Exception
//	{
//		GenericDocument curDoc = (GenericDocument)baseDoc.clone();
//		if (useSmoothing.booleanValue() == true)
//		{
//			curDoc.SmoothSentCounts(parzenWindow.intValue(), parzenAlpha.doubleValue());
//		}
//		curDoc.ApplyTfIdfWeighting();
//		
//		
//		File outDir = new File (this.inputParameters.GetStringValue("outputDir"));
//		GenericCosineSimComputer simComp = new GenericCosineSimComputer();
//		simComp.Init(curDoc.getDataSource());
//		
//		
//		
//		
//		simComp.SetUp(curDoc.getTokenDict(), slidingWindow, sparse, outDir );
//		simComp.SetSentenceVectors(curDoc.getSentVectors());
//		simComp.ComputeSimilarities();
//		//simComp.OutputSimilarities(outDir);
//		
//		//free memory
//		curDoc = null;
//		simComp.ForgetSentVectors();
//		return simComp;
//		
//	}
	
	public EvalResultSet GenerateRandomValues(File[] inputFiles)
	{
		EvalResultSet result = new EvalResultSet();
		result.Init(inputFiles);
		
		result.SetMetricName("randomvalue");
		
		for (File f: inputFiles)
		{
			result.MapResult(f, new Double( Math.random()));
		}
		
		return result;
		
	}
	
	public void OutputTrainResults() throws Exception
	{
		Set<Integer> idSet = this.trainResultsMap.GetAllIds();
		
		if ( idSet == null || idSet.size() <= 0)
		{
			String msg = "No training results to output.";
			Exception e = new Exception (msg);
			throw (e);
		}
		
		StringBuilder msg = new StringBuilder();
		
		String outputDirPath = this.inputParameters.GetStringValue("outputDir");
		//String trainFilePath = this.inputParameters.GetStringValue("trainResultsFile");
		String trainFilePath = "train_fold_" + String.valueOf( this.GetFoldId() ) + ".txt";
		File outputFile = new File(outputDirPath, trainFilePath);
		
		ArrayList<String> paramNames = new ArrayList<String>();
		paramNames.add("useSegmentDf");
		paramNames.add("numTFIDFsegments");
		paramNames.add("smoothing");
		paramNames.add("smoothingAlpha");
		paramNames.add("smoothingWindow");
		paramNames.add("windowRatio");
		paramNames.add("damping");
		paramNames.add("preference");
		//paramNames.add("inputDir");
		
		
		
		for (String name: paramNames)
		{
			msg.append(name);
			msg.append("\t");
		}
		
		Iterator<Integer> it = idSet.iterator();
		
		
		Integer firstId = it.next();
		EvalResultSet first = this.trainResultsMap.GetResult(firstId);
		String metricName = first.GetMetricName();
		
		msg.append(metricName);
		msg.append("\t");
		msg.append("std.dev");
		msg.append("\n");
		
		it = idSet.iterator();
		
		//for (AbstractParameterDictionary param: this.trainResults.keySet())
		
		while (it.hasNext())
		{
			Integer curId = it.next();
			EvalResultSet result = this.trainResultsMap.GetResult(curId);
			AbstractParameterDictionary params = this.trainResultsMap.GetParams(curId);
			
			for (String name: paramNames)
			{
				ArrayList<Object> paramValues = params.GetObjectList(name);
				for (Object obj: paramValues)
				{
					String item = obj.toString();
					msg.append(item);
					//msg.append("_");
				}
				msg.append("\t");	
			}
			msg.append(result.GetAverageValue().toString());
			msg.append("\t");
			msg.append(result.GetStdDev().toString());
			msg.append("\n");
			
		}
		
		TextFileIO.OutputFile(outputFile, msg.toString());
	}

	@Override
	public void PrepareSearchSpace() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OutputTestResults() throws Exception {
		
		Integer testId = new Integer (this.testResultsMap.GetBestId());
		
		
		if ( testId == null || testId < 0)
		{
			String msg = "No test results to output.";
			Exception e = new Exception (msg);
			throw (e);
		}
		
		StringBuilder msg = new StringBuilder();
		
		String outputDirPath = this.inputParameters.GetStringValue("outputDir");
		//file to output parameters used
		String testParamsFilePath = "test_fold_" + String.valueOf( this.GetFoldId() ) + "_params_used.txt";
		File outputParamsFile = new File(outputDirPath, testParamsFilePath);
		//file to output detailed per file results
		String testDetailedFilePath = "test_fold_" + String.valueOf( this.GetFoldId() ) + "_details.txt";
		File outputDetailedFile = new File(outputDirPath, testDetailedFilePath);
		
		ArrayList<String> paramNames = new ArrayList<String>();
		paramNames.add("useSegmentDf");
		paramNames.add("numTFIDFsegments");
		paramNames.add("smoothing");
		paramNames.add("smoothingAlpha");
		paramNames.add("smoothingWindow");
		paramNames.add("windowRatio");
		paramNames.add("damping");
		paramNames.add("preference");
		
		
		
		for (String name: paramNames)
		{
			msg.append(name);
			msg.append("\t");
		}
		
		EvalResultSet result = this.testResultsMap.GetResult(testId);
		String metricName = result.GetMetricName();
		
		msg.append(metricName);
		msg.append("\t");
		msg.append("std.dev");
		msg.append("\n");
		
		
		AbstractParameterDictionary params = this.testResultsMap.GetParams(testId);
		
		for (String name: paramNames)
		{
			ArrayList<Object> paramValues = params.GetObjectList(name);
			for (Object obj: paramValues)
			{
				String item = obj.toString();
				msg.append(item);
				//msg.append("_");
			}
			msg.append("\t");	
		}
		msg.append(result.GetAverageValue().toString());
		msg.append("\t");
		msg.append(result.GetStdDev().toString());
		msg.append("\n");
		
		
		TextFileIO.OutputFile(outputParamsFile, msg.toString());
		
		//now output details per file
		TextFileIO.OutputFile(outputDetailedFile, result.GetResultsString());
		
	}

}
