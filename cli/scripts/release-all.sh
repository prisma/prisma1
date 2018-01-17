#!/bin/bash

set -e

cd packages

cd prisma-yml
echo -e "Releasing prisma-yml\n\n"
npm version patch --no-git-tag-version
npm publish
export ymlversion=$(cat package.json | jq -r '.version')
cd ..

cd prisma-cli-engine
echo -e "\n\nAdding prisma-yml@$ymlversion to prisma-cli-engine\n\n"
sleep 1
yarn add prisma-yml@$ymlversion
yarn install
npm version patch --no-git-tag-version
npm publish
export engineversion=$(cat package.json | jq -r '.version')
cd ..

cd prisma-cli-core
echo -e "\n\nAdding prisma-yml@$ymlversion to prisma-cli-core\n\n"
sleep 1
yarn add prisma-yml@$ymlversion
yarn install
npm version patch --no-git-tag-version
npm publish
export coreversion=$(cat package.json | jq -r '.version')
cd ..

cd prisma-cli
yarn add prisma-cli-core@$coreversion prisma-cli-engine@$engineversion
yarn install

# detect if alpha, beta or latest should be used
export version=$(cat package.json | jq -r '.version')

if [[ $version == *"beta"* ]]; then
  echo "It's a beta!!"
elif [[ $version == *"alpha"* ]]; then
  echo "It's a beta!!"
else
  echo "It's latet!!"
fi