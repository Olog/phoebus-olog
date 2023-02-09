#!/usr/bin/env bash

# Per GitHub docs: The $GITHUB_ENV is an environment
# variable that points to a temporary file. If you want
# to test locally, then export that var as a filepath, e.g.:
# export GITHUB_ENV=myfile.txt
# Otherwise you will be an "ambiguous redirect" error when
# running this script locally
touch $GITHUB_ENV
while read p; do
  echo "$p" >> $GITHUB_ENV
done <PROJECT_INFO.txt