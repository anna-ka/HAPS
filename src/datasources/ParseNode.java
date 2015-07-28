package datasources;

import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.TreeMap;
import java.lang.Comparable;
import java.util.NoSuchElementException;

import java.lang.StringBuilder;
import java.util.regex.*;


/* a class to hold one parse token (node)
 * from an XML Connexor parse tree
 * 
 * */

public class ParseNode implements Comparable<ParseNode>{
	
	/*the ParseNodes are compared only using ids. Therefore they can only be compared within a document, where ids are _never_ repeated
	 * 
	 * */
	
	public static int INVALID = -1;//a constant to remember bad nodes
	
	
	int id = -1;
	int parentId = -1;
	boolean isPunct = false; // is this a punctuation node?
	public final static int PUNCTUATION = -2;
	TreeMap<Integer, ParseNode> children = new TreeMap <Integer, ParseNode>();
	ParseNode parent = null;
	
	// a regular expression used for finding punctuation nodes
	//we will only check that the node starts with a punctuation character
	final static Pattern punctPattern = Pattern.compile("[\\W]+.*");
	final static Pattern cleanUpPattern = Pattern.compile("(?:[\\s\\n\\r\\t]+)");
	
	//an enum of valid named-entity tags
	public static enum NE{PERSON, LOCATION, ORGANIZATION, NONE};
	NE neTag = NE.NONE;
	
	public static enum ANIMACY{ANIMATE, INANIMATE};
	ANIMACY animacyTag = ANIMACY.INANIMATE;

	String text = null;
	String lemma = null;
	
	//if no tag is found, it is either a punctuation mark of a root of a subtree
	String dependency = null; //a dependency with the parent
	
	
	
	HashSet<String> morpho = null;
	HashSet<String> syntFunction = null;
	HashSet<String> syntSurface = null;
	HashSet<String> otherTags = null;
	
	ParseNode (int newId)
	{
		this.id = newId;
		morpho = new HashSet<String>();
		syntFunction = new HashSet<String>();
		syntSurface = new HashSet<String>();
		otherTags = new HashSet<String>();
	}
	
	public void CheckNodeType()
	// a method to check whether this is a punctuation node
	{
		if ( this.dependency == null && this.parentId == this.INVALID && this.children.isEmpty() == true)
		{
			//String t = this.text;
			//System.out.println("CANDIDATE for punct:'" + t + "'");
			Matcher m = this.punctPattern.matcher(this.text);
			if (m.matches() == true)
			{
				this.isPunct = true;
				//System.out.println("found punct node");
			}
		}
	}
	
	public String GetNodeShortInfo()
	{
		return this.GetNodeInfo("");
		/*
		StringBuilder info = new StringBuilder();
		String tmpId = String.valueOf(this.id);
		String tmpText = ( this.text != null ) ? this.text : "NULL";
		String tmpLemma = ( this.lemma != null ) ? this.lemma : "NULL";
		String tmpDep = ( this.dependency != null ) ? this.dependency : "NULL";
		String parent = String.valueOf(this.parentId);
		info.append("Node: " + tmpText + "\n\tid: " + tmpId +
				"\n\tlemma: " + tmpLemma + "\n\tdep: " + tmpDep + "\n\tparent id:" + parent);
		if (this.isPunct == true)
		{
			info.append("\n\tPUNCT NODE: true");
		}
		else
			info.append("\n\tpunct node: false");
		
		
		
		return info.toString();
		*/
	}
	
	public String GetNodeShortInfo(String offset)
	{
		StringBuilder info = new StringBuilder();
		String tmpId = String.valueOf(this.id);
		String tmpText = ( this.text != null ) ? this.text : "NULL";
		String tmpLemma = ( this.lemma != null ) ? this.lemma : "NULL";
		String tmpDep = ( this.dependency != null ) ? this.dependency : "NULL";
		String parent = String.valueOf(this.parentId);
		info.append(offset + "Node: " + tmpText + 
				"\n" + offset + "\tid: " + tmpId +
				"\n" + offset + "\tlemma: " + tmpLemma + 
				"\n" + offset + "\tdep: " + tmpDep + 
				"\n" + offset + "\tparent id:" + parent);
		if (this.isPunct == true)
		{
			info.append("\n" + offset + "\tPUNCT NODE: true");
		}
		else
			info.append( "\n" + offset  + "\tpunct node: false");
		
		String ne = "NONE";
		switch (this.neTag)
		{
		case LOCATION:
			ne = "LOCATION";
			break;
		case PERSON:
			ne = "PERSON";
			break;
		case ORGANIZATION:
			ne = "ORGANIZATION";
		}
		
		info.append("\n" + offset + "\tNE tag: " + ne);
		String animacy = "INANIMATE";
		switch (this.animacyTag)
		{
		case ANIMATE:
			animacy = "ANIMATE";
			break;
		default:
			animacy = "INANIMATE";
		}
		
		info.append("\n" + offset + "\tANIM tag: " + animacy);
		
		return info.toString();
		
	}
	
	public String GetNodeInfo()
	{
		return this.GetNodeInfo("");
	}
	
	public String GetNodeInfo(String offset)
	{
		try{
			StringBuilder info = new StringBuilder();
			String tmpId = String.valueOf(this.id);
			String tmpText = ( this.text != null ) ? this.text : "NULL";
			String tmpLemma = ( this.lemma != null ) ? this.lemma : "NULL";
			String tmpDep = ( this.dependency != null ) ? this.dependency : "NULL";
			String parent = String.valueOf(this.parentId);
			info.append(offset + "Node: " + tmpText + 
					"\n" + offset + "\tid: " + tmpId +
					"\n" + offset + "\tlemma: " + tmpLemma + 
					"\n" + offset + "\tdep: " + tmpDep + 
					"\n" + offset + "\tparent id:" + parent);
			if (this.isPunct == true)
			{
				info.append("\n" + offset + "\tPUNCT NODE: true");
			}
			else
				info.append( "\n" + offset  + "\tpunct node: false");
			
			String ne = "NONE";
			switch (this.neTag)
			{
			case LOCATION:
				ne = "LOCATION";
				break;
			case PERSON:
				ne = "PERSON";
				break;
			case ORGANIZATION:
				ne = "ORGANIZATION";
			}
			
			info.append("\n" + offset + "\tNE tag: " + ne);
			
			Iterator <String> mIt = this.morpho.iterator();
			Iterator <String> sfIt = this.syntFunction.iterator();
			Iterator <String> surfaceIt = this.syntSurface.iterator();
			Iterator <String> otherIt = this.otherTags.iterator();
			
			info.append("\n" + offset + "\tsynt. func: ");
			while (sfIt.hasNext())
			{
				String tag = sfIt.next();
				info.append(tag + ", ");
			}
			
			info.append("\n" + offset + "\tsurface: ");
			while (surfaceIt.hasNext())
			{
				String tag = surfaceIt.next();
				info.append(tag + ", ");
			}
			
			info.append("\n" + offset + "\tmorpho: ");
			while (mIt.hasNext())
			{
				String tag = mIt.next();
				info.append(tag + ", ");
			}
			
			info.append("\n" + offset + "\tother ");
			while (otherIt.hasNext())
			{
				String tag = otherIt.next();
				info.append(tag + ", ");
			}
			
			return info.toString();
		}
		catch (Exception e)
		{
			System.out.println("ERROR in ParseNode.GetNodeINfo " + e.getMessage());
			return null;
		}
		
	}
	
	//TO DO: re-write this, it does not work
	/*
	//the method returns the size of the subtree: 1 + the number of decendants
	public int GetSizeOfSubtree() throws Exception
	{
		//get id of the first child
		ParseNode firstChild = this;
		ParseNode lastChild = this;
		
		int firstId = -1;
		int lastId = -1;
		
		while(firstChild.children.isEmpty() == false)
		{
			
			firstChild = firstChild.children.get( firstChild.children.firstKey() );
		}
		//we got to the bottom of the subtree
		firstId = firstChild.GetId();
		System.out.println("first child " + firstChild.GetText() + " id: " + String.valueOf(firstId));
		
		
		//get the id of the last child
		while(lastChild.children.isEmpty() == false)
		{
			lastChild = lastChild.children.get( lastChild.children.lastKey() );
			
		}
		if (lastChild == firstChild)
			
		
		lastId = lastChild.GetId();
		System.out.println("last child " + lastChild.GetText() + " id: " + String.valueOf(lastId));
		
		if (firstId == -1 || lastId == -1)
		{
			throw new Exception ("Exception in ParseNode.GetSizeOfSubtree: firstId or lastId has illegal value");
		}
		else if (firstId == lastId)
		{
			return 1;
		}
		{
			return (lastId - firstId + 2);
		}
			
		
	}*/
	
	//computes subtree size by traversing the full subtree
	public int ComputeSubtreeSize()
	{
		int size = 1; //counted the root
		for (Integer curId: this.children.keySet())
		{
			ParseNode curChild = this.children.get(curId);
			size = this.CountChildren(curChild, size);
		}
		return size;
	}
	
	private int CountChildren(ParseNode root, int oldSize)
	{
		//System.out.println("***\t"+ root.GetText() + " > \told size: " + String.valueOf(oldSize));
		int newSize = oldSize + 1;
		//System.out.println("***\t"+ root.GetText() + " > \tnew size: " + String.valueOf(newSize));
		for (Integer curId: root.children.keySet())
		{
			ParseNode curChild = root.children.get(curId);
			newSize = this.CountChildren(curChild, newSize);
		}
		return newSize;
		
	}
	
	public int GetId()
	{return this.id;}
	
	public void SetId(int newId)
	{
		this.id = newId;
	}
	
	public String GetText()
	{return this.text;}
	
	public void SetText(String newText)
	{
		//this.text = newText;
		Matcher m = this.cleanUpPattern.matcher(newText);
		this.text = m.replaceAll("");
	}
	
	public String GetDependency ()
	{return this.dependency;}
	
	public void SetDependency(String newDep)
	{
		String nDep = newDep.replace(":", "");
		this.dependency = nDep;
	}
	
	public String GetLemma()
	{return this.lemma;}
	
	public void SetLemma(String newLemma)
	{
		this.lemma = newLemma;
	}
	
	public int GetParentId()
	{return this.parentId;}
	
	public void SetParentId(int newId)
	{
		this.parentId = newId;
	}
	
	public TreeMap<Integer, ParseNode> GetChildren()
	{
		return this.children;
	}
	
	public void AddChild(Integer key, ParseNode child)
	{
		this.children.put(key, child);
	}
	
	public void SetParent(ParseNode newDad)
	{
		this.parent = newDad;
	}
	
	public ParseNode GetParent()
	{
		return this.parent;
	}
	
	public NE GetNE()
	{
		return this.neTag;
	}
	
	public void SetNE(NE newTag)
	{
		this.neTag = newTag;
	}
	
	public ANIMACY GetAnimacyTag() {
		return animacyTag;
	}

	public void SetAnimacyTag(ANIMACY animacyTag) {
		this.animacyTag = animacyTag;
	}

	public int compareTo(ParseNode n2) {
		Integer oldId = new Integer (this.id);
		return oldId.compareTo(n2.id);
	}

}
