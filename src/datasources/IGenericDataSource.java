package datasources;

import java.io.File;
import java.util.ArrayList;

/**
 * Generic interface for data sources for both single- and mutiple annoations.
 * It supercedes IDataSource and IDataSourceMultipleAnnots.
 * 
 */
public interface IGenericDataSource {
	
		/**
		 * constants to specify the basic units of segmentation
		 */
		final static int SENT_LEVEL = 1;
		final static int PAR_LEVEL = 2;
		/**
		 * constants specifying the types of DataSources
		 */
		final static int SIMPLE_DS = 0; //text file with inline annotations
		final static int SIMPLE_MULTIPLE_ANNOTS_DS = 1; //text file with several annotations available in a separate file
		final static int CONNEXOR_SIMPLE_DS = 2; // parsed xml file with annotations inline
		final static int CONNEXOR_MULTIPLE_ANNOTS_DS = 3; // parsed xml file with annotations available in a separate file
		
		final static int HIER_MULTIPLE_ANNOTS_DS = 4;
		
		/**
		 * 
		 * @param  levelOfSegmentation  Specifies granularity of segmentation. IGenericDataSource.SENT_LEVEL or IGenericDataSource.PAR_LEVEL
		 * @param  segmPattern	specifies how segment breaks are marked in the textFile 
		 * @param  textFile	the file with the original text of the document
		 * @param  annotationFiles	available annotations
		 */
		void Init (int levelOfSegmentation, String segmPattern, File textFile, File annotationsFile) throws Exception;
		
		/**
		 * A method to initialize a data source from text, not file. 
		 * @param levelOfSegmentation
		 * @param segmPattern
		 * @param text
		 * @param annotationsFile
		 * @throws Exception
		 */
		void Init (int levelOfSegmentation, String segmPattern, String text, File annotationsFile) throws Exception;
		
		/**
		 * this method should create a dummy data source with breaks specified but no text loaded.
		 * It is to be used to hold hypothetical annotattions produced by the segmenters.
		 */
		void LightWeightInit( int numChunks);
		
		void SetIfHierarchical(boolean isHierarchical);
		boolean GetIfHierarchical();
		
		int GetNumberOfAnnotators();
		
		
		String GetChunk(int annotId) throws Exception;
		
		/**
		 @return the name of this data source (the name of the input file)
		 */
		String GetName();
		
		
		int GetNumberOfChunks();
		
		int GetNumberOfSegments(int annotId);
		int GetNumberOfSegmentsAtLevel(int annotId, int levelId) throws Exception;
		
		ArrayList<Integer> GetReferenceBreaks(int annotId);
		ArrayList<Integer> GetReferenceBreaksAtLevel(int annotId, int levelId) throws Exception;
		
		void SetReferenceBreaks(int annotId, ArrayList<Integer> breaks) throws Exception;
		void SetReferenceBreaksAtLevel(int annotId, ArrayList<Integer> breaks, int levelId) throws Exception;
		
		//across all annotations
		/**
		 * Average number of segments across all available annotations
		 */
		Double GetAveNumberOfSegments() throws Exception;
		Double GetAveNumberOfSegmentsAtLevel(int levelId) throws Exception;
		
		
		/**
		 * Average segment length across all available annotations
		 */
		Double GetAverageSegmentLength() throws Exception;
		Double GetAverageSegmentLengthAtLevel(int levelId) throws Exception;
		
		/**
		 * Average segment length for particular annotator
		 */
		Double GetAverageSegmentLengthForAnnot(int annotId) throws Exception;
		/**
		 * Average number of segments for particular annotator
		 */
		Integer GetNumberOfSegmentsForAnnot(int annotId) throws Exception;
		
		ArrayList<Integer> GenerateRandomReferenceBreaks( int numBreaks );
		
		void OutputFullText(File outputFile, ArrayList<Integer> breaks) throws Exception;
		void OutputBreaksOnly(File outputFile, ArrayList<Integer> breaks) throws Exception;
		
		/**
		 * returns the level of segmentation - sentence or paragraph (see IGenericDataSource)
		 */
		int GetLevelOfSegm();
		
		/**
		 * sets the level of segmentation
		 * @param segmLevel: 1 sentences, 2 paragraphs
		 * @throws Exception 
		 */
		void SetLevelOfSegm(int segmLevel) throws Exception;

}
