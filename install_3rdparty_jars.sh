#install third party jars from 3rd_party_libs into the local maven repository

mvn install:install-file -Dfile=./3rd_party_libs/bayesseg.jar -DgroupId=bayesseg -DartifactId=bayesseg -Dversion=0.0 -Dpackaging=jar

mvn install:install-file -Dfile=./3rd_party_libs/lingpipe-3.9.1.jar -DgroupId=lingpipe -DartifactId=lingpipe -Dversion=3.9.1 -Dpackaging=jar

mvn install:install-file -Dfile=./3rd_party_libs/options.jar -DgroupId=options -DartifactId=options -Dversion=0.0 -Dpackaging=jar

mvn install:install-file -Dfile=./3rd_party_libs/sax2r2.jar -DgroupId=sax2r2 -DartifactId=sax2r2 -Dversion=0.0 -Dpackaging=jar

mvn install:install-file -Dfile=./3rd_party_libs/stanford-corenlp-3.2.0-models.jar -DgroupId=stanford-corenlp-models -DartifactId=stanford-corenlp-models -Dversion=3.2.0 -Dpackaging=jar