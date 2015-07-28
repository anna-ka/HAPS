package datasources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import segmenter.SegFileFilter;

public class HierarchicalMultipleAnnotsDataSource extends
		SimpleFileMultipleAnnotsDataSource {
	
	Pattern namePattern = Pattern.compile("[^\\.]+"); // a pattern to identify only the name of the file name, not the extensions
	
	
	
	public HierarchicalMultipleAnnotsDataSource()
	{
		super();
	}
	
	
	
	/**
	 * annotId -> <levelId -> ArrayList<breaksInThisLevel>>
	 */
	TreeMap <Integer, TreeMap<Integer, ArrayList<Integer>>> referenceBreaks = new TreeMap <Integer, TreeMap<Integer, ArrayList<Integer>>> ();
	
	/**
	 * Initialize using several hierarchical annotations
	 * @param annotationsFile directory that contains all annotations. The annotations must be in sub-directories with the same name as the main file
	 * For example ch1.txt will have reference annotations in annotationsFile/ch1/ch1-an1.csv
	 * 																			/ch1-an2.csv 
	 * etc
	 * 
	 */
	@Override
	public void Init(int levelOfSegmentation, String segmPattern,
			File textFile, File annotationsFile) throws Exception
	{
		if (levelOfSegmentation != IGenericDataSource.SENT_LEVEL && levelOfSegmentation != IGenericDataSource.PAR_LEVEL)
		{
			Exception e = new Exception ("Exception in SimpleFileMultipleAnnotsDataSource.Init: invalid level of segmentation specified: " + String.valueOf(levelOfSegmentation));
			throw (e);
		}
		this.inputFile = textFile;
		this.annotFile = annotationsFile;
		this.levelOfSegm = levelOfSegmentation;
		
		if (levelOfSegmentation == IGenericDataSource.SENT_LEVEL)
			this.InitSentLevel();
		else if (levelOfSegmentation == IGenericDataSource.PAR_LEVEL)
			this.InitParLevel();
		else
		{
			Exception e = new Exception ("Exception in SimpleFileDataSource.Init(): Invalid value of basic units " + String.valueOf(levelOfSegmentation) );
			throw e;
		}
		
		
		
		this.referenceBreaks = LoadHierarchicalBreaks(this.annotFile);
		
	}
	
	
	/**
	 * returns the name of the input file for this data source without any extensions. E.g. for somefile.txt -> somefile
	 * @return
	 */
	public String GetShortName()
	{
		String name = this.GetName();
		Matcher m1 = namePattern.matcher(name);
		if (m1.find())
		{
			name = m1.group(0);
			//System.out.println("new name " + newName);
		}
		return name;
	}
	
	/**
	 * 
	 * @param annotDir is the directory containing all annotations for this chapter. The directory must be named 'chN' where N is the chapter number
	 * annotation files in the directory must be named chN-annotN.csv
	 * @throws Exception
	 */
	public static TreeMap <Integer, TreeMap<Integer, ArrayList<Integer>>> LoadHierarchicalBreaks(File annotDir) throws Exception
	{
		String[] fileExt = {"csv"};
		
		TreeMap <Integer, TreeMap<Integer, ArrayList<Integer>>> refBreaks = new TreeMap <Integer, TreeMap<Integer, ArrayList<Integer>>>();
		
		File[] annotFiles = annotDir.listFiles( new SegFileFilter( fileExt ));
		Pattern annotIdPattern = Pattern.compile("an([\\d]+)");
		
		for (File aFile: annotFiles)
		{
			TreeMap <Integer, ArrayList<Integer>> levelsToBreaks = new TreeMap <Integer, ArrayList<Integer>> ();
			
			Matcher m = annotIdPattern.matcher(aFile.getName());
			Integer annotId = null;
			
			if (m.find() == true)
			{
				String annotIdStr = m.group(1);
				annotId = new Integer(annotIdStr);
			}
			else
			{
				throw (new Exception("invalid annotation file name, cannot find annotator id: " + aFile.getName()));
			}
			
			String body = TextFileIO.ReadTextFile(aFile);
			String[] lines = body.split("[\\n\\r]");
			
			
			//at first we see how many level are there in this annotation
			int[] numSegmPerLevel = {0,0,0,0,0,0};
			//first line is the header
			for (int i = 1; i < lines.length; i++)
			{
				//first column is the paragraph ids
				String[] fields = lines[i].split(",");
				for (int j = 1; j < fields.length; j++)
				{
					int curLevel = j-1;
					Integer curPosition = new Integer(fields[j]);
					if (curPosition > 0)
						numSegmPerLevel[curLevel]++;
				}
			}
			
			int numLevels = 1;
			for (int i = 0; i < numSegmPerLevel.length; i++)
			{
				if (numSegmPerLevel[i] <= 1)
				{
					numLevels = i;
					break;
				}
			}
			
			//now we actually load the annotations
			for (int i = 1; i < lines.length; i++)
			{
				//first column is the paragraph ids
				String[] fields = lines[i].split(",");
				for (int j = 1; j < fields.length; j++)
				{
					if ( j > numLevels)
						break;
					
					Integer evalHDSLevel = new Integer(j + 1);
					
					String curField = fields[j];
					Integer curBreak = new Integer(curField);
					
					if (curBreak > 0)
					{
						ArrayList<Integer> breaks;
						if (levelsToBreaks.keySet().contains(evalHDSLevel) == false)
						{
							breaks = new ArrayList<Integer>();
						}
						else
							breaks = levelsToBreaks.get(evalHDSLevel);
						
						breaks.add(new Integer(i - 1));
						levelsToBreaks.put(evalHDSLevel, breaks);
					}
				}
		
			}
			refBreaks.put (annotId, levelsToBreaks);
			
		}
		
		return refBreaks;
		
		
	}
	
	public void ReconcileBreaks()
	{
		for (Integer annot: this.GetAnnotatorIds())
		{
			TreeMap<Integer, ArrayList<Integer>> anotBreaks = referenceBreaks.get(annot);
			ArrayList<Integer> levels = new ArrayList<Integer>();
			
			for (Integer level : anotBreaks.keySet())
			{
				levels.add(level);
			}
			
			for (int i = 0; i < levels.size() - 1; i++) {
				for (Integer br : anotBreaks.get(levels.get(i))) {
					insertBreakInLevel(annot, levels.get(i + 1), br);
				}
			}
		}
		
		
	}
	
	private void insertBreakInLevel(Integer annotator, Integer level, Integer br)
	{
		ArrayList<Integer> breaks = referenceBreaks.get(annotator).get(level);
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
	 * a method to transform a hierarchy of breaks specified by the segmenter into a Carrol-style breaks 
	 * That is top level's index is 2. However, we do not remove closing breaks here.
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
	
	/**
	 * outputs the text with reference breaks that were loaded from gold standard, if any
	 * @param outputFile name of the directory where to output the files corresponding to each available annotation
	 * @throws Exception
	 */
	public void OutputFullTextHierarchical(File outputDir) throws Exception
	{
		if (outputDir.exists() == false)
		{
			outputDir.mkdir();
		}
		
		for (Integer annotId: this.referenceBreaks.keySet())
		{
			TreeMap<Integer, ArrayList<Integer>> curAnnotation = this.referenceBreaks.get(annotId);
			
			
			String name = this.GetShortName() + "_an" + annotId.toString() + ".txt";
			File curOutFile = new File(outputDir,name );
			this.OutputHierWithHypoBreaks(curOutFile, curAnnotation);
			


//			Set<Integer> levels = curAnnotation.keySet(); // ascending
//			
//			
//			
//			StringBuilder str = new StringBuilder();
//			for (int i = 0; i < this.GetNumberOfChunks(); i++)
//			{
//				String curChunk = this.GetChunk(i);
//				str.append(i + " " + curChunk + "\n");
//				
//				for (Integer curLevel: levels)
//				{
//					ArrayList<Integer> curBreaks = curAnnotation.get(curLevel);
//					
//					if (curBreaks.contains(new Integer(i)))
//					{
//						str.append("==== " + curLevel + " ====" + "\n");
//						break;
//					}
//				}
//			}
//			TextFileIO.OutputFile(curOutFile, str.toString());
			
			
		}
		
	}
	
	/**
	 * This method  takes a TreeMap<Integer, ArrayList<Integer>>, usually the output of HierarchicalAffinityPropagation.GetAllHypoBreaks
	 * and prints out a file that can be use to run EvalHDS
	 * 
	 * It outputs break in a style consistent with Lucien Carrol's annotations: the closing break of a segment is not explicitely specified
	 */
	public void OutputHierWithHypoBreaks(File outputFile, TreeMap<Integer, ArrayList<Integer>> assigns) throws Exception
	{
		Set<Integer> levels = assigns.keySet();
		
		TreeSet<Integer> breaksAlreadyIncluded = new TreeSet<Integer>();
		
		
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < this.GetNumberOfChunks(); i++)
		{
			String curChunk = this.GetChunk(i);
			str.append(i + " " + curChunk + "\n");
			
			for (Integer curLevel: levels)
			{
				if (curLevel > 3)
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
		
		
//		Set<Integer> levels = assigns.descendingKeySet();
//		
//		
//		
//		StringBuilder str = new StringBuilder();
//		for (int i = 0; i < this.GetNumberOfChunks(); i++)
//		{
//			String curChunk = this.GetChunk(i);
//			str.append(i + " " + curChunk + "\n");
//			
//			for (Integer curLevel: levels)
//			{
//				ArrayList<Integer> curBreaks = assigns.get(curLevel);
//				
//				Integer evalHDSLevel = 2 + (levels.size()-1 - curLevel);//in EvalHDS all levels start at 2, where 2 is the topmost level
//				
//				if (curBreaks.contains(new Integer(i)))
//				{
//					str.append("==== " + evalHDSLevel.toString() + " ====" + "\n");
//					break;
//				}
//			}
//		}
//		TextFileIO.OutputFile(outputFile, str.toString());
	}
	
	@Override
	public int GetNumberOfSegmentsAtLevel(int annotId, int levelId)
			throws Exception {
		
		if (this.referenceBreaks.keySet().contains(annotId) == false)
		{
			Exception e = new Exception ("Invalid annotatorId: " + annotId);
			throw (e);
		}
		
		TreeMap<Integer, ArrayList<Integer>> breaks = this.referenceBreaks.get(annotId);
		
		if ( breaks.keySet().contains(levelId) == false)
		{
			Exception e = new Exception ("invalid level Id (" + levelId + ") for annotator " + annotId);
			throw (e);
		}
		
		return breaks.get(levelId).size();
	}
	
	public int GetNumLevelsForAnnotator(int annotId) throws Exception
	{
		if (this.referenceBreaks.keySet().contains(annotId) == false)
		{
			Exception e = new Exception ("Invalid annotatorId: " + annotId);
			throw (e);
		}
		
		TreeMap<Integer, ArrayList<Integer>> breaks = this.referenceBreaks.get(annotId);
		return breaks.size();
		
	}
	

	@Override
	public ArrayList<Integer> GetReferenceBreaksAtLevel(int annotId, int levelId) throws Exception
	{
		if (this.referenceBreaks.keySet().contains(annotId) == false)
		{
			Exception e = new Exception ("Invalid annotatorId: " + annotId);
			throw (e);
		}
		
		TreeMap<Integer, ArrayList<Integer>> breaks = this.referenceBreaks.get(annotId);
		
		if ( breaks.keySet().contains(levelId) == false)
		{
			Exception e = new Exception ("invalid level Id (" + levelId + ") for annotator " + annotId);
			throw (e);
		}
		
		return breaks.get(levelId);
	}

	@Override
	public void SetReferenceBreaksAtLevel(int annotId,
			ArrayList<Integer> breaks, int levelId) throws Exception {
		Exception e = new Exception ("Not implemented in HierarchicalFileMultipleAnnotsDataSource");
		throw (e);
		
	}

	/**
	 * Returns the average number of segments per level across all annotators
	 */
	@Override
	public Double GetAveNumberOfSegmentsAtLevel(int levelId) throws Exception {
		
		Double ave = new Double(0);
		int numAnnot = this.referenceBreaks.size();
		
		for (Integer annotId: this.referenceBreaks.keySet())
		{
			TreeMap<Integer, ArrayList<Integer>> breaks = this.referenceBreaks.get(annotId);
			if (breaks.containsKey(levelId) == false)
				continue;
			int numSegm = breaks.get(levelId).size();
			ave += numSegm;
			
		}
		
		return (ave / numAnnot );
		
	}

	
	/**
	 * returns average segment length at a given level across all annotations
	 */
	@Override
	public Double GetAverageSegmentLengthAtLevel(int levelId) throws Exception {
		
		
		Double numChunks = new Double (this.GetNumberOfChunks());
		Double aveNumSegm = this.GetAveNumberOfSegmentsAtLevel(levelId);
		
		Double aveLength = numChunks / aveNumSegm;
		
		return aveLength;
		
	}
	
	@Override
	public int GetNumAnnotators()
	{
		return this.referenceBreaks.size();
	}
	
	/**
	 * returns the ids of all annotators available in the gold standard
	 * @return
	 */
	public Set<Integer> GetAnnotatorIds()
	{
		return this.referenceBreaks.keySet();
	}
	
	public String PrintRefBreaks(int annotId) throws Exception
	{
		if (this.referenceBreaks.containsKey(annotId) == false)
		{
			throw (new Exception("Invalid annotator id " + annotId));
		}
		return this.referenceBreaks.get(annotId).toString();
	}

	public TreeMap <Integer, TreeMap<Integer, ArrayList<Integer>>> GetReferenceBreaks()
	{
		return this.referenceBreaks;
	}
	

}
