#!/bin/bash

# Sample script: run a docker container with iTool in assembly mode. Local pathes shall be reviewed to run somewhere else.

mkdir -p assembly

docker run \
    -v ${PWD}/results:/var/input \
    -v ${PWD}/assembly:/var/output \
    itool sh /usr/local/bin/ci-assembly \
    --outputFile=assembly.result.json.gz \
    --appName=Application-Name \
    --appVersion=Application-Version
