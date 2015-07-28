package experiments;

import java.io.File;

import evaluation.EvalResultSet;


/**
 * 
 * @author anna
 * An interface to hold the result of one run of a segmenter on a set of files with a single specified set of parameters. 
 * Hence the parameters must be specified for one run, not for tuning (e.g., for APS, it would mean one preference value, not a range, etc.)
 *
 */
public interface IRun {
	
	public void Run() throws Exception;
	
	
	/**
	 * Initialize the segmenter so that it can be used for a set of files. This method should do whatever needs to be done only once for all those files.
	 * @param params dictionary of parameters, must specify only one value per parameter, not a range. Otherwise, only the first value will be used.
	 * @throws Exception
	 */
	public void Init(AbstractParameterDictionary params) throws Exception;
	
	/**
	 * Implementations of this method must return file->value mapping in EvalResultSet 
	 * @param runParams parameters to be used for this particular run. If runParams == null, then original paramaters are used
	 * @return
	 * @throws Exception
	 */
	public EvalResultSet Evaluate(AbstractParameterDictionary runParams) throws Exception;
	
	
	public void SetInputFiles(File[] inputFiles) throws Exception;

}
