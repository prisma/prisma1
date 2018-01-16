#!/bin/bash

set -e

cd packages/prisma-cli-engine
yarn install
yarn build
yarn test
cd ../../packages/prisma-cli-core
yarn test
cd ../../packages/prisma-yml
yarn test
