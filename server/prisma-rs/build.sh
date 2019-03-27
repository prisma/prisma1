#!/bin/bash

set -e
cd "$(dirname "$0")"

declare -a channels=("release" "debug") # Release channels to copy

git submodule update --init || true
cargo build ${CARGO_FLAGS}
mkdir -p build

for i in "${channels[@]}"
do
    mv target/${i}/libnative_bridge.dylib build/ 2>/dev/null || :
    mv target/${i}/libnative_bridge.a build/libnative_bridge_static.a 2>/dev/null || :
    mv target/${i}/libnative_bridge.so build/ 2>/dev/null || :
    mv target/${i}/libnative_bridge.dll build/ 2>/dev/null || :
done
