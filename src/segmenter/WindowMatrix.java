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

/*an implementation of  a sparse matrix corresponding to sliding window
 * 
 * */

public class WindowMatrix implements IMatrix {
	
	public double matrix[][];
	private int numPoints = 0;
	private int windowSize = 0;
	
	// data structures to keep track of the indices of the first and last indices in each row and column
	
	//for each row, the index of the column with the first valid similarity value (valid != -INFINITY)
	private int rowStarts[];
	//for each row, the index of the column with the last valid similarity
	private int rowEnds[];
	//for each column, the index of the first row with a valid sim. value
	private int columnStarts[];
	private int columnEnds[];
	
	public WindowMatrix(int numPoints, int winSize)
	{
		this.numPoints = numPoints;
		this.windowSize = winSize;
		this.matrix = new double [numPoints][winSize];
		
		//although we physically only have windowSize columns, we need
		// to keep start/end indices for the abstract numPoint*numPoints matrix
		this.columnStarts = new int[this.numPoints];
		this.columnEnds = new int[this.numPoints];
		
		this.rowStarts = new int[this.numPoints];
		this.rowEnds = new int[this.numPoints];
	}
	
	public void SetColumnEnd(int columnIndex, int lastIndex) {
		this.columnEnds[columnIndex] = lastIndex;
	}

	public void SetColumnStart(int columnIndex, int firstIndex) {
		this.columnStarts[columnIndex] = firstIndex;
	}

	public void SetElement(int row, int column, double value) throws Exception {
		int offset = this.rowStarts[row];
		int modColIndex = column - offset;

		this.matrix[row][modColIndex] = value;
	}

	public void SetRowEnd(int rowIndex, int lastIndex) {
		this.rowEnds[rowIndex] = lastIndex;
	}

	public void SetRowStart(int rowIndex, int firstIndex) {
		this.rowStarts[rowIndex] = firstIndex;
	}

	public int GetColumnEnd(int columnIndex) {
		return this.columnEnds[columnIndex];
	}

	public int GetColumnStart(int columnIndex) {
		return this.columnStarts[columnIndex];
	}

	public double GetElement(int row, int column) throws Exception{

		if (row < 0 || row >= numPoints) {
			throw new Exception();
		}
		
		if (column < rowStarts[row] || column > rowEnds[row]) {
			throw new Exception();
		}

		int offset = this.rowStarts[row];
		int modColIndex = column - offset;
		
		return this.matrix[row][modColIndex];
	}

	public int GetNumColumns() {
		return this.windowSize;
	}

	public int GetNumRows() {
		return this.numPoints;
	}

	public int GetRowEnd(int rowIndex) {
		return this.rowEnds[rowIndex];
	}

	public int GetRowStart(int rowIndex) {
		return this.rowStarts[rowIndex];
	}

	public double[][] GetMatrix() {
		// TODO Auto-generated method stub
		return this.matrix;
	}

	public void CalcColumnBounds() {
		for (int i = 0; i < numPoints; i++) {
			columnStarts[i] = columnEnds[i] = -1;
		}
		
		for (int i = 0; i < numPoints; i++) {
			for (int j = 0; j < numPoints; j++) {
				if (j >= rowStarts[i] && j <= rowEnds[i]) {
					if (columnStarts[j] == -1) {
						columnStarts[j] = i;
					}
					columnEnds[j] = i;
				}
			}
		}
	}
	
	public void PrintActualMatrix()
	{
		System.out.println("\nActual matrix:");
		for (int row = 0; row < this.matrix.length; row++)
		{
			System.out.println("row " + String.valueOf(row));
			System.out.print("\t");
			for (int col = 0; col < this.GetNumColumns(); col++)
			{
				System.out.println(String.valueOf(col) + ": " + String.valueOf( this.matrix[row][col] ) + ", ");
			}
		}
	}
	
	public void PrintAbstractMatrix()
	{
		System.out.println("\nAbstract matrix:");
		for (int row = 0; row < this.numPoints; row++)
		{
			System.out.println("row " + String.valueOf(row));
			System.out.print("\t");
			for (int col = 0; col < this.numPoints; col++)
			{
				String val = "-";
				try{
					val = String.valueOf( this.GetElement(row, col) );
				}
				catch(Exception e)
				{
					val = "-INF";
				}
				System.out.println(String.valueOf(col) + ": " + val + ", ");
			}
		}
		
		
	}
	
	public void PrintOffsets()
	{
		System.out.println("\nRow starts and ends: ");
		for (int i = 0; i < this.rowStarts.length; i++)
		{
			System.out.print("row " + String.valueOf(i) + ": ");
			System.out.print("start " + this.GetRowStart(i));
			System.out.print("; end  " + this.GetRowEnd(i));
			System.out.println();
		}
		
		System.out.print("Column starts and ends: ");
		for (int i = 0; i < this.columnStarts.length; i++)
		{
			System.out.print("col " + String.valueOf(i) + ": ");
			System.out.print("start " + this.GetColumnStart(i));
			System.out.print("; end  " + this.GetColumnEnd(i));
			System.out.println();
		}
	}

	

}
