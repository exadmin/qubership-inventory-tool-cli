#!/bin/bash

# Sample script: run a docker container with iTool in CI mode. Local paths shall be reviewed to run somewhere else.
# --repository is mandatory parameter, --outputFile is optional

mkdir -p results

docker run \
    -v /c/Work/Projects/iTool/superrepo/repositories/domain/component:/var/input \
    -v /c/Work/git/iTool/qubership-inventory-tool-cli/docker/results:/var/output \
    itool ci-exec \
    --componentName=component \
    --repository=https://git.your.host/path/component.git \
    --outputFile=result.component.json.gz
