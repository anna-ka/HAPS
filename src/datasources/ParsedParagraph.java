package datasources;

import java.util.ArrayList;
import java.util.List;

/**
 * a class encapsulating a parsed paragraph from Connexor parser output
 * @author anna
 *
 */
public class ParsedParagraph {

	/**
	 * indices of the first and last sentences as they appear in the source document
	 */
	int startSentenceIndex = -1;
	int endSentenceIndex = -1; 
	ConnexorXMLDataSource parentDoc = null;
	
	/**
	 * the index of this paragraph in the parent document
	 */
	int paragraphIndex = -1;
	/**
	 * id of this paragraph passed through the Connexor Parser. It is mostly only relevant for data sources with multiple
	 * annotations. There it corresponds to the paragraph ids as they were given to the annotators.
	 */
	int idInAnnotations = -1;
	
	

	public int GetIdInAnnotations() {
		return idInAnnotations;
	}

	public void SetIdInAnnotations(int idInAnnotations) {
		this.idInAnnotations = idInAnnotations;
	}

	/**
	 * 
	 * @param parentDocument
	 * @param startSent the index of the first sentence in this paragraph (as specified in parentDocument
	 * @param curParIndex the index of this paragraph in the parent document
	 */
	public ParsedParagraph(ConnexorXMLDataSource parentDocument, int curParIndex, int startSent)
	{
		this.parentDoc = parentDocument;
		this.startSentenceIndex = startSent;
		this.paragraphIndex = curParIndex;
	}
	
	public int GetStartSentenceIndex() {
		return startSentenceIndex;
	}

	public void SetStartSentenceIndexm(int startSentenceIndexm) {
		this.startSentenceIndex = startSentenceIndexm;
	}

	public int GetEndSentenceIndex() {
		return endSentenceIndex;
	}

	public void SetEndSentenceIndex(int endSentenceIndex) {
		this.endSentenceIndex = endSentenceIndex;
	}
	
	/**
	 * returns an arrayList of all sentences in this paragraph
	 * @return
	 */
	public List<SentenceTree> GetSentences() throws Exception
	{
		if (this.startSentenceIndex < 0 || this.endSentenceIndex< 0 || this.endSentenceIndex < this.startSentenceIndex)
		{
			String msg = "Faulty start or end index in ParsedParagraph.GetSentence: start -" + this.startSentenceIndex + "  end - " + this.endSentenceIndex;
			throw (new Exception(msg));
		}
		List<SentenceTree> sents = this.parentDoc.sentences.subList(this.startSentenceIndex, this.endSentenceIndex + 1);
		return (sents);
		
	}
	
	public String GetParagraphText ()
	{
		StringBuilder text= new StringBuilder();
		int curIndex = this.startSentenceIndex;
		while (curIndex <= this.endSentenceIndex)
		{
			SentenceTree curTree = this.parentDoc.GetSentence(curIndex);
			//text.append(curTree.GetSentText() + "\n");
			text.append(curTree.GetSentText() + "\n");
			curIndex++;
		}
		return text.toString();
	}
	
	/**
	 * This method checks whether candidatePar is a valid paragraph - that start index is less or equals to the end index and that 
	 * it has a positive number of sentences. This method is necessary since sometimes Connexor produces erroneus annotations w.r.t. paragraphs
	 * or segment breaks
	 * @param candidatePar
	 * @return
	 */
	static public boolean CheckParagraph(ParsedParagraph candidatePar)
	{
		try{
			
			if (candidatePar.startSentenceIndex > candidatePar.endSentenceIndex)
				return false;
			else if (candidatePar.endSentenceIndex > candidatePar.parentDoc.GetLastSentenceIndex())
				return false;
			else if ( (candidatePar.endSentenceIndex - candidatePar.startSentenceIndex) < 0)
				return false;
			else 
				return (true);
			
		}
		catch (Exception e)
		{
			System.out.println("Exception in ParsedParagraph.CheckParagraph: ");
			System.out.println(e.getMessage());
			return( false);
		}
	}
	

}
