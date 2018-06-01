#! /bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/kill-all-docker-containers.sh

# Stable release through tag. Tag both x.x and x.x.x.
NEXT_DOCKER_TAG=${BUILDKITE_TAG}
IFS=. read major minor patch <<< ${BUILDKITE_TAG}

ADDITIONALLY_RELEASE=""

# Check which image we additionally have to tag. Either x.x or x.x.x, depending which tag we pushed
if [ -z $patch ]; then
    # We are releasing a x.x simage, so tag x.x.x as well.
    ADDITIONALLY_RELEASE="$BUILDKITE_TAG.0"
else
    # We already have x.x.x, so we need to retag the x.x image.
    ADDITIONALLY_RELEASE="$major.$minor"
fi

echo "Releasing stable ${NEXT_DOCKER_TAG} and ${ADDITIONALLY_RELEASE}..."
${DIR}/docker-build.sh stable ${NEXT_DOCKER_TAG} ${ADDITIONALLY_RELEASE}
