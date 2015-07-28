package evaluation;

import java.util.ArrayList;


/**
 * This class compares to SimpleFileDataSource instances and evaluates the hypothetical segmentation one using WindowDiff metric.
 * @author anna
 *
 */
public class SimpleWinDiffEvaluator extends AbstractEvaluator {

	/**
	 * This method computes windowDiff between hypothetical and reference segmentations. 
	 * @return windowDiff value
	 */
	public Double ComputeValue() throws Exception{
		
		ArrayList<Integer> refIndices = this.refDS.GetReferenceBreaks(0);
		ArrayList<Integer> hypoIndices = this.hypoDS.GetReferenceBreaks(0);
		
		if (this.VerifyCompatibility() != this.COMPATIBLE)
		{
			Exception e = new Exception ("Exeption in SimpleWiinDiffEvaluator.ComputeValue(). VerifyCompatibility() failed.");
			throw (e);
		}
		if (this.SpecificVerifyCompatibility() != this.COMPATIBLE)
		{
			Exception e = new Exception ("Exeption in SimpleWiinDiffEvaluator.ComputeValue(). SpecificVerifyCompatibility() failed.");
			throw (e);
		}
		
		//window size is half the average segment length in the reference segmentation
		int winSize = (int) Math.floor( this.refDS.GetAverageSegmentLength() / 2 );
		
		int totalMismatches = 0;
		for (int winStart = 0; winStart < this.refDS.GetNumberOfChunks() - winSize + 1; winStart++)
		{
			int winEnd = winStart + winSize - 1;
			int refCount = 0;
			int hypoCount = 0;
			
			for (int j = winStart;  j < winEnd; j++)
			{
				if (refIndices.contains(new Integer(j)))
					refCount++;
				if (hypoIndices.contains(new Integer(j)))
					hypoCount++;
			}
			if (refCount != hypoCount)
			{
				totalMismatches++;
			}
		}
		
		double wd = (double)totalMismatches / (double) (this.refDS.GetNumberOfChunks() - winSize)  ;
		return new Double (wd);
		
	}

	/**
	 * 
	 */
	public int SpecificVerifyCompatibility() {
		int result = this.NON_COMPATIBLE;
		if (this.refDS.getClass().getName().compareTo("datasources.SimpleFileDataSource") == 0 &&
				this.hypoDS.getClass().getName().compareTo("datasources.SimpleFileDataSource") == 0	)
		{
			result = this.COMPATIBLE;
		}
		else if (this.refDS.getClass().getName().compareTo("datasources.ConnexorXMLSimpleDS") == 0 &&
				this.hypoDS.getClass().getName().compareTo("datasources.ConnexorXMLSimpleDS" +
						"") == 0)
		{
			result = this.COMPATIBLE;
		}
		return result;
	}

}
