#!/bin/bash

set -e
set -x

if [[ $CIRCLE_TAG ]]; then
  echo "WARNING: CIRCLE_TAG is set to $CIRCLE_TAG. This will publish a new version on the @latest tag."
  sleep 5
else
  echo "INFO: This will deploy a new version on the @beta tag"
fi

export changedFiles=$(git diff-tree --no-commit-id --name-only -r HEAD)

ymlChanged=false
coreChanged=false
engineChanged=false

if [[ "$changedFiles" = *"cli/packages/prisma-yml"* ]]; then
  ymlChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-cli-core"* ]]; then
  coreChanged=true
fi

if [[ "$changedFiles" = *"cli/packages/prisma-cli-engine"* ]]; then
  engineChanged=true
fi

echo "yml changed: $ymlChanged. core changed: $coreChanged. engine changed: $engineChanged"

cd cli/packages/

export ymlVersionBefore=$(cat prisma-yml/package.json | jq -r '.version')
if [ $ymlChanged ]; then
  echo "Going to publish yml"
  cd prisma-yml
  yarn build
  if [[ $CIRCLE_TAG ]]; then
    npm version patch --no-git-tag-version
    npm publish
  else
    npm version prerelease --no-git-tag-version
    npm publish --tag beta
  fi
  yarn install
  cd ..
fi
export ymlVersion=$(cat prisma-yml/package.json | jq -r '.version')

if [ $ymlVersionBefore != $ymlVersion ] || [ $engineChanged ]; then
  cd prisma-cli-engine
  sleep 3.0
  #npm install prisma-yml@$ymlVersion --save --no-package-lock
  yarn add prisma-yml@$ymlVersion
  sleep 0.2
  yarn install
  yarn build
  if [[ $CIRCLE_TAG ]]; then
    npm version patch --no-git-tag-version
    npm publish
  else
    npm version prerelease --no-git-tag-version
    npm publish --tag beta
  fi
  cd ..
fi
export engineVersion=$(cat prisma-cli-engine/package.json | jq -r '.version')

if [ $ymlVersionBefore != $ymlVersion ] || [ $coreChanged ]; then
  cd prisma-cli-core
  sleep 3.0
  # npm install prisma-yml@$ymlVersion --save --no-package-lock
  yarn add prisma-yml@$ymlVersion
  sleep 0.2
  yarn install
  yarn build
  if [[ $CIRCLE_TAG ]]; then
    npm version patch --no-git-tag-version
    npm publish
  else
    npm version prerelease --no-git-tag-version
    npm publish --tag beta
  fi
  cd ..
fi
export coreVersion=$(cat prisma-cli-core/package.json | jq -r '.version')

cd prisma-cli
sleep 0.5
yarn add prisma-cli-engine@$engineVersion prisma-cli-core@$coreVersion
yarn install
yarn build

if [ -z "$CIRCLE_TAG" ]; then
  latestVersion=$(npm info prisma version)
  latestBetaVersion=$(npm info prisma version --tag beta)
  betaMinor=`echo $latestBetaVersion | sed -n 's/[0-9]\{1,\}\.\([0-9]\{1,\}\)\.[0-9]\{1,\}.*/\1/p'`
  latestMinor=`echo $latestVersion | sed -n 's/[0-9]\{1,\}\.\([0-9]\{1,\}\)\.[0-9]\{1,\}/\1/p'`
  latestMajor=`echo $latestVersion | sed -n 's/\([0-9]\{1,\}\)\.\([0-9]\{1,\}\)\.[0-9]\{1,\}/\1/p'`
  betaLastNumber=`echo $latestBetaVersion | sed -n 's/.*beta\.\([0-9]\{1,\}\)/\1/p'`

  # calc next minor
  step=1
  nextMinor=$((minor + step))

  nextLastNumber=0

  echo "beta minor $betaMinor latest minor $latestMinor"

  # calc next last number
  if [ $betaMinor > $latestMinor ] && [ $betaMinor != $latestMinor ]; then
    echo "$betaMinor is greater than $latestMinor"
    nextLastNumber=$((betaLastNumber + step))
  fi

  newVersion="$latestMajor.$nextMinor.0-beta.$nextLastNumber"

  echo "new version: $newVersion"
  npm version $newVersion
  npm publish
else
  newVersion=$CIRCLE_TAG

  echo "new version: $newVersion"
  npm version $newVersion
  npm publish --tag beta
fi
