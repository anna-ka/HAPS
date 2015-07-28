package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import datasources.SimpleFileDataSource;
import datasources.TextFileIO;

/**
 * This class compares two SimpleFileDataSource data sources that contain hierarchical reference and annotation
 * @author anna
 *
 */
public class HierEvalHDSEvaluator extends AbstractEvaluator {
	
	File optionsFile = null;
	
	boolean useWD = false;
	
	public boolean GetUseWD()
	{
		return this.useWD;
	}
	
	public void SetUseWD(boolean value)
	{
		this.useWD = value;
	}

	@Override
	public int SpecificVerifyCompatibility() {
		if (this.refDS.GetIfHierarchical() == false)
		{
			System.out.println("error in HierEvalHDSEvaluator.SpecificVerifyCompatibility. Non hierarchical reference " + this.refDS.GetName());
			return super.NON_COMPATIBLE;
		}
		else if (this.hypoDS.GetIfHierarchical() == false)
		{
			System.out.println("error in HierEvalHDSEvaluator.SpecificVerifyCompatibility. Non hierarchical hypothetical " + this.hypoDS.GetName());
			return super.NON_COMPATIBLE;
		}
		
		return super.COMPATIBLE;
	}
	
	//File optDir, String fileName, File goldStandardPath, File hypoSegmPath, String algoName
	
	/**
	 * this method generates gold standard and opts files
	 * @throws Exception
	 */
	public void SetUp(File tmpDir, String algorithmName) throws Exception
	{
		if (tmpDir.exists() == false || tmpDir.isDirectory() == false)
			tmpDir.mkdir();
		
		SimpleFileDataSource ref = (SimpleFileDataSource)this.refDS;
		SimpleFileDataSource hypo = (SimpleFileDataSource)this.hypoDS;
		
		String goldStandardName = ref.GetShortName() + ".ref.txt";
		String hypoName = ref.GetShortName() + ".hypo.txt";
		String optsName = ref.GetShortName() + ".opts.txt";
		//String resultsName = ref.GetShortName() + ".results.txt";
		
		File goldStandardFile = new File(tmpDir, goldStandardName);
		File hypoFile = new File(tmpDir, hypoName);
		File optsFile = new File(tmpDir, optsName);
		
		if (goldStandardFile.exists())
			goldStandardFile.delete();
		if (hypoFile.exists())
			hypoFile.delete();
		if (optsFile.exists())
			optsFile.delete();
		
		
		
		//output gold standard file
		//ds.OutputFullTextWithHypoBreaks(outputFile,haps.GetAllHypoBreaks());
		ref.OutputFullTextHierarchical(goldStandardFile);
		
		//output hypothetical segmentation
		ref.OutputFullTextWithHypoBreaks(hypoFile, hypo.GetReferenceBreaksHierarchical());
		
		//generate opts file for EvalHDS
		this.GenerateEvalHDSOptFiles(optsFile, goldStandardFile, hypoFile, algorithmName);
		this.optionsFile = optsFile;
	}
	
	
	public void GenerateEvalHDSOptFiles(File optFile, File goldStandardPath, File hypoSegmPath, String algoName) throws Exception
	{
		
		StringBuilder str = new StringBuilder();
		
		if (goldStandardPath.exists() == false)
		{
			throw (new Exception ("goldStandardPath does not exist " + goldStandardPath.getAbsolutePath()));
		}
		else if (hypoSegmPath.exists() == false)
		{
			throw (new Exception ("hypoSegmPath does not exist " + hypoSegmPath.getAbsolutePath()));
		}	
		
		str.append("gold " + goldStandardPath.getAbsolutePath() + "\n\n");
		str.append("file " + hypoSegmPath.getAbsolutePath() + "\n");
		str.append("judge " + algoName + "\n");
		
		TextFileIO.OutputFile(optFile, str.toString());
		
	}

	@Override
	public Double ComputeValue() throws Exception {
		
		Integer topLayer = 2;
		if (this.useWD == false)
			return this.EvalHDS();
		
		TreeMap<Integer, Double> map = this.GetWinDiffPerLayer();
		return map.get(topLayer);
	}
	
	/**
	 * This methods runs Lucien Carrol's Python EvalHDS package and evaluates hypothetical segmentation using that metric
	 * @return
	 * @throws Exception
	 */
	public Double EvalHDS() throws Exception
	{
		
		ProcessBuilder pb = new ProcessBuilder("python", "/Users/anna/software/EvalHDS/eval/Beeferman.py", "-wal", this.optionsFile.getCanonicalPath());
		Process process = pb.start();
		
		InputStream is = process.getInputStream();
		InputStream errS = process.getErrorStream();
		 
		 //get errors, hopefully none
		 InputStreamReader esr = new InputStreamReader(errS);
	     BufferedReader eBr = new BufferedReader(esr);
	     String line;
//	     while ((line = eBr.readLine()) != null) 
//	     {
//	    	 System.out.println("ERROR: ");
//	         System.out.println(line);
//	     }
	     //System.out.println("no more errors");
	     
	     //get output
	     InputStreamReader isr = new InputStreamReader(is);
	     BufferedReader br = new BufferedReader(isr);
	     line = null;
	     StringBuilder str =  new StringBuilder();
	     while ((line = br.readLine()) != null) 
	     {
	    	 System.out.println("--" + line);
	      str.append(line + "\n");
	     }
	     System.out.println("Program terminated!");
	     
	     String output = str.toString();
//	     System.out.println("EVAL_HDS RESULT: \n" + output);
	     
	     String[] lines = output.split("[\\n\\r]+");
	     if (lines == null || lines.length < 2)
	    	 throw (new Exception("no lines in EvalHDS output " + output) );
	     
	     String[] fields = lines[1].split("\\s+");
	     System.out.println("fields: " + fields);
	     Double value = new Double(fields[2]);
	     return value;
	    
	     
	}
	
	/**
	 * 
	 * @return
	 */
	public TreeMap<Integer, Double> GetWinDiffPerLayer() throws Exception
	{
		
		SimpleFileDataSource ref = (SimpleFileDataSource)this.refDS;
		SimpleFileDataSource hypo = (SimpleFileDataSource)this.hypoDS;
		
		TreeMap<Integer, Double> res = new TreeMap<Integer, Double> ();
		
		Set<Integer> levels = ref.GetReferenceBreaksHierarchical().keySet();
		
		for (Integer curLevel: levels)
		{
			ArrayList<Integer> curRef = ref.GetReferenceBreaksAtLevel(0, curLevel);
			ArrayList<Integer> curHypo = null;
			try{
				curHypo = hypo.GetReferenceBreaksAtLevel(0, curLevel);
			}
			catch (Exception e)
			{
				System.out.println("Error getting level in hypo: " + curLevel);
				e.printStackTrace();
				continue;
			}
			
			if (curHypo == null)
				continue;
			
			
			Double value = this.ComputeWindowDiff(curRef, curHypo);
			res.put(curLevel, value);
			
		}
		
		return res;
	}
	
	public Double ComputeWindowDiff(ArrayList<Integer> refIndices, ArrayList<Integer> hypoIndices) throws Exception
	{
		//window size is half the average segment length in the reference segmentation
		
		//window size is half the average segment length in the reference segmentation
		int winSize = (int)Math.floor(this.refDS.GetNumberOfChunks() / (refIndices.size() * 2) );
		
		if (winSize <= 0)
			winSize = 1;
		
		int totalMismatches = 0;
		
		int[] ref = new int[this.refDS.GetNumberOfChunks()];
		int[] hypo = new int[this.refDS.GetNumberOfChunks()];
		
		for (int i = 0; i < ref.length; i++)
		{
			if (refIndices.contains(i))
				ref[i] = 1;
			else
				ref[i] = 0;
			
			if (hypoIndices.contains(i))
				hypo[i] = 1;
			else 
				hypo[i] = 0;
			}
		
		for (int winStart = 0; winStart < ref.length - winSize + 1; winStart++)
		{
			int winEnd = winStart + winSize - 1;
			int refCount = 0;
			int hypoCount = 0;
			
			for (int j = winStart;  j <= winEnd; j++)
			{
				if (ref[j] == 1)
					refCount++;
				if (hypo[j] == 1)
					hypoCount++;
			}
			
			if (refCount != hypoCount)
			{
					totalMismatches++;
			}

		}
		
		double wd = (double)totalMismatches / (double) (ref.length - winSize)  ;
		
		
//		System.out.println("computing WD: ");
//		System.out.println("ref " + refIndices.toString());
//		System.out.println("hypo " + hypoIndices.toString());
//		System.out.println("wd " + wd);
		
		return new Double (wd);
		
//				int winSize = (int)Math.floor(this.refDS.GetNumberOfChunks() / (refIndices.size() * 2) );
//				
//				int totalMismatches = 0;
//				for (int winStart = 0; winStart < this.refDS.GetNumberOfChunks() - winSize + 1; winStart++)
//				{
//					int winEnd = winStart + winSize - 1;
//					int refCount = 0;
//					int hypoCount = 0;
//					
//					for (int j = winStart;  j < winEnd; j++)
//					{
//						if (refIndices.contains(new Integer(j)))
//							refCount++;
//						if (hypoIndices.contains(new Integer(j)))
//							hypoCount++;
//					}
//					if (refCount != hypoCount)
//					{
//						totalMismatches++;
//					}
//				}
//				
//				double wd = (double)totalMismatches / (double) (this.refDS.GetNumberOfChunks() - winSize)  ;
//				return new Double (wd);
	}

}
