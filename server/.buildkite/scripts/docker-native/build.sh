#!/bin/bash

set -e

cd "$(dirname "$0")"
cp ../../../images/prisma-native/target/prisma-native-image/prisma-native .

docker build -t prismagraphql/prisma:native .
docker push prismagraphql/prisma:native
rm prisma-native