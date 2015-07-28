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
import datasources.IGenericDataSource;


import com.aliasi.tokenizer.TokenizerFactory;

//a class to keep document frequencies of terms in a corpus

public class DfDictionary implements Cloneable {
	
	IGenericDataSource[] corpus = null;
	/**
	 * all tokens in the corpus
	 */
	TokenDictionary tokenDict = null;
	/**
	 * document frequencies for all tokens in the corpus
	 */
	TreeMap<Integer, Double> dfDictionary = null;
	
	TokenizerFactory tFactory = null;
	/**
	 * number of documents in tf.idf corpus. If segment tf.idf is used, then this is number of segments
	 */
	int numDocuments = -1;
	/**
	 * for segment tf.idf, the mapping between sent id and segment id
	 */
	TreeMap<Integer, Integer> sentToSegmentMap = null;
	boolean useSegmentDf = false;
	
	@Override
	/**
	 * This method creates a deep copy of the actual dfDictionary, but everything else is shared. 
	 */
	protected Object clone() throws CloneNotSupportedException {
		
		DfDictionary cloned  = (DfDictionary)super.clone();
		TreeMap<Integer, Double> newDfDict = new TreeMap<Integer, Double>(this.dfDictionary);
		return cloned;
		
		
	}
	
	//numDocs reflects the number of documents in the corpus
	//or if we only have one document, the number of segments we should split it into
	public DfDictionary( IGenericDataSource[] corpus, //the data sources must have been initialized
			TokenizerFactory tokenFactory, int numDocs)
	{
		
		this.corpus = corpus;
		this.dfDictionary = new TreeMap<Integer, Double>();
		this.tFactory = tokenFactory;
		this.numDocuments = numDocs;
		
		
		this.tokenDict = new TokenDictionary("", this.tFactory);
		try {
			this.tokenDict.ProcessText();
		} catch (Exception e1) {
			System.out.println("Exception in DfDictionary():" + e1.getMessage());
			e1.printStackTrace();
			return;
		}
		
		//create a token dictionary containing all tokens in the corpus
		for (IGenericDataSource curData: this.corpus)
		{
			int numChunks = curData.GetNumberOfChunks();
			for (int i = 0; i < numChunks; i++)
			{
				String curChunk;
				try {
					curChunk = curData.GetChunk(i);
					this.tokenDict.AddText(curChunk);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		
		
		
		//initialize df map
		for (Integer id: this.tokenDict.GetAllTokenIds() )
		{
			this.dfDictionary.put(id, new Double(0));
		}
	}
	
	public TokenDictionary GetTokenDictionary()
	{
		return this.tokenDict;
	}
	
	//frees memory
	public void ForgetCorpus()
	{
		this.corpus = null;
	}
	
	
	/**
	 * This method computes document frequencies for each token in each document.
	 * If the size of the corpus is 1, then segment tf.idf is used. In this case,
	 * numDocs specified in the constructor is used to divide a document into numDoc segments of uniform length.
	 * 
	 * @throws Exception
	 */
	public void ProcessCorpus() throws Exception
	{
		if (corpus.length == 1)
		{
			this.useSegmentDf = true;
			this.sentToSegmentMap = new TreeMap<Integer, Integer>();
			this.ProcessDocSegments(this.corpus[0], this.numDocuments);
			return;
		}
		
		TreeMap<Integer, TokenDictionary> dicts = new TreeMap<Integer, TokenDictionary> ();
		for (int i = 0; i < this.corpus.length; i++)
		{
				IGenericDataSource curData = this.corpus[i];
				//compute term frequencies in this document;
				TokenDictionary curDict = new TokenDictionary(curData, this.tFactory);
				curDict.ProcessText();
				dicts.put(new Integer(i), curDict);
		}
		
		for (Integer curTokenId: this.tokenDict.GetAllTokenIds())
		{
			String curTokenString = this.tokenDict.GetTokenString(curTokenId);
			for (int i = 0; i < this.corpus.length; i++)
			{
				TokenDictionary curDict = dicts.get(new Integer(i));
				if (curDict.GetAllTokenStrings().contains(curTokenString))
				{
					Double oldCount = this.dfDictionary.get(curTokenId);
					this.dfDictionary.put(curTokenId, oldCount + 1);
				}			
			}
		}
	}
	
	public void ProcessDocSegments(IGenericDataSource data, int numSegm) throws Exception
	{
		int numSegments;
		if (numSegm > data.GetNumberOfChunks())
		{
			this.numDocuments = data.GetNumberOfChunks();
			numSegments = this.numDocuments;
			
			System.out.println("Warning in DfDictionary.ProcessDocSegments for doc " + data.GetName() );
			System.out.println("\tSpecified num segmentf for tf.idf > number of chunks: " + String.valueOf(numSegm));
			System.out.println("\tUsing number of chunks " + String.valueOf(numSegments));
		}
		else
		{
			numSegments = numSegm;
		}
		
		
		int segmSize = (int) Math.ceil
				( ((double) data.GetNumberOfChunks()) /( (double
						)numSegments));
		
		int curStart = 0;
		int curEnd = 0;
		String curSegm = "";
		int curSegmId = 0;
		
		TreeMap<Integer, TokenDictionary> segmDictionaries = new TreeMap<Integer, TokenDictionary> ();
		int nCh = data.GetNumberOfChunks();
		
		
		while(curStart < data.GetNumberOfChunks())
		{
			curEnd = curStart + segmSize - 1;
			//System.out.println("curStart " + String.valueOf(curStart) + " curEnd " + String.valueOf(curEnd));
			
			if (curEnd >= data.GetNumberOfChunks())
				curEnd = data.GetNumberOfChunks() - 1;
			StringBuilder sb = new StringBuilder();
			for (int i = curStart; i <= curEnd; i++)
			{
//				if (i == 410)
//				{
//					System.out.println("sent 410");
//				}
				sb.append(" " + data.GetChunk(i) + " ");
				this.sentToSegmentMap.put(new Integer(i), curSegmId);
			}

			curSegm = sb.toString();
			TokenDictionary segmDict = new TokenDictionary(curSegm, this.tFactory);
			segmDict.ProcessText();
			segmDictionaries.put(new Integer(curSegmId), segmDict);
			
			curStart = curEnd + 1;
			curSegmId++;
		}
		
		for (Integer curTokenId: this.tokenDict.GetAllTokenIds())
		{
			String curTokenString = this.tokenDict.GetTokenString(curTokenId);
			for (int i = 0; i < segmDictionaries.size(); i++)
			{
				TokenDictionary curDict = segmDictionaries.get(new Integer(i));
				if (curDict.GetAllTokenStrings().contains(curTokenString))
				{
					Double oldCount = this.dfDictionary.get(curTokenId);
					this.dfDictionary.put(curTokenId, oldCount + 1);
				}			
			}
		}
	}
	
	/**
	 * returns a mapping between sentence vector ids and segment ids. 
	 * This is useful when segment tf.ifd is used. 
	 * IF regular tf.idf is used, then null is returned.
	 */
	public TreeMap<Integer, Integer> GetSentToSegmentMap()
	{
		return this.sentToSegmentMap;
	}
	
	//use this if we only have one document. Split it into equal size segments and
	//compute dfs based on that
	public void OldProcessDocSegments(IDataSource data, int numSegments) throws Exception
	{
		
		int segmSize = (int) Math.floor( data.GetNumChunks() / numSegments);
		
		int curStart = 0;
		int curEnd = 0;
		String curSegm = "";
		
		while(curStart < data.GetNumChunks())
		{
			curEnd = curStart + segmSize;
			//System.out.println("curStart " + String.valueOf(curStart) + " curEnd " + String.valueOf(curEnd));
			
			if (curEnd >= data.GetNumChunks())
				curEnd = data.GetNumChunks() - 1;
			StringBuilder sb = new StringBuilder();
			for (int i = curStart; i <= curEnd; i++)
			{
				sb.append(" " + data.GetChunk(i) + " ");
			}
			curSegm = sb.toString();
			TokenDictionary segmDict = new TokenDictionary(curSegm, this.tFactory);
			segmDict.ProcessText();
			
			Set<String> tokens = segmDict.GetAllTokenStrings();
			for (String token: tokens)
			{
				Integer id = this.tokenDict.GetTokenId(token);
				if (id == null)
				{
					System.out.println("Warning in DfDictionary.ProcessSegments. Token in doc not found in this.tokenDict:\t" + token);
					continue;
				}
				Double oldCount = this.dfDictionary.get(id);
				this.dfDictionary.put(id, oldCount + 1);
				//System.out.println(id.toString() + "\t" + this.tokenDict.GetTokenString(id) + " old count:\t" + oldCount.toString() + " new count:\t" 
				//		+ this.dfDictionary.get(id).toString());
			}
			
			
			curStart = curEnd + 1;
		}
		
	}
	
	public void PrintDf()
	{
		Set<Integer> tokenIds = this.tokenDict.GetAllTokenIds();
		for (Integer curId: tokenIds)
		{
			String curToken = this.tokenDict.GetTokenString(curId);
			System.out.println(curId.toString() + "\t" + curToken + ":\t" + String.valueOf(this.dfDictionary.get( curId)) );
			if (curId > 400)
				break;
		}
	}
	
	public Double GetDfValue (Integer tokenId) throws Exception
	{
		try{
			return (this.getDfDictionary().get(tokenId));
		}
		catch(Exception e)
		{
			Exception exp = new Exception ("DfDictionary.GetDfValue: No token with id " + tokenId.toString() + " is found in dfDictionary.");
			throw (exp);
		}
	}


	public TreeMap<Integer, Double> getDfDictionary() {
		return dfDictionary;
	}
	
	public Integer GetTokenId(String token)
	{
		return this.tokenDict.GetTokenId(token);
	}
	
	public String GetTokenString(Integer id)
	{
		return this.tokenDict.GetTokenString(id);
	}
	
	public int GetNumDocuments()
	{
		return this.numDocuments;
	}
	
	public boolean GetUseSegmentDf()
	{
		return this.useSegmentDf;
	}
	

}
