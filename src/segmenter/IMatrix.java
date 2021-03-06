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



package segmenter;


import java.io.Serializable;

public interface IMatrix extends Serializable{
	
	double GetElement(int row, int column) throws Exception;
	void SetElement(int row, int column, double value) throws Exception;
	
	//get the indices of the first and the last elements in this row
	int GetRowStart(int rowIndex);
	int GetRowEnd(int rowIndex);
	
	void SetRowStart(int rowIndex, int firstIndex);
	void SetRowEnd(int rowIndex, int lastIndex);
	
	//indices of the first and last elements in the column
	int GetColumnStart(int columnIndex);
	int GetColumnEnd(int columnIndex);
	
	void SetColumnStart(int columnIndex, int firstIndex);
	void SetColumnEnd(int columnIndex, int lastIndex);
	
	int GetNumColumns();
	int GetNumRows();
	
	double[][] GetMatrix();
	
	void PrintOffsets();
	void PrintActualMatrix();
	public void PrintAbstractMatrix();
	
	
}
