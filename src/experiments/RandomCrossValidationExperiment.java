package experiments;

import java.io.File;
import java.util.ArrayList;

public class RandomCrossValidationExperiment extends
		AbstractCrossValidationExperiment {

	/**
	 * Creates a RandomFold with specified dev and test files
	 * 
	 */
	public void CreateSpecificFold(int foldId, ArrayList<Integer> curDevSet,
			ArrayList<Integer> curTestSet) {
		RandomFold curFold = new RandomFold();
		
		curFold.inputParameters = this.paramsInUse;
		curFold.SetFoldId(foldId);
		
		
		File[] dev = new File[curDevSet.size()];
		int i = 0;
		for (Integer fileIndex: curDevSet)
		{
			File curFile = this.allFiles.get(fileIndex.intValue());
			dev[i] = curFile;
			i++;
			
		}
		
		
		File[] test = new File[curTestSet.size()];
		
		int j = 0;
		for (Integer fileIndex: curTestSet)
		{
			File curFile = this.allFiles.get(fileIndex.intValue());
			test[j] = curFile;
			j++;
			
		}
		
		
		curFold.SetDevFiles(dev);
		curFold.SetTestFiles(test);
		
		this.allFolds.put(new Integer(foldId), curFold);

	}

	

}
