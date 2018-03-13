#! /bin/bash

if [ -z "$BUILDKITE_TAG" ]; then
    # Regular commit
    git diff --exit-code --name-only ${BUILDKITE_COMMIT} ${BUILDKITE_COMMIT}~1 | grep "server/"
    if [ $? -ne 0 ]; then
      buildkite-agent pipeline upload ./server/.buildkite/empty-pipeline.yml
    else
      buildkite-agent pipeline upload ./server/.buildkite/unstable-release.yml
    fi
else
    # Build was triggered by a tagged commit
    buildkite-agent pipeline upload ./server/.buildkite/stable-release.yml
fi
