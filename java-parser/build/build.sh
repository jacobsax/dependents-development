#!/bin/bash


mkdir -p ./tmp
cp -r ../src ./tmp/src
cp ../pom.xml ./tmp/pom.xml

docker build -t java-parser-cli-base:latest .