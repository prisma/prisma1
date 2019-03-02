#!/bin/bash

set -e
TAG="${1:?Provide the tag this script is building}"

cd "$(dirname "$0")"

SCRIPT_ROOT=$(dirname "$(pwd)")
BK_ROOT=$(dirname "$SCRIPT_ROOT")
SERVER_ROOT=$(dirname "$BK_ROOT")

cp ${SERVER_ROOT}/images/prisma-native/target/prisma-native-image/prisma-native .

docker build -t prismagraphql/prisma-native:${TAG} .
docker push prismagraphql/prisma-native:${TAG}

rm prisma-native
