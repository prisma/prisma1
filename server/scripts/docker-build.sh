#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/check-if-tests-must-execute.sh
if [ $? -eq 0 ]; then
  exit 0;
fi

$DIR/kill-all-docker-containers.sh

docker run -e "BRANCH=${BUILDKITE_BRANCH}" -e "PACKAGECLOUD_PW=${PACKAGECLOUD_PW}" -e "GITHUB_ACCESS_TOKEN=${GITHUB_ACCESS_TOKEN}" -e "OTHER_REPO_OWNER=${OTHER_REPO_OWNER}" -e "OTHER_REPO=${OTHER_REPO}" -e "OTHER_REPO_FILE=${OTHER_REPO_FILE}" -v $(pwd):/root/build -w /root/build/server -v ~/.ivy2:/root/.ivy2 -v ~/.coursier:/root/.coursier  -v /var/run/docker.sock:/var/run/docker.sock schickling/scala-sbt-docker sbt docker

docker images

#TAG=$(echo $BUILDKITE_COMMIT | cut -c1-7)
TAG=latest

for service in graphcool-deploy graphcool-api graphcool-dev;
do
  latest=$(docker images graphcool/$service -q | head -n 1)

  echo "Tagging graphcool/$service:$latest image with $TAG..."
  docker tag graphcool/$service:$latest graphcool/$service:$TAG

  echo "Pushing graphcool/$service:$TAG..."
  docker push graphcool/$service:$TAG
done

#docker push graphcool/graphcool-dev:latest