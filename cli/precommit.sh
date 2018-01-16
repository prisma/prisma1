#!/bin/bash

set -e

cd packages/prisma-cli-engine
yarn precommit
cd ../../packages/prisma-cli-core
yarn precommit
