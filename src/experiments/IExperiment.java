package experiments;

import java.io.File;
import java.util.ArrayList;

/**
 * The main interface to wrap around a particular experiment
 * @author anna
 *
 */
public interface IExperiment {
	/**
	 * A method to set all available data. Implementations of IExperiment should
	 * implement details of how those are split into developement, testing and hold out
	 * @param files
	 */
	void SetAvailFiles(File[] files);
	ArrayList<File> GetAvailableFiles();
	
	/**
	 * number of fold for cross-validation style experiments
	 * @param numFolds
	 */
	void SetNumberOfFolds(int numFolds);
	int GetNumberOfFolds();
	
	/**
	 * Segmenter to be used.
	 * @param segmenterClassString
	 */
	void SetSegmenter(String segmenterClassString);
	
	/**
	 * A method that should handle splitting all available data into dev-test folds
	 * @throws Exception 
	 */
	void CreateFolds() throws Exception;
	
	/**
	 * Run a particular fold 
	 * @param foldId
	 */
	void RunFold(int foldId) throws Exception;
	
	/**
	 * This method trains and tests on all folds that must have been previously specified
	 */
	void RunExperiment() throws Exception;
	
	/**
	 * In addition to dev-test folds, do we want to keep a holdout set?
	 * @param sizeOfHoldout
	 */
	void SetUseHoldoutSet(int sizeOfHoldout);
	/**
	 * @return 0 if no holdout set is used, or the size of the holdout set otherwise
	 * @return
	 */
	int GetUseHoldoutSet();
	
	/**
	 * write the results of the experiment to a file
	 */
	void OutputResults() throws Exception;
	
	File GetOutputDir();
	void SetOutputDir(File outputDirPath) throws Exception;
	String GetResultsFileName();
	/**
	 * sets the name of the output file without the path, e.g., results.txt
	 * @param fileName
	 * @see SetOutputDir(File outputDirPath)
	 */
	void SetResultsFileName (String fileName);

}
