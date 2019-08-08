#!/bin/bash
set -e

cargo build --release

export PRISMA_BINARY_PATH=`pwd`/target/release/prisma
export MIGRATION_ENGINE_BINARY_PATH=`pwd`/target/release/migration-engine

cd query-engine/connector-test-kit
sbt -mem 3072 test