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

public class AffinityPropagationSegmenterSparse extends AbstractAPSegmenterDP {
	
	private int winSize = 0;

	/**
	 * This method is obsolete. Use Init(IGenericSImComputer instead)
	 */
	public void Init(ISimComputer simComputer) throws Exception{
		this.numPoints = simComputer.GetPointsNumber();
		this.winSize = simComputer.GetWindowSize();
		
		this.similarities = new WindowMatrix(this.numPoints, this.winSize);
		//by default the similarities matrix is initialized to -INF 
		//so if a similarity is not specified, two points are infinitely dissimilar
		for (int i = 0; i < this.similarities.GetNumRows(); i++)
		{
			for (int j = 0; j < this.similarities.GetNumColumns(); j++)
				this.similarities.SetElement(i, j, Double.NEGATIVE_INFINITY);
			
			//also initialize row and columns starts and ends to -1
			this.similarities.SetRowStart(i, -1);
			this.similarities.SetRowEnd(i, -1);
			this.similarities.SetColumnStart(i, -1);
			this.similarities.SetColumnEnd(i, -1);
		}
		
		
		
		this.resp = new WindowMatrix(this.numPoints, this.winSize);
		this.avail = new WindowMatrix(this.numPoints, this.winSize);
		
		int lastRow = -1; 
		int lastCol = -1;
		TripletSim prevTriplet = null;
		
		//fill in the similarities matrix
		for ( TripletSim sim : simComputer.GetSimilarities() )
		{
			//check row index
			if (sim.firstId < lastRow )
			{
				String msg = "Exception in AffinityPropagationPSegmenterSparse.Init: row index out of order: " + sim.ToString();
				throw (new Exception (msg));
			}
			if (sim.secondId < lastCol )
			{
				String msg = "Exception in AffinityPropagationPSegmenterSparse.Init: col index out of order: " + sim.ToString();
				throw (new Exception (msg));
			}
			//this is the first similarity for a new row 
			if (sim.firstId > lastRow)
			{
				this.similarities.SetRowStart(sim.firstId, sim.secondId);
				//remember last valid index in previous row 
				if (lastRow != -1)
				{
					this.similarities.SetRowEnd(lastRow, prevTriplet.secondId);
				}
				lastRow = sim.firstId;
				lastCol = sim.secondId;
			}
			
			//check if this is the first element in this column
			int oldIndex = this.similarities.GetColumnStart(sim.secondId);
			if (oldIndex == -1)
				this.similarities.SetColumnStart(sim.secondId, sim.firstId);
			
			//check if this is the last element in the column
			int oldEndIndex = this.similarities.GetColumnEnd(sim.secondId);
			if (oldEndIndex == -1)
				this.similarities.SetColumnEnd(sim.secondId, sim.firstId);
			else if (oldEndIndex < sim.firstId)
				this.similarities.SetColumnEnd(sim.secondId, sim.firstId);
			
			this.similarities.SetElement(sim.firstId, sim.secondId, sim.similarity);
			prevTriplet = sim;
			//System.out.println( sim.ToString() );
		}
		
		//record row end for the last row
		this.similarities.SetRowEnd(prevTriplet.firstId, prevTriplet.secondId);
		
		//copy row starts/ends and column starts/ends for availability and resp matrices
		for (int i = 0; i < this.similarities.GetNumRows(); i++)
		{
			this.avail.SetRowStart(i, this.similarities.GetRowStart(i));
			this.avail.SetRowEnd(i, this.similarities.GetRowEnd(i));
			this.avail.SetColumnStart(i, this.similarities.GetColumnStart(i));
			this.avail.SetColumnEnd(i, this.similarities.GetColumnEnd(i));
			
			this.resp.SetRowStart(i, this.similarities.GetRowStart(i));
			this.resp.SetRowEnd(i, this.similarities.GetRowEnd(i));
			this.resp.SetColumnStart(i, this.similarities.GetColumnStart(i));
			this.resp.SetColumnEnd(i, this.similarities.GetColumnEnd(i));
			
		}
		
		
//		System.out.println("Created SparseSegmenter.");
	}
	
	
	/**
	 * Initializes the segmenter using a list of BOW sentence vectors for the source document
	 * @param simComputer
	 * @throws Exception
	 */
	public void Init(IGenericSimComputer simComputer) throws Exception{
		this.numPoints = simComputer.GetPointsNumber();
		this.winSize = simComputer.GetWindowSize();
		
		this.similarities = new WindowMatrix(this.numPoints, this.winSize);
		//by default the similarities matrix is initialized to -INF 
		//so if a similarity is not specified, two points are infinitely dissimilar
		for (int i = 0; i < this.similarities.GetNumRows(); i++)
		{
			for (int j = 0; j < this.similarities.GetNumColumns(); j++)
				this.similarities.SetElement(i, j, Double.NEGATIVE_INFINITY);
			
			//also initialize row and columns starts and ends to -1
			this.similarities.SetRowStart(i, -1);
			this.similarities.SetRowEnd(i, -1);
			this.similarities.SetColumnStart(i, -1);
			this.similarities.SetColumnEnd(i, -1);
		}
		
		
		this.resp = new WindowMatrix(this.numPoints, this.winSize);
		this.avail = new WindowMatrix(this.numPoints, this.winSize);
		
		IMatrix sims = simComputer.GetSimilarities();

		for (int i = 0; i < sims.GetNumRows(); i++) {
			this.similarities.SetRowStart(i, sims.GetRowStart(i));
			this.similarities.SetRowEnd(i, sims.GetRowEnd(i));

			this.avail.SetRowStart(i, sims.GetRowStart(i));
			this.avail.SetRowEnd(i, sims.GetRowEnd(i));
			
			this.resp.SetRowStart(i, sims.GetRowStart(i));
			this.resp.SetRowEnd(i, sims.GetRowEnd(i));
		}
		
		for (int j = 0; j < sims.GetNumRows(); j++) {
			this.similarities.SetColumnStart(j, sims.GetColumnStart(j));
			this.similarities.SetColumnEnd(j, sims.GetColumnEnd(j));

			this.avail.SetColumnStart(j, sims.GetColumnStart(j));
			this.avail.SetColumnEnd(j, sims.GetColumnEnd(j));
			
			this.resp.SetColumnStart(j, sims.GetColumnStart(j));
			this.resp.SetColumnEnd(j, sims.GetColumnEnd(j));
		}
		
		for (int i = 0; i < sims.GetNumRows(); i++) {
			for (int j = sims.GetRowStart(i); j <= sims.GetRowEnd(i); j++) {
				this.similarities.SetElement(i, j, sims.GetElement(i, j));
			}
		}

//		
//		int lastRow = -1; 
//		int lastCol = -1;
//		TripletSim prevTriplet = null;
//		
//		//fill in the similarities matrix
//		for ( TripletSim sim : simComputer.GetSimilarities() )
//		{
//			//check row index
//			if (sim.firstId < lastRow )
//			{
//				String msg = "Exception in AffinityPropagationPSegmenterSparse.Init: row index out of order: " + sim.ToString();
//				throw (new Exception (msg));
//			}
//			if (sim.secondId < lastCol )
//			{
//				String msg = "Exception in AffinityPropagationPSegmenterSparse.Init: col index out of order: " + sim.ToString();
//				throw (new Exception (msg));
//			}
//			//this is the first similarity for a new row 
//			if (sim.firstId > lastRow)
//			{
//				this.similarities.SetRowStart(sim.firstId, sim.secondId);
//				//remember last valid index in previous row 
//				if (lastRow != -1)
//				{
//					this.similarities.SetRowEnd(lastRow, prevTriplet.secondId);
//				}
//				lastRow = sim.firstId;
//				lastCol = sim.secondId;
//			}
//			
//			//check if this is the first element in this column
//			int oldIndex = this.similarities.GetColumnStart(sim.secondId);
//			if (oldIndex == -1)
//				this.similarities.SetColumnStart(sim.secondId, sim.firstId);
//			
//			//check if this is the last element in the column
//			int oldEndIndex = this.similarities.GetColumnEnd(sim.secondId);
//			if (oldEndIndex == -1)
//				this.similarities.SetColumnEnd(sim.secondId, sim.firstId);
//			else if (oldEndIndex < sim.firstId)
//				this.similarities.SetColumnEnd(sim.secondId, sim.firstId);
//			
//			this.similarities.SetElement(sim.firstId, sim.secondId, sim.similarity);
//			prevTriplet = sim;
//			//System.out.println( sim.ToString() );
//		}
//		
//		//record row end for the last row
//		this.similarities.SetRowEnd(prevTriplet.firstId, prevTriplet.secondId);
		
		//copy row starts/ends and column starts/ends for availability and resp matrices
		for (int i = 0; i < this.similarities.GetNumRows(); i++)
		{
			this.avail.SetRowStart(i, this.similarities.GetRowStart(i));
			this.avail.SetRowEnd(i, this.similarities.GetRowEnd(i));
			this.avail.SetColumnStart(i, this.similarities.GetColumnStart(i));
			this.avail.SetColumnEnd(i, this.similarities.GetColumnEnd(i));
			
			this.resp.SetRowStart(i, this.similarities.GetRowStart(i));
			this.resp.SetRowEnd(i, this.similarities.GetRowEnd(i));
			this.resp.SetColumnStart(i, this.similarities.GetColumnStart(i));
			this.resp.SetColumnEnd(i, this.similarities.GetColumnEnd(i));
			
		}
	
	

}

}