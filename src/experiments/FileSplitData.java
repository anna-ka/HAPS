package experiments;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import datasources.HierarchicalMultipleAnnotsDataSource;
import datasources.IGenericDataSource;


class FileSplitData {
	private static File tempFolder;
	private static String lineSep = System.getProperty("line.separator");
	public IGenericDataSource parentDS;
	public File curChunk;
	public int chunkStart;
	public int chunkEnd;

	public static void setTempFolder(File folder) {
		tempFolder = folder;
		if (!tempFolder.exists()) tempFolder.mkdir();
	}
	
	public FileSplitData(IGenericDataSource ds, File inputFile) {
		parentDS = ds;
		curChunk = inputFile;
		chunkStart = 0;
		chunkEnd = ds.GetNumberOfChunks() - 1;
	}
	
	public FileSplitData(IGenericDataSource ds, int start, int end) throws Exception {
		parentDS = ds;
		curChunk = File.createTempFile("aps", "", tempFolder);
		curChunk.deleteOnExit();
		chunkStart = start;
		chunkEnd = end;

		if (start < 0 || end >= ds.GetNumberOfChunks()) 
			throw new Exception("Segment boundaries out of range");
		
		FileWriter writer = new FileWriter(curChunk);
		
		try {
			for (int i = start; i <= end; i++) {
				if (ds.GetLevelOfSegm() == IGenericDataSource.PAR_LEVEL) {
					String text = ds.GetChunk(i);
					
					if (text.endsWith(lineSep)) {
						text = text.substring(0, text.length() - 1 - lineSep.length());
					}
					
					writer.write(text + lineSep);
				} else {
					writer.write(ds.GetChunk(i) + lineSep);
				}
			}
		} finally {
			writer.close();
		}
	}

	public int getNumRefSegs(int level) throws Exception {
		if (parentDS instanceof HierarchicalMultipleAnnotsDataSource) {
			return getNumRefSegs((HierarchicalMultipleAnnotsDataSource)parentDS, level);
		}
		
		int numSegs = 0;
	
		ArrayList<Integer> breaks = parentDS.GetReferenceBreaksAtLevel(0, level);
		if (breaks != null) {
			for (Integer br : breaks) {
				if (br < chunkStart || br >= chunkEnd) continue;
				
				numSegs++;
			}
		}
		
		numSegs++;
		
		return numSegs;
	}
	
	private int getNumRefSegs(HierarchicalMultipleAnnotsDataSource multiDS, int level) {
		int totalSegs = 0;
		int numAnot = multiDS.GetAnnotatorIds().size();
		
		for (Integer anotId : multiDS.GetAnnotatorIds()) {
			try {

				int numSegs = 0;
			
				ArrayList<Integer> breaks = multiDS.GetReferenceBreaksAtLevel(anotId, level);
				for (Integer br : breaks) {
					if (br < chunkStart || br >= chunkEnd) continue;
					
					numSegs++;
				}
				
				numSegs++;

				totalSegs += numSegs;
			} catch (Exception e) {
				numAnot--;
			}
		}
		
		return numAnot > 0 ? totalSegs / numAnot : 0;
	}

}
