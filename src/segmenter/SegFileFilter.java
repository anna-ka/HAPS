package segmenter;

import java.io.File;
import java.io.FileFilter;

public class SegFileFilter implements FileFilter {

	String[] okExtensions = null;
	public SegFileFilter(String[] validExtensions)
	{
		this.okExtensions = validExtensions;
	}

	public boolean accept(File file)
	{
		for (String goodExtension: this.okExtensions)
		{
			if (file.getName().toLowerCase().endsWith(goodExtension))
				return true;
		}
		return false;
	}

}


