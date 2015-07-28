package similarity;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import segmenter.DenseMatrix;
import segmenter.IMatrix;
import segmenter.WindowMatrix;

import com.aliasi.matrix.SparseFloatVector;

import datasources.IGenericDataSource;
import datasources.TextFileIO;

/**
 * This class computer co-referential similatiry between every pair two chunks in a document
 * @author anna
 *
 */
public class CorefSimComputer implements IGenericSimComputer {
	
	int MAX_NUM_POINTS = 1000;
	
	IGenericDataSource dataSource = null;
	GenericDocument curDoc = null;
	ArrayList<SentSyntacticVector> vectors = null;
	
	boolean useSparseMatrix = false;
	int windowSize = -1;
	
	//LinkedList<TripletSim> similarities = null;
	IMatrix similarities = null;
	
	File outputDir = null;
	
	int numPoints = 0;
	int numLexicalDimensions = 0;
	
	Double decayFactor = new Double (-1);
	int decayWindow = -1; 
	IGenericSimComputer lexicalSims = null;
	ArrayList<Double> expWeights = new ArrayList<Double>();
	
	ArrayList<SentTokenVector> lexicalSentVectors;
	MapOfSimilarities lexSimMap = null;
	
	TripletSim minSim = new TripletSim(0,0, 1.0);
	TripletSim maxSim = null;
	double netSim = 0.0;
	double meanSim = 0.0;
	// a counter to keep track of positive similarities. 
	int positiveSimCounter = 0;
	

	@Override
	public void Init(IGenericDataSource data) {
		this.dataSource = data;
		this.similarities = new DenseMatrix(dataSource.GetNumberOfChunks());

	}
	
	@Override
	public void dispose()
	{}
	
	public void SetUp(GenericDocument doc, Double decayFactor, int decayWinSize, IGenericSimComputer lexicalSims) throws Exception
	{
		String className = this.dataSource.getClass().getName();
		if (className.endsWith("ConnexorXMLSimpleDS") == false && className.endsWith("ConnexorXMLMultipleAnnotsDS") == false)
		{
			String msg = "Exception in CorefSimComputer.SetUp: invalid type of dataSource: " + className;
			throw (new Exception(msg));
		}
		this.curDoc = doc;
		this.vectors = doc.GetSyntacticVectors();
		
		this.numPoints = vectors.size();
		this.decayFactor = decayFactor;
		this.decayWindow = decayWinSize;
		this.lexicalSims = lexicalSims;
		this.lexicalSentVectors = this.lexicalSims.GetSentenceVectors();
				//((GenericCosineSimComputer)this.lexicalSims).GetSentenceVectors();
		this.lexSimMap = new MapOfSimilarities();
		this.lexSimMap.Init(this.lexicalSims.GetSimilarities());
		
		this.numLexicalDimensions = this.curDoc.getTokenDict().GetDictDimensions();
		
		//for now we will weight all expression types equally
		for (String s: SentSyntacticVector.exprNames)
		{
			this.expWeights.add(new Double (1));
		}
		
		this.SetExpressionWeights();
	}
	
	public void SetExpressionWeights()
	{
		
		 // weights for moonstone. we want to focus on people.
		 
		this.expWeights.set(SentSyntacticVector.PERSONAL_PRONOUN_ANIM , new Double(4));
		this.expWeights.set(SentSyntacticVector.DEMONSTR_PRONOUN_ANIM, new Double(2));
		this.expWeights.set(SentSyntacticVector.PROPER_NAME_ANIM, new Double(1));
		this.expWeights.set(SentSyntacticVector.DEF_NP_ANIM , new Double(0.5));
		this.expWeights.set(SentSyntacticVector.INDEF_NP_ANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.MOD_BY_PER_PRON_POSESS_ANIM, new Double(0.5));
		this.expWeights.set(SentSyntacticVector.MOD_BY_GEN_NP_ANIM, new Double(1));
	
		this.expWeights.set(SentSyntacticVector.PERSONAL_PRONOUN_INANIM , new Double(1));
		this.expWeights.set(SentSyntacticVector.DEMONSTR_PRONOUN_INANIM, new Double(2));
		this.expWeights.set(SentSyntacticVector.PROPER_NAME_INANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.DEF_NP_INANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.INDEF_NP_INANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.MOD_BY_PER_PRON_POSESS_INANIM, new Double(0.5));
		this.expWeights.set(SentSyntacticVector.MOD_BY_GEN_NP_INANIM, new Double(0.5));
		
		
		/**
		 * weights for ai lectures
		
		this.expWeights.set(SentSyntacticVector.PERSONAL_PRONOUN_ANIM , new Double(2));
		this.expWeights.set(SentSyntacticVector.DEMONSTR_PRONOUN_ANIM, new Double(3));
		this.expWeights.set(SentSyntacticVector.PROPER_NAME_ANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.DEF_NP_ANIM , new Double(0));
		this.expWeights.set(SentSyntacticVector.INDEF_NP_ANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.MOD_BY_PER_PRON_POSESS_ANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.MOD_BY_GEN_NP_ANIM, new Double(1));
	
		this.expWeights.set(SentSyntacticVector.PERSONAL_PRONOUN_INANIM , new Double(4));
		this.expWeights.set(SentSyntacticVector.DEMONSTR_PRONOUN_INANIM, new Double(5));
		this.expWeights.set(SentSyntacticVector.PROPER_NAME_INANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.DEF_NP_INANIM, new Double(0));
		this.expWeights.set(SentSyntacticVector.INDEF_NP_INANIM, new Double(-1));
		this.expWeights.set(SentSyntacticVector.MOD_BY_PER_PRON_POSESS_INANIM, new Double(0.5));
		this.expWeights.set(SentSyntacticVector.MOD_BY_GEN_NP_INANIM, new Double(0.5));
		 */
	}

	@Override
	public void ComputeSimilarities() throws Exception {
			try{
				
				//CosineVectorSimilarity metric = (CosineVectorSimilarity) this.simMetric;
				
				int segmLevel = IGenericDataSource.SENT_LEVEL;

				
				//now compute similarities within a window's distance from each sent
				for (int i = 0; i < this.vectors.size(); i++)
				{
					ArrayList<Integer> vector1 = this.vectors.get(i).GetCounts();
					segmLevel = this.vectors.get(i).GetSegmLevel();
					int prevIndex = i - 1;
					
					for (; prevIndex >= 0 && (i - this.decayWindow) <= this.decayWindow; prevIndex--)
					{
						
						ArrayList<Integer> vector2 = this.vectors.get(prevIndex).GetCounts();
						int distance = i - prevIndex;
						
						Double rawSim = ComputeRawSim(i, prevIndex);
						Double decayedSim = DecaySim( distance, rawSim);
						
						 
						Double adjustedSim = decayedSim;
						
						if (distance > 1 )
							adjustedSim = AdjustSim(decayedSim, i, prevIndex);
						
						TripletSim curTriplet = new TripletSim(i, prevIndex, adjustedSim);
						this.similarities.SetElement(i, prevIndex, adjustedSim);
						
						ComputeMinMaxNetSimilarityForSparse(curTriplet);					
					}					
			}
			this.ComputeMeanSim();
			System.out.println("computed coref sims");
		}
		catch (Exception e)
		{
			System.out.println("Exception in CorefSimComputer.ComputeSimilarities: " + e.getMessage());
			throw (e);
		}

	}
		
	public Double ComputeRawSim(int curSentIndex, int prevSentIndex) throws Exception
	{
		ArrayList<Integer> curVector = this.vectors.get(curSentIndex).GetCounts();
		//ArrayList<Integer> prevVector = this.vectors.get(prevSentIndex).GetCounts();
		
		//get the denominators
		SparseFloatVector curLexVector = this.lexicalSentVectors.get(curSentIndex).getFreqVector();
		SparseFloatVector prevLexVector = this.lexicalSentVectors.get(curSentIndex).getFreqVector();
		
		int[] dims1 = curLexVector.nonZeroDimensions();
		if (dims1.length <= 0)
		{
			throw (new Exception("Exception in ComputeRawSim: no non-zero dimentsions in dims1"));
		}
		int[] dims2 = prevLexVector.nonZeroDimensions();
		if (dims2.length <= 0)
		{
			throw (new Exception("Exception in ComputeRawSim: no non-zero dimentsions in dims2"));
		}
		
		Double denom1 = new Double(0);
		Double denom2 = new Double(0);
		
		//cpmpute magnitude of the first vector
		for (int i = 0; i < dims1.length; i++)
		{
			int curDim = dims1[i];
			double curVal = curLexVector.value(curDim);
			denom1 += Math.pow(curVal, 2);
		}
		denom1 = Math.pow(denom1, 0.5);
		
		//compute magnitude of the second vector
		for (int i = 0; i < dims2.length; i++)
		{
			int curDim = dims2[i];
			double curVal = prevLexVector.value(curDim);
			denom2 += Math.pow(curVal, 2);
		}
		denom2 = Math.pow(denom2, 0.5);
		
		Double denom = denom1*denom2;
		
		
		//get vector magnitudes for the denominator
//		Double magn1 = curLexVector.length();
//		Double magn2 = prevLexVector.length();
		
		//now approximate dot product for the numerator
		Double sim = new Double(0);
		for (int dim = 0; dim < curVector.size(); dim++)
		{
			Integer curCount = curVector.get(dim);
			//apply weight
			Double curWeight = this.expWeights.get(dim);
			Double weightedCount = new Double (curCount) * curWeight;
			
			sim += weightedCount;
			
			
		}
		
		Double answer = sim / denom;
		return (answer);
	}
	
	public Double DecaySim (int dist, Double sim) throws Exception
	{
		Double power = new Double(dist) * this.decayFactor;
		//Double newSim = Math.pow(sim, power);
		Double newSim = Math.pow(sim, dist);
		return newSim;
	}
	
	public Double AdjustSim (Double sim, int curInd, int prevIndex) throws Exception
	{
		Double curLexSim = this.GetLexicalSim(curInd, prevIndex);
		if (curLexSim == null)
			curLexSim = this.GetLexicalSim( prevIndex, curInd);
		Double newSim = curLexSim * sim;
		return newSim;
	}
	
	private Double GetLexicalSim(int firstInd, int secondInd) throws Exception
	{
		Double sim = this.lexSimMap.GetSimilarity(firstInd, secondInd);
		
		if (sim == null)
			sim = this.lexSimMap.GetSimilarity( secondInd, firstInd);
		//this is a rare case when decay window in coref is larger then sliding window in regular similarity computations
		if (sim == null)
			sim = new Double (0);
		
		return (sim);
	}


	@Override
	public IMatrix GetSimilarities() {
		return this.similarities;
	}

	@Override
	public void OutputSimilarities(File outputDir) throws Exception{
		
		File outputFile = new File (outputDir, "sims_from_corefSimComputer.txt");
		StringBuilder str = new StringBuilder();
		
		//for (TripletSim curTriplet: this.similarities)
		for (int i = 0; i < this.similarities.GetNumRows(); i++)
		{
			for (int j = this.similarities.GetRowStart(i); j <= this.similarities.GetRowEnd(i); j++)
			{
				int firstInd = i;
				int secondInd = j;
				
				String text1 = firstInd + "[" + this.dataSource.GetChunk(firstInd) + "]";
				String text2 = secondInd + "[" + this.dataSource.GetChunk(secondInd) + "]";
				str.append(this.similarities.GetElement(i, j));
				str.append("\t" + text1 + "\n");
				str.append("\t" + text2 + "\n\n");
			}
		}
		
		TextFileIO.OutputFile(outputFile, str.toString());

	}

	@Override
	public IGenericDataSource GetRawData() {
		
		return this.dataSource;
	}

	@Override
	public int GetPointsNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int GetWindowSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean GetIfSparse() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void ComputeMinMaxNetSimilarityForSparse(TripletSim curTriplet)
	{
		//min
		if (this.minSim == null)
			this.minSim = curTriplet;
		else if (curTriplet.similarity > 0.0 &&  curTriplet.similarity < this.minSim.similarity)
			this.minSim = curTriplet;
		
		//max
		if (this.maxSim == null)
			this.maxSim = curTriplet;
		else if (this.maxSim.similarity < curTriplet.similarity)
			this.maxSim = curTriplet;
		
		
		//net counter
		//System.out.println("Net sim before " + curTriplet.ToString() + ":\t" + String.valueOf(this.netSim));
		this.netSim = this.netSim  + curTriplet.similarity;
		//System.out.println("Net sim after " + curTriplet.ToString() + ":\t" + String.valueOf(this.netSim));
	
	
	if (curTriplet.similarity > 0)
		this.positiveSimCounter++;
	}
	
	public TripletSim GetMinSim() {
		return minSim;
	}

	public TripletSim GetMaxSim() {
		return maxSim;
	}

	public double GetNetSim() {
		return netSim;
	}

	public double GetMeanSim() {
		return meanSim;
	}

	public int GetPositiveSimCounter() {
		return positiveSimCounter;
	}

	private void ComputeMeanSim()
	{
		this.meanSim = this.netSim /  this.positiveSimCounter;
	}

	@Override
	public ArrayList<SentTokenVector> GetSentenceVectors() {
		return this.lexicalSentVectors;
	}

}
