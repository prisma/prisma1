#! /bin/bash

set -e

CHANNEL="${1:?Provide the channel this script is run on (e.g. alpha, beta, stable)}"
DOCKER_TAG="${2:?Provide the docker tag that should be released}"
ADDITIONALLY_RELEASE="${3:?Provide the secondary docker tag that should be released}"

docker run -e "BRANCH=$BUILDKITE_BRANCH" -e "COMMIT_SHA=$BUILDKITE_COMMIT" -e "CLUSTER_VERSION=$DOCKER_TAG" -v $(pwd):/root/build -w /root/build/server -v ~/.ivy2:/root/.ivy2 -v ~/.coursier:/root/.coursier  -v /var/run/docker.sock:/var/run/docker.sock graphcool/scala-sbt-docker sbt docker
docker images

for service in prisma prisma-prod;
do
  echo "Tagging prismagraphql/$service:latest image with $DOCKER_TAG..."
  docker tag prismagraphql/${service}:latest prismagraphql/${service}:${DOCKER_TAG}

  echo "Pushing prismagraphql/$service:$DOCKER_TAG..."
  docker push prismagraphql/${service}:${DOCKER_TAG}

  if [ ! -z "$ADDITIONALLY_RELEASE" ]; then
    echo "Additionally tagging and pushing prismagraphql/$service:latest image with $ADDITIONALLY_RELEASE..."
    docker tag prismagraphql/${service}:latest prismagraphql/${service}:${ADDITIONALLY_RELEASE}
    docker push prismagraphql/${service}:${ADDITIONALLY_RELEASE}
  fi
done

printf "
- trigger: \"prisma-cloud\"
  label: \":cloud: Trigger Prisma Cloud Tasks ${DOCKER_TAG} :cloud:\"
  async: true
  build:
    env:
        BUILD_TAGS: \"${DOCKER_TAG},${ADDITIONALLY_RELEASE}\"
        CHANNEL: \"${CHANNEL}\"
" | buildkite-agent pipeline upload

