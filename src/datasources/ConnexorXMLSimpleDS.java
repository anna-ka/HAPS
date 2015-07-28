package datasources;

import java.util.ArrayList;

//import sun.tools.tree.ThisExpression;

public class ConnexorXMLSimpleDS extends ConnexorXMLDataSource {
	
	/**
	 * A dummy value, by default 0, to store the only available annotation for this data source
	 */
	private Integer dummyAnnotId = new Integer (0);
	

	@Override
	public int GetNumberOfAnnotators() {
		return 1;
	}

	
	@Override
	public int GetNumberOfSegments(int annotId) {
		ArrayList<Integer> br = this.GetReferenceBreaks(0);
		return br.size();
		//return this.segmentBreaks.size();
	}

	@Override
	public ArrayList<Integer> GetReferenceBreaks(int annotId) {
		try{
			return this.segmentBreaks.get(this.dummyAnnotId);
		}
		catch (Exception e)
		{
			System.out.println("Warning in ConnexorSimpleXMLDS.GetReferenceBreaks: no segment breaks have been specified, returning null");
			return null;
		}
	}
	
	public void AddReferenceBreak (int annotId, int breakPosition) throws Exception
	{
		int maxSize = 0;
		if (this.GetLevelOfSegm() == IGenericDataSource.PAR_LEVEL)
			maxSize = this.GetLastParIndex();
		else
			maxSize = this.GetLastSentenceIndex();
		if (breakPosition > maxSize)
		{
			throw (new Exception("Exception in AddReferenceBreak. breakPosition is out of bounds " + breakPosition));
		}
		
		if (this.segmentBreaks.containsKey(this.dummyAnnotId) == false)
		{
			this.segmentBreaks.put(this.dummyAnnotId, new ArrayList<Integer>());
		}
		this.segmentBreaks.get(this.dummyAnnotId).add(breakPosition);
			
	}

	@Override
	public void SetReferenceBreaks(int annotId, ArrayList<Integer> breaks)
			throws Exception {
		this.segmentBreaks.put(this.dummyAnnotId, breaks);

	}

	/**
	 * returns the exact number of segments since only one annotation is available
	 */
	public Double GetAveNumberOfSegments() throws Exception {
		ArrayList<Integer> br = this.segmentBreaks.get(this.dummyAnnotId);
		//return (new Double(this.segmentBreaks.size()));
		return (new Double(br.size()));
	}

	@Override
	public Double GetAverageSegmentLength() throws Exception {
		ArrayList<Integer> br = this.segmentBreaks.get(this.dummyAnnotId);
		
		//return ( new Double( this.GetNumberOfChunks() / this.segmentBreaks.size()) );
		return ( new Double( this.GetNumberOfChunks() / br.size()) );
	}

	@Override
	public Double GetAverageSegmentLengthForAnnot(int annotId) throws Exception {
		
		return (this.GetAverageSegmentLength());
	}

	@Override
	public Integer GetNumberOfSegmentsForAnnot(int annotId) throws Exception {
		return (this.GetAveNumberOfSegments().intValue());
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


	@Override
	public void SetIfHierarchical(boolean isHierarchical) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean GetIfHierarchical() {
		// TODO Auto-generated method stub
		return false;
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
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void SetReferenceBreaksAtLevel(int annotId,
			ArrayList<Integer> breaks, int levelId) throws Exception {
		// TODO Auto-generated method stub
		
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


}
