#!/bin/bash
cd /app
java \
    -Xmx512m -Dlogback.configurationFile=./logback.xml \
    -jar ./inventory-tool.jar \
    ci-obfuscate \
    --dockerMode=true \
    "$@"
