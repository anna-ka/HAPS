package similarity;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import datasources.ConnexorXMLDataSource;
import datasources.IGenericDataSource;
import datasources.ParsedParagraph;
import datasources.SentenceTree;


import com.aliasi.matrix.SparseFloatVector;

// a class that hold a list of sentence representations
// as vectors

//to be used in tf.idf computations, so it may not correspond to the whole document but only to a part of it
//if this is how we compute tf.idf

/**
 * A class that holds a list BOW vectors for sentences in a document.
 * To be used in tf.idf computations.
 * 
 * It is based on old code so the usage is somewhat strange. First a constructore needs to be called, then setDocFreqs, then Init
 * 
 * @author anna
 *
 */
public class GenericDocument  implements Cloneable{

	private ArrayList<SentTokenVector> sentVectors = null;
	private ArrayList <SentSyntacticVector> sentSyntacticVectors = null;

	private TokenDictionary tokenDict = null;
	
	//term frequencies
	//private TreeMap<Integer, Double> tfDictionary = null;
	
	private TreeMap<Integer, TreeMap<Integer, Double>> termFreq = null;
	
	//tf idf values
	private TreeMap<Integer, TreeMap<Integer, Double>>  tfIdf = null;
	//document frequencies
	private DfDictionary docFreqs = null;
	int numDocsInCorpus = -1;
	IGenericDataSource dataSource = null;
	
	boolean useSegmDf = false;
	boolean useWeightedTfIdf = false;
	boolean usingConnexor = false;
	
	TreeMap<Integer, Integer> sentToSegmentMap = null;
	//this map keep maximum term frequencies per segment to be used in weighted tf.idf computations
	TreeMap<Integer, Double> maxFrequenciesPerSegment = null;
	

	private int numSegments = 1;
	
	/**
	 * parameters for extracting NPS from sentences and paragraphs
	 */
	private int openingCutOff = -1;
	private int decayWindow = -1;
	private Double decayFactor = new Double(-1);
	
	@Override
	/**
	 * This method implements deep cloning. tfDictionary, tfIdf, DfDictionary are all cloned.
	 * Sentence vectors sentVectors are cloned deeply as well.
	 * 
	 * However, the fundamental mapping of token-> id and id-> token (tokenDict) is shared. Normally it should never be altered.
	 */
	public Object clone() throws CloneNotSupportedException {
		
		GenericDocument cloned = (GenericDocument)super.clone();
		
//		//term frequencies
//		TreeMap<Integer, Double> newTfDict = new TreeMap<Integer, Double>(this.tfDictionary);
//		//tf idf values
//		TreeMap <Integer, Double> newTfIdf = new TreeMap<Integer, Double>(this.tfIdf);
		
		//new sent to segm mapping
		TreeMap<Integer, Integer> newSentToSegm = new TreeMap<Integer,Integer>(this.sentToSegmentMap);
		
		//new map of max frequencies
		TreeMap<Integer,Double> newMaxFreqMap = new TreeMap<Integer,Double>(this.maxFrequenciesPerSegment);
		
		//term frequencies
		TreeMap<Integer, TreeMap <Integer, Double>> newTermFreqs = new TreeMap<Integer, TreeMap <Integer, Double>>();
		//tf idf dictionaries
		TreeMap<Integer, TreeMap <Integer, Double>> newTfIdf = new TreeMap<Integer, TreeMap <Integer, Double>> ();
		
		for (Integer curSegm: this.termFreq.keySet())
		{
			TreeMap <Integer, Double> curTfDict = new TreeMap <Integer, Double>(this.termFreq.get(curSegm));
			newTermFreqs.put(curSegm, curTfDict);
			
			TreeMap <Integer, Double> curTfIdfDict = new TreeMap <Integer, Double>(this.tfIdf.get(curSegm));
			newTfIdf.put(curSegm, curTfIdfDict);
		}
		
		
		//document frequencies
		DfDictionary newDocFreqs = (DfDictionary) this.getDocFreqs().clone();
		
		//sentence vectors
		ArrayList<SentTokenVector> newSentVectors = new ArrayList<SentTokenVector>(this.sentVectors.size());
		for (int i = 0; i < this.sentVectors.size(); i++)
		{
			SentTokenVector clonedSent = (SentTokenVector) this.sentVectors.get(i).clone();
			newSentVectors.add(i, clonedSent);
		}
		
		//syntactic sent vectors
		ArrayList<SentSyntacticVector> newSyntacticVectors = new ArrayList<SentSyntacticVector>(this.sentVectors.size());
		for (int i = 0; i < this.sentSyntacticVectors.size(); i++)
		{
			SentSyntacticVector clonedVector = (SentSyntacticVector) this.sentSyntacticVectors.get(i).clone();
			newSyntacticVectors.add(i, clonedVector);
		}
		
		cloned.setTfDictionaries(newTermFreqs);
		cloned.setTfIdfDictionaries(newTfIdf);
		cloned.setSentVectors(newSentVectors);
		try {
			cloned.setDocFreqs(newDocFreqs);
		} catch (Exception e) {
			CloneNotSupportedException e2 = new CloneNotSupportedException("Exception in GenericDocument.Clone: could not clone DfDictionary in setDocumentFreq"); 
			e2.setStackTrace(e.getStackTrace());
			throw (e2);
		}
		cloned.sentToSegmentMap = newSentToSegm;
		cloned.maxFrequenciesPerSegment = newMaxFreqMap;
		
		return cloned;
	}
	
	
	
	public DfDictionary getDocFreqs() {
		return docFreqs;
	}

	/**
	 * This method sets DfDictionary to be used in tf.idf computations. It must be run before Init.
	 * @param docFreqs DfDictionary containing document frequencies
	 */
	public void setDocFreqs(DfDictionary docFreqs) throws Exception {
		try{
			this.docFreqs = docFreqs;
			this.sentToSegmentMap = this.docFreqs.GetSentToSegmentMap();
			
			if (this.sentToSegmentMap != null && this.sentToSegmentMap.size() > 1)
				this.useSegmDf = true;
		}
		catch (Exception e)
		{
			Exception e2 = new Exception ("Exception in GenericDocument.setDocFreqs \n" + e.getMessage());
			throw (e2);
		}
		
	}

	/**
	 * 
	 * @param dict dfDictionary of inverse document frequencies
	 * @param numDocs number of documents in the corpus, or if segment-based tf.idf is used, this is number of segments to split the document into
	 * @param data IGenericDataSource corresponding to the complete document
	 */
	public GenericDocument (TokenDictionary dict, int numDocs, IGenericDataSource data, int numSegm)
	{
		this.numSegments = numSegm;
		this.tokenDict = dict;
		this.numDocsInCorpus = numDocs;
		this.dataSource = data;
		
		this.termFreq = new TreeMap<Integer, TreeMap<Integer, Double>>();
		this.sentVectors = new ArrayList<SentTokenVector>();
		this.sentSyntacticVectors = new ArrayList<SentSyntacticVector>();
		this.tfIdf = new TreeMap<Integer, TreeMap<Integer, Double>>();
		
		//initialize termFreq and tf.idf maps
		for (int i = 0; i < this.GetNumSegments(); i++)
		{
			TreeMap<Integer, Double> curSegmTf = new TreeMap<Integer, Double>();
			TreeMap<Integer, Double> curSegmTfIdf = new TreeMap<Integer, Double>();
			//initialize term frequencies map
			for (Integer tokenId: this.tokenDict.GetAllTokenIds() )
			{
				
				curSegmTf.put(tokenId, new Double(0));
				curSegmTfIdf.put(tokenId, new Double(0));
			}
			
			this.termFreq.put(new Integer(i), curSegmTf);
			this.tfIdf.put(new Integer(i), curSegmTfIdf);
		}
		
		//initialize maxFreq map
		
		this.maxFrequenciesPerSegment = new TreeMap<Integer,Double>();
		for (int i = 0; i < this.numSegments; i++)
		{
			this.maxFrequenciesPerSegment.put(new Integer(i), new Double(0));
		}
		
		//is this document linked to a parsed data source? 
		String dsType = this.dataSource.getClass().getName();
		if (dsType.endsWith("ConnexorXMLMultipleAnnotsDS") || dsType.endsWith("ConnexorXMLSimpleDS"))
		{
			this.usingConnexor = true;
		}
		
		
	}
	
	public void Init() throws Exception
	{
		
		try{
			//calculate frequencies for all chunks in this document
			int numSent = this.dataSource.GetNumberOfChunks();
			
			//if the map of sentToSegm has not been initialized, make a default one by putting all sent in one segm
			if (this.sentToSegmentMap ==null || this.sentToSegmentMap.isEmpty())
			{
				this.sentToSegmentMap = new TreeMap<Integer, Integer>();
				for (int i = 0; i < numSent; i++)
				{
					this.sentToSegmentMap.put(new Integer(i), new Integer(0));
				}
				this.numSegments = 1;
			}
			
			
			for (int i = 0; i < numSent; i++ )
			{
				String chunk = this.dataSource.GetChunk(i);
				SentTokenVector curSent = new SentTokenVector(chunk, this.tokenDict, i, true);
				this.AddSent(curSent, i); 
				
			}
			
			//now if we are using parsed data, create a representation for syntactic sent vectors
			if (this.GetUsingConnexor() == true)
			{
				ConnexorXMLDataSource ds = (ConnexorXMLDataSource) this.dataSource;
				if (this.dataSource.GetLevelOfSegm() == IGenericDataSource.SENT_LEVEL)
				{
					for (int i = 0; i < numSent; i++ )
					{
						SentSyntacticVector curSyntSent = new SentSyntacticVector(this.openingCutOff, this.decayWindow, 
								this.decayFactor, this.dataSource.GetLevelOfSegm());
						SentenceTree tree = ds.GetSentence(i);
						curSyntSent.InitSentLevel(tree);
						this.sentSyntacticVectors.add(
								curSyntSent);
					}		
				}
				else if (this.dataSource.GetLevelOfSegm() == IGenericDataSource.PAR_LEVEL)
				{
					for (int i = 0; i < numSent; i++ )
					{
						SentSyntacticVector curSyntPar = new SentSyntacticVector(this.openingCutOff, this.decayWindow, 
								this.decayFactor, this.dataSource.GetLevelOfSegm());
						ParsedParagraph curPar = ds.GetParagraph(i);
						curSyntPar.InitParLevel(curPar);
						//this.sentSyntacticVectors.set(i, curSyntPar);
						this.sentSyntacticVectors.add(curSyntPar);
					}
				}
				else
				{
					System.out.println("Warning: invalid segmentation level in GenericDocument.Init: " + this.dataSource.GetLevelOfSegm());
				}
			}
			
		}
		catch (Exception e)
		{
			Exception e2 = new Exception ("Exception in GenericDocument.Init() \n" + e.getMessage());
			e.printStackTrace();
			throw (e2);
		}
		
		
	}
	
	/**
	 * this method is only useful when we are working with COnnexor Data sources and want to extract noun phrases. This method passes the parameters
	 * necessary to construct SentSyntacticVector representations
	 * 
	 * @param parThreshold if working with paragraphs, this specifies how many opening sentences in each paragraph are considered
	 * @param winSize the size of the window for the decaying function
	 * @param decayParam decay factor
	 */
	public void SetParametersForSyntacticVectors(int parThreshold, int winSize, Double decayParam)
	{
		this.openingCutOff = parThreshold;
		this.decayWindow = winSize;
		this.decayFactor = decayParam;
	}
	
	public TokenDictionary getTokenDict() {
		return tokenDict;
	}



	public IGenericDataSource getDataSource() {
		return dataSource;
	}



	public void setDataSource(IGenericDataSource dataSource) {
		this.dataSource = dataSource;
	}



	/**
	 * Adds a sentence to sentVectors as well as updates term frequencies for tokens found in this sentence.
	 * @param newSent SentTokenVector to be processed
	 * @param sentId its id
	 */
	public void AddSent(SentTokenVector newSent, int sentId) throws Exception
	{
		this.sentVectors.add(newSent);
		//process tokens found in this sentence and increment counters in termFreqs vector:
		SparseFloatVector sentFreqs = newSent.getFreqVector();
		int[] nonZeroKeys = sentFreqs.nonZeroDimensions();
		Integer curSegm = this.sentToSegmentMap.get(new Integer(sentId));
		Double curMaxForSegm = this.maxFrequenciesPerSegment.get(curSegm);
		
//		if (sentId == 42)
//		{
//			System.out.println("found it!");
//		}
		
		for (int tokenId: nonZeroKeys)
		{
			try{
				int tokenFreq = (int) sentFreqs.value(tokenId);
				Double oldTf = this.termFreq.get(curSegm).get(new Integer (tokenId) );
				Double newTf = new Double (oldTf+tokenFreq);
				this.termFreq.get(curSegm).put(new Integer (tokenId), newTf);
				if ( newTf > curMaxForSegm)
					this.maxFrequenciesPerSegment.put(curSegm, newTf);
				
			}
			catch (Exception e)
			{
				String msg = "Exception is GenericDocument.AddSent: problem in sent " + String.valueOf(sentId) + newSent.sentText + "\n";
				msg = msg + "Error on token " + String.valueOf(tokenId) + " " + this.tokenDict.GetTokenString(tokenId);
				System.out.print(e.getMessage());
				e.printStackTrace();
				throw (new Exception(msg));
			}
		}
	}
	
	/**
	 * computes tf.idf weights for all tokens in all segments
	 */
	public void ComputeTfIdf() throws Exception {
		
		for (Integer curTokenId: this.tokenDict.GetAllTokenIds())
		{
			for (int i = 0; i < this.numSegments; i++)
			{
				String curToken = this.tokenDict.GetTokenString(curTokenId);
				try{
					
					Integer curSegmId = new Integer(i);
					Double curTf = this.termFreq.get(curSegmId).get(curTokenId);
					Double curDf = this.docFreqs.GetDfValue(curTokenId);
							//.getDocFreqs().GetDfValue(curTokenId);
					int corpusSize = this.numDocsInCorpus;
					if (this.useSegmDf == true)
						corpusSize = this.GetNumSegments();
					Double curTfIdf;
					if (this.useWeightedTfIdf == true)
					{
						Double maxFreq = this.maxFrequenciesPerSegment.get(curSegmId).doubleValue();
						curTfIdf = this.CalculateWeightedTfIdf(curTf, curDf, corpusSize, maxFreq);
					}
					else
						curTfIdf = this.CalculateTfIdf(curTf, curDf, corpusSize);
					
					this.tfIdf.get(curSegmId).put(curTokenId, curTfIdf);
				}
				catch (Exception e)
				{
					
					String msg = "Warning in GenericDocument.ComputeTfIdf : problem with token " + curToken + " id " + curTokenId.toString() +" in segm " + String.valueOf(i);
					msg = msg + e.getMessage();
					Exception e2 = new Exception(msg);
					e2.setStackTrace(e.getStackTrace());
					throw (e2);
				}
			}
		}
		
	}
	
	//same variant as Malioutov: tfIdfWeights_[i][j] = termCounts[i][j] * 1.0 * Math.log((numSegments_) / documentCount[i]);
//	public void OldComputeTfIdf()
//	{
//		for (Integer tokenId: this.tokenDict.GetAllTokenIds())
//		{
//			
//			//System.out.print(tokenId.toString() + "\t");
//			//get the id for dfDictionary
//			String token = this.tokenDict.GetTokenString(tokenId);
//			try{
//			//System.out.println(token + "\t");
//			Integer tokenDfId = this.docFreqs.GetTokenId(token);
//			if (tokenDfId == null)
//			{
//				System.out.println("Warning in Document.ComputeTfIdf. Token not found in this.docFreqs:\t" + token);
//				continue;
//			}
//			//System.out.print(tokenDfId.toString() + "\n");
//			
//			Double tf = this.tfDictionary.get(tokenId);
//			Double df = this.docFreqs.getDfDictionary().get(tokenDfId);
//			Double tfIdf = 0.0;
//			if (df == 0)
//			{
//				System.out.println("Warning in ComputeTfIdf: df is 0 for " + token);
//			}
//			else if (tf != 0 )
//			{	
////				double idf = Math.log(this.numDocsInCorpus / df);
////				double tmp = 1 + Math.log(tf);
////				tfIdf = new Double ( tmp * idf );
//				tfIdf = CalculateTfIdf(tf, df, this.numDocsInCorpus);
//
//			}
//			this.tfIdf.put(tokenId, tfIdf);
//			}
//			catch (Exception e)
//			{
//				this.tfIdf.put(tokenId, 0.0);
//				System.out.println("Warning: in ComputeTfIdf. Recorded 0 for " + token);
//				System.out.println(e.getMessage());
//			}
//		}
//	}
	
	/**
	 * Tf.Idf as in Jurafsky p. 653
	 * @param tf
	 * @param df
	 * @param totalNumDocs
	 * @return
	 */
	public double CalculateTfIdf(double tf, double df, int totalNumDocs) throws Exception
	{
		
//		double idf = Math.log10(totalNumDocs / df);
//		double tmp = 1 + Math.log10(tf);
//		double tfIdf = tmp * idf ;
//		return tfIdf;
		
		double idf = this.log2(totalNumDocs / df); 	
		double tmp = 1 + this.log2(tf + 1);
		double tfIdf = tmp * idf ;
		
		if (tfIdf < 0 || Double.isNaN(tfIdf))
			return 0;
		
		return tfIdf;
	}
	
	private double log2(double num)
	{
		if (num == 0)
			return Double.NEGATIVE_INFINITY;
		return (Math.log(num) / Math.log(2));
	}
	
	/**
	 * weighted tf.idf as in Jurafsky p. 654
	 */
	public double CalculateWeightedTfIdf(double tf, double df, int totalNumDocs, double maxFreqInSegm) throws Exception
	{
//		double idf = Math.log(totalNumDocs / df);
//		double tmp = 1 + Math.log(tf );
//		double tfIdf = tmp * idf ;
//		return tfIdf;
		
		double wTf = 0.5 + ( 0.5* tf / maxFreqInSegm);
		double idf = this.log2(totalNumDocs / df); 	
		double tfidf = wTf * idf;
		return tfidf;
		
	}
	
	public void PrintFreqs()
	{
		System.out.println("id\ttoken\tdf\ttf\tf.df:");
		Set<Integer> tokenIds = this.tokenDict.GetAllTokenIds();
		for (Integer curId: tokenIds)
		{
			
			String curToken = this.tokenDict.GetTokenString(curId);
			Integer dfId = this.docFreqs.GetTokenId(curToken);
			if (dfId == null)
			{
				System.out.println("Warning in Document.PrintFreqs. Token not found in this.docFreqs:\t" + curToken);
				continue;
			}
			
			//String tf = String.valueOf(this.tfDictionary.get( curId));
			String df = String.valueOf(this.docFreqs.getDfDictionary().get( dfId));
			//String tfIdf = String.valueOf(this.tfIdf.get(curId));
			
			StringBuilder text = new StringBuilder();
			text.append(curId.toString() + "\t" + curToken + "\t" + df + "\t");
			
			for (Integer curSegm: this.termFreq.keySet())
			{
				Double curTf = this.termFreq.get(curSegm).get(curId);
				Double curTfIdf = this.tfIdf.get(curSegm).get(curId);
				text.append("segm" + curSegm.toString() + " tf:"+ curTf + "\t"  + "segm" +curSegm.toString() + " tf.tdf:"+ curTfIdf + "\t");
			}
			
			//text.append("\n");
			
			System.out.println( text.toString() );
			if (curId > 40)
				break;
		}
		
		
		
		
	}
	
	public void PrintTfIdf()
	{
		System.out.println("TfIdf:");
		Set<Integer> tokenIds = this.tokenDict.GetAllTokenIds();
		for (Integer curId: tokenIds)
		{
			String curToken = this.tokenDict.GetTokenString(curId);
			
			StringBuilder text = new StringBuilder();
			text.append(curId.toString() + "\t" + curToken + "\t" );
			
			for (Integer curSegm: this.termFreq.keySet())
			{
				Double curTfIdf = this.tfIdf.get(curSegm).get(curId);
				text.append( "segm" + curSegm.toString() + " tf.tdf:"+ curTfIdf + "\t");
			}
			
			//text.append("\n");
			
			System.out.println( text.toString() );
			
			if (curId > 40)
				break;
			
			
			if (curId > 40)
				break;
		}
	}
	
	public void PrintTf()
	{
		System.out.println("Tf:");
		Set<Integer> tokenIds = this.tokenDict.GetAllTokenIds();
		for (Integer curId: tokenIds)
		{
			String curToken = this.tokenDict.GetTokenString(curId);
			StringBuilder text = new StringBuilder();
			text.append(curId.toString() + "\t" + curToken + "\t" );
			
			for (Integer curSegm: this.termFreq.keySet())
			{
				Double curTf = this.termFreq.get(curSegm).get(curId);
				text.append("segm" + curSegm.toString() + " tf:"+ curTf + "\t"  );
			}
			
			//text.append("\n");
			
			System.out.println( text.toString() );
			
			if (curId > 40)
				break;
		}
	}
	
	//apply smoothing to frequency vectors for sentences:
	// in addition to counting tokens in the sentence, also count tokens in adjacent sentences
	//following Malioutov and Barzilay 2006
	
	//winSize: how many neighboring sentences to consider
	// alpha: smoothing parameter
	public void SmoothSentCounts(int winSize, double alpha)
	{
		ArrayList<SentTokenVector> oldSents = new ArrayList<SentTokenVector> (this.sentVectors);
		int curSentIndex = 0;
		int adjoinIndex = 0;
		for (; curSentIndex < oldSents.size()  ; curSentIndex++)
		{
			
			SparseFloatVector curFreqVector = this.sentVectors.get(curSentIndex).getFreqVector();
			
			//this.sentVectors.get(curSentIndex).PrintVector();
			
			//DenseVector modifications = new DenseVector(curFreqVector.numDimensions());
			TreeMap<Integer, Double> modCounts = new TreeMap<Integer, Double>();
			int[] nonZeroIndices = curFreqVector.nonZeroDimensions();
			for (int curDimension: nonZeroIndices)
			{
				modCounts.put(new Integer(curDimension), new Double (curFreqVector.value(curDimension)) );
			}
			
			//perform the summations within the adjoining window of sentences
			for (adjoinIndex = curSentIndex+1; adjoinIndex <= curSentIndex + winSize && adjoinIndex < oldSents.size(); adjoinIndex++)
			{
				SparseFloatVector adjoinFreqVector = oldSents.get(adjoinIndex).getFreqVector();
				double coefficient = Math.pow( Math.E, -alpha * (adjoinIndex - curSentIndex) );
				//System.out.println("curIndex\t" + String.valueOf(curSentIndex) + "\tadj index\t" + String.valueOf(adjoinIndex) 
				//		+ "\tcoef\t" + String.valueOf(coefficient));

				int[] indices = adjoinFreqVector.nonZeroDimensions();
				
				
				
				for (int i = 0; i < indices.length; i++)
				{
					Integer curIndex = new Integer ( indices[i] );
					Double oldFreq  = 0.0;
					if (modCounts.containsKey(curIndex) == true)
						oldFreq = modCounts.get(curIndex);
					Double modFreq =  adjoinFreqVector.value(curIndex.intValue()) * coefficient;
					modCounts.put(curIndex, oldFreq + modFreq);	
				}	
			}
			
			SparseFloatVector newFreqVector = new SparseFloatVector (modCounts, curFreqVector.numDimensions());
			this.sentVectors.get(curSentIndex).setFreqVector(newFreqVector);	
			
			//this.sentVectors.get(curSentIndex).PrintVector();
		}
		
	}
	
	public void ApplyTfIdfWeighting() throws Exception
	{
		for (int sentId = 0; sentId < this.sentVectors.size(); sentId++)
		{
			SentTokenVector sent = this.sentVectors.get(sentId);
			Integer curSegmId = this.sentToSegmentMap.get(new Integer(sentId));
			TreeMap <Integer, Double> modSent = new TreeMap<Integer, Double>();
			int[] nonZeroIndices = sent.getFreqVector().nonZeroDimensions();
			for (int i = 0; i < nonZeroIndices.length; i++)
			{
				int curTokenIndex = nonZeroIndices[i];
				//get tf.idf for this token
				Double tfIdf = this.tfIdf.get(curSegmId).get(curTokenIndex);
				Double oldValue = new Double ( sent.getFreqVector().value(curTokenIndex) );
				//Double newValue = tfIdf * Math.exp( oldValue );
				
				
				Double newValue = tfIdf * oldValue ;
				//Double newValue =( 0.5* oldValue)+ (0.5 * tfIdf * oldValue) ;
				modSent.put(new Integer(curTokenIndex), newValue);
			}
			SparseFloatVector newFreqVector = new SparseFloatVector (modSent, sent.getFreqVector().numDimensions());
			sent.setFreqVector(newFreqVector);
		}
		
	}
	
	public void PrintSentVectors(int start, int end)
	{
		System.out.println("sentence vectors:");
		if (start >= this.sentVectors.size() )
			return;
		for (int i = start; i <= end && i < this.sentVectors.size(); i++)
		{
			SentTokenVector sent = this.sentVectors.get(i);
			sent.PrintVector();
		}
	}

	public ArrayList<SentTokenVector> getSentVectors() {
		return sentVectors;
	}
	
	public ArrayList<SentSyntacticVector> GetSyntacticVectors() {
		return this.sentSyntacticVectors;
	}
	
	public void SetSyntacticVectors(ArrayList<SentSyntacticVector> newSyntVectors)
	{
		this.sentSyntacticVectors = newSyntVectors;
	}



	public TreeMap<Integer, TreeMap <Integer, Double>> getTfDictionaries() {
		return this.termFreq;
	}



	public void setTfDictionaries(TreeMap<Integer, TreeMap <Integer, Double>> tfDictionaries) {
		this.termFreq = tfDictionaries;
	}



	public TreeMap<Integer, TreeMap <Integer, Double>> getTfIdfDictionaries() {
		return this.tfIdf;
	}



	public void setTfIdfDictionaries(TreeMap<Integer, TreeMap <Integer, Double>> newTfIdf) {
		this.tfIdf = newTfIdf;
	}



	public void setSentVectors(ArrayList<SentTokenVector> sentVectors) {
		this.sentVectors = sentVectors;
	}
	
	public boolean GetUseSegmDf() {
		return useSegmDf;
	}



	public void SetUseSegmDf(boolean useSegmDf) {
		this.useSegmDf = useSegmDf;
	}



	public int GetNumSegments() {
		return numSegments;
	}



	public void SetNumSegments(int numSegments) {
		this.numSegments = numSegments;
	}

	public Double GetTfIdfValue (Integer tokenId, int segmId)
	{
		try{
			Double val =this.tfIdf.get(new Integer(segmId)).get(tokenId);
			return val;
		}
		catch (Exception e)
		{
			System.out.println ("Failed to get TfIdf for token " + tokenId.toString() + " in segment " + String.valueOf(segmId));
			return (Double.NaN);
		}
	}
	
	public Double GetTfValue (Integer tokenId, int segmId)
	{
		try{
			Double val =this.termFreq.get(new Integer(segmId)).get(tokenId);
			return val;
		}
		catch (Exception e)
		{
			System.out.println ("Failed to get Tf for token " + tokenId.toString() + " in segment " + String.valueOf(segmId));
			return (Double.NaN);
		}
	}
	
	public boolean GetUseWeightedTfIdf()
	{
		return this.useWeightedTfIdf;
	}
	
	public void SetUseWeightedTfIdf(boolean newUseWeightedTfIdf)
	{
		this.useWeightedTfIdf = newUseWeightedTfIdf;
	} 
	
	public boolean GetUsingConnexor()
	{
		return this.usingConnexor;
	}
	
}

