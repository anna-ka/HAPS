package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import datasources.TextFileIO;

import evaluation.EvalResultSet;

public class HierarchicalAPSFold extends AbstractFold {

	@Override
	public void PrepareSearchSpace() {
		// TODO Auto-generated method stub

	}

	@Override
	public void Train() throws Exception {
		
	
		
		ArrayList<Double> winRatios = this.inputParameters.GetNumericList("windowRatio");
		ArrayList<Boolean> useSegmDfs = this.inputParameters.GetBooleanList("useSegmentDf");
		ArrayList<Boolean> useWeightedTfs = this.inputParameters.GetBooleanList("useWeightedTf");
		ArrayList<Double> numSegms = this.inputParameters.GetNumericList("numTFIDFsegments");
		ArrayList<Boolean> smoothValues = this.inputParameters.GetBooleanList("smoothing");
		ArrayList<Double> smoothingWindows = this.inputParameters.GetNumericList("smoothingWindow");
		ArrayList<Double> smoothingAlphas = this.inputParameters.GetNumericList("smoothingAlpha");
		ArrayList<Double> damps = this.inputParameters.GetNumericList("damping");
		ArrayList<Double> prefs0 = this.inputParameters.GetNumericList("preference_level0");
		ArrayList<Double> prefs1 = this.inputParameters.GetNumericList("preference_level1");
		ArrayList<Double> prefs2 = this.inputParameters.GetNumericList("preference_level2");
		
		Boolean useDf = new Boolean(false);
		
		HierarchicalAPSSimpleRun hierRun = new HierarchicalAPSSimpleRun();
		hierRun.Init(inputParameters);
		
		hierRun.SetInputFiles(this.devFiles);
		hierRun.ComputePreliminaryFrequencies(null);
		
		for (Double windowRatio: winRatios)
		{
				Boolean unsmoothedRun = false;
				for (Boolean curSmooth: smoothValues)
				{
					for (Double curPref0: prefs0)
					{
						for (Double curPref1: prefs1)
						{
							for (Double curPref2: prefs2)
							{
								for (Double curDamp: damps)
								{	
									for (Double parzenAlpha: smoothingAlphas)
									{
										for (Double parzenWindow: smoothingWindows)
										{
											
											
											APSParameterDictionary curParams = (APSParameterDictionary) this.inputParameters.clone();
											
											
											//alter parameters

											curParams.SetValue("isHier", new Boolean(true));
											
											curParams.SetValue("smoothing", curSmooth);
											curParams.SetValue("smoothingAlpha", parzenAlpha);
											curParams.SetValue("smoothingWindow", parzenWindow);
											
											curParams.SetValue("useSegmentDf", useDf);
											curParams.SetValue("windowRatio", windowRatio);
											
											curParams.SetValue("preference_level0", curPref0);
											curParams.SetValue("preference_level1", curPref1);
											curParams.SetValue("preference_level2", curPref2);
											
											curParams.SetValue("damping", curDamp);
											
											System.out.println("***Parameters on this iteration:");
											curParams.PrintParametersInUse();
											
											EvalResultSet curResult = hierRun.Evaluate(curParams);
//											System.gc(); //free memory
//											EvalResultSet curResult = this.GenerateRandomValues(this.GetDevFiles());
											this.trainResultsMap.AddEntry(curResult, curParams);
											
										}//end parzenWindow
									}//end parzenAlpha
								}//end curDamp
							}
						}
					}
							
					}//pref0
				}

	}

	@Override
	public void Test() throws Exception {
		int bestId = this.GetBestTrainRun();
		EvalResultSet bestResult = this.trainResultsMap.GetBestResult();
		AbstractParameterDictionary bestParams = this.trainResultsMap.GetParams(bestId);
		
		String outputDirPath = this.inputParameters.GetStringValue("outputDir");
		File wdFile = new File(outputDirPath, "test_fold_"+ this.GetFoldId() + "winDiff.txt");
		
		if (this.inputParameters.GetBooleanValue("useWDPerLevel")!= null && this.inputParameters.GetBooleanValue("useWDPerLevel").booleanValue() == true)
			bestParams.SetValue("wdPerLevelPath", wdFile.getCanonicalPath());
		
		HierarchicalAPSSimpleRun testHierRun = new HierarchicalAPSSimpleRun();
		testHierRun.Init(bestParams);
		
		testHierRun.SetInputFiles(this.GetTestFiles());
		testHierRun.ComputePreliminaryFrequencies(null);
		
		
		EvalResultSet curResult = testHierRun.Evaluate(bestParams);
//		EvalResultSet curResult = this.GenerateRandomValues(this.GetTestFiles());
		this.testResultsMap.AddEntry(curResult, bestParams);

	}
	
	public int GetBestTrainRun() throws Exception
	{
		if (this.trainResultsMap == null || this.trainResultsMap.GetNumEntries() <= 0)
		{
			throw (new Exception("trainResultsMap is empty"));
		}
		
		return this.trainResultsMap.GetBestId();
	}

	@Override
	public void OutputTrainResults() throws Exception {
		
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
//		paramNames.add("useSegmentDf");
//		paramNames.add("numTFIDFsegments");
		paramNames.add("smoothing");
		paramNames.add("smoothingAlpha");
		paramNames.add("smoothingWindow");
		paramNames.add("windowRatio");
		paramNames.add("damping");
		paramNames.add("preference_level0");
		paramNames.add("preference_level1");
		paramNames.add("preference_level2");
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
//		paramNames.add("useSegmentDf");
//		paramNames.add("numTFIDFsegments");
		paramNames.add("smoothing");
		paramNames.add("smoothingAlpha");
		paramNames.add("smoothingWindow");
		paramNames.add("windowRatio");
		paramNames.add("damping");
		paramNames.add("preference_level0");
		paramNames.add("preference_level1");
		paramNames.add("preference_level2");
		
		
		
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
