#!/bin/bash

set -e
docker run -e "BRANCH=$BUILDKITE_BRANCH" -e "COMMIT_SHA=$BUILDKITE_COMMIT" -e "CLUSTER_VERSION=$DOCKER_TAG" -v $(pwd):/root/build -w /root/build -v ~/.ivy2:/root/.ivy2 -v ~/.coursier:/root/.coursier -v /var/run/docker.sock:/var/run/docker.sock prismagraphql/build-image sbt "project prisma-native" prisma-native-image:packageBin

## todo: Build & push image with binary inside
