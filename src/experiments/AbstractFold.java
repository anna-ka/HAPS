package experiments;

import java.io.File;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;

import evaluation.EvalResultSet;
import evaluation.EvalResultSetComparator;
import java.util.Comparator;

/**
 * A class to hold one cross-validation-styel fold
 * @author anna
 *
 */
public abstract class AbstractFold {
	
	
	
	AbstractParameterDictionary inputParameters;
	EvalResultMap trainResultsMap;
	EvalResultMap testResultsMap;
	
//	TreeMap<AbstractParameterDictionary, EvalResultSet> trainResults;
//	TreeMap<AbstractParameterDictionary, EvalResultSet> testResults;
//	EvalResultSet bestTrainResult = null;
//	EvalResultSet bestTestResult = null;
//	AbstractParameterDictionary bestTrainParams = null;
//	AbstractParameterDictionary bestTestParams = null;
	
	
	File[] devFiles = null;
	File[] testFiles = null;
	
	
	public AbstractFold()
	{
		this.trainResultsMap = new EvalResultMap();
		this.testResultsMap = new EvalResultMap();
	}
	
	protected int foldId = 0;
	
	public void SetFoldId(int id)
	{
		this.foldId = id;
	}
	
	public int GetFoldId ()
	{
		return this.foldId;
	}
	
	/**
	 * This method sets input parameters (that define search space) for training and testing this segmenter.
	 */
	public void SetInputParameters(AbstractParameterDictionary params) throws Exception {
		//String paramsClass = params.getClass().getSimpleName();
		
		this.inputParameters = params;
	}
	
	/**
	 * Set developement files for this fold
	 * @param devFiles
	 */
	public void SetDevFiles(File[] devFiles)
	{
		this.devFiles = devFiles;
	}
	/**
	 * Set test files for this fold
	 * @param testFiles
	 */
	public void SetTestFiles(File[] testFiles)
	{
		this.testFiles = testFiles;
	}
	
	public File[] GetDevFiles()
	{
		return this.devFiles;
	}
	
	public File[] GetTestFiles()
	{
		return this.testFiles;
	}
	
	/**
	 * implementations of this method should prepare the space of parameters that need to be searched through
	 */
	public abstract void PrepareSearchSpace();
	
	/**
	 * Select the best performing parameter combination using dev set of this fold
	 */
	public abstract void Train() throws Exception;
	
	/**
	 * run the segmenter using the best parameters from the dev. set on the test portion of the fold
	 */
	public abstract void Test() throws Exception;
	
	public EvalResultMap GetTrainResults()
	{
		return this.trainResultsMap;
	}
	
	public EvalResultMap GetTestResults()
	{
		return this.testResultsMap;
	}
	
	public abstract void OutputTrainResults() throws Exception;
	
	public abstract void OutputTestResults() throws Exception;

}
