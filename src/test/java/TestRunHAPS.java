package test.java;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import experiments.APSParameterDictionary;
import experiments.HAPSCrossValidationExperiment;
import experiments.RunHAPS;

public class TestRunHAPS {
	
	String[] commandLineMoonstone = {"-useConfig", "./config/hier_moonstone_simple" };
//	String[] commandLineMoonstone = {"-useConfig", "./config/test_hier_moonstone.config" };

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test_Synopsys() throws Exception {
		
		
		
		RunHAPS runHaps = new RunHAPS();
		runHaps.params= new APSParameterDictionary();
		
		//we can evaluate using windowDiff per each level
		//the results for the top level of the tree are output in ./Results/<name-of-file-specified-in-config-file>
		//the results for each level within each fold are output in the directory specified as <outputDir> in the configuration file
		// in the file names test_fold_0winDiff.txt
		runHaps.params.ParseCommandLine(commandLineMoonstone);
		runHaps.params.SetValue("useWDPerLevel", new Boolean(true));
		
//		//or let's try using EvalHDS
		//however for that you need to set up Lucien Carroll's evalHDS software and point HAPS to it
		// see Lucien Carroll. 2010. Evaluating Hierarchical Discourse Segmentation. In Proceedings of NAACL10.
		
//		runHaps.params.SetValue("outputDir", "./Results/hier_moonstone_evalHDS");
//		runHaps.params.SetValue("useWDPerLevel", new Boolean(false));
		
		
		runHaps.params.PrintParametersInUse();
		
		
		
		HAPSCrossValidationExperiment exp = new HAPSCrossValidationExperiment();
		exp.Init(runHaps.params);
		
		exp.SetUseHoldoutSet(0);
		exp.SetNumberOfFolds(4);
		
		exp.CreateFolds();
		exp.PrintSetUp();
		exp.RunExperiment();
		
		System.out.println("Done!");

	}

}
