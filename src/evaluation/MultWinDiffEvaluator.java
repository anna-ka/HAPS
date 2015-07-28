package evaluation;

import java.util.ArrayList;

/**
 * Evaluator class to use different flavors of WindowDiff when multiple annotations available. 
 * The actual code is from the old code base, so it is pretty messy.
 * @author anna
 *
 */
public class MultWinDiffEvaluator extends AbstractEvaluator {
	
	public static enum MULTWD_TYPE{MULTWD_NORM, MULTWD_UNNORM, MAJORITY_OPINION};
	
	protected  MULTWD_TYPE metricType = MULTWD_TYPE.MULTWD_UNNORM;

	public MULTWD_TYPE GetMetricType() {
		return metricType;
	}

	public void SetMetricType(MULTWD_TYPE metricType) {
		this.metricType = metricType;
	}

	@Override
	public int SpecificVerifyCompatibility() {
		int result = this.NON_COMPATIBLE;
		if (
				( this.refDS.getClass().getName().compareTo("datasources.SimpleFileMultipleAnnotsDataSource") == 0 &&
				this.hypoDS.getClass().getName().compareTo("datasources.SimpleFileMultipleAnnotsDataSource") == 0)
				|| 
				(this.refDS.getClass().getName().compareTo("datasources.ConnexorXMLMultipleAnnotsDS") == 0 &&
				this.hypoDS.getClass().getName().compareTo("datasources.ConnexorXMLMultipleAnnotsDS") == 0)
			)
		{
			int numAnnot = this.hypoDS.GetNumberOfAnnotators();
			if (numAnnot == 1)
				
			{
				result = this.COMPATIBLE;
			}
			else
			{
				System.out.println("MultWinDiffEvaluator.SpecificVerifyCompatibility failed: incorrect number of hypo annotations " +  numAnnot);
			}
		}
		else
		{
			System.out.println("MultWinDiffEvaluator.SpecificVerifyCompatibility failed: wrong classes " );
			System.out.println("ref class:  " +  this.refDS.getClass().getName());
			System.out.println("hypo class:  " +  this.hypoDS.getClass().getName());
		}
		return result;
	}

	@Override
	public Double ComputeValue() throws Exception {
		Double value = new Double (-1);
		
		if (this.metricType.equals(MultWinDiffEvaluator.MULTWD_TYPE.MAJORITY_OPINION))
		{
			value = this.EvalMajority();
		}
		else if (this.metricType.equals(MultWinDiffEvaluator.MULTWD_TYPE.MULTWD_UNNORM))
		{
			value = this.EvalMultWD();
		}
		else // normalized
		{
			value = this.EvalMultWDNormalized();
		}
		return value;
	}
	
	public Double EvalMultWD() throws Exception
	{
		int[] totalErrorArray = new int [this.refDS.GetNumberOfAnnotators()];
		int totalError = 0;
		double aveSegmLength = this.refDS.GetAverageSegmentLength();
		int winSize = (int)(Math.round ( aveSegmLength / (double)2)  );
		
		if (winSize >= this.refDS.GetNumberOfChunks() )
		{
			winSize = this.refDS.GetNumberOfChunks()/ 2;
		}
		
		int[] hypo = new int[this.refDS.GetNumberOfChunks()];
		int[][] ref = new int[this.refDS.GetNumberOfChunks()][this.refDS.GetNumberOfAnnotators() ];
		
		for (int i = 0; i < this.refDS.GetNumberOfChunks(); i++)
		{
			hypo[i] = 0;
			for (int a = 0; a < this.refDS.GetNumberOfAnnotators(); a++)
			{
				ref[i][a] = 0;
			}
		}
		Integer[] hypoSegm;
		try{
			ArrayList<Integer> hBreaks = this.hypoDS.GetReferenceBreaks(1);
			hypoSegm = new Integer[hBreaks.size()];
			hypoSegm = hBreaks.toArray(hypoSegm);
		}
		catch (Exception e)
		{
			String msg = "failed to get 1 and only hypo segmentation";
			throw (new Exception(e.getMessage() + " " + msg));
		}
		
		for (int h = 0; h < hypoSegm.length; h++)
		{
			Integer curBr = hypoSegm[h];
			hypo[curBr.intValue()] = 1;
		}
		
		
		for (int a = 0; a < this.refDS.GetNumberOfAnnotators(); a++)
		{
			ArrayList<Integer> curRef = this.refDS.GetReferenceBreaks(a + 1);
					
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
		
		for (int a = 0; a < this.refDS.GetNumberOfAnnotators(); a++)
		{
			totalErrorArray[a] = 0;
//			System.out.println("Annot " + String.valueOf(a));
			
			for (int i = 0; i <= this.refDS.GetNumberOfChunks() - winSize; i++)
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
		
		int denom =  (hypo.length - winSize ) * this.refDS.GetNumberOfAnnotators();
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
	
	/**
	 * a method to compute upper and lower bounds for multWD
	 * @returnresult.get(0) = lower bound (best case scenario), result.get(1) = upper bound (worst case)
	 * @throws Exception
	 */
		public ArrayList<Double> ComputeMultWdBounds() throws Exception
		{
			ArrayList<Double> result = new ArrayList<Double>();
			
			double aveSegmLength = this.refDS.GetAverageSegmentLength();
			int winSize = (int)(Math.round ( aveSegmLength / (double) 2)  );
			
			System.out.println("winSize " + String.valueOf(winSize));
			
			int numPossibleOpinions = winSize + 1;
			int numWindows = this.refDS.GetNumberOfChunks() - winSize + 1;
			
			int[][]opinions = new int[numWindows][numPossibleOpinions];
			
			for (int i = 0; i < numPossibleOpinions; i++)
			{
				for (int j = 0 ; j < numWindows; j++)
				{
					opinions[j][i] = 0;
				}
			}
			
			
			
			//create references in binary form
			int[][] ref = new int[this.refDS.GetNumberOfChunks()][this.refDS.GetNumberOfAnnotators() ];	
			for (int j = 0; j < this.refDS.GetNumberOfChunks(); j++)
			{
				for (int a = 0; a < this.refDS.GetNumberOfAnnotators(); a++)
				{
					ref[j][a] = 0;
				}
			}
			
			for (int a = 0; a < this.refDS.GetNumberOfAnnotators(); a++)
			{
				ArrayList<Integer> curRef = this.refDS.GetReferenceBreaks(a + 1);
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
				
				for (int a = 0; a < this.refDS.GetNumberOfAnnotators(); a++)
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
			
			int denom =  numWindows * this.refDS.GetNumberOfAnnotators();
			
			//compute upper bound
			for (int i = 0; i < numWindows; i++)
			{
				//find the most popular opinion in this window
				int mostPopularOpinionSupport = 0;
				int leastPopularOpinionSupport = this.refDS.GetNumberOfAnnotators();
				
				for (int j = 0; j < numPossibleOpinions; j++)
				{
					if ( opinions[i][j] > mostPopularOpinionSupport )
						mostPopularOpinionSupport = opinions[i][j];
					if ( opinions[i][j] < leastPopularOpinionSupport )
						leastPopularOpinionSupport = opinions[i][j];
				}
				
				// if you guess the best possible option, this is the least possible penalty
				int numMinorityOpinions = this.refDS.GetNumberOfAnnotators() - mostPopularOpinionSupport;
				netMinorityOpinions += numMinorityOpinions;
				
				//if you guess the worst possible option, this is the maximum possible penalty
				int numRegularOpinions = this.refDS.GetNumberOfAnnotators() - leastPopularOpinionSupport;
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

		/**
		 * this method evaluates the result by comparing hypo segmentation against the majority opinion among all annotators
		 * @return
		 */
		public Double EvalMajority() throws Exception
		{
			int [] refSequence = new int[this.refDS.GetNumberOfChunks()];
			for (int i = 0; i < refSequence.length; i++)
			{
				Integer curPosition = new Integer (i);
				int counter = this.refDS.GetNumberOfAnnotators();
				int half = (int)Math.floor( this.refDS.GetNumberOfAnnotators() / 2 );
				
				int numIncluded = 0;
				
				for (int a = 1; a <= this.refDS.GetNumberOfAnnotators(); a++)
				{
					ArrayList<Integer> curRef = this.refDS.GetReferenceBreaks(a);
							
					if (curRef.contains(curPosition))
					{	
						//counter--;
						numIncluded++;
					}
				}
				
//				if ( counter <= half )
				if ( numIncluded >= half )
					//all annotators included this break
				{
					refSequence[i] = 1;
				}
				else
					refSequence[i] = 0;
			}
			
			
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < refSequence.length; i++)
			{
				if (refSequence[i] == 0)
					continue;
				str.append(i);
				str.append(", ");
				
			}
			
			str.append("\n");
			System.out.println("reference seq: " + str.toString()); 
			
			return WinDiffSequences(refSequence);	
		}
	
		
		/**
		 * This method compares hypothetical segmentation with the given reference one. This method is useful
		 * to try different flavors of the reference segmentation composed of multiple opinions
		 * (e.g., majority opinion, union, intersection)
		 * @param ref a sequence of breaks to compare against.
		 * @return
		 */
		private Double WinDiffSequences(int[] ref) throws Exception
		{
			int numSegments = 0;
			ArrayList<Integer> hBreaks = this.hypoDS.GetReferenceBreaks(1);
			Integer[] hypoSegm = new Integer[hBreaks.size()];
			hypoSegm = hBreaks.toArray(hypoSegm);
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
			
//			System.out.println("id\tref\thypo");
//			for (int i = 0; i < ref.length; i++)
//			{
//				System.out.println(String.valueOf(i) + "\t" + String.valueOf(ref[i]) + "\t" + String.valueOf(hypo[i]));
//			}
//			System.out.println("***");
			
			int winSize = (int) Math.round(ref.length / (numSegments * 2));
			if (winSize == 0)
				winSize = 1;
			
//			System.out.println("\twinSize " + String.valueOf(winSize));
			
			if (numSegments <= 1 ) //|| winSize >= ref.length
			{
				winSize = (int) ref.length / 2;
			}
//			System.out.println("\twinSize " + String.valueOf(winSize));
			
			int start;
			int end;
			
			int totalError = 0;
			
			for (int i = 0; i <= ref.length - winSize; i++)
			{
				start = i;
				end = start + winSize - 1;
				
				int refCounter = 0;
				int hypoCounter = 0;
//				System.out.println("win start " + String.valueOf(start) + " and end " + String.valueOf(end));
				
				for (int j = start; j <= end; j++)
				{
//					System.out.println("\tcomparing " + String.valueOf(j));
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
//					System.out.println("MISMATCH: ref breaks: " + String.valueOf(refCounter) + " hypo breaks: " + String.valueOf(hypoCounter));
					totalError++;
				}
			}//end looping over windows
			
			int denom = ref.length - winSize;
//			
//			System.out.println("WinDiff total error " + String.valueOf(totalError));
//			System.out.println("WinDiff N " + String.valueOf(ref.length));
//			System.out.println("WinDiff winSize " + String.valueOf(winSize));
//			System.out.println("WinDiff norm factor " + String.valueOf(denom));
			
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
		

}
