#!/bin/bash

# Sample script: run a docker container with iTool in CI mode. Local paths shall be reviewed to run somewhere else.
# --repository is mandatory parameter, --outputFile is optional

mkdir -p results

docker run \
    -v ${PWD}/component:/var/input \
    -v ${PWD}/results:/var/output \
    itool sh /usr/local/bin/ci-exec \
    --componentName=component \
    --repository=https://git.your.host/path/component.git \
    --outputFile=result.component.json.gz
