package segmenter;

import java.util.ArrayList;
import java.util.TreeMap;

import datasources.IDataSource;
import datasources.IDataSourceMultipleAnnots;
import datasources.NoSuchAnnotatorException;

public class SegmEvaluator implements ISegmEvaluator {
	
	private IDataSourceMultipleAnnots refDS = null;
	private IDataSource hypoDS = null;
	private TreeMap<Integer, ArrayList<Integer>> refBreaks = null;
	
	
	public SegmEvaluator()
	{super();};

	@Override
	public void Init(IDataSourceMultipleAnnots refDs) {
		this.refDS = refDs;
		this.refBreaks = refDs.GetReferenceSegmentBreaks();

	}

	@Override
	public void SetUp(IDataSource hypoDs) {
		this.hypoDS = hypoDs;

	}
	
	@Override
	public Double EvaluateAll(int mode) throws Exception {
		if (mode == ISegmEvaluator.INTERSECTION)
			return EvalIntersection();
		else if (mode == ISegmEvaluator.MAJORITY)
			return EvalMajority();
		else if (mode == ISegmEvaluator.MULT_WD)
			return EvalMultWD();
		else if (mode == ISegmEvaluator.MULT_WD_NORM)
			return EvalMultWDNormalized();
		else if (mode == ISegmEvaluator.MULT_WD_BEST)
			return GetMultWDBestCase();
		else if (mode == ISegmEvaluator.MULT_WD_WORST)
			return GetMultWDWorstCase();
		else {
			System.out.println("Error in SegmEvaluator.EvaluateAll: unknown mode");
			return null;
		}
	}
	
	public Double EvalIntersection()
	{
		int [] refSequence = new int[this.refDS.GetNumChunks()];
		for (int i = 0; i < refSequence.length; i++)
		{
			Integer curPosition = new Integer (i);
			int counter = this.refDS.GetNumAnnotators();
			
			for (int a = 1; a <= this.refDS.GetNumAnnotators(); a++)
			{
				ArrayList<Integer> curRef = this.refBreaks.get( new Integer(a) );
				if (curRef.contains(curPosition))
					counter--;
			}
			
			if ( counter <= 0 )
				//all annotators included this break
			{
				refSequence[i] = 1;
			}
			else
				refSequence[i] = 0;
		}
		
		return WinDiffSequences(refSequence);	
	}
	
	public Double EvalMajority()
	{
		int [] refSequence = new int[this.refDS.GetNumChunks()];
		for (int i = 0; i < refSequence.length; i++)
		{
			Integer curPosition = new Integer (i);
			int counter = this.refDS.GetNumAnnotators();
			int half = (int)Math.floor( this.refDS.GetNumAnnotators() / 3 );
			
			int numIncluded = 0;
			
			for (int a = 1; a <= this.refDS.GetNumAnnotators(); a++)
			{
				ArrayList<Integer> curRef = this.refBreaks.get(new Integer(a));
				if (curRef.contains(curPosition))
				{	
					//counter--;
					numIncluded++;
				}
			}
			
//			if ( counter <= half )
			if ( numIncluded >= half )
				//all annotators included this break
			{
				refSequence[i] = 1;
			}
			else
				refSequence[i] = 0;
		}
		
		return WinDiffSequences(refSequence);	
	}

	@Override
	public Double EvaluateSingle(int annotId) throws NoSuchAnnotatorException{
		int [] refSequence = new int[this.refDS.GetNumChunks()];
		ArrayList<Integer> curSeq = null;
		
		curSeq = this.refBreaks.get(new Integer(annotId));
		
//		try
//		{
//			curSeq = this.refBreaks.get(new Integer(annotId));
//		}
//		catch (Exception e)
//		{
//			NoSuchAnnotatorException exc = new NoSuchAnnotatorException ("There is no annotator " + String.valueOf(annotId) + " in " + this.refDS.GetName());
//			throw exc;
//		}
		
		if (curSeq == null)
		{
			NoSuchAnnotatorException exc = new NoSuchAnnotatorException ("There is no annotator " + String.valueOf(annotId) );
			throw exc;
		}
		
		for (int i = 0; i < refSequence.length; i++)
		{
			Integer curPosition = new Integer(i);
			if (curSeq.contains(i))
				refSequence[i] = 1;
			else
				refSequence[i] = 0;
		}
		return WinDiffSequences(refSequence);	
	}
	
	//compares hypothetical segmentation with the given reference one
	private Double WinDiffSequences(int[] ref)
	{
		int numSegments = 0;
		Integer[] hypoSegm = this.hypoDS.GetReferenceSegmentBreaks();
		int[] hypo = new int[ref.length];
		
		for (int i = 0; i < ref.length; i++)
		{
			if (ref[i] == 1)
				numSegments++;
			hypo[i] = 0;
		}
		
		for (int h = 0; h < hypoSegm.length; h++)
		{
			Integer curBr = hypoSegm[h];
			hypo[curBr.intValue()] = 1;
		}
		
//		System.out.println("id\tref\thypo");
//		for (int i = 0; i < ref.length; i++)
//		{
//			System.out.println(String.valueOf(i) + "\t" + String.valueOf(ref[i]) + "\t" + String.valueOf(hypo[i]));
//		}
//		System.out.println("***");
		
		int winSize = (int) Math.round(ref.length / numSegments);
//		System.out.println("\twinSize " + String.valueOf(winSize));
		
		if (numSegments <= 1 ) //|| winSize >= ref.length
		{
			winSize = (int) ref.length / 2;
		}
//		System.out.println("\twinSize " + String.valueOf(winSize));
		
		int start;
		int end;
		
		int totalError = 0;
		
		for (int i = 0; i <= ref.length - winSize; i++)
		{
			start = i;
			end = start + winSize - 1;
			
			int refCounter = 0;
			int hypoCounter = 0;
//			System.out.println("win start " + String.valueOf(start) + " and end " + String.valueOf(end));
			
			for (int j = start; j <= end; j++)
			{
//				System.out.println("\tcomparing " + String.valueOf(j));
				if (ref[j] == 1)
				{
					refCounter++;
					//System.out.println("\tfound REF");
				}
				if (hypo[j] == 1)
				{
					hypoCounter++;
					//System.out.println("\tfound HYPO");
				}
				
			}
			
			if ( refCounter != hypoCounter)
			{
//				System.out.println("MISMATCH: ref breaks: " + String.valueOf(refCounter) + " hypo breaks: " + String.valueOf(hypoCounter));
				totalError++;
			}
		}//end looping over windows
		
		int denom = ref.length - winSize;
//		
//		System.out.println("WinDiff total error " + String.valueOf(totalError));
//		System.out.println("WinDiff N " + String.valueOf(ref.length));
//		System.out.println("WinDiff winSize " + String.valueOf(winSize));
//		System.out.println("WinDiff norm factor " + String.valueOf(denom));
		
		Double result = new Double (1);
		try 
		{
			 result =  new Double ( (double) totalError / (double)denom );
		}
		catch (Exception e)
		{
			System.out.print("Exception in SegmEvaluator.WDSequences: " + e.getMessage());
			e.printStackTrace();
			//return result;
		}
		finally
		{
		return result;
		}
		
	}
	
	public Double GetMultWDWorstCase() throws Exception
	{
		Double worst = new Double(-1);
		
		ArrayList<Double> bounds = this.ComputeMultWdBounds();
		worst = bounds.get(1);
		return worst;
	}
	
	public Double GetMultWDBestCase() throws Exception
	{
		Double best = new Double(-1);
		
		ArrayList<Double> bounds = this.ComputeMultWdBounds();
		best = bounds.get(0);
		return best;
	}
	
	
	public Double EvalMultWDNormalized() throws Exception
	{
		Double multWD = EvalMultWD();
		ArrayList<Double> bounds = this.ComputeMultWdBounds();
		Double bestCase = bounds.get(0);
		Double worstCase = bounds.get(1);
		Double range = worstCase - bestCase;
		
		Double normWD = (multWD - bestCase) / range;
		return normWD;
	}
	
	public Double EvalMultWD()
	{
		int[] totalErrorArray = new int [this.refDS.GetNumAnnotators()];
		int totalError = 0;
		int aveSegmLength = this.refDS.GetAverageSegmLength();
		int winSize = (int)(Math.round ( (double)aveSegmLength / (double)2)  );
		
		if (winSize >= this.refDS.GetNumChunks() )
		{
			winSize = this.refDS.GetNumChunks() / 2;
		}
		
		int[] hypo = new int[this.refDS.GetNumChunks()];
		int[][] ref = new int[this.refDS.GetNumChunks()][this.refDS.GetNumAnnotators() ];
		
		for (int i = 0; i < this.refDS.GetNumChunks(); i++)
		{
			hypo[i] = 0;
			for (int a = 0; a < this.refDS.GetNumAnnotators(); a++)
			{
				ref[i][a] = 0;
			}
		}
		
		Integer[] hypoSegm = this.hypoDS.GetReferenceSegmentBreaks();
		for (int h = 0; h < hypoSegm.length; h++)
		{
			Integer curBr = hypoSegm[h];
			hypo[curBr.intValue()] = 1;
		}
		
		
		for (int a = 0; a < this.refDS.GetNumAnnotators(); a++)
		{
			ArrayList<Integer> curRef = this.refBreaks.get(new Integer(a + 1));
			for (Integer curBr: curRef)
			{
				ref[curBr.intValue()][a] = 1;
			}
		}
		
//		System.out.print("id\thypo");
//		for (int a = 0;  a < this.refDS.GetNumAnnotators(); a++)
//		{
//			System.out.print("\tannot" + String.valueOf(a));
//		}
//		System.out.println();
//		
//		for (int i = 0; i < ref.length; i++)
//		{
//			System.out.print(String.valueOf(i) + "\t" + String.valueOf(hypo[i]) ) ;
//			for (int a = 0;  a < this.refDS.GetNumAnnotators(); a++)
//			{
//				System.out.print("\t" + String.valueOf(ref[i][a]));
//			}
//			System.out.println();
//		}
//		System.out.println("\n");
//		System.out.println("***");
		
		int start;
		int end;
		
		for (int a = 0; a < this.refDS.GetNumAnnotators(); a++)
		{
			totalErrorArray[a] = 0;
//			System.out.println("Annot " + String.valueOf(a));
			
			for (int i = 0; i <= this.refDS.GetNumChunks() - winSize; i++)
			{
				start = i;
				end = start + winSize - 1;
				
				int refCounter = 0;
				int hypoCounter = 0;
//				System.out.println("\twin start " + String.valueOf(start) + " and end " + String.valueOf(end));
				
				for (int j = start; j <= end; j++)
				{
					//System.out.println("\t\tcomparing " + String.valueOf(j));
					if (ref[j][a] == 1)
					{
						refCounter++;
						//System.out.println("\t\tfound REF");
					}
					if (hypo[j] == 1)
					{
						hypoCounter++;
						//System.out.println("\t\tfound HYPO");
					}
					
				}
				
				if ( refCounter != hypoCounter)
				{
//					System.out.println("\tMISMATCH: ref breaks: " + String.valueOf(refCounter) + " hypo breaks: " + String.valueOf(hypoCounter));
					totalErrorArray[a]++;
					totalError++;
				}
			}
		}
		
		Double result = new Double(1);
		
		int denom =  (hypo.length - winSize ) * this.refDS.GetNumAnnotators();
//		
//		System.out.println("MULT WD total error " + String.valueOf(totalError));
//		System.out.println("MULT WD N " + String.valueOf(ref.length));
//		System.out.println("MULT WD winSize " + String.valueOf(winSize));
//		System.out.println("MULT WD norm factor " + String.valueOf(denom));
		
		try 
		{
			 result =  new Double ( (double) totalError / (double) denom );
		}
		catch (Exception e)
		{
			System.out.print("Exception in SegmEvaluator.WDSequences: " + e.getMessage());
			e.printStackTrace();
		}

		return result;
		
	}
	
	//a method to compute upper and lower bounds for multWD
	//result.get(0) = lower bound (best case scenario)
	//result.get(1) = upper bound (worst case)
	public ArrayList<Double> ComputeMultWdBounds() throws Exception
	{
		ArrayList<Double> result = new ArrayList<Double>();
		
		int aveSegmLength = this.refDS.GetAverageSegmLength();
		int winSize = (int)(Math.round ( (double)aveSegmLength / (double) 2)  );
		
		System.out.println("winSize " + String.valueOf(winSize));
		
		int numPossibleOpinions = winSize + 1;
		int numWindows = this.refDS.GetNumChunks() - winSize + 1;
		
		int[][]opinions = new int[numWindows][numPossibleOpinions];
		
		for (int i = 0; i < numPossibleOpinions; i++)
		{
			for (int j = 0 ; j < numWindows; j++)
			{
				opinions[j][i] = 0;
			}
		}
		
		
		
		//create references in binary form
		int[][] ref = new int[this.refDS.GetNumChunks()][this.refDS.GetNumAnnotators() ];	
		for (int j = 0; j < this.refDS.GetNumChunks(); j++)
		{
			for (int a = 0; a < this.refDS.GetNumAnnotators(); a++)
			{
				ref[j][a] = 0;
			}
		}
		
		for (int a = 0; a < this.refDS.GetNumAnnotators(); a++)
		{
			ArrayList<Integer> curRef = this.refBreaks.get(new Integer(a + 1));
			for (Integer curBr: curRef)
			{
				ref[curBr.intValue()][a] = 1;
			}
		}
		
		//create a table of opinions for each window position
		for (int i = 0; i <  numWindows; i++)
		{
			int start = i;
			int end = start + winSize - 1;
			
			for (int a = 0; a < this.refDS.GetNumAnnotators(); a++)
			{
				int refCounter = 0;
				//count the number of boundaries for this annotator for this window
				for (int w = start; w <= end; w++)
				{
					if ( ref[w][a] == 1 )
					{
						refCounter++;
					}
				}
				opinions[start][refCounter]++;
			}
			
		}
		
		int netMinorityOpinions = 0;
		int netRegularOpinions = 0; // here it means the number supporting opinions other than the least popular one
		
		int denom =  numWindows * this.refDS.GetNumAnnotators();
		
		//compute upper bound
		for (int i = 0; i < numWindows; i++)
		{
			//find the most popular opinion in this window
			int mostPopularOpinionSupport = 0;
			int leastPopularOpinionSupport = this.refDS.GetNumAnnotators();
			
			for (int j = 0; j < numPossibleOpinions; j++)
			{
				if ( opinions[i][j] > mostPopularOpinionSupport )
					mostPopularOpinionSupport = opinions[i][j];
				if ( opinions[i][j] < leastPopularOpinionSupport )
					leastPopularOpinionSupport = opinions[i][j];
			}
			
			// if you guess the best possible option, this is the least possible penalty
			int numMinorityOpinions = this.refDS.GetNumAnnotators() - mostPopularOpinionSupport;
			netMinorityOpinions += numMinorityOpinions;
			
			//if you guess the worst possible option, this is the maximum possible penalty
			int numRegularOpinions = this.refDS.GetNumAnnotators() - leastPopularOpinionSupport;
			netRegularOpinions += numRegularOpinions;
		}
		
		Double bestCase = new Double ( 0);
		Double worstCase = new Double ( 0);
		
		try{
			
			bestCase = (double) netMinorityOpinions / (double) denom;
			worstCase = (double) netRegularOpinions / (double) denom;
			
			result.add(bestCase);
			result.add(worstCase);
			
			System.out.println ("BEST multWD: " + bestCase.toString() + " WORST multWD: " + worstCase.toString());
			
		}
		catch (Exception e)
		{
			throw ( new Exception ("Exception in ComputeMultWdBounds: " + e.getMessage()));
		}
		
		return result;
	}

	

}
