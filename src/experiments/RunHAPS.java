package experiments;

public class RunHAPS {
	
//	String[] commandLineMoonstone = {"-useConfig", "./config/test_hier_moonstone.config" };
	String[] commandLineMoonstone = {"-useConfig", "./config/hier_moonstone_simple" };
	String[] commandLineWikiForCV = {"-useConfig", "./config/test_hier_wiki_cv.config"};
	public APSParameterDictionary params ;
	
	public RunHAPS()
	{
		this.params = new APSParameterDictionary();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		RunHAPS runHaps = new RunHAPS();
		
		
		
		try {
			
			//runHaps.params.ParseCommandLine(runHaps.commandLineMoonstone);
			runHaps.params.ParseCommandLine(args);
			runHaps.params.SetValue("useWDPerLevel", new Boolean(true));
			
//			//or let's try using EvalHDS
//			runHaps.params.SetValue("outputDir", "./Results/hier_moonstone_evalHDS");
//			runHaps.params.SetValue("useWDPerLevel", new Boolean(false));
			
			
			runHaps.params.PrintParametersInUse();
			
			
			
			HAPSCrossValidationExperiment exp = new HAPSCrossValidationExperiment();
			exp.Init(runHaps.params);
			
			exp.SetUseHoldoutSet(0);
			exp.SetNumberOfFolds(4);
			
			exp.CreateFolds();
			exp.PrintSetUp();
			exp.RunExperiment();
			
			
			//now run WIKI
			if (1 > 0)
				return;
			
			exp = new HAPSCrossValidationExperiment();
			runHaps.params = new APSParameterDictionary();
			runHaps.params.ParseCommandLine(runHaps.commandLineWikiForCV);
			runHaps.params.SetValue("useHier", new Boolean(true));
//			runHaps.params.SetValue("useWDPerLevel", new Boolean(true));
			runHaps.params.PrintParametersInUse();
			
			
			exp.Init(runHaps.params);
			
			exp.SetUseHoldoutSet(0);
			exp.SetNumberOfFolds(10);
			
//			exp.CreateFolds();
//			exp.PrintSetUp();
//			exp.RunExperiment();
			
			//for the second experiment
			//wiki_haps_evalHDS_6_03_10cv
			runHaps.params.SetValue("outputDir", "./Results_Output/wiki_haps_wd_15_03_10cv");
			runHaps.params.SetValue("useWDPerLevel", new Boolean(true));
			
			HAPSCrossValidationExperiment exp2 = new HAPSCrossValidationExperiment();
			exp2.Init(runHaps.params);
			
			exp2.SetUseHoldoutSet(0);
			exp2.SetNumberOfFolds(10);
			
			exp2.CreateFolds();
			exp2.PrintSetUp();
			
			
			exp2.RunExperiment();
			
			
			

			
			
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

}
