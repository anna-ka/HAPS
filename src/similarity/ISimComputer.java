/*
 	APS - Affinity Propagation for Segmentation, a linear text segmenter.
 
    Copyright (C) 2011, Anna Kazantseva

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */


package similarity;

import java.io.File;
import java.util.LinkedList;
import datasources.IDataSource;

/**This interface is obsolete. Use IGenericSimComputer instead
 * an interface for building a list of similarities between all chunks of text in IDataSource.
 * 
 * This may correspond to either dense or sparse similarity matrix
 * 
 * */

public interface ISimComputer {
	
	int MAX_NUM_POINTS = -1; // maximum number of points that can be used for computing a dense similarities matrix
	ISimMetric simMetric = null;
	
	void Init(IDataSource data);
	void ComputeSimilarities() throws Exception;
	LinkedList<TripletSim> GetSimilarities();
	void OutputSimilarities(File outputDir);
	
	IDataSource GetRawData();
	int GetPointsNumber();
	int GetWindowSize();
	boolean GetIfSparse();
	
	

}
