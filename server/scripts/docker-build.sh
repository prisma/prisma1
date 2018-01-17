#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/check-if-tests-must-execute.sh
if [ $? -eq 0 ]; then
  exit 0;
fi

$DIR/kill-all-docker-containers.sh

docker run -e "BRANCH=${BUILDKITE_BRANCH}" -v $(pwd):/root/build -w /root/build/server -v ~/.ivy2:/root/.ivy2 -v ~/.coursier:/root/.coursier  -v /var/run/docker.sock:/var/run/docker.sock graphcool/scala-sbt-docker sbt docker
docker images

TAG=latest

for service in deploy database prisma;
do
  latest=$(docker images prismagraphql/$service -q | head -n 1)

  echo "Tagging prismagraphql/$service ($latest) image with $TAG..."
  docker tag $latest prismagraphql/$service:$TAG

  echo "Pushing prismagraphql/$service:$TAG..."
  docker push prismagraphql/$service:$TAG
done
