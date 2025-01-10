#!/bin/bash

exec /usr/bin/java \
    -Xmx512m -Dlogback.configurationFile=/app/logback.xml \
    -jar /app/inventory-tool.jar \
    ci-assembly \
    --dockerMode=true \
    "$@"
