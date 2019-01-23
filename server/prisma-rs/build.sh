#!/bin/bash

set -e
cd "$(dirname "$0")"

declare -a channels=("release" "debug") # Release channels to copy

cargo build ${CARGO_FLAGS}
mkdir -p build

for i in "${channels[@]}"
do
    mv target/${i}/libprisma.a build/libprisma_static.a 2>/dev/null || :
    mv target/${i}/libprisma.dylib build/ 2>/dev/null || :
    mv target/${i}/libprisma.so build/ 2>/dev/null || :
    mv target/${i}/libprisma.dll build/ 2>/dev/null || :
done
