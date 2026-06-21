#!/usr/bin/env sh
set -e
set -x
./gradlew -p tests/jvm build
./gradlew -p tests/kmp publishAggregationToCentralPortal
./gradlew -p tests/kmp nmcpPublishDeployment -PnmcpDeploymentId=599ab6f5-dd08-4e7b-ae5a-85b45031715a
./gradlew -p tests/kmp build
./gradlew -p tests/publish-all-checksums build
