#!/bin/bash

set -e

cd packages/prisma-yml
yarn build
yarn test
cd ../prisma-cli-engine
yarn build
yarn test
cd ../prisma-cli-core
yarn build
yarn test
