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


package datasources;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;



public class TextFileIO {
	
	
	public static String ReadTextFile(File input) throws Exception
	{
		FileReader reader = null;
		StringBuilder text = new StringBuilder();
		try{
			reader = new FileReader(input);
			char[] buffer = new char[4096];
			int len = -1;
			while ( (len = reader.read(buffer)) != -1 )
			{
				text.append(buffer, 0, len);
			}
			return text.toString();
		}
		catch (IOException e)
		{
			System.out.println("TextFileIO: Could not read " + input.getAbsolutePath());
			System.out.println(e.getMessage());
			//return null;
			throw (new Exception (e));
		}
		catch (Exception e)
		{
			System.out.println("Exception in TextFileIO: " + e.getMessage());
			//return null;
			throw (new Exception (e));
		}
		finally
		{
			reader.close();
		}
	}
	
	//a method to break a pieces of text into lines and put each line as an element of array
	public static String[] LinesToArray(String text) throws Exception
	{
		String[] lines = text.split("\\n");
		return lines;
	}
	
	public static void AppendToFile(File outputFile, String text) throws Exception
	{
		StringBuilder str = new StringBuilder();
		if (outputFile.exists())
			str.append(TextFileIO.ReadTextFile(outputFile));
		str.append(text);
		
		FileWriter fr = null;
		try
		{
			fr = new FileWriter(outputFile);
			//fr.write(text);
			fr.append(str.toString());
			fr.close();
		}		
		catch(IOException e)
		{
			System.out.println("IOException in TextFileIO.AppendToFile: " + e.getMessage());
			throw (new Exception (e));
		}
	}
	
	public static void OutputFile(File outputFile, String text) throws Exception
	{
		FileWriter fr = null;
		try
		{
			fr = new FileWriter(outputFile);
			fr.write(text);
		}		
		catch(IOException e)
		{
			System.out.println("IOException in TextFileIO.OutputFile: " + e.getMessage());
			throw (new Exception (e));
		}
		finally
		{
			try
			{
    			if (fr != null)
    				fr.close();
			}
			catch (IOException e)
			{
				System.out.println("IOException in CreateCorpus.OutputFile 2: " + e.getMessage());
				throw (new Exception (e));
			}
		}
	}
}
