#! /bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/check-if-tests-must-execute.sh
if [ $? -eq 0 ]; then
  exit 0;
fi

$DIR/kill-all-docker-containers.sh

docker run -e "BRANCH=${BUILDKITE_BRANCH}" -e "PACKAGECLOUD_PW=${PACKAGECLOUD_PW}" -e "GITHUB_ACCESS_TOKEN=${GITHUB_ACCESS_TOKEN}" -e "OTHER_REPO_OWNER=${OTHER_REPO_OWNER}" -e "OTHER_REPO=${OTHER_REPO}" -e "OTHER_REPO_FILE=${OTHER_REPO_FILE}" -v $(pwd):/root -v /var/run/docker.sock:/var/run/docker.sock schickling/scala-sbt-docker sbt docker publish propagateVersionToOtherRepo

docker images

TAG=$(echo $BUILDKITE_COMMIT | cut -c1-7)


for service in backend-api-relay backend-api-simple backend-api-system backend-api-simple-subscriptions backend-api-subscriptions-websocket backend-api-fileupload backend-api-schema-manager backend-workers graphcool-dev;
do
  echo "Tagging graphcool/$service image with $TAG..."
  docker tag graphcool/$service graphcool/$service:$TAG
  echo "Pushing graphcool/$service:$TAG..."
  docker push graphcool/$service:$TAG
done

docker push graphcool/graphcool-dev:latest