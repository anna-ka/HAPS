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


package similarity;



import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import datasources.IDataSource;
import datasources.TextFileIO;


import com.aliasi.matrix.SparseFloatVector;


/**
 * This class is obsolete. Use GenericCosineSimComputer instead.
 * a class that computes cosine similarities between all chunks of text in an IDataSource
 * @see GenericCosineSimComputer
 * 
 * */
public class CosineSimComputer implements ISimComputer {
	
	IDataSource dataSource = null;
	ISimMetric simMetric = null;
	
	int MAX_NUM_POINTS = 1000;
	TripletSim minSim = new TripletSim(0,0, 1.0);
	TripletSim maxSim = null;
	double netSim = 0.0;
	double meanSim = 0.0;
	// a counter to keep track of positive similarities. 
	int positiveSimCounter = 0;
	
	boolean useSparseMatrix = false;
	int windowSize = -1;
	
	LinkedList<TripletSim> similarities = null;
	File outputDir = null;
	TokenDictionary tokenDict;
	
	ArrayList<SentTokenVector> sentVectors = null;
	
	
	String fieldDelimiter = "\t"; // column delimiter for MatLab files
	String lineDelimiter = "\n";
	static Pattern namePattern = Pattern.compile("[^\\.]+"); // a pattern to identify only the name of the file name, not the extensions
	
	public CosineSimComputer()
	{
		
	}
	
	public void Init(IDataSource data) {
		this.dataSource = data;
		this.simMetric = new CosineVectorSimilarity();

	}
	
	public void SetUp(TokenDictionary dict,
			int window, 
			boolean isSparse, 
			File outputDirectory)
	{
		this.tokenDict = dict;
		this.windowSize = window;
		this.useSparseMatrix = isSparse;
		this.outputDir = outputDirectory;
		
		this.similarities = new LinkedList<TripletSim>();
	}

	public void ComputeSimilarities() throws TooManyPointsException{
		
		if (this.useSparseMatrix == false)
			ComputeDenseSimMatrix();
		else
			ComputeSparseSimMatrix();
	}
	
	public void ComputeSentenceVectors()
	{
		int numSent = this.dataSource.GetNumChunks();
		
		//create token-frequency vectors for all chunks of text
		ArrayList<SentTokenVector> sentTokenVectors = new ArrayList<SentTokenVector>();
		for (int i = 0; i < numSent; i++ )
		{
			String chunk = this.dataSource.GetChunk(i);
			//SparseFloatVector sentVector = SentTokenVector.ProcessSent(chunk, this.tokenDict);
			SentTokenVector curSent = new SentTokenVector(chunk, this.tokenDict, i, false);
			sentTokenVectors.add(curSent);
		}
		this.sentVectors = sentTokenVectors;
		System.out.println("created token-frequency vectors");
	}
	
	public void SetSentenceVectors(ArrayList<SentTokenVector> sentences)
	{
		this.sentVectors = sentences;
	}
	
	public void ForgetSentVectors()
	{
		this.sentVectors = null;
	}
	
	private void ComputeDenseSimMatrix() throws TooManyPointsException
	{
		//LinkedList<TripletSim> refSims = new LinkedList<TripletSim>();
		CosineVectorSimilarity metric = (CosineVectorSimilarity) this.simMetric;
		
		try{
		//System.out.println("ComputeDenseSimMatrix()");
		int numSent = this.dataSource.GetNumChunks();
		
		if (numSent > this.MAX_NUM_POINTS )
		{
			//System.out.println("too many data points to use a dense matrix. use a sparse matrix instead.");
			TooManyPointsException e = new TooManyPointsException("Exception: running ComputeComputeDenseSimMatrix with " + String.valueOf(numSent) +
					" point. Max number is " + String.valueOf(this.MAX_NUM_POINTS) + ". Use ComputeSparseSimMatrix");
			throw e;
		}
		
		
		
		
		
		//now compute similarities
		
		for (int i = 0; i < this.sentVectors.size(); i++)
		{
			SparseFloatVector vector1 = this.sentVectors.get(i).getFreqVector();
			//System.out.println("SENT 1:\t" + this.dataSource.GetChunk(i));
			
			for (int j = 0; j < this.sentVectors.size(); j++)
			{
				//since the matrix is symmetric we will only compute a half of values
				//if (j >= i)
				//	break;
				
				SparseFloatVector vector2 = this.sentVectors.get(j).getFreqVector();
				//System.out.println("SENT 2:\t" + this.dataSource.GetChunk(j));
				double sim = metric.MeasureSimilarity(vector1, vector2);
				TripletSim curTriplet = new TripletSim(i ,j ,sim);
				this.similarities.add( curTriplet );
				//System.out.println("\tSIM: " + curTriplet.ToString());
				ComputeMinMaxNetSimilarity(curTriplet);
				
				//add the reflection
				//refSims.add( new TripletSim( j + 1,i + 1,sim ) );
			}	
		}
		
		//for (TripletSim triplet : refSims)
		//	this.similarities.add(triplet);
		
		this.ComputeMeanSim();
		//System.out.println("computed sims");
		
	}
	catch (Exception e)
	{
		System.out.println("Exception in ComputeDenseSimMatrix");
		System.out.println(e.getMessage()+ "\n" + e.getStackTrace());
	}
		
	}

	private void ComputeMinMaxNetSimilarity(TripletSim curTriplet)
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
		//this.netSim = this.netSim  + (curTriplet.similarity * 2);
		this.netSim += (curTriplet.similarity );
		//System.out.println("Net sim after " + curTriplet.ToString() + ":\t" + String.valueOf(this.netSim));
		if (curTriplet.similarity > 0)
			this.positiveSimCounter+=1 ;
			//this.positiveSimCounter+=2 ;
	}
	
	private void ComputeSparseSimMatrix()
	{
		try{
			
			CosineVectorSimilarity metric = (CosineVectorSimilarity) this.simMetric;
			
			//now compute similarities within a window's distance from each sent
			for (int i = 0; i < this.sentVectors.size(); i++)
			{
				SparseFloatVector vector1 = this.sentVectors.get(i).getFreqVector();
				
				
				int offset = (int)Math.ceil( this.windowSize / 2);
					
				int windowStart = i - offset;
				int windowEnd = windowStart + this.windowSize - 1 ;
				
				for (int j = 0; j < this.sentVectors.size(); j++)
				{
					
					SparseFloatVector vector2 = this.sentVectors.get(j).getFreqVector();
	
					double sim ;
					TripletSim curTriplet = null;
					if (j < windowStart || j > windowEnd )
					{
						continue;
					}
					else
					{
						sim = metric.MeasureSimilarity(vector1, vector2);
						curTriplet = new TripletSim(i ,j ,sim);
					}
						
					this.similarities.add( curTriplet );
					ComputeMinMaxNetSimilarityForSparse(curTriplet);
					
				}
			}
			
			this.ComputeMeanSim();
			//System.out.println("computed sims");
			
		}
		catch (Exception e)
		{
			System.out.println("Exception in AffinityPropagationSegmentor.ComputeSparseMatrix");
			System.out.println(e.getMessage());
		}
		
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
	
	/*what we are computing is a mean of positive similarities only
	 * the regular mean is almost always 0
	 * */
	private void ComputeMeanSim()
	{
		
		this.meanSim = this.netSim /  this.positiveSimCounter;
//		System.out.println("positive sims: " + String.valueOf(this.positiveSimCounter));
	}
	

	public void OutputSimilarities(File outputDir) {
		try {
			this.GenerateMatLabFiles();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void GenerateMatLabFiles() throws Exception
	{
		
		String name = this.dataSource.GetName();
		Matcher m1 = namePattern.matcher(name);
		if (m1.find())
		{
			name = m1.group(0);
			//System.out.println("new name " + newName);
		}
		
		
		File sentFile = new File(this.outputDir, name + "_sent_text.txt");
		File simFile = new File(this.outputDir, name + "_sent_sim.txt");
		//file to contain preferences set to minimum similarity, for smaller clusters
		File pref1 = new File(this.outputDir, name + "_sent_pref_min.txt");
		//file to contain preferences set to mean similarity, for moderate number of clusters
		File pref2 = new File(this.outputDir, name + "_sent_pref_mean.txt");
		
		StringBuilder text = new StringBuilder();
		for (int s = 0; s < this.dataSource.GetNumChunks(); s++)
		{
			String sent = String.valueOf(s ) + "\t" + this.dataSource.GetChunk(s) + "\n";
			text.append(sent);
		}
		
		TextFileIO.OutputFile(sentFile, text.toString());
		
		//output similarities
		StringBuilder simText = new StringBuilder();
		for (TripletSim triplet: this.similarities)
		{
			String line = String.valueOf( triplet.firstId ) + this.fieldDelimiter + String.valueOf( triplet.secondId )+ this.fieldDelimiter  + String.valueOf( triplet.similarity ) + this.lineDelimiter;
			simText.append(line);
		}
		TextFileIO.OutputFile(simFile, simText.toString());
		
		//output preferences
		StringBuilder meanPref = new StringBuilder();
		StringBuilder minPref = new StringBuilder();
		 
		 for (int i = 0; i < this.dataSource.GetNumChunks(); i++)
		 {
			 meanPref.append(String.valueOf(this.meanSim) + this.lineDelimiter);
			 minPref.append(String.valueOf(this.minSim.similarity) + this.lineDelimiter);
		 }
		TextFileIO.OutputFile(pref1, minPref.toString() );
		TextFileIO.OutputFile(pref2, meanPref.toString() );
	}

	public LinkedList<TripletSim> GetSimilarities() {
		
		return this.similarities;
	}

	public IDataSource GetRawData() {
		
		return this.dataSource;
	}

	
	public int GetPointsNumber() {
		
		return this.dataSource.GetNumChunks();
	}

	public boolean GetIfSparse() {
		// TODO Auto-generated method stub
		return this.useSparseMatrix;
	}

	public int GetWindowSize() {
		// TODO Auto-generated method stub
		if (!this.useSparseMatrix)
			return this.GetPointsNumber();
		else
			return this.windowSize;
			
	}

}


