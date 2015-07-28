package evaluation;

import java.util.ArrayList;

import datasources.ConnexorXMLMultipleAnnotsDS;
import datasources.SimpleFileMultipleAnnotsDataSource;
import evaluation.MultWinDiffEvaluator.MULTWD_TYPE;
import java.lang.ProcessBuilder;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class calls SegEval (http://segeval.readthedocs.org/en/latest/) by Chris Founier and
 * returns boundary similarity between two segementations.
 *  Works for version of Segeval as of Dec. 11, 2013
 * 
 * @author anna
 *
 */
public class FournierBoundarySimEvaluator extends AbstractEvaluator {
	
	private String segEvalPath = "/Users/anna/software/SegEval/segmentation.evaluation";
	
	ArrayList<Integer> referenceSegmentation = new ArrayList<Integer>();
	boolean isInitialized = false;
	public static enum REF_TYPE{MAJORITY_OPINION, UNION, INTERSECTION};
	
	protected  REF_TYPE metricType = REF_TYPE.MAJORITY_OPINION;

	public REF_TYPE GetMetricType() {
		return metricType;
	}

	public void SetMetricType(REF_TYPE metricType) {
		this.metricType = metricType;
	}
	

	public boolean GetIsInitialized() {
		return isInitialized;
	}

	public void SetIsInitialized(boolean isInitialized) {
		this.isInitialized = isInitialized;
	}
	
	/**
	 * If multiple reference annotations are available, this method combines them
	 * into a single gold standard according to metricType value.
	 * For now we simply use majority opinion
	 */
	public void SetUpReference() throws Exception
	{
		String refClass = this.refDS.getClass().toString();
		if (refClass.endsWith("SimpleFileDataSource") || refClass.endsWith("ConnexorXMLSimpleDS") )
		{
			this.referenceSegmentation = this.refDS.GetReferenceBreaks(0);
		}
		else if  (refClass.endsWith("SimpleFileMultipleAnnotsDataSource") ) //this is a multiple DS class
		{
			SimpleFileMultipleAnnotsDataSource dummyRef = (SimpleFileMultipleAnnotsDataSource)this.refDS;
			

			this.referenceSegmentation = dummyRef.GetMajorityOpinion();
//			this.referenceSegmentation = dummyRef.GetIntersectionOpinion();
					//this.GetMajorityOpinion();
		}
		else if (refClass.endsWith("ConnexorXMLMultipleAnnotsDS") )
		{
			ConnexorXMLMultipleAnnotsDS dummyRef = (ConnexorXMLMultipleAnnotsDS)this.refDS;
//			this.referenceSegmentation = dummyRef.GetIntersectionOpinion();

			this.referenceSegmentation = dummyRef.GetMajorityOpinion();
		}
		this.SetIsInitialized(true);
	}
	
	public ArrayList<Integer> GetMajorityOpinion() throws Exception
	{
		
		int [] refSequence = new int[this.refDS.GetNumberOfChunks()];
		for (int i = 0; i < refSequence.length; i++)
		{
			Integer curPosition = new Integer (i);
			int numAnnots = this.refDS.GetNumberOfAnnotators();
			if (numAnnots == 1)
				return (this.refDS.GetReferenceBreaks(1));
			
			int half = (int)Math.floor( (double)numAnnots / (double)2 );
			
			int numIncluded = 0;
			
			
			for (int a = 1; a <= this.refDS.GetNumberOfAnnotators(); a++)
			{
				ArrayList<Integer> curRef = this.refDS.GetReferenceBreaks(a);
						
				if (curRef.contains(curPosition))
				{	
					//counter--;
					numIncluded++;
				}
			}
			
//			if ( counter <= half )
			if ( numIncluded >= half )
				//all annotators included this break
			{
				refSequence[i] = 1;
			}
			else
				refSequence[i] = 0;
		}
		
		ArrayList<Integer> majority = new ArrayList<Integer>();
		for (int i = 0; i < refSequence.length; i++)
		{
			majority.add(refSequence[i]);
		}
		System.out.println("majority: " + majority.toString());
		return majority;
	}

	@Override
	public int SpecificVerifyCompatibility() {
		
		
		int result = this.COMPATIBLE;
		return result;
	}

	@Override
	public Double ComputeValue() throws Exception {
		
		if ( this.referenceSegmentation.size() <= 0)
			this.SetUpReference();
		String hypoStr = this.GenerateFournierRepr(this.hypoDS.GetReferenceBreaks(1));
		String refStr = this.GenerateFournierRepr(this.referenceSegmentation);
		return this.RunSegEval(refStr, hypoStr);
	}
	
	/**
	 * Returns 1 - boundarySimilarity
	 * @param refStr
	 * @param hypoStr
	 * @return
	 * @throws Exception
	 */
	public Double RunSegEval(String refStr, String hypoStr) throws Exception
	{
		Double result = new Double (-100);
		
		//"import segeval; print(segeval.boundary_similarity ((1,6,4,2,4,5), (2,6,4,2,4,4)))"
		//ProcessBuilder pb = new ProcessBuilder("python",  "-c",   "import segeval; print(segeval.boundary_similarity ((1,6,4,2,4,5), (2,6,4,2,4,4)))");
		String commandStr = "import segeval; print(segeval.boundary" +
				"_similarity (" + refStr + ", " + hypoStr + "))";
		System.out.println("segeval command: " + commandStr);
		
		ProcessBuilder pb = new ProcessBuilder("python",  "-c",   commandStr);
		
		File cwd = new File(this.segEvalPath);
		pb.directory(cwd);
		Process process = pb.start();
		
		InputStream is = process.getInputStream();
		InputStream errS = process.getErrorStream();
		 
		 //get errors, hopefully none
		 InputStreamReader esr = new InputStreamReader(errS);
	     BufferedReader eBr = new BufferedReader(esr);
	     String line;
	     while ((line = eBr.readLine()) != null) 
	     {
	      System.out.println(line);
	     }
	     //System.out.println("no more errors");
	     
	     //get output
	     InputStreamReader isr = new InputStreamReader(is);
	     BufferedReader br = new BufferedReader(isr);
	     line = null;
	     while ((line = br.readLine()) != null) 
	     {
	      System.out.println("segeval result: " + line);
	      result = new Double (line);
	      break;
	     }
	     //System.out.println("Program terminated!");
	     
	     System.out.println("returning 1-boundarySim: " + (1 - result));
	     return (1 - result);
		
	}

	public String GenerateFournierRepr(ArrayList<Integer>  breaks) throws Exception
	{
		StringBuilder fRepr = new StringBuilder();
		fRepr.append("(");
		
		Integer start = 0;
		Integer end = 0;
		
		for (int i = 0; i < breaks.size(); i++)
		{
			Integer curBreak = breaks.get(i);
			
			end = curBreak;
			Integer segmLength = end - start + 1;
			fRepr.append(segmLength );
			
			if (i < (breaks.size() - 1))
				fRepr.append(",");
			
			start = end + 1;
		}
		
		fRepr.append(")");
		
		System.out.println("converted " + breaks.toString() + " to ");
		System.out.println(fRepr.toString());
		
		return fRepr.toString();
	}
	

}
