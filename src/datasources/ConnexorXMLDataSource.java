package datasources;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This class reads in XML files containing parsed text from Connexor Machinese parser
 * 
 * Connexor assigns its own ids to each sentence (which can be found in SentenceTree.id. However, the numbering is not
 * always exhaustive. That is why by default all sentences are indexed by their order of appearance in the document - corresponding to 
 * indices in the sentences ArrayList
 * 
 * 
 * @author anna
 *
 */
public abstract class ConnexorXMLDataSource implements IGenericDataSource {
	
	protected int levelOfSegmentation = 1; // default is sentences
	protected String segmPattern = "==========";
	protected File sourceFile = null;
	protected File annotsFile = null;
	
	protected ArrayList<SentenceTree> sentences = new ArrayList<SentenceTree>();
	protected TreeMap <Integer, Integer> indexToConnexorId = new TreeMap<Integer, Integer>();
	protected ArrayList<ParsedParagraph> paragraphs = new ArrayList<ParsedParagraph>();
	
	protected TreeMap <Integer, Integer> sentIndexToPar = new TreeMap<Integer, Integer>();
	protected boolean isHierarchical = false;
	
	/**
	 * the variable specifies if loaded reference segment breaks have been remapped to parIds as ordered in this document
	 */
	protected boolean annotIdsRemapped = false;
	
	public boolean GetAnnotIdsRemapped() {
		return annotIdsRemapped;
	}

	public void SetAnnotIdsRemapped(boolean annotIdsRemapped) {
		this.annotIdsRemapped = annotIdsRemapped;
	}



	/**
	 * maps paragraph ids as they were given to the annotators to their indices in paragraphs ArrayList.
	 * This structure is only useful in data sources where annotations are loaded from separate files.
	 */
	protected TreeMap <Integer, Integer> parAnnotIdToIndex = new TreeMap<Integer, Integer>();
	/**
	 * maps paragraph ids as specified in annotations to paragraph ids as ordered in this document
	 */
	protected TreeMap<Integer, Integer> annotIdToParId = new TreeMap<Integer, Integer>();
	
	public void MapParIdToAnnotation(Integer parId, Integer idInAnnotation)
	{
		this.parAnnotIdToIndex.put(parId, idInAnnotation);
		this.annotIdToParId.put(idInAnnotation, parId);
	}
	
	public Integer GetAnnotIdForPar(Integer parId) throws Exception
	{
		return this.parAnnotIdToIndex.get(parId);
	}
	
	/**
	 * 
	 * @param idInAnnotations paragraph id as specified in the annotations File
	 * @return  id of the corresponding paragraph as specified in this.paragraphs
	 * @throws Exception
	 */
	public Integer GetParIdForAnnotId(Integer idInAnnotations) throws Exception
	{
		return (this.annotIdToParId.get(idInAnnotations));
	}
	
	
	
	/**
	 * the data structure contains a mapping between the annotator ids and arrays of segment breaks that they specified
	 */
	protected TreeMap<Integer, ArrayList<Integer>> segmentBreaks = new TreeMap<Integer, ArrayList<Integer>> ();
	
	

	/**
	 * @param levelOfSegmentation 1- sentences, 2 paragraphs
	 * @param the string that marks segment breaks in xml files, if any
	 * @param source xml file with a parse from Connexor Machinese parser
	 * @param annotationsFile null or a link to annotations by several people
	 */
	public void Init(int levelOfSegm, String segmPattern,
			File textFile, File annotationsFile) throws Exception {
		
		if (levelOfSegm != IGenericDataSource.SENT_LEVEL && levelOfSegm != IGenericDataSource.PAR_LEVEL)
		{
			Exception e = new Exception ("Exception in ConnexorXMLDataSource.Init: invalid level of segmentation specified: " + String.valueOf(levelOfSegm));
			throw (e);
		}
		
		this.levelOfSegmentation = levelOfSegm;
		this.segmPattern = segmPattern;
		this.sourceFile = textFile;
		this.annotsFile = annotationsFile;

	}
	
	/**
	 * returns level of segmentation 1- sentences, 2 - paragraphs
	 */
	public int GetLevelOfSegm() {
		return this.levelOfSegmentation;
	}

	
	public void SetLevelOfSegm(int segmLevel) throws Exception{
		if (segmLevel != IGenericDataSource.SENT_LEVEL && segmLevel!= IGenericDataSource.PAR_LEVEL)
		{
			throw (new Exception("invalid segmentation level in ConnexorXMLDataSource.SetLevelOfSegm: " + segmLevel));
		}
		this.levelOfSegmentation = segmLevel;

	}

	/**
	 * this method is not available for this class. 
	 */
	public void Init(int levelOfSegmentation, String segmPattern, String text,
			File annotationsFile) throws Exception {
		Exception e = new Exception ("cannot instantiate this class from text. You need a source XML file.");
		throw (e);

	}
	
	/**
	 * The methods add a parsed sentence to this document.
	 * 
	 * @param curTree SentenceTree instance corresponding to the last processed sentence.
	 * @param curParIndex the index of the paragraph which includes this sentence
	 */
	public void AddSentence (SentenceTree curTree, int curParIndex)
	{
		this.sentences.add(curTree);
		Integer connexorId = new Integer (curTree.GetId());
		this.indexToConnexorId.put(this.sentences.size() -1, connexorId);
		this.sentIndexToPar.put(this.sentences.size() - 1, new Integer(curParIndex));
	}
	
	public void AddParagraph(ParsedParagraph curPar)
	{
		this.paragraphs.add(curPar);
	}
	
	public ParsedParagraph GetParagraph( int curParId) throws Exception
	{
		return (this.paragraphs.get(curParId));
	}
	
	/**
	 * returns a sentence with a given index
	 * @param id sentence id
	 * @return
	 */
	public SentenceTree GetSentence(int id)
	{
		return (this.sentences.get(id));
	}
	
	/**
	 * maps sentence indices as stored in this document to their Connexor Ids
	 * @param connexorId
	 * @param sentId
	 */
	public void MapIndexToConnexorId(int connexorId, int sentId)
	{
		this.indexToConnexorId.put( new Integer(sentId), new Integer(connexorId));
	}
	
	public int GetConnexorId(int sentIndex) throws Exception
	{
		if (this.indexToConnexorId.containsKey(new Integer(sentIndex)))
		{
			return (this.indexToConnexorId.get(new Integer(sentIndex)));
		}
		else
		{
			Exception e = new Exception ("No such sent index in indexToConnexorId: " + sentIndex);
			throw (e);
		}
	}
	
	/**
	 * returns the index of the last sentence in this document (as stored in ConnexorXMLDataSource.sentences)
	 * @return
	 */
	public Integer GetLastSentenceIndex()
	{
		return (this.sentences.size() - 1);
	}
	
	public Integer GetLastParIndex()
	{
		return (this.paragraphs.size() - 1);
	}
	
	/**
	 * This methods adds a segment boundary for a specified annotation.
	 * 
	 * @param annotId
	 * @param breakPosition
	 * @throws Exception
	 */
	public abstract void AddReferenceBreak (int annotId, int breakPosition) throws Exception;
	
	/**
	 * This method maps a sentence with sentId to its parent paragraph
	 * @param sentId
	 * @param parId
	 */
	public void MapSentToParagraph(int sentId, int parId)
	{
		this.sentIndexToPar.put(new Integer(sentId), new Integer(parId));
	}
	
	/**
	 * returns id of the parent paragraph for a given sentence
	 * @param sentId
	 * @return
	 */
	public Integer GetParagraphId (int sentId)
	{
		return ( this.sentIndexToPar.get(new Integer(sentId)) );
	}
	

	
	abstract public void LightWeightInit(int numChunks) ;

	@Override
	public abstract int GetNumberOfAnnotators();

	/**
	 * depending on whether this.levelOfSegmentation is 1 or 2, the method returns
	 * a sentence or a paragraph indexed chunkIndex
	 */
	public String GetChunk(int chunkIndex)  throws Exception
	{
		StringBuilder chunkText = new StringBuilder();
		
		if (this.GetLevelOfSegm() == IGenericDataSource.SENT_LEVEL)
		{
			SentenceTree tree = this.GetSentence(chunkIndex);
			return tree.GetLemmatizedText();
			//return tree.GetSentText();
		}
		else if (this.GetLevelOfSegm() == IGenericDataSource.PAR_LEVEL)
		{
			ParsedParagraph par = this.GetParagraph(chunkIndex);
			return par.GetParagraphText();
		}
		else
		{
			throw (new Exception("invalid segmentation level in ConnexorXMLDataSource.GetChunk: " + this.levelOfSegmentation));
		}
		
	}

	/**
	 * returns the name of the source document
	 */
	public String GetName() {
		return this.sourceFile.getName();
	}

	/**
	 * @retrun if levelOfSegmentation is sentences, this method returns the number of sentences. If levelOfSegmentation is paragraphs, then it returns the number of paragraphs
	 */
	public int GetNumberOfChunks() {
		if (this.levelOfSegmentation == IGenericDataSource.SENT_LEVEL)
			return this.sentences.size();
		else 
			return this.paragraphs.size();
	}

	
	public abstract int GetNumberOfSegments(int annotId) ;

	@Override
	public abstract ArrayList<Integer> GetReferenceBreaks(int annotId) ;

	@Override
	public abstract void SetReferenceBreaks(int annotId, ArrayList<Integer> breaks)
			throws Exception ;

	@Override
	public abstract Double GetAveNumberOfSegments() throws Exception ;

	@Override
	public abstract Double GetAverageSegmentLength() throws Exception ;

	@Override
	public abstract Double GetAverageSegmentLengthForAnnot(int annotId) throws Exception ;

	@Override
	public abstract Integer GetNumberOfSegmentsForAnnot(int annotId) throws Exception ;

	@Override
	public ArrayList<Integer> GenerateRandomReferenceBreaks(int numBreaks) {
		
		ArrayList<Integer> newBr = new ArrayList<Integer>();
		
		int range = this.GetNumberOfChunks();
		int counter = 0;
		
		while (counter < numBreaks) //for (int i = 0; i < numBreaks; i++)
		{
			double curBrD = Math.random() * range;
			int curBreak = (int) Math.round(curBrD);
			if ( newBr.contains( new Integer(curBreak) ) == false  && curBreak < (this.GetNumberOfChunks() - 1) )
			{
				newBr.add(new Integer(curBreak));
				counter++;	
			}
		}
		
		//this.refBreaks = newBr;
		return newBr;
	}

	@Override
	public void OutputFullText(File outputFile, ArrayList<Integer> breaks)
			throws Exception {
		try{
			StringBuilder text = new StringBuilder();
			if (this.levelOfSegmentation == IGenericDataSource.SENT_LEVEL)
			{
				for (int i = 0; i <= this.GetLastSentenceIndex(); i++)
				{
					text.append(String.valueOf(i) + "\t");
					text.append(this.GetSentence(i).GetSentText());
					text.append("\n");
					if ( breaks.contains(new Integer(i)))
					{
						text.append("<segmentbreak>\n");
					}
				}
			}
			else // it is paragraph level
			{
				for (int i = 0; i <= this.GetLastParIndex(); i++)
				{
					text.append(String.valueOf(i) + "\t");
					ParsedParagraph curPar = this.GetParagraph(i);
					String curParText = curPar.GetParagraphText();
					text.append(curParText);
					text.append("\n");
					if ( breaks.contains(new Integer(i)))
					{
						text.append("<segmentbreak>\n");
					}
					
				}
			}
			TextFileIO.OutputFile(outputFile, text.toString());
		}
		catch (Exception e)
		{
			String msg = "Exception in ConnexorXMLDataSource:OutputFullText: " + e.getMessage();
			System.out.println(msg);
			throw (new Exception (msg));
		}

	}

	@Override
	public void OutputBreaksOnly(File outputFile, ArrayList<Integer> breaks)
			throws Exception {
		// TODO Auto-generated method stub

	}
	
	public static XMLReader CreateXMLReader() throws Exception
	{
		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setFeature("http://xml.org/sax/features/validation", false);
		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		return reader;
		
	}

}
