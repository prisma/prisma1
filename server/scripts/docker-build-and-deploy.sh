#! /bin/bash

set -e

TOKEN=${GITHUB_TOKEN}
CLUSTERS="${1:?Provide the clusters as comma separated list that you want to deploy}"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

$DIR/kill-all-docker-containers.sh

if [ -z ${BUILDKITE_TAG} ]; then
    # Rolling number versioning for unstable
    LAST_GIT_TAG=$(git describe --abbrev=0) # Get closest git tag
    TAG_ELEMENTS=(${LAST_GIT_TAG//./ })
    NEXT_VERSION="${TAG_ELEMENTS[0]}.$((${TAG_ELEMENTS[1]} + 1))"
    LAST_DOCKER_TAG=$(curl -sS 'https://registry.hub.docker.com/v2/repositories/prismagraphql/prisma/tags/' | jq '."results"[]["name"]' --raw-output | grep -v latest | grep ${NEXT_VERSION}-beta | head -n 1)

    echo "Last git tag: $LAST_GIT_TAG"
    echo "Next version: $NEXT_VERSION"
    echo "Last docker tag: $LAST_DOCKER_TAG"

    if [ -z LAST_DOCKER_TAG ]; then
        NEXT_DOCKER_TAG="$NEXT_VERSION-beta-1"
    else
        IFS=- read version betaStr rollingVersion <<< $LAST_DOCKER_TAG
        NEXT_DOCKER_TAG="$NEXT_VERSION-beta-$(($rollingVersion + 1))"

        echo "Rolling version: $rollingVersion"
        echo "Next docker tag: $NEXT_DOCKER_TAG"
    fi
else
    # Stable release through tag
    NEXT_VERSION=${BUILDKITE_TAG}
    NEXT_DOCKER_TAG=${BUILDKITE_TAG}
fi

docker run -e "BRANCH=${BUILDKITE_BRANCH}" -e "COMMIT_SHA=${BUILDKIT_COMMIT}" -e "CLUSTER_VERSION=${NEXT_DOCKER_TAG}" -v $(pwd):/root/build -w /root/build/server -v ~/.ivy2:/root/.ivy2 -v ~/.coursier:/root/.coursier  -v /var/run/docker.sock:/var/run/docker.sock graphcool/scala-sbt-docker sbt docker
docker images

for service in prisma deploy database workers subscriptions;
do
  echo "Tagging prismagraphql/$service:latest image with $NEXT_DOCKER_TAG..."
  docker tag prismagraphql/$service:latest prismagraphql/$service:$NEXT_DOCKER_TAG

  echo "Pushing prismagraphql/$service:$NEXT_DOCKER_TAG..."
  docker push prismagraphql/$service:$NEXT_DOCKER_TAG

  if [ -z ${BUILDKITE_TAG} ]; then
    echo "Additionally tagging and pushing prismagraphql/$service:latest image with $NEXT_VERSION-beta..."
    docker tag prismagraphql/$service:latest prismagraphql/$service:$NEXT_VERSION-beta
    docker push prismagraphql/$service:$NEXT_VERSION-beta
  fi
done

echo "Fetching cb binary..."
curl --header "Authorization: token ${TOKEN}" \
     --header 'Accept: application/vnd.github.v3.raw' \
     --location "https://api.github.com/repos/graphcool/coolbelt/releases/latest" -sSL | \
     jq '.assets[] | select(.name == "coolbelt_linux") | .url' | \
     xargs -I "{}" \
         curl -sSL --header 'Accept: application/octet-stream' -o cb \
         --location "{}?access_token=${TOKEN}"

chmod +x cb

echo "Replacing images..."
export CB_MODE=env
export CB_TAG_OVERRIDE=${NEXT_DOCKER_TAG}

CLUSTER_ELEMENTS=(${CLUSTERS//,/ })
for cluster in "${CLUSTER_ELEMENTS[@]}"
do
    ./cb service replace-all --customer graphcool --cluster $cluster
done
