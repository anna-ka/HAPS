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

/**a class for evaluating linear segmenters
 * 
 */
package segmenter;

import datasources.IDataSource;
import datasources.IDataSourceMultipleAnnots;


/**
 * @author anna
 *
 */
public class LinearEvaluator {
	
	//possible modes of comparing the number of reference and hypothetical segm breaks in windiff
	public static enum WINDIFF_MODE {REGULAR, FN, FP};
	int[] refIndices;
	int[] hypoIndices;
	IDataSource dataSource;
	
	public LinearEvaluator ()
	{
		
	}
	
	public void Init(IDataSource data, Integer[] hypo)
	{
		Integer[] ref = data.GetReferenceSegmentBreaks();
		this.refIndices = new int[ref.length];
		this.hypoIndices = new int[hypo.length];
		this.dataSource = data;
		
		for (int i = 0; i < ref.length; i++)
		{
			this.refIndices[i] = ref[i].intValue();
		}
		for (int j = 0; j < hypo.length; j++)
		{
			this.hypoIndices[j] = hypo[j].intValue();
		}
	}
	
	public void PrintBreaks()
	{
		System.out.println("REference breaks:");
		for (int i: this.refIndices)
			System.out.print(String.valueOf(i) + ", ");
		
		System.out.println("\nHypothetical breaks:");
		for (int j: this.hypoIndices)
			System.out.print(String.valueOf(j) + ", ");
	}
	
	
	public String evaluate()
	{
		if (this.refIndices == null || this.refIndices.length <= 0)
		{
			System.out.println("Error: reference indices are invalid.");
			return "unable to evaluate";
		}
		else if (this.hypoIndices == null || this.hypoIndices.length <=  0)
		{
			System.out.println("Error: no hypothetical indices are provided.");
			return "unable to evaluate";
		}
		float winDiff = ComputeWinDiff(WINDIFF_MODE.REGULAR);
		float precWinDiff = ComputeWinDiff(WINDIFF_MODE.FP);
		float recallWinDiff =  ComputeWinDiff(WINDIFF_MODE.FN);
		
		String result = "Regular WinDiff: " + String.valueOf(winDiff);
		result += "\nFalse positives only WinDIff: " + String.valueOf(precWinDiff);
		result += "\nFalse negatives only WinDIff: " + String.valueOf(recallWinDiff);
		return result;
	}
	
	public float ComputeWinDiff(WINDIFF_MODE mode)
	{
		//compute average segment length using reference segmentation
		double aveSegmLength = 0;
		
		if (this.refIndices == null || this.refIndices.length <= 0)
		{
			System.out.println("No reference indices");
			return 0;
		}
		
		if (this.hypoIndices == null || this.hypoIndices.length <= 0)
		{
			System.out.println("No hypo indices");
			return 0;
		}
		
		aveSegmLength = Math.floor( this.dataSource.GetNumChunks() / this.refIndices.length );	
		//System.out.println("average reference segm length: " + String.valueOf(aveSegmLength));
		
		double winTmp = Math.floor( aveSegmLength / 2);
		
		int windowSize = new Double(winTmp).intValue();
		
//		System.out.println("average reference segm length: " + String.valueOf(aveSegmLength) + ". Windiff window size " + String.valueOf(windowSize));
		
		//slide the window and compute the number of windows where the number
		//of hypothetical and reference segment boundaries are different
		int totalMismatches = 0;
		for (int winStart = 0; winStart < this.dataSource.GetNumChunks() - windowSize + 1; winStart++)
		{
			int winEnd = winStart + windowSize - 1;
			//count the number of ref segments
			int refCount = 0;
			int hypoCount = 0;
			//TO DO: make sure that <= and >= signs are counted properly
			
//			System.out.println("WindowStart:\t" + String.valueOf(winStart));
//			System.out.println("WindowEnd:\t" + String.valueOf(winEnd));
			
			//count the number of reference segm breaks in this window
			for (int r = 0; r < this.refIndices.length - 1; r++)
			{
				int curRefIndex = this.refIndices[r];
				if (curRefIndex >= winStart && curRefIndex < winEnd)
				{
					refCount++;
//					System.out.println("\tref break:\t" + String.valueOf(curRefIndex));
				}
				else if (curRefIndex > winEnd)	
					break;
			}
			
			//count the number of hypothetical segm breaks in this window
			for (int h = 0; h < this.hypoIndices.length - 1; h++)
			{
				int curHypoIndex = this.hypoIndices[h];
				if (curHypoIndex >= winStart && curHypoIndex < winEnd)
				{
					hypoCount++;
//					System.out.println("\thypo break:\t" + String.valueOf(curHypoIndex));
				}
				else if (curHypoIndex > winEnd)
					break;
			}
			
			boolean isMismatched = this.CompareNumberOfBreaks(refCount, hypoCount, mode);
			
			if ( isMismatched )
			{
//				System.out.println("Mismatch!");
				totalMismatches++;
			}
			
			
			//System.out.println("\twindow " + String.valueOf(winStart) + " to "  + String.valueOf(winEnd));
//			System.out.println("\t\tref: " + String.valueOf(refCount) + "; hypo: "  + String.valueOf(hypoCount));
//			System.out.println("\tmismatches so far: " + String.valueOf(totalMismatches) );
		}
		
		//divide by (number of chunks - window size)
		int chunkCount = this.dataSource.GetNumChunks();
		int denom = (chunkCount - windowSize);
		
		float winDiff =  ( (float)totalMismatches ) / ( (float) denom );
//		System.out.println("\tNET mismatches: " + String.valueOf(totalMismatches) );
//		System.out.println("\tnum chunks: " + String.valueOf(chunkCount) );
//		System.out.println("\tdenom: " + String.valueOf(denom) );
		//System.out.println("\tnum chunks: " + String.valueOf(chunkCount) );
		
		return winDiff;
	}
	
	//value true increases the number of mismatches found
	private boolean CompareNumberOfBreaks(int refCount, int hypoCount, WINDIFF_MODE mode)
	{
		if (mode == WINDIFF_MODE.REGULAR)
		{
//			System.out.println("regular. ref: " + String.valueOf(refCount) + " hypo: " + String.valueOf(hypoCount));
			if (refCount != hypoCount)
				return true;
			else
				return false;
		}
		//increase counter only if number of hypothetical breaks > reference breaks
		else if (mode == WINDIFF_MODE.FP)
		{
			//System.out.println("precision. ref: " + String.valueOf(refCount) + " hypo: " + String.valueOf(hypoCount));
			if (refCount < hypoCount)
				return true;
			else
				return false;
		}
		//increase counter only if number of hypothetical breaks < reference breaks
		else if (mode == WINDIFF_MODE.FN)
		{
			//System.out.println("recall. ref: " + String.valueOf(refCount) + " hypo: " + String.valueOf(hypoCount));
			if (refCount > hypoCount)
				return true;
			else
				return false;
		}
		else
		{
			System.out.println("ERROR: INVALID WINDIFF_MODE");
			return false;
		}
	}
	
	

}
