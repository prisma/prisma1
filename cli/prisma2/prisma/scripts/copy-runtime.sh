#!/usr/bin/env bash

mkdir -p runtime
mkdir -p photogen_ncc_build
cp -R node_modules/@prisma/photon/runtime/* runtime
cp -R node_modules/photogen/photogen_ncc_build/* photogen_ncc_build
cp node_modules/@prisma/lift/dist/GeneratorWorker.js build/GeneratorWorker.js
cp -R node_modules/@prisma/studio-server/public/* build/public
rm -rf build/public/static/js/*.map
rm -rf build/public1/static/js/*.map