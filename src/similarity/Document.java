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

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import datasources.IDataSource;


import com.aliasi.matrix.SparseFloatVector;

/**
 * This class is obsolete.
 * 
 * a class that hold a list of sentence representations
 *  as vectors to be used in tf.idf computations, so it may not correspond to the whole document but only to a part of it
 *  if this is how we compute tf.idf
 *  
 * @author anna
 *
 */
public class Document {

	private ArrayList<SentTokenVector> sentVectors = null;

	private TokenDictionary tokenDict = null;
	
	//term frequencies
	private TreeMap<Integer, Double> tfDictionary = null;
	//tf idf values
	private TreeMap <Integer, Double> tfIdf = null;
	//document frequencies
	private DfDictionary docFreqs = null;
	int numDocsInCorpus = -1;
	IDataSource dataSource = null;
	
	public DfDictionary getDocFreqs() {
		return docFreqs;
	}

	public void setDocFreqs(DfDictionary docFreqs) {
		this.docFreqs = docFreqs;
	}

	public Document(TokenDictionary dict, int numDocs, IDataSource data)
	{
		this.tokenDict = dict;
		this.tfDictionary = new TreeMap<Integer, Double>();
		this.sentVectors = new ArrayList<SentTokenVector>();
		this.tfIdf = new TreeMap <Integer, Double>();
		//initialize term frequencies map
		for (Integer tokenId: this.tokenDict.GetAllTokenIds() )
		{
			
			this.tfDictionary.put(tokenId, new Double(0));
			this.tfIdf.put(tokenId, new Double(0));
		}
		this.numDocsInCorpus = numDocs;
		this.dataSource = data;
	}
	
	public void Init()
	{
		//calculate frequencies for all chunks in this document
		try{
			int numSent = this.dataSource.GetNumChunks();
			for (int i = 0; i < numSent; i++ )
			{
				String chunk = this.dataSource.GetChunk(i);
				SentTokenVector curSent = new SentTokenVector(chunk, this.tokenDict, i, true);
				this.AddSent(curSent);
			}
//			System.out.println("created token-frequency vectors");
			
		}
		catch (Exception e)
		{
			
		}
	}
	
	public void AddSent(SentTokenVector newSent)
	{
		this.sentVectors.add(newSent);
		//process tokens found in this sentence and increment counters in tf vector:
		SparseFloatVector sentFreqs = newSent.getFreqVector();
		int[] nonZeroKeys = sentFreqs.nonZeroDimensions();
		for (int tokenId: nonZeroKeys)
		{
			int tokenFreq = (int) sentFreqs.value(tokenId);
			Double oldTf = this.tfDictionary.get(new Integer(tokenId));
			this.tfDictionary.put(tokenId, new Double (oldTf.intValue() + tokenFreq));
		}
	}
	
	//same variant as Malioutov: tfIdfWeights_[i][j] = termCounts[i][j] * 1.0 * Math.log((numSegments_) / documentCount[i]);
	public void ComputeTfIdf()
	{
		for (Integer tokenId: this.tokenDict.GetAllTokenIds())
		{
			
			//System.out.print(tokenId.toString() + "\t");
			//get the id for dfDictionary
			String token = this.tokenDict.GetTokenString(tokenId);
			try{
			//System.out.println(token + "\t");
			Integer tokenDfId = this.docFreqs.GetTokenId(token);
			if (tokenDfId == null)
			{
				System.out.println("Warning in Document.ComputeTfIdf. Token not found in this.docFreqs:\t" + token);
				continue;
			}
			//System.out.print(tokenDfId.toString() + "\n");
			
			Double tf = this.tfDictionary.get(tokenId);
			Double df = this.docFreqs.getDfDictionary().get(tokenDfId);
			Double tfIdf = 0.0;
			if (df == 0)
			{
				System.out.println("Warning in ComputeTfIdf: df is 0 for " + token);
			}
			else if (tf != 0 )
			{	
//				double idf = Math.log(this.numDocsInCorpus / df);
//				double tmp = 1 + Math.log(tf);
//				tfIdf = new Double ( tmp * idf );
				tfIdf = CalculateTfIdf(tf, df, this.numDocsInCorpus);

			}
			this.tfIdf.put(tokenId, tfIdf);
			}
			catch (Exception e)
			{
				this.tfIdf.put(tokenId, 0.0);
				System.out.println("Warning: in ComputeTfIdf. Recorded 0 for " + token);
				System.out.println(e.getMessage());
			}
		}
	}
	
	public double CalculateTfIdf(double tf, double df, int totalNumDocs)
	{
		
		double idf = Math.log10(totalNumDocs / df);
		double tmp = 1 + Math.log10(tf);
		double tfIdf = tmp * idf ;
		return tfIdf;
	}
	
	public double CalculateWeightedTfIdf(double tf, double df, int totalNumDocs)
	{
		double idf = Math.log(totalNumDocs / df);
		double tmp = 1 + Math.log(tf);
		double tfIdf = tmp * idf ;
		return tfIdf;
	}
	
	public void PrintFreqs()
	{
		System.out.println("id\ttoken\ttf\tdf\tidf:");
		Set<Integer> tokenIds = this.tokenDict.GetAllTokenIds();
		for (Integer curId: tokenIds)
		{
			
			String curToken = this.tokenDict.GetTokenString(curId);
			Integer dfId = this.docFreqs.GetTokenId(curToken);
			if (dfId == null)
			{
				System.out.println("Warning in Document.PrintFreqs. Token not found in this.docFreqs:\t" + curToken);
				continue;
			}
			
			String tf = String.valueOf(this.tfDictionary.get( curId));
			String df = String.valueOf(this.docFreqs.getDfDictionary().get( dfId));
			String tfIdf = String.valueOf(this.tfIdf.get(curId));
			
			System.out.println( curId.toString() + "\t" + curToken + "\t" + tf + "\t" + df + "\t" + tfIdf );
			if (curId > 40)
				break;
		}
	}
	
	public void PrintTfIdf()
	{
		System.out.println("TfIdf:");
		Set<Integer> tokenIds = this.tokenDict.GetAllTokenIds();
		for (Integer curId: tokenIds)
		{
			String curToken = this.tokenDict.GetTokenString(curId);
			System.out.println(curId.toString() + "\t" + curToken + ":\t" + String.valueOf(this.tfIdf.get( curId)) );
			if (curId > 40)
				break;
		}
	}
	
	public void PrintTf()
	{
		System.out.println("Tf:");
		Set<Integer> tokenIds = this.tokenDict.GetAllTokenIds();
		for (Integer curId: tokenIds)
		{
			String curToken = this.tokenDict.GetTokenString(curId);
			System.out.println(curId.toString() + "\t" + curToken + ":\t" + String.valueOf(this.tfDictionary.get( curId)) );
			if (curId > 40)
				break;
		}
	}
	
	//apply smoothing to frequency vectors for sentences:
	// in addition to counting tokens in the sentence, also count tokens in adjacent sentences
	//following Malioutov and Barzilay 2006
	
	//winSize: how many neighboring sentences to consider
	// alpha: smoothing parameter
	public void SmoothSentCounts(int winSize, double alpha)
	{
		ArrayList<SentTokenVector> oldSents = new ArrayList<SentTokenVector> (this.sentVectors);
		int curSentIndex = 0;
		int adjoinIndex = 0;
		for (; curSentIndex < oldSents.size()  ; curSentIndex++)
		{
			
			SparseFloatVector curFreqVector = this.sentVectors.get(curSentIndex).getFreqVector();
			
			//this.sentVectors.get(curSentIndex).PrintVector();
			
			//DenseVector modifications = new DenseVector(curFreqVector.numDimensions());
			TreeMap<Integer, Double> modCounts = new TreeMap<Integer, Double>();
			int[] nonZeroIndices = curFreqVector.nonZeroDimensions();
			for (int curDimension: nonZeroIndices)
			{
				modCounts.put(new Integer(curDimension), new Double (curFreqVector.value(curDimension)) );
			}
			
			//perform the summations within the adjoining window of sentences
			for (adjoinIndex = curSentIndex+1; adjoinIndex <= curSentIndex + winSize && adjoinIndex < oldSents.size(); adjoinIndex++)
			{
				SparseFloatVector adjoinFreqVector = oldSents.get(adjoinIndex).getFreqVector();
				double coefficient = Math.pow( Math.E, -alpha * (adjoinIndex - curSentIndex) );
				//System.out.println("curIndex\t" + String.valueOf(curSentIndex) + "\tadj index\t" + String.valueOf(adjoinIndex) 
				//		+ "\tcoef\t" + String.valueOf(coefficient));

				int[] indices = adjoinFreqVector.nonZeroDimensions();
				
				
				
				for (int i = 0; i < indices.length; i++)
				{
					Integer curIndex = new Integer ( indices[i] );
					Double oldFreq  = 0.0;
					if (modCounts.containsKey(curIndex) == true)
						oldFreq = modCounts.get(curIndex);
					Double modFreq =  adjoinFreqVector.value(curIndex.intValue()) * coefficient;
					modCounts.put(curIndex, oldFreq + modFreq);	
				}	
			}
			
			SparseFloatVector newFreqVector = new SparseFloatVector (modCounts, curFreqVector.numDimensions());
			this.sentVectors.get(curSentIndex).setFreqVector(newFreqVector);	
			
			//this.sentVectors.get(curSentIndex).PrintVector();
		}
		
	}
	
	public void ApplyTfIdfWeighting()
	{
		for (SentTokenVector sent: this.sentVectors)
		{
			TreeMap <Integer, Double> modSent = new TreeMap<Integer, Double>();
			int[] nonZeroIndices = sent.getFreqVector().nonZeroDimensions();
			for (int i = 0; i < nonZeroIndices.length; i++)
			{
				int curTokenIndex = nonZeroIndices[i];
				//get tf.idf for this token
				Double tfIdf = this.tfIdf.get(new Integer(curTokenIndex));
				Double oldValue = new Double ( sent.getFreqVector().value(curTokenIndex) );
				//Double newValue = tfIdf * Math.exp( oldValue );
				
				
				Double newValue = tfIdf * oldValue ;
				//Double newValue =( 0.5* oldValue)+ (0.5 * tfIdf * oldValue) ;
				modSent.put(new Integer(curTokenIndex), newValue);
			}
			SparseFloatVector newFreqVector = new SparseFloatVector (modSent, sent.getFreqVector().numDimensions());
			sent.setFreqVector(newFreqVector);
		}
		
	}
	
	public void PrintSentVectors(int start, int end)
	{
		System.out.println("sentence vectors:");
		if (start >= this.sentVectors.size() )
			return;
		for (int i = start; i <= end && i < this.sentVectors.size(); i++)
		{
			SentTokenVector sent = this.sentVectors.get(i);
			sent.PrintVector();
		}
	}

	public ArrayList<SentTokenVector> getSentVectors() {
		return sentVectors;
	}
	
	
}
