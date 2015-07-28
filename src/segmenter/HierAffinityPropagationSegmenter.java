package segmenter;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import similarity.IGenericSimComputer;
import similarity.ISimComputer;

public class HierAffinityPropagationSegmenter implements ISegmenter {
	
	int numPoints = 0;
	int numLayers = 0;
	
	protected double dampFactor = 0.9; 
	protected int maxIterations = 1000;
	protected int maxConvCount = 100;
	
	protected int convergenceCount = 0;
	
	TreeMap<Integer, HAPSLayer> layers = new TreeMap<Integer, HAPSLayer>();
	
	public HierAffinityPropagationSegmenter()
	{}

	public void Init(int numberChunks, int numberLayers, double dampenFactor)
	{
		this.numPoints = numberChunks;
		this.numLayers = numberLayers;
		this.dampFactor = dampenFactor;
		
		//initialize layers
		for (int i = 0; i < numberLayers; i++)
		{
			HAPSLayer curLayer = new HAPSLayer();
			curLayer.SetTotalNumberOfLayers(numberLayers);
			this.layers.put(i, curLayer);
			if (i > 0)
			{
				HAPSLayer prevLayer = this.layers.get(i - 1);
				prevLayer.SetLayerAbove(curLayer);
				curLayer.SetLayerBelow(prevLayer);
			}
		}
	}
	
	/**
	 * sets s spefcific similarity matric for a given layer
	 * @param layerId
	 * @param sims
	 * @throws Exception
	 */
	public void SetSimsForLayer(int layerId, IGenericSimComputer sims) throws Exception
	{
		if (layerId < 0 || layerId >= this.numLayers)
		{
			Exception e = new Exception ("Exception in HierAffinityPropagationSegmenter.SetSimsForLayer: invalid layerId " + layerId );
			throw(e);
		}
		HAPSLayer lay = this.layers.get(layerId);
		lay.Init(this.numPoints, sims.GetWindowSize(), layerId, this.dampFactor, this.numLayers);	
		lay.SetSimilarities(sims);
	}
	
	/**
	 * Set the same similarity matrix for all layers
	 * @param sims
	 */
	public void SetUniformSims(IGenericSimComputer sims) throws Exception
	{
		for (int  i = 0; i < this.numLayers; i++)
		{
			this.SetSimsForLayer(i, sims);
		}
	}
	
	/**
	 * This method adds the matrix newSims to the similarity matrix in each layer of the segmenter
	 * @param newSims
	 * @throws Exception
	 */
	public void ModifyMatrix(IGenericSimComputer newSims) throws Exception
	{
		for (int i = 0; i < this.GetNumLayers(); i++)
		{
			this.layers.get(i).ModifyMatrix(newSims);
		}
	}
	
	public void SetSimsForLayer(int layerId, double[][] sims) throws Exception
	{}
	
	
	/**
	 * set uniform preferences for a given layer. It does not make sense to provide uniform preferences across layers.
	 * @param layerId
	 * @param prefValue
	 * @throws Exception
	 */
	public void SetUniformPrefsForLayer(int layerId, double prefValue) throws Exception
	{
		if (layerId < 0 || layerId >= this.numLayers)
		{
			Exception e = new Exception ("Exception in HierAffinityPropagationSegmenter.SetUniformPrefsForLayer: invalid layerId " + layerId );
			throw(e);
		}
		HAPSLayer lay = this.layers.get(layerId);
		lay.SetUniformPref(prefValue);
	}
	
	/**
	 * deprecated, do not use this method.
	 */
	@Override
	public void Init(ISimComputer simComputer) throws Exception {
		

	}

	@Override
	public void Run() throws Exception {
		
		boolean ifConverged = false;
		int numIterations = 0;
		
		while( !ifConverged)
		{
			boolean ifExamplarsChanged = false;
//			System.out.println("***Iteration " + numIterations);
//			System.out.println("curConvergenceCount: " + this.GetConvergenceCount());
			
			
			for (int i = 0; i < this.numLayers; i++)
			{
//				System.out.println("LAYER "+ i);
				HAPSLayer layer = this.layers.get(i);
				layer.SendMessages();
//				layer.PrintMatrices();
				if ( layer.CheckExamplarsForChange() == true )
				{
					ifExamplarsChanged = true;
					this.convergenceCount = 0;
				}
			}
			//if no examplars have changed in any of the layers, increment convCount
			if ( ifExamplarsChanged == false)
			{
				this.IncrementConvergenceCount();
//				System.out.println("inrementing curConvergenceCount: " + this.GetConvergenceCount());
			}
			numIterations++;
			
			if (numIterations >= this.GetMaxIterations())
			{
				System.out.println("Reached max iterations");
				break;
			}
			if ( this.GetConvergenceCount() >= this.GetMaxConvCount())
			{
				System.out.println("Reached max conv count");
				ifConverged = true;
			}
		}

	}
	
	public void RunGivoniSchedule() throws Exception {
		
		boolean ifConverged = false;
		int numIterations = 0;
		
		while( !ifConverged)
		{
			boolean ifExamplarsChanged = false;
			System.out.println("***G. Iteration " + numIterations);
			
			for (int i = 0; i < this.numLayers; i++)
			{
				System.out.println("\tUP Layer "+ i);
				HAPSLayer layer = this.layers.get(i);
				//layer.SendMessages();
				layer.DoUpwardIteration();
			}
			for (int i = this.numLayers - 1; i >= 0; i--)
			{
				System.out.println("\tDOWN Layer "+ i);
				HAPSLayer layer = this.layers.get(i);
				//layer.SendMessages();
				layer.DoDownwardIteration();
			}
			
			//now check all layers for convergence
			for (int i = 0; i < this.numLayers; i++)
			{
				HAPSLayer layer = this.layers.get(i);
				System.out.println("LAYER (conv check) " + i);
				if ( layer.CheckExamplarsForChange() == true )
				{
					ifExamplarsChanged = true;
				}
			}
			
			
			//if no examplars have changed in any of the layers, increment convCount
			if ( ifExamplarsChanged == false)
			{
				this.IncrementConvergenceCount();
			}
			
			
			numIterations++;	
			if (numIterations >= this.GetMaxIterations())
			{
				System.out.println("Reached max iterations");
				break;
			}
			if ( this.GetConvergenceCount() >= this.GetMaxConvCount())
			{
				System.out.println("Reached max conv count");
				ifConverged = true;
			}
		}

	}
	
	public TreeMap<Integer, ArrayList<Integer>> GetAllHypoBreaks() throws Exception
	{
		TreeMap<Integer, ArrayList<Integer>> allBreaks = new TreeMap<Integer, ArrayList<Integer>>();
		TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> allAssignments = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
		for (int i = 0; i < this.numLayers; i++)
		{
			HAPSLayer layer = this.layers.get(i);
			TreeMap<Integer, TreeSet<Integer>> curAssigns = layer.GetNonConflictingAssignments();
			allAssignments.put(i, curAssigns);
			allBreaks.put(i, layer.GetHypoBreaksAsList(curAssigns));
		}
		
		return (allBreaks);
	}
	
	/**
	 * This methods corrects situations where a datapoint is assigned to a new examplar at a higher level when an examplar from level l-1 is 
	 * still active
	 * @return
	 */
	public TreeMap<Integer, ArrayList<Integer>>  GetAllHypoBreaksWithConflictsResolved() throws Exception
	{
		TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> allAssignments = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
		TreeMap<Integer, ArrayList<Integer>> allBreaks = new TreeMap<Integer, ArrayList<Integer>>();
		TreeMap<Integer,Integer> prevPointToExamplar = null;
		
		
		for (int i = 0; i < this.numLayers; i++)
		{
			
			HAPSLayer layer = this.layers.get(i);
			TreeMap<Integer, TreeSet<Integer>> curAssigns = layer.GetNonConflictingAssignments();
			TreeMap<Integer, TreeSet<Integer>> correctedAssigns = new TreeMap<Integer, TreeSet<Integer>>();
			
			TreeMap<Integer,Integer> pointToExamplar = new TreeMap<Integer,Integer>();
			for (Integer examplar: curAssigns.keySet())
			{
				for (Integer point: curAssigns.get(examplar))
				{
					if (i > 0)
					{
						//what was the examplar for this point at prev level?
						Integer prevExamplar = prevPointToExamplar.get(point);
						if (prevExamplar != examplar)
						{
							if (curAssigns.containsKey(prevExamplar) == true)
							{
								pointToExamplar.put(point, prevExamplar);
								continue;
							}
						}
					}
					
					pointToExamplar.put(point, examplar);
				}
			}
			prevPointToExamplar = pointToExamplar;
			
			for (Integer curPoint: pointToExamplar.keySet())
			{
				Integer curExamplar = pointToExamplar.get(curPoint);
				if (correctedAssigns.containsKey(curExamplar) == false)
				{
					TreeSet<Integer> newAssign = new TreeSet<Integer>();
					newAssign.add(curPoint);
					correctedAssigns.put(curExamplar, newAssign);
				}
				else
					correctedAssigns.get(curExamplar).add(curPoint);
			}
			allBreaks.put(i, layer.GetHypoBreaksAsList(correctedAssigns));
			allAssignments.put(i, correctedAssigns);
		}
		
		return allBreaks;
	}
	
	public void PrintResults() throws Exception
	{
		TreeMap<Integer, ArrayList<Integer>> allBreaks = new TreeMap<Integer, ArrayList<Integer>>();
		TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> allAssignments = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
		for (int i = 0; i < this.numLayers; i++)
		{
			HAPSLayer layer = this.layers.get(i);
			TreeMap<Integer, TreeSet<Integer>> curAssigns = layer.GetNonConflictingAssignments();
			allAssignments.put(i, curAssigns);
			allBreaks.put(i, layer.GetHypoBreaksAsList(curAssigns));
			
			System.out.println("LAYER " + i);
			for (Integer exId: curAssigns.keySet())
			{
				System.out.println("\tExamplar "  + exId);
				System.out.println("\t\t " + curAssigns.get(exId) );
			}
			System.out.println("\tHYPO breaks: " + layer.GetHypoBreaksAsList(curAssigns).toString() );
			
			
		}
	}
	
	/**
	 * output examplars at the end of the run, prior to the resolutionof conflicts
	 */
	public void PrintConservativeExamplars() throws Exception
	{
		System.out.println("CONSERVATIVE ");
		//TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> allAssignments = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
		
		for (int i = 0; i < this.numLayers; i++)
		{
//			HAPSLayer layer = this.layers.get(i);
			TreeMap<Integer, TreeSet<Integer>> curAssigns = this.GetAssignmentsForLayer(i);
			System.out.println("LAYER  " + i);
			for (Integer exId: curAssigns.keySet())
			{
				System.out.println("\tExamplar "  + exId);
				System.out.println("\t\t " + curAssigns.get(exId) );
			}
			
//			ArrayList<Integer> eEx = layer.GetExamplarsFromE();
//			System.out.println("examplars from e: " + eEx.toString() );
//			
//			ArrayList<Integer> arEx = layer.GetExamplarsFromE();
//			System.out.println("examplars from AVAIL + resp: " + arEx.toString() );
			
		}
	}
	
	public TreeMap<Integer, TreeSet<Integer>> GetAssignmentsForLayer(int layerId) throws Exception
	{
		HAPSLayer layer = this.layers.get(layerId);
		TreeMap<Integer, TreeSet<Integer>> curAssigns = layer.GetAssignments();
		return curAssigns ;
	}
	
	/**
	 * prints the value of the objective function for each layer and the total
	 */
	public void PrintObjectiveFunction() throws Exception
	{
		double totalVal = 0;
		StringBuilder str = new StringBuilder();
		
		
		for (int i = 0; i < this.numLayers; i++)
		{
			HAPSLayer layer = this.layers.get(i);
			double curVal = layer.GetObjectiveFunction();
			str.append("Layer " + i + ": " + curVal + "\n");
			totalVal += curVal;
		}
		str.append("Overall obj func:\t" + totalVal);
		System.out.println(str.toString());
	}
	
	void IncrementConvergenceCount()
	{
		this.convergenceCount++;
	}

	public int GetMaxIterations() {
		return maxIterations;
	}

	public void SetMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public int GetMaxConvCount() {
		return maxConvCount;
	}

	public void SetMaxConvCount(int maxConvCount) {
		this.maxConvCount = maxConvCount;
	}

	public int GetNumPoints() {
		return numPoints;
	}

	public int GetNumLayers() {
		return numLayers;
	}

	public double GetDampFactor() {
		return dampFactor;
	}
	
	public int GetConvergenceCount()
	{
		return this.convergenceCount;
	}
	
	/**
	 * this is a debugging method. it output all matrices for all layers
	 */
	public void PrintDetailedMatrices() throws Exception
	{
		for (Integer id: this.layers.keySet())
		{
			this.layers.get(id).PrintMatrices();
		}
	}

}
