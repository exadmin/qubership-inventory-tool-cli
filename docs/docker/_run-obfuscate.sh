#!/bin/bash

# Sample script: run a docker container with iTool in obfuscation mode. Local pathes shall be reviewed to run somewhere else.

docker run \
    -v ${PWD}/assembly:/var/input \
    -v ${PWD}/assembly:/var/output \
    itool sh /usr/local/bin/ci-obfuscate \
    --inputFile=assembly.result.json.gz \
    --outputFile=obfuscate.result.json.gz
