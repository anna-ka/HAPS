package similarity;

import java.util.ArrayList;
import java.util.List;

import com.aliasi.matrix.SparseFloatVector;

import datasources.NPExtractor;
import datasources.NounPhrase;
import datasources.ParsedParagraph;
import datasources.SentenceTree;

/**
 * This class holds counts of referential expressions of different types in a sentence.
 * The counts are stores in an array expressionCounts. They are indexed by a set of static constants.
 * 
 * @author anna
 *
 */
public class SentSyntacticVector implements Cloneable {
	
	public static final int PERSONAL_PRONOUN_ANIM = 0;
	public static final int DEMONSTR_PRONOUN_ANIM = 1;
	public static final int PROPER_NAME_ANIM = 2;
	public static final int DEF_NP_ANIM = 3;
	public static final int INDEF_NP_ANIM = 4;
	public static final int MOD_BY_PER_PRON_POSESS_ANIM = 5;
	public static final int MOD_BY_GEN_NP_ANIM = 6;
	
	public static final int PERSONAL_PRONOUN_INANIM = 7;
	public static final int DEMONSTR_PRONOUN_INANIM = 8;
	public static final int PROPER_NAME_INANIM = 9;
	public static final int DEF_NP_INANIM = 10;
	public static final int INDEF_NP_INANIM = 11;
	public static final int MOD_BY_PER_PRON_POSESS_INANIM = 12;
	public static final int MOD_BY_GEN_NP_INANIM = 13;
	
	private int openingCutOff = 2;
	private int decayWindow = 0;
	private Double decayFactor = new Double(0);
	int segmLevel = -1;
	
	
	public static final String[] exprNames = {"personal_pronouns_anim","demonstr_pronouns_anim", "proper_names_anim", "def_np_anim", "indef_np_anim", 
		"modified_by_pers_posess_anim", "modified_by_genitive_np_anim",
		 "personal_pronouns_inanim", "demonstr_pronouns_inanim", "proper_names_inanim", "def_np_inanim", "indef_np_inanim",
		 "modified_by_pers_posess_inanim", "modified_by_genitive_np_inanim"};
	
	protected ArrayList<Integer> expressionCounts = new ArrayList<Integer>(exprNames.length);
	//NPExtractor curExtractor = null;
	
	private ArrayList<SentenceTree> trees = null;
	
	public SentSyntacticVector(int parCutOff, int decayWin, Double decayValue, int segLevel)
	{
		int numEl = this.exprNames.length;
		for (int i = 0; i < numEl; i++)
		{
			this.expressionCounts.add(0);
		}
		
		this.openingCutOff = parCutOff;
		this.decayWindow = decayWin;
		this.decayFactor = decayValue;
		
		this.segmLevel = segLevel;
		
		this.trees = new ArrayList<SentenceTree>();
	}
	
	/**
	 * This is the method when each chunk corresponds to a sentence and therefore we only need one sentence tree
	 * @param tree
	 * @throws Exception
	 */
	public void InitSentLevel (SentenceTree tree) throws Exception
	{
		this.trees.add(tree);
		String sentText = tree.GetSentText();
//		System.out.println(sentText);
		
		NPExtractor curExtractor = new NPExtractor (tree);
		curExtractor.ExtractNPs();
		
		ArrayList<NounPhrase> nps = curExtractor.GetNPs();
		for (NounPhrase curNP: nps)
		{
			NounPhrase.EXP_TYPE expType= curNP.GetType();
			NounPhrase.DETERMINER curDet = curNP.GetDeterminer();
			NounPhrase.MOD_TYPE curMod = curNP.GetModType();
			String lemma = curNP.GetHead().GetLemma().trim();
			boolean isIt = false;
			if (lemma.compareToIgnoreCase("it") == 0)
				isIt = true;
			
			//is this an animate NP?
			if (curNP.GetIsAnimate() == true)
			{
				if ( expType == NounPhrase.EXP_TYPE.PERS_PRONOUN)
				{
					this.IncrementDimension(PERSONAL_PRONOUN_ANIM);
				}
				else if (expType == NounPhrase.EXP_TYPE.DEM_PRONOUN || curDet == NounPhrase.DETERMINER.DEMONSTR)
				{
					this.IncrementDimension(DEMONSTR_PRONOUN_ANIM);
				}
				else if (expType == NounPhrase.EXP_TYPE.PROPER)
				{
					this.IncrementDimension(PROPER_NAME_ANIM);
				}
				else if (curMod == NounPhrase.MOD_TYPE.PERS_PRON_GEN)
				{
					this.IncrementDimension(MOD_BY_PER_PRON_POSESS_ANIM);
				}
				else if (curMod == NounPhrase.MOD_TYPE.NP_GEN)
				{
					this.IncrementDimension(MOD_BY_GEN_NP_ANIM);
				}
				else if (curDet == NounPhrase.DETERMINER.DEF)
				{
					this.IncrementDimension(DEF_NP_ANIM);
				}
				else if (curDet == NounPhrase.DETERMINER.INDEF)
				{
					this.IncrementDimension(INDEF_NP_ANIM);
				}
				
			}
			else // it is an inanimate NP
			{
				//if ( expType == NounPhrase.EXP_TYPE.PERS_PRONOUN)
				if (isIt == true)
				{
					this.IncrementDimension(PERSONAL_PRONOUN_INANIM);
				}
				else if (expType == NounPhrase.EXP_TYPE.DEM_PRONOUN || curDet == NounPhrase.DETERMINER.DEMONSTR)
				{
					this.IncrementDimension(DEMONSTR_PRONOUN_INANIM);
				}
				else if (expType == NounPhrase.EXP_TYPE.PROPER)
				{
					this.IncrementDimension(PROPER_NAME_INANIM);
				}
				else if (curMod == NounPhrase.MOD_TYPE.PERS_PRON_GEN)
				{
					this.IncrementDimension(MOD_BY_PER_PRON_POSESS_INANIM);
				}
				else if (curMod == NounPhrase.MOD_TYPE.NP_GEN)
				{
					this.IncrementDimension(MOD_BY_GEN_NP_INANIM);
				}
				else if (curDet == NounPhrase.DETERMINER.DEF)
				{
					this.IncrementDimension(DEF_NP_INANIM);
				}
				else if (curDet == NounPhrase.DETERMINER.INDEF)
				{
					this.IncrementDimension(INDEF_NP_INANIM);
				}
				
			}
		}
		
//		System.out.println("**Vector for :" + this.expressionCounts);
//		this.PrintDetails();
		
	}
	
	/**
	 * This method counts referential expressions of different types in a paragraph.
	 * We are not interested in ref. expressions in all of the paragraph, but only in the opening part of it.
	 * 
	 * @param curPar
	 */
	public void InitParLevel(ParsedParagraph curPar) throws Exception
	{
		//int openingCutOff = 3;
		
		
		List<SentenceTree> sents = curPar.GetSentences();
		int numSent = sents.size();
		
		int sentCounter = 0;
		
		while (sentCounter < openingCutOff && sentCounter < numSent)
		{
			SentenceTree curTree = sents.get(sentCounter);
			this.trees.add(curTree);
			
			sentCounter++;
			NPExtractor ext = new NPExtractor(curTree);
			ext.ExtractNPs();
			
			System.out.println("Processing tree...");
			System.out.println("*****" + curTree.GetSentText());
			
			ArrayList<NounPhrase> nps = ext.GetNPs();
			for (NounPhrase curNP: nps)
			{
				NounPhrase.EXP_TYPE expType= curNP.GetType();
				NounPhrase.DETERMINER curDet = curNP.GetDeterminer();
				NounPhrase.MOD_TYPE curMod = curNP.GetModType();
				
				//is this an animate NP?
				if (curNP.GetIsAnimate() == true)
				{
					if ( expType == NounPhrase.EXP_TYPE.PERS_PRONOUN)
					{
						this.IncrementDimension(PERSONAL_PRONOUN_ANIM);
						System.out.println("\t found personal pronoun " + curNP.GetHead().GetText());
					}
					else if (expType == NounPhrase.EXP_TYPE.DEM_PRONOUN || curDet == NounPhrase.DETERMINER.DEMONSTR)
					{
						this.IncrementDimension(DEMONSTR_PRONOUN_ANIM);
					}
					else if (expType == NounPhrase.EXP_TYPE.PROPER)
					{
						this.IncrementDimension(PROPER_NAME_ANIM);
					}
					else if (curMod == NounPhrase.MOD_TYPE.PERS_PRON_GEN)
					{
						this.IncrementDimension(MOD_BY_PER_PRON_POSESS_ANIM);
					}
					else if (curMod == NounPhrase.MOD_TYPE.NP_GEN)
					{
						this.IncrementDimension(MOD_BY_GEN_NP_ANIM);
					}
					else if (curDet == NounPhrase.DETERMINER.DEF)
					{
						this.IncrementDimension(DEF_NP_ANIM);
					}
					else if (curDet == NounPhrase.DETERMINER.INDEF)
					{
						this.IncrementDimension(INDEF_NP_ANIM);
					}
				}
				else // it is an inanimate NP
				{
					if ( expType == NounPhrase.EXP_TYPE.PERS_PRONOUN)
					{
						this.IncrementDimension(PERSONAL_PRONOUN_INANIM);
					}
					else if (expType == NounPhrase.EXP_TYPE.DEM_PRONOUN || curDet == NounPhrase.DETERMINER.DEMONSTR)
					{
						this.IncrementDimension(DEMONSTR_PRONOUN_INANIM);
					}
					else if (expType == NounPhrase.EXP_TYPE.PROPER)
					{
						this.IncrementDimension(PROPER_NAME_INANIM);
					}
					else if (curMod == NounPhrase.MOD_TYPE.PERS_PRON_GEN)
					{
						this.IncrementDimension(MOD_BY_PER_PRON_POSESS_INANIM);
					}
					else if (curMod == NounPhrase.MOD_TYPE.NP_GEN)
					{
						this.IncrementDimension(MOD_BY_GEN_NP_INANIM);
					}
					else if (curDet == NounPhrase.DETERMINER.DEF)
					{
						this.IncrementDimension(DEF_NP_INANIM);
					}
					else if (curDet == NounPhrase.DETERMINER.INDEF)
					{
						this.IncrementDimension(INDEF_NP_INANIM);
					}
				}
			}
			
		}
		
//		System.out.println("**Vector for :" + this.expressionCounts);
//		System.out.println(curPar.GetParagraphText());
//		this.PrintDetails();
		
	}
	
	public int GetDimension(int dimension) throws Exception
	{
		if (dimension < 0 || dimension > (this.exprNames.length - 1))
		{
			String msg = "Invalid dimension in SentSyntactycVector.GetDimension: " + dimension;
			throw (new Exception (msg));
		}
		
		return this.expressionCounts.get(dimension);
	}
	
	public void SetDimension (int dimension, int value) throws Exception
	{
		if (dimension < 0 || dimension > (this.exprNames.length - 1))
		{
			String msg = "Invalid dimension in SentSyntactycVector.SetDimension: " + dimension;
			throw (new Exception (msg));
		}
		this.expressionCounts.set(dimension, value);
	}
	
	public void IncrementDimension(int dimension) throws Exception
	{
		if (dimension < 0 || dimension > (this.exprNames.length - 1))
		{
			String msg = "Invalid dimension in SentSyntactycVector.IncrementDimension: " + dimension;
			throw (new Exception (msg));
		}
		int oldCount = this.GetDimension(dimension);
		this.SetDimension(dimension, ++oldCount);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {

		SentSyntacticVector cloned = new SentSyntacticVector(this.openingCutOff, this.decayWindow, this.decayFactor, this.segmLevel);
		
		try{
			for (int i = 0; i < this.expressionCounts.size(); i++)
			{
				int curValue = this.GetDimension(i);
				cloned.SetDimension(i, curValue);
			}	
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw (new CloneNotSupportedException(e.getMessage()));
		}
		
		
		
		return cloned;
	}
	
	public ArrayList<Integer> GetCounts()
	{
		return this.expressionCounts;
	}
	
	public int GetSegmLevel() {
		return segmLevel;
	}

	public void SetSegmLevel(int segmLevel) {
		this.segmLevel = segmLevel;
	}

	
	public void PrintDetails()
	{
		System.out.println("Expressions vector");
		for (int i = 0; i < this.exprNames.length; i++)
		{
			String name = this.exprNames[i];
			int count  = -1;
			try {
					count = this.GetDimension(i);
				
				
			} catch (Exception e) {
				System.out.println("Exception in SentSyntacticVector.PrintDetails: " + e.getMessage());
				e.printStackTrace();
			}
			System.out.println("\t" + name + ": " + count);
		}
		
		for (SentenceTree tree: this.trees)
		{
			System.out.println(tree.GetSentText());
			
			NPExtractor curExtractor = new NPExtractor (tree);
			curExtractor.ExtractNPs();
			
			curExtractor.PrintNPs();
		}
	}

}
