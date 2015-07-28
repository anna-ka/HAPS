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

/**
 * 
 */
package datasources;

import java.io.File;
import java.util.ArrayList;

/**
 * @author anna
 *
 */
public interface IDataSource {
	
	//constants to specify the basic units of segmentation
	final static int SENT_LEVEL = 1;
	final static int PAR_LEVEL = 2;

	
	int GetNumChunks();
	
	void Init(int basicUnits) throws Exception;
	
	String GetChunk(int index);
	
	String GetName();
	
	//get gold standard segment breaks
	Integer[] GetReferenceSegmentBreaks();
	
	void SetReferenceBreaks(Integer[] newRefBreaks);
	ArrayList<Integer> GenerateRandomReferenceBreaks( int numBreaks );
	
	void Output(File outputFile, Integer[] breaks) throws Exception;

}
