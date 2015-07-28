/**
 * 
 */
package datasources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Set;

/**
 * @author anna
 *a class that takes a SentenceTree instance and finds all NPs and their features
 *
 *features: 
 *	determiner
 *	type
 *	animate/inanimate
 *	modifier type
 *	is ultimate head
 *	fist np in sent (or position in sent?)
 */


public class NPExtractor {
	
	SentenceTree sent = null;
	ArrayList<NounPhrase> nps = new ArrayList<NounPhrase>();
	
	public ArrayList<NounPhrase> GetNPs()
	{
		return this.nps;
	}
	
	public NPExtractor(SentenceTree tree)
	{
		this.sent = tree;
	}
	
	public void PrintNPs()
	{
		Iterator<NounPhrase> it = this.nps.iterator();
		while( it.hasNext() )
		{
			NounPhrase curNP = it.next();
			curNP.PrintNP("");
		}
	}

	public void ExtractNPs()
	{
		Iterator<ParseNode> it = sent.roots.iterator();
		while (it.hasNext())
		{
			ParseNode curNode = it.next();
			this.ExtractNPsFromSubtree(curNode);
		}
	}
	
	public void ExtractNPsFromSubtree(ParseNode root)
	{
		if (root == null)
		{
			System.out.println("warning in NPExtractor.ExtratNPsFromSubtree: null root");
			return;
		}
		if (root.syntSurface.contains("NH") )
		{
			try{
				NounPhrase np = new NounPhrase(root);
				//System.out.println("found np " + root.GetText());
				np.Init();
				np.GetDependents();
				this.nps.add(np);
			}
			catch (Exception e)
			{
				System.out.println("Exception in NPExtractor.ExtratNPsFromSubtree: " + e.getMessage());
			}	
		}
		else
		{
			//System.out.println("Not an np head " + root.GetText());
		}
		
		//now check children
		TreeMap<Integer, ParseNode> kids = root.GetChildren();
		Set<Integer> keys = kids.keySet();
		Iterator <Integer> it = keys.iterator();
		while (it.hasNext())
		{
			ParseNode curNode = kids.get(it.next());
			this.ExtractNPsFromSubtree(curNode);
		}
				
	}
}
