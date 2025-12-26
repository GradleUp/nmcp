#!/usr/bin/env sh
set -e
set -x
./gradlew -p tests/jvm build
./gradlew -p tests/kmp publishAggregationToCentralPortal --configuration-cache
./gradlew -p tests/kmp build
