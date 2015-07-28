package datasources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class to load and manipulate multiple reference segmentations for the same source document.
 * 
 * An important note: chunk indices (that is sentence or paragraph indices are 0-based.
 * Annotator ids are 1-based. 
 * 
 * 
 * @author anna
 *
 */
public class SimpleFileMultipleAnnotsDataSource implements
		IDataSourceMultipleAnnots, IGenericDataSource, Cloneable {
	
	protected ArrayList<String> chunks = new ArrayList<String>();
	protected TreeMap<Integer, ArrayList<Integer>> refBreaks = new TreeMap<Integer, ArrayList<Integer>>();
	protected File inputFile;
	protected File annotFile;
	protected int levelOfSegm = 0;
	
	protected int numAnnotators = -1;
	protected boolean isHierarchical = false;
	
	@Override
	/**
	 * This method creates a deep copy of refBreaks.
	 * Everything else is shared.
	 */
	public Object clone() throws CloneNotSupportedException {
		SimpleFileMultipleAnnotsDataSource cloned = (SimpleFileMultipleAnnotsDataSource) super.clone();
		cloned.refBreaks = new TreeMap<Integer, ArrayList<Integer>>();
		for (Integer annotId: this.refBreaks.keySet())
		{
			ArrayList<Integer> copiedBreaks = new ArrayList<Integer>( this.refBreaks.get(annotId) );
			try {
				cloned.SetReferenceBreaks(annotId.intValue(), copiedBreaks);
			} 
			catch (Exception e) {
				e.printStackTrace();
				CloneNotSupportedException e2 = new CloneNotSupportedException(e.getMessage());
				throw (e2);
			}
		}
		
		return cloned;
		
	}
	
	public void PrintChunks()
	{
		for (int i = 0;  i < this.GetNumChunks(); i++)
		{
			
			System.out.println(String.valueOf(i) + "\t" + this.GetChunk(i));
			
			
		}
	}
	
	public SimpleFileMultipleAnnotsDataSource()
	{}
	
	
	/**
	 * This constructor is obsolete. It is only here to comply with also obsolete IDateSourceMultipleFiles.
	 * 
	 * Use the no no argument constructor and 
	 * Init((int levelOfSegmentation, String segmPattern,
			File textFile, File[] annotationFiles)
	 * 
	 * @param textFile
	 * @param annotationsFile
	 */
	public SimpleFileMultipleAnnotsDataSource(File textFile, File annotationsFile)
	{
	
		this.inputFile = textFile;
		this.annotFile = annotationsFile;
		
	}
	
	
	/**
	 * This method is obsolete.
	 */
	public void Init(int basicUnits) throws Exception
	{
		if (basicUnits != IGenericDataSource.SENT_LEVEL && basicUnits != IGenericDataSource.PAR_LEVEL)
		{
			Exception e = new Exception ("Exception in SimpleFileMultipleAnnotsDataSource.Init: invalid level of segmentation specified: " + String.valueOf(basicUnits));
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
		
		LoadReferenceSegmentations();
		for (Integer annot: this.refBreaks.keySet())
		{
			this.TransformBreaks(annot);
		}
	}
	
	/**
	 * @param levelOfSegmentation IGenericDataSource.SENT_LEVEL or IGenericDataSource.PAR_LEVEL
	 * @param segmPattern is redundant, since we load the break positions from annotationsFile
	 * @param textFile
	 * @param annotationFiles 
	 */
	public void Init(int levelOfSegmentation, String segmPattern,
			File textFile, File annotationsFile) throws Exception {
		
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
		
		LoadReferenceSegmentations();
		for (Integer annot: this.refBreaks.keySet())
		{
			this.TransformBreaks(annot);
		}
		
	}
	
	
	/**
	 * Initialize this datasource from text, not from a file
	 * 
	 */
	public void Init(int levelOfSegmentation, String segmPattern, String text,
			File annotationsFile) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private void LoadReferenceSegmentations() throws Exception
	{
		String body = TextFileIO.ReadTextFile(this.annotFile);
		String[] lines = body.split("[\n\r]+");
			//TextFileIO.LinesToArray(body);
		int numAnnotators = -1;
		
		//the csv files are formatted like this
		/*par id | annotator id | annotator id ...
		 * the annotator ids start at 1, not 0
		 * 
		 * */
		
		for (int i = 0; i < lines.length; i++)
		{
			String curLine = lines[i];
			if (i == 0)
			{
				String[] fields = curLine.split("\\,");
				numAnnotators = fields.length - 1;
				
				for (int j = 1; j <= numAnnotators; j++)
				{
					Integer curAnnot = new Integer(j);
					this.refBreaks.put(curAnnot, new ArrayList<Integer>());
				}
				continue;
			}
			String[] fields = curLine.split(",");
			for (int f = 1; f < fields.length; f++)
			{
				Integer curBreak = new Integer(fields[f]);
				Integer annotId = new Integer(f);
				this.refBreaks.get(annotId).add(curBreak);
			}
			
		}
		
		this.numAnnotators = this.refBreaks.keySet().size();
			
	}
	
	//changes the format from what was in csv files to a list of paragraphs after which there are breaks
	
	
	// paragraphs are indexed from 0, not from 1
	private void TransformBreaks(Integer annotId)
	{
		ArrayList<Integer> oldBreaks = this.refBreaks.get(annotId);
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
		this.refBreaks.put(annotId, newBreaks);
	}
	
	public void PrintReferences( boolean ifDetailed)
	{
		for (Integer annot: this.refBreaks.keySet())
		{
			System.out.println("Annotator " + annot.toString() + "\n\t");
			ArrayList<Integer> breaks = this.refBreaks.get(annot);
			for (Integer curBreak : breaks)
			{
				System.out.print(curBreak.toString() + ", ");
			}
			System.out.println();
		}
	}
	
	protected void InitSentLevel()
	{
		try
		{
			
			BufferedReader reader = new BufferedReader(new FileReader(this.inputFile));
			String line;
			while ((line = reader.readLine()) != null)
			{
				
				this.chunks.add(line);
			
			}
			
			
		}
		catch (Exception e)
		{
			System.out.println("Exception in SimpleFileMultipleAnnotsDataSource.InitSentLevel(): " + e.getMessage());
		}
	}
	
	protected void InitParLevel()
	{
		//was the last line a paragraph break?
		boolean parEnd = false;
		
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(this.inputFile));
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
				
//				Matcher m = this.segmPattern.matcher(line);
//				if (m.matches())
//					this.refBreaks.add( this.chunks.size() - 1 );
//				else
				
				curPar.append(" " + line);
			
			}
			
			//include the last paragraph
			if (curPar.toString().isEmpty() == false )
				this.chunks.add(curPar.toString());
		}
		catch (Exception e)
		{
			System.out.println("Exception in SimpleFileDataSource.InitParLevel(): " + e.getMessage());
		}
	}
	
	

	@Override
	public String GetChunk(int chunkIndex) {
		// TODO Auto-generated method stub
		return this.chunks.get(chunkIndex);
	}

	@Override
	public String GetName() {
		if (this.inputFile != null)
			return this.inputFile.getName();
		else
		{
			return "text-initialized SimpleFileMultipleAnnotsDataSource.";
		}
	}

	/**
	 * obsolte method from IDataSourceMultipleAnnots.
	 * use IGenericDataSource.GetNumberOfChunks()
	 */
	public int GetNumChunks() {
		return this.chunks.size();
	}
	
	/**
	 * @return total number of basic units in the underlying document (sentences or paragraphs)
	 */
	public int GetNumberOfChunks() {
		return this.chunks.size();
	}

	/**
	 * @return TreeMap<Integer, ArrayList<Integer>> refBreaks mapping of annotatorId to ArrayList<Integer> of reference breaks
	 */
	public TreeMap<Integer, ArrayList<Integer>> GetReferenceSegmentBreaks() {
		return this.refBreaks;
	}
	
	/**
	 * @return returns average segment length across all available annotations
	 */
	public Double GetAverageSegmentLength() throws Exception{
		Integer sum = this.chunks.size() * this.GetNumberOfAnnotators();
		Integer totalBreaks = 0;
		Double ave = new Double(0);
		try{
			for (Integer annot: this.refBreaks.keySet())
			{
				totalBreaks += this.GetNumberOfSegmentsForAnnot(annot);
			}
			ave = new Double ((double) sum / (double) totalBreaks);
		}
		catch (Exception e)
		{
			System.out.println("Strange Exception in SimpleFileMultipleAnnotsDataSource.GetAverageSegmentLength: ");
			System.out.println( e.getMessage() );
			System.out.println(e.getStackTrace());
			throw (e);
		}
		finally 
		{
			return ave;
		}
	}

	/**
	 * @param 1-based index of the annotator id
	 * @return average segment length for a specific annotator 
	 */
	public Double GetAverageSegmentLengthForAnnot(int annotId) throws Exception {
		
		return new Double ((double)this.chunks.size() / (double)this.GetNumberOfSegments(annotId));
	}
	
	/**
	 * returns average number of segments across all available annotators
	 */
	public Double GetAveNumberOfSegments() throws Exception {
		
		Integer totalBreaks = 0;
		
		for (Integer annot: this.refBreaks.keySet())
		{
			totalBreaks += this.refBreaks.get(annot).size();
		}
		return new Double ((double) totalBreaks/ (double) this.GetNumberOfAnnotators());
	}

	/**
	 * @param 1-based index of the annotator id
	 * @return number of segments for a specific annotator 
	 */
	public Integer GetNumberOfSegmentsForAnnot(int annotId) throws Exception {
		return this.refBreaks.get(new Integer(annotId)).size();
	}
	
	/**
	 * @param annotId 1-based annotator ID
	 * @return number of segments indetified by a particular annotator
	 */
	public int GetNumberOfSegments(int annotId) {
		
		return this.refBreaks.get(new Integer(annotId)).size();
	}

	
	/**
	 * This method is obsolete. It is only here to preserve compatibility with IDataSourceMultipleAnnots.
	 * Use Double GetAveNumberOfSegments() instead.
	 * @see Double GetAveNumberOfSegments()
	 */
	public int GetAverageNumberOfSegments()
	{
		int totalNumSegm = 0;
		int numAnnot = this.refBreaks.keySet().size();
		
		for (Integer annot: this.refBreaks.keySet())
		{
			ArrayList<Integer> br = this.refBreaks.get(annot);
			int size = br.size();
			totalNumSegm += size;
		}
		
		return (int) (Math.round( (double)totalNumSegm / (double)numAnnot ));
	}
	
	/**
	 * This method is obsolete. It is only here to preserve compatibility with IDataSourceMultipleAnnots.
	 * Use Double Double GetAverageSegmentLength() instead.
	 * @see Double Double GetAverageSegmentLength()
	 */
	public int GetAverageSegmLength()
	{
		int totalNumSegm = 0;
		int numAnnot = this.refBreaks.keySet().size();
		int aveSegmLength = 0;
		
		for (Integer annot: this.refBreaks.keySet())
		{
			ArrayList<Integer> br = this.refBreaks.get(annot);
			int size = br.size();
			totalNumSegm += size;
		}
		
		aveSegmLength = (int) ( (double) (this.GetNumChunks() * numAnnot) / (double) totalNumSegm );
		return aveSegmLength;
		
	}
	
	public int GetNumAnnotators()
	{
		return this.numAnnotators;
	}

	

	/**
	 * obsolte method from IDataSourceMultipleAnnots.
	 */
	public void Output(File outputFile, Integer[] breaks, int annotatorId) throws Exception{
		if (annotatorId > 0)
			return;
		ArrayList<Integer> br = new ArrayList<Integer>();
		
		for (Integer b : breaks)
		{
			br.add(b);
		}
		
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < this.GetNumChunks(); i++)
		{
			str.append(this.GetChunk(i) + "\n");
			if  (br.contains(new Integer (i) ))
			{
				str.append("==========\n");
			}
		}
		
		TextFileIO.OutputFile(outputFile, str.toString());

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
		int cutOff = (int)Math.floor( new Double(numAnnot) * new Double(0.25) );//numAnnot;
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
	
	/**
	 * output text delineated by specified segment breaks
	 */
	public void OutputFullText(File outputFile, ArrayList<Integer> breaks)
			throws Exception {
		
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < this.GetNumChunks(); i++)
		{
			str.append(this.GetChunk(i) + "\n");
			if  (breaks.contains(new Integer (i) ))
			{
				str.append("==========\n");
			}
		}
		
		TextFileIO.OutputFile(outputFile, str.toString());
	}

	/**
	 * outputs only the set of specified breaks
	 */
	public void OutputBreaksOnly(File outputFile, ArrayList<Integer> breaks)
			throws Exception {
		StringBuilder str = new StringBuilder();
		for (Integer br: breaks)
		{
			str.append(br + ",");
		}
		TextFileIO.OutputFile(outputFile, str.toString());
	}


	

	
	/**
	 * @return the set of breaks specified by a given annotator
	 */
	public ArrayList<Integer> GetReferenceBreaks(int annotId) {
		return this.refBreaks.get(new Integer(annotId));
	}

	/**
	 * sets breaks for a specific annotator
	 */
	public void SetReferenceBreaks(int annotId, ArrayList<Integer> breaks)
			throws Exception {
//		if (this.refBreaks.keySet().contains(new Integer (annotId)))
//		{
//			this.refBreaks.put(new Integer (annotId), breaks);
//		}
		try{
			this.refBreaks.put(new Integer (annotId), breaks);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw (new Exception ("Exception in SimpleFileMultipleAnnotsDataSource.SetReferenceBreaks: " + e.getMessage()));
		}
	}
	
	

	/**
	 * @param numBreaks desired number of breaks
	 * @return a set of randomly generated breaks fro this data source
	 */
	public ArrayList<Integer> GenerateRandomReferenceBreaks(int numBreaks) {
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
		
		return newBr;
	}

	

	/**
	 * @return number of available annotations
	 */
	public int GetNumberOfAnnotators() {
		return this.refBreaks.keySet().size();
	}

	/**
	 * A method to create light-weight instances, mostly to hold results of segmentations
	 * @param numChunks number of chunks in the source document
	 */
	public void LightWeightInit(  int numChunks) {
		
		this.refBreaks = new TreeMap<Integer, ArrayList<Integer>>();
		
		for (int i = 0; i < numChunks; i++)
		{
			this.chunks.add("segm" + String.valueOf(i));
		}	
	}

	@Override
	public int GetLevelOfSegm() {
		return this.levelOfSegm;
	}

	@Override
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

	@Override
	public int GetNumberOfSegmentsAtLevel(int annotId, int levelId)
			throws Exception {
		
		Exception e = new Exception ("Not implemented in SimpleFileMultipleAnnotsDataSource");
		throw (e);
		
		//return 0;
	}

	@Override
	public ArrayList<Integer> GetReferenceBreaksAtLevel(int annotId, int levelId)
			throws Exception {
		Exception e = new Exception ("Not implemented in SimpleFileMultipleAnnotsDataSource");
		throw (e);
	}

	@Override
	public void SetReferenceBreaksAtLevel(int annotId,
			ArrayList<Integer> breaks, int levelId) throws Exception {
		Exception e = new Exception ("Not implemented in SimpleFileMultipleAnnotsDataSource");
		throw (e);
		
	}

	@Override
	public Double GetAveNumberOfSegmentsAtLevel(int levelId) throws Exception {
		Exception e = new Exception ("Not implemented in SimpleFileMultipleAnnotsDataSource");
		throw (e);
	}

	@Override
	public Double GetAverageSegmentLengthAtLevel(int levelId) throws Exception {
		Exception e = new Exception ("Not implemented in SimpleFileMultipleAnnotsDataSource");
		throw (e);
	}

	

	

	

}
