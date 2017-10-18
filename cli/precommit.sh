#!/bin/bash

set -e

cd packages/graphcool-cli-engine
yarn precommit
#cd ../../packages/graphcool-cli-core
#yarn precommit
