package similarity;

import java.util.LinkedList;
import java.util.TreeMap;

import segmenter.IMatrix;

/**
 * a class to hold a convenient representation of pairwise similarities
 * @author anna
 *
 */
public class MapOfSimilarities {
	
	TreeMap<Integer, TreeMap<Integer, Double>> sims = new TreeMap<Integer, TreeMap<Integer, Double>>();
	
	public MapOfSimilarities()
	{
		
	}
	
	public void Init(LinkedList<TripletSim> listOfSims)
	{
		this.sims = new TreeMap<Integer, TreeMap<Integer, Double>>();
		
		for (TripletSim curTriplet: listOfSims)
		{
			int firstInd = curTriplet.firstId;
			int secondInd = curTriplet.secondId;
			
			Double curSim = new Double (curTriplet.similarity);
			
			TreeMap<Integer, Double> simsForFirst = this.sims.get(firstInd);
			
			if (simsForFirst == null)
				simsForFirst = new TreeMap<Integer, Double>();
			
			simsForFirst.put(secondInd, curSim);
			this.sims.put(firstInd, simsForFirst);		
		}
	}
	
	public void Init(IMatrix sims) throws Exception
	{
		for (int i = 0; i < sims.GetNumRows(); i++)
		{
			for (int j = sims.GetRowStart(i); j <= sims.GetRowEnd(i); j++)
			{
				Double curSim = sims.GetElement(i, j);
				
				TreeMap<Integer, Double> simsForFirst = this.sims.get(i);
				
				if (simsForFirst == null)
					simsForFirst = new TreeMap<Integer, Double>();
				
				simsForFirst.put(j, curSim);
				this.sims.put(i, simsForFirst);	
			}
		}
	}
	
	public Double GetSimilarity(int firstInd, int secondInd)
	{
		if (this.sims.containsKey(firstInd) == false )
			return null;
		TreeMap<Integer, Double> simsForFirst = this.sims.get(firstInd);
		return (simsForFirst.get(secondInd));
	}

}
