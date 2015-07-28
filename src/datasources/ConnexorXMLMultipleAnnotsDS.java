package datasources;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnexorXMLMultipleAnnotsDS extends ConnexorXMLDataSource {
	
	@Override
	public void Init(int levelOfSegm, String segmPattern,
			File textFile, File annotationsFile) throws Exception 
			
	{
		super.Init(levelOfSegm, segmPattern, textFile, annotationsFile);
		
		if (this.levelOfSegmentation == IGenericDataSource.SENT_LEVEL)
		{}
		else if (this.levelOfSegmentation == IGenericDataSource.PAR_LEVEL)
		{}
		else
		{
			String msg = "Invalid segmentation level in ConnexorXMLMultipleAnnotsDS.Init: " + String.valueOf(this.levelOfSegmentation);
			throw (new Exception (msg));
		}
		LoadReferenceSegmentations();
		
	}
	
	/**
	 * Loads reference annotations from this.annotsFile.The file must be a .csv file with proper extension
	 * The format of annotsFile is like this:
	 * paragraph,an1,an2,..,ann
	 * 1,an1_assignment,an2_assignment,...,ann_assignment
	 * .
	 * .
	 * .
	 *last_paragraph,an1_assignment,an2_assignment,...,ann_assignment
	 * @throws Exception
	 */
	public void LoadReferenceSegmentations () throws Exception
	{
		String body = "";
		try{
			body = TextFileIO.ReadTextFile(this.annotsFile);
			if (body == null || body.isEmpty())
			{
				String msg = "Exception in ConnexorXMLMultipleAnnotsDS.LoadReferenceSegmentations: cannot open file: " + this.annotsFile;
				System.out.println(msg);
				throw (new Exception (msg));
			}
		}
		catch (Exception e)
		{
			System.out.println("Exception in ConnexorXMLMultipleAnnotsDS.LoadReferenceSegmentations: cannot open file: " + this.annotsFile);
			e.printStackTrace();
			throw (new Exception(e));
		}
		String[] lines = body.split("[\n\r]+");
			//TextFileIO.LinesToArray(body);
		int numAnnotators = -1;
		
		TreeMap<Integer,ArrayList<Integer>> tempBreaks = new TreeMap<Integer,ArrayList<Integer>>();
		
		for (int i = 0; i < lines.length; i++)
		{
			String curLine = lines[i];
			//process the headline
			if (i == 0)
			{
				String[] fields = curLine.split("\\,");
				numAnnotators = fields.length - 1;
				
				for (int j = 1; j <= numAnnotators; j++)
				{
					Integer curAnnot = new Integer(j);
					tempBreaks.put(curAnnot, new ArrayList<Integer>());
					//this.segmentBreaks.put(curAnnot,  new ArrayList<Integer>());
				}
				continue;
			}
			String[] fields = curLine.split(",");
			for (int f = 1; f < fields.length; f++)
			{
				Integer curBreak = new Integer(fields[f]);
				Integer annotId = new Integer(f);
				tempBreaks.get(annotId).add(curBreak);
			}	
		}
		
		//now transform the breaks
		
		for (Integer curAnnot: tempBreaks.keySet())
		{
			ArrayList<Integer> oldBreaks = tempBreaks.get(curAnnot);
			ArrayList<Integer> newBreaks = new ArrayList<Integer>();
			
			Integer prevSegment = 1;
			for (int i = 0; i < oldBreaks.size(); i++)
			{
				Integer curSegment = oldBreaks.get(i);
				
	;			if (curSegment > prevSegment)
				{
					newBreaks.add(new Integer(i - 1));
					prevSegment = curSegment;
				}
			}
			
			newBreaks.add(new Integer(oldBreaks.size() - 1));
			this.segmentBreaks.put(curAnnot, newBreaks);
			
			int numChunks = this.GetNumberOfChunks();
			if (numChunks > oldBreaks.size())
			{
				String msg = "Exception in ConnexorXMLMultipleAnnotsDS.LoadReferenceSegmentations: " +
			" Not all chunks are covered by the annotations file. There are " + String.valueOf(oldBreaks.size() + 
					" chunks annotated out of a total of " + String.valueOf(numChunks));
				throw (new Exception (msg));
			}
			
		}
		
	}
	
	public void ReloadAnnotations() throws Exception
	{
		String body = "";
		try{
			body = TextFileIO.ReadTextFile(this.annotsFile);
			if (body == null || body.isEmpty())
			{
				String msg = "Exception in ConnexorXMLMultipleAnnotsDS.ReloadAnnotations: cannot open file: " + this.annotsFile;
				System.out.println(msg);
				throw (new Exception (msg));
			}
		}
		catch (Exception e)
		{
			System.out.println("Exception in ConnexorXMLMultipleAnnotsDS.ReloadAnnotations: cannot open file: " + this.annotsFile);
			e.printStackTrace();
			throw (new Exception(e));
		}
		String[] lines = body.split("[\n\r]+");
		
		
		//determine dimensions
		String curLine =lines[0];
		String[] fields = curLine.split(",");
		int numAnnotators = fields.length - 1;
		
		int numPars = lines.length - 1;
		
		/**
		 * we want to create a temporary table looking like the input file minus the heading
		 */
		int tempBreaks[][] = new int[numPars][numAnnotators + 1];
		
		for (int i = 0; i < lines.length; i++)
		{
			curLine =lines[i];
			
			//skip the header
			if (i == 0)
				continue;
			
			
			fields = curLine.split(",");
			Integer curParIndex = new Integer(fields[0]);
			tempBreaks[i-1][0] = curParIndex;
			
			for (int f = 1; f < fields.length; f++)
			{
				Integer curSegmAssignment = new Integer(fields[f]);
				Integer annotId = new Integer(f);
				
				tempBreaks[i-1][annotId] = curSegmAssignment;
			}	
		}
		
		//now let's transform the breaks. The parId to annotId maps must be filled in
		for (int annotId = 1; annotId <= numAnnotators; annotId++)
		{
			ArrayList<Integer> newBreaks = new ArrayList<Integer>();
			int prevSegmAssignment = 1;
			
			for (int row = 0; row < lines.length-1; row++)
			{
				
				int curSegmAssignment = tempBreaks[row][annotId];
				if (curSegmAssignment > prevSegmAssignment)
				{
					if (row == 0)
					{
						String msg = "Exception in ConnexorXMLMultipleAnnotsDS.ReloadAnnotations: no segment breaks allowed before first chunk. Error in annotation " + String.valueOf(annotId);
						throw (new Exception (msg));
					}
					int prevParIndex = tempBreaks[row-1][0];
					newBreaks.add(prevParIndex);	
				} 
				//by convention add a break after the last chunk
				if (row == lines.length - 2)
				{
					int parIndex = tempBreaks[row][0];
					newBreaks.add(parIndex);
				}
				prevSegmAssignment = curSegmAssignment;
			}//end rows
			this.segmentBreaks.put(annotId, newBreaks);
		}//end annotId
	}
	
	/**
	 * this method makes sure that the breaks specified in the annotations (annotationIds) are correctly
	 * placed with respect to paragraph ids as they appear in this document
	 */
	public void RemapLoadedSegmentBreaks() throws Exception
	{
		if (this.GetAnnotIdsRemapped())
			return;
		for (Integer annotId: this.segmentBreaks.keySet())
		{
			ArrayList<Integer> newAnnotBreaks= new ArrayList<Integer>();
			ArrayList<Integer> curBreaks = this.GetReferenceBreaks(annotId);
			
			for (Integer annotParId: curBreaks)
			{
				Integer trueId = this.GetParIdForAnnotId(annotParId);
				newAnnotBreaks.add(trueId);
			}
			this.segmentBreaks.put(annotId, newAnnotBreaks);
		}
		this.SetAnnotIdsRemapped(true);
	}
	
	/**
	 * This method verifies the integrity of all the loaded annotations.
	 * @return
	 */
	public boolean CheckReferenceAnnotations()
	{
		int numChunks = this.GetNumberOfChunks();
		 
		for (Integer annot: this.segmentBreaks.keySet())
		{
			try{
				ArrayList<Integer> curBreaks = this.GetReferenceBreaks(annot);
				for (Integer brPosition: curBreaks)
				{
					if (brPosition < 0 || brPosition >= numChunks)
						{
							System.out.println("Invalid segemnt break - " + brPosition.toString() + " - in annot " + annot.toString());
							return false;
						}
				}
				
				//check that the last break position is the last chunk
				Integer lastBreak = curBreaks.get(curBreaks.size() - 1);
				if (lastBreak < numChunks - 1)
				{
					System.out.println("Not all chunks are covered by annotation " + annot.toString() + ". Last position is " + lastBreak.toString() 
							+ " for " + String.valueOf(numChunks) + " chunks");
					return false;
				}
				return true;
			}
			catch (Exception e)
			{
				return false;
			}
		}
		return true;
		
	}

	

	@Override
	public int GetNumberOfAnnotators() {
		return this.segmentBreaks.size();
	}

	@Override
	public int GetNumberOfSegments(int annotId) {
		try{
			ArrayList<Integer> curBreaks = this.segmentBreaks.get(annotId);
			return (curBreaks.size());
		}
		catch (Exception e)
		{
			System.out.println("Exception in ConnexorXMLMultipleAnnotsDS.GetNumberOfSegments: " + e.getMessage());
			e.printStackTrace();
			return (-1);
		}
	}

	@Override
	public ArrayList<Integer> GetReferenceBreaks(int annotId) {
		try{
			ArrayList<Integer> curBreaks = this.segmentBreaks.get(annotId);
			return (curBreaks);
		}
		catch (Exception e)
		{
			System.out.println("Exception in ConnexorXMLMultipleAnnotsDS.GetReferenceBreaks: " + e.getMessage());
			e.printStackTrace();
			return (null);
		}
	}

	@Override
	public void SetReferenceBreaks(int annotId, ArrayList<Integer> breaks)
			throws Exception {
		this.segmentBreaks.put(annotId, breaks);

	}

	@Override
	public Double GetAveNumberOfSegments() throws Exception {
		Double ave = new Double(0);
		for (Integer annotId: this.segmentBreaks.keySet())
		{
			Integer numSegm = this.segmentBreaks.get(annotId).size();
			ave += numSegm;
		}
		ave = ave / this.segmentBreaks.keySet().size();
		return (ave);
	}

	@Override
	public Double GetAverageSegmentLength() throws Exception {
		Double netLength = new Double (this.GetNumberOfChunks() * this.GetNumberOfAnnotators());
		Integer netNumSegm = 0;
		
		for  (Integer annotId: this.segmentBreaks.keySet())
		{
			ArrayList<Integer> curBreaks = this.segmentBreaks.get(annotId);
			netNumSegm += curBreaks.size();
		}
		
		Double ave = netLength / netNumSegm;
		return (ave);
		
	}

	@Override
	public Double GetAverageSegmentLengthForAnnot(int annotId) throws Exception {
		try{
			Double aveSegmLength = new Double(0);
			ArrayList<Integer> breaks = this.segmentBreaks.get(annotId);
			aveSegmLength = ( (new Double (this.GetNumberOfChunks())) / breaks.size());
			return aveSegmLength;
		}
		catch (Exception e)
		{
			String msg = "Exception in ConnexorXMLMultipleAnnotsDS.GetAverageSegmentLengthForAnnot " + e.getMessage();
			e.printStackTrace();
			throw (new Exception (msg));
		}
		
	}

	@Override
	public Integer GetNumberOfSegmentsForAnnot(int annotId) throws Exception {
		try{
			ArrayList<Integer> breaks = this.segmentBreaks.get(annotId);
			return (breaks.size());
		}
		catch (Exception e)
		{
			String msg = "Exception in ConnexorXMLMultipleAnnotsDS.GetNumberOfSegmentsForAnnot " + e.getMessage();
			e.printStackTrace();
			throw (new Exception (msg));
		}
	}

	@Override
	public void AddReferenceBreak(int annotId, int breakPosition)
			throws Exception {
		try{
			if (this.segmentBreaks.containsKey(annotId) == false)
			{
				ArrayList <Integer> breaks = new ArrayList<Integer>();
				breaks.add(breakPosition);
				this.segmentBreaks.put(annotId, breaks);
			}
			else
			{
				ArrayList<Integer> breaks = this.segmentBreaks.get(annotId);
				if (breaks.contains(breakPosition) == false)
					breaks.add(breakPosition);
			}
		}
		catch (Exception e){
			String msg = "Exception in ConnexorXMLMultipleAnnotsDS.AddReferenceBreak " + e.getMessage();
			e.printStackTrace();
			throw (new Exception (msg));
		}
		
	}
	
	/**
	 * given the name of the main input file and the  directory containing the annotations, the method returns a path to the .csv 
	 * file corresponding to the input file.
	 * e.g. GetCSVPath(input.xml, /some_dir) return File object for /some_dir/input.csv
	 * @param curInputName the name of the main input file 
	 * @param annotDir the  directory containing the annotations
	 * @return
	 */
	public static File GetCSVPath(String curInputName, File annotDir)
	{
		Pattern p = Pattern.compile("\\.[\\w\\.]+");
		Matcher m = p.matcher(curInputName);
		String csvName = m.replaceAll(".csv");
		return (new File(annotDir, csvName));
	}

	@Override
	public void LightWeightInit(int numChunks) {
		ArrayList<Integer> refBreaks = new ArrayList<Integer>();
		for (int i = 0; i < numChunks; i++)
		{
			if (this.GetLevelOfSegm() == IGenericDataSource.SENT_LEVEL)
			{
				SentenceTree tree = new SentenceTree(i);
				this.AddSentence(tree, 0);
			}
			else if (this.GetLevelOfSegm() == IGenericDataSource.PAR_LEVEL)
			{
				ParsedParagraph par = new ParsedParagraph(null, i, 0);
				this.AddParagraph(par);
			}
		}
		
	}
	
	/**
	 * Get majority opinion between all annotators.
	 * 
	 * The majority opinion contains all breaks specified by at least a half of all anotators.
	 * @return
	 */
	public ArrayList<Integer> GetMajorityOpinion()
	{
		ArrayList<Integer> major = new ArrayList<Integer>();
		
		int numAnnot = this.GetNumberOfAnnotators();
		int half = (int)Math.floor( (double)numAnnot / (double)3 );
		int numChunks = this.GetNumberOfChunks();
		
		for (int i = 0; i < this.GetNumberOfChunks(); i++)
		{
			Integer curPosition = new Integer (i);
			int numIncluded = 0;
			
			for (int a = 1; a <= this.GetNumberOfAnnotators(); a++)
			{
				ArrayList<Integer> curRef = this.GetReferenceBreaks(a);
						
				if (curRef.contains(curPosition))
				{	
					numIncluded++;
				}
			}
			
			if ( numIncluded >= half )
				
			{
				major.add(i);
			}
		}
		return major;
		
	}
	
	public ArrayList<Integer> GetUnionOpinion()
	{
		ArrayList<Integer> union = new ArrayList<Integer>();
		
		int numAnnot = this.GetNumberOfAnnotators();
		int half = (int)Math.floor( (double)numAnnot / (double)3 );
		int numChunks = this.GetNumberOfChunks();
		
		for (int i = 0; i < this.GetNumberOfChunks(); i++)
		{
			Integer curPosition = new Integer (i);
			int numIncluded = 0;
			
			for (int a = 1; a <= this.GetNumberOfAnnotators(); a++)
			{
				ArrayList<Integer> curRef = this.GetReferenceBreaks(a);
						
				if (curRef.contains(curPosition))
				{	
					union.add(i);
					break;
				}
			}
			
			
		}
		System.out.println("Union opinion " + union.toString());
		return union;
		
	}
	
	public ArrayList<Integer> GetIntersectionOpinion()
	{
		ArrayList<Integer> major = new ArrayList<Integer>();
		
		int numAnnot = this.GetNumberOfAnnotators();
		int cutOff = (int)Math.floor( new Double(numAnnot) * new Double(0.75) );//numAnnot;
		int numChunks = this.GetNumberOfChunks();
		
		for (int i = 0; i < this.GetNumberOfChunks(); i++)
		{
			Integer curPosition = new Integer (i);
			int numIncluded = 0;
			
			for (int a = 1; a <= this.GetNumberOfAnnotators(); a++)
			{
				ArrayList<Integer> curRef = this.GetReferenceBreaks(a);
						
				if (curRef.contains(curPosition))
				{	
					numIncluded++;
				}
			}
			
			if ( numIncluded >= cutOff )
				
			{
				major.add(i);
			}
		}
		return major;
		
	}

	@Override
	public void SetIfHierarchical(boolean isHier) {
		this.isHierarchical = isHier;
		
	}

	@Override
	public boolean GetIfHierarchical() {
		return this.isHierarchical;
	}

	@Override
	public int GetNumberOfSegmentsAtLevel(int annotId, int levelId)
			throws Exception {
		Exception e = new Exception ("Not implemented in ConnexorXMLMultipleAnnotsDS");
		throw (e);
	}

	@Override
	public ArrayList<Integer> GetReferenceBreaksAtLevel(int annotId, int levelId)
			throws Exception {
		Exception e = new Exception ("Not implemented in ConnexorXMLMultipleAnnotsDS");
		throw (e);
	}

	@Override
	public void SetReferenceBreaksAtLevel(int annotId,
			ArrayList<Integer> breaks, int levelId) throws Exception {
		Exception e = new Exception ("Not implemented in ConnexorXMLMultipleAnnotsDS");
		throw (e);
		
	}

	@Override
	public Double GetAveNumberOfSegmentsAtLevel(int levelId) throws Exception {
		Exception e = new Exception ("Not implemented in ConnexorXMLMultipleAnnotsDS");
		throw (e);
	}

	@Override
	public Double GetAverageSegmentLengthAtLevel(int levelId) throws Exception {
		Exception e = new Exception ("Not implemented in ConnexorXMLMultipleAnnotsDS");
		throw (e);
	}

}
