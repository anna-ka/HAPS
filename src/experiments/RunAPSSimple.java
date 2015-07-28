package experiments;

import java.io.File;

//import org.junit.Before;

public class RunAPSSimple {
	
	String[] commandLineAI = {"-useConfig", "./config/ai_10fold_cv_21_12_2013.config" };
	String[] commandLinePhysics = {"-useConfig", "./config/physics_5cv_cosine_18_02_2014.config" };
	String[] commandLineClinical = {"-useConfig", "./config/clinical_10cv_23_12_2013.config" };
	String[] commandLineMoonstone = {"-useConfig", "./config/moonstone_19_02_2014_5cv_aps.config" };
	String[] commandLineFiction = {"-useConfig", "./config/fiction_10cv_19_02_2014.config" };
	
	
	public RunAPSSimple()
	{
		this.params = new APSParameterDictionary();
		this.simpleAPSExp = new SimpleAPSCrossValidationExperiment();
	}
	
	APSParameterDictionary params ;
	SimpleAPSCrossValidationExperiment simpleAPSExp;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			//-useConfig ./config/ai_10fold_cv_9_11_2013.config
			
			
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
			
			
			
			RunAPSSimple run = new RunAPSSimple();
			
			//multWDUnnorm
			run.params.ParseCommandLine(run.commandLineAI);
			run.params.PrintParametersInUse();
			
			run.simpleAPSExp.Init(run.params);
			
			
			
			
			int numFolds = 5;
			int numHoldout = 0;
			
			try{
//				run.simpleAPSExp.SetUseHoldoutSet(numHoldout);
//				run.simpleAPSExp.SetNumberOfFolds(numFolds);
//				
//				run.simpleAPSExp.CreateFolds();
//				run.simpleAPSExp.PrintSetUp();
//				
//				
//
//				run.simpleAPSExp.RunExperiment();
//				run.simpleAPSExp.OutputResults();
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			
			
			if (1 > 0)
			{
//				return;
			}
			
			System.out.println("Second experiment!");
			
			run = null;
			run = new RunAPSSimple();
//			
//			//run second experiment - physics
			
//			run.params.ParseCommandLine(run.commandLinePhysics);
//			run.simpleAPSExp.Init(run.params);
//			
//			run.simpleAPSExp.SetUseHoldoutSet(numHoldout);
//			run.simpleAPSExp.SetNumberOfFolds(numFolds);
//			
//			run.simpleAPSExp.CreateFolds();
//			run.simpleAPSExp.PrintSetUp();
//			
//			
//
//			run.simpleAPSExp.RunExperiment();
//			run.simpleAPSExp.OutputResults();
			
			
			//run third experiment clinical
			
			run = null;
			run = new RunAPSSimple();
			numFolds = 10;
			run.params.ParseCommandLine(run.commandLineClinical);
			run.simpleAPSExp.Init(run.params);
			
			run.simpleAPSExp.SetUseHoldoutSet(numHoldout);
			run.simpleAPSExp.SetNumberOfFolds(numFolds);
			
			run.simpleAPSExp.CreateFolds();
			run.simpleAPSExp.PrintSetUp();
			
			

			run.simpleAPSExp.RunExperiment();
			run.simpleAPSExp.OutputResults();
		}
		catch (Exception e)
		{
			System.out.println("Something went worng");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

}
