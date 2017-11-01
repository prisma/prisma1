#! /bin/bash

git diff --exit-code --name-only ${BUILDKITE_COMMIT} ${BUILDKITE_COMMIT}~1 | grep "server/"
if [ $? -ne 0 ]; then
  echo "No changes have been detected in the server folder. Will skip the tests entirely."
  exit 0;
else 
  echo "Changes have been detected in the server folder."
  exit 1;
fi