This Java package contains the code of Hierarchical Affinity Propagation for Segmentation or HAPS system described in my COLING 2014 paper. 

I tried not to include external code to the extent possible and because of that this is a bare-bones package 
that does not contain the code for using a number of external resources for benchmarking, evaluating or computing similarities more intelligently.
If you need that code please contact me.


The main entry class is RunHAPS in 'experiments' package. You can run it like this, assuming all the correct libraries are in the class path:

RunHaps -useConfig config_file.txt

The directory called 'config' contains three example configuration files. Let's go over the parameters using hier_moonstone_simple as an example:

*preference_level0 = 0.5
*preference_level1 =  -0.4
*preference_level2 = -2

preference values softly suggest how fine-grained the segmentation should be. The lower the value (you can use small negative values too) the fewer segments there will be. 
We will need a preference value for each layer of segmentation.
Here the numbering is from the bottom of the tree to the top: preference_level2 (the lowest value) corresponds to the layer of segmentation closest to the root of the tree. 
preference_level0 (the highest value) corresponds to the leaves of the tree. The values for lower levels must be higher then for the upper ones (to enforce the tree shape).

*damping = 0.9
damping factor using when sending update messages. Reasonable values are between 0.8 to 0.95. 0.9 is a good default.

*windowRatio =  0.9
HAPS uses a sliding window when computing pairwise similarities between sentences or paragraphs. Effectively it reduces the size of the matrix by making it more sparse. For larger files
it may make sense to use a smaller window size (e.g. 0.3). However, the window size should be at least twice the expected size of the segment.

*inputDir = ./data/moonstone_hierarchical/text
The inputDir should contain 1) a folder 'text' containing the documents you want to segment and 2) a folder 'annots' containing the annotations, e.g., the gold standard segmentations 
to be used for evaluation. The annotations are .csv files in the following format:

 paragraph_id | level1_judgement | level2_judgements | level_n_judgements
 
 paragraph_id column is 1-based (the first paragraph's index is 1). The judgements are binary. For instance:
 
 par,level1,level2
 1,1,0
 2,1,1
 3,0,0
 4,1,1
 
 would correspond to a tiny document with 4 units (sentence or paragraphs). Here the annotator identified 2 nodes
 in the top level of the tree (breaks after units 2 and 4) and 3 nodes in the bottom level of the tree (breaks after 1,2 and 4). 
 Please note that by convention we always include a break after the last unit in the document.


*outputDir = ./Results/hier_moonstone_wd
annotDir= ./data/moonstone_hierarchical/annots
inputExtensions = txt
corpusExtensions = txt,df
resultsFile = moonstone_hier_wd_4_fold_5_03_2014.txt
sparse = true
numTFIDFsegments = 30
smoothing = true
smoothingAlpha = 1.45
smoothingWindow = 2
inputDataType = 4
evalMetric = winDiff
segmLevel = 2
numLevels = 3
useHier = true



If you use the package in your research please cite:

Anna Kazantseva, Stan Szpakowicz. 2014. Hierarchical Topical Segmentation with Affinity Propagation. COLING 2014: 37-47.
Anna Kazantseva. 2014. Topical Structure in Long Informal Documents. Ph.D. Thesis, University of Ottawa.