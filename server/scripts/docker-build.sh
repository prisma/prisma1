#! /bin/bash

set -e

docker run -e "BRANCH=${BUILDKITE_BRANCH}" -e "PACKAGECLOUD_PW=${PACKAGECLOUD_PW}" -e "GITHUB_ACCESS_TOKEN=${GITHUB_ACCESS_TOKEN}" -e "OTHER_REPO_OWNER=${OTHER_REPO_OWNER}" -e "OTHER_REPO=${OTHER_REPO}" -e "OTHER_REPO_FILE=${OTHER_REPO_FILE}" -v $(pwd):/root/build -w /root/build -v ~/.ivy2:/root/.ivy2 -v ~/.coursier:/root/.coursier  -v /var/run/docker.sock:/var/run/docker.sock schickling/scala-sbt-docker sbt docker

docker images

TAG=$(echo $BUILDKITE_COMMIT | cut -c1-7)


for service in backend-api-relay backend-api-simple backend-api-system backend-api-simple-subscriptions backend-api-subscriptions-websocket backend-api-fileupload backend-api-schema-manager backend-workers graphcool-dev localfaas;
do
  echo "Tagging graphcool/$service image with $TAG..."
  docker tag graphcool/$service graphcool/$service:$TAG
  echo "Pushing graphcool/$service:$TAG..."
  docker push graphcool/$service:$TAG
done

docker push graphcool/graphcool-dev:latest
docker push graphcool/localfaas:latest