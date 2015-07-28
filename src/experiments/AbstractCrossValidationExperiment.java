package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import segmenter.SegFileFilter;

import datasources.TextFileIO;
/**
 * A typical unpaired cross-validation experiment.
 * 
 * @author anna
 *
 */
public abstract class AbstractCrossValidationExperiment implements IExperiment{
	
	AbstractParameterDictionary paramsInUse;
	ArrayList<File> allFiles = null;
	TreeMap<Integer, AbstractFold> allFolds = new TreeMap<Integer, AbstractFold>();

	//maps foldId -> obtained mean test value
	TreeMap <Integer, Double> foldEvalValueMap ;
	
	File outputDir;

	String resultsFileName = "";
	
	int numFolds = 0;
	int sizeHoldout = 0;
	ArrayList<File> holdoutSet = null;
	String segmClassString = null;
	
	final int maxSizeOfTrainingFold = 3;
	
	public void Init(AbstractParameterDictionary params)
	{
		this.paramsInUse = params;
		
		this.allFiles = new ArrayList<File>();
		this.allFolds = new TreeMap<Integer, AbstractFold>();
		
		
		int numFolds = 0;
		int sizeHoldout = 0;
		this.holdoutSet = new ArrayList<File>();
		String segmClassString = null;
		
		//maps foldId -> obtained mean test value
		this.foldEvalValueMap = new TreeMap <Integer, Double> ();
		
		try {
			this.outputDir = new File (this.paramsInUse.GetStringValue("outputDir"));
			this.resultsFileName = this.paramsInUse.GetStringValue("resultsFile");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This method removes the information about the number of folds, folds computed and desired holdout set
	 */
	public void CleanUp()
	{
		this.SetNumberOfFolds(0);
		this.SetUseHoldoutSet(0);
		this.holdoutSet = new ArrayList<File>();
		this.RemoveFolds();
	}
	
	public File GetOutputDir() {
		return outputDir;
	}

	public void SetOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	public String GetResultsFileName() {
		return resultsFileName;
	}

	public void SetResultsFileName(String resultsFile) {
		this.resultsFileName = resultsFile;
	}
	
	/**
	 * Set all files available for this experiment, including dev, test and holdout data.
	 */
	public void SetAvailFiles(File[] files) {
		this.allFiles = new ArrayList<File>();
		for (File f: files)
			this.allFiles.add(f);
		
	}

	/**
	 * @return all available files for this experiment, including dev, test and holdout data.
	 */
	public ArrayList<File> GetAvailableFiles() {
		return (this.allFiles);
	}

	/**
	 * @param number of folds for the experiment (must be >=1)
	 */
	public void SetNumberOfFolds(int numFolds) {
		this.numFolds = numFolds;
		
	}

	
	public int GetNumberOfFolds() {
		return (this.numFolds);
	}

	
	public void SetSegmenter(String segmenterClassString) {
		this.segmClassString = segmenterClassString;
		
	}
	
	/**
	 * This method removes the fold assignments if any have been computed. After running it the class instance becomes useless until CreateFolds() is run again
	 */
	public void RemoveFolds()
	{
		this.allFolds = new TreeMap<Integer, AbstractFold>();
	}
	
	public void SetAvailableFilesFromParameters() throws Exception
	{
		File inputDir = new File (this.paramsInUse.GetStringValue("inputDir"));
		
		String[] inputExt = new String[this.paramsInUse.GetStringList("inputExtensions").size()];
		inputExt = this.paramsInUse.GetStringList("inputExtensions").toArray(inputExt);
		File[] tmpFiles= inputDir.listFiles(new SegFileFilter(inputExt));
		this.SetAvailFiles(tmpFiles);
	}
	
	/**
	 * This method splits the available data into developement/test folds, possibly leaving a number of files for hold-out set.
	 * 
	 *  To make training faster and since we do not need many train files, I will restrict the size of train set to this.maxSizeOfTrainingFold.
	 */
	public void CreateFolds() throws Exception
	{
		if (this.allFiles == null || this.allFiles.size() <= 0)
		{
			this.SetAvailableFilesFromParameters();
		}
		
		int devSize = this.allFiles.size();
		
		
		
		//create a holdout set, if the option is specified
		TreeSet<Integer> selectedIndices = new TreeSet<Integer>();
		
		if (this.sizeHoldout > 0)
		{
			//check that the requested hold out is not too great
			if (this.sizeHoldout >= this.allFiles.size())
			{
				System.out.println ("Warning: specified of the hold out set is too great: " + String.valueOf(this.sizeHoldout) +  " files for " + String.valueOf(this.allFiles.size()) + " available files. I will not use holdout. " );
				this.SetUseHoldoutSet(0);
			}
			
			
			for (int i = 0; i < this.sizeHoldout; i++)
			{
				//randomly select an index from allFiles
				//Min + (int)(Math.random() * ((Max - Min) + 1))
				int index = (int)  (Math.random() *   this.allFiles.size() ); 
				if (selectedIndices.contains(new Integer(index)))
				{
					i--;//try again
				}
				else
				{
					selectedIndices.add(new Integer(index));
					this.holdoutSet.add(this.allFiles.get(index));
				}
			}	
		}
		
		
		devSize = devSize  - this.sizeHoldout;
		
		//a set of indices of files available for train/test
		ArrayList<Integer> workingFiles = new ArrayList<Integer>();
		
		for (int i = 0; i < this.allFiles.size(); i++)
		{
			if ( selectedIndices.contains(new Integer(i)))
				continue; // this file is in holdout set
			
			workingFiles.add(new Integer(i));
		}
		
		//check the requested number of folds is less then the number of available files
		if (workingFiles.size() <= 1)
		{
			String msg = "Too few files are available after making the hold out set to make any folds. Number of avail. files: "+ String.valueOf(workingFiles.size());
			throw (new Exception (msg));
		}
		
		if (this.numFolds > workingFiles.size())
		{
			System.out.println ("Warning: specified number of cross validation folds is too great: " + String.valueOf(this.numFolds) +  " folds for " + String.valueOf(workingFiles.size()) + " available files. I will use " + String.valueOf(workingFiles.size()) + "folds");
			this.SetNumberOfFolds(workingFiles.size());
		}
		
		
		
		
		//now create buckets of randomly selected files, sampled without replacement
		TreeMap<Integer, ArrayList<Integer>> buckets = new TreeMap<Integer, ArrayList<Integer>>();
		int foldSize = (int) Math.floor( (double)(devSize) / (double)(this.numFolds));
		
		if (foldSize <= 0)
		{
			Exception e = new Exception ("Invalid fold size in CreateFolds: " + String.valueOf(foldSize));
			throw (e);
		}
		
		for (int i = 0; i < numFolds; i++)
		{
			buckets.put(new Integer(i), new ArrayList<Integer>());
		}
		
		
		for (int curFold = 0; curFold < this.numFolds; curFold++)
		{
			//use all remaining files for the last fold
			if (curFold == this.numFolds - 1)
			{
				for (Integer i : workingFiles)
				{
					buckets.get(new Integer(curFold)).add(i);
				}
				break;
			}
			
			//for all other folds
			for (int curFile = 0; curFile < foldSize; curFile++)
			{
				int curIndex =  (int)  (Math.random() *   workingFiles.size() ); 
				Integer i = workingFiles.get(curIndex);
				//put this file into this fold
				buckets.get(new Integer(curFold)).add(new Integer(i));
				//remove it from the list of availabel Files
				workingFiles.remove(curIndex);
			}
		}
		
		//now actually create specific folds
		//this.CreateSpecificFold(i, curDevIndices, curTestIndices);	
		
		for (int curFold = 0; curFold < this.numFolds; curFold++)
		{
			ArrayList<Integer> curDevIndices;
			ArrayList<Integer> curTestIndices;
			
			int devFold;
			int testFold;
			
			if (curFold == this.numFolds - 1)
			{
				devFold = curFold;
				testFold = 0;
			}
			else
			{
				devFold = curFold;
				testFold = curFold + 1;
			}
			
			curDevIndices = buckets.get(devFold);
			
			if (curDevIndices.size() > this.maxSizeOfTrainingFold)
			{
				ArrayList<Integer> shortDevIndices = new ArrayList<Integer>();
				for (int i = 0; i <
						this.maxSizeOfTrainingFold; i++)
				{
					shortDevIndices.add(curDevIndices.get(i));
				}
				curDevIndices = shortDevIndices;
			}
			
			curTestIndices = buckets.get(testFold);
			
			this.CreateSpecificFold(curFold, curDevIndices, curTestIndices);
		}
		
		
	}

	/**
	 * Obsolte, do not use
	 * this method splits the available data into developement/test folds, possibly leaving a number of files for hold-out set
	 */
	public void CreateFoldsOld() {
		
		
		int devSize = this.allFiles.size();
		
		if (this.sizeHoldout > 0)
			devSize = devSize  - this.sizeHoldout;
		
		//create a holdout set, if the option is specified
		TreeSet<Integer> selectedIndices = new TreeSet<Integer>();
		
		if (this.sizeHoldout > 0)
		{
			for (int i = 0; i < this.sizeHoldout; i++)
			{
				//randomly select an index from allFiles
				//Min + (int)(Math.random() * ((Max - Min) + 1))
				int index = (int)  (Math.random() *   this.allFiles.size() ); 
				if (selectedIndices.contains(new Integer(index)))
				{
					i--;//try again
				}
				else
				{
					selectedIndices.add(new Integer(index));
					this.holdoutSet.add(this.allFiles.get(index));
				}
			}	
		}
		
		
		//create sets of indices of files available for development and testing folds (excluding the holdout set)
		TreeSet<Integer> devSet = new TreeSet<Integer>();
		TreeSet<Integer> testSet = new TreeSet<Integer>();
		for (int i = 0; i < this.allFiles.size(); i++)
		{
			if (selectedIndices.contains(new Integer(i)) == false)
				{
					devSet.add(new Integer(i));
					testSet.add(new Integer(i));
				}
				
		}
		
		//now create folds
		int foldSize = (int) Math.floor( (double)(devSize) / (double)(this.numFolds));
		
		for (int i = 0; i < this.numFolds - 1; i++)
		{
			TreeSet<Integer> curDevIndices = new TreeSet<Integer>();
			
			//select foldSize development files
			for (int j = 0; j < foldSize; j++)
			{
				int index = (int)  (Math.random() *   this.allFiles.size() ); 
				if (devSet.contains(new Integer(index)))
				{
					curDevIndices.add(new Integer(index));
					devSet.remove(new Integer(index));
				}
				else
					j--;
			}
			
			//now select foldSize test Files such that they do not overlap with dev files
			TreeSet<Integer> curTestIndices = new TreeSet<Integer>();
			
			for (int k = 0; k < foldSize; k++)
			{
				int index = (int)  (Math.random() *   this.allFiles.size() ); 
				if (testSet.contains(new Integer(index)) && curDevIndices.contains(new Integer(index)) == false)
				{
					curTestIndices.add(new Integer(index));
					testSet.remove(new Integer(index));
				}
				else
					k--;
			}
		
			//this.CreateSpecificFold(i, curDevIndices, curTestIndices);		
		}
		
		//using the remaining data create the last fold
		//this.CreateSpecificFold(this.numFolds - 1, devSet, testSet);	
		
		
		
	}

	/**
	 * Implementations of this method should create specific instances of subclasses of AbstractFold
	 * @param foldId
	 * @param curDevSet
	 * @param curTestSet
	 */
	public abstract void CreateSpecificFold(int foldId, ArrayList<Integer> curDevSet, ArrayList<Integer> curTestSet);
	
/**
 * Trains and tests each of the available folds
 */
	public void RunFold(int foldId) throws Exception {
		System.out.println("Staring fold " + foldId);
		AbstractFold curFold = this.GetSpecificFold(foldId);
		curFold.Train();
		System.out.println("\t finished training " + foldId);
		curFold.OutputTrainResults();
		curFold.Test();
		System.out.println("\t finished testing " + foldId);
		
		this.foldEvalValueMap.put(new Integer(foldId), curFold.GetTestResults().GetBestResult().GetAverageValue());
		curFold.OutputTestResults();
	}
	
	/**
	 * 
	 */
	public void RunExperiment() throws Exception
	{
		
		for (Integer foldId: this.allFolds.keySet())
		{
			try{
//				if (foldId <=1 )
//					continue;
				this.RunFold(foldId);
			}
			catch (Exception e)
			{
				System.out.println("Exception when running fold " + foldId.toString());
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
		}
		this.OutputResults();
		
	}	
		
	
	/**
	 * This method returns the fold specified by foldId.
	 * 
	 * @param foldId
	 */
	public AbstractFold GetSpecificFold(int foldId) throws Exception
	{
		if (foldId <0 || foldId >= this.allFolds.keySet().size())
		{
			Exception e = new Exception ("Invalid foldId specified: " + String.valueOf(foldId));
			throw (e);
		}
		
		return ( this.allFolds.get(foldId) ) ;
		
	}

	/**
	 * Set the size of the holdout set. The holdout set will not be used when creating the folds
	 */
	public void SetUseHoldoutSet(int sizeOfHoldout) {
		this.sizeHoldout = sizeOfHoldout;
		
	}

	/**
	 * @return 0 if no holdout set has been specified, or positive int = size of the holdout set
	 */
	public int GetUseHoldoutSet() {
		return this.sizeHoldout;
	}
	
	public void OutputResults() throws Exception
	{
		StringBuilder msg = new StringBuilder();
		
		for (Integer id: this.allFolds.keySet())
		{
			msg.append("Fold " + id.toString() + " : " +  this.foldEvalValueMap.get(id).toString() + "\n");
		}
		
		File outputFile = new File(this.outputDir, this.resultsFileName);
		TextFileIO.OutputFile(outputFile, msg.toString());
	}
	
	/**
	 * Prints out how the available data is split between holdout, and the folds
	 */
	public void PrintSetUp()
	{
		String sep = "\t";
		StringBuilder msg = new StringBuilder();
		
		msg.append("All available files: " + String.valueOf(this.allFiles.size()) + "\n");
		msg.append("Number of folds: " + String.valueOf(this.GetNumberOfFolds()) + "\n");
		
		
		msg.append("Holdout set: ("  + String.valueOf(this.GetUseHoldoutSet())+ " files) \n");
		for (File f: this.holdoutSet)
		{
			msg.append(sep + f.getAbsolutePath() + "\n");
			
		}
		
		
		for (Integer foldId: this.allFolds.keySet())
		{
			msg.append("Fold " + foldId.toString() + "\n");
			
			AbstractFold curFold = this.allFolds.get(foldId);
			
			//print out training files for this fold
			msg.append(sep + "training: " +  "\n");
			for (File f: curFold.GetDevFiles() ) 
			{
				msg.append(sep + sep + f.getAbsolutePath() +  "\n");
			}
			// now print out testing files for this fold
			msg.append(sep + "testing: " +  "\n");
			for (File f: curFold.GetTestFiles() ) 
			{
				msg.append(sep + sep + f.getAbsolutePath() +  "\n");
			}
		}
		
		System.out.println(msg.toString());
	//later may want to add info about particular segmenter, search space etc
			
		
	}

}
