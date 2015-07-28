package datasources;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Set;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Comparator;
import java.util.HashSet;

//import edu.stanford.nlp.ie.AbstractSequenceClassifier;
//import edu.stanford.nlp.ling.CoreLabel;
//import edu.stanford.nlp.ling.Sentence;
//import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.Annotation;


/*
 * a class to hold a parsed sentence tree from Connexor Machinese Syntax 3.9
 * */
public class SentenceTree {
	
	
	public final static int INVALID_ID = -1;
	
	//STOP_POS serve to filter useless words when computing semantic similarity
	public static HashSet<String> STOP_POS = new HashSet<String>();
	{
		STOP_POS.add("PRON"); //pronouns
		STOP_POS.add("DET"); //determiner
		STOP_POS.add("INTERJ"); // interjection
		STOP_POS.add("CC"); // coordinating conjunction
		STOP_POS.add("CS"); //subord conjunction
		STOP_POS.add("PREP"); //preposition
		STOP_POS.add("NEG-PART"); // negative particle
		STOP_POS.add("INFMARK>"); // infinitival marker
		//STOP_POS.add("<?>"); //unknown word
	}
	
	
	int id;
	
	//this is to number sentences by something other than their id
	private int pseudoId = -1;
	
	TreeMap<Integer, ParseNode> nodes = null;
	LinkedList<ParseNode> roots = null;
	String text = "";
	
	public SentenceTree(int newId)
	{
		this.id = newId;
		this.nodes = new TreeMap<Integer, ParseNode>();
		this.roots = new LinkedList<ParseNode>();
	}
	
	/*a method to add new parse nodes to the underlying node list, linearly.
	 * Once all nodes are in, call BuildTree to create a tree
	 * */
	public void AddNode(ParseNode node) throws Exception 
	{
		if (node == null )
		{
			throw (new Exception ("SentenceTree.AddNode: cannot add null node."));
		}
		else
		{
			this.nodes.put(new Integer (node.id), node);
		}
	}
	
	public void BuildTree() throws Exception
	{
		if (this.nodes.isEmpty())
			throw (new Exception ("SentenceNode.BuildTree: can't build a tree when no nodes are present"));
		
		Set<Integer> keys = this.nodes.keySet();
		Iterator<Integer> it = keys.iterator();
		while(it.hasNext())
		{
			Integer curKey = it.next();
			ParseNode curNode = this.nodes.get(curKey);
			if (curNode.isPunct)
				continue;
			
			//attach the node to its parent
			Integer parentId = new Integer(curNode.parentId);
			//Machinese Syntax assigns sent id to the root element
			if (parentId.compareTo( new Integer (this.id) ) == 0)
				{
					this.roots.add(curNode);
					continue;
				}
			//if the parse tree consists of partial parses, collect all roots
			else if ( curNode.parentId == curNode.INVALID  )
			{
				if ( curNode.GetDependency() == null )
				{
					this.roots.add(curNode);
				}
				else
				{
					System.out.println("WARNING in SentenceTree.BuildTree: node without parent but with dependency " + curNode.GetNodeInfo());
				}
			}
			else //this is a non-root non-punct node, attach it to its parent
			{
				ParseNode parent = this.nodes.get( parentId);
				
				//add the parent link
				curNode.SetParent(parent);
				
				//add the node as the parent's child
				parent.AddChild(new Integer(curNode.GetId()), curNode);
			}
			
		}
		this.text = this.GetSentText();
	}
	
	public String GetSentText()
	{
		if (this.nodes == null || this.nodes.isEmpty())
		{
			return ( "no text in sent " + String.valueOf(this.id));
		}
		
		Set<Integer> keys = this.nodes.keySet();
		Iterator<Integer> kIt = keys.iterator(); 
		
		StringBuilder prose = new StringBuilder();
		//prose.append ("sent id=" + String.valueOf(this.id) + ": ");
		
		while (kIt.hasNext())
		{
			Integer curKey = kIt.next();
			ParseNode n = this.nodes.get(curKey);
			if (n.isPunct == false)
				prose.append(" "); // add whitespace before non-punctuation nodes
			prose.append(n.text + " "); //+ " (" + curKey.toString() +") ");	 
		}
		
		String str = prose.toString().replace("\n", "");
		return (str);
			
	}
	
	/**
	 * returns lemmatized text of this sentence
	 * @return
	 */
	public String GetLemmatizedText()
	{
		if (this.nodes == null || this.nodes.isEmpty())
		{
			return ( "no text in sent " + String.valueOf(this.id));
		}
		
		Set<Integer> keys = this.nodes.keySet();
		Iterator<Integer> kIt = keys.iterator(); 
		
		StringBuilder lemText = new StringBuilder();
		//prose.append ("sent id=" + String.valueOf(this.id) + ": ");
		
		while (kIt.hasNext())
		{
			Integer curKey = kIt.next();
			ParseNode n = this.nodes.get(curKey);
			if (n.isPunct == false && n.lemma != null)
			{
				lemText.append(" "); // add whitespace before non-punctuation nodes
				lemText.append(n.lemma + " "); //+ " (" + curKey.toString() +") ");
			}
					 
		}
		
		String str = lemText.toString().replace("\n", "");
		return (str);
	}
	
	public int GetId()
	{return this.id;}
	
	public void SetId(int newId)
	{
		this.id = newId;
	}
	
	
	public void PrintTree(String offset)
	{
		Iterator <ParseNode> rIt = this.roots.listIterator();
		while (rIt.hasNext())
		{
			ParseNode curRoot = rIt.next();
			System.out.println("root: ");
			this.PrintSubtree (offset, curRoot );
		}
		
	}
	
	public void PrintSubtree (String offset, ParseNode curRoot)
	{
		
		
		//output the current node
		System.out.println ( curRoot.GetNodeInfo(offset) );
		
		TreeMap<Integer, ParseNode> kids = curRoot.GetChildren();
		Set <Integer> keys  = kids.keySet();
		Iterator<Integer> it = keys.iterator();
		//recurse to children
		while (it.hasNext())
		{
			System.out.println(offset.toString() + "child");
			ParseNode node = kids.get(it.next());
			
			this.PrintSubtree(offset + "\t", node);
		}
	}
	
	/** a method that takes an instance of AbstractSequenceClassifier from Stanford NER and tags all tokens in the sentence
	// for <PERSON>, <LOCATION>, <ORGANIZATION>
	public void FindNamedEntitiesOld(AbstractSequenceClassifier classifier)  throws Exception
	{
		Set<Integer> ids = this.nodes.keySet();
		
		//we need to represent all tokens as a List<String>
		ArrayList<String> sentWords = new ArrayList<String>(ids.size());
		for (Integer curId: ids)
		{
			ParseNode curNode = this.nodes.get(curId);
			sentWords.add(curNode.GetText());
		}
		
		//now classify
//		Sentence sent = Sentence.toSentence(sentWords);
//		List<CoreLabel> taggedSeq = classifier.classifySentence(sent);
		
		List<CoreLabel> taggedSeq = new ArrayList<CoreLabel>();
		
		//now loop over all tokens in sent, find the corresponding ParseNode in this.nodes
		//and set neTag
		
		//these two iterators should work in parallel since the keys of a TreeMap are returned in ascending order
		Iterator <CoreLabel> it = taggedSeq.iterator();
		Iterator <Integer> nodesIt = ids.iterator();
		while(it.hasNext())
		{
			CoreLabel curWord = it.next();
			String curText = curWord.word();
			String curLabel = curWord.get(AnswerAnnotation.class);
			
			Integer curId = nodesIt.next();
			ParseNode curNode = this.nodes.get(curId);
			if (curNode.GetText().compareTo(curText) != 0)
			{
				throw new Exception ("Error in FindNamedEntities. Mismatched text. curText: <" + curText + "> but node.GetText() is <" + 
						curNode.GetText() + ">");
			}
			
			ParseNode.NE newTag = ParseNode.NE.NONE;
			
			if (curLabel.compareTo("PERSON") == 0)
				newTag = ParseNode.NE.PERSON;
			else if (curLabel.compareTo("ORGANIZATION") == 0) 
				newTag = ParseNode.NE.ORGANIZATION;
			else if (curLabel.compareTo("LOCATION") == 0) 
				newTag = ParseNode.NE.LOCATION;
			
			curNode.SetNE(newTag);
			
			System.out.print(curNode.GetText() + "/" + curLabel + " ");

		}
		System.out.println();
	}
	
	**/
	
	 
	
	public void FindNamedEntities(StanfordCoreNLP pipeline)  throws Exception
	{
		Set<Integer> ids = this.nodes.keySet();
		
		//we need to represent all tokens as a List<String>
		ArrayList<String> sentWords = new ArrayList<String>(ids.size());
		StringBuilder t = new StringBuilder();
		
		for (Integer curId: ids)
		{
			ParseNode curNode = this.nodes.get(curId);
			sentWords.add(curNode.GetText());
			if (curNode.isPunct == false)
				t.append(" ");
			t.append(curNode.GetText());
		}
		
		//String sentText = sentWords.toString() + ".";
		String sentText = t.toString() ;
		//System.out.println("NEW TEXT " + sentText);
		
		// create an empty Annotation just with the given text
	    Annotation sentAnnot = new Annotation(sentText);
	    
	    // run all Annotators on this text
	    pipeline.annotate(sentAnnot);
	    List<CoreLabel> tokenAnnots =  sentAnnot.get(TokensAnnotation.class);
	    
//	    int i = 0;
//	    for (CoreLabel token: tokenAnnots) {
//	    	i++;
//	    	
//	        // this is the text of the token
//	        String word = token.get(TextAnnotation.class);
//	        // this is the POS tag of the token
//	        String pos = token.get(PartOfSpeechAnnotation.class);
//	        // this is the NER label of the token
//	        String ne = token.get(NamedEntityTagAnnotation.class); 
//	        int startPosition = token.beginPosition();
//	        
//	        System.out.println("Token " + i + ": "  + word + " at position "+ startPosition + ", pos: " + pos + ", ne" + ne);
//	        
//	      }
	    
	    Map<Integer, CorefChain> graph = 
	    	      sentAnnot.get(CorefChainAnnotation.class);
	    List<CorefChain.CorefMention> mentions  = null;
	    for (Entry<Integer, CorefChain> entry : graph.entrySet())
	    {
	    	CorefChain value = entry.getValue();
	    	
	    	mentions =  value.getMentionsInTextualOrder();
	    	
	    	for (CorefChain.CorefMention curMention: mentions)
	    	{
	    		Dictionaries.Animacy animacy = curMention.animacy;
	    		
	    		int startOfMention = curMention.startIndex;
//	    		System.out.println("Mention " + curMention.mentionSpan + " at position "+ startOfMention + " /" + animacy);
//	    		System.out.println(animacy);
	    	}
	    	;
	    	//break;
	    }
	    
	    
	    
	  //now loop over all tokens in sent, find the corresponding ParseNode in this.nodes
	  //and set neTag 
	    
	  //these two iterators should work in parallel since the keys of a TreeMap are returned in ascending order
		Iterator <CoreLabel> it = tokenAnnots.iterator();
		Iterator <Integer> nodesIt = ids.iterator();
		int counter = 0;
		while(it.hasNext() && nodesIt.hasNext())
		{
			counter++;
			Integer curId = nodesIt.next();
			
			CoreLabel curWord = it.next();
			ParseNode curNode = this.nodes.get(curId);
			
			String curText = curWord.word();
			String curNELabel = curWord.get(NamedEntityTagAnnotation.class); 
			
			CorefChain.CorefMention candMention = this.GetCorrespondingMention(counter, graph);
			
			String animTag = "INANIMATE";
			if (candMention == null)
			{
				//System.out.println("\n**	could not find mention for " + curNode.GetText());
				//default annotation is inaminate
			}
			else
			{
				Dictionaries.Animacy animacy = candMention.animacy;
				
				if (animacy == Dictionaries.Animacy.ANIMATE && (curNode.syntSurface.contains("NH")))
				{
					curNode.SetAnimacyTag(ParseNode.ANIMACY.ANIMATE);
					animTag = "ANIMATE";
				}
			}
			
			String textInNode = curNode.GetText();
			
			//if (curNode.GetText().compareTo(curText) != 0)
			
			//since we are iterating over ParseNodes and also over the output of Stanford CoreNLP 
			//it is possible that things can become misaligned. There are some frequence cases
			//1. empty space
			//2. Connexor treats Genitive case nouns as one node, while StanfordCore NLP splits them into two tokens:
			// e.g. Anna's = Anna + 's
			//I try to catch these common cases
			
			if (textInNode.compareTo(curText) != 0)
			{
				if (curText.compareTo("''") == 0)
					continue;
				else if (curText.compareTo("'s") == 0)
				{
					it.next();
					continue;
				}
				else if (textInNode.contains("'s")) //e.g. Anna's
				{
					String trunkText = textInNode.replace("\'s", "");
					if (trunkText.compareTo(curText) != 0)
					{
//						System.out.println("Warning in FindNamedEntities in sent" +
//								" " + this.GetSentText());
//						System.out.println("\tMismatched text. curText: <" + curText + "> but TRUNK node.GetText() is <" + 
//								trunkText + ">");
						continue;
					}
					else
					{
						it.next(); // move the iterator of Stanford CoreNLP by one
					}
				}
				else
				{
//					throw new Exception ("Error in FindNamedEntities. Mismatched text. curText: <" + curText + "> but node.GetText() is <" + 
//							curNode.GetText() + ">");
					
					System.out.println("Warning in FindNamedEntities in sent" +
							" " + this.GetSentText());
					System.out.println("\tMismatched text. curText: <" + curText + "> but node.GetText() is <" + 
							curNode.GetText() + ">");
					continue;	
				}
			}
			
			
			ParseNode.NE newTag = ParseNode.NE.NONE;
			
			if (curNELabel.compareTo("PERSON") == 0)
				newTag = ParseNode.NE.PERSON;
			else if (curNELabel.compareTo("ORGANIZATION") == 0) 
				newTag = ParseNode.NE.ORGANIZATION;
			else if (curNELabel.compareTo("LOCATION") == 0) 
				newTag = ParseNode.NE.LOCATION;
			
			curNode.SetNE(newTag);
			
			//System.out.print(curNode.GetText() + "/" + curNELabel + " " + "/" + animTag);

		}    
}
	/**
	 * This method takes a list of CorefChain.CorefMentions found in this sentence by Stanford CoreNLP coref module
	 * and return parent mention for a given token. Following the convention of Stanford CoreNLP, token indices are 1-based here
	 * 
	 * @param tokenIndex1-based index of the token in this sentence
	 * @param graph coreference graph provided by Stanford CoreNLP
	 * @return
	 */
	private CorefChain.CorefMention GetCorrespondingMention(int tokenIndex, Map<Integer, CorefChain> graph)//List<CorefChain.CorefMention> allMentions)
	{
		
		CorefChain.CorefMention parentMention = null;
		List<CorefChain.CorefMention> mentionsInCurChain  = null;
		for (Entry<Integer, CorefChain> entry : graph.entrySet())
	    {
	    	CorefChain value = entry.getValue();
	    	//System.out.println("id " + entry.getKey());
	    	mentionsInCurChain =  value.getMentionsInTextualOrder();
			for (CorefChain.CorefMention curMention: mentionsInCurChain)
			{
				int mentionStartInd = curMention.startIndex;
				if (mentionStartInd  > tokenIndex) //we went past this token
				{
					break;
				}
				else
				{
					int mentionEndIndex = curMention.endIndex;
					if (mentionEndIndex >= tokenIndex)
					{
						parentMention = curMention;
						break;
					}
				}
			}//end looping over this chain
	    }//end looping over all chains
		
		return parentMention;
		
	}
	
	public void ApplyAnimacyAnnotation()
	{
		
	}
	
	/**
	 * a simple method for computing token overlap between 2 sentences
	 * 
	 * filters parts of speech
	 */
	public double CosineSimilaity(SentenceTree tree2)
	{
		int size1 = 0;
		int overlap = 0;
		
		ArrayList<String> lemmas2 = new ArrayList<String>();
		for (Integer curId: tree2.nodes.keySet() )
		{
			ParseNode curChild = tree2.nodes.get(curId);
			if (curChild.isPunct == true)
				continue;
			lemmas2.add(curChild.GetLemma());
		}
		
		for (Integer id1: this.nodes.keySet())
		{
			ParseNode child1 = this.nodes.get(id1);
			if (child1.isPunct == true)
				continue;
			
			//make sure this is not a stop word
			boolean isStopPOS = false;
			for (String morpho1: child1.morpho)
			{
				if (SentenceTree.STOP_POS.contains(morpho1))
				{
					isStopPOS = true;
					break;
				}
			}
			
			size1++; //we count stop word in size but not in overlap
			
			if (isStopPOS)
				continue;
			

			
			String lemma1 = child1.GetLemma();
			
			if (lemmas2.contains(lemma1))
				overlap++;
		}
		
		double denominator = Math.sqrt( (double)( size1 * lemmas2.size() ) );
		double numerator = (double) overlap;
		
		double result = numerator / denominator;
		return result;
		
	}
	
	public void SetPseudoId(int pseudo)
	{
		this.pseudoId = pseudo;
	}
	
	public int GetPseudoId()
	{
		return this.pseudoId;
	}

	
	/*private class NodeComparator implements Comparator<ParseNode>
	{
//compares nodes based on their id. We assume no two nodes ever have same id.
		
        public int compare(ParseNode n1, ParseNode n2) {
        	if (n1.id == n2.id)
        		return 0;
        	else if (n1.id > n2.id)
        		return 1;
        	else //n1.id < n2.id
        		return -1;
        }    
	}*/
	
	
}



