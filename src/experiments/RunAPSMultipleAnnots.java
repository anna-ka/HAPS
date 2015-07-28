package experiments;

//moonstone_small_multwindiff_unnorm_nosegm_18_12_2013.config
public class RunAPSMultipleAnnots {
	
	String[] commandLineMoonstone = {"-useConfig", "./config/moonstone_19_02_2014_5cv_aps.config" };
	
	public RunAPSMultipleAnnots()
	{
		this.params = new APSParameterDictionary();
		this.simpleAPSExp = new SimpleAPSCrossValidationExperiment();
	}
	
	APSParameterDictionary params ;
	SimpleAPSCrossValidationExperiment simpleAPSExp;
	
	public static void main(String[] args) {
		try{
			int mb = 1024*1024;
	         
	        //Getting the runtime reference from system
	        Runtime runtime = Runtime.getRuntime();
	         
	        System.out.println("##### Heap utilization statistics [MB] #####");
	         
	        //Print used memory
	        System.out.println("Used Memory:"
	            + (runtime.totalMemory() - runtime.freeMemory()) / mb);
	 
	        //Print free memory
	        System.out.println("Free Memory:"
	            + runtime.freeMemory() / mb);
	         
	        //Print total available memory
	        System.out.println("Total Memory:" + runtime.totalMemory() / mb);
	 
	        //Print Maximum available memory
	        System.out.println("Max Memory:" + runtime.maxMemory() / mb);
			
			
			
	        RunAPSMultipleAnnots run = new RunAPSMultipleAnnots();
			
			run.params.ParseCommandLine(run.commandLineMoonstone);
			
//			File inputDir = new File (run.params.GetStringValue("inputDir"));
//			File[] inputFiles = inputDir.listFiles();
			
			run.simpleAPSExp.Init(run.params);
//			run.simpleAPSExp.SetAvailFiles(inputFiles);
			
			
			
			
			int numFolds = 5;
			int numHoldout = 0;
			
			run.simpleAPSExp.SetUseHoldoutSet(numHoldout);
			run.simpleAPSExp.SetNumberOfFolds(numFolds);
			
			run.simpleAPSExp.CreateFolds();
			run.simpleAPSExp.PrintSetUp();
			
			
//wdMajority multWDUnnorm 
			run.simpleAPSExp.RunExperiment();
			run.simpleAPSExp.OutputResults();
			run = null;
			
	        
			
			//second experiemnt, using unnormalized multWInDIff
			//moonstone_small_no_segm_boundary_sim
			
//			RunAPSSimple run2 = new RunAPSSimple();
//			
//			
//			
//			run2.params.ParseCommandLine(run2.commandLineMoonstone);
//			run2.params.SetValue("evalMetric", "multWDUnnorm");
//			run2.params.SetValue("outputDir", "./Results_Output/moonstone_aps_5cv_19_02_2014_unnorm_mwd");
//			
//			run2.simpleAPSExp.Init(run2.params);
//			run2.simpleAPSExp.SetUseHoldoutSet(numHoldout);
//			run2.simpleAPSExp.SetNumberOfFolds(numFolds);
//			
//			run2.simpleAPSExp.CreateFolds();
//			run2.simpleAPSExp.PrintSetUp();
//			
//			
//
//			run2.simpleAPSExp.RunExperiment();
//			run2.simpleAPSExp.OutputResults();
			
		}
		catch (Exception e)
		{
			System.out.println("Exception " + e.getMessage());
			e.printStackTrace();
		}
	}

}
