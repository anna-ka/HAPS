package segmenter;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import similarity.IGenericSimComputer;
import similarity.TripletSim;

/**
 * a class to represent a layer in the hierarchical version of APS
 * @author anna
 *
 */
public class HAPSLayer {
	
	protected ArrayList<Integer> examplars = null;
	//protected int convergenceCount = 0;
	protected TreeMap<Integer, TreeSet<Integer>> assignments = null;
	
	/**
	 * parameters for iterating
	 *
	 */
	int numIntraLevelIterations = 10;
	
	int numPoints = 0;
	int winSize = 0;
	int layerId = -1;
	
	int totalNumberOfLayers = -1;
	
	

	IMatrix sims;
	double[] prefs;
	
	/**
	 * the four types of messages sent by this model
	 */
	IMatrix avail;
	IMatrix resp;
	
	
	/**
	 * Each layer keeps tract of its downward going fi messages, sent from I at this level to e at prev. level
	 */
	double [] fi_downward; //send to prev layer, layerId - 1
	/**
	 * each method also keeps track of its upward going ro messages, sent from e at this level to I of the next level
	 */
	double [] ro_upward; //send to next layer layerId + 1
	
	/**
	 * this is a dummy array initialized to 0 for getting 0 messages for top and bottom layers
	 */
	double [] dummy_ro_fi; 
	
//	double [] ro_downward; //received from the previous, layerId - 1
//	double [] fi_upward; //received from the next layer, layerId + 1
	
	double dampFactor = 0;
	
	//layers above and below
	HAPSLayer layerAbove = null;
	HAPSLayer layerBelow = null;
	
	public void SetLayerAbove(HAPSLayer layer)
	{
		this.layerAbove = layer;
	}
	
	public HAPSLayer GetLayerAbove()
	{
		return this.layerAbove;
	}
	
	public void SetLayerBelow(HAPSLayer layer)
	{
		this.layerBelow = layer;
	}
	
	public HAPSLayer GetLayerBelow()
	{
		return this.layerBelow;
	}
	
	
	public HAPSLayer()
	{}
	
	/**
	 * Creates empty matrices for sims, avail, resp, ro and fi messages initialized to zeros
	 * @param numberPoints
	 * @param windowSize
	 * @param curLayerId
	 * @throws Exception
	 */
	public void Init(int numberPoints, int windowSize, int curLayerId, double dampenFactor, int numLayers) throws Exception
	{
		this.numPoints = numberPoints;
		this.winSize = windowSize;
		this.layerId = curLayerId;
		this.dampFactor = dampenFactor;
		
		this.sims = new WindowMatrix(this.numPoints, this.winSize);
		this.avail = new WindowMatrix(this.numPoints, this.winSize);
		this.resp = new WindowMatrix(this.numPoints, this.winSize);
		
		this.ro_upward = new double[this.numPoints];
		this.fi_downward = new double[this.numPoints];
		this.prefs = new double[this.numPoints];
		this.dummy_ro_fi = new double[this.numPoints];
		
		for (int i = 0; i < this.numPoints; i++)
		{
			for (int j  = 0; j < this.winSize; j++)
			{
				//default value of similarities varies depending on the specific layer
				//in the upper layers we do not want to have -INF 
				if (this.layerId < 1)
					this.sims.SetElement(i, j, Double.NEGATIVE_INFINITY);
				else
					this.sims.SetElement(i, j, 0);
				
				this.avail.SetElement(i, j, 0);
				this.resp.SetElement(i, j, 0);
			}
			this.ro_upward[i] = 0;
			this.fi_downward[i] = 0;
			this.dummy_ro_fi[i] = 0;
		}
	}
	
	public void SetUniformPref(double prefValue)
	{
		
		for (int i = 0; i < this.numPoints; i++)
		{
			this.prefs[i] = prefValue;
		}
	}
	
	/**
	 * load similarities from an IGenericSimComputer
	 * @param simComp
	 * @throws Exception
	 */
	public void SetSimilarities(IGenericSimComputer simComp) throws Exception
	{
		IMatrix m = simComp.GetSimilarities();

		for (int i = 0; i < m.GetNumRows(); i++) {
			this.sims.SetRowStart(i, m.GetRowStart(i));
			this.sims.SetRowEnd(i, m.GetRowEnd(i));
		}
		
		for (int j = 0; j < m.GetNumRows(); j++) {
			this.sims.SetColumnStart(j, m.GetColumnStart(j));
			this.sims.SetColumnEnd(j, m.GetColumnEnd(j));
		}
		
		for (int i = 0; i < m.GetNumRows(); i++) {
			for (int j = m.GetRowStart(i); j <= m.GetRowEnd(i); j++) {
				this.sims.SetElement(i, j, m.GetElement(i, j));
			}
		}

		//		int lastRow = -1; 
//		int lastCol = -1;
//		TripletSim prevTriplet = null;
//		
//		for (int i = 0; i < this.sims.GetNumRows(); i++)
//		{
//			
//			
//			//initialize row and columns starts and ends to -1
//			this.sims.SetRowStart(i, -1);
//			this.sims.SetRowEnd(i, -1);
//			this.sims.SetColumnStart(i, -1);
//			this.sims.SetColumnEnd(i, -1);
//		}
//		
//		//fill in the similarities matrix
//		for ( TripletSim sim : simComp.GetSimilarities() )
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
//				this.sims.SetRowStart(sim.firstId, sim.secondId);
//				//remember last valid index in previous row 
//				if (lastRow != -1)
//				{
//					this.sims.SetRowEnd(lastRow, prevTriplet.secondId);
//				}
//				lastRow = sim.firstId;
//				lastCol = sim.secondId;
//			}
//			
//			//check if this is the first element in this column
//			int oldIndex = this.sims.GetColumnStart(sim.secondId);
//			if (oldIndex == -1)
//				this.sims.SetColumnStart(sim.secondId, sim.firstId);
//			
//			//check if this is the last element in the column
//			int oldEndIndex = this.sims.GetColumnEnd(sim.secondId);
//			if (oldEndIndex == -1)
//				this.sims.SetColumnEnd(sim.secondId, sim.firstId);
//			else if (oldEndIndex < sim.firstId)
//				this.sims.SetColumnEnd(sim.secondId, sim.firstId);
//			
//			this.sims.SetElement(sim.firstId, sim.secondId, sim.similarity);
//			prevTriplet = sim;
//			//System.out.println( sim.ToString() );
//		}
//		
//		//record row end for the last row
//		this.sims.SetRowEnd(prevTriplet.firstId, prevTriplet.secondId);
		
		//copy row starts/ends and column starts/ends for availability and resp matrices
		for (int i = 0; i < this.sims.GetNumRows(); i++)
		{
			this.avail.SetRowStart(i, this.sims.GetRowStart(i));
			this.avail.SetRowEnd(i, this.sims.GetRowEnd(i));
			this.avail.SetColumnStart(i, this.sims.GetColumnStart(i));
			this.avail.SetColumnEnd(i, this.sims.GetColumnEnd(i));
			
			this.resp.SetRowStart(i, this.sims.GetRowStart(i));
			this.resp.SetRowEnd(i, this.sims.GetRowEnd(i));
			this.resp.SetColumnStart(i, this.sims.GetColumnStart(i));
			this.resp.SetColumnEnd(i, this.sims.GetColumnEnd(i));
			
			//set self-similarities to 0
			this.sims.SetElement(i, i, 0);
		}
		
	}
	
	public void ModifyMatrix(IGenericSimComputer newSims) throws Exception
	{
		IMatrix newM = newSims.GetSimilarities();
		int numRows = newM.GetNumRows();
		int numCols = newM.GetNumColumns();
		
		for (int r = 0; r < numRows; r++)
		{
			int rowStart = this.sims.GetRowStart(r);
			int rowEnd = this.sims.GetRowEnd(r);
			for (int c = rowStart; c <= rowEnd; c++)
			{
				try{
					double old =this.sims.GetElement(r, c);
					double mod = newM.GetElement(r, c);
					double newSim = old + mod;
					this.sims.SetElement(r, c, newSim);
				}
				catch (Exception e)
				{
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * get ro messages from prev layer, layerId - 1
	 * @return
	 */
	public double[] GetRosFromBelow()
	{
		if (this.layerBelow != null)
			return this.layerBelow.GetUpwardRos();
		
		return null;
	}
	
	public double[] GetUpwardRos()
	{
		return this.ro_upward;
	}
	
	/**
	 * get fi messages from the next layer, layerId + 1.
	 * This information is stored in this layer.
	 * @return
	 */
	public double[] GetDownwardFis()
	{
		return this.fi_downward;
	}
	
	public double[] GetFisFromAbove()
	{
		if (this.layerAbove == null)
		{
			return this.dummy_ro_fi;
			
		}
		return this.layerAbove.GetDownwardFis();
	}
	
	
	/**
	 * sends an iteration of messages
	 */
	protected void SendMessages() throws Exception
	{
		if (this.layerId == 0)
		{
			//send layer 1 messages, the same as in flat APS
			
			this.SendResps();
			this.SendAvails();
			this.SendRos();
			
//			this.PrintMatrices();
			
			return;
		}
		
		//otherwise send all
		this.SendFis();
		this.SendResps();
		this.SendAvails();
		this.SendRos();
//		this.PrintMatrices();
	}
	
	/**
	 * Following Givoni 2011, each iteration consists of sending intra-layer messages 10 times and then sending messages upward, rho^{l+1} messages
	 */
	public void DoUpwardIteration() throws Exception
	{
		
		for (int i = 0; i < this.numIntraLevelIterations; i++)
		{
			this.SendAvails();
			this.SendResps();
		}
		this.SendRos();
	}
	
	/**
	 * send intra-level messages 10 times and then send downward fi messages
	 * @throws Exception
	 */
	public void DoDownwardIteration() throws Exception
	{
		
		for (int i = 0; i < this.numIntraLevelIterations; i++)
		{
			this.SendAvails();
			this.SendResps();
		}
		this.SendFis();
	}
	
	
	/**
	 * send downward fi messages
	 */
	protected void SendFis() throws Exception
	{
		for (int i = 0; i < this.fi_downward.length; i++)
			this.CalcFi(i);
	}
	
	/**
	 * send upward ro messages
	 */
	protected void SendRos() throws Exception
	{
		if (this.layerAbove == null)
			return;
		for (int i = 0; i < this.ro_upward.length; i++)
		{
			this.CalcRo(i);
		}
	}
	
	/**
	 * Send all responsibility messages.
	 * Responsibilities are sent row by row.
	 * @throws Exception
	 */
	protected void SendResps() throws Exception
	{
		boolean isFirstLayer = false;
		if (this.layerId == 0)
			isFirstLayer = true;
		
		for (int row = 0; row < this.numPoints; row++)
		{
			int rowStartIndex = this.sims.GetRowStart(row);
			int rowEndIndex = this.sims.GetRowEnd(row);
			
			for (int col = rowStartIndex; col <= rowEndIndex; col++)
			{
				if (isFirstLayer == true)
					this.CalcRespLayer1(row, col);
				else
					this.CalcResp(row, col);
			}
		}
	}
	
	/**
	 * send availability messages. Availabilities are sent column by column
	 * @throws Exception
	 */
	protected void SendAvails() throws Exception
	{
		for (int col = 0; col < this.numPoints; col++)
		{
			int colStartIndex = this.sims.GetColumnStart(col);
			int colEndIndex = this.sims.GetColumnEnd(col);
			for (int row = colStartIndex; row <= colEndIndex; row++)
			{
				this.CalcAvailability(row, col);
			}
		}
	}
	
	/**
	 * calculate an upward ro message sent from variable e_ind to function I_ind
	 * 
	 * ro_j = pref_j + resp_jj + max_{s=1 to j} sum_{k=s to j-1}resp_{kj} + max_{e=j to N} sum _{k=j+1 to e}resp_{kj}
	 * @param ind
	 */
	protected void CalcRo(int ind) throws Exception
	{
		double self_pref = this.prefs[ind];
		double self_resp = this.resp.GetElement(ind, ind);
		
		//int[] exclude = {ind};
		//double sum = this.SumNonZeroResp(ind, exclude);
		
		int colStartInd = this.resp.GetColumnStart(ind);
		int colEndInd = this.resp.GetColumnEnd(ind);
		
		double d1 = this.MaximizeSumRL(ind, colStartInd, ind - 1);
		double d2 = this.MaximizeSumLR(ind, ind + 1, colEndInd);
		
		//self_pref + self_resp + d1 + d2;
		double newRo =  this.SafePlus(self_pref, self_resp);
		newRo =  this.SafePlus(newRo, d1);
		newRo =  this.SafePlus(newRo, d2);
		this.ro_upward[ind] = this.DampenMessage(this.ro_upward[ind], newRo);
	}
	
	
	
	/**
	 * A downward message fi sent from function I at this level to variable e at prev level.
	 * fi_j = max [0, 
	 * 				a_jj - max_{k!=j}(sjk + ajk)]
	 * @param ind
	 * @throws Exception
	 */
	protected void CalcFi(int ind) throws Exception
	{
		//find k so as to max sjk + ajk
		
		double max = Double.NEGATIVE_INFINITY;
		int max_ind = -1;
		
		int startRowInd = this.avail.GetRowStart(ind);
		int endRowInd = this.avail.GetRowEnd(ind);
		
		for (int k = startRowInd; k <= endRowInd; k++)
		{
			if ( k == ind)
				continue;
			
			double curSim = this.sims.GetElement(ind, k);
			double curAvail = this.avail.GetElement(ind, k);
			double sum = this.SafePlus(curSim, curAvail);
			
			if (sum > max)
			{
				max = sum;
				max_ind = k;
			}
		}
		
		double self_avail= this.avail.GetElement(ind, ind);
//		double diff = self_avail - max;
		double diff = this.SafeMinus( self_avail, max);
		
		if (diff < 0)
			diff = 0;
		
		this.fi_downward[ind] = this.DampenMessage(this.fi_downward[ind], diff);
	}
	
	
	
	
	protected void CalcRespLayer1(int src, int dest) throws Exception
	{
		double max = Double.NEGATIVE_INFINITY;
		int max_ind = -1;
		
		int startInd = this.sims.GetRowStart(src);
		int endInd = this.sims.GetRowEnd(src);
		
		for (int j = startInd; j <= endInd; j++ )
		{
			if (j == dest)
				continue;
			double curSim = this.sims.GetElement(src, j);
			double curAvail = this.avail.GetElement(src, j);
			double curSum = this.SafePlus(curSim, curAvail); ;
			if (curSum > max)
			{
				max = curSum;
				max_ind = j;
			}
		}
		
		double sim = this.sims.GetElement(src, dest);
//		if (src == dest)
//			sim = this.prefs[src];
		double result = this.SafeMinus(sim, max); 
		result = this.DampenMessage(this.resp.GetElement(src, dest), result);
		
		this.resp.SetElement(src, dest, result);
	}
	
	
	
	/**
	 * A method to calculate responsibility messages for layers other then the first one 
	 * @param src is row, dest is column, sent from c_src,dest to E_dest
	 * @param dest
	 */
	protected void CalcResp(int src, int dest) throws Exception
	{
		
		
		double ros_from_below[] = this.GetRosFromBelow();
		
		//find k so as to max sjk + ajk
		
		double max_excl_ij = Double.NEGATIVE_INFINITY;
		
		//we also need to find second best value, in case the best one is for k = j
		int max_ind = -1;
		
		int rowStartInd = this.sims.GetRowStart(src);
		int rowEndInd = this.sims.GetRowEnd(src);
		
		for (int k = rowStartInd; k <= rowEndInd; k++)
		{
			if ( k == src || k == dest)
				continue;
			
			
			
			double curSim = this.sims.GetElement(src, k);
			double curAvail = this.avail.GetElement(src, k);
			double sum = this.SafePlus(curSim, curAvail); 
			
			if (sum > max_excl_ij)
			{
				max_excl_ij = sum;
				max_ind = k;
				
			}
		}
		
		double prevRo = ros_from_below[src];
		
		//resp_ii = min(0, ro_i) - max_for_k(s_ik+a_ik)
		if (src == dest)
		{
			double max = max_excl_ij;
			double sum_ij = this.SafePlus(this.sims.GetElement(src, dest),this.sims.GetElement(src, dest)); 
			if (sum_ij > max)
				max = sum_ij;
			
			double firstTerm = prevRo;
			if (prevRo > 0)
				firstTerm = 0;
			
			double result = this.SafeMinus(firstTerm, max);
			result = this.DampenMessage(resp.GetElement(src, dest), result);
			this.resp.SetElement(src, dest, result);
			return;
		}
		//otherwise src != dest
		//resp_ij = s_ij + min[ max(0, -r_i) - a_ii,
		//						-max_{k!=ij}(s_ik + a_ik)]
		
		double term = -prevRo;
		if (term < 0)
			term = 0;
		
		double firstMinTerm = this.SafeMinus( term, this.avail.GetElement(src, src));
		
		//now compute the second min term
		double secondMinTerm = -max_excl_ij;
		
		double result = firstMinTerm;
		
		if (secondMinTerm < firstMinTerm)
			result = secondMinTerm;
		
		result = this.SafePlus(result, this.sims.GetElement(src, dest));
		
		result = this.DampenMessage(resp.GetElement(src, dest), result);
		this.resp.SetElement(src, dest, result);

	}
	
	
	protected void CalcAvailability(int i, int j) throws Exception
	{
		if (i == j)
			CalcA1(i);
		else if (i < j)
			CalcA2(i, j);
		else // i > j
			CalcA3(i, j);
	}
	
	/**
	 * Calculate availability message  from variable node c_jj to function node E_j
	 * @param j
	 * @throws Exception
	 */
	void CalcA1(int j) throws Exception
	{
		double self_pref, self_fi;
		self_pref = this.prefs[j];
		if (this.layerId == this.totalNumberOfLayers - 1)
		{
			self_fi = 0;
		}
		else
		{
			self_fi = this.GetFisFromAbove()[j];
			
		}
		
		int firstSimIndex = this.sims.GetColumnStart(j);
		int lastSimIndex = this.sims.GetColumnEnd(j);
		
		double sum1 = MaximizeSumRL(j, firstSimIndex, j-1);
		double sum2 = MaximizeSumLR(j, j + 1, lastSimIndex);
		double newA = this.SafePlus(sum1, sum2 );
		
		//newA = newA + self_pref + self_fi;
		newA = this.SafePlus(newA, self_pref);
		newA = this.SafePlus(newA, self_fi);
		
		this.avail.SetElement(j,j, DampenMessage(this.avail.GetElement(j, j), newA) );
	}
	
	/**
	 * Calculate availability message sent from variable node c_ij to function node E_j, where i < j
	 * @param i
	 * @param j
	 * @throws Exception
	 */
	void CalcA2(int i, int j) throws Exception
	{
		double self_pref, self_fi;
		self_pref = this.prefs[j];
		if (this.layerId == this.totalNumberOfLayers - 1)
		{
			self_fi  = 0;
		}
		else
		{
			self_fi = this.GetFisFromAbove()[j];
		}
		
		int firstSimIndex = this.sims.GetColumnStart(j);
		int lastSimIndex = this.sims.GetColumnEnd(j); 
		
		//first min arg
		double d1 = this.MaximizeSumRL(j, firstSimIndex, i-1);
		double d2 = this.Sum(j, i+1, j);
		double d3 =  this.MaximizeSumLR(j, j+1, lastSimIndex);
		//double firstArg = d1 + d2 + d3 + self_pref + self_fi ;
		double firstArg = this.SafePlus(d1, d2);
		firstArg = this.SafePlus(firstArg, d3);
		firstArg = this.SafePlus(firstArg, self_pref);
		firstArg = this.SafePlus(firstArg, self_fi);
		
		//second min arg
		
		double d4 = d1;
		double d5 = this.MinimizeSumBeforeStart(j, i+1, j-1);
	
		//double secondArg = d4 + d5;
		double secondArg = this.SafePlus(d4, d5);
		
		double newA = Math.min(firstArg, secondArg);
		
		this.avail.SetElement(i, j, DampenMessage( this.avail.GetElement(i, j), newA) );
	}

	/**
	 * Calculate availability message sent from variable node c_ij to function node E_j, where i > j
	 * @param i
	 * @param j
	 * @throws Exception
	 */
	void CalcA3(int i, int j) throws Exception
	{
		double self_pref, self_fi;
		self_pref = this.prefs[j];
		if (this.layerId == this.totalNumberOfLayers - 1)
		{
			self_fi  = 0;
		}
		else
		{
			self_fi = this.GetFisFromAbove()[j];
		}
		
		int firstSimIndex = this.sims.GetColumnStart(j);
		int lastSimIndex = this.sims.GetColumnEnd(j); 
		
		//first min arg
		double d3 = this.MaximizeSumRL(j, firstSimIndex, j-1);
		double d4 = this.Sum(j, j, i-1);
		double d5 =  this.MaximizeSumLR(j, i+1, lastSimIndex);
		
//		double firstArg = d3 + d4 + d5 + self_fi + self_pref ;
		double firstArg = this.SafePlus(d3, d4);
		firstArg = this.SafePlus(firstArg , d5);
		firstArg = this.SafePlus(firstArg , self_fi);
		firstArg = this.SafePlus(firstArg , self_pref);
		
		//second min arg
		
		double d1 = this.MinimizeSumAfterEnd(j, j+1, i-1);
		double d2 = d5;
		
		
//		double secondArg = d1 + d2;
		double secondArg = this.SafePlus(d1, d2);
		
		double newMsg = Math.min(firstArg, secondArg);
		
		this.avail.SetElement(i, j, DampenMessage( this.avail.GetElement(i, j), newMsg) );
	}
	
	/**
	 * we only ever need to compute mu to find the best settings for e variables
	 * @param src
	 * @param dest
	 * @return
	 */
	double ComputeMu(int dest) throws Exception
	{
		double self_resp = this.resp.GetElement(dest, dest);
		
		int firstSimIndex = this.sims.GetColumnStart(dest);
		int lastSimIndex = this.sims.GetColumnEnd(dest);
		
		double sum1 = MaximizeSumRL(dest, firstSimIndex, dest-1);
		double sum2 = MaximizeSumLR(dest, dest + 1, lastSimIndex);
		
		//double mu =sum1 + sum2 + self_resp;
		double mu = this.SafePlus(sum1, sum2);
		mu = this.SafePlus(mu, self_resp);
		
		return mu;
		
	}
	
	
	/**
	 * Maximize sum going going from right to left. We ususallly use this method to find the most likely start of a segment
	 * when calculating availabilitiy messages.
	 * @param j
	 * @param left
	 * @param right
	 * @return
	 * @throws Exception
	 */
	double MaximizeSumRL(int j, int left, int right) throws Exception
	{
		double maxSum = 0.0;
		double sum = 0.0;
		
		int curIndex = right;
		while(curIndex >= left && curIndex >= 0
				)
		{

			sum = this.SafePlus(sum, this.resp.GetElement(curIndex, j));
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
	
	
	/**
	 * Maximize sum going going from left to right. Usually used to find the end of the segment in availability messages.
	 * @param j
	 * @param left
	 * @param right
	 * @return
	 * @throws Exception
	 */
	double MaximizeSumLR(int j, int left, int right) throws Exception
	{
		double maxSum = 0.0;

		double sum = 0.0;
		int curIndex = left;
		while(curIndex <= right && curIndex < this.numPoints)
		{
			sum = this.SafePlus(sum, this.resp.GetElement(curIndex, j));
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
	
	/**
	 * Minimize sum for finding the end of the segment.
	 * @param j
	 * @param left
	 * @param right
	 * @return
	 * @throws Exception
	 */
	double BadMinimizeSumLR(int j, int left, int right) throws Exception
	{
		double sum = 0.0;
		double minSum = 0.0;
		
		int curIndex = left;
		int endOfCol = this.resp.GetColumnEnd(j);
		//while (curIndex < this.numPoints && curIndex <= right)
		while (curIndex <= endOfCol && curIndex <= right)
		{
			//sum += this.resp.GetElement(curIndex, j);
			sum = this.SafePlus(sum, this.resp.GetElement(curIndex, j));
			if (curIndex == left)
				minSum = sum;
			else if (sum <= minSum )
				minSum = sum;

			curIndex++;
		}
		return minSum;
	}
	
	/**
	 * a method for finding the minimal configuration of sums of resp between i and j, i < j.
	 * This is for finding the piece before the start of the segment.
	 * This correspond to finding the configuration between i<j and start of segment s, i<s<=j
	 * @return
	 */
	double MinimizeSumBeforeStart(int j, int left, int right) throws Exception
	{
		double minSum = this.Sum(j, left, right);
		double curSum = minSum;
		
		int curIndex = right;
		while (curIndex >= left)
		{
			double curResp = this.resp.GetElement(curIndex, j);
			curSum = this.SafeMinus(curSum, curResp);
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
			curSum = this.SafeMinus(curSum, curResp);
			if (curSum < minSum)
				minSum = curSum;
			curIndex++;
		}
	
		return minSum;
	}
	
	/**
	 * Minimize sum for finding the start of the segment
	 * @param j
	 * @param left
	 * @param right
	 * @return
	 * @throws Exception
	 */
	double BadMinimizeSumRL(int j, int left, int right) throws Exception
	{
		double sum = 0.0;
		double minSum = 0.0;
		
		int curIndex = right;
		int colStartInd = this.resp.GetColumnStart(j);
//		while (curIndex >= 0 && curIndex >= left )
		while (curIndex >= colStartInd && curIndex >= left )
		{
			//sum += this.resp.GetElement(curIndex, j);
			sum = this.SafePlus(sum, this.resp.GetElement(curIndex, j));
			if (curIndex == right)
				minSum = sum;
			else if (sum <= minSum )
				minSum = sum;

			curIndex--;
		}
		return minSum;
	}
	
	/**
	 * This method sums up responsibitlities in column j between rows start and end, inclusive
	 */
	double Sum(int j, int start, int end) throws Exception
	{
		int colEndInd = this.resp.GetColumnEnd(j);
		if (start < 0 || start > colEndInd )
			return 0;
		
		double sum = 0;
			
		for (int k = start; k <= end && k <= colEndInd ; k ++)
		{
			//sum += this.resp.GetElement(k, j);
			sum = this.SafePlus(sum, this.resp.GetElement(k, j));
		}
		
		return sum;
	}
	
	
	
	
	/**
	 * The method to perform summations neede to compute ro and availability messages. The summations
	 * are of form sum_{k!=ij}( max( 0, resp(kj) ) )
	 * @param colIndex
	 * @param excludeIndices
	 * @return
	 * @throws Exception
	 */
	public double SumNonZeroResp(int colIndex, int[] excludeIndices) throws Exception
	{
		double sum = 0;
		HashSet<Integer> exclude = new HashSet<Integer>();
		for (int ex: excludeIndices)
		{
			exclude.add(ex);
		}
		
		int colStartInd = this.resp.GetColumnStart(colIndex);
		int colEndInd = this.resp.GetColumnEnd(colIndex);
		
		for (int row = colStartInd; row <= colEndInd; row++)
		{
			if (exclude.contains(row))
				continue;
			double resp = this.resp.GetElement(row, colIndex);
			if (resp > 0)
			{
				//sum += resp;
				sum = this.SafePlus(sum, resp);
			}
		}
		
		return sum;
	}
	
	public double SafePlus(double left, double right )
            throws ArithmeticException {
//	if ((right > 0 && left > Double.MAX_VALUE - right) ||
//			(right <= 0 && left < Double.MIN_VALUE - right))
//	{
//		throw new ArithmeticException("Integer overflow");
//	}
//	return left + right;
		
		double sum = left + right;
		if (Double.compare(sum, Double.POSITIVE_INFINITY) == 0)
			sum = Double.MAX_VALUE;
		return sum;
	}
	
	public double SafeMinus(double first, double second)
	{
		double diff = first - second;
		if (Double.compare(diff, Double.NEGATIVE_INFINITY) == 0)
			diff = -Double.MAX_VALUE;
		return diff;
	}
	
	public double SafeMult(double first, double second)
	{
		double product = first * second;
		if (Double.compare(product, Double.POSITIVE_INFINITY) == 0)
			product = Double.MAX_VALUE;
		else if (Double.compare(product, Double.NEGATIVE_INFINITY) == 0)
			product = -Double.MAX_VALUE;
		return product;
	}
	
	double DampenMessage(double oldMsg, double newMsg)
	{
		//double msg = this.dampFactor * oldMsg + (1 - this.dampFactor) * newMsg;
		double oldPart = this.SafeMult(dampFactor, oldMsg );
		double newPart = this.SafeMult((1 - dampFactor), newMsg );
		double msg = this.SafePlus(oldPart, newPart);
		return msg;
	}
	
	/**
	 * A method to check if the set of examplars on this iteration has changed from the previous iteration.
	 * @return true if examplars changed from the last iteration, false if they remain the same
	 */
	public boolean CheckExamplarsForChange() throws Exception
	{
//		ArrayList<Integer> newExamplars = this.GetExamplars();
		ArrayList<Integer> newExamplars = this.GetExamplarsFromE();
		
		if (this.examplars == null || 
				this.examplars.size() != newExamplars.size() ||
				this.examplars.size() == 0 ||
			!this.examplars.containsAll(newExamplars) )
		{
//			System.out.println("old examplars: " + this.examplars);
			this.examplars = newExamplars;
//			System.out.println("new examplars: " + this.examplars);
			return (true);
		}
//		System.out.println("unchanged examplars: " + this.examplars);
		
	
		
		return (false);
		
	}
	
	/**
	 * the method returns the value of the objective function
	 * @return
	 */
	public double GetObjectiveFunction() throws Exception
	{
		StringBuilder str = new StringBuilder();
		str.append("layer " + this.getLayerId() + "\n");
		double result = 0;
		
		TreeMap<Integer, TreeSet<Integer>> assigns = this.GetAssignments();
		
		Set<Integer> ex = assigns.keySet();
		
		for (Integer curEx: ex)
		{
			//add preference
			result = this.SafePlus(result, this.prefs[curEx]);
			str.append("\t ex "+ curEx + "\t pref: " + this.prefs[curEx] +  "\n");
			TreeSet<Integer> children = assigns.get(curEx);
			for (Integer child: children)
			{
				double sim = this.sims.GetElement(child, curEx);
				result = this.SafePlus(result, sim);
				str.append("\t\t sim with " + child + ":\t" + sim + "\n");
			}
		}
		str.append("\t net sim:\t" + result + "\n");
		System.out.println(str.toString());
		return result;
		
	}
	
	/**
	 * Get examplars at a given point in computation.
	 * @return
	 * @throws Exception
	 */
	public ArrayList<Integer> GetExamplars() throws Exception
	{
//		System.out.println("EXAMPLARS from AVAIL + RESP");
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < this.numPoints; i++)
		{
			double sum = this.avail.GetElement(i, i) + this.resp.GetElement(i, i) ;
			if ( sum > 0)
			{
				list.add(i);
//				System.out.println("\tEXAmplar " + i + " :\t" + sum + "=" + this.avail.GetElement(i, i) + "+" + this.resp.GetElement(i, i));
			}
		}
		return list;
	}
	
	/**
	 * an alternative way to check for examplars. It sums up fi from above an pref and mu
	 * @return
	 * @throws Exception
	 */
	public ArrayList<Integer> GetExamplarsFromE() throws Exception
	{
//		System.out.println("EXAMPLARS from fi + mu");
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < this.numPoints; i++)
		{
			double pref = this.prefs[i];
			double mu = this.ComputeMu(i);
			double fi = this.GetFisFromAbove()[i];
			double avail  = this.avail.GetElement(i, i);
			double resp = this.resp.GetElement(i, i);
			double sum2 = avail + resp;
			
			double sum = pref + mu + fi ;
			if ( sum > 0)
			{
				list.add(i);
//				System.out.println("\tEXamplar " + i + " " + sum );
//				System.out.println("\t\tpref:" + pref + ",\tmu:" + mu + ",\tfi:"  + fi);
//				System.out.println("\t\tavail:" + avail + ",\tresp:" + resp + ",\tsum:\t" + sum2);
			}
		}
		
		this.GetExamplars();
		
		return list;
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
						simPrec = this.sims.GetElement(curPoint, precExamplar.intValue());
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
						simFollow = this.sims.GetElement(curPoint, followingExamplar.intValue());
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
	 * given a data point, finds the closest preceding examplar and the closest following one
	 * @param pointIndex
	 * @param examplars
	 * @return
	 * @throws Exception
	 */
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
	
	
	
	/**
	 * returns the point with highest avail+resp even if it is not labelled as an examplar
	 * @param index
	 * @return
	 * @throws Exception
	 */
	int GetMostLikelyExamplar(int index) throws Exception
	{
		double dMax = Double.NEGATIVE_INFINITY;
		int examplar = -1;
		
		int rowStart = this.sims.GetRowStart(index);
		int rowEnd = this.sims.GetRowEnd(index);
		
//		System.out.println("Assigning point " + index);
		
		for (int i = rowStart; i <= rowEnd; i++)
		{
			//if (i == nIndex)
			//	continue;
			
			double avail = this.avail.GetElement(index, i);
			double resp = this.resp.GetElement(index, i);
			
			double d = resp + avail ;
			
//			System.out.println("\t" + i + ": avail:" + avail + " resp: " + resp + "\tsum: "+ d);
			
			if (d > dMax)
			{
				examplar = i;
				dMax = d;
			}
		}
		
//		System.out.println("Most likely examplar for " + index +  " is " + examplar);
		return examplar;
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
	
	public ArrayList<Integer> GetHypoBreaksAsList(TreeMap<Integer, TreeSet<Integer>> assigns)
	{
		Integer[] breaks = this.GetHypoBreaks(assigns);
		ArrayList<Integer> listBreaks = new ArrayList<Integer>();
		for (Integer br: breaks)
		{
			listBreaks.add(br);
		}
		return listBreaks;
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
	
	public int getNumPoints() {
		return numPoints;
	}

	public int getWinSize() {
		return winSize;
	}

	public int getLayerId() {
		return layerId;
	}

	public IMatrix getSims() {
		return sims;
	}

	public double[] getPrefs() {
		return prefs;
	}

	public IMatrix getAvail() {
		return avail;
	}

	public IMatrix getResp() {
		return resp;
	}
	
	public int GetTotalNumberOfLayers() {
		return totalNumberOfLayers;
	}

	public void SetTotalNumberOfLayers(int totalNumberOfLayers) {
		this.totalNumberOfLayers = totalNumberOfLayers;
	}
	
	public void PrintDownwardFis()
	{
		System.out.println("\nFis (going to previous level from this one):");
		if (this.GetDownwardFis() == null)
		{
			return;
		}
		for (int i = 0; i < this.GetDownwardFis().length; i++)
		{
			System.out.print(i + " \t,");
		}
		System.out.println();
		for (int i = 0; i < this.GetDownwardFis().length; i++)
		{
			System.out.print(this.GetDownwardFis()[i] + "\t,");
		}
		System.out.println();
	}
	
	public void PrintUpwardRos()
	{
		System.out.println("Ros (upward):");
		if (this.GetUpwardRos() == null)
		{
			return;
		}
		for (int i = 0; i < this.GetUpwardRos().length; i++)
		{
			System.out.print(i + " \t,");
		}
		System.out.println();
		for (int i = 0; i < this.GetUpwardRos().length; i++)
		{
			System.out.print(this.GetUpwardRos()[i] + "\t,");
		}
		System.out.println();
	}
	
	/**
	 * method for printing avail, resp and sim matrices
	 * @param matrix
	 * @param matrixName
	 */
	public void PrintSparseMatrix (IMatrix matrix, String matrixName) throws Exception
	{
		int numCols = matrix.GetNumColumns();
		int numRows = matrix.GetNumRows();
		
		System.out.println("Printing " + matrixName);
		
		StringBuilder str = new StringBuilder();
		str.append("**\t");
		for (int col = 0; col < this.getNumPoints(); col++)
		{
			str.append(col + ",\t");
		}
		str.append("\n");	
		
		
		for (int row = 0; row < numRows; row++)
		{
			str.append(row + "\t");
			for (int col = 0; col < numRows; col++)
			{
				if (col < matrix.GetRowStart(row) || col > matrix.GetRowEnd(row) )
				{
					str.append(col + ":...,\t");
					continue;
				}
				
//				String label = String.format(" : %.3f, ", Float.valueOf(String.valueOf( matrix1[r][c] )));
				String label = String.format(":%.2" +
						"f,\t ", Float.valueOf(String.valueOf( matrix.GetElement(row, col) )));
				str.append( col + label);
			}
			str.append("\n");
		}
		str.append("\n");
		System.out.print(str.toString());
		
	}

	/**
	 * This is a debugging method. It prints all message matrices
	 */
	public void PrintMatrices() throws Exception
	{
		System.out.println("Layer " + this.layerId);
		this.PrintIncomingRhos();
		this.PrintDownwardFis();
		this.PrintPrefs();
		this.PrintSparseMatrix(this.sims, "SIMS");
		this.PrintSparseMatrix(this.avail, "AVAIL");
		this.PrintSparseMatrix(this.resp, "RESP");
		this.PrintUpwardRos();
		this.PrintIncomingFis();
	}
	
	public double[] GetPrefs()
	{
		return this.prefs;
	}
	
	public void PrintPrefs()
	{
		StringBuilder str = new StringBuilder();
		str.append("PREFS:\n");
		for (int i = 0; i < this.GetPrefs().length; i++)
		{
			double el = this.GetPrefs()[i];
			String label = String.format(":%.2" +
					"f,\t ", Float.valueOf(String.valueOf( el )));
			str.append( i + label);
		}
		
		System.out.println(str.toString());
	}
	
	public void PrintIncomingFis() throws Exception
	{
		StringBuilder str = new StringBuilder();
		str.append("INCOMING FIS:\n");
		double[] array = this.GetFisFromAbove();
		if (array == null)
		{
			str.append("no fis coming from above");
			System.out.print(str.toString());
			return;
		}
		
		for (int i = 0; i < this.GetFisFromAbove().length; i++)
		{
			double el = this.GetFisFromAbove()[i];
			String label = String.format(":%.2" +
					"f,\t ", Float.valueOf(String.valueOf( el )));
			str.append( i + label);
		}
		
		System.out.println(str.toString());
		
	}
	
	public void PrintIncomingRhos() throws Exception
	{
		StringBuilder str = new StringBuilder();
		str.append("INCOMING RHOSS:\n");
		double[] array = this.GetRosFromBelow();
		if (array == null)
		{
			str.append("no Rhos coming from below");
			System.out.print(str.toString());
			return;
		}
		
		for (int i = 0; i < this.GetRosFromBelow().length; i++)
		{
			double el = this.GetRosFromBelow()[i];
			String label = String.format(":%.2" +
					"f,\t ", Float.valueOf(String.valueOf( el )));
			str.append( i + label);
		}
		
		System.out.println(str.toString());
		
	}
	

}
