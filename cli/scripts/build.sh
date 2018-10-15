#!/bin/bash

cd packages/prisma-yml
yarn build
cd ../prisma-cli-engine
yarn build
cd ../prisma-db-introspection
yarn build
cd ../prisma-cli-core
yarn build
cd ../prisma-generate-schema
yarn build
cd ../prisma-cli
yarn build
