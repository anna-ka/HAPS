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

/**
 * 
 */
package datasources;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author anna
 *
 */
public class SimpleFileDataSource implements IDataSource, IGenericDataSource, Cloneable {
	
	protected ArrayList<String> chunks = new ArrayList<String>();
	//protected ArrayList<Integer> refBreaks = new ArrayList<Integer>();
	
	protected TreeMap <Integer, ArrayList<Integer>> refBreaks = new TreeMap <Integer, ArrayList<Integer>>();
	protected Pattern segmPattern;
	protected String segmString = "==========";
	protected File inputFile;
	protected Double aveSegmLength = null;
	protected String text = null;
	protected int levelOfSegm = 1;
	protected Pattern punktPattern  = Pattern.compile("[^\\w]+");
	
	/**
	 * this is the top level according to Lucien Carroll's representation
	 */
	protected int defaultLevel = 2;
	
	protected boolean isHierarchical = false;
	
	@Override
	/**
	 * This method creates a deep copy of refBreaks and aveSegmLength.
	 * Everything else is shared.
	 */
	public Object clone() throws CloneNotSupportedException {
		SimpleFileDataSource cloned = (SimpleFileDataSource) super.clone();
		
		
		try {
			cloned.SetReferenceBreaks(0, new ArrayList<Integer>(this.GetReferenceBreaks(0)));
		} catch (Exception e) {
			CloneNotSupportedException e1 = new CloneNotSupportedException(e.getMessage());
			throw (e1);
		}
		cloned.aveSegmLength = null;
		return cloned;
		
	}
	
	/**
	 * the next 2 methods are a workround to allow making new instance new newInstance()
	 * this code needs to be cleaned up to unify Init methods
	 */
	public SimpleFileDataSource()
	{
		
	}
	
	
	public void Initialize(File inputF, String segmPatternStr)
	{
		this.segmPattern = Pattern.compile( segmPatternStr );
		
		this.inputFile = inputF;
		
	}
	
	public SimpleFileDataSource(File inputF, String segmPatternStr)
	{
		this.segmPattern = Pattern.compile( segmPatternStr );
		this.inputFile = inputF;
		
	}
	
	/**
	 * This method is obsolete. It only preserves backward compatibility. 
	 * Use Init(int levelOfSegmentation, String segmPattern, File textFile, File[] annotationFiles) instead.
	 * 
	 @return the name of this data source (the name of the input file)
	 */
	public void Init(int basicUnits) throws Exception
	{
		if (basicUnits != IGenericDataSource.SENT_LEVEL && basicUnits != IGenericDataSource.PAR_LEVEL)
		{
			Exception e = new Exception ("Exception in SimpleFileDataSource.Init: invalid level of segmentation specified: " + String.valueOf(basicUnits));
			throw (e);
		}
		this.levelOfSegm = basicUnits;
		if (basicUnits == IDataSource.SENT_LEVEL)
			this.InitSentLevel();
		else if (basicUnits == IDataSource.PAR_LEVEL)
			this.InitParLevel();
		else
		{
			Exception e = new Exception ("Exception in SimpleFileDataSource.Init(): Invalid value of basic units " + String.valueOf(basicUnits) );
			throw e;
		}
	}
	
	/**
	 * Main Init method.
	 * @param levelOfSegmentation
	 * @param segmPattern a string denoting segment breaks in textFile, should be compileable into a regular expression
	 * @param textFile : input file with reference segmentation
	 * @param annotationFiles  : should be null for this class
	 * 
	 */
	public void Init(int levelOfSegmentation, String segmPattern,
			File textFile, File annotationsFile) throws Exception {
		
		if (levelOfSegm != IGenericDataSource.SENT_LEVEL && levelOfSegm != IGenericDataSource.PAR_LEVEL)
		{
			Exception e = new Exception ("Exception in SimpleFileDataSource.Init: invalid level of segmentation specified: " + String.valueOf(levelOfSegmentation));
			throw (e);
		}
		this.levelOfSegm = levelOfSegmentation;
		
		if (segmPattern != null)
			this.segmPattern = Pattern.compile( segmPattern );
		this.inputFile = textFile;
		
		if (this.isHierarchical == true)
			this.InitHierarchicalCarrolStyle();
		else if (levelOfSegmentation == IDataSource.SENT_LEVEL)
			this.InitSentLevel();
		else if (levelOfSegmentation == IDataSource.PAR_LEVEL)
			this.InitParLevel();
		else
		{
			Exception e = new Exception ("Exception in SimpleFileDataSource.Init(): Invalid value of basic units " + String.valueOf(levelOfSegmentation) );
			throw e;
		}
		
	}
	
	public void Init (int levelOfSegmentation, String segmPattern, String text, File annotationsFile) throws Exception
	{
		if (segmPattern != null)
			this.segmPattern = Pattern.compile( segmPattern );
		
		this.inputFile = null;
		this.text = text;
		
		if (levelOfSegmentation == IDataSource.SENT_LEVEL)
			this.InitSentLevel();
		else if (levelOfSegmentation == IDataSource.PAR_LEVEL)
			this.InitParLevel();
		else
		{
			Exception e = new Exception ("Exception in SimpleFileDataSource.Init(): Invalid value of basic units " + String.valueOf(levelOfSegmentation) );
			throw e;
		}
	}
	

	
	protected void InitSentLevel()
	{
		try
		{
			
			BufferedReader reader ;
			ArrayList <Integer> levelZeroRefBreaks = new ArrayList<Integer>();
			
			if (this.inputFile != null)
				reader = new BufferedReader(new FileReader(this.inputFile));
			else 
				reader = new BufferedReader(new StringReader(this.text));
			
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty() )
					continue;
				Matcher m = this.segmPattern.matcher(line);
				if (m.matches())
				{
					//do not include a break before beginning of the text 
					if (this.chunks.size() > 0)
						
						levelZeroRefBreaks.add( this.chunks.size() - 1 );
//					this.refBreaks.put(this.defaultLevel, this.chunks.size() - 1)
				}
				else
					this.chunks.add(line);
			
			}
			
			//by convention always add a break at the end, if there was not one
			Integer lastRefBreak = levelZeroRefBreaks.get( levelZeroRefBreaks.size() - 1);

			if (lastRefBreak < this.GetNumChunks() - 1)
				levelZeroRefBreaks.add( new Integer (this.GetNumChunks() - 1));
		
			this.refBreaks.put (this.defaultLevel, levelZeroRefBreaks);	
			
		}
		catch (Exception e)
		{
			System.out.println("Exception in SimpleFileDataSource.InitSentLevel(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	protected void InitParLevel()
	{
		//was the last line a paragraph break?
		boolean parEnd = false;
		
		try
		{
			
			BufferedReader reader ;
			ArrayList <Integer> levelZeroRefBreaks = new ArrayList<Integer>();
			
			if (this.inputFile != null)
				reader = new BufferedReader(new FileReader(this.inputFile));
			else 
				reader = new BufferedReader(new StringReader(this.text));
			
			String line;
			StringBuilder curPar = new StringBuilder();
			
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty() )
				{
					if ( parEnd == true)
					{
						continue;
					}
					else // we encountered the end of a paragraph
					{
						parEnd = true;
						this.chunks.add(curPar.toString());
						curPar = new StringBuilder();
						continue;
					}
				}
				
				parEnd = false;
				Matcher m = this.segmPattern.matcher(line);
				if (m.matches())
					levelZeroRefBreaks.add( this.chunks.size() - 1 );
				else
					curPar.append(" " + line);
			
			}
			//include the last paragraph
			if (curPar.toString().isEmpty() == false )
				this.chunks.add(curPar.toString());
			
			this.refBreaks.put(this.defaultLevel, levelZeroRefBreaks);
			
		}
		catch (Exception e)
		{
			System.out.println("Exception in SimpleFileDataSource.InitParLevel(): " + e.getMessage());
		}
	}
	
	
	/**
	 * a method to load hierarchial segmentations, sentence level
	 */
	public void InitHierarchicalCarrolStyle() throws Exception
	{
		if (this.isHierarchical == false)
		{
			Exception e = new Exception ("cannot do InitHierarchicalCarrolStyle when data source is not hierarchical");
			throw (e);
		}
		Pattern boundaryPattern = Pattern.compile("===[\\s]+([\\d]+)[\\s]+===");
		
		
		String body;
		if (this.inputFile != null)
			body = TextFileIO.ReadTextFile(this.inputFile);
		else 
			body = this.text;
		
		String[] lines = TextFileIO.LinesToArray(body);
		

		for (int i = 0; i < lines.length; i++)
		{
			String line = lines[i];
			
			Matcher m = boundaryPattern.matcher(line);
			if (m.find() == true)
				//found a boundary marker
			{
				String level = m.group(1);
				Integer boundaryLevel = new Integer(level);
				if (this.chunks.size() > 0)
				{
					//System.out.println(" boundary level " + boundaryLevel + " after chunk " + this.chunks.size() + "\n\t***" + lines[i-1]);
					if (this.refBreaks.containsKey(boundaryLevel))
					{
						this.refBreaks.get(boundaryLevel).add(this.chunks.size() - 1);
					}
					else
					{
						ArrayList<Integer> newLevel = new ArrayList<Integer>();
						newLevel.add(this.chunks.size() - 1);
						this.refBreaks.put (boundaryLevel, newLevel);
					}
	
				}
				
			}
			else //otherwise it is just a regular line
			{
				Matcher punktMatcher = this.punktPattern.matcher(line);
				if (punktMatcher!= null && punktMatcher.matches() == true)
					continue;
				this.chunks.add(line);
			}
		}
	}
	
	public void ReconcileBreaks()
	{
		ArrayList<Integer> levels = new ArrayList<Integer>();
		for (Integer level : refBreaks.keySet())
		{
			levels.add(level);
		}
		
		for (int i = 0; i < levels.size() - 1; i++) {
			for (Integer br : refBreaks.get(levels.get(i))) {
				insertBreakInLevel(levels.get(i + 1), br);
			}
		}
	}
	
	private void insertBreakInLevel(Integer level, Integer br)
	{
		ArrayList<Integer> breaks = refBreaks.get(level);
		for (int i = 0; i < breaks.size(); i++)
		{
			Integer refBreak = breaks.get(i);
			if (refBreak.equals(br)) return;
			if (refBreak > br) {
				breaks.add(i, br);
				return;
			}
		}
		
		breaks.add(br);
	}
	
	/**
	 * this method transforms breaks downloaded from Lucian Carrol's dataset so as to include all the breaks explicitely
	 */
	public void TransformCarrolsBreaks()
	{
		
		ArrayList<Integer> root = new ArrayList<Integer>();
		root.add(this.GetNumberOfChunks() - 1);
		
		
		ArrayList<Integer> levelAbove = root;
		
		Set<Integer> levels = this.refBreaks.keySet(); // ascending order, we start at the top
		
		for (Integer curLevel: levels)
		{
			ArrayList<Integer> curBreaks = this.refBreaks.get(curLevel);
			
			try {
				levelAbove = this.refBreaks.get(curLevel - 1);
			}
			catch(Exception e)
			{
				levelAbove  = root;
				System.out.println("No level above " + curLevel);
			}
			
			if (levelAbove == null)
				levelAbove  = root;
			
			int[] curLevelBinary = new int[this.GetNumberOfChunks()];
			int[] levelAboveBinary = new int[this.GetNumberOfChunks()];
			
			for (int i = 0; i < this.GetNumberOfChunks(); i++)
			{
				if (levelAbove.contains(new Integer(i)))
					levelAboveBinary[i] = 1;
				else
					levelAboveBinary[i] = 0;
				
				
				if (curBreaks.contains(i))
					curLevelBinary[i] = 1;
				else
					curLevelBinary[i] = 0;
			}
			
			//now let's go segment by segment, looking at the level above
			
			int segmStart = 0;
			int segmEnd = segmStart;
			
			//for (int j = segmStart; j< this.GetNumberOfChunks(); j++)
			while(segmStart < this.GetNumberOfChunks() && segmEnd < this.GetNumberOfChunks())
			{
				while (levelAboveBinary[segmEnd] != 1 && segmEnd < this.GetNumberOfChunks() - 1)
					segmEnd++;
				
				
				
				//if there are any segments in this span at the current level, propagate the end of the segment from above
				
				int counter = 0;
				for (int j = segmStart; j <= segmEnd; j++)
				{
					if ( curLevelBinary[j] == 1)
						counter++;
				}
				
				if (counter > 0)
				{
					if (curLevelBinary[segmEnd] == 0)
						curLevelBinary[segmEnd] = 1;
				}
				
				segmStart = segmEnd + 1;
				segmEnd = segmStart;
				
			}
			
			ArrayList<Integer> newCurBreaks = new ArrayList<Integer>();
			for (int j = 0; j < this.GetNumberOfChunks(); j++)
			{
				if (curLevelBinary[j] == 1)
					newCurBreaks.add(j);
			}
			
			this.refBreaks.put(curLevel, newCurBreaks);
			
		}
		
		ReconcileBreaks();
	}
	

	/**
	 * @see org.kazantseva.segmentor.IDataSource#GetChunk(int)
	 */
	public String GetChunk(int index) {
		// TODO Auto-generated method stub
		return this.chunks.get(index);
	}

	/** 
	 * Obsolete method. Use GetNumberOfChunks(int annotId) instead with annotId = 1
	 */
	public int GetNumChunks() {
		return this.chunks.size();
	}
	
	/**
	 * @return number of basic units (sent, pars or chunks) in the reference segmentation
	 */
	public int GetNumberOfChunks() {
		return this.chunks.size();
	}
	
	/**
	 * @param annotId always 0
	 * @return number of segments in the reference annotation
	 */
	public int GetNumberOfSegments(int annotId) {
		return this.refBreaks.get(this.defaultLevel).size();
	}


	/**
	 * Returns the positions of segment boundaries in this data source
	 * @param annotId = 0
	 */
	public ArrayList<Integer> GetReferenceBreaks(int annotId) {
		return this.refBreaks.get(this.defaultLevel);
	}

	/**
	 * Obsolete method.
	 * Returns the positions of segment boundaries in this data source
	 * @param annotId = 0
	 * @see ArrayList<Integer> GetReferenceBreaks(int annotId)
	 */
	public Integer[] GetReferenceSegmentBreaks() {
		// TODO Auto-generated method stub
		return this.refBreaks.get(this.defaultLevel).toArray(new Integer[this.refBreaks.get(this.defaultLevel).size()]);
	}

	/**
	 * returns refrence breaks in hierarchical format
	 * @return
	 */
	public TreeMap<Integer, ArrayList<Integer>> GetReferenceBreaksHierarchical()
	{
		return this.refBreaks;
	}
	
	public void SetReferenceBreaksHierarchical(TreeMap<Integer, ArrayList<Integer>> referenceBreaks)
	{
		this.refBreaks = referenceBreaks;
	}

	public String GetName() {
		if (this.inputFile != null)
			return this.inputFile.getName();
		else
		{
			return "text-initialized SimpleFileDataSource.";
		}
	}
	
	public void PrintChunks()
	{
		for (int i = 0; i < this.chunks.size(); i++ )
		{
			System.out.println("<" + String.valueOf(i) + ">\t" + this.chunks.get(i) );
		}
	}


	
	public void Output(File outputFile, Integer[] breaks) throws Exception{
		if (breaks == null || breaks.length == 0)
			breaks = new Integer[]{ new Integer (this.GetNumChunks() - 1) };
		StringBuilder str = new StringBuilder();
		
		int breakIndex = 0;
		int breakValue = breaks[breakIndex];
		
		for (int i = 0; i <= this.GetNumChunks() - 1; i++)
		{
			if (i <= breakValue)
			{
				str.append(this.GetChunk(i) + "\n");
			}
			else
			{
				str.append(this.segmString + "\n");
				
				if (breakIndex < breaks.length - 1)
				{
					breakIndex ++;
					breakValue = breaks[breakIndex];
					
				}
				else
				{
					breakValue = Integer.MAX_VALUE;
				}	
			}
		}
		str.append(this.segmString + "\n");
		
		TextFileIO.OutputFile(outputFile, str.toString());
		System.out.println("output " + outputFile.getAbsolutePath());
		
		
	}
	
	public void OutputBreaksOnly(File outputFile, Integer[] breaks) throws Exception
	{
		StringBuilder txt = new StringBuilder();
		
		for (Integer curBr: breaks)
		{
			txt.append(curBr.toString() + "\t");
		}
		TextFileIO.OutputFile(outputFile, txt.toString());
		System.out.println("output breaks to " + outputFile.getAbsolutePath());
	}
	
	public ArrayList<Integer> GenerateRandomReferenceBreaks( int numBreaks )
	{
		
		ArrayList<Integer> newBr = new ArrayList<Integer>();
		
		int range = this.GetNumChunks() - 1;
		int counter = 0;
		
		while (counter < numBreaks) //for (int i = 0; i < numBreaks; i++)
		{
			double curBrD = Math.random() * range;
			int curBreak = (int) Math.round(curBrD);
			if ( newBr.contains( new Integer(curBreak) ) == false  && curBreak < (this.GetNumChunks() - 1) )
			{
				newBr.add(new Integer(curBreak));
				counter++;	
			}
		}
		
		//this.refBreaks = newBr;
		return newBr;
	}
	
	public void SetReferenceBreaks(Integer[] newRefBreaks)
	{
		ArrayList<Integer> newBr = new ArrayList<Integer>();
		for (int i = 0; i < newRefBreaks.length; i++)
		{
			Integer curBr = newRefBreaks[i];
			newBr.add(curBr);
		}
		this.refBreaks.put(this.defaultLevel, newBr);
	}
	
	/**
	 * @param annotId always 0
	 * @param  breaks positions of new segment breaks
	 */
	public void SetReferenceBreaks(int annotId, ArrayList<Integer> breaks) throws Exception {
	
		for (Integer br : breaks)
		{
			int numChunk = this.GetNumberOfChunks();
			if (br >= this.chunks.size())
			{
				String msg = "Exception in SimpleFileDataSource.SetReferenceBreaks. Segment break out of bounds: ";
				msg = msg + "segment break at " + br.toString() + " when there are " + String.valueOf(this.chunks.size()) + " chunks ";
				Exception e = new Exception (msg);
				throw (e);
			}
		}
		
		this.refBreaks.put(this.defaultLevel, breaks);
		
	}
	
	public String PrintRefBreaks()
	{
		String str = this.refBreaks.toString();
		return str;
	}


	

	/**
	 *@return Since this class is only for 1 available reference, the method always returns 1
	 */
	public int GetNumberOfAnnotators() {
		// TODO Auto-generated method stub
		return 1;
	}


	


	/**
	 * @return exact number of segments in the data source
	 */
	public Double GetAveNumberOfSegments() {
		return new Double (this.GetNumberOfSegments(0));
	}


	/**
	 * @return average segment length in the reference annotation
	 */
	public Double GetAverageSegmentLength() {
		
		if (this.aveSegmLength != null)
			return this.aveSegmLength;
		int numChunks = this.GetNumberOfChunks();
		int numSegm = this.GetNumberOfSegments(0);
		
		this.aveSegmLength = new Double ( (new Double (numChunks)) / (new Double( numSegm) ) );
		return this.aveSegmLength;
	}
	
	


	/**
	 * @param annotId always should be 0
	 * @return average segment length in the only reference annotation
	 */
	public Double GetAverageSegmentLengthForAnnot(int annotId) {
		return new Double ((double)(this.chunks.size()) / (double)(this.GetNumberOfSegments(0)));
	}


	/**
	 * @return number of segments in the reference annotation
	 */
	public Integer GetNumberOfSegmentsForAnnot(int annotId) {
		return this.refBreaks.get(this.defaultLevel).size();
	}


	/**
	 * Outputs the complete text marked with the breaks specified as a parameter 
	 * @param outputFile 
	 * @param breaks a set of breaks that needs to be output. Usually these will be hypothetical breaks computed by a segmenter
	 */
	public void OutputFullText(File outputFile, ArrayList<Integer> breaks) throws Exception {
		
		if (this.isHierarchical == false)
			this.OutputFullTextFlat(outputFile,  breaks);
		else
			this.OutputFullTextHierarchical(outputFile);
			
	}
	public void OutputFullTextHierarchical(File outputFile) throws Exception
	{
		this.OutputFullTextWithHypoBreaks(outputFile, this.refBreaks);

	}
	
	/**
	 * This method  takes a TreeMap<Integer, ArrayList<Integer>>
	 * and prints out a file that can be use to run EvalHDS.
	 * 
	 * The method expects the levels to be consistent with Carrol's style: 2 is the topmost layer, increasing indices for lower levels.
	 * Use HAPSBreaksToCarrolStyle to transform the breaks;
	 * 
	 * to be consistent with the style of Lucien Carrol's wikipedia dataset, we do not output the closing break of the segment
	 */
	public void OutputFullTextWithHypoBreaks(File outputFile, TreeMap<Integer, ArrayList<Integer>> assigns) throws Exception
	{
//		Set<Integer> levels = assigns.descendingKeySet();
		Set<Integer> levels = assigns.keySet();
		
		TreeSet<Integer> breaksAlreadyIncluded = new TreeSet<Integer>();
		
		
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < this.GetNumberOfChunks(); i++)
		{
			String curChunk = this.GetChunk(i);
			str.append(i + " " + curChunk + "\n");
			
			for (Integer curLevel: levels)
			{
				if (curLevel > 4)
					continue;
				
				ArrayList<Integer> curBreaks = assigns.get(curLevel);
				Integer lastBreak = curBreaks.get(curBreaks.size() - 1);
				
//				Integer evalHDSLevel = 2 + (levels.size()-1 - curLevel);//in EvalHDS all levels start at 2, where 2 is the topmost level
				Integer evalHDSLevel = curLevel;
				
				if (curBreaks.contains(new Integer(i)))
				{
					if (i != lastBreak && breaksAlreadyIncluded.contains(new Integer(i)) == false)
					{
						str.append("==== " + evalHDSLevel.toString() + " ====" + "\n");
						breaksAlreadyIncluded.add(i);
						break;
					}
					
				}
			}
		}
		TextFileIO.OutputFile(outputFile, str.toString());
	}
	
	public void OutputFullTextFlat(File outputFile, ArrayList<Integer> breaks) throws Exception
	{
		int breakIndex = 0;
		
		if (breaks == null || breaks.isEmpty())
			breaks = this.refBreaks.get(this.defaultLevel);
		
		Integer nextBreak = breaks.get(breakIndex);
		StringBuilder str = new StringBuilder();
		
		for (int i = 0; i <= this.chunks.size(); i++)
		{
			str.append(this.GetChunk(i) + "\n");
			if (i == nextBreak)
			{
				str.append(this.segmString + "\n");
				breakIndex++;
				nextBreak = breaks.get(breakIndex);
			}
			
		}
		TextFileIO.OutputFile(outputFile, str.toString());
	}


	/**
	 * Outputs segment breaks specified as a parameter 
	 * @param outputFile 
	 * @param breaks a set of breaks that needs to be output. Usually these will be hypothetical breaks computed by a segmenter
	 */
	public void OutputBreaksOnly(File outputFile, ArrayList<Integer> breaks) throws Exception {
		StringBuilder str = new StringBuilder();
		
		for (Integer br: breaks)
		{
			str.append(br + ",");
		}
		TextFileIO.OutputFile(outputFile, str.toString());
	}


	/**
	 * A method to create light weight instances, to hold results of segmentations
	 * @param numChunks
	 */
	public void LightWeightInit(  int numChunks) {
		
		ArrayList<Integer> breaks = new ArrayList<Integer>();
		
		for (int i = 0; i < numChunks; i++)
		{
			this.chunks.add("segm" + String.valueOf(i));
		}
		
		this.refBreaks.put(this.defaultLevel, breaks);
		
	}

	/**
	 * returns the level of segmentation - sentence or paragraph (see IGenericDataSource)
	 */
	public int GetLevelOfSegm() {
		return this.levelOfSegm;
	}

	/**
	 * specifies the level of segm: 1 is senetences, 2 is paragraphs
	 */
	public void SetLevelOfSegm(int segmLevel) {
		this.levelOfSegm = segmLevel;
		
	}

	@Override
	public void SetIfHierarchical(boolean isHier) {
		this.isHierarchical = isHier;
		
	}

	@Override
	public boolean GetIfHierarchical() {
		return this.isHierarchical;
	}
	
	public int GetNumberOfLevels()
	{
		return (this.refBreaks.keySet().size());
	}

	@Override
	public int GetNumberOfSegmentsAtLevel(int annotId, int levelId)
			throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Integer> GetReferenceBreaksAtLevel(int annotId, int levelId)
			throws Exception {
		return this.refBreaks.get(levelId);
	}

	@Override
	public void SetReferenceBreaksAtLevel(int annotId,
			ArrayList<Integer> breaks, int levelId) throws Exception {
		this.refBreaks.put(levelId, breaks);
		
	}
	
	/**
	 * This method transforms breaks returned by HAPS segmenter (or any other segmenter) to format acceptable by evalHDS
	 * 
	 * It takes a tree where 0 is the most prominent level and all breaks are explicitely specified and transforms it to tree where 2 is the most prominent level
	 * and n, n > 2 is the least prominent level
	 * 
	 * @param hapsBreaks
	 * @return
	 */
	public TreeMap<Integer, ArrayList<Integer>> HAPSBreaksToCarrolStyle( TreeMap<Integer, ArrayList<Integer>> hapsBreaks)
	{
		Set<Integer> levels = hapsBreaks.descendingKeySet() ;// in descending order, starts at 0
		TreeMap<Integer, ArrayList<Integer>> carrolBreaks = new TreeMap<Integer, ArrayList<Integer>> ();
		
		int curCarrolLevel = 2;
		
		for (Integer curLevel: levels)
		{
			carrolBreaks.put (curCarrolLevel, hapsBreaks.get(curLevel));
			curCarrolLevel++;
		}
		
		return carrolBreaks;
	}

	@Override
	public Double GetAveNumberOfSegmentsAtLevel(int levelId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double GetAverageSegmentLengthAtLevel(int levelId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * returns the name of the input file for this data source without any extensions. E.g. for somefile.txt -> somefile
	 * @return
	 */
	public String GetShortName()
	{
		Pattern namePattern = Pattern.compile("[^\\.]+"); // a pattern to identify only the name of the file name, not the extensions
		String name = this.GetName();
		Matcher m1 = namePattern.matcher(name);
		if (m1.find())
		{
			name = m1.group(0);
			//System.out.println("new name " + newName);
		}
		return name;
	}


	

}
