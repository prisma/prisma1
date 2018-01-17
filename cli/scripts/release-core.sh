#!/bin/bash

set -e

cd packages

cd prisma-cli-core
echo -e "Releasing new core"
npm version patch --no-git-tag-version
npm publish
export coreversion=$(cat package.json | jq -r '.version')
cd ..

cd prisma-cli
sleep 0.2
yarn add prisma-cli-core@$coreversion
yarn install

# detect if alpha, beta or latest should be used
version=$(cat package.json | jq -r '.version')
lastnumber=`echo $version | sed -n 's/[0-9]\.[0-9]\.[0-9]-[[:alpha:]_]\{1,\}\.[0-9]\.[0-9]\.\(.*\)/\1/p'`
step=1
nextnumber=$((lastnumber + step))

if [[ $1 == "alpha" ]]; then
  newversion=`echo $version | sed s/beta/alpha/`
  newversion=`echo $newversion | sed -n 's/\(.*\)[0-9]/\1/p'`
  newversion="$newversion$nextnumber"
  echo -e "\nOld version: $version  New version: $newversion\n\n"
  npm version $newversion
  npm publish --tag alpha
elif [[ $1 == "beta" ]]; then
  newversion=`echo $version | sed s/beta/beta/`
  newversion=`echo $newversion | sed -n 's/\(.*\)[0-9]/\1/p'`
  newversion="$newversion$nextnumber"
  echo -e "\nOld version: $version  New version: $newversion\n\n"
  npm version $newversion
  npm publish --tag beta
else
  echo "You have to either provide beta or alpha"
fi