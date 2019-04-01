#!/bin/bash

pushd ./pom-search-service
./build.sh
popd

pushd ./java-parser/build
./build.sh
popd

pushd ./pom-parsing-queue
./build.sh
popd

pushd ./ast-parsing-queue
./build.sh
popd

pushd ./main-dependents-service
./build.sh
popd

pushd ./dependents-visualisation-ui
./build.sh
popd

pushd ./nginx-proxy
./build.sh
popd