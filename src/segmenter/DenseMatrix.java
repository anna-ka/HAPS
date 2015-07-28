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

/**regular dense matrix, almost the same as two-dimensional array
 * 
 * the matrix is necessarily symmetrical, N*N, with N being number of points
 * **/

public class DenseMatrix implements IMatrix {
	
	private double matrix[][];
	private int numPoints = 0;
	
	public DenseMatrix(int numPoints)
	{
		this.matrix = new double[numPoints][numPoints];
		this.numPoints = numPoints;
	}

	public int GetColumnEnd(int columnIndex) {
		return this.numPoints - 1;
	}

	public int GetColumnStart(int columnIndex) {
		return 0;
	}

	public double GetElement(int row, int column)  throws Exception{
		return this.matrix[row][column];
	}

	public int GetRowEnd(int rowIndex) {
		return this.numPoints - 1;
	}

	public int GetRowStart(int rowIndex) {
		return 0;
	}

	public void SetColumnEnd(int columnIndex, int lastIndex) {
		// TODO Auto-generated method stub

	}

	public void SetColumnStart(int columnIndex, int firstIndex) {
		// TODO Auto-generated method stub

	}

	public void SetElement(int row, int column, double value) throws Exception{
		// TODO Auto-generated method stub
		this.matrix[row][column] = value;
	}

	public void SetRowEnd(int rowIndex, int lastIndex) {
		// TODO Auto-generated method stub

	}

	public void SetRowStart(int rowIndex, int firstIndex) {
		// TODO Auto-generated method stub

	}

	public int GetNumColumns() {
		return this.numPoints ;
	}

	public int GetNumRows() {
		return this.numPoints ;
	}

	public double[][] GetMatrix() {
		// TODO Auto-generated method stub
		return this.matrix;
	}

	public void PrintAbstractMatrix() {
		// TODO Auto-generated method stub
		
	}

	public void PrintActualMatrix() {
		// TODO Auto-generated method stub
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

	public void PrintOffsets() {
		// TODO Auto-generated method stub
		
	}


}
