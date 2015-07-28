package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import datasources.HierarchicalMultipleAnnotsDataSource;
import datasources.SimpleFileDataSource;
import datasources.TextFileIO;


/**
 * a class for evaluating hierarchical datasources with multiple annotations.
 * 
 * It compared the hypothetical segmentation with all available reference segmentations
 * and then outputs the average
 * 
 * @author anna
 *
 */
public class HierMultAnnotEvaluator extends AbstractEvaluator {
	
	TreeMap<Integer, Double> evalDSPerAnnotator = new TreeMap<Integer, Double> ();
	TreeMap<Integer, TreeMap<Integer, Double>> winDiffPerAnnotatorPerLevel = new TreeMap<Integer, TreeMap<Integer, Double>>();
	
	File tmpOutputDir = null;
	int numLevels = 3;
	
	/**
	 * this value is used to discard suspiciously good evaluations of EvalHDS. These are usually rogue and dur to
	 * to one of the segmentations being very fine grained
	 */
	Double evalHSDOutliersCutOff = 0.15;
	String evalHDSPath = "/Users/anna/software/EvalHDS/eval/Beeferman.pu";
	
	boolean useWD = false;
	public void SetUseWD(boolean val)
	{
		this.useWD = val;
	}
	
	public boolean GetUseWD()
	{
		return this.useWD;
	}
	

	@Override
	public int SpecificVerifyCompatibility() {
		if(this.refDS.getClass().toString().endsWith("HierarchicalMultipleAnnotsDataSource") == false)
		{
			System.out.println("error in HierEvalHDSEvaluator.SpecificVerifyCompatibility. Reference DS must be of class HierarchicalMultipleAnnotsDataSource. "+
					"Actual class is " + this.refDS.getClass().toString());
			return super.NON_COMPATIBLE;
		}
		
		else if (this.hypoDS.GetIfHierarchical() == false)
		{
			System.out.println("error in HierEvalHDSEvaluator.SpecificVerifyCompatibility. Non hierarchical hypothetical " + this.hypoDS.GetName());
			return super.NON_COMPATIBLE;
		}
		
		return super.COMPATIBLE;
	}
	
	public Set<Integer> GetIdsOfRefAnnotators()
	{
		HierarchicalMultipleAnnotsDataSource ds = (HierarchicalMultipleAnnotsDataSource) this.refDS;
		return (ds.GetAnnotatorIds());
	}
	
	/**
	 * 
	 * @param tmpDir
	 * @param algorithmName
	 * @throws Exception
	 */
	public void SetUp(File tmpDir, String algorithmName) throws Exception
	{
		
		HierarchicalMultipleAnnotsDataSource rDS = (HierarchicalMultipleAnnotsDataSource)this.refDS;
		SimpleFileDataSource hDS = (SimpleFileDataSource)this.hypoDS;
		
		//output reference files for each available annotator
		//the references will be written to tpmDir/chX/ref/chX_anX.txt
		
		if (tmpDir.exists() == false || tmpDir.isDirectory() == false)
			tmpDir.mkdir();
		
		this.tmpOutputDir = tmpDir;
		
		
		File refDir = new File(tmpDir, "ref");
		if (refDir.exists() == false || refDir.isDirectory() == false)
		{
			refDir.mkdir();
		}
		
		File hypoDir = new File(tmpDir, "hypo");
		File hypoFile = new File(hypoDir, rDS.GetShortName() + ".txt");
		if (hypoDir.exists() == false || hypoDir.isDirectory() == false)
		{
			hypoDir.mkdir();
		}
		
		
		rDS.OutputFullTextHierarchical(refDir);
		
		//output the hypothetical segmentation
//		rDS.OutputHierWithHypoBreaks(hypoFile, hDS.HAPSBreaksToCarrolStyle(hDS.GetReferenceBreaksHierarchical()));
		rDS.OutputHierWithHypoBreaks(hypoFile, hDS.GetReferenceBreaksHierarchical());
		
		//output evalHDS options files
		for (Integer curAnnot: this.GetIdsOfRefAnnotators())
		{
			//do not consider annotations with less then 3 levels
			TreeMap<Integer, ArrayList<Integer>> curAnnotation = rDS.GetReferenceBreaks().get(curAnnot);
//			if (curAnnotation.keySet().size() < 3)
//				continue;
			
			File curRefFile = new File (tmpDir.getCanonicalFile() + "/ref" + "/" + rDS.GetShortName() + "_an" + curAnnot.toString() + ".txt");
			File curOptsFile = new File(tmpDir, rDS.GetShortName() + "_an" + curAnnot.toString() + ".opts");
			this.GenerateEvalHDSOptFiles(curOptsFile, curRefFile, hypoFile, algorithmName);
		}
		
	}

	/**
	 * runs evalHDS against all available annotations and returns the mean value
	 */
	@Override
	public Double ComputeValue() throws Exception {
		
		Integer topLevel = 2;
		
		if (this.GetUseWD() == true)
		{
			this.ComputeWinDiffPerLevel();
			TreeMap<Integer, Double> ave = this.GetAverageWDPerLevel();
			return ave.get(topLevel);
		}
		
		HierarchicalMultipleAnnotsDataSource rDS = (HierarchicalMultipleAnnotsDataSource)this.refDS;
		SimpleFileDataSource hDS = (SimpleFileDataSource)this.hypoDS;
		Double netValue = new Double(0);
		Double numAnnot = new Double(this.GetIdsOfRefAnnotators().size());
		int annotCounter = 0;
		
		for (Integer curAnnot: this.GetIdsOfRefAnnotators())
		{
			TreeMap<Integer, ArrayList<Integer>> curAnnotation = rDS.GetReferenceBreaks().get(curAnnot);
//			if (curAnnotation.keySet().size() < 3)
//				continue;
			
			File curOptsFile = new File(this.tmpOutputDir, rDS.GetShortName() + "_an" + curAnnot.toString() + ".opts");
			Double curVal = this.EvalHDS(curOptsFile);
			
			if (curVal <= this.evalHSDOutliersCutOff)
			{
				System.out.println("Discarding evalHDS result for annotator " + curAnnot + " : " + curVal);
				continue;
			}
			
			
			this.evalDSPerAnnotator.put(curAnnot, curVal);
			netValue += curVal;
			annotCounter++;
			System.out.println("Annot - " + curAnnot + ",  evalHDS - " + curVal.toString());
			System.out.println ("REF: " + rDS.PrintRefBreaks(curAnnot));
			System.out.println ("HYPO: " + hDS.PrintRefBreaks());
		}
		Double v = netValue / new Double(annotCounter);
		System.out.println("AVE for " + this.refDS.GetName() + ": " + v);
		return (v);
		
	}
	
	/**
	 * this method computes winDiff for each level of the segmentation
	 */
	public TreeMap<Integer, TreeMap<Integer, Double>> ComputeWinDiffPerLevel() throws Exception
	{
		
		HierarchicalMultipleAnnotsDataSource rDS = (HierarchicalMultipleAnnotsDataSource)this.refDS;
		
		TreeMap<Integer, TreeMap<Integer, Double>> res = new TreeMap<Integer, TreeMap<Integer, Double>>();
		
		for (int i = 0; i < this.numLevels; i++)
		{
			Integer carrolLevel = new Integer(i + 2);
			
			ArrayList<Integer> curLevelHypo = this.hypoDS.GetReferenceBreaksAtLevel(0, carrolLevel);
			 
			for (Integer curAnnot: this.GetIdsOfRefAnnotators())
			{
				ArrayList<Integer> curRefBreaks = null;
				
				try{
					curRefBreaks = rDS.GetReferenceBreaksAtLevel(curAnnot, carrolLevel);
				}
				catch (Exception e)
				{}
				
				if (curRefBreaks == null)
					continue;
				Double wdValue = this.ComputeWindowDiff(curRefBreaks, curLevelHypo);
				
				if (res.containsKey(curAnnot) == false)
				{
					res.put(curAnnot, new TreeMap<Integer, Double>());
				}
				
				TreeMap<Integer, Double> curLevelToWD = res.get(curAnnot);
				curLevelToWD.put(carrolLevel, wdValue);
				res.put(curAnnot, curLevelToWD);
				
			}		
		}
		
		this.winDiffPerAnnotatorPerLevel = res;
		return this.winDiffPerAnnotatorPerLevel;	
	}
	
	public TreeMap<Integer, Double> GetAverageWDPerLevel() throws Exception
	{
		if (this.winDiffPerAnnotatorPerLevel == null)
			this.ComputeWinDiffPerLevel();
		
		TreeMap<Integer,Double> res = new TreeMap<Integer,Double>();
		
		
		for (int i = 0; i < this.numLevels; i++)
		{
			Integer carrolLevel = new Integer(i + 2);
			Double netWD = new Double(0);
			int annotCounter = 0;
			
			for (Integer curAnnot: this.GetIdsOfRefAnnotators())
			{
				TreeMap <Integer, Double> curRes = this.winDiffPerAnnotatorPerLevel.get(curAnnot);
				
				if (curRes.containsKey(carrolLevel) == false)
					continue;
				else if (curRes.get(carrolLevel) == 0)
					continue;
				
				Double curWD = curRes.get(carrolLevel);
				netWD += curWD;
				annotCounter++;
			}
			
			Double aveWD = netWD / (new Double(annotCounter));
			res.put(carrolLevel, aveWD);
		}
		
		return res;
	}
	
	public void GenerateEvalHDSOptFiles(File optFile, File goldStandardPath, File hypoSegmPath, String algoName) throws Exception
	{
		
//		System.out.println("opts file: " + optFile);
//		System.out.println("ref file: " + goldStandardPath);
//		System.out.println("hypo file: " + hypoSegmPath);
		
		
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
		
//		System.out.println(str.toString());
		TextFileIO.OutputFile(optFile, str.toString());
		
	}
	
	/**
	 * Runs evalHDS on one opts file and returns the value
	 * @return
	 * @throws Exception
	 */
	public Double EvalHDS(File optionsFile) throws Exception
	{
		
		ProcessBuilder pb = new ProcessBuilder("python", evalHDSPath, "-wal", optionsFile.getCanonicalPath());
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
	
	public Double ComputeWindowDiff(ArrayList<Integer> refIndices, ArrayList<Integer> hypoIndices) throws Exception
	{
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
				
				
				System.out.println("computing WD: ");
				System.out.println("ref " + refIndices.toString());
				System.out.println("hypo " + hypoIndices.toString());
				System.out.println("wd " + wd);
				
				return new Double (wd);
				
				

	}
	
	public int GetNumLevels()
	{
		return this.numLevels;
	}

}
