/**
 * 
 */
package datasources;

import java.util.regex.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;


/**
 *a class that represents any syntactic unit that functions as a noun phrase, as based on
 *Connexor 3.9 output.
 *
 *An NP consists of a head and all its pre- and postmodifiers (including determiners). 
 *Basically, an NP is a nominal head along with its complete subtree.
 *
 */
public class NounPhrase {
	
	public static enum DETERMINER {INDEF, DEF, DEMONSTR,  NONE, OTHER};
	public static enum EXP_TYPE {COMMON, PROPER, PERS_PRONOUN, DEM_PRONOUN, OTHER};
	//public static enum ANIMACY {ANIMATE, INANIMATE};
	public static enum MOD_TYPE {NONE, PERS_PRON_GEN, NP_GEN, PREMOD, POSTMOD, PREPOST};
	
	//the head of this NP
	ParseNode nomHead = null;
	
	//features
	DETERMINER determiner = null;
	EXP_TYPE exp_type = null;
	boolean is_animate = false;
	MOD_TYPE mod_type = null;
	//is this NP a subject complement of a cleft construction?
	boolean is_cleft = false;
	
	//the number of tokens in the NP (the head + all of the sub-tree)
	int lengthInTokens = 0; 
	
	//simple pattern to capture proper names
	Pattern properPattern = Pattern.compile("[A-Z].*");
	
	ArrayList<ParseNode> premods = new ArrayList<ParseNode>();
	ArrayList<ParseNode> postmods = new ArrayList<ParseNode>();
	
	ArrayList<ParseNode> dependants = new ArrayList<ParseNode>();
	
	
	public NounPhrase( ParseNode head) throws NPException
	{
		if (head != null  )
		{
			if (head.syntSurface.contains( "NH") )
				{
					this.nomHead = head;
				}
			else
			{
				throw (new NPException ("cannot create an NP from a node that does not have %NH surface tag "));
			}
		}
		else
		{
			throw (new NPException ("cannot create an NP from a null node "));
		}
	
	}
	
	public void PrintNP()
	{
		this.PrintNP("");
	}
	
	public void PrintNP(String offset)
	{
		System.out.println(offset + "NP head:"  +  this.nomHead.GetNodeShortInfo(offset));
		
		offset = offset + "\t";
		String det = "";
		if (this.determiner == DETERMINER.DEF)
			det = "definite";
		else if (this.determiner == DETERMINER.INDEF)
			det = "indefinite";
		else if (this.determiner == DETERMINER.DEMONSTR)
			det = "demonstrative";
		else if (this.determiner == DETERMINER.NONE)
			det = "none";
		else if (this.determiner == DETERMINER.OTHER)
			det = "other";
		else
			det = "WARNING: unknown determiner";
		
		
		
		//exp type
		String exp = "";
		switch (this.exp_type)
		{
		case COMMON:
			exp = "common";
			break;
		case PROPER:
			exp = "proper";
			break;
		//PERS_PRONOUN, DEM_PRONOUN, OTHER
		case  PERS_PRONOUN:
			exp = "personal pronoun";
			break;
		case DEM_PRONOUN:
			exp = "demonstrative pronoun";
			break;
		case OTHER:
			exp = "other";
			break;
		default:
			exp = "BAD EXP TYPE";
			break;
		}
		
		
		String mod = "";
		switch (this.mod_type)
		{
		case PERS_PRON_GEN:
			mod = "modified by a personal posessive pronoun";
			break;
		case NP_GEN:
			mod = "modified by a genitive noun";
			break;
		case PREMOD:
			mod = "premodified";
			break;
		case POSTMOD:
			mod = "posmodified";
			break;
		case PREPOST:
			mod = "pre and post modified";
			break;
		case NONE:
			mod = "none";
			break;
		default:
			mod = "bad modofier type";
			
		}
		
		String cleft = (this.is_cleft == true) ? "CLEFT CONSTR" : "NOT CLEFT";
		String anim = (this.is_animate ) ? "ANIMATE" : "INANIMATE";
		
		
		System.out.println(offset + "Determiner: " + det);
		System.out.println(offset + "Exp type: " + exp);
		System.out.println(offset + "Modifier: " + mod);
		System.out.println(offset + "Cleft: " + cleft);
		System.out.println(offset + "animacy: " + anim);
		
		//length
		System.out.println(offset + "length: " + String.valueOf(this.GetLength()));
		
	}
	
	public void Init()
	{
		//collect all children and other descendats, including self
		this.ComputeDependents();
		
		this.SetDeterminer(); // definite, indefinite, demonstrative, no determiner, other
		this.SetExpType();
		this.SetModType();	
		
		//compute the size of the subtree; it may be modified by IsCleft();
		this.ComputeLength();
		
		//check whether this is a cleft construction
		this.SetIsCleft();
		//check if this NP is animate
		this.SetIsAnimate();
	}
	
	/**
	 * this method checks the ANIMACY property of the head of this NP 
	 * and sets the flag accordningly. The value initially should have been set
	 * by running Stanford Core NLP on the whole sentence
	 */
	public void SetIsAnimate() {
		
		ParseNode head = this.GetHead();
		
		ParseNode.NE neTag = head.GetNE();
		if (neTag != null &&  (neTag != ParseNode.NE.NONE) && (neTag != ParseNode.NE.PERSON) )
			this.is_animate = false;
		else if (neTag != null && (neTag == ParseNode.NE.PERSON) )
			this.is_animate = true;
		else if (head.animacyTag.equals( ParseNode.ANIMACY.ANIMATE ))
			this.is_animate = true;	
	}
	
	public boolean GetIsAnimate()
	{
		return this.is_animate;
	}

	/**
	 * a method that determines if this NP is part of a cleft construction, e.g.
	 * "It was Anna's smile that did the trick."
	 */
	public void SetIsCleft()
	{
		boolean isSubjComplement = SubtreeUtils.MatchSyntacticFunction(this.nomHead, "PCOMPL-S");
		//only subject complements participate in cleft according to Machinese
		if ( !isSubjComplement )
			return;
		//System.out.println("this is a PCPMPL-S node");
		
		//is it the parent verb "be"?
		ParseNode parent = this.nomHead.GetParent();
		
		if (parent == null )
			return;
		
		else if (parent.GetLemma().compareTo("be") != 0 )
			return;
		//System.out.println("found a parent BE");
		//was the formal subject "it"?
		ParseNode subjNode = SubtreeUtils.FindChildBySyntax(parent, "SUBJ");
		
		if (subjNode == null)
		{
			//System.out.println("subj node is null");
			return;
		}
		else if ( subjNode.GetLemma().compareTo("it") != 0 )
		{
			//System.out.println("Subj node is not IT: " + subjNode.GetNodeShortInfo() );
			return;
		}
		//if we got here, this NP is a subject complement of it + be + X construction
		//we need to check that it is modified by a subordinate clause
		//in Machinese this is realized as a postmodifier of 'it"
		
		//System.out.println("found a sibling subject IT");
		
		//avoid situations when the NP is not modified by a subordinate clause, 
		//e.g., It was Anna. 
		ParseNode subordVerb = SubtreeUtils.FindChildBySurface(subjNode, "VA"); // main verb in an active chain
		if (subordVerb == null)
			subordVerb = SubtreeUtils.FindChildBySurface(subjNode, "VP"); // main verb in a passive chain
		
		if (subordVerb != null)
		{
			/*
			//include the subordinate clause when counting the length of the NP
			//the parse attaches the subordinate clause to "it"  but I want to count it
			//as a part of the NP that it semantically describes:
			//'It was his wife that undid him. '> here 'that undid him' will be counted towards 'wife'
			//TO DO: for now I discard 'it's but if I ever include them, I will have to at the least avoid double-counting the 
			//subordinate clause
			int subordSize = 0;
			try
			{
				subordSize = subordVerb.ComputeSubtreeSize();
				this.ModifyLength(subordSize);
				//System.out.println("size of " + subordVerb.GetText() + " is " + String.valueOf(subordSize));
			}
			catch (Exception e)
			{
				System.out.println("Error in SetIsCleft" + e.getMessage());
			}
			*/
			this.is_cleft = true;
		}
		
	}
	
	public boolean GetIsCleft()
	{
		return this.is_cleft;
	}
	
	
	/**
	 * a method for finding if this NP is pre-modified and/or post-modified
	 */
	public void SetModType()
	{
		//first, find pre-modifiers
		
		//we are especially interested in NPs modified by posessive pronouns
		// and NPs in genitive case
		String genitiveFunction = "GEN";
		
		
		//first look at % tags, then if nothing is found there, look at @ tags
		ParseNode premod = SubtreeUtils.FindChildBySurface(this.nomHead, ">N"); //all premod or determiners
		if (premod != null)
		{
			//check that this is not  a determiner node
			if (premod.morpho.contains("DET"))
				premod = null;
		}
		
		
		if (premod == null )
		{
			//try to look by syntactic function tag
			premod = SubtreeUtils.FindChildBySyntax(this.nomHead, "A>"); //premodifiers of a nominal
		}
		if (premod == null )
		{
			//try to look by syntactic function tag
			premod = SubtreeUtils.FindChildBySyntax(this.nomHead, "QN>"); //premodifying quantifiers
		}
		
		//did we find any pre-modifiers?
		if (premod == null)
		{
			this.mod_type = MOD_TYPE.NONE;
		}
		else
		{
			//check if the premodifer is a posessive pronoun or a genitive-cased NP
			
			ParseNode genNode = null;
			if (premod.morpho.contains(genitiveFunction))
				genNode = premod;
			else
				genNode = SubtreeUtils.FindChildByMorphoTag(premod, genitiveFunction);
			
			if (genNode != null)
			{
				String genText = genNode.GetText();
				if (genNode.morpho.contains("PRON") )
				{
					if ( genNode.morpho.contains("SG3") || genNode.morpho.contains("PL3")  )
					{
						this.mod_type = MOD_TYPE.PERS_PRON_GEN;
						System.out.println("found a PERS PRON posessive modifier: " + genText);
					}
					else
						this.mod_type = MOD_TYPE.PREMOD;
				}
				else
				{
					this.mod_type = MOD_TYPE.NP_GEN;
					System.out.println("found a GENITIVE case NP modifier: " + genText);
				}
				
			}
			else
			{
				this.mod_type = MOD_TYPE.PREMOD;
			}
		}
	
		
		//now look for post-modifiers
		
		//first look in surface tags
		ParseNode postmod = SubtreeUtils.FindChildBySurface(this.nomHead, "N<");
		if (postmod == null)
			postmod = SubtreeUtils.FindChildByDependency(this.nomHead, "mod"); //all "other" postmodifiers 
		if (postmod == null)
			postmod = SubtreeUtils.FindChildBySyntax(this.nomHead, "<NOM");
		if (postmod == null)
			postmod = SubtreeUtils.FindChildBySyntax(this.nomHead, "<NOM-OF");
		//did we find any postmodifiers?
		if (postmod != null)
		{
			if (this.mod_type == MOD_TYPE.PREMOD)
				this.mod_type = MOD_TYPE.PREPOST; //if we already found some premodifiers
			else
				this.mod_type = MOD_TYPE.POSTMOD;
		}
		
		
	}
	
	/**
	 *  a method that computes the info about the type of referring expession
	 */
	public void SetExpType()
	{
	
		//is this a proper noun?
		if (this.IsProperNoun() == true  && this.nomHead.morpho.contains("PRON") == false)
			this.exp_type = EXP_TYPE.PROPER;
		//is this a pronoun?
		else if (this.nomHead.morpho.contains("PRON"))
		{
			//we are only interested in collecting 3rd person personal pronouns (he, she, they)
			// and demonstrative pronouns (this, those)
			if ( this.nomHead.morpho.contains("PERS") && (
					this.nomHead.morpho.contains("SG3") || this.nomHead.morpho.contains("PL3") ) )
				//this is a 3rd person personal pronouns
			{
				this.exp_type = EXP_TYPE.PERS_PRONOUN;
				this.is_animate = true;	 //she,he,they are all animate
				
			}
			else if (this.nomHead.morpho.contains("DEM"))
				//this is a demonstrative pronoun
			{
				this.exp_type = EXP_TYPE.DEM_PRONOUN;
			}
			else
				//this is a pronoun of a type we do not want to deal with, e.g. all, many
			{
				this.exp_type = EXP_TYPE.OTHER;
			}	
		}
		//otherwise it is a common noun
		else
		{
			this.exp_type = EXP_TYPE.COMMON;
		}
	}
	
	
	//TO DO: eventually replace it by running some NP recognizer
	boolean IsProperNoun()
	{
		ParseNode.NE ne = this.nomHead.GetNE(); // the tag given by Stanford NE recognizer
		switch (ne)
		{
		case PERSON:
			this.SetAnymacy(true);
			return true;
		case LOCATION:
			return true;
		case ORGANIZATION:
			return true;
		default:
			return false;
		}
	}
	
	
	/**
	 * a method that computes information about the determiner of this NP
	 */
	public void SetDeterminer()
	{
		//find determiner type
		String detFunction = "DN>";
		ParseNode detNode = SubtreeUtils.FindChildBySyntax(this.nomHead, detFunction);

		
		
		if (detNode == null )
		{
			this.determiner = DETERMINER.NONE;
			return;
		}
		
		//see if this is the definite article "the"
		String detText = detNode.GetText();
		if (detText.compareToIgnoreCase("THE") == 0)
		{
			this.determiner = DETERMINER.DEF;
		}
		else if (detText.compareToIgnoreCase("a") == 0 ||
				detText.compareToIgnoreCase("an") == 0 ||
				detText.compareToIgnoreCase("any") == 0)
		{
			this.determiner = DETERMINER.INDEF;
		}
		else if (detNode.morpho.contains("DEM"))
		{
			this.determiner = DETERMINER.DEMONSTR;
		}
		else 
		{
			this.determiner = DETERMINER.OTHER;
		}
		
		//increase size
		if (this.determiner != DETERMINER.NONE)
			this.ModifyLength( 1 );
		
	}
	
	public ArrayList<ParseNode> GetDependents()
	{
		return this.dependants;
	}
	
	/**
	 *  a method that collects all children in the subtree + the nominal head itself
	 */
	public void ComputeDependents()
	{
		ParseNode root = this.nomHead;
		
		if (this.dependants.isEmpty() == false)
			this.dependants = new ArrayList<ParseNode>();
		
		this.dependants.add(root);
		for (Integer curId: root.children.keySet() )
		{
			ParseNode curChild = root.children.get(curId);
			this.CollectDescendants(this.dependants, curChild);
		}
		
//		System.out.print("dependents of " + this.nomHead.GetText() + ": ");
//		//test print
//		for (ParseNode child: this.dependants)
//		{
//			System.out.print(child.GetText() + " ");
//		}
//		System.out.println();
	}
	
	private void CollectDescendants(ArrayList<ParseNode> kids, ParseNode root)
	{
		kids.add(root);
		for (Integer curId: root.children.keySet())
		{
			ParseNode curChild = root.children.get(curId);
			this.CollectDescendants(kids, curChild);
		}
	}
	
	/*a method to compute simple cosine similarity between two NPs
	 * for now it does not filter stop-words or stop-pos (e.g., prepositions, interjection)
	 * those should be added later
	 * */
	public double CosineSim(NounPhrase np2)
	{
		//get all nodes from the subtree of the NP we are comparing to
		ArrayList<ParseNode> desc2 = np2.GetDependents();
		
		int overlap = 0;
		int size1 = 0; //the number of non-punctuation nodes in this subtree
		
		//collect all lemmas
		HashSet<String> lemmas2 = new HashSet<String>();
		for (ParseNode node2: desc2)
		{
			if (node2.isPunct == false)
				lemmas2.add(node2.GetLemma());
		}
		
		
		for (ParseNode node1: this.dependants)
		{
			if (node1.isPunct == false)
				continue;
			size1++;
			String lemma1 = node1.GetLemma();
			if (lemmas2.contains(lemma1))
			{
				overlap++;
			}
		}
		
		double denominator = Math.sqrt( (double)( size1 * lemmas2.size() ) );
		double numerator = (double) overlap;
		
		double result = numerator / denominator;
		return result;
		
	}
	
	public ParseNode GetHead()
	{
		return this.nomHead;
	}
	
	public DETERMINER GetDeterminer()
	{
		return this.determiner;
	}
	
	public EXP_TYPE GetType()
	{
		return this.exp_type;
	}
	
//	public boolean GetAnimacy()
//	{
//		return this.is_animate;
//	}
	
	public void SetAnymacy(boolean newAnim)
	{
		this.is_animate = newAnim;
	}
	
	public MOD_TYPE GetModType()
	{
		return this.mod_type;
	}
	
	public int GetLength()
	{
		return this.lengthInTokens;
	}
	
	public void ComputeLength()
	{
		
		this.lengthInTokens = this.dependants.size();
	}
	
	public void ModifyLength(int increment)
	{
		this.lengthInTokens += increment;
	}

}

