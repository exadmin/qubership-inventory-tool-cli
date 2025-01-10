#!/bin/bash

# Sample script: run a docker container with iTool in assembly mode. Local pathes shall be reviewed to run somewhere else.

mkdir -p assembly

docker run \
    -v /c/Work/git/iTool/qubership-inventory-tool-cli/docker/results:/var/input \
    -v /c/Work/git/iTool/qubership-inventory-tool-cli/docker/assembly:/var/output \
    itool ci-assembly \
    --outputFile=assembly.result.json.gz \
    --appName=Application-Name \
    --appVersion=Application-Version
