#!/bin/bash
set -e
export CONNECTOR_TO_TEST=$1
export PRISMA_BINARY_PATH=`pwd`/target/release/prisma
export MIGRATION_ENGINE_BINARY_PATH=`pwd`/target/release/migration-engine

echo "Will run tests against connector $CONNECTOR_TO_TEST"
echo $CONNECTOR_TO_TEST > connector_to_test

cargo build --release

cd query-engine/connector-test-kit
sbt -mem 3072 test