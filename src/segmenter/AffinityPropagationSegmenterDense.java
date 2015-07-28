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


package segmenter;

import similarity.IGenericSimComputer;
import similarity.ISimComputer;
import similarity.TripletSim;


public class AffinityPropagationSegmenterDense extends AbstractAPSegmenterDP
{
	public AffinityPropagationSegmenterDense()
	{}

	
	/**
	 * This method is obsolete. Use Init(IGenericSimComputer simComputer) instead.
	 * @see Init(IGenericSimComputer simComputer)
	 */
	public void Init(ISimComputer simComputer) throws Exception {
		
		this.numPoints = simComputer.GetPointsNumber();
		
		this.similarities = new DenseMatrix(this.numPoints);
		//by default the similarities matrix is initialized to -INF 
		//so if a similarity is not specified, two points are infinitely dissimilar
		for (int i = 0; i < this.similarities.GetNumRows(); i++)
		{
			for (int j = 0; j < this.similarities.GetNumColumns(); j++)
				this.similarities.SetElement(i, j, Double.NEGATIVE_INFINITY);
		}
		
		this.resp = new DenseMatrix(this.numPoints);
		this.avail = new DenseMatrix(this.numPoints);
		
		//fill in the similarities matrix
		for ( TripletSim sim : simComputer.GetSimilarities() )
		{
			this.similarities.SetElement(sim.firstId, sim.secondId, sim.similarity);
		}
		//System.out.println("Done");
	}
	
	public void Init(IGenericSimComputer simComputer) throws Exception {
		
		this.numPoints = simComputer.GetPointsNumber();
		
		this.similarities = new DenseMatrix(this.numPoints);
		//by default the similarities matrix is initialized to -INF 
		//so if a similarity is not specified, two points are infinitely dissimilar
		for (int i = 0; i < this.similarities.GetNumRows(); i++)
		{
			for (int j = 0; j < this.similarities.GetNumColumns(); j++)
				this.similarities.SetElement(i, j, Double.NEGATIVE_INFINITY);
		}
		
		this.resp = new DenseMatrix(this.numPoints);
		this.avail = new DenseMatrix(this.numPoints);
		
		IMatrix sims = simComputer.GetSimilarities();
		for (int i = 0; i < sims.GetNumRows(); i++) {
			for (int j = sims.GetRowStart(i); j <= sims.GetRowEnd(i); j++) {
				this.similarities.SetElement(i, j, sims.GetElement(i, j));
			}
		}
		
		//fill in the similarities matrix
//		for ( TripletSim sim : simComputer.GetSimilarities() )
//		{
//			this.similarities.SetElement(sim.firstId, sim.secondId, sim.similarity);
//		}
		//System.out.println("Done");
	}
	
//	protected void CalcResponsibility(int src, int dst) throws Exception
//	{
//		double max = Double.NEGATIVE_INFINITY;
//		
//		
//		
//		for (int i = 0; i < this.similarities.GetNumColumns(); i++)
//		{
//			if (i == dst)
//				continue;
//			
//			double sum = this.avail.GetElement(src, i) + this.similarities.GetElement(src, i);
//			if (sum > max)
//			{
//				max = sum;
//			}
//		}
//		
//		double response = this.similarities.GetElement(src, dst) - max;
//		this.resp.SetElement(src, dst, DampenMessage( this.resp.GetElement(src, dst), response) );
//	}
//	
//	protected void CalcAvailability(int i, int j) throws Exception
//	{
//		//System.out.println("AVAILABILITY  " + String.valueOf(i) + "," + String.valueOf(j));
//		if (i == j)
//			CalcA1(i);
//		else if (i < j)
//			CalcA2(i, j);
//		else
//			CalcA3(i, j);
//	}
//	

}
