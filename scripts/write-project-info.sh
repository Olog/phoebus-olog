#!/usr/bin/env bash

touch PROJECT_INFO.txt
echo "PROJECT_GROUP_ID="$(mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout) >> PROJECT_INFO.txt
echo "PROJECT_ARTIFACT_ID="$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout) >> PROJECT_INFO.txt
echo "PROJECT_VERSION="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout) >> PROJECT_INFO.txt
