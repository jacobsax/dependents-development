#!/bin/bash

set -e

if [[ $1 == "build" ]]; then
    mkdir -p ./tooling

    pushd ../
    mvn package
    popd

    cp ../target/java_parser_cli.jar-jar-with-dependencies.jar ./tooling/cli.jar
fi

pushd ./harness

python3 -m unittest discover -v

popd