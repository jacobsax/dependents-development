#!/bin/bash

if [ -d ./tmp ]; then rm -rf ./tmp; fi

mkdir ./tmp
cp -r ../src ./tmp/src
cp ../pom.xml ./tmp/pom.xml

docker build -t java-parser-cli-base:latest .