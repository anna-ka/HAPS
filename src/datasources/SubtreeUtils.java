/**helper methods to find information in subtrees
 * 
 */
package datasources;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.regex.*;
import java.util.ArrayList;

/**
 * @author anna
 *
 */
public class SubtreeUtils {
	
	/*a method for finding the first child of a node
	 * that has a specified syntactic function
	 * 
	 * it does not look more that one level into the subtree and only finds one child, even if there are more
	 * */
	public static ParseNode FindChildBySyntax(ParseNode node, String syntFunction)
	{
		
		TreeMap<Integer, ParseNode> kids = node.GetChildren();
		
		if (kids.isEmpty() || syntFunction.isEmpty())
			return null;
		
		
		Iterator <Integer> it = kids.keySet().iterator();
		while (it.hasNext())
		{
			ParseNode curChild = kids.get(it.next());
			if (curChild.syntFunction.contains(syntFunction))
				return curChild;
		}
		return null;
	}
	
	// a node to find the first child with a specific surface pattern
	//does not look at grand-children or further in the tree
	public static ParseNode FindChildBySurface(ParseNode node, String surface)
	{
		TreeMap<Integer, ParseNode> kids = node.GetChildren();
				
		if (kids.isEmpty() || surface.isEmpty())
			return null;
		
		Iterator <Integer> it = kids.keySet().iterator();
		while (it.hasNext())
		{
			ParseNode curChild = kids.get(it.next());
			if (curChild.syntSurface.contains(surface))
				return curChild;
		}
		return null;
	}
	
	public static ParseNode FindChildByMorphoTag(ParseNode node, String morphoTag)
	{
		TreeMap<Integer, ParseNode> kids = node.GetChildren();
				
		if (kids.isEmpty() || morphoTag.isEmpty())
			return null;
		
		Iterator <Integer> it = kids.keySet().iterator();
		while (it.hasNext())
		{
			ParseNode curChild = kids.get(it.next());
			if (curChild.morpho.contains(morphoTag))
				return curChild;
		}
		return null;
	}
	
	
	/*Find child by dependency
	 * */
	public static ParseNode FindChildByDependency(ParseNode node, String dep)
	{
		
		TreeMap<Integer, ParseNode> kids = node.GetChildren();
		
		if (kids.isEmpty() || dep.isEmpty())
			return null;
		
		
		Iterator <Integer> it = kids.keySet().iterator();
		while (it.hasNext())
		{
			ParseNode curChild = kids.get(it.next());
			String childDep = curChild.GetDependency();
			if (childDep.compareToIgnoreCase(dep) == 0)
			{	
				//System.out.println("found child with matching dep: " + dep + " - " + curChild.GetText());
				return curChild;
			}
			/*else
			{
				System.out.println( "'"+ dep + "' and '" + childDep + "' do not match in: " + curChild.GetText());
			}*/
		}
		return null;
	}
	

	// a method to find all children with a specific surface tag
	// does not look into grandchildren or further down the tree
	public static ArrayList<ParseNode> FindChildrenBySurface(ParseNode node, String surface)
	{
		TreeMap<Integer, ParseNode> kids = node.GetChildren();
		ArrayList<ParseNode> goodNodes = new ArrayList<ParseNode>();
				
		if (kids.isEmpty() || surface.isEmpty())
			return null;
		
		Iterator <Integer> it = kids.keySet().iterator();
		while (it.hasNext())
		{
			ParseNode curChild = kids.get(it.next());
			if (curChild.syntSurface.contains(surface))
				goodNodes.add(curChild);
		}
		return goodNodes;
	}
	
	// a method to find all children with a syntactic function tag
	// does not look into grandchildren or further down the tree
	public static ArrayList<ParseNode> FindChildrenBySyntFunc(ParseNode node, String syntFunc)
	{
		TreeMap<Integer, ParseNode> kids = node.GetChildren();
		ArrayList<ParseNode> goodNodes = new ArrayList<ParseNode>();
				
		if (kids.isEmpty() || syntFunc.isEmpty())
			return null;
		
		Iterator <Integer> it = kids.keySet().iterator();
		while (it.hasNext())
		{
			ParseNode curChild = kids.get(it.next());
			if (curChild.syntFunction.contains(syntFunc))
				goodNodes.add(curChild);
		}
		return goodNodes;
	}
	
	// a method that returns true iff a node has a particular syntactic function
	public static boolean MatchSyntacticFunction(ParseNode node, String syntFunction)
	{
		if (node.syntFunction.contains(syntFunction))
			return true;
		else
			return false;
		
	}
	
}
