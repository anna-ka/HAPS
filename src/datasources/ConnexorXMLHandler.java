package datasources;

import java.lang.StringBuilder;
import java.io.*;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Pattern;
//import java.util.regex.Matcher;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;


// a class that loads a parse tree from COnnexor Parser in XML
//processes it
//and selects Noun Phrases and their attributes for each sentence

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.aliasi.coref.Matcher;

//import segmentor.RecentContext;

//import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class ConnexorXMLHandler implements ContentHandler, ErrorHandler {
	
	final static String stanfordClassifierPath = "/Users/anna/nlp_applications/stanford_ner/stanford-ner-2009-01-16/classifiers/ner-eng-ie.crf-3-all2008.ser.gz";
	
	String segmentBoundaryMarker = "====="; // boundary marker to be output in the new document
	String paragraphMarker = "<p>"; // a tag to be output in the new doc
	
	String xmlBoundaryMarker = "<segmentbreak>"; // a segment boundary tag used in the xml parse tree files
	
	File inputFile = null;
	File outputFile = null;
	File logFile = null;
	File annotationsFile = null; // a file to hold multiple annotations for the same source
	
	Locator locator;
	
	StringBuilder documentRepr = null; //a mutable string to hold the new document representation
	StringBuilder textBuffer = null; // a mutable string to collect characters found inside any one given element
	
	
	//indicator of where inside the document we are
	int status = -1;
	
	//possible values for the indicator, they represent the name of the parent node
	static final int INVALID = -1;
	static final int TEXT = 0;
	ParseNode curParseNode = null;
	SentenceTree curSent = null;
	
	//named entities classifier
	AbstractSequenceClassifier neClassifier = null;
	
	protected int curSegmLevel = 1 ; // 1- sent, 2 paragraphs
	protected int curParagraphIndex = 0;
	protected ParsedParagraph curParagraph = null;
	
	protected ConnexorXMLDataSource curDocument = null;
	protected int documentDataType = -1;
	
	/**
	 * 
	 * @param input xml file containing Connexor parse
	 * @param annotationsFile if we have multiple annotations for this document, this is the file containing them
	 * @param output output file
	 * @param logs log file
	 * @param segmentationLevel 1 - sentences, 2 - paragraphs(see IGenericDataSource)
	 * @param segmBoundaryMarker element that marks segment boundaries in the xml file, if any
	 */
	public void Init(File input, File annotationsFile, File output, File logs, int segmentationLevel, 
			String segmBoundaryMarker, int documentType) throws Exception
	{
		this.documentRepr = new StringBuilder();
		this.textBuffer = new StringBuilder();
		
		this.inputFile = input;
		this.outputFile = output;
		this.logFile = logs;
		this.documentDataType = documentType;
		this.annotationsFile = annotationsFile;
		
		//this.neClassifier = CRFClassifier.getClassifierNoExceptions(this.stanfordClassifierPath);
		
		if (segmentationLevel != IGenericDataSource.SENT_LEVEL && segmentationLevel != IGenericDataSource.PAR_LEVEL)
		{
			Exception e = new Exception ("Exception in ConnexorXMLHandler.Init: invalid level of segmentation specified: " + String.valueOf(segmentationLevel));
			throw (e);
		}
		
		this.curSegmLevel = segmentationLevel;
		//do we have more several annotations for this document?
		if (documentType ==IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS)
		{
			this.curDocument = new ConnexorXMLMultipleAnnotsDS();
			this.curDocument.Init(this.curSegmLevel, this.segmentBoundaryMarker, this.inputFile, this.annotationsFile);
		}
		//otherwise the only annotations are inline in the xml file
		else
		{
			this.curDocument = new ConnexorXMLSimpleDS();
			this.curDocument.Init(this.curSegmLevel, this.segmentBoundaryMarker, this.inputFile, this.annotationsFile);
		}
		
		this.curParagraphIndex = 0;
		//create a new paragraph object starting at sent 0
		this.curParagraph = new ParsedParagraph(this.curDocument, this.curParagraphIndex, 0);
		
	}

	public void startElement(String uri, String localName, String name,
			Attributes atts) throws SAXException {
		try
		{
			//System.out.println("<" + name + ">");
			
			if (name.compareTo("sentence") == 0)
			{
				//get sent id 
				String idValue = atts.getValue("id");
				if (idValue == null )
				{
					String msg = "ERROR: encountered <sentence> element without id in column "
					+ this.locator.getColumnNumber() + ", line "+ this.locator.getLineNumber();
					throw (new SAXException(msg));
				}
				
				//all connexor ids are of form 'w0000', remove the leading 'w'
				String idNum = idValue.substring(1);
				int id = Integer.parseInt(idNum);
				
				this.curSent  = new SentenceTree(id);
				this.documentRepr.append("\n<sentence id=" + idNum + ">" );
			}

			//this is a token node, create new ParseNode
			else if (name.compareTo("token") == 0)
			{
				//get token id
				String idValue = atts.getValue("id");
				if (idValue == null )
				{
					String msg = "ERROR: encountered token element without id in column "
					+ this.locator.getColumnNumber() + ", line "+ this.locator.getLineNumber();
					throw (new SAXException(msg));
				}
				
				//all connexor ids are of form 'w0000', remove the leading 'w'
				String idNum = idValue.substring(1);
				int id = Integer.parseInt(idNum);
				
				this.curParseNode = new ParseNode(id);
			}
			
			else if(name.compareTo("depend") == 0)
			{
				//get id of the parent node
				String parentValue = atts.getValue("head");
				if (parentValue == null )
				{
					String msg = "ERROR: encountered 'depend' element without 'head' attr in column "
					+ this.locator.getColumnNumber() + ", line "+ this.locator.getLineNumber();
					throw (new SAXException(msg));
				}
				
				//all connexor ids are of form 'w0000', remove the leading 'w'
				String pNum = parentValue.substring(1);
				int parent = Integer.parseInt(pNum);
				
				this.curParseNode.SetParentId(parent);
			}
			
			else if (name.compareTo("paragraph") == 0)
				//this is a paragraph node
			{
				this.documentRepr.append(this.paragraphMarker + "\n");
				
				//close and attach the previous paragraph
				int endIndex = this.curDocument.GetLastSentenceIndex();
				this.curParagraph.SetEndSentenceIndex(endIndex);
				if (ParsedParagraph.CheckParagraph(this.curParagraph) == true)
				{
					System.out.println("PARAGRAPH " + this.curParagraph.paragraphIndex);
					System.out.println("PARAGRAPH (in annots)" + this.curParagraph.GetIdInAnnotations());
					System.out.println(this.curParagraph.GetParagraphText());
					
					this.curDocument.AddParagraph(this.curParagraph);
					this.curParagraphIndex++;
				}
				this.curParagraph = new ParsedParagraph(this.curDocument,this.curParagraphIndex, endIndex + 1);
				
			}
			//we found a segment boundary marker
			else if (name.compareTo("comment") == 0)
			{
				String commentValue = atts.getValue("value");
				Pattern p = Pattern.compile("<parid([\\d]+)>");
				java.util.regex.Matcher parMatcher = p.matcher(commentValue);
				
				if (commentValue != null && commentValue.equalsIgnoreCase(this.xmlBoundaryMarker))
				{
					this.documentRepr.append("\n" + this.segmentBoundaryMarker + "\n");
					
					
					if (this.curDocument.getClass().getName().endsWith("ConnexorXMLSimpleDS"))
					{
						if (this.curDocument.GetLevelOfSegm() == IGenericDataSource.SENT_LEVEL)
						{
							int lastSentInd = this.curDocument.GetLastSentenceIndex();
							if (this.curDocument.sentences.size() > 0)
								this.curDocument.AddReferenceBreak(0, lastSentInd);
						}
						else if (this.curDocument.GetLevelOfSegm() == IGenericDataSource.PAR_LEVEL)
						{
							int lastParInd = this.curDocument.GetLastParIndex();
							if (this.curDocument.paragraphs.size() > 0)
								this.curDocument.AddReferenceBreak(0, lastParInd);
						}
						
					}
					
				}
				//this is an end of a paragraph
				else if (parMatcher.matches())
				{
					int lastSentInd = this.curDocument.GetLastSentenceIndex();
					//first of all, check if the previous paragraph has been closed with <paragraph> as expected
					if (this.curParagraph.GetStartSentenceIndex() < lastSentInd + 1)
					{
						System.out.println("Improperly closed paragraph " + this.curParagraph.paragraphIndex);
						//close the paragraph
						this.documentRepr.append(this.paragraphMarker + "\n");
						
						//close and attach the previous paragraph
						int endIndex = this.curDocument.GetLastSentenceIndex();
						this.curParagraph.SetEndSentenceIndex(endIndex);
						if (ParsedParagraph.CheckParagraph(this.curParagraph) == true)
						{
							System.out.println("PARAGRAPH " + this.curParagraph.paragraphIndex);
							System.out.println("PARAGRAPH (in annots)" + this.curParagraph.GetIdInAnnotations());
							System.out.println(this.curParagraph.GetParagraphText());
							
							this.curDocument.AddParagraph(this.curParagraph);
							this.curParagraphIndex++;
						}
						this.curParagraph = new ParsedParagraph(this.curDocument,this.curParagraphIndex, endIndex + 1);
					}
					
					
					String strId = parMatcher.group(1);
					Integer curParAnnotId = new Integer(strId);
					this.curParagraph.SetIdInAnnotations(curParAnnotId);
					this.curDocument.MapParIdToAnnotation(this.curParagraphIndex, curParAnnotId);
					
					
//					
//					this.documentRepr.append(this.paragraphMarker + "\n");
//					
//					//close and attach the previous paragraph
//					int endIndex = this.curDocument.GetLastSentenceIndex();
//					this.curParagraph.SetEndSentenceIndex(endIndex);
//					if (ParsedParagraph.CheckParagraph(this.curParagraph) == true)
//					{
//						System.out.println("PARAGRAPH " + this.curParagraph.paragraphIndex);
//						System.out.println("PARAGRAPH (in annots)" + this.curParagraph.GetIdInAnnotations());
//						System.out.println(this.curParagraph.GetParagraphText());
//						
//						this.curDocument.AddParagraph(this.curParagraph);
//						this.curParagraphIndex++;
//						
//						
//					}
//					this.curParagraph = new ParsedParagraph(this.curDocument,this.curParagraphIndex, endIndex + 1);
					
					
					
				}
			}
//			else if (name.compareTo("sentence") == 0)
//			{
//				String sentId = atts.getValue("id");
//				if (sentId != null)
//				{
//					this.documentRepr.append("\n<sentence id=" + sentId + ">" );
//				}
//				else
//				{
//					SAXException e = new SAXException("Sentence without id on line " + this.locator.getLineNumber() 
//							+ " column " + this.locator.getColumnNumber());
//					throw e;
//				}
//			}
			else if (name.compareTo("text") == 0)
			{
				this.status = this.TEXT;
			}
		}
		catch (Exception e)
		{
			System.out.println("Exception in startElement " + uri + ", name: " + name + ", column "
					+ this.locator.getColumnNumber() + ", line "+ this.locator.getLineNumber() );
			System.out.println(e.getMessage());
		}
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String curChars = new String (ch, start, length);
		this.textBuffer.append(curChars);
		
		//output tokens
		if (this.status == this.TEXT)
		{
			this.documentRepr.append(curChars);
		}

	}
	

	public void endElement(String uri, String localName, String name)
			throws SAXException {
		
		
		//System.out.println( " " + this.textBuffer.toString() + " ");
		//System.out.println("</" + name + ">");
		
		if (name.compareTo("token") == 0)
		{
			//check whether this is a punct node or a comment node
			this.curParseNode.CheckNodeType();
			
			//System.out.print( this.curParseNode.GetNodeInfo() + "\n");
			
			//add this node to sentence
			try
			{
				this.curSent.AddNode(this.curParseNode);
			}
			catch (Exception e)
			{
				System.out.println("ERROR: could not add node to tree\n");
				System.out.println(e.getMessage());
			}
			
			this.curParseNode = null;		
		}
		
		if (name.compareTo("sentence") == 0)
		{
			//System.out.println(this.curSent.GetSentText());
			
			try
			{
				
				
				this.curSent.BuildTree();
				//System.out.println("built tree  successfully...");
				
				this.curDocument.AddSentence(this.curSent, this.curParagraphIndex);
				//this.curSent.PrintTree("");
				
				
				
//				this.context.AddSentence(this.curSent);
//				this.context.ComputeSimilarities();
				
				//this.curSent.FindNamedEntities(this.neClassifier);
				//System.out.println("checked for NEs  successfully...");
				//this.curSent.PrintTree("");
				
				//Extract NPs and print them
				//NPExtractor extractor = new NPExtractor(this.curSent);
				//extractor.ExtractNPs();
				//extractor.PrintNPs();
				
				//create a node for the graph and add it to the graph
				//SentVector sentRepr = new SentVector(this.curSent.nodes, this.curSent.GetSentText());
				//TreeMap<String, Double> vocab = this.CopyVocabulary(this.vocabVector);
				//sentRepr.ComputeFeatureVector(vocab);
				
				//add node to the graph
				//this.graph.addVertex(sentRepr);
				
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
			}
			finally
			{
				this.curSent = null;
			}
		}	
		//if this was a text field of a token element, insert space
		else if (name.compareTo("text") == 0)
		{
			String curText = this.textBuffer.toString();
			curText = curText.replace("\n|\r", "");
			this.curParseNode.SetText(curText);
			
			//System.out.println( " " + this.textBuffer.toString() + " ");
			
			//add whitespace for debugging
			this.documentRepr.append(" ");
			this.status = this.INVALID;
		}
		
		else if (name.compareTo("lemma") == 0)
		{
			this.curParseNode.SetLemma(this.textBuffer.toString());
		}
		
		else if (name.compareTo("depend") == 0)
		{
			this.curParseNode.SetDependency(this.textBuffer.toString());
		}
		else if (name.compareTo("morpho")  == 0)
		{
			//check whether ParseNode alrady has morpho information
			//we will ignore second parses fro now
			if (this.curParseNode.morpho.isEmpty() == false)
			{
				System.out.println("Warning: more than one morpho element on line " + this.locator.getLineNumber() 
						+ " column " + this.locator.getColumnNumber() + "\n" + this.textBuffer.toString());
			return;
			}
			
			//split morphology tags on whitespaces
			String morpho = this.textBuffer.toString();
			if (morpho.isEmpty())
			{
				System.out.println("Warning: empty morpho element on line " + this.locator.getLineNumber() 
							+ " column " + this.locator.getColumnNumber() + "\n" + this.textBuffer.toString());
				return;
			}
			
			String[] tags = morpho.split("\\s+");
			for (int i = 0; i < tags.length; i++)
			{
				this.curParseNode.morpho.add(tags[i]);
			}
			
		}
		else if (name.compareTo("syntax")  == 0)
		{
			//check whether ParseNode alrady has morpho information
			//we will ignore second parses fro now
			if (this.curParseNode.syntFunction.isEmpty() == false)
			{
//				System.out.println("Warning: more than one syntax element on line " + this.locator.getLineNumber() 
//						+ " column " + this.locator.getColumnNumber() + "\n" + this.textBuffer.toString());
			return;
			}
			
			//split syntax tags on %
			String syntax = this.textBuffer.toString();
			if (syntax.isEmpty())
			{
//				System.out.println("Warning: empty syntax element on line " + this.locator.getLineNumber() 
//							+ " column " + this.locator.getColumnNumber() + "\n" + this.textBuffer.toString());
				return;
			}
			
			/*the syntax tags are of forms
			 *  <syntax> @SYNTACTIC function tags %SYNTACTIC surface tags </syntax>
			 * */
			String[] synTags = syntax.split("\\%");
			//there should be at most 2 elements
			if (synTags.length != 2) 
			{
				System.out.println("Warning: more than one@ synt tag or % surface tag on line " + this.locator.getLineNumber() 
							+ " column " + this.locator.getColumnNumber() + "\n" + this.textBuffer.toString());
				return;
			}
			
			//process syntactic function tags (those starting with "@"
			String funcTags[] = synTags[0].split("[\\@\\s]");
			for (int f = 0; f < funcTags.length; f++)
			{
				this.curParseNode.syntFunction.add(funcTags[f]);
			}
			
			//process surface tags -- those starting with %
			if (synTags.length < 2)
			{
				System.out.println("Warning: NO % surface tags on line " + this.locator.getLineNumber() 
						+ " column " + this.locator.getColumnNumber() + "\n" + this.textBuffer.toString());
				return;
			}
			else
			{
				String[] surfaceTags = synTags[1].split("[\\%\\s]");
				for (int s = 0; s < surfaceTags.length; s++)
					this.curParseNode.syntSurface.add(surfaceTags[s]);
			}
				
		}
		
		
		
		//no matter what node it is, flush the characters buffer
		this.textBuffer  = new StringBuilder();

	}
	
	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub

	}

	public void endDocument() throws SAXException {
		
		int lastChunkIndex = this.curDocument.GetNumberOfChunks() - 1;
		if (this.documentDataType ==IGenericDataSource.CONNEXOR_SIMPLE_DS)
			try {
				this.curDocument.AddReferenceBreak(0, lastChunkIndex);
			} catch (Exception e) {
				System.out.println("Excpetion in COnnexorXMLHandler.endDocument.endDocument" );
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		
		
		//check if the last paragraph that was opened needs to be attached
		//this is useful in situations when the document does not have a </paragraph> element in the end
		if (ParsedParagraph.CheckParagraph(this.curParagraph) == true)
		{
			this.curDocument.AddParagraph(this.curParagraph);
			this.curParagraphIndex++;
		}
		else // the test has failed, let's see if indeed it is a bad paragraph
		{
			int lastParInd = this.curDocument.GetLastParIndex();
			try {
				ParsedParagraph lastPar = this.curDocument.GetParagraph(lastParInd);
				int lastEndSent = lastPar.GetEndSentenceIndex();
				if (lastEndSent == (this.curParagraph.GetStartSentenceIndex() -1))
				{
					int lastSentInDoc = this.curDocument.GetLastSentenceIndex();
					{
						if (lastSentInDoc >= this.curParagraph.GetStartSentenceIndex())
						{
							this.curParagraph.SetEndSentenceIndex(lastSentInDoc);
							this.curDocument.AddParagraph(this.curParagraph);
							this.curParagraphIndex++;
						}
					}
				}
				
				
			} catch (Exception e) {
				System.out.println("Exception in endDocument(): cannot get paragraph " + String.valueOf(lastParInd));
				e.printStackTrace();
			}
		}
		
		
		if (this.documentDataType == IGenericDataSource.CONNEXOR_MULTIPLE_ANNOTS_DS)
		{
			//check the annotations
			ConnexorXMLMultipleAnnotsDS doc = (ConnexorXMLMultipleAnnotsDS)this.curDocument;
			try {
				doc.ReloadAnnotations();
				doc.RemapLoadedSegmentBreaks();
			} catch (Exception e) {
				System.out.println("Exception in ConnexorXMLHandler.endDocument: failed to reload or remap annotations.");
				e.printStackTrace();
				throw (new SAXException (e));
			}
			
			
			if (doc.CheckReferenceAnnotations() == false)
			{
				String msg = "Invalid annotation for Data Source " + doc.GetName();
				throw (new SAXException (msg));
			}
			
		}


	}

	public void endPrefixMapping(String prefix) throws SAXException {
		// TODO Auto-generated method stub

	}

	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub

	}

	public void processingInstruction(String target, String data)
			throws SAXException {
		// TODO Auto-generated method stub

	}

	public void setDocumentLocator(Locator locator) {
		this.locator = locator;

	}

	public void skippedEntity(String name) throws SAXException {
		// TODO Auto-generated method stub

	}


	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// TODO Auto-generated method stub

	}

	public void error(SAXParseException exception) throws SAXException {
		// TODO Auto-generated method stub

	}

	public void fatalError(SAXParseException exception) throws SAXException {
		// TODO Auto-generated method stub

	}

	public void warning(SAXParseException exception) throws SAXException {
		// TODO Auto-generated method stub

	}
	
	public TreeMap<String, Double> CopyVocabulary(TreeMap<String, Double> old)
	{
		TreeMap<String, Double> newVocab = new TreeMap<String, Double>();
		Iterator <String> it = old.keySet().iterator();
		while (it.hasNext())
		{
			String lemma = it.next();
			newVocab.put(lemma, new Double(0.0));
		}
		return newVocab;
	}
	
	public int GetSegmLevel()
	{
		return this.curSegmLevel;
	}
	
	public ConnexorXMLDataSource GetDocumentRepresentation()
	{
		return this.curDocument;
	}
	

}
