#!/bin/bash

set -xe
cd packages/prisma-datamodel
yarn
yarn build
cd ../prisma-yml
yarn
yarn build
cd ../prisma-cli-engine
yarn
yarn build
cd ../prisma-db-introspection
yarn
yarn build
cd ../prisma-client-lib
yarn
yarn build
cd ../prisma-cli-core
yarn
yarn build
cd ../prisma-generate-schema
yarn
yarn build
cd ../prisma-cli
yarn
yarn build
