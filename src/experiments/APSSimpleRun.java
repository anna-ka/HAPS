package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import segmenter.AbstractAPSegmenterDP;
import similarity.IGenericSimComputer;
//import segmenter.IGenericDataSource;
//import segmenter.SimpleFileDataSource;
//import segmenter.SimpleFileMultipleAnnotsDataSource;
import datasources.IGenericDataSource;
import datasources.SimpleFileDataSource;
import datasources.SimpleFileMultipleAnnotsDataSource;

import evaluation.AbstractEvaluator;
import evaluation.FournierBoundarySimEvaluator;
import evaluation.MultWinDiffEvaluator;
import evaluation.SimpleWinDiffEvaluator;


/**
 * 
 * @author anna
 * 
 * A single run of APS on a set of SimpleFileDataSource annotations
 *
 */
public class APSSimpleRun extends AbstractAPSRun {
	


	
	/**
	 * 
	 * 
	 */
	public Double EvaluateDocument( File curInputFile, int dataType, AbstractParameterDictionary runParams) throws Exception
	{

		
		AbstractParameterDictionary  paramsInUse = null;
		if ( runParams == null)
			paramsInUse = this.curParams;
		else
			paramsInUse = runParams;
		
		Double inputDataType = paramsInUse.GetNumericValue("inputDataType");
		
		IGenericDataSource curDS = this.freqMap.get(curInputFile).getDataSource();
		
		IGenericDataSource hypoSegmentation = null;
		
		if (inputDataType == IGenericDataSource.SIMPLE_DS)
		{
			hypoSegmentation = new SimpleFileDataSource();		
			hypoSegmentation.LightWeightInit(curDS.GetNumberOfChunks());
		}
		else if (inputDataType == IGenericDataSource.SIMPLE_MULTIPLE_ANNOTS_DS)
		{
			hypoSegmentation = new SimpleFileMultipleAnnotsDataSource();
			hypoSegmentation.LightWeightInit(curDS.GetNumberOfChunks());	
		}
		else
		{
			throw (new Exception ("Unknown inputDataType in APSSimpleRun.EvaluateDocument: " + inputDataType));
		}
		
		
		//build the matrix of similarities
		IGenericSimComputer simComp = BuildSimMatrix(curInputFile, dataType, paramsInUse);
		
		Double curPref = paramsInUse.GetNumericValue("preference");
		Double curDamp = paramsInUse.GetNumericValue("damping");
		Boolean sparse = paramsInUse.GetBooleanValue("sparse");
		
		AbstractAPSegmenterDP segmenter = this.CreateAffinityPropagationSegmenter(simComp, 
				curPref,
				curDamp,
				sparse );
		
		simComp.dispose();
		simComp = null;
		
		segmenter.Run();
		System.out.println("APS finished " + curDS.GetName());
		
//		System.out.println("WARNING: DEBUG MODE, SEGMENTATIONS are RANDOM");
		TreeMap<Integer, TreeSet<Integer>> assigns;
		assigns = segmenter.GetNonConflictingAssignments();
		
		segmenter.PrintAssignments();
		
		
		Integer[] hypo = segmenter.GetHypoBreaks(assigns);
		
		ArrayList<Integer> hypoBreaks = new ArrayList<Integer>();
		for (Integer el: hypo)
			hypoBreaks.add(el);
		
//		ArrayList<Integer> hypoBreaks = hypoSegmentation.GenerateRandomReferenceBreaks(2);
		
		//we use annotID=1 here because there is only one hypo segmentation
		hypoSegmentation.SetReferenceBreaks(1, hypoBreaks);
		
		AbstractEvaluator evaluator;
		
		String evalMetric = paramsInUse.GetStringValue("evalMetric");
		if (evalMetric.compareTo("winDiff") == 0)
		{
			evaluator= new SimpleWinDiffEvaluator();
		}
		else if (paramsInUse.GetStringValue("evalMetric").compareTo("multWDUnnorm") == 0)
		{
			evaluator= new MultWinDiffEvaluator();	
			
		}
		else if (evalMetric.compareTo("multWDNorm") == 0)
		{
			evaluator= new MultWinDiffEvaluator();	
			MultWinDiffEvaluator ev = (MultWinDiffEvaluator)evaluator;
			ev.SetMetricType(MultWinDiffEvaluator.MULTWD_TYPE.MULTWD_NORM);
			evaluator = ev;
			
		}
		else if (evalMetric.compareTo("wdMajority") == 0)
		{
			evaluator= new MultWinDiffEvaluator();	
			MultWinDiffEvaluator ev = (MultWinDiffEvaluator)evaluator;
			ev.SetMetricType(MultWinDiffEvaluator.MULTWD_TYPE.MAJORITY_OPINION);
			evaluator = ev;
			
		}
		else if (evalMetric.compareTo("boundarySim") == 0)
		{
			evaluator= new FournierBoundarySimEvaluator();
			//evaluator.Init("boundarySim");
			
		}
		else 
		{
			Exception e2 = new Exception ("Unknown evaluation metric in APSSImpleRun.EvaluateDocument: " + paramsInUse.GetStringValue("evalMetric"));
			throw (e2);
		}
		
		evaluator.SetMetricName(evalMetric);
		
		evaluator.Init(evalMetric);
		evaluator.SetRefDS(curDS);
		evaluator.SetHypoDS(hypoSegmentation);
		
		
		
		if (evaluator.VerifyCompatibility() != AbstractEvaluator.COMPATIBLE )
		{
			String msg = "Exception in APSSimpleRUn.EvaluateDocument. Reference and hypo DS are not compatible. evaluator.VerifyCompatibility() failed.";
			throw (new Exception(msg));
		}
		else if (evaluator.SpecificVerifyCompatibility() != AbstractEvaluator.COMPATIBLE )
		{
			String msg = "Exception in APSSimpleRun.EvaluateDocument. Reference and hypo DS are not compatible. evaluator.SpecificVerifyCompatibility() failed.";
			throw (new Exception(msg));
		}
		
		if (evalMetric.compareTo("boundarySim") == 0)
		{
			FournierBoundarySimEvaluator ev = (FournierBoundarySimEvaluator)evaluator;
			
			//SimpleFileDataSource dummyRef = new SimpleFileData
			
			ev.SetUpReference();
			evaluator = ev;
		}
		
		System.out.println("Reference: " + curDS.GetReferenceBreaks(1));
		System.out.println("Hypo: " + hypoSegmentation.GetReferenceBreaks(1));
		
		Double wd = evaluator.ComputeValue();
		System.out.println("winDiff: " + wd.toString());
		
		
		return wd;
	}

	
	
}	
