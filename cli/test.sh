#!/bin/bash

set -e

cd packages/graphcool-cli-engine
npm run build
npm test
cd ../../packages/graphcool-cli-core
npm test
