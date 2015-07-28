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
import java.util.TreeMap;

import com.aliasi.matrix.SparseFloatVector;


/*a class to hold raw token/counts vector for a given sent
 * */

public class SentTokenVector implements Cloneable{
	
	private TokenDictionary tokenDict = null;
	private SparseFloatVector freqVector = null;
	private int id = -1;
	String sentText = null;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		SentTokenVector cloned = (SentTokenVector)super.clone();
		cloned.setTokenDict( (TokenDictionary)this.getTokenDict().clone() );
		
		float[] values = new float[this.getFreqVector().nonZeroDimensions().length];
		int[] keys = this.getFreqVector().nonZeroDimensions();
		for (int i = 0; i < keys.length; i++)
		{
			values[i] = (float) this.getFreqVector().value(keys[i]);
		}
		
		
		SparseFloatVector copyVector = new SparseFloatVector(keys, values, this.getFreqVector().numDimensions());
		cloned.setFreqVector(copyVector);
		return cloned;
	}
	
	
	public SentTokenVector(String sentText, TokenDictionary dict, int sentId, boolean keepText)
	{
		this.tokenDict = dict;
		//this.freqVector = SentTokenVector.ProcessSent(sentText, dict);
		this.id = sentId;
		
		
		TreeMap<Integer, Integer> featureValueMap = new TreeMap<Integer, Integer>();
		
		ArrayList<String> tokenList = dict.tokenize(sentText);
		for (String token: tokenList)
		{
			Integer tokenId = dict.GetTokenId(token);
			if (tokenId == null)
			{
				System.out.println("SentTokenVector.SparseFloatVector: Warning: unknown token " + token);
				System.out.println(sentText);
				continue;
			}
			Integer tokenFreq = featureValueMap.get(tokenId);
			
			//this token has already been encountered in this sent
			if (( tokenFreq ) != null ) 
			{
				tokenFreq = tokenFreq + 1;
				featureValueMap.put(tokenId, tokenFreq);
			}
			else
			{
				featureValueMap.put(tokenId, new Integer(1));
			}	
		}

		SparseFloatVector sentVector = new SparseFloatVector(featureValueMap, dict.GetDictDimensions()) ;
		this.freqVector = sentVector;
		
		if (keepText == true)
			this.sentText = sentText;
	}
	
	
	public static SparseFloatVector ProcessSent(String sent, TokenDictionary dict)
	{
		//ArrayList<Integer> nonEmptyKeys = new ArrayList<Integer>();
		//ArrayList<Integer> nonEmptyValues = new ArrayList<Integer>();
		
		TreeMap<Integer, Integer> featureValueMap = new TreeMap<Integer, Integer>();
		
		ArrayList<String> tokenList = dict.tokenize(sent);
		for (String token: tokenList)
		{
			Integer tokenId = dict.GetTokenId(token);
			if (tokenId == null)
			{
				System.out.println("SentTokenVector.SparseFloatVector: Warning: unknown token " + token);
				System.out.println(sent);
				continue;
			}
			Integer tokenFreq = featureValueMap.get(tokenId);
			
			//this token has already been encountered in this sent
			if (( tokenFreq ) != null ) 
			{
				tokenFreq = tokenFreq + 1;
				featureValueMap.put(tokenId, tokenFreq);
			}
			else
			{
				featureValueMap.put(tokenId, new Integer(1));
			}	
		}
//		int nonEmptyEntries = featureValueMap.size();
//		int[] keys = new int[nonEmptyEntries];
//		float[] values = new float[nonEmptyEntries];
//		
//		Iterator it = featureValueMap.keySet().iterator();
//		int i = 0;
//		while ( it.hasNext() )
//		{
//			Integer k = (Integer) it.next();
//			keys[i] = k.intValue();
//			values[i] =  ( (Integer) featureValueMap.get(k) ).floatValue();
//			i++;
//		}
		//System.out.println("ready to create sent vector");
		SparseFloatVector sentVector = new SparseFloatVector(featureValueMap, dict.GetDictDimensions()) ;
		return sentVector;
		
	}


	public TokenDictionary getTokenDict() {
		return tokenDict;
	}


	public void setTokenDict(TokenDictionary tokenDict) {
		this.tokenDict = tokenDict;
	}


	public SparseFloatVector getFreqVector() {
		return freqVector;
	}
	
	public void setFreqVector(SparseFloatVector newFreqs)
	{
		this.freqVector = newFreqs;
	}


	public int getId() {
		return id;
	}
	
	public void PrintVector()
	{
		
		StringBuilder sb = new StringBuilder( "Sent " + String.valueOf(this.id) );
		if (this.sentText != null)
			sb.append(": " + this.sentText);
		sb.append("\n");
		
		
		int[] keys = this.freqVector.nonZeroDimensions();
		for (int key: keys)
		{
			String token = this.tokenDict.GetTokenString(new Integer(key));
			sb.append("\t" + token + ": " + String.valueOf(this.freqVector.value(key)) + "\n");
		}
		
		System.out.println(sb.toString());
	}
	
	
	public void ApplyWeighting(int dimension, double weight)
	{
		
	}

}
