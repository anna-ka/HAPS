package datasources;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

/*a interface for data sources that are annotated by more than one person.
 * 
 * */

public interface IDataSourceMultipleAnnots {
	
	//constants to specify the basic units of segmentation
	final static int SENT_LEVEL = 1;
	final static int PAR_LEVEL = 2;

	
	int GetNumChunks();
	int GetNumAnnotators();
	int GetAverageNumberOfSegments();
	int GetAverageSegmLength();
	
	void Init(int basicUnits) throws Exception;
	
	String GetChunk(int chunkIndex);
	
	String GetName();
	
	//get gold standard segment breaks
	TreeMap<Integer, ArrayList<Integer>> GetReferenceSegmentBreaks();
	
	void Output(File outputFile, Integer[] breaks, int annotatorId) throws Exception;

}
