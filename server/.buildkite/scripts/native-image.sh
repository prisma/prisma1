#!/bin/bash

## Note: Execute in context of server sub folder of git repo/
set -e
export BUILDKITE_ARTIFACT_UPLOAD_DESTINATION="s3://$ARTIFACT_BUCKET/$BUILDKITE_JOB_ID"

docker run -e "BRANCH=$BUILDKITE_BRANCH" -e "COMMIT_SHA=$BUILDKITE_COMMIT" -e "CLUSTER_VERSION=$DOCKER_TAG" \
  -w /root/build \
  -v $(pwd):/root/build \
  -v ~/.ivy2:/root/.ivy2 \
  -v ~/.coursier:/root/.coursier \
  -v /var/run/docker.sock:/var/run/docker.sock \
  prismagraphql/build-image:debian sbt "project prisma-native" prisma-native-image:packageBin
#  -v /.cargo/:~/root/.cargo \

buildkite-agent artifact upload images/prisma-native/target/prisma-native-image/prisma-native

if [ "$BUILDKITE_BRANCH" = "master" ]
then
    ./docker-native/build.sh latest
elif [ "$BUILDKITE_BRANCH" = "beta" ]
then
    ./docker-native/build.sh beta
elif [ "$BUILDKITE_BRANCH" = "alpha" ]
then
    ./docker-native/build.sh alpha
fi

## todo: Upload to github release on tag