#!/bin/bash

set -e

cd packages/graphcool-yml
yarn build

cd ../..

cd packages/graphcool-cli-engine
yarn build

cd ../..

cd packages/graphcool-cli-core
yarn build

cd ../..

cd packages/graphcool-cli
yarn build