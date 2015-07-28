package evaluation;

import java.util.Comparator;

public class EvalResultSetComparator implements Comparator<EvalResultSet> {

	/**
	 * EvalResultSet instances are compared using the average value of the metric.
	 */
	//@Override
	public int compare(EvalResultSet result1, EvalResultSet result2) throws ClassCastException{
		Double ave1 , ave2;
		
		try{
			ave1 = result1.GetAverageValue();
			ave2 = result2.GetAverageValue();
		}
		catch (Exception e)
		{
			ClassCastException a = new ClassCastException ("Something went wrong when fetching average values");
			throw (a);
		}
		
		return Double.compare(ave1.doubleValue(), ave2.doubleValue());
	}

}
