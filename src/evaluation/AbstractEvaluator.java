/**
 * 
 */
package evaluation;

import datasources.IGenericDataSource;

/**
 * @author anna
 *
 */
public abstract class AbstractEvaluator {
	
	public final static int COMPATIBLE = 0;
	public final static int NON_COMPATIBLE = -1;
	
	protected String metricName;
	
	public String GetMetricName() {
		return metricName;
	}

	public void SetMetricName(String metricName) {
		this.metricName = metricName;
	}

	protected IGenericDataSource refDS;
	protected IGenericDataSource hypoDS;
	protected Double metricValue;
	
	/**
	 * 
	 * @param metricName the name of the metric, e.g. windowDiff
	 */
	public void Init(String metricName)
	{
		this.metricName = metricName;
	}
	
	/**
	 * @param ds data source with reference (gold standard) annotations
	 */
	public void SetRefDS(IGenericDataSource ds)
	{
		this.refDS = ds;
	}
	
	/**
	 * @param ds data source with hypothetical annotations (usually produced by the automatic segmenter)
	 */
	public void SetHypoDS(IGenericDataSource ds)
	{
		this.hypoDS = ds;
	}
	
	/**
	 * this method must verify that the reference data source and the hypothetical data source are compatible: 
	 * they must be of the same class and have the same number of chunks.
	 * @return
	 */
	public int VerifyCompatibility()
	{
		int result = this.NON_COMPATIBLE;
		if (this.refDS.getClass() == this.hypoDS.getClass())
		{
			if (this.hypoDS.GetNumberOfChunks() == this.refDS.GetNumberOfChunks())
				result = this.COMPATIBLE;
		}
		return result;
	}
	
	/**
	 * Implementations of this method should do class specific checks.
	 * @return this.COMPATIBLE or this.NON_COMPATIBLE
	 */
	public abstract int SpecificVerifyCompatibility();
	
	/**
	 * Implementations of this method should report the value of evaluating hypothetical segmentations against the reference one.
	 * @return value of the metric
	 */
	public abstract Double ComputeValue() throws Exception;

}
