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

/**creates a dictionary mapping all token-types in a file
 * to their 0-based indices
 * 
 * It is further used to create vectors for sentences, also tf vectors and idf vectors
 * */


import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import datasources.IDataSource;
import datasources.IGenericDataSource;
import datasources.TextFileIO;


import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;


public class TokenDictionary implements Cloneable {
	
	File inputFile;
	String textString = null;
	TokenizerFactory tokenFactory;
	
	private TreeMap<String, Integer> tokenToIdMap = null;
	private TreeMap<Integer, String> idToTokenMap = null;
	private int tokenTypeCounter = 0;
	
	@Override
	/**
	 * This method implements only shallow cloning - deep cloning does not make sense here.
	 */
	public Object clone() throws CloneNotSupportedException {
		TokenDictionary cloned = (TokenDictionary) super.clone();
		return cloned;
	}
	
	public TokenDictionary(File input, TokenizerFactory tFactory)
	{
		this.inputFile = input;
		this.tokenFactory = tFactory;
		this.tokenToIdMap = new TreeMap<String, Integer>();
		this.idToTokenMap = new TreeMap<Integer, String>();
	}
	
	public TokenDictionary(IDataSource dataSource, TokenizerFactory tFactory)
	{
		this.inputFile = null;
		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < dataSource.GetNumChunks(); i++)
		{
			tmp.append( " " + dataSource.GetChunk(i) + " ");
		}
		
		this.textString = tmp.toString();
		this.tokenFactory = tFactory;
		this.tokenToIdMap = new TreeMap<String, Integer>();
		this.idToTokenMap = new TreeMap<Integer, String>();
	}
	
	public TokenDictionary(IGenericDataSource dataSource, TokenizerFactory tFactory)
	{
		this.inputFile = null;
		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < dataSource.GetNumberOfChunks(); i++)
		{
			try {
				tmp.append( " " + dataSource.GetChunk(i) + " ");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		this.textString = tmp.toString();
		this.tokenFactory = tFactory;
		this.tokenToIdMap = new TreeMap<String, Integer>();
		this.idToTokenMap = new TreeMap<Integer, String>();
	}
	
	public TokenDictionary(String text, TokenizerFactory tFactory)
	{
		this.inputFile = null;
		this.textString = text;
		this.tokenFactory = tFactory;
		this.tokenToIdMap = new TreeMap<String, Integer>();
		this.idToTokenMap = new TreeMap<Integer, String>();
	}
	
	public void ProcessText() throws Exception
	{
		String text;
		if (this.inputFile != null)
			text = TextFileIO.ReadTextFile(this.inputFile);
		else
			text = this.textString;
		
		try 
		{
			Tokenizer tokenizer = this.tokenFactory.tokenizer(text.toCharArray(),0,text.length());
			
			String curToken;
			while ( (curToken = tokenizer.nextToken() ) != null)
			{
				
				if (this.tokenToIdMap.keySet().contains(curToken) == false)
				{
					Integer newId = new Integer(this.getTokenTypeCounter());
					
					this.tokenToIdMap.put(curToken, newId);
					this.idToTokenMap.put(newId, curToken);
					
					this.incrementTokenTypeCounter(tokenTypeCounter);
				}
			}
		}//end try
		catch (Exception e)
		{
				System.out.println("Exception in TokenDictionary.ProcessText: " + e.getMessage());
		}
	}
	
	public void AddText(String newText)
	{
		try 
		{
			Tokenizer tokenizer = this.tokenFactory.tokenizer(newText.toCharArray(),0,newText.length());
			
			String curToken;
			while ( (curToken = tokenizer.nextToken() ) != null)
			{
				if (this.tokenToIdMap.keySet().contains(curToken) == false)
				{
					Integer newId = new Integer(this.getTokenTypeCounter());
					
					this.tokenToIdMap.put(curToken, newId);
					this.idToTokenMap.put(newId, curToken);
					
					this.incrementTokenTypeCounter(tokenTypeCounter);
				}
			}
		}//end try
		catch (Exception e)
		{
			System.out.println("Exception in TokenDictionary.ProcessText: " + e.getMessage());
		}
	}
	
	public ArrayList<String> tokenize(String text)
	{
		ArrayList<String> tokensList = new ArrayList<String>();
		Tokenizer tokenizer = this.tokenFactory.tokenizer(text.toCharArray(),0,text.length());
		
		try
		{
			String curToken;
			while ((curToken = tokenizer.nextToken()) != null )
			{
				tokensList.add(curToken);
			}
		}//end try
		catch (Exception e)
		{
			System.out.println("Exception in TokenDictionary.tokenize: " + e.getMessage());
		}

		return tokensList;
	}
	
	public Integer GetTokenId(String token)
	{
		return this.tokenToIdMap.get(token);
	}
	
	public String GetTokenString(Integer tokenId)
	{
		return this.idToTokenMap.get(tokenId);
	}

	public int getTokenTypeCounter() {
		return tokenTypeCounter;
	}

	private void incrementTokenTypeCounter(int tokenTypeCounter) {
		this.tokenTypeCounter++;
	}
	
	public int GetDictDimensions()
	{
		return this.tokenToIdMap.size();
	}
	
	public Set<Integer> GetAllTokenIds()
	{
		return this.idToTokenMap.keySet();
	}
	
	public Set<String> GetAllTokenStrings()
	{
		return this.tokenToIdMap.keySet();
	}
	
	public void PrintDictionary()
	{
		for (Integer id: this.idToTokenMap.keySet())
		{
			String curString = this.idToTokenMap.get(id);
			Integer id2 = this.tokenToIdMap.get(curString);
			System.out.println(id.toString() + "\t" + curString + "\t" + id2.toString() );
			if (id > 10)
				break;
		}
	}
}
