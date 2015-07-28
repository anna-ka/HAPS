package experiments;

import java.util.Set;
import java.util.TreeMap;

import evaluation.EvalResultSet;
import evaluation.EvalResultSetComparator;

/**
 * 
 * @author anna
 * This class encapsulates the mapping between different parameters (AbstractPatameterDictionary) for a run
 * and the actual result of a run (EvalResultSet)
 *
 */
public class EvalResultMap {
	
	TreeMap<Integer, EvalResultSet> idToEvalResult ;
	TreeMap <Integer, AbstractParameterDictionary> idToParams ;
	
	/**
	 * counter to keep track of ids
	 */
	int lastId= -1;
	
	/**
	 * info a about the best result
	 */
	int bestId = -1;
	EvalResultSet bestResult = null;
	
	public EvalResultMap()
	{
		this.idToEvalResult = new TreeMap<Integer, EvalResultSet>();
		this.idToParams = new TreeMap <Integer, AbstractParameterDictionary>();
	}
	
	public int AddEntry(EvalResultSet result, AbstractParameterDictionary params) throws Exception
	{
		
		String msg;
		
		if (result == null)
		{
			msg = "Exception in EvalResultMap.AddEntry: result value is null";
			throw (new Exception(msg));
		}
		else if (params == null)
		{
			msg = "Exception in EvalResultMap.AddEntry: params value is null";
			throw (new Exception(msg));
		}
		
		int newId = lastId + 1;
		lastId++;
		
		this.idToEvalResult.put(new Integer(newId), result);
		this.idToParams.put(new Integer(newId), params);
		
		EvalResultSetComparator compare = new EvalResultSetComparator ();
		//is this the best result so far?
		if ( this.bestResult == null)
		{
			this.bestId = newId;
			this.bestResult = result;
		}
		//else if ( compare.compare(this.bestResult, result) < 0)
		else if ( compare.compare(this.bestResult, result) > 0)
		{
			this.bestResult = result;
			this.bestId = newId;
		}

		return newId;
	}
	
	public Set<Integer> GetAllIds()
	{
		return this.idToEvalResult.keySet();
	}
	
	public EvalResultSet GetResult(int id) throws Exception
	{
		if (this.idToEvalResult.keySet().contains(new Integer(id)) == false)
		{
			throw (new Exception ("Id " + String.valueOf(id) + "is not found in the idToResult map"));
		}
		return this.idToEvalResult.get(new Integer(id));
	}
	
	public AbstractParameterDictionary GetParams(int id) throws Exception
	{
		if (this.idToParams.keySet().contains(new Integer(id)) == false)
		{
			throw (new Exception ("Id " + String.valueOf(id) + "is not found in the idToParams map"));
		}
		return this.idToParams.get(new Integer(id));
	}
	
	public int GetBestId()
	{
		return this.bestId;
	}
	
	public EvalResultSet GetBestResult()
	{
		return this.bestResult;
	}
	
	public int GetNumEntries()
	{
		return this.idToEvalResult.size();
	}
	
}

