#!/bin/bash

DOT_FILE=test-graph.dot
IMAGE_FILE=test-img.png

pushd ../
mvn package
popd

./parse --source_dir /Users/jwu/3rd-year-project/Development/testing/java/JavaParser/src --jar_dir /Users/jwu/.m2 --jar_dir /Users/jwu/.gradle --jar_dir /Users/jwu/3rd-year-project/Development/testing/java/JavaParser/src -o $DOT_FILE -p parser_project -i parser_id_0

dot -Tpng $DOT_FILE > $IMAGE_FILE
open -a "Xee³" ./$IMAGE_FILE