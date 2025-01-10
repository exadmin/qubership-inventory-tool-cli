#!/bin/bash

# Build a docker container with iTool within *this* directory.
# Some versions of docker may need to transfer too much data if run in the top project directory.

echo Cleanup
rm -rf ./tmp

echo Gather files
mkdir -p ./tmp/target ./tmp/docker

# Gather exactly as written in Dockerfile:

# ADD target/inventory-tool /app/inventory-tool
cp -r ../target/inventory-tool  ./tmp/target/
# ADD target/qubership-inventory-tool-cli-*-fat.jar /app/inventory-tool.jar
cp ../target/qubership-inventory-tool-cli-*-fat.jar ./tmp/target/

# ADD docker/ci.properties /app/inventory-tool/default/config/profiles/ci.properties
# ADD docker/logback.xml /app/logback.xml
# ADD docker/ci-exec.sh      /usr/local/bin/ci-exec
# ADD docker/ci-assembly.sh  /usr/local/bin/ci-assembly
# ADD docker/ci-obfuscate.sh /usr/local/bin/ci-obfuscate
cp ci.properties ci-*.sh logback.xml  ./tmp/docker/

echo Docker build
cd tmp
docker build . -f ../../Dockerfile --tag itool
