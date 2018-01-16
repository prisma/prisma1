#!/bin/bash

set -e

cd packages/prisma-cli-engine
npm run build
npm test
cd ../../packages/prisma-cli-core
npm test
cd ../../packages/prisma-yml
npm test
