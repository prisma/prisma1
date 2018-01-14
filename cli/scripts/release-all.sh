#!/bin/bash

set -e

cd packages/prisma-yml
echo "Releasing prisma-yml"
npm version patch --no-git-tag-version
npm publish
export ymlversion=$(cat package.json | jq -r '.version')
cd ..

cd prisma-cli-engine
echo "Adding prisma-yml@$ymlversion to prisma-cli-engine"
yarn add prisma-yml@$ymlversion
npm version patch --no-git-tag-version
npm publish
export engineversion=$(cat package.json | jq -r '.version')
cd ..

cd prisma-cli-core
echo "Adding prisma-yml@$ymlversion to prisma-cli-core"
yarn add prisma-yml@$ymlversion
npm version patch --no-git-tag-version
npm publish
export coreversion=$(cat package.json | jq -r '.version')
cd ..

cd prisma-cli
yarn add prisma-cli-core@$coreversion prisma-cli-engine@$engineversion

# detect if alpha, beta or latest should be used
export version=$(cat package.json | jq -r '.version')

if [[ $version == *"beta"* ]]; then
  echo "It's a beta!!"
elif [[ $version == *"alpha"* ]]; then
  echo "It's a beta!!"
else
  echo "It's latest!!"
fi