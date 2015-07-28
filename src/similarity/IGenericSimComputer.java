package similarity;


import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import segmenter.IMatrix;
import datasources.IDataSource;
import datasources.IGenericDataSource;

/**
 * An interface for building a list of similarities between all chunks of text in IGenericDataSource.
 * 
 * This may correspond to either dense or sparse similarity matrix
 * 
 * */

public interface IGenericSimComputer {
	
	int MAX_NUM_POINTS = -1; // maximum number of points that can be used for computing a dense similarities matrix
	ISimMetric simMetric = null;

	void dispose();
	
	void Init(IGenericDataSource data);
	void ComputeSimilarities() throws Exception;
	IMatrix GetSimilarities();
	void OutputSimilarities(File outputDir)throws Exception;
	
	IGenericDataSource GetRawData();
	int GetPointsNumber();
	int GetWindowSize();
	boolean GetIfSparse();
	
	ArrayList<SentTokenVector> GetSentenceVectors();
	
	

}
