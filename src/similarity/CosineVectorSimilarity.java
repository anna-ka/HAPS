/*
 	APS - Affinity Propagation for Segmentation, a linear text segmenter.
 
    Copyright (C) 2011, Anna Kazantseva

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */


package similarity;

import com.aliasi.matrix.CosineDistance;
import com.aliasi.matrix.SparseFloatVector;

public class CosineVectorSimilarity implements ISimMetric {
	
	CosineDistance cos = new CosineDistance();
	
	public CosineVectorSimilarity()
	{
		
	}

//	public double MeasureSimilarity(SparseFloatVector v1, SparseFloatVector v2) {
//		double sim = this.cos.proximity(v1, v2);
//		
//		if (Double.compare(sim, Double.NaN) == 0)
//		{
//			//System.out.println("Warning: NaNcosine sim  " );
//			sim = 0.0;
//		}
//		
//		else if (sim < -1 || sim > 1)
//		{
//			//System.out.println("Warning: bad cosine sim ");
//			sim = 0.0;
//		}
//		
//		return sim;
//	}

	public double MeasureSimilarity(Object vector1, Object vector2) {
		SparseFloatVector v1 = (SparseFloatVector)vector1;
		SparseFloatVector v2 = (SparseFloatVector)vector2;
		
		double sim = this.cos.proximity(v1, v2);
		
		if (Double.compare(sim, Double.NaN) == 0)
		{
			//System.out.println("Warning: NaNcosine sim  " );
			sim = 0.0;
		}
		
		else if (sim < -1 || sim > 1)
		{
			//System.out.println("Warning: bad cosine sim ");
			sim = 0.0;
		}
		
//		sim = Math.exp(sim);
		
		return sim;
	}

}
