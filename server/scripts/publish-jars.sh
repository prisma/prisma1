#! /bin/bash

set -e

docker run -e "BRANCH=${BUILDKITE_BRANCH}" -e "PACKAGECLOUD_PW=${PACKAGECLOUD_PW}" -e "GITHUB_ACCESS_TOKEN=${GITHUB_ACCESS_TOKEN}" -e "OTHER_REPO_OWNER=${OTHER_REPO_OWNER}" -e "OTHER_REPO=${OTHER_REPO}" -e "OTHER_REPO_FILE=${OTHER_REPO_FILE}" -v $(pwd):/root/build -w /root/build -v ~/.ivy2:/root/.ivy2 -v ~/.coursier:/root/.coursier -v /var/run/docker.sock:/var/run/docker.sock schickling/scala-sbt-docker sbt publish propagateVersionToOtherRepo
