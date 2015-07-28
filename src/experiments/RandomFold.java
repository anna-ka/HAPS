package experiments;

import java.io.File;
import java.util.ArrayList;

import datasources.TextFileIO;

import evaluation.EvalResultSet;

public class RandomFold extends AbstractFold {

	@Override
	public void PrepareSearchSpace() {
		// TODO Auto-generated method stub

	}

	/**
	 * There is no training method in RandomSegmenter, so this method does nothing
	 */
	public void Train() throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * This method randomly generates segment breaks. 
	 */
	public void Test() throws Exception {
		RandomRun run = new RandomRun();
		run.Init(this.inputParameters);
		EvalResultSet res = run.Evaluate(this.inputParameters);
		this.testResultsMap.AddEntry(res, this.inputParameters);

	}

	/**
	 * There is no training here, so nothing to output
	 */
	public void OutputTrainResults() throws Exception {
		String outputDirPath = this.inputParameters.GetStringValue("outputDir");
		//String trainFilePath = this.inputParameters.GetStringValue("trainResultsFile");
		String trainFilePath = "train_fold_" + String.valueOf( this.GetFoldId() ) + ".txt";
		File outputFile = new File(outputDirPath, trainFilePath);
		TextFileIO.OutputFile(outputFile, "There is no training in random baseline");

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
		String testParamsFilePath = "test_fold_" + String.valueOf( this.GetFoldId() ) + "_random_aggregate.txt";
		File outputParamsFile = new File(outputDirPath, testParamsFilePath);
		//file to output detailed per file results
		String testDetailedFilePath = "test_fold_" + String.valueOf( this.GetFoldId() ) + "_random_details.txt";
		File outputDetailedFile = new File(outputDirPath, testDetailedFilePath);
		
		
		EvalResultSet result = this.testResultsMap.GetResult(testId);
		String metricName = result.GetMetricName();
		
		msg.append(metricName);
		msg.append("\t");
		msg.append("std.dev");
		msg.append("\n");
		
		
		
		msg.append(result.GetAverageValue().toString());
		msg.append("\t");
		msg.append(result.GetStdDev().toString());
		msg.append("\n");
		
		
		TextFileIO.OutputFile(outputParamsFile, msg.toString());
		
		//now output details per file
		TextFileIO.OutputFile(outputDetailedFile, result.GetResultsString());

	}

}
