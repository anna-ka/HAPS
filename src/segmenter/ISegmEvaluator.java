package segmenter;
import datasources.IDataSource;
import datasources.IDataSourceMultipleAnnots;
import datasources.NoSuchAnnotatorException;

public interface ISegmEvaluator {
	
	final static int MAJORITY = 100;
	final static int INTERSECTION = 101;
	final static int MULT_WD = 102;
	final static int MULT_WD_NORM = 103;
	final static int MULT_WD_BEST = 104;
	final static int MULT_WD_WORST = 105;
	final static int SINGLE_ANNOT = 106;
	
	public void Init(IDataSourceMultipleAnnots refDs);
	
	public void SetUp(IDataSource hypoDs);
	
	//evaluates against all available annotations
	public Double EvaluateAll(int mode) throws Exception;
	
	//evaluate against single annotator
	public Double EvaluateSingle(int annotId) throws NoSuchAnnotatorException;

}
