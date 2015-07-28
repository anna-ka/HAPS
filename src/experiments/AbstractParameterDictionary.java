package experiments;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import evaluation.EvalResultSet;

import similarity.GenericDocument;

/**
 * an abstract class that holds parameters for any of the segmenters.
 * 
 * Specific implementations must implement 
 * 
 * @author anna
 *
 */
public abstract class AbstractParameterDictionary implements Cloneable {
	
	protected TreeMap<String, ArrayList<Object>> paramValueDictionary ;
	
	public AbstractParameterDictionary()
	{
		this.paramValueDictionary = new TreeMap <String, ArrayList<Object>> ();
	}
	
	public Set<String> GetParameterNames()
	{
		return this.paramValueDictionary.keySet();
	}
	
//	@Override
//	public int compareTo(AbstractParameterDictionary second) {
//		Set<String> labels = this.paramValueDictionary.keySet();
//		for (String label: labels)
//		{
//			
//		}
//		
//		
//	}
	
	@Override
	/**
	 * creates a deep clone of the label-> values papping in the dictionary
	 */
	public Object clone() throws CloneNotSupportedException {
		AbstractParameterDictionary cloned = (AbstractParameterDictionary)super.clone();
		
		TreeMap<String, ArrayList<Object>> clonedDict = new TreeMap<String, ArrayList<Object>>();
		
		for (String label: this.paramValueDictionary.keySet())
		{
			ArrayList<Object> clonedValues = new ArrayList<Object>();
			ArrayList<Object> oldValues = this.paramValueDictionary.get(label);
			
			for (Object value: oldValues)
			{
				clonedValues.add(value);
			}
			
			clonedDict.put(label, clonedValues);			
		}
		cloned.paramValueDictionary = clonedDict;
		
		return cloned;
	}

	/**
	 * Returns a parameter associated with a single numeric value.
	 * @param paramName
	 * @return
	 * @throws Exception
	 */
	public Double GetNumericValue(String paramName) throws Exception
	{
		String msg = "Parameter " + paramName + " is not present in the dictionary";
		
		if (this.paramValueDictionary.containsKey(paramName))
		{
			Double value = (Double) this.paramValueDictionary.get(paramName).get(0);
			return value;
		}
		else 
		{
			return null;
		}
		
	}
	
	/**
	 * Returns a parameter value associated with a list of numeric values (as ArrayList<Double)
	 * @param paramName
	 * @return
	 * @throws Exception
	 */
	public ArrayList<Double> GetNumericList(String paramName) throws Exception
	{
		String msg = "Parameter " + paramName + " is not present in the dictionary";
		
		if (this.paramValueDictionary.containsKey(paramName))
		{
			ArrayList<Object> vals = (ArrayList<Object>) this.paramValueDictionary.get(paramName);
			
			ArrayList<Double> values = new ArrayList<Double>();
			for (Object v: vals)
			{
				values.add((Double)v);
			}
			
			return values;
		}
		else 
		{
			return null;
		}
	}
	
	/**
	 * Specify the value of a parameter associated with a numeric value
	 * @param paramName
	 * @param value
	 */
	public void SetValue(String paramName, Object value)
	{
		ArrayList<Object> list = new ArrayList<Object>();
		list.add(value);
		
		this.paramValueDictionary.put(paramName, list);
	}
	
	
	/**
	 * Specify the value of a parameter associated with a list of values
	 * @param paramName
	 * @param values
	 */
	public void SetList(String paramName, ArrayList<Object> values)
	{
		
		this.paramValueDictionary.put(paramName, values);
		
	}
	
	/**
	 * If a parameter with paramName exists in the dictionary, the method adds value to the list associated with it.
	 * Otherwise an exception in thrown.
	 * @param paramName
	 * @param value
	 * @throws Exception
	 */
	public void AddValueToList(String paramName, Object value) throws Exception
	{
		String msg = "The dictionary contains no parameters associated with this paramName: " + paramName; 
		if (this.paramValueDictionary.containsKey(paramName) == false)
		{
			Exception e = new Exception (msg);
			throw (e);
		}
		
		this.paramValueDictionary.get(paramName).add( value);
		
	}
	
	/**
	 * Initialise a data structure for a parameter associated with a list of values
	 * @param paramName
	 */
	public void InitializeList(String paramName)
	{
		this.paramValueDictionary.put(paramName, new ArrayList<Object>());
	}
	
	/**
	 * returns the value of the parameter associated with a String value
	 * @param paramName
	 * @return
	 * @throws Exception
	 */
	public String GetStringValue(String paramName) throws Exception
	{
		String msg = "String arameter " + paramName + " is not present in the dictionary";
		
		if (this.paramValueDictionary.containsKey(paramName))
		{
			String value = (String) this.paramValueDictionary.get(paramName).get(0);
			return value;
		}
		else 
		{
			return null;
		}
	}
	
	public ArrayList<String> GetStringList(String paramName) throws Exception
	{
		String msg = "String parameter " + paramName + " is not present in the dictionary";
		
		if (this.paramValueDictionary.containsKey(paramName))
		{
			ArrayList<Object> vals = (ArrayList<Object>) this.paramValueDictionary.get(paramName);
			
			ArrayList<String> values = new ArrayList<String>();
			for (Object v: vals)
			{
				values.add((String)v);
			}
			
			return values;
		}
		else 
		{
			return null;
		}
		
	}
	
	public Object GetObjectValue(String paramName) throws Exception
	{
		String msg = "String arameter " + paramName + " is not present in the dictionary";
		
		if (this.paramValueDictionary.containsKey(paramName))
		{
			return ( this.paramValueDictionary.get(paramName).get(0));
			
		}
		else 
		{
			return null;
		}
	}
	
	public ArrayList<Object> GetObjectList(String paramName) throws Exception
	{
		String msg = "String parameter " + paramName + " is not present in the dictionary";
		
		if (this.paramValueDictionary.containsKey(paramName))
		{
			ArrayList<Object> vals = (ArrayList<Object>) this.paramValueDictionary.get(paramName);
			return vals;
		}
		else 
		{
			return null;
		}
		
	}
	
	
	public Boolean GetBooleanValue(String paramName) throws Exception
	{
	
			String msg = "Boolean parameter " + paramName + " is not present in the dictionary";
			
			if (this.paramValueDictionary.containsKey(paramName))
			{
				Boolean value = (Boolean) this.paramValueDictionary.get(paramName).get(0);
				return value;
			}
			else 
			{
				return null;
			}
	
	}
	
	
	public ArrayList<Boolean> GetBooleanList(String paramName) throws Exception
	{
		String msg = "Boolean parameter " + paramName + " is not present in the dictionary";
		
		if (this.paramValueDictionary.containsKey(paramName))
		{
			ArrayList<Object> vals = (ArrayList<Object>) this.paramValueDictionary.get(paramName);
			
			ArrayList<Boolean> values = new ArrayList<Boolean>();
			for (Object v: vals)
			{
				values.add((Boolean)v);
			}
			
			return values;
		}
		else 
		{
			return null;
		}
	}
	
	/**
	 * This method output current parameters that are in use
	 */
	public void PrintParametersInUse() throws Exception
	{
		StringBuilder str = new StringBuilder();
		
		for (String paramName: this.paramValueDictionary.keySet())
		{
			//System.out.println(paramName);
			ArrayList<Object> paramList = this.paramValueDictionary.get(paramName);
			Object firstEl = paramList.get(0);
			
			str.append(paramName + ": ");
			
			if (firstEl.getClass().getName().compareToIgnoreCase("java.lang.String") == 0)
			{
				for (Object el: paramList)
				{
					String value = (String) el;
					str.append(value + ", ");
				}
				str.append("\n");
			}
			else if (firstEl.getClass().getName().compareToIgnoreCase("java.lang.Double") == 0)
			{
				for (Object el: paramList)
				{
					Double value = (Double) el;
					str.append(value.toString() + ", ");
				}
				str.append("\n");
			}
			else if (firstEl.getClass().getName().compareToIgnoreCase("java.lang.Boolean") == 0)
			{
				for (Object el: paramList)
				{
					Boolean value = (Boolean) el;
					str.append(value.toString() + ", ");
				}
				str.append("\n");
			}
			else
			{
				String msg = "Unexpected class " + firstEl.getClass().getName() + " for parameter " + paramName;
				Exception e = new Exception (msg);
				throw (e);
			}
		}
		System.out.println(str.toString());
		
	}
	
	
	
	/**
	 * Concrete subclasses must implement this method. It must populate the dictionary with all relevant options from the command line
	 * @param args
	 */
	public abstract void ParseCommandLine(String[] args) throws Exception;
	
	/**
	 * Concrete subclasses must implement this method. It must populate the dictionary with all relevant options from the config file
	 * @param args
	 */
	public abstract void ParseConfigFile(String configPath) throws Exception;
	
	
}
