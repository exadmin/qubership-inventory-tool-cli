#!/bin/bash

# Sample script: run a docker container with iTool in obfuscation mode. Local pathes shall be reviewed to run somewhere else.

docker run \
    -v /c/Work/git/iTool/qubership-inventory-tool-cli/docker/assembly:/var/input \
    -v /c/Work/git/iTool/qubership-inventory-tool-cli/docker/assembly:/var/output \
    itool ci-obfuscate \
    --inputFile=assembly.result.json.gz \
    --outputFile=obfuscate.result.json.gz
