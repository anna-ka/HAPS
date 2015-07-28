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

//import dkpro.similarity.algorithms.api.SimilarityException;
//import dkpro.similarity.algorithms.api.TextSimilarityMeasure;
//import dkpro.similarity.algorithms.lexical.ngrams.WordNGramContainmentMeasure;
////import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.WordNGramJaccardMeasure;
//import dkpro.similarity.algorithms.lexical.ngrams.WordNGramJaccardMeasure;
////import dkpro.similarity.algorithms.lsr.path.WuPalmerComparator;
//import dkpro.similarity.algorithms.lsr.path.ResnikComparator;
//import dkpro.similarity.algorithms.lsr.path.WuPalmerComparator;
//
//import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
//import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
//import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
//import dkpro.similarity.algorithms.lsr.LexSemResourceComparator;
//import de.tudarmstadt.ukp.dkpro.lexsemresource.wordnet.WordNetResource;



public interface ISimMetric {
	
	double MeasureSimilarity(Object v1, Object v2) throws Exception;

}
