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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import similarity.IGenericSimComputer;
import similarity.ISimComputer;
import similarity.TripletSim;

public abstract class AbstractAPSegmenterDP implements ISegmenter {

	
	public abstract void Init(ISimComputer simComputer) throws Exception;
	public abstract void Init(IGenericSimComputer simComputer) throws Exception;

	//number of points corresponds to the number of conceptual data points (sentences), not the number of similarities
	protected int numPoints;
	protected double dampFactor = 0.9; // used 0.9
	protected int maxIterations = 1000;//500;
	protected int maxConvCount = 100;
	
	protected IMatrix similarities; 
	

	protected IMatrix resp;  // responsibilities
	protected IMatrix avail; //availabilities
	protected ArrayList<Integer> examplars = null;
	protected int convergenceCount = 0;
	protected TreeMap<Integer, TreeSet<Integer>> assignments = null;
	
	protected double maxSim = 0.0;
	protected double minSim = 0.0;
	
	private int numberOfThreads = Runtime.getRuntime().availableProcessors();
	
	private class WorkerThread extends Thread {
		private AbstractAPSegmenterDP parent;
		private int startRow, startCol;
		private int endRow, endCol;
		private boolean calcResp;
		public WorkerThread(AbstractAPSegmenterDP seg, boolean calcResponsibility, int start, int end) {
			parent = seg;
			startRow = startCol = start;
			endRow = endCol = end;
			calcResp = calcResponsibility;
		}
		public void run() {
			try
			{
				if (calcResp)
				{
					int [] respCache = new int[2];
					for (int i = startRow; i <= endRow; i++)
					{
						respCache[0] = respCache[1] = -1;
						int rowStart = parent.similarities.GetRowStart(i);
						int rowEnd = parent.similarities.GetRowEnd(i);
						for (int j = rowStart; j <= rowEnd; j++)
						{
							parent.CalcResponsibility(i, j, respCache);
						}
					}
				}
				else
				{				
					for (int j = startCol; j <= endCol; j++)
					{
						int colStart = parent.similarities.GetColumnStart(j);
						int colEnd = parent.similarities.GetColumnEnd(j);
						for (int i = colStart; i <= colEnd; i++)
						{
							parent.CalcAvailability(i, j);
						}
					}
				}
			}
			catch (Exception ex)
			{
				System.out.println("Exception in worker thread" + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
	private Thread[] threads = new WorkerThread[this.numberOfThreads];

	public int GetExamplarsCount()
	{
		return this.examplars.size();
	}
	
	public IMatrix GetSimilarities() {
		return similarities;
	}
	
	/**
	 * This method adds the similarities specified in newSims to the already loaded similarity matrix.
	 * @param newSims
	 */
	public void ModifySimMatrix(IGenericSimComputer newSims) throws Exception
	{
		IMatrix newM = newSims.GetSimilarities();

		//fill in the similarities matrix
//		for (int i = 0; i < newM.GetNumRows(); i++) {
//			for (int j = newM.GetRowStart(i); j <= newM.GetRowEnd(i); j++) {
		for (int i = 0; i < this.similarities.GetNumRows(); i++) {
			for (int j = this.similarities.GetRowStart(i); j <= this.similarities.GetRowEnd(i); j++) {
				try{
					double oldValue = this.similarities.GetElement(i, j);
					double newValue = newM.GetElement(i, j);
					newValue = newValue + oldValue;
					this.similarities.SetElement(i, j, newValue);
				}
				catch (Exception e)
				{
					String msg = "Exception in ModifySimMatrix when processing row " + i + " col " + j;
					System.out.println(msg);
					System.out.println(e.getMessage());
					//e.printStackTrace();
				}
			}
		}
	}
	
	void LoadSimilarities(String strFileName) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(strFileName));
		
		String s;
		while ((s = reader.readLine()) != null)
		{
			String [] str = s.split("\\s+");
			if (str.length != 3)
				throw new Exception("bad line in " + strFileName + "\n" + s);
			
			int row = Integer.parseInt(str[0]) ;
			int col = Integer.parseInt(str[1]) ;
			double value = Double.parseDouble(str[2]);
			if (value < this.minSim)
				this.minSim = value;
			if (value > this.maxSim)
				this.maxSim = value;
			
			this.similarities.SetElement(row, col, value);
		}
	}
	
	void LoadPreferences(String strFileName) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(strFileName));
		
		String s;
		int nIndex = 0;
		while ((s = reader.readLine()) != null)
		{
			double value = Double.parseDouble(s);
			
			this.similarities.SetElement(nIndex, nIndex, value);
			nIndex++;
		}
	}
	
	public void SetPreferences(double commonPrefValue) throws Exception
	{
		for (int i = 0; i < this.similarities.GetNumRows(); i++)
		{
			this.similarities.SetElement(i,i, commonPrefValue);
		}
	}
	
	protected boolean Iterate() throws Exception
	{
		int sliceSize = this.similarities.GetNumRows() / this.numberOfThreads;
		int sliceLeft = this.similarities.GetNumRows() % this.numberOfThreads;
		
		//calculate responsibilities
		for (int i = 0; i < this.numberOfThreads; i++)
		{
			int startRow = i * sliceSize;
			int endRow = startRow + sliceSize - 1;
			
			if (i == this.numberOfThreads - 1)
				endRow += sliceLeft;
			
			threads[i] = new WorkerThread(this, true, startRow, endRow);
			threads[i].start();
		}
		
		for (int i = 0; i < this.numberOfThreads; i++)
		{
			threads[i].join();
		}
		
		
		//calculate availabilites
		for (int i = 0; i < this.numberOfThreads; i++)
		{
			int startRow = i * sliceSize;
			int endRow = startRow + sliceSize - 1;
			
			if (i == this.numberOfThreads - 1)
				endRow += sliceLeft;
			
			threads[i] = new WorkerThread(this, false, startRow, endRow);
			threads[i].start();
		}

		for (int i = 0; i < this.numberOfThreads; i++)
		{
			threads[i].join();
		}

//		this.PrintMatrix(this.resp.GetMatrix(), this.avail.GetMatrix());
		
		ArrayList<Integer> newExamplars = GetExamplars();
		
		if (this.examplars == null || 
				this.examplars.size() != newExamplars.size() ||
				this.examplars.size() == 0 ||
			!this.examplars.containsAll(newExamplars))
		{
			this.examplars = newExamplars;
			this.SetCurrentConvergenceCount (0);
		}
		else
		{
			this.IncrementConvergenceCount();
		}
		
		if (this.convergenceCount > this.getMaxConvCount())
		{
			//this.PrintMatrix(this.resp.GetMatrix(), this.avail.GetMatrix());
			
//			System.out.println("examplars: " +  newExamplars.toString());
//			System.out.println("Converged!");
			return true;
		}
		else
			return false;
	}
	
	
	double DampenMessage(double oldMsg, double newMsg)
	{
		double msg = this.dampFactor * oldMsg + (1 - this.dampFactor) * newMsg;
		return msg;
	}
	

	//protected abstract void CalcResponsibility(int src, int dst) throws Exception;
	//this method is called with indices based on dimensions of the matrix [numPoints][numPoints]
	protected void CalcResponsibility(int src, int dst, int [] cache) throws Exception
	{
		double max = Double.NEGATIVE_INFINITY;
		 
		int rowStart = this.similarities.GetRowStart(src);
		int rowEnd = this.similarities.GetRowEnd(src);
		
		if (cache[0] == -1)
		{
			double highest_sum = Double.NEGATIVE_INFINITY;
			double second_highest_sum = Double.NEGATIVE_INFINITY;
			
			for (int col = rowStart; col <= rowEnd; col++)
			{
				double sum = this.avail.GetElement(src, col) + this.similarities.GetElement(src, col);
				if (sum > highest_sum)
				{
					second_highest_sum = highest_sum;
					highest_sum = sum;
					
					cache[1] = cache[0];
					cache[0] = col;
				}
				else if (sum > second_highest_sum)
				{
					second_highest_sum = sum;
					cache[1] = col;
				}
			}
		}
		
		int nMaxIndex = -1;
		if (cache[0] == dst)
			nMaxIndex = cache[1];
		else
			nMaxIndex = cache[0];
		
		max = this.avail.GetElement(src, nMaxIndex) + this.similarities.GetElement(src, nMaxIndex);
		
		double response = this.similarities.GetElement(src, dst) - max;
		this.resp.SetElement(src, dst, DampenMessage( this.resp.GetElement(src, dst), response) );
	}
	
	//protected abstract void CalcAvailability(int i, int j) throws Exception;
	//this method is called with indices based on dimensions of the matrix [numPoints][numPOints]
	protected void CalcAvailability(int i, int j) throws Exception
	{
		if (i == j)
			CalcA1(i);
		else if (i < j)
			CalcA2(i, j);
		else
			CalcA3(i, j);
	}
	
	//i == j
	void CalcA1(int j) throws Exception
	{
		int firstSimIndex = this.similarities.GetColumnStart(j);
		int lastSimIndex = this.similarities.GetColumnEnd(j);
		
		double sum1 = MaximizeSumRL(j, firstSimIndex, j-1);
		double sum2 = MaximizeSumLR(j, j + 1, lastSimIndex);
		double newA =  Math.max(sum1, 0.0) + Math.max(sum2, 0.0) ;
		this.avail.SetElement(j,j, DampenMessage(this.avail.GetElement(j, j), newA) );
	}
	
	// i <j
	void CalcA2(int i, int j) throws Exception
	{
		int firstSimIndex = this.similarities.GetColumnStart(j);
		int lastSimIndex = this.similarities.GetColumnEnd(j); 
		
		//first min arg
		double d4 = Math.max(0, this.MaximizeSumRL(j, firstSimIndex, i-1));
		double d5 = this.Sum(j, i+1, j);
		double d6 = Math.max(0, this.MaximizeSumLR(j, j+1, lastSimIndex));
		double firstArg = d4 + d5 + d6;
		
		//second min arg
		double sum1 = d4;
//		double d1 = this.MinimizeSumRL(j, i+1, j-1);
		double d1 = this.MinimizeSumBeforeStart(j, i+1, j-1);
		double d2 = this.Sum(j, i+1, j-1);
		double f2 = Math.min(d1, d2);
		
		double secondArg = sum1 + f2;
		
		double newA = Math.min(firstArg, secondArg);
		
		this.avail.SetElement(i, j, DampenMessage( this.avail.GetElement(i, j), newA) );
	}

	//i > j
	void CalcA3(int i, int j) throws Exception
	{
		int firstSimIndex = this.similarities.GetColumnStart(j);
		int lastSimIndex = this.similarities.GetColumnEnd(j); 
		
		//first min arg
		double d3 = Math.max(this.MaximizeSumRL(j, firstSimIndex, j-1), 0);
		double d4 = this.Sum(j, j, i-1);
		double d5 = Math.max(0, this.MaximizeSumLR(j, i+1, lastSimIndex));
		
		double firstArg = d3 + d4 + d5;
		
		//second min arg
		double sum1 = d5;
		//double d1 = this.MinimizeSumLR(j, j+1, i-1);
		double d1 = this.MinimizeSumAfterEnd(j, j+1, i-1);
		
		double d2 = this.Sum(j, j+1, i-1);
		double sum2 = Math.min(d1, d2);
		
		double secondArg = sum1 + sum2;
		
		double newMsg = Math.min(firstArg, secondArg);
		
		this.avail.SetElement(i, j, DampenMessage( this.avail.GetElement(i, j), newMsg) );
	}
	
	//maximize sum going going from right to left
	double MaximizeSumRL(int j, int left, int right) throws Exception
	{
		double maxSum = 0.0;
		double sum = 0.0;
		
		int curIndex = right;
		while(curIndex >= left && curIndex >= 0
				)
		{

			sum = sum + this.resp.GetElement(curIndex, j);
//			if (curIndex == right)
//				maxSum = sum;
//			else if (sum >= maxSum)
//				maxSum = sum;
			if (sum >= maxSum)
				maxSum = sum;

			curIndex--;
		}

		return maxSum;
	}
	
	double MaximizeSumRLWrong(int j, int left, int right) throws Exception
	{
		double maxSum = 0.0;
		double sum = 0.0;
		
		int curIndex = right;
		while(curIndex >= left && curIndex >= 0)
		{
			sum +=  this.resp.GetElement(curIndex, j);
			if (curIndex == right)
				maxSum = sum;
			else if (sum >= maxSum)
				maxSum = sum;

			curIndex--;
		}

		return maxSum;
	}
	
	//maximize sum going going from left to right
	double MaximizeSumLR(int j, int left, int right) throws Exception
	{
		double maxSum = 0.0;

		double sum = 0.0;
		int curIndex = left;
		while(curIndex <= right && curIndex < this.numPoints)
		{
			sum = sum + this.resp.GetElement(curIndex, j);
//			if (curIndex == left)
//				maxSum = sum;
//			else if (sum >= maxSum)
//				maxSum = sum;
			if (sum >= maxSum)
				maxSum = sum;
			curIndex++;
		}

		return maxSum;
	}
	double MaximizeSumLRWrong(int j, int left, int right) throws Exception
	{
		double maxSum = 0.0;

		double sum = 0.0;
		int curIndex = left;
		while(curIndex <= right && curIndex < this.numPoints)
		{
			sum +=  this.resp.GetElement(curIndex, j);
			if (curIndex == left)
				maxSum = sum;
			else if (sum >= maxSum)
				maxSum = sum;
			curIndex++;
		}

		return maxSum;
	}
	
	double MinimizeSumBeforeStart(int j, int left, int right) throws Exception
	{
		double minSum = this.Sum(j, left, right);
		double curSum = minSum;
		
		int curIndex = right;
		while (curIndex >= left)
		{
			double curResp = this.resp.GetElement(curIndex, j);
			curSum = curSum - curResp;
			if (curSum < minSum)
				minSum = curSum;
			curIndex--;
		}
		
		return minSum;
	}
	
	/**
	 * a method to find a minimizing configuration of sums of responsibilities for the tail of the segment.
	 * Here we are looking to find e, j <= e < i, such that it minimizes sums between e+1 and i-1, inclusive
	 * 
	 * @param j
	 * @param left
	 * @param right
	 * @return
	 * @throws Exception
	 */
	double MinimizeSumAfterEnd(int j, int left, int right) throws Exception
	{
		double minSum = this.Sum(j, left, right);
		double curSum = minSum;
		
		int curIndex = left;
		while (curIndex <= right)
		{
			double curResp = this.resp.GetElement(curIndex, j);
			curSum = curSum - curResp;
			if (curSum < minSum)
				minSum = curSum;
			curIndex++;
		}
	
		return minSum;
	}
	
	double MinimizeSumLRWrong(int j, int left, int right) throws Exception
	{
		double sum = 0.0;
		double minSum = 0.0;
		
		int curIndex = left;
		while (curIndex < this.numPoints && curIndex <= right)
		{
			sum += this.resp.GetElement(curIndex, j);
			if (curIndex == left)
				minSum = sum;
			else if (sum <= minSum )
				minSum = sum;

			curIndex++;
		}
		return minSum;
	}
	
	double MinimizeSumRL(int j, int left, int right) throws Exception
	{
		double sum = 0.0;
		double minSum = 0.0;
		
		int curIndex = right;
		while (curIndex >= 0 && curIndex >= left )
		{
			sum += this.resp.GetElement(curIndex, j);
			if (curIndex == right)
				minSum = sum;
			else if (sum <= minSum )
				minSum = sum;

			curIndex--;
		}
		return minSum;
	}
	
	double Sum(int j, int start, int end) throws Exception
	{
		if (start < 0 || start >= this.numPoints)
			return 0;
		
		double sum = 0;
			
		for (int k = start; k <= end && k < this.numPoints; k ++)
		{
			sum += this.resp.GetElement(k, j);
		}
		
		return sum;
	}

	public void Run() throws Exception
	{
//		System.out.println("using " + String.valueOf(this.numberOfThreads) + " processors");
		int iteration = 1;
		while (!this.Iterate() && iteration <= this.maxIterations)
		{
			iteration++;
		}
	}
	
	ArrayList<Integer> GetExamplars() throws Exception
	{
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < this.numPoints; i++)
		{
			if (this.avail.GetElement(i, i) + this.resp.GetElement(i, i) > 0)
				list.add(i);
		}
		return list;
	}
	
	//this is a method to resolve conflicting assignments
	//possible in loopy belief propagation
	//it basically is a half-iteration of k-means
	//each point is assigned to the nearest exmplar among the points that are self-examplars
	Integer GetNearestExamplar(int pointIndex, ArrayList<Integer> examplars) throws Exception
	{
		double maxSim = Double.NEGATIVE_INFINITY;
		Integer nearestExamplar = new Integer (-1);
		for (Integer examplar: examplars)
		{
			try
			{
				double curSim = this.similarities.GetElement(pointIndex, examplar.intValue());
				if ( curSim > maxSim )
					nearestExamplar = examplar;
			}
			//the similarity for this pair was -INF
			catch (Exception e)
			{
				System.out.println("Exception in GetNearestExamplar ");
				e.printStackTrace();
			}
		}
		return nearestExamplar;
		
	}
	
	//given a data point, finds the closest preceding examplar and the closest following one
	Integer[] GetNeighborExamplars (int pointIndex, ArrayList<Integer> examplars) throws Exception
	{
		Integer[] neighborExamplars = new Integer[2];
		neighborExamplars[0] = new Integer(-1);
		neighborExamplars[1] = new Integer(-1);
		
		for (Integer curExamplar: examplars)
		{
			if (curExamplar <= pointIndex)
			{
				neighborExamplars[0] = curExamplar;
			}
			else
			{
				neighborExamplars[1] = curExamplar;
				break;
			}
				
		}
		
		return neighborExamplars;
	}
	
	public TreeMap<Integer, TreeSet<Integer>> GetAssignments() throws Exception
	{
		TreeMap<Integer, TreeSet<Integer>> assigns = new TreeMap<Integer, TreeSet<Integer>>();
		
		for (int j = 0; j < this.getNumPoints(); j++)
		{
			Integer examplar = new Integer ( this.GetMostLikelyExamplar(j) );
			//this examplar is already in the map
			if (assigns.containsKey(examplar))
			{
				assigns.get(examplar).add(new Integer(j));
			}
			else//new examplar
			{
				TreeSet<Integer> children = new TreeSet<Integer>();
				children.add( new Integer(j) );
				assigns.put(examplar, children);
			}
		}
		
		this.assignments = assigns;
		return this.assignments;
	}
	
	public TreeMap<Integer, TreeSet<Integer>> GetNonConflictingAssignments() throws Exception
	{
		//first return all assignments
		TreeMap<Integer, TreeSet<Integer>> badAssigns = this.GetAssignments();
		
		//assignments corrected for conflicts
		TreeMap<Integer, TreeSet<Integer>> newAssigns = new TreeMap<Integer, TreeSet<Integer>>();
		
		//all points labelled as examplars
		ArrayList<Integer> examplars = this.GetExamplars();
		
		if (examplars == null || examplars.size() <= 0)
		{
			System.out.println("NO EXAMPLARS FOUND");
			return null;
		}
		
		for (int curPoint = 0; curPoint < this.getNumPoints(); curPoint++)
		{
			
			try {
				Integer selectedEx = this.GetMostLikelyExamplar(curPoint);
				if ( examplars.contains( selectedEx))
				{
					if (newAssigns.containsKey(selectedEx))
					{
						newAssigns.get(selectedEx).add(new Integer(curPoint));
					}
					else//new examplar
					{
							TreeSet<Integer> children = new TreeSet<Integer>();
							children.add( new Integer(curPoint) );
							newAssigns.put(selectedEx, children);
					}
					continue;
				}
				
				Integer[] neighborExamplars = this.GetNeighborExamplars(curPoint, examplars);
				
				Integer precExamplar = neighborExamplars[0];
				Integer followingExamplar = neighborExamplars[1];
				
				double simPrec = Double.NEGATIVE_INFINITY;
				double simFollow = Double.NEGATIVE_INFINITY;
				TreeSet<Integer> precAssignments = new TreeSet<Integer>();
				TreeSet<Integer> followAssignments = new TreeSet<Integer>();
				
				if (precExamplar > -1)
				{
					try{
						simPrec = this.similarities.GetElement(curPoint, precExamplar.intValue());
						precAssignments = badAssigns.get(precExamplar);
					}
					//this exception can fire if preceding examplar is not in the window of similarities for this point
					catch (Exception e)
					{
					}
				}
				else
					simPrec = Double.NEGATIVE_INFINITY;
				
				if (followingExamplar > -1)
				{
					try{
						simFollow = this.similarities.GetElement(curPoint, followingExamplar.intValue());
						followAssignments = badAssigns.get(followingExamplar);
					}
					catch (Exception e)
					{
						System.out.println("point " + String.valueOf(curPoint ) + "cannot be assigned to examplar " + precExamplar.toString() );
					}
				}
				else
					simFollow = Double.NEGATIVE_INFINITY;
				
				selectedEx = -1;
				
				//consider the preceding examplar
				if ( simPrec >= simFollow )
				{
					//check for assignments violating linearity
					if (followingExamplar > -1  && followAssignments.size() > 0 )
					{
						Integer firstChild = followAssignments.first();
						//no violation
						if (firstChild > curPoint)
						{
							selectedEx = precExamplar;
						}
						else //violation
						{
							selectedEx = followingExamplar;
						}
					}
					else
					{
						selectedEx = precExamplar;
					}
				}
				else // simFollow > simPrec
				{
					//check for linearity violations
					if (precExamplar > -1  && precAssignments.size() > 0 )
					{
						Integer lastChild = precAssignments.last();
						//no violation
						if (lastChild < curPoint)
						{
							selectedEx = followingExamplar;
						}
						else
						{
							selectedEx = precExamplar;
						}
					}
					else
					{
						selectedEx = followingExamplar;
					}
					
				}
				
				
				//do the assignment
				if (newAssigns.containsKey(selectedEx))
				{
					newAssigns.get(selectedEx).add(new Integer(curPoint));
				}
				else//new examplar
				{
						TreeSet<Integer> children = new TreeSet<Integer>();
						children.add( new Integer(curPoint) );
						newAssigns.put(selectedEx, children);
				}
				continue;
			} 
			catch (Exception e) 
			{
				System.out.println("Exception in GetNonConflictingAssignments " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		this.assignments = newAssigns;
		return this.assignments;
	}
	
	/**
	 * Returns an array with 0-based indices of the identified segment breaks
	 * @param assigns a set of non-conflicting assignemnts
	 * @return an array with 0-based indices of the identified segment breaks
	 */
	public Integer[] GetHypoBreaks(TreeMap<Integer, TreeSet<Integer>> assigns)
	{
		if (assigns == null || assigns.isEmpty())
			return (new Integer[]{});
		TreeSet<Integer> sortedBreaks = new TreeSet<Integer>();
		for (Integer examplar: assigns.keySet() )
		{
			
			TreeSet<Integer> children = assigns.get(examplar);
			Integer lastChild = children.last();
			sortedBreaks.add(lastChild);
		}
		//remove last break
		//Integer last = sortedBreaks.last();
		//sortedBreaks.remove(last);
		
		
		//String[] x = (String[]) v.toArray(new String[0]);

		Integer[] ar = (Integer[]) sortedBreaks.toArray(new Integer[0]);
		
//		for (Integer br: ar)
//		{
//			System.out.println("Hypo break after " + br.toString());
//		}
		
		return ar;
	}
	
	public void PrintAssignments()
	{
		if ( this.assignments == null )
		{
			System.out.println("Assignments have not been initialized");
			return;
		}
		for (Integer examplar: this.assignments.keySet())
		{
			System.out.print("\nExamplar " + examplar.toString() + ": ");
			for (Integer child: this.assignments.get(examplar) )
			{
				System.out.print(child.toString() + ", ");
			}
		}
		System.out.println();
	}
	
	
	/*returns the point with highest avail+resp
	 * even if it is not labelled as an examplar
	 * */
	int GetMostLikelyExamplar(int index) throws Exception
	{
		double dMax = Double.NEGATIVE_INFINITY;
		int examplar = -1;
		
		int rowStart = this.similarities.GetRowStart(index);
		int rowEnd = this.similarities.GetRowEnd(index);
		
		for (int i = rowStart; i <= rowEnd; i++)
		{
			//if (i == nIndex)
			//	continue;
			
			double d = this.resp.GetElement(index, i) + this.avail.GetElement(index, i) ;
			if (d > dMax)
			{
				examplar = i;
				dMax = d;
			}
		}
		
		return examplar;
	}
	
	public void AddNoise() throws Exception
	{
		double[][] noiseMatrix = new double[this.similarities.GetNumRows()][this.similarities.GetNumColumns()];
		this.CreateNoiseMatrix(noiseMatrix);
		for (int r = 0; r < this.similarities.GetNumRows(); r++)
		{
			for (int c = 0; c < this.similarities.GetNumColumns(); c++)
			{
				double v = this.similarities.GetElement(r, c) + noiseMatrix[r][c];
				this.similarities.SetElement(r, c, v);
			}
		}
		noiseMatrix = null;
		System.out.print("done");
	}
	
	public void PrintMatrix(double[][] matrix1, double[][]matrix2)
	{
		for (int r = 0; r < matrix1.length; r++)
		{
			double max = 0.0;
			int maxIndex = -1;
			System.out.print("ROW " + String.valueOf(r) + ":\n\tResp: ");
			for (int c = 0; c < matrix1[0].length; c++)
			{
				int abstractCol = c + this.similarities.GetRowStart(r);
				String label = String.format(" : %.3f, ", Float.valueOf(String.valueOf( matrix1[r][c] )));
				System.out.print(String.valueOf(abstractCol) + label);
			}
			System.out.print("\n\tAvail: ");
			for (int c = 0; c < matrix2[0].length; c++)
			{
				int abstractCol = c + this.similarities.GetRowStart(r);
				String label = String.format(" : %.3f, ", Float.valueOf(String.valueOf( matrix2[r][c] )));
				System.out.print(String.valueOf(abstractCol) + label);
			}
			System.out.print("\n\tSum: ");
			int abstractMaxIndex = -1;
			for (int c = 0; c < matrix2[0].length; c++)
			{
				int abstractCol = c + this.similarities.GetRowStart(r);
				double sum = matrix2[r][c] + matrix1[r][c];
				if (c == 0)
				{
					max = sum;
					maxIndex = c;
				}
				else if (sum > max)
				{
					max = sum;
					maxIndex = c;
				}
				abstractMaxIndex = maxIndex + this.similarities.GetRowStart(r);
				String label = String.format(" : %.3f, ", Float.valueOf(String.valueOf( sum )));
				System.out.print(String.valueOf(abstractCol) + label);
			}
			String l = String.format(" : %.3f at %d ", Float.valueOf(String.valueOf( max )), abstractMaxIndex );
			System.out.println("\n\tMax: " +  l);
		}
	}
	
	private void CreateNoiseMatrix(double[][] noiseMatrix)
	{
		for (int row = 0; row < noiseMatrix.length; row++)
		{
			for (int c = 0; c < noiseMatrix.length; c++)
			{
				double e = 1e-12;
				double r = Math.random();
				double dif = this.maxSim - this.minSim;
				double answer = e * r * dif;
				noiseMatrix[row][c] = answer;
			}
		}
	}

	int GetCurrentConvergenceCount()
	{
		return this.convergenceCount;
	}
	
	void SetCurrentConvergenceCount (int value)
	{
		this.convergenceCount = value;
	}
	
	void IncrementConvergenceCount()
	{
		this.convergenceCount++;
	}

	public int getNumPoints() {
		return numPoints;
	}

	public void setNumPoints(int numPoints) {
		this.numPoints = numPoints;
	}

	public double getDampFactor() {
		return dampFactor;
	}

	public void setDampFactor(double dampFactor) {
		this.dampFactor = dampFactor;
	}

	public double getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}


	public int getMaxConvCount() {
		return maxConvCount;
	}


	public void setMaxConvCount(int maxConvCount) {
		this.maxConvCount = maxConvCount;
	}
}
