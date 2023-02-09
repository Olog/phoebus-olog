#!/usr/bin/env bash

# Create and clear out temporary file if it already exists
TMP_FILE=PROJECT_INFO.txt
touch $TMP_FILE
echo -n '' > $TMP_FILE

# Get Project info from maven
PROJECT_GROUP_ID=$(mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout)
PROJECT_ARTIFACT_ID=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

# Construct docker image names that conform to their standards
IMAGE_OWNER=$(echo $GITHUB_REPOSITORY_OWNER | tr '[:upper:]' '[:lower:]' | tr '.' '-')
IMAGE_PROJECT_GROUP_ID=$(echo $PROJECT_GROUP_ID | tr '[:upper:]' '[:lower:]' | tr '.' '-')
IMAGE_PROJECT_ARTIFACT_ID=$(echo $PROJECT_ARTIFACT_ID | tr '[:upper:]' '[:lower:]' | tr '.' '-')
IMAGE_PROJECT_VERSION=$(echo $PROJECT_VERSION | tr '[:upper:]' '[:lower:]' | tr -d '\-snapshot')

IMAGE_NAME="ghcr.io/${IMAGE_OWNER}/${IMAGE_PROJECT_GROUP_ID}-${IMAGE_PROJECT_ARTIFACT_ID}"
IMAGE_TIMESTAMP=$(date '+%Y%m%d%H%M%S')

# Write vars to the temporary file
echo "PROJECT_GROUP_ID="$PROJECT_GROUP_ID >> $TMP_FILE
echo "PROJECT_ARTIFACT_ID="$PROJECT_ARTIFACT_ID >> $TMP_FILE
echo "PROJECT_VERSION="$PROJECT_VERSION >> $TMP_FILE
echo "IMAGE_OWNER="$IMAGE_OWNER >> $TMP_FILE
echo "IMAGE_PROJECT_GROUP_ID="$IMAGE_PROJECT_GROUP_ID >> $TMP_FILE
echo "IMAGE_PROJECT_ARTIFACT_ID="$IMAGE_PROJECT_ARTIFACT_ID >> $TMP_FILE
echo "IMAGE_PROJECT_VERSION="$IMAGE_PROJECT_VERSION >> $TMP_FILE
echo "IMAGE_OWNER="$IMAGE_OWNER >> $TMP_FILE
echo "IMAGE_NAME="$IMAGE_NAME >> $TMP_FILE
echo "IMAGE_TIMESTAMP="$IMAGE_TIMESTAMP >> $TMP_FILE
echo "IMAGE_TAG_LATEST=${IMAGE_NAME}:latest" >> $TMP_FILE
echo "IMAGE_TAG_VERSION=${IMAGE_NAME}:${IMAGE_PROJECT_VERSION}" >> $TMP_FILE
echo "IMAGE_TAG_VERSION_TIMESTAMP=${IMAGE_NAME}:${IMAGE_PROJECT_VERSION}-${IMAGE_TIMESTAMP}" >> $TMP_FILE