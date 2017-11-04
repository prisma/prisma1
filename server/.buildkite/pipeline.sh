#! /bin/bash

git diff --exit-code --name-only ${BUILDKITE_COMMIT} ${BUILDKITE_COMMIT}~1 | grep "server/"
if [ $? -ne 0 ]; then
  buildkite-agent pipeline upload ./server/.buildkite/empty-pipeline.yml
else
  buildkite-agent pipeline upload ./server/.buildkite/pipeline.yml
fi