#!/bin/bash

set -e
export BUILDKITE_ARTIFACT_UPLOAD_DESTINATION="s3://$ARTIFACT_BUCKET/$BUILDKITE_JOB_ID"

# Use current dir as working dir base
cd "$(dirname "$0")"

SCRIPT_ROOT=$(dirname "$(pwd)")
BK_ROOT=$(dirname "$SCRIPT_ROOT")
SERVER_ROOT=$(dirname "$BK_ROOT")

docker run -e "BRANCH=$BUILDKITE_BRANCH" -e "COMMIT_SHA=$BUILDKITE_COMMIT" -e "CLUSTER_VERSION=$DOCKER_TAG" \
  -w /root/build \
  -v ${SERVER_ROOT}:/root/build \
  -v ~/.ivy2:/root/.ivy2 \
  -v ~/.coursier:/root/.coursier \
  -v /var/run/docker.sock:/var/run/docker.sock \
  prismagraphql/build-image:debian sbt "project prisma-native" prisma-native-image:packageBin
#  -v /.cargo/:~/root/.cargo \

buildkite-agent artifact upload ${SERVER_ROOT}/images/prisma-native/target/prisma-native-image/prisma-native

if [ "$BUILDKITE_BRANCH" = "master" ]
then
    ${SCRIPT_ROOT}/docker-native/build.sh latest
elif [ "$BUILDKITE_BRANCH" = "beta" ]
then
    ${SCRIPT_ROOT}/docker-native/build.sh beta
elif [ "$BUILDKITE_BRANCH" = "alpha" ]
then
    ${SCRIPT_ROOT}/docker-native/build.sh alpha
fi

## Todo This is for testing only before the merges. Remove in final cleanup pass.
${SCRIPT_ROOT}/docker-native/build.sh latest


## todo: Upload to github release on tag