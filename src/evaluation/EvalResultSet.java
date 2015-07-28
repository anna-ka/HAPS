package evaluation;

import java.util.Iterator;
import java.util.TreeMap;
import java.io.File;


/**
 * 
 * @author anna
 *a class to hold the results of a single exp. run.
 *it contains per files results as well as an average
 */
public class EvalResultSet implements Comparable<EvalResultSet> {
	
	private TreeMap<File, Double> results;
	private Double averageValue = null;
	private Double stdDev = null;
	private String metricName = "unknown";
	
	
	public EvalResultSet()
	{}
	
	public void Init(File[] filesToProcess)
	{
		this.results = new TreeMap<File, Double>();
		for (File curFile: filesToProcess)
		{
			this.results.put(curFile, new Double(-1));
		}
	}
	
	public String GetMetricName()
	{
		return this.metricName;
	}
	
	public void SetMetricName(String newName)
	{
		this.metricName = newName;
	}
	
	public void MapResult(File curFile, Double curValue)
	{
		this.results.put(curFile, curValue);
	}
	
	public int GetNumberOfFiles()
	{
		return this.results.size();
	}
	
	public Double GetAverageValue()
	{
		if (this.averageValue != null)
			return this.averageValue;
		
		Double ave = new Double(0);
		for (Double el: this.results.values())
			ave += el;
		ave = ave / this.results.size();
		this.averageValue = ave;
		return ave;
	}
	
	
	public Double GetStdDev()
	{
		if (this.stdDev != null)
			return this.stdDev;
		
		Double std = new Double(0);
		Double ave = this.GetAverageValue();
		Double var = new Double(0);
		
		for (Double el: this.results.values())
		{
			var = var + Math.pow((ave - el), 2);
		}
		
		var = var / this.results.size();
		std = Math.sqrt(var);
		this.stdDev = std;
		return std;
	}
	
	public Double GetResultForFile(String fileName) throws Exception
	{
		return this.results.get(fileName);
	}
	
	public void PrintResults()
	{
		
		StringBuilder text = new StringBuilder();
		
		for (File key: this.results.descendingKeySet())
		{
			text.append(key.getAbsolutePath() + ":\t" + this.results.get(key).toString() + "\n");
		}
		text.append("MEAN:\t" + this.GetAverageValue() + "\n");
		text.append("STD DEV:\t" + this.GetStdDev() + "\n");
		
		System.out.println(text.toString());
	}
	
	public String GetResultsString()
	{
		
		StringBuilder text = new StringBuilder();
		
		for (File key: this.results.descendingKeySet())
		{
			text.append(key.getAbsolutePath() + ":\t" + this.results.get(key).toString() + "\n");
		}
		text.append("MEAN:\t" + this.GetAverageValue() + "\n");
		text.append("STD DEV:\t" + this.GetStdDev() + "\n");
		
		return(text.toString());
	}


	//@Override
	public int compareTo(EvalResultSet second) {
		
		
		return (int) (this.GetAverageValue() - second.GetAverageValue());		
	}

}
